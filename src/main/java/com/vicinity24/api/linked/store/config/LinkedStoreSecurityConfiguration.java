package com.vicinity24.api.linked.store.config;

import com.vicinity24.api.core.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class LinkedStoreSecurityConfiguration {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public LinkedStoreSecurityConfiguration(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    @Order(10)
    public SecurityFilterChain linkedStoreSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher(
                        "/api/stores",
                        "/api/stores/**",
                        "/api/v1/stores",
                        "/api/v1/stores/**",
                        "/api/categories",
                        "/api/categories/**",
                        "/api/v1/categories",
                        "/api/v1/categories/**",
                        "/api/products",
                        "/api/products/*/variants",
                        "/api/products/**",
                        "/api/v1/products",
                        "/api/v1/products/*/variants",
                        "/api/v1/products/**"
                )
                .csrf(csrf -> csrf.disable())
                .sessionManagement(mgmt -> mgmt.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.HEAD, "/**").permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/stores",
                                "/api/stores/**",
                                "/api/v1/stores",
                                "/api/v1/stores/**",
                                "/api/categories",
                                "/api/categories/**",
                                "/api/v1/categories",
                                "/api/v1/categories/**",
                                "/api/products",
                                "/api/products/**",
                                "/api/v1/products",
                                "/api/v1/products/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
