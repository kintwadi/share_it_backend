package com.nearshare.api.enterprise.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

public class EnterpriseCategoryDTOs {
    @Data
    @Builder
    public static class CategoryItem {
        private String label;
        private List<String> keywords;
    }

    @Data
    @Builder
    public static class CategoryGroup {
        private String label;
        private List<CategoryItem> items;
    }

    @Data
    @Builder
    public static class CategorySector {
        private String label;
        private List<CategoryGroup> groups;
    }
}

