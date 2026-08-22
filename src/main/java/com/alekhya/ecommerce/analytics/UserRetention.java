package com.alekhya.ecommerce.analytics;

import java.math.BigDecimal;
import java.util.List;

public record UserRetention(
        Long userId,
        String name,
        String email,
        String role,
        int riskScore,
        int orderCount,
        BigDecimal totalSpend,
        int abandonedCarts,
        BigDecimal recoverableValue,
        long daysInactive,
        List<String> insights
) {
}
