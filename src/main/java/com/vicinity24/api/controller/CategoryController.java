package com.vicinity24.api.controller;

import com.vicinity24.api.dto.CategoryDTO;
import com.vicinity24.api.model.Category;
import com.vicinity24.api.repository.CategoryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryRepository categoryRepository;

    public CategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @GetMapping("/")
    public ResponseEntity<List<CategoryDTO>> list() {
        List<Category> categories = categoryRepository.findByActiveTrueOrderByCodeAsc();
        List<CategoryDTO> dtoList = categories.stream()
                .map(c -> CategoryDTO.builder()
                        .id(c.getId())
                        .code(c.getCode())
                        .labelKey(c.getLabelKey())
                        .build())
                .toList();
        return ResponseEntity.ok(dtoList);
    }
}

