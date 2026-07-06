package com.vicinity24.api.core.config;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "app_config_overrides")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppConfigOverride {
    @Id
    @Column(name = "key_name", nullable = false, length = 255)
    private String key;

    @Column(name = "value_json", nullable = false, columnDefinition = "text")
    private String valueJson;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 255)
    private String updatedBy;
}

