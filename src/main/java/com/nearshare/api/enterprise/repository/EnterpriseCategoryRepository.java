package com.nearshare.api.enterprise.repository;

import com.nearshare.api.enterprise.model.EnterpriseCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EnterpriseCategoryRepository extends JpaRepository<EnterpriseCategory, UUID> {
    long countBy();
    List<EnterpriseCategory> findAllByOrderBySectorAscCategoryGroupAscItemLabelAsc();
}

