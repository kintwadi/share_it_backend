package com.vicinity24.api.linked.store.filter;

import com.vicinity24.api.linked.store.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class StoreRequestTenantFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String header = request.getHeader(TenantContext.HEADER_NAME);
            if (header != null && !header.isBlank()) {
                long storeId;
                try {
                    storeId = Long.parseLong(header.trim());
                } catch (NumberFormatException ex) {
                    writeBadRequest(response);
                    return;
                }
                if (storeId <= 0) {
                    writeBadRequest(response);
                    return;
                }
                TenantContext.setStoreId(storeId);
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private void writeBadRequest(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"error\":\"invalid_store_header\",\"message\":\"X-Store-Id header must be a positive numeric value\"}"
        );
    }
}


