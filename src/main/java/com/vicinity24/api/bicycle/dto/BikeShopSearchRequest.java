package com.vicinity24.api.bicycle.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.util.List;

public record BikeShopSearchRequest(
        String query,
        String saleType,
        List<String> categories,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        String sort,
        @Min(0) Integer page,
        @Min(1) @Max(48) Integer size,
        @Valid List<BikeShopFilterSelectionRequest> filters
) {

    @JsonIgnore
    public int resolvedPage() {
        return page == null ? 0 : page;
    }

    @JsonIgnore
    public int resolvedSize() {
        return size == null ? 12 : size;
    }

    @JsonIgnore
    public String resolvedSort() {
        return (sort == null || sort.isBlank()) ? "featured" : sort;
    }
}
