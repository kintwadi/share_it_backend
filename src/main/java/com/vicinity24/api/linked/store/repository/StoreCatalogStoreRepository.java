package com.vicinity24.api.linked.store.repository;

import com.vicinity24.api.linked.store.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoreCatalogStoreRepository extends JpaRepository<Store, Long> {
    Optional<Store> findBySlugIgnoreCase(String slug);

    boolean existsBySlugIgnoreCase(String slug);
}


