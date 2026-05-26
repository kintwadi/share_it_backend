package com.nearshare.api.enterprise.controller;

import com.nearshare.api.enterprise.dto.EnterpriseCategoryDTOs;
import com.nearshare.api.enterprise.service.EnterpriseCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/enterprise")
@RequiredArgsConstructor
public class EnterpriseCategoryController {
    private final EnterpriseCategoryService service;

    @GetMapping("/categories")
    public ResponseEntity<List<EnterpriseCategoryDTOs.CategorySector>> categories() {
        return ResponseEntity.ok(service.getHierarchy());
    }
}

