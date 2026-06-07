package com.vicinity24.api.config.tenant;

import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public class TenantRegistry {
    private final String headerName;
    private final String defaultTenantId;
    private final boolean useDefaultDatabase;
    private final Map<String, TenantDefinition> tenants;

    public TenantRegistry(
            String headerName,
            String defaultTenantId,
            boolean useDefaultDatabase,
            Map<String, TenantDefinition> tenants
    ) {
        this.headerName = headerName;
        this.defaultTenantId = defaultTenantId;
        this.useDefaultDatabase = useDefaultDatabase;
        this.tenants = Map.copyOf(new LinkedHashMap<>(tenants));
    }

    public boolean hasTenant(String tenantId) {
        return tenants.containsKey(tenantId);
    }

    public record TenantDefinition(
            String tenantId,
            String url,
            String username,
            String password,
            String driverClassName
    ) {
    }
}
