package com.alekhya.ecommerce.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;

public record UpdateProductRequest(
        String name,
        String description,
        @DecimalMin("0.00") BigDecimal price,
        @Min(0) Integer availableQuantity,
        String variantName,
        String variantOptions
) {
    public UpdateProductRequest(String name, String description, BigDecimal price, Integer availableQuantity) {
        this(name, description, price, availableQuantity, null, null);
    }
}
