package com.vicinity24.api.bicycle.dto;

import java.util.List;

public record BikeShopPaginationDto(
        int currentPage,
        int pageSize,
        int totalPages,
        long totalElements,
        long from,
        long to,
        boolean hasNext,
        boolean hasPrevious,
        List<Integer> pages
) {
}
