package com.vicinity24.api.core.config;

import com.vicinity24.api.core.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);
    private final JwtAuthenticationFilter jwtFilter;
    private final List<String> allowedOriginPatterns;
    private final String tenantHeaderName;
    private final String publicContactHeaderName;
    private final String apiVersion;

    public SecurityConfig(
            JwtAuthenticationFilter jwtFilter,
            @Value("${security.cors.allowed-origin-patterns:}") String allowedOriginPatterns,
            @Value("${tenants.header-name:X-Tenant-ID}") String tenantHeaderName,
            @Value("${mail.contact.public-header-name:X-Public-Origin}") String publicContactHeaderName,
            @Value("${api.version:v1}") String apiVersion
    ) {
        this.jwtFilter = jwtFilter;
        this.allowedOriginPatterns = Arrays.stream(allowedOriginPatterns.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
        this.tenantHeaderName = tenantHeaderName;
        this.publicContactHeaderName = publicContactHeaderName;
        this.apiVersion = apiVersion != null ? apiVersion.trim() : "v1";
        if (this.allowedOriginPatterns.isEmpty()) {
            throw new IllegalStateException("security.cors.allowed-origin-patterns must not be empty");
        }
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        String versionedApiPrefix = "/api/" + (apiVersion == null || apiVersion.isBlank() ? "v1" : apiVersion);
        http.csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    log.warn("Security authentication failure: method={}, uri={}, origin={}, authHeaderPresent={}, message={}",
                            request.getMethod(),
                            request.getRequestURI(),
                            request.getHeader("Origin"),
                            request.getHeader("Authorization") != null,
                            authException.getMessage());
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                    log.warn("Security access denied: method={}, uri={}, origin={}, principal={}, authorities={}, message={}",
                            request.getMethod(),
                            request.getRequestURI(),
                            request.getHeader("Origin"),
                            auth != null ? auth.getName() : "anonymous",
                            auth != null ? auth.getAuthorities() : java.util.List.of(),
                            accessDeniedException.getMessage());
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
                })
            )
            .sessionManagement(mgmt -> mgmt.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(HttpMethod.HEAD, "/**").permitAll()
                .requestMatchers("/api/auth/verify-2fa-login", versionedApiPrefix + "/auth/verify-2fa-login").authenticated()
                .requestMatchers("/api/admin/auth/verify-2fa-login", "/api/partner/auth/verify-2fa-login", versionedApiPrefix + "/admin/auth/verify-2fa-login", versionedApiPrefix + "/partner/auth/verify-2fa-login").authenticated()
                .requestMatchers(
                        "/api/auth/login", "/api/auth/register", "/api/auth/forgot-password", "/api/auth/verify-reset-code", "/api/auth/reset-password", "/api/auth/email-verification/**",
                        "/api/admin/auth/login", "/api/admin/auth/register", "/api/partner/auth/login", "/api/partner/auth/register",
                        "/api/config/**", "/api/location/**", "/ws/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/api/payments/webhook", "/api/health", "/api/seed",
                        versionedApiPrefix + "/auth/login", versionedApiPrefix + "/auth/register", versionedApiPrefix + "/auth/forgot-password", versionedApiPrefix + "/auth/verify-reset-code", versionedApiPrefix + "/auth/reset-password", versionedApiPrefix + "/auth/email-verification/**",
                        versionedApiPrefix + "/admin/auth/login", versionedApiPrefix + "/admin/auth/register", versionedApiPrefix + "/partner/auth/login", versionedApiPrefix + "/partner/auth/register",
                        versionedApiPrefix + "/config/**", versionedApiPrefix + "/location/**", versionedApiPrefix + "/payments/webhook", versionedApiPrefix + "/health", versionedApiPrefix + "/seed"
                ).permitAll()
                .requestMatchers("/api/subscriptions/**", versionedApiPrefix + "/subscriptions/**").permitAll()
                .requestMatchers("/api/borrower-subscription/**", versionedApiPrefix + "/borrower-subscription/**").permitAll()
                .requestMatchers("/api/insurance/**", versionedApiPrefix + "/insurance/**").permitAll()
                .requestMatchers("/api/reviews/invite/**", versionedApiPrefix + "/reviews/invite/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/mail-contact-request", versionedApiPrefix + "/mail-contact-request").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/listings/**", versionedApiPrefix + "/listings/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/items/**", versionedApiPrefix + "/items/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/listings/evaluate", versionedApiPrefix + "/listings/evaluate").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/**", versionedApiPrefix + "/**").hasAnyRole("ADMIN", "LENDER", "BORROWER", "MEMBER")
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
        configuration.setAllowedHeaders(List.of("Authorization", "Cache-Control", "Content-Type", tenantHeaderName, publicContactHeaderName));
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
