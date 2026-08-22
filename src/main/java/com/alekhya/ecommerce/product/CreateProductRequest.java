package com.alekhya.ecommerce.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank String name,
        @NotBlank String description,
        @NotNull @DecimalMin("0.00") BigDecimal price,
        @NotNull @Min(0) Integer availableQuantity,
        String variantName,
        String variantOptions
) {
    public CreateProductRequest(String name, String description, BigDecimal price, Integer availableQuantity) {
        this(name, description, price, availableQuantity, null, null);
    }
}
