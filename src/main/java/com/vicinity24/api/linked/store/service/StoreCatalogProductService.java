package com.vicinity24.api.linked.store.service;

import com.vicinity24.api.linked.store.dto.StoreProductRequest;
import com.vicinity24.api.linked.store.dto.StoreProductResponse;
import com.vicinity24.api.linked.store.entity.Store;
import com.vicinity24.api.linked.store.entity.StoreCategory;
import com.vicinity24.api.linked.store.entity.StoreProduct;
import com.vicinity24.api.linked.store.repository.StoreCatalogCategoryRepository;
import com.vicinity24.api.linked.store.repository.StoreCatalogProductRepository;
import com.vicinity24.api.linked.store.repository.StoreCatalogStoreRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class StoreCatalogProductService extends StoreTenantScopedService {
    private final StoreCatalogProductRepository productRepository;
    private final StoreCatalogCategoryRepository categoryRepository;
    private final StoreCatalogStoreRepository storeRepository;
    private final LinkedStoreRealImageService realImageService;

    public StoreCatalogProductService(
            StoreCatalogProductRepository productRepository,
            StoreCatalogCategoryRepository categoryRepository,
            StoreCatalogStoreRepository storeRepository,
            LinkedStoreRealImageService realImageService
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.storeRepository = storeRepository;
        this.realImageService = realImageService;
    }

    @Transactional(readOnly = true)
    public List<StoreProductResponse> getAll(Long categoryId) {
        return inCurrentStore(() -> {
            List<StoreProduct> products;
            if (categoryId == null) {
                products = productRepository.findAll(Sort.by(Sort.Direction.ASC, "name", "id"));
            } else {
                findCategory(categoryId);
                products = productRepository.findByCategoryId(categoryId);
            }
            return products.stream().map(this::toResponse).toList();
        });
    }

    @Transactional(readOnly = true)
    public StoreProductResponse getById(Long id) {
        return inCurrentStore(() -> toResponse(findProduct(id)));
    }

    @Transactional
    public StoreProductResponse create(StoreProductRequest request) {
        return inCurrentStore(() -> {
            Store store = requireCurrentStore(storeRepository);
            StoreProduct product = new StoreProduct();
            product.setStore(store);
            applyRequest(product, request);
            return toResponse(productRepository.saveAndFlush(product));
        });
    }

    @Transactional
    public StoreProductResponse update(Long id, StoreProductRequest request) {
        return inCurrentStore(() -> {
            StoreProduct product = findProduct(id);
            applyRequest(product, request);
            return toResponse(productRepository.saveAndFlush(product));
        });
    }

    @Transactional
    public void delete(Long id) {
        inCurrentStore(() -> productRepository.delete(findProduct(id)));
    }

    private void applyRequest(StoreProduct product, StoreProductRequest request) {
        product.setSku(requireText(request.sku(), "product_sku_required"));
        product.setName(requireText(request.name(), "product_name_required"));
        product.setDescription(request.description() == null ? null : request.description().trim());
        product.setBasePrice(request.basePrice());
        product.setCurrency(normalizeCurrency(request.currency()));
        product.setProperties(copyMap(request.properties()));
        product.setActive(request.isActive() == null || request.isActive());
        if (request.categoryId() == null) {
            product.setCategory(null);
        } else {
            product.setCategory(findCategory(request.categoryId()));
        }
    }

    private StoreProduct findProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "product_not_found"));
    }

    private StoreCategory findCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "category_not_found"));
    }

    private StoreProductResponse toResponse(StoreProduct product) {
        var properties = copyMap(product.getProperties());
        properties.put("images", realImageService.resolveProductImages(product));
        return new StoreProductResponse(
                product.getId(),
                product.getStore().getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getBasePrice(),
                product.getCurrency(),
                product.getCategory() == null ? null : product.getCategory().getId(),
                properties,
                product.isActive(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}


