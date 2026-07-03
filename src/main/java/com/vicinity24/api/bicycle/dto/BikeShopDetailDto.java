package com.vicinity24.api.bicycle.dto;

import java.math.BigDecimal;
import java.util.List;

public record BikeShopDetailDto(
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
        List<BikeShopSkuDto> skus,
        List<BikeShopSpecGroupDto> specs
) {
}
