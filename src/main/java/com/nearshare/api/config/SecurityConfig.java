package com.nearshare.api.config;

import com.nearshare.api.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtFilter;
    private final List<String> allowedOriginPatterns;

    public SecurityConfig(
            JwtAuthenticationFilter jwtFilter,
            @Value("${security.cors.allowed-origin-patterns:}") String allowedOriginPatterns
    ) {
        this.jwtFilter = jwtFilter;
        this.allowedOriginPatterns = Arrays.stream(allowedOriginPatterns.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
        if (this.allowedOriginPatterns.isEmpty()) {
            throw new IllegalStateException("security.cors.allowed-origin-patterns must not be empty");
        }
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(mgmt -> mgmt.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(HttpMethod.HEAD, "/**").permitAll()
                .requestMatchers("/api/auth/verify-2fa-login").authenticated()
                .requestMatchers("/api/admin/auth/verify-2fa-login", "/api/partner/auth/verify-2fa-login").authenticated()
                .requestMatchers("/api/auth/login", "/api/auth/register", "/api/auth/forgot-password", "/api/auth/verify-reset-code", "/api/auth/reset-password", "/api/auth/email-verification/**", "/api/admin/auth/login", "/api/admin/auth/register", "/api/partner/auth/login", "/api/partner/auth/register", "/api/config/**", "/api/location/**", "/ws/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/api/payments/webhook", "/api/health", "/api/seed").permitAll()
                .requestMatchers("/api/subscriptions/**").permitAll()
                .requestMatchers("/api/insurance/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/listings/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/items/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/listings/evaluate").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/**").hasAnyRole("ADMIN", "LENDER", "BORROWER", "MEMBER")
                .requestMatchers(HttpMethod.GET, "/**").permitAll()
                .anyRequest().hasAnyRole("ADMIN", "LENDER", "BORROWER", "MEMBER"));
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(allowedOriginPatterns);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Cache-Control", "Content-Type"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
