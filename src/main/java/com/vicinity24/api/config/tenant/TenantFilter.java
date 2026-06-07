package com.vicinity24.api.config.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {
    private final TenantRegistry tenantRegistry;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = resolvePath(request);
        return !path.startsWith("/api")
                || path.equals("/error")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/ws")
                || path.equals("/api/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String tenantId = normalize(request.getHeader(tenantRegistry.getHeaderName()));
            if (tenantId == null) {
                if (tenantRegistry.isUseDefaultDatabase()) {
                    tenantId = tenantRegistry.getDefaultTenantId();
                } else {
                    writeBadRequest(response, "missing_tenant", "Missing tenant identifier header: " + tenantRegistry.getHeaderName());
                    return;
                }
            } else if (!tenantRegistry.hasTenant(tenantId)) {
                writeBadRequest(response, "invalid_tenant", "Unknown tenant identifier: " + tenantId);
                return;
            }

            TenantContextHolder.setTenantId(tenantId);
            filterChain.doFilter(request, response);
        } finally {
            // Clear request-scoped tenant data for pooled servlet threads.
            TenantContextHolder.clear();
        }
    }

    private String resolvePath(HttpServletRequest request) {
        String contextPath = request.getContextPath() == null ? "" : request.getContextPath();
        String requestUri = request.getRequestURI() == null ? "" : request.getRequestURI();
        return requestUri.startsWith(contextPath) ? requestUri.substring(contextPath.length()) : requestUri;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private void writeBadRequest(HttpServletResponse response, String error, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"" + error + "\",\"message\":\"" + escapeJson(message) + "\"}");
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
