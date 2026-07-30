package com.vicinity24.api.linked.store.service;

import com.vicinity24.api.linked.store.TenantContext;
import com.vicinity24.api.linked.store.entity.Store;
import com.vicinity24.api.linked.store.repository.StoreCatalogStoreRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

public abstract class StoreTenantScopedService {
    @PersistenceContext
    private EntityManager entityManager;

    protected Long requireCurrentStoreId() {
        Long storeId = TenantContext.getStoreId();
        if (storeId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing_store_header");
        }
        return storeId;
    }

    protected Store requireCurrentStore(StoreCatalogStoreRepository storeRepository) {
        Long storeId = requireCurrentStoreId();
        return storeRepository.findById(storeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "store_not_found"));
    }

    protected <T> T inCurrentStore(Supplier<T> supplier) {
        Long storeId = requireCurrentStoreId();
        Session session = entityManager.unwrap(Session.class);
        Filter filter = session.getEnabledFilter("tenantFilter");
        boolean alreadyEnabled = filter != null;
        if (alreadyEnabled) {
            filter.setParameter("storeId", storeId);
        } else {
            session.enableFilter("tenantFilter").setParameter("storeId", storeId);
        }
        try {
            return supplier.get();
        } finally {
            if (!alreadyEnabled) {
                session.disableFilter("tenantFilter");
            }
        }
    }

    protected void inCurrentStore(Runnable action) {
        inCurrentStore(() -> {
            action.run();
            return null;
        });
    }

    protected Map<String, Object> copyMap(Map<String, Object> input) {
        return input == null ? new LinkedHashMap<>() : new LinkedHashMap<>(input);
    }

    protected String normalizeSlug(String rawSlug) {
        String normalized = Normalizer.normalize(rawSlug == null ? "" : rawSlug.trim(), Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (normalized.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_slug");
        }
        return normalized;
    }

    protected String normalizeCurrency(String value) {
        if (value == null || value.isBlank()) {
            return "EUR";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    protected String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }
}


