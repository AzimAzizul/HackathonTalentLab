package com.alekhya.ecommerce.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alekhya.ecommerce.common.BadRequestException;
import com.alekhya.ecommerce.product.Product;
import com.alekhya.ecommerce.product.ProductService;
import com.alekhya.ecommerce.user.User;
import com.alekhya.ecommerce.user.UserRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductService productService;

    private CartService cartService;
    private User user;
    private Product product;

    @BeforeEach
    void setUp() {
        cartService = new CartService(cartRepository, userRepository, productService);
        user = new User("Alice", "alice@example.com");
        ReflectionTestUtils.setField(user, "id", 1L);
        product = new Product("Keyboard", "Mechanical keyboard", new BigDecimal("3499.00"), 10);
        ReflectionTestUtils.setField(product, "id", 1L);
    }

    @Test
    void addItemCreatesAnActiveCartWhenOneDoesNotExist() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserIdAndStatus(1L, CartStatus.ACTIVE)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productService.findProduct(1L)).thenReturn(product);

        CartResponse response = cartService.addItem(1L, new CartItemRequest(1L, 2));

        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().quantity()).isEqualTo(2);
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void addItemIncreasesQuantityForAnExistingProduct() {
        Cart cart = new Cart(user);
        cart.addItem(new CartItem(cart, product, 2));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserIdAndStatus(1L, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(productService.findProduct(1L)).thenReturn(product);

        CartResponse response = cartService.addItem(1L, new CartItemRequest(1L, 3));

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().quantity()).isEqualTo(5);
    }

    @Test
    void updateItemReplacesItsQuantity() {
        Cart cart = activeCartWithItem(2);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserIdAndStatus(1L, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));

        CartResponse response = cartService.updateItem(1L, 1L, new UpdateQuantityRequest(4));

        assertThat(response.items().getFirst().quantity()).isEqualTo(4);
    }

    @Test
    void removeItemDeletesItFromTheCart() {
        Cart cart = activeCartWithItem(2);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserIdAndStatus(1L, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));

        cartService.removeItem(1L, 1L);

        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    void getActiveCartFailsWhenTheUserHasNoActiveCart() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserIdAndStatus(1L, CartStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.getActiveCart(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("does not have an active cart");
    }

    private Cart activeCartWithItem(int quantity) {
        Cart cart = new Cart(user);
        cart.addItem(new CartItem(cart, product, quantity));
        return cart;
    }
}
