package com.alekhya.ecommerce.cart;

import com.alekhya.ecommerce.common.BadRequestException;
import com.alekhya.ecommerce.common.NotFoundException;
import com.alekhya.ecommerce.product.Product;
import com.alekhya.ecommerce.product.ProductService;
import com.alekhya.ecommerce.user.User;
import com.alekhya.ecommerce.user.UserRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductService productService;

    public CartService(CartRepository cartRepository, UserRepository userRepository, ProductService productService) {
        this.cartRepository = cartRepository;
        this.userRepository = userRepository;
        this.productService = productService;
    }

    @Transactional(readOnly = true)
    public CartResponse getActiveCart(Long userId) {
        getUser(userId);
        return CartResponse.from(getRequiredActiveCart(userId));
    }

    @Transactional
    public CartResponse addItem(Long userId, CartItemRequest request) {
        User user = getUser(userId);
        Cart cart = cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .orElseGet(() -> cartRepository.save(new Cart(user)));
        Product product = productService.findProduct(request.productId());

        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(product.getId()))
                .findFirst();

        if (existingItem.isPresent()) {
            existingItem.get().increaseQuantity(request.quantity());
        } else {
            cart.addItem(new CartItem(cart, product, request.quantity()));
        }

        return CartResponse.from(cart);
    }

    @Transactional
    public CartResponse updateItem(Long userId, Long productId, UpdateQuantityRequest request) {
        Cart cart = getRequiredActiveCartForUser(userId);
        CartItem item = getRequiredItem(cart, productId);
        item.setQuantity(request.quantity());
        return CartResponse.from(cart);
    }

    @Transactional
    public void removeItem(Long userId, Long productId) {
        Cart cart = getRequiredActiveCartForUser(userId);
        CartItem item = getRequiredItem(cart, productId);
        cart.getItems().remove(item);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User " + userId + " was not found."));
    }

    private Cart getRequiredActiveCartForUser(Long userId) {
        getUser(userId);
        return getRequiredActiveCart(userId);
    }

    private Cart getRequiredActiveCart(Long userId) {
        return cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .orElseThrow(() -> new BadRequestException("User " + userId + " does not have an active cart."));
    }

    private CartItem getRequiredItem(Cart cart, Long productId) {
        return cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Product " + productId + " is not in the active cart."));
    }
}
