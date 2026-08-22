package com.alekhya.ecommerce.product;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        int availableQuantity,
        String status,
        String variantName,
        List<String> variantOptions
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getAvailableQuantity(),
                product.getStatus().name(),
                product.getVariantName(),
                splitOptions(product.getVariantOptions())
        );
    }

    private static List<String> splitOptions(String options) {
        if (options == null || options.isBlank()) {
            return List.of();
        }
        return Arrays.stream(options.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
    }
}
