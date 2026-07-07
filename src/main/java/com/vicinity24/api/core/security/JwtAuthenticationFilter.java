package com.vicinity24.api.core.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, UserDetailsService userDetailsService) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String requestUri = String.valueOf(request.getRequestURI());
        String header = request.getHeader("Authorization");
        if (requestUri.contains("/borrower-subscription/")) {
            log.info("JWT filter borrower request: uri={}, hasAuthorizationHeader={}", requestUri, header != null && header.startsWith("Bearer "));
        }
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                String email = tokenProvider.getSubject(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                if (requestUri.contains("/borrower-subscription/")) {
                    log.info("JWT filter resolved borrower principal: uri={}, email={}, authorities={}", requestUri, email, userDetails.getAuthorities());
                }
                
                if (tokenProvider.isPreAuth(token)) {
                    // Restricted authentication for 2FA verification only
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        userDetails, null, java.util.Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_PRE_AUTH_2FA"))
                    );
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } else {
                    // Full authentication
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception e) {
                if (requestUri.contains("/borrower-subscription/")) {
                    log.warn("JWT filter failed borrower authentication for uri={}: {}", requestUri, e.getMessage(), e);
                }
            }
        }
        chain.doFilter(request, response);
    }
}
