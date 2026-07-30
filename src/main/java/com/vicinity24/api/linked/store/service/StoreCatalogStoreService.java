package com.vicinity24.api.linked.store.service;

import com.vicinity24.api.linked.store.dto.StoreResponse;
import com.vicinity24.api.linked.store.dto.StoreUpsertRequest;
import com.vicinity24.api.linked.store.entity.Store;
import com.vicinity24.api.linked.store.repository.StoreCatalogStoreRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class StoreCatalogStoreService extends StoreTenantScopedService {
    private final StoreCatalogStoreRepository storeRepository;
    private final LinkedStoreRealImageService realImageService;

    public StoreCatalogStoreService(StoreCatalogStoreRepository storeRepository, LinkedStoreRealImageService realImageService) {
        this.storeRepository = storeRepository;
        this.realImageService = realImageService;
    }

    @Transactional
    public StoreResponse create(StoreUpsertRequest request) {
        String slug = normalizeSlug(request.slug());
        if (storeRepository.existsBySlugIgnoreCase(slug)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "store_slug_exists");
        }

        Store store = new Store();
        store.setName(requireText(request.name(), "store_name_required"));
        store.setSlug(slug);
        return toResponse(storeRepository.save(store));
    }

    @Transactional(readOnly = true)
    public StoreResponse getById(Long id) {
        return storeRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "store_not_found"));
    }

    @Transactional(readOnly = true)
    public List<StoreResponse> getAll() {
        return storeRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private StoreResponse toResponse(Store store) {
        return new StoreResponse(
                store.getId(),
                store.getName(),
                store.getSlug(),
                resolveBannerImageUrl(store),
                store.getCreatedAt()
        );
    }

    private String resolveBannerImageUrl(Store store) {
        return realImageService.resolveStoreBanner(store);
    }
}


