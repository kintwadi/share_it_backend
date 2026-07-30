package com.vicinity24.api.linked.store.repository;

import com.vicinity24.api.linked.store.entity.StoreCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoreCatalogCategoryRepository extends JpaRepository<StoreCategory, Long> {
    Optional<StoreCategory> findBySlug(String slug);

    List<StoreCategory> findByParentId(Long parentId);

    Optional<StoreCategory> findByStoreIdAndSlugIgnoreCase(Long storeId, String slug);
}


