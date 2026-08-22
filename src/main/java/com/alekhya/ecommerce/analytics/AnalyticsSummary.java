package com.alekhya.ecommerce.analytics;

import java.math.BigDecimal;
import java.util.List;

public record AnalyticsSummary(
        int totalUsers,
        int atRiskUsers,
        int churnRatePercent,
        int avgRiskScore,
        BigDecimal totalRevenue,
        BigDecimal recoverableRevenue,
        List<ChurnTrendPoint> churnTrend
) {
}
