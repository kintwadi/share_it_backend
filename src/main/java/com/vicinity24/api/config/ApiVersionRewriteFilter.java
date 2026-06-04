package com.vicinity24.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiVersionRewriteFilter extends OncePerRequestFilter {

    @Value("${api.version:v1}")
    private String apiVersion;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws java.io.IOException, jakarta.servlet.ServletException {
        String v = String.valueOf(apiVersion == null ? "" : apiVersion).trim();
        if (v.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        String contextPath = String.valueOf(request.getContextPath() == null ? "" : request.getContextPath());
        String requestUri = String.valueOf(request.getRequestURI() == null ? "" : request.getRequestURI());
        String path = requestUri.startsWith(contextPath) ? requestUri.substring(contextPath.length()) : requestUri;

        String versionedPrefix = "/api/" + v;
        if (path.equals(versionedPrefix) || path.startsWith(versionedPrefix + "/")) {
            String newPath = "/api" + path.substring(versionedPrefix.length());
            HttpServletRequestWrapper wrapper = new HttpServletRequestWrapper(request) {
                @Override
                public String getRequestURI() {
                    return contextPath + newPath;
                }

                @Override
                public StringBuffer getRequestURL() {
                    StringBuffer original = super.getRequestURL();
                    String originalStr = original.toString();
                    String uri = request.getRequestURI();
                    if (uri == null) return original;
                    String replaced = originalStr.substring(0, originalStr.length() - uri.length()) + contextPath + newPath;
                    return new StringBuffer(replaced);
                }
            };
            filterChain.doFilter(wrapper, response);
            return;
        }

        filterChain.doFilter(request, response);
    }
}

