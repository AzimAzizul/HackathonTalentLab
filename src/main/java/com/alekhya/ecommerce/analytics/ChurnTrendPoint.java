package com.alekhya.ecommerce.analytics;

public record ChurnTrendPoint(
        String date,
        int abandonedCarts
) {
}
