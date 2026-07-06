package com.vicinity24.api.bicycle.service;

import com.vicinity24.api.bicycle.domain.valueobject.BikeCategory;
import com.vicinity24.api.bicycle.domain.valueobject.BikeSaleType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public record BikeSearchCriteria(
        String tenantId,
        String query,
        BikeSaleType saleType,
        Set<BikeCategory> categories,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        String sort,
        int page,
        int size,
        List<ResolvedFilterGroup> filters
) {
    public record ResolvedFilterGroup(
            String key,
            String attributeName,
            List<String> values,
            boolean componentSpecific
    ) {
    }
}
