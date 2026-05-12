package com.nearshare.api.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminPageResponse<T> {
    private List<T> items;
    private long total;
    private int page;
    private int size;
}

