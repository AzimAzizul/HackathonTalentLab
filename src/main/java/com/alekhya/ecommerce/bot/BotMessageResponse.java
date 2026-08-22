package com.alekhya.ecommerce.bot;

public record BotMessageResponse(
        String reply,
        Long conversationId
) {
}
