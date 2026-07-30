package com.vicinity24.api.linked.store.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Map;

public record StoreProductRequest(
        @NotBlank String sku,
        @NotBlank String name,
        String description,
        @NotNull @DecimalMin("0.00") BigDecimal basePrice,
        String currency,
        Long categoryId,
        Map<String, Object> properties,
        Boolean isActive
) {
}


