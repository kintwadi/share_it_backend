package com.vicinity24.api.bicycle.dto;

import java.math.BigDecimal;

public record BikeShopProductPreviewDto(
        Long id,
        String brandName,
        String modelName,
        String displayName,
        Integer modelYear,
        String category,
        String saleType,
        BigDecimal basePrice,
        String description,
        String imageUrl,
        boolean active,
        boolean inStock,
        int totalStock,
        int skuCount,
        String defaultColor
) {
}
