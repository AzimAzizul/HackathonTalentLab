package com.alekhya.ecommerce.bot;

public record EngagementResponse(
        boolean shouldEngage,
        String opener,
        String cartSummary,
        Long conversationId
) {
}
