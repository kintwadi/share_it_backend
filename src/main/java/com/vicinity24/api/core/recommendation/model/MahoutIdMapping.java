package com.vicinity24.api.core.recommendation.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "mahout_id_mapping", indexes = {
    @Index(name = "idx_mahout_mapping_entity", columnList = "entityId, entityType", unique = true),
    @Index(name = "idx_mahout_mapping_long", columnList = "mahoutId", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MahoutIdMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID entityId;

    @Column(nullable = false)
    private String entityType; // "USER" or "LISTING"

    @Column(nullable = false)
    private Long mahoutId;
}
