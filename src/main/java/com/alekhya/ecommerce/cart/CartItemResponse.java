package com.alekhya.ecommerce.cart;

import com.alekhya.ecommerce.product.ProductStatus;
import java.math.BigDecimal;

public record CartItemResponse(
        Long productId,
        String productName,
        BigDecimal price,
        int quantity,
        String variant,
        BigDecimal lineTotal,
        boolean discontinued
) {
    public static CartItemResponse from(CartItem item) {
        BigDecimal lineTotal = item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        return new CartItemResponse(
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getProduct().getPrice(),
                item.getQuantity(),
                item.getVariant(),
                lineTotal,
                item.getProduct().getStatus() == ProductStatus.DISCONTINUED
        );
    }
}
