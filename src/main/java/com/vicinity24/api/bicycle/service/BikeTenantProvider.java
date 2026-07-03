package com.vicinity24.api.bicycle.service;

import com.vicinity24.api.core.config.tenant.TenantContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Component
public class BikeTenantProvider {

    public String requireTenantId() {
        String tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "tenant_context_missing");
        }
        return tenantId;
    }
}
