package com.vicinity24.api.bicycle.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record BikeAdminUpsertBikeRequest(
        @NotBlank String brandName,
        @NotBlank String modelName,
        @NotNull Integer modelYear,
        @NotBlank String category,
        @NotBlank String saleType,
        @NotNull @DecimalMin("0.00") BigDecimal basePrice,
        String description,
        String imageUrl,
        Boolean isActive,
        @Valid List<BikeAdminSpecSelectionRequest> specs,
        @Valid @NotEmpty List<BikeAdminSkuRequest> skus
) {
    public record BikeAdminSpecSelectionRequest(
            @NotBlank String attributeName,
            @NotEmpty List<String> values
    ) {
    }

    public record BikeAdminSkuRequest(
            Long id,
            @NotBlank String skuCode,
            @NotBlank String colorName,
            @NotBlank String sizeValue,
            Integer riderHeightMinCm,
            Integer riderHeightMaxCm,
            Integer stackMm,
            Integer reachMm,
            @NotNull @Min(0) Integer stockQuantity,
            BigDecimal priceModifier
    ) {
    }
}
