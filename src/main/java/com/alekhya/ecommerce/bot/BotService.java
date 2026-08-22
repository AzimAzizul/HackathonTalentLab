package com.alekhya.ecommerce.bot;

import com.alekhya.ecommerce.cart.Cart;
import com.alekhya.ecommerce.cart.CartItem;
import com.alekhya.ecommerce.cart.CartRepository;
import com.alekhya.ecommerce.cart.CartStatus;
import com.alekhya.ecommerce.product.Product;
import com.alekhya.ecommerce.product.ProductResponse;
import com.alekhya.ecommerce.product.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BotService {

    private static final Logger log = LoggerFactory.getLogger(BotService.class);

    private final CartRepository cartRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final AiClient aiClient;
    private final ProductService productService;

    public BotService(
            CartRepository cartRepository,
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            AiClient aiClient,
            ProductService productService
    ) {
        this.cartRepository = cartRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.aiClient = aiClient;
        this.productService = productService;
    }

    @Transactional(readOnly = true)
    public EngagementResponse getEngagement(Long userId) {
        Cart cart = cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE).orElse(null);
        if (cart == null || cart.getItems().isEmpty()) {
            return new EngagementResponse(false, null, null, null);
        }
        String summary = buildCartSummary(cart);
        String catalog = buildCatalogSummary();
        String opener = generateOpener(cart, summary, catalog);
        return new EngagementResponse(true, opener, summary, null);
    }

    @Transactional
    public BotMessageResponse sendMessage(Long userId, String message) {
        Cart cart = cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE).orElse(null);
        String summary = cart == null ? "(empty)" : buildCartSummary(cart);
        String catalog = buildCatalogSummary();

        Conversation conversation = conversationRepository
                .findTopByUserIdAndStatusOrderByCreatedAtDesc(userId, ConversationStatus.OPEN)
                .orElseGet(() -> conversationRepository.save(new Conversation(userId, cart == null ? null : cart.getId())));

        String system = buildSystemPrompt(summary, catalog);
        String reply = generateReply(system, message);

        messageRepository.save(new Message(conversation.getId(), MessageRole.USER, message));
        messageRepository.save(new Message(conversation.getId(), MessageRole.BOT, reply));

        return new BotMessageResponse(reply, conversation.getId());
    }

    private String generateOpener(Cart cart, String summary, String catalog) {
        try {
            return aiClient.chat(
                    buildSystemPrompt(summary, catalog),
                    "The customer has items in their cart. Write a short, friendly message offering help with their "
                            + "order, referencing what is in their cart."
            );
        } catch (Exception exception) {
            log.warn("AI opener failed, using fallback: {}", exception.getMessage());
            return fallbackOpener(cart);
        }
    }

    private String generateReply(String system, String user) {
        try {
            String reply = aiClient.chat(system, user);
            if (reply == null || reply.isBlank()) {
                throw new IllegalStateException("empty reply");
            }
            return reply;
        } catch (Exception exception) {
            log.warn("AI reply failed, using fallback: {}", exception.getMessage());
            return "Thanks for reaching out! I can help with product details, availability, and variants. "
                    + "What would you like to know?";
        }
    }

    private String buildSystemPrompt(String cartSummary, String catalogSummary) {
        return "You are a friendly, concise shopping assistant for ShopLite. Help the customer complete their purchase. "
                + "Answer questions about products, variants, and prices using ONLY the catalog and cart information "
                + "provided below. Keep replies to 2-3 sentences. "
                + "All prices are in Malaysian Ringgit. Always write prices with 'RM' (for example 'RM 189.00') and "
                + "never use '$'. "
                + "Product catalog:\n" + catalogSummary + "\n"
                + "Current cart:\n" + cartSummary;
    }

    private String buildCatalogSummary() {
        StringBuilder builder = new StringBuilder();
        for (ProductResponse product : productService.getCatalog()) {
            builder.append("- ").append(product.name());
            builder.append(" at RM ").append(product.price());
            if (product.variantName() != null && !product.variantOptions().isEmpty()) {
                builder.append(" (").append(product.variantName())
                        .append(": ").append(String.join(", ", product.variantOptions())).append(")");
            }
            builder.append(" — ").append(product.availableQuantity()).append(" in stock");
            if (product.description() != null && !product.description().isBlank()) {
                builder.append(" — ").append(product.description());
            }
            builder.append("\n");
        }
        return builder.isEmpty() ? "(no products)" : builder.toString();
    }

    private String buildCartSummary(Cart cart) {
        StringBuilder builder = new StringBuilder();
        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            builder.append("- ").append(product.getName());
            if (item.getVariant() != null && !item.getVariant().isBlank()) {
                builder.append(" (").append(item.getVariant()).append(")");
            }
            builder.append(" x").append(item.getQuantity());
            builder.append(" at RM ").append(product.getPrice());
            if (product.getVariantName() != null && product.getVariantOptions() != null) {
                builder.append(" [").append(product.getVariantName()).append(": ").append(product.getVariantOptions()).append("]");
            }
            builder.append("\n");
        }
        return builder.isEmpty() ? "(empty)" : builder.toString();
    }

    private String fallbackOpener(Cart cart) {
        CartItem first = cart.getItems().isEmpty() ? null : cart.getItems().get(0);
        if (first == null) {
            return "Hi! How can I help with your shopping today?";
        }
        return "Hi! I see you have the " + first.getProduct().getName()
                + " in your cart. Is there anything I can help with before you check out?";
    }
}
