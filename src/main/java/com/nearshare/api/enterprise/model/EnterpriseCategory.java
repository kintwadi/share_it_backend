package com.nearshare.api.enterprise.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "enterprise_categories",
        uniqueConstraints = @UniqueConstraint(columnNames = {"sector", "category_group", "item_label"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnterpriseCategory {
    @Id
    private UUID id;

    @Column(nullable = false, length = 200)
    private String sector;

    @Column(name = "category_group", nullable = false, length = 200)
    private String categoryGroup;

    @Column(name = "item_label", nullable = false, length = 300)
    private String itemLabel;

    @Column(length = 1000)
    private String keywords;

    private LocalDateTime createdAt;
}

