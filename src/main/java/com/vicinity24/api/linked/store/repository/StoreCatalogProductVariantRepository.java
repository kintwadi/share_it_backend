package com.vicinity24.api.linked.store.repository;

import com.vicinity24.api.linked.store.entity.StoreProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoreCatalogProductVariantRepository extends JpaRepository<StoreProductVariant, Long> {
    Optional<StoreProductVariant> findBySku(String sku);

    List<StoreProductVariant> findByProductId(Long productId);

    Optional<StoreProductVariant> findByStoreIdAndSkuIgnoreCase(Long storeId, String sku);
}


