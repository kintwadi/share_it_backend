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
public class BicycleCatalogPageDto {
    private List<BicycleCatalogItemDto> content;
    private BicycleCatalogPaginationDto pagination;
}
