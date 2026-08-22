package com.alekhya.ecommerce.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.alekhya.ecommerce.cart.Cart;
import com.alekhya.ecommerce.cart.CartItem;
import com.alekhya.ecommerce.cart.CartRepository;
import com.alekhya.ecommerce.order.CustomerOrder;
import com.alekhya.ecommerce.order.OrderRepository;
import com.alekhya.ecommerce.product.Product;
import com.alekhya.ecommerce.user.Role;
import com.alekhya.ecommerce.user.User;
import com.alekhya.ecommerce.user.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private OrderRepository orderRepository;

    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsService(userRepository, cartRepository, orderRepository);
    }

    @Test
    void getUsersComputesRiskAndRecoverableValue() {
        User bob = new User("Bob", "bob@example.com", Role.CUSTOMER);
        ReflectionTestUtils.setField(bob, "id", 2L);

        Product keyboard = new Product("Keyboard", "Mechanical keyboard", new BigDecimal("189.00"), 10);
        ReflectionTestUtils.setField(keyboard, "id", 1L);

        Cart cart = new Cart(bob);
        cart.addItem(new CartItem(cart, keyboard, 2, "Tactile"));
        cart.markAbandoned();

        when(userRepository.findAll()).thenReturn(List.of(bob));
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(2L)).thenReturn(List.of());
        when(cartRepository.findByUserId(2L)).thenReturn(List.of(cart));

        List<UserRetention> users = analyticsService.getUsers();

        assertThat(users).hasSize(1);
        UserRetention user = users.getFirst();
        assertThat(user.riskScore()).isEqualTo(35);
        assertThat(user.abandonedCarts()).isEqualTo(1);
        assertThat(user.recoverableValue()).isEqualByComparingTo("378.00");
        assertThat(user.insights()).contains("No orders yet", "1 abandoned cart");
    }

    @Test
    void getSummaryAggregatesKpisAndExcludesAdmin() {
        User alice = new User("Alice", "alice@example.com", Role.CUSTOMER);
        ReflectionTestUtils.setField(alice, "id", 1L);
        User admin = new User("Admin", "admin@shop.test", Role.ADMIN);

        CustomerOrder order = new CustomerOrder(alice, new BigDecimal("100.00"));

        when(userRepository.findAll()).thenReturn(List.of(alice, admin));
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(order));
        when(cartRepository.findByUserId(1L)).thenReturn(List.of());
        when(orderRepository.findAll()).thenReturn(List.of(order));
        when(cartRepository.findAll()).thenReturn(List.of());

        AnalyticsSummary summary = analyticsService.getSummary();

        assertThat(summary.totalUsers()).isEqualTo(1);
        assertThat(summary.atRiskUsers()).isEqualTo(0);
        assertThat(summary.totalRevenue()).isEqualByComparingTo("100.00");
        assertThat(summary.avgRiskScore()).isEqualTo(15);
    }
}
