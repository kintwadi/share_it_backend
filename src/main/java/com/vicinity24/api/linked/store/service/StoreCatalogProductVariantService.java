package com.vicinity24.api.linked.store.service;

import com.vicinity24.api.linked.store.dto.StoreProductVariantRequest;
import com.vicinity24.api.linked.store.dto.StoreProductVariantResponse;
import com.vicinity24.api.linked.store.entity.StoreProduct;
import com.vicinity24.api.linked.store.entity.StoreProductVariant;
import com.vicinity24.api.linked.store.repository.StoreCatalogProductRepository;
import com.vicinity24.api.linked.store.repository.StoreCatalogProductVariantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
public class StoreCatalogProductVariantService extends StoreTenantScopedService {
    private final StoreCatalogProductVariantRepository variantRepository;
    private final StoreCatalogProductRepository productRepository;
    private final LinkedStoreRealImageService realImageService;

    public StoreCatalogProductVariantService(
            StoreCatalogProductVariantRepository variantRepository,
            StoreCatalogProductRepository productRepository,
            LinkedStoreRealImageService realImageService
    ) {
        this.variantRepository = variantRepository;
        this.productRepository = productRepository;
        this.realImageService = realImageService;
    }

    @Transactional(readOnly = true)
    public List<StoreProductVariantResponse> getAll(Long productId) {
        return inCurrentStore(() -> {
            requireProduct(productId);
            return variantRepository.findByProductId(productId).stream()
                    .map(this::toResponse)
                    .toList();
        });
    }

    @Transactional(readOnly = true)
    public StoreProductVariantResponse getById(Long productId, Long variantId) {
        return inCurrentStore(() -> toResponse(findVariant(productId, variantId)));
    }

    @Transactional
    public StoreProductVariantResponse create(Long productId, StoreProductVariantRequest request) {
        return inCurrentStore(() -> {
            StoreProduct product = requireProduct(productId);
            StoreProductVariant variant = new StoreProductVariant();
            variant.setStore(product.getStore());
            variant.setProduct(product);
            applyRequest(variant, request);
            return toResponse(variantRepository.saveAndFlush(variant));
        });
    }

    @Transactional
    public StoreProductVariantResponse update(Long productId, Long variantId, StoreProductVariantRequest request) {
        return inCurrentStore(() -> {
            StoreProductVariant variant = findVariant(productId, variantId);
            applyRequest(variant, request);
            return toResponse(variantRepository.saveAndFlush(variant));
        });
    }

    @Transactional
    public void delete(Long productId, Long variantId) {
        inCurrentStore(() -> variantRepository.delete(findVariant(productId, variantId)));
    }

    private void applyRequest(StoreProductVariant variant, StoreProductVariantRequest request) {
        variant.setSku(requireText(request.sku(), "variant_sku_required"));
        variant.setPrice(request.price());
        variant.setStock(request.stock() == null ? 0 : request.stock());
        variant.setOptions(copyMap(request.options()));
        variant.setActive(request.isActive() == null || request.isActive());
    }

    private StoreProduct requireProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "product_not_found"));
    }

    private StoreProductVariant findVariant(Long productId, Long variantId) {
        StoreProduct product = requireProduct(productId);
        StoreProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "variant_not_found"));
        if (!variant.getProduct().getId().equals(product.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "variant_not_found");
        }
        return variant;
    }

    private StoreProductVariantResponse toResponse(StoreProductVariant variant) {
        Map<String, Object> options = copyMap(variant.getOptions());
        options.put("images", realImageService.resolveVariantImages(variant));
        return new StoreProductVariantResponse(
                variant.getId(),
                variant.getStore().getId(),
                variant.getProduct().getId(),
                variant.getSku(),
                variant.getPrice(),
                variant.getStock(),
                options,
                variant.isActive(),
                variant.getCreatedAt()
        );
    }
}


