package com.vicinity24.api.admin.security;

import com.vicinity24.api.model.User;
import com.vicinity24.api.model.enums.AdminScope;
import com.vicinity24.api.model.enums.UserRole;
import com.vicinity24.api.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminScopeInterceptor implements HandlerInterceptor {
    private final UserRepository userRepository;

    public AdminScopeInterceptor(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        if (path == null) return true;
        if (!path.startsWith("/api/admin")) return true;
        if (path.startsWith("/api/admin/auth")) return true;
        if (path.startsWith("/api/admin/partner")) return true;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) return true;

        User user = userRepository.findByEmail(auth.getName()).orElse(null);
        if (user == null) return true;
        if (user.getRole() != UserRole.ADMIN) return true;

        AdminScope scope = user.getAdminScope() != null ? user.getAdminScope() : AdminScope.FULL;
        if (scope == AdminScope.PARTNER) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
        return true;
    }
}

