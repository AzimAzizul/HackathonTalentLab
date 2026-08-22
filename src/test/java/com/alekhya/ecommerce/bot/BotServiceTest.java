package com.alekhya.ecommerce.bot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alekhya.ecommerce.cart.Cart;
import com.alekhya.ecommerce.cart.CartItem;
import com.alekhya.ecommerce.cart.CartRepository;
import com.alekhya.ecommerce.cart.CartStatus;
import com.alekhya.ecommerce.product.Product;
import com.alekhya.ecommerce.product.ProductResponse;
import com.alekhya.ecommerce.product.ProductService;
import com.alekhya.ecommerce.user.User;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BotServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private AiClient aiClient;

    @Mock
    private ProductService productService;

    private BotService botService;

    @BeforeEach
    void setUp() {
        botService = new BotService(cartRepository, conversationRepository, messageRepository, aiClient, productService);
    }

    @Test
    void getEngagementReturnsFalseWhenThereIsNoActiveCart() {
        when(cartRepository.findByUserIdAndStatus(1L, CartStatus.ACTIVE)).thenReturn(Optional.empty());

        EngagementResponse response = botService.getEngagement(1L);

        assertThat(response.shouldEngage()).isFalse();
        verify(aiClient, never()).chat(anyString(), anyString());
    }

    @Test
    void getEngagementEngagesWhenCartHasItems() {
        User user = new User("Alice", "alice@example.com");
        Product product = new Product("Keyboard", "Mechanical keyboard", new BigDecimal("189.00"), 10);
        Cart cart = new Cart(user);
        cart.addItem(new CartItem(cart, product, 1));
        when(cartRepository.findByUserIdAndStatus(1L, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(aiClient.chat(anyString(), anyString())).thenReturn("Hi! I see something in your cart.");

        EngagementResponse response = botService.getEngagement(1L);

        assertThat(response.shouldEngage()).isTrue();
        assertThat(response.opener()).isEqualTo("Hi! I see something in your cart.");
        assertThat(response.conversationId()).isNull();
    }

    @Test
    void sendMessagePersistsUserAndBotMessages() {
        when(cartRepository.findByUserIdAndStatus(1L, CartStatus.ACTIVE)).thenReturn(Optional.empty());
        when(conversationRepository.findTopByUserIdAndStatusOrderByCreatedAtDesc(1L, ConversationStatus.OPEN))
                .thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> {
            Conversation conversation = invocation.getArgument(0);
            ReflectionTestUtils.setField(conversation, "id", 50L);
            return conversation;
        });
        when(aiClient.chat(anyString(), eq("Do you have this in size M?"))).thenReturn("Yes, size M is available!");

        BotMessageResponse response = botService.sendMessage(1L, "Do you have this in size M?");

        assertThat(response.reply()).isEqualTo("Yes, size M is available!");
        assertThat(response.conversationId()).isEqualTo(50L);
        verify(messageRepository, times(2)).save(any(Message.class));
    }

    @Test
    void sendMessageIncludesCatalogInPrompt() {
        ProductResponse product = new ProductResponse(2L, "Wireless Mouse", "Wireless mouse.",
                new BigDecimal("70.00"), 20, "ACTIVE", "Color", List.of("Black", "White"));
        when(productService.getCatalog()).thenReturn(List.of(product));
        when(cartRepository.findByUserIdAndStatus(1L, CartStatus.ACTIVE)).thenReturn(Optional.empty());
        when(conversationRepository.findTopByUserIdAndStatusOrderByCreatedAtDesc(1L, ConversationStatus.OPEN))
                .thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> {
            Conversation conversation = invocation.getArgument(0);
            ReflectionTestUtils.setField(conversation, "id", 52L);
            return conversation;
        });
        when(aiClient.chat(anyString(), eq("Tell me about the Wireless Mouse.")))
                .thenReturn("It comes in Black and White.");

        BotMessageResponse response = botService.sendMessage(1L, "Tell me about the Wireless Mouse.");

        assertThat(response.reply()).isEqualTo("It comes in Black and White.");
        verify(aiClient).chat(contains("Product catalog"), eq("Tell me about the Wireless Mouse."));
        verify(aiClient).chat(contains("Wireless Mouse"), eq("Tell me about the Wireless Mouse."));
    }

    @Test
    void sendMessageFallsBackWhenTheAiFails() {
        when(cartRepository.findByUserIdAndStatus(1L, CartStatus.ACTIVE)).thenReturn(Optional.empty());
        when(conversationRepository.findTopByUserIdAndStatusOrderByCreatedAtDesc(1L, ConversationStatus.OPEN))
                .thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> {
            Conversation conversation = invocation.getArgument(0);
            ReflectionTestUtils.setField(conversation, "id", 51L);
            return conversation;
        });
        when(aiClient.chat(anyString(), anyString())).thenThrow(new RuntimeException("boom"));

        BotMessageResponse response = botService.sendMessage(1L, "hello");

        assertThat(response.reply()).contains("Thanks for reaching out");
    }
}
