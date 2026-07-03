package com.vicinity24.api.bicycle.dto;

import java.math.BigDecimal;
import java.util.List;

public record BikeShopSidebarDto(
        BigDecimal minAvailablePrice,
        BigDecimal maxAvailablePrice,
        BigDecimal selectedMinPrice,
        BigDecimal selectedMaxPrice,
        boolean framesetSelected,
        List<BikeShopFilterSectionDto> sections
) {
}
