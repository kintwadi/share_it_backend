package com.vicinity24.api.bicycle.dto;

import java.util.List;

public record BikeShopSearchResponse(
        List<BikeShopProductPreviewDto> products,
        BikeShopPaginationDto pagination,
        BikeShopSidebarDto sidebar
) {
}
