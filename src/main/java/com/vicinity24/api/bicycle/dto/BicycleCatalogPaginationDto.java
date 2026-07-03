package com.vicinity24.api.bicycle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BicycleCatalogPaginationDto {
    private int currentPage;
    private int pageSize;
    private int totalPages;
    private long totalElements;
    private long from;
    private long to;
    private boolean hasNext;
    private boolean hasPrevious;
    private List<Integer> pages;
}
