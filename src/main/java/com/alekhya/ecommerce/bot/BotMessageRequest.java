package com.alekhya.ecommerce.bot;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BotMessageRequest(
        @NotNull Long userId,
        @NotBlank String message
) {
}
