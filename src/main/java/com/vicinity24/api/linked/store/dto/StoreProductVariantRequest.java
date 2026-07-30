package com.vicinity24.api.linked.store.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.Map;

public record StoreProductVariantRequest(
        @NotBlank String sku,
        BigDecimal price,
        @Min(0) Integer stock,
        Map<String, Object> options,
        Boolean isActive
) {
}


