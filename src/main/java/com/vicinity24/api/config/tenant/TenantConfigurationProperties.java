package com.vicinity24.api.config.tenant;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "tenants")
public class TenantConfigurationProperties {
    private String headerName = "X-Tenant-ID";
    private String defaultTenantId = "default";
    private boolean useDefaultDatabase = true;
    private Map<String, TenantDataSourceProperties> config = new LinkedHashMap<>();

    @Data
    public static class TenantDataSourceProperties {
        private String url;
        private String username;
        private String password;
        private String driverClassName;
    }
}
