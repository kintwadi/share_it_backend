package com.vicinity24.api.bicycle.dto;

import java.math.BigDecimal;

public record BikeShopSkuDto(
        Long id,
        String skuCode,
        String colorName,
        String sizeValue,
        Integer riderHeightMinCm,
        Integer riderHeightMaxCm,
        Integer stackMm,
        Integer reachMm,
        Integer stockQuantity,
        BigDecimal priceModifier
) {
}
