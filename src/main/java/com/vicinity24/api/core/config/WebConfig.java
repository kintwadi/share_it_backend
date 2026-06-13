package com.vicinity24.api.core.config;

import com.vicinity24.api.core.admin.security.AdminScopeInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class WebConfig implements WebMvcConfigurer {

    @Value("${spring.mvc.cors.allowed-origins}")
    private String[] allowedOrigins;

    @Value("${tenants.header-name:X-Tenant-ID}")
    private String tenantHeaderName;

    private final AdminScopeInterceptor adminScopeInterceptor;

    public WebConfig(AdminScopeInterceptor adminScopeInterceptor) {
        this.adminScopeInterceptor = adminScopeInterceptor;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedOriginPatterns(
                        "http://localhost",
                        "https://localhost",
                        "capacitor://localhost",
                        "http://localhost:*",
                        "https://localhost:*",
                        "http://127.0.0.1:*",
                        "https://127.0.0.1:*",
                        "https://*.onrender.com"
                )
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Cache-Control", "Content-Type", tenantHeaderName)
                .allowCredentials(true);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminScopeInterceptor).addPathPatterns("/api/admin/**");
    }
}
