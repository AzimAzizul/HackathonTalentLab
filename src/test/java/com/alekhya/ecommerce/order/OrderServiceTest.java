package com.alekhya.ecommerce.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alekhya.ecommerce.cart.Cart;
import com.alekhya.ecommerce.cart.CartItem;
import com.alekhya.ecommerce.cart.CartRepository;
import com.alekhya.ecommerce.cart.CartStatus;
import com.alekhya.ecommerce.common.BadRequestException;
import com.alekhya.ecommerce.common.ConflictException;
import com.alekhya.ecommerce.product.Product;
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
class OrderServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    private OrderService orderService;
    private User user;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(cartRepository, orderRepository, userRepository);
        user = new User("Alice", "alice@example.com");
        ReflectionTestUtils.setField(user, "id", 1L);
    }

    @Test
    void checkoutCreatesAnOrderAndReducesStock() {
        Product product = product(1L, "Keyboard", "3499.00", 5);
        Cart cart = new Cart(user);
        cart.addItem(new CartItem(cart, product, 2));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserIdAndStatus(1L, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(orderRepository.save(any(CustomerOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.checkout(1L);

        assertThat(response.status()).isEqualTo("PLACED");
        assertThat(response.totalAmount()).isEqualByComparingTo("6998.00");
        assertThat(response.items()).hasSize(1);
        assertThat(product.getAvailableQuantity()).isEqualTo(3);
        assertThat(cart.getStatus()).isEqualTo(CartStatus.CHECKED_OUT);
        verify(orderRepository).save(any(CustomerOrder.class));
    }

    @Test
    void checkoutRejectsAnEmptyCart() {
        Cart cart = new Cart(user);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserIdAndStatus(1L, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> orderService.checkout(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("empty");

        verify(orderRepository, never()).save(any(CustomerOrder.class));
    }

    @Test
    void checkoutRejectsInsufficientStockWithoutCreatingAnOrder() {
        Product product = product(1L, "Keyboard", "3499.00", 1);
        Cart cart = new Cart(user);
        cart.addItem(new CartItem(cart, product, 2));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserIdAndStatus(1L, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> orderService.checkout(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Not enough stock");

        assertThat(product.getAvailableQuantity()).isEqualTo(1);
        verify(orderRepository, never()).save(any(CustomerOrder.class));
    }

    @Test
    void checkoutRejectsACartWithADiscontinuedProduct() {
        Product product = product(1L, "Keyboard", "3499.00", 5);
        product.markDiscontinued();
        Cart cart = new Cart(user);
        cart.addItem(new CartItem(cart, product, 2));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserIdAndStatus(1L, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> orderService.checkout(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("discontinued");

        verify(orderRepository, never()).save(any(CustomerOrder.class));
    }

    private Product product(Long id, String name, String price, int quantity) {
        Product product = new Product(name, "Description", new BigDecimal(price), quantity);
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }
}
