package com.vicinity24.api.linked.store.service;

import com.vicinity24.api.linked.store.dto.StoreCategoryRequest;
import com.vicinity24.api.linked.store.dto.StoreCategoryResponse;
import com.vicinity24.api.linked.store.entity.Store;
import com.vicinity24.api.linked.store.entity.StoreCategory;
import com.vicinity24.api.linked.store.repository.StoreCatalogCategoryRepository;
import com.vicinity24.api.linked.store.repository.StoreCatalogStoreRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class StoreCatalogCategoryService extends StoreTenantScopedService {
    private final StoreCatalogCategoryRepository categoryRepository;
    private final StoreCatalogStoreRepository storeRepository;
    private final LinkedStoreRealImageService realImageService;

    public StoreCatalogCategoryService(
            StoreCatalogCategoryRepository categoryRepository,
            StoreCatalogStoreRepository storeRepository,
            LinkedStoreRealImageService realImageService
    ) {
        this.categoryRepository = categoryRepository;
        this.storeRepository = storeRepository;
        this.realImageService = realImageService;
    }

    @Transactional(readOnly = true)
    public List<StoreCategoryResponse> getAll() {
        return inCurrentStore(() -> {
            List<StoreCategory> categories = categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "name", "id"));
            Map<Long, List<StoreCategory>> childrenByParent = groupByParent(categories);
            return categories.stream()
                    .filter(category -> category.getParent() == null)
                    .map(category -> toResponse(category, childrenByParent))
                    .toList();
        });
    }

    @Transactional(readOnly = true)
    public StoreCategoryResponse getById(Long id) {
        return inCurrentStore(() -> {
            StoreCategory category = findCategory(id);
            Map<Long, List<StoreCategory>> childrenByParent = groupByParent(categoryRepository.findAll(Sort.by("name", "id")));
            return toResponse(category, childrenByParent);
        });
    }

    @Transactional
    public StoreCategoryResponse create(StoreCategoryRequest request) {
        return inCurrentStore(() -> {
            Store store = requireCurrentStore(storeRepository);
            StoreCategory category = new StoreCategory();
            category.setStore(store);
            category.setName(requireText(request.name(), "category_name_required"));
            category.setSlug(normalizeSlug(request.slug()));
            category.setAttributeSchema(copyMap(request.attributeSchema()));
            if (request.parentId() != null) {
                category.setParent(findCategory(request.parentId()));
            }
            StoreCategory saved = categoryRepository.saveAndFlush(category);
            Map<Long, List<StoreCategory>> childrenByParent = groupByParent(categoryRepository.findAll(Sort.by("name", "id")));
            return toResponse(saved, childrenByParent);
        });
    }

    @Transactional
    public StoreCategoryResponse update(Long id, StoreCategoryRequest request) {
        return inCurrentStore(() -> {
            StoreCategory category = findCategory(id);
            category.setName(requireText(request.name(), "category_name_required"));
            category.setSlug(normalizeSlug(request.slug()));
            category.setAttributeSchema(copyMap(request.attributeSchema()));
            if (request.parentId() == null) {
                category.setParent(null);
            } else {
                StoreCategory parent = findCategory(request.parentId());
                if (parent.getId().equals(id)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "category_parent_invalid");
                }
                category.setParent(parent);
            }
            StoreCategory saved = categoryRepository.saveAndFlush(category);
            Map<Long, List<StoreCategory>> childrenByParent = groupByParent(categoryRepository.findAll(Sort.by("name", "id")));
            return toResponse(saved, childrenByParent);
        });
    }

    @Transactional
    public void delete(Long id) {
        inCurrentStore(() -> categoryRepository.delete(findCategory(id)));
    }

    private StoreCategory findCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "category_not_found"));
    }

    private Map<Long, List<StoreCategory>> groupByParent(List<StoreCategory> categories) {
        Map<Long, List<StoreCategory>> childrenByParent = new LinkedHashMap<>();
        for (StoreCategory category : categories) {
            Long parentId = category.getParent() == null ? null : category.getParent().getId();
            childrenByParent.computeIfAbsent(parentId, ignored -> new ArrayList<>()).add(category);
        }
        return childrenByParent;
    }

    private StoreCategoryResponse toResponse(StoreCategory category, Map<Long, List<StoreCategory>> childrenByParent) {
        Long categoryId = category.getId();
        Long parentId = category.getParent() == null ? null : category.getParent().getId();
        List<StoreCategoryResponse> children = childrenByParent.getOrDefault(categoryId, List.of())
                .stream()
                .map(child -> toResponse(child, childrenByParent))
                .toList();
        Map<String, Object> attributeSchema = copyMap(category.getAttributeSchema());
        attributeSchema.put("bannerImageUrl", resolveBannerImageUrl(category));
        return new StoreCategoryResponse(
                categoryId,
                category.getStore().getId(),
                parentId,
                category.getName(),
                category.getSlug(),
                attributeSchema,
                children
        );
    }

    private String resolveBannerImageUrl(StoreCategory category) {
        return realImageService.resolveCategoryBanner(category);
    }
}

