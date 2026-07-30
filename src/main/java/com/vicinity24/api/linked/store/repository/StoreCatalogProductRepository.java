package com.vicinity24.api.linked.store.repository;

import com.vicinity24.api.linked.store.entity.StoreProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoreCatalogProductRepository extends JpaRepository<StoreProduct, Long> {
    Optional<StoreProduct> findBySku(String sku);

    List<StoreProduct> findByCategoryId(Long categoryId);

    Optional<StoreProduct> findByStoreIdAndSkuIgnoreCase(Long storeId, String sku);
}


