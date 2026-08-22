package com.alekhya.ecommerce.analytics;

import com.alekhya.ecommerce.cart.Cart;
import com.alekhya.ecommerce.cart.CartRepository;
import com.alekhya.ecommerce.cart.CartStatus;
import com.alekhya.ecommerce.order.CustomerOrder;
import com.alekhya.ecommerce.order.OrderRepository;
import com.alekhya.ecommerce.product.ProductStatus;
import com.alekhya.ecommerce.user.Role;
import com.alekhya.ecommerce.user.User;
import com.alekhya.ecommerce.user.UserRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsService {

    private static final int RISK_THRESHOLD = 40;
    private static final int TREND_DAYS = 30;
    private static final long ABANDONED_IDLE_DAYS = 1;

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;

    public AnalyticsService(UserRepository userRepository, CartRepository cartRepository, OrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.cartRepository = cartRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public List<UserRetention> getUsers() {
        return userRepository.findAll().stream()
                .filter(user -> user.getRole() == Role.CUSTOMER)
                .map(this::buildUserRetention)
                .toList();
    }

    @Transactional(readOnly = true)
    public AnalyticsSummary getSummary() {
        List<UserRetention> users = getUsers();
        int totalUsers = users.size();
        int atRiskUsers = (int) users.stream().filter(user -> user.riskScore() >= RISK_THRESHOLD).count();
        int churnRatePercent = totalUsers == 0 ? 0 : Math.round(atRiskUsers * 100f / totalUsers);
        int avgRiskScore = totalUsers == 0 ? 0 : (int) Math.round(
                users.stream().mapToInt(UserRetention::riskScore).average().orElse(0));
        BigDecimal totalRevenue = orderRepository.findAll().stream()
                .map(CustomerOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal recoverableRevenue = users.stream()
                .map(UserRetention::recoverableValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new AnalyticsSummary(totalUsers, atRiskUsers, churnRatePercent, avgRiskScore,
                totalRevenue, recoverableRevenue, buildChurnTrend());
    }

    private UserRetention buildUserRetention(User user) {
        Long userId = user.getId();
        List<CustomerOrder> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<Cart> carts = cartRepository.findByUserId(userId);

        int orderCount = orders.size();
        BigDecimal totalSpend = orders.stream()
                .map(CustomerOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        OffsetDateTime lastActivityAt = orders.isEmpty() ? null : orders.get(0).getCreatedAt();

        int abandonedCarts = 0;
        BigDecimal recoverableValue = BigDecimal.ZERO;
        boolean hasDiscontinuedItem = false;

        for (Cart cart : carts) {
            if (cart.getStatus() != CartStatus.ACTIVE || cart.getItems().isEmpty()) {
                continue;
            }
            OffsetDateTime activity = cart.getLastActivityAt();
            if (activity != null && (lastActivityAt == null || activity.isAfter(lastActivityAt))) {
                lastActivityAt = activity;
            }
            if (isAbandonedForAnalytics(cart)) {
                abandonedCarts++;
                recoverableValue = recoverableValue.add(cartValue(cart));
            }
            if (cartHasDiscontinuedItem(cart)) {
                hasDiscontinuedItem = true;
            }
        }

        long daysInactive = lastActivityAt == null
                ? 0
                : Math.max(0, Duration.between(lastActivityAt, OffsetDateTime.now()).toDays());

        int riskScore = computeRiskScore(daysInactive, abandonedCarts, orderCount, hasDiscontinuedItem);
        List<String> insights = buildInsights(orderCount, abandonedCarts, daysInactive, hasDiscontinuedItem);

        return new UserRetention(userId, user.getName(), user.getEmail(), user.getRole().name(),
                riskScore, orderCount, totalSpend, abandonedCarts, recoverableValue, daysInactive, insights);
    }

    private boolean isAbandonedForAnalytics(Cart cart) {
        if (cart.getAbandonedAt() != null) {
            return true;
        }
        OffsetDateTime activity = cart.getLastActivityAt();
        if (activity == null) {
            return false;
        }
        return Duration.between(activity, OffsetDateTime.now()).toDays() >= ABANDONED_IDLE_DAYS;
    }

    private int computeRiskScore(long daysInactive, int abandonedCarts, int orderCount, boolean hasDiscontinuedItem) {
        int score = 0;
        score += Math.min((int) (daysInactive * 2), 40);
        score += Math.min(abandonedCarts * 15, 30);
        score += orderCount == 0 ? 20 : Math.max(0, 20 - orderCount * 5);
        if (hasDiscontinuedItem) {
            score += 10;
        }
        return Math.min(score, 100);
    }

    private List<String> buildInsights(int orderCount, int abandonedCarts, long daysInactive, boolean hasDiscontinuedItem) {
        List<String> insights = new ArrayList<>();
        if (orderCount == 0) {
            insights.add("No orders yet");
        }
        if (abandonedCarts > 0) {
            insights.add(abandonedCarts + " abandoned cart" + (abandonedCarts == 1 ? "" : "s"));
        }
        if (daysInactive >= 7) {
            insights.add("Inactive for " + daysInactive + " days");
        }
        if (hasDiscontinuedItem) {
            insights.add("Cart contains a discontinued item");
        }
        return insights;
    }

    private BigDecimal cartValue(Cart cart) {
        return cart.getItems().stream()
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean cartHasDiscontinuedItem(Cart cart) {
        return cart.getItems().stream()
                .anyMatch(item -> item.getProduct().getStatus() == ProductStatus.DISCONTINUED);
    }

    private List<ChurnTrendPoint> buildChurnTrend() {
        List<Cart> allCarts = cartRepository.findAll();
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        List<ChurnTrendPoint> points = new ArrayList<>();
        for (int i = TREND_DAYS - 1; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            long count = allCarts.stream()
                    .filter(cart -> cart.getAbandonedAt() != null)
                    .map(cart -> cart.getAbandonedAt().toLocalDate())
                    .filter(date -> date.equals(day))
                    .count();
            points.add(new ChurnTrendPoint(day.format(formatter), (int) count));
        }
        return points;
    }
}
