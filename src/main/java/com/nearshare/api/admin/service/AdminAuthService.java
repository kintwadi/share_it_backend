package com.nearshare.api.admin.service;

import com.nearshare.api.admin.dto.AdminRegisterRequest;
import com.nearshare.api.dto.LocationDTO;
import com.nearshare.api.dto.LoginRequest;
import com.nearshare.api.dto.TokenResponse;
import com.nearshare.api.dto.UserDTO;
import com.nearshare.api.model.User;
import com.nearshare.api.model.embeddable.Location;
import com.nearshare.api.model.enums.AdminScope;
import com.nearshare.api.model.enums.UserRole;
import com.nearshare.api.model.enums.UserStatus;
import com.nearshare.api.model.enums.VerificationStatus;
import com.nearshare.api.partner.repository.PartnerAdminRepository;
import com.nearshare.api.repository.UserRepository;
import com.nearshare.api.security.JwtTokenProvider;
import com.nearshare.api.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AdminAuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthService authService;
    private final String signupSecret;
    private final PartnerAdminRepository partnerAdminRepository;

    public AdminAuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider,
            AuthService authService,
            PartnerAdminRepository partnerAdminRepository,
            @Value("${security.admin.signup.secret:}") String signupSecret
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.authService = authService;
        this.partnerAdminRepository = partnerAdminRepository;
        this.signupSecret = signupSecret != null ? signupSecret.trim() : "";
    }

    public TokenResponse login(LoginRequest request, String userAgent, String ipAddress) {
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> new RuntimeException("user_not_found"));
        ensureAdminAccess(user);
        if (user.getRole() != UserRole.ADMIN) {
            throw new RuntimeException("forbidden");
        }
        return authService.login(request, userAgent, ipAddress);
    }

    public TokenResponse verify2faLogin(String email, String code, String userAgent, String ipAddress) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("user_not_found"));
        ensureAdminAccess(user);
        if (user.getRole() != UserRole.ADMIN) {
            throw new RuntimeException("forbidden");
        }
        return authService.verify2faLogin(email, code, userAgent, ipAddress);
    }

    public TokenResponse register(AdminRegisterRequest request) {
        if (request == null) {
            throw new RuntimeException("invalid_request");
        }
        String email = request.getEmail();
        if (email == null || email.isBlank()) {
            throw new RuntimeException("email_required");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("email_exists");
        }

        if (!signupSecret.isBlank()) {
            String provided = request.getSignupSecret() != null ? request.getSignupSecret().trim() : "";
            if (!signupSecret.equals(provided)) {
                throw new RuntimeException("forbidden");
            }
        } else {
            throw new RuntimeException("forbidden");
        }

        User user = User.builder()
                .id(UUID.randomUUID())
                .name(request.getName() != null ? request.getName() : "")
                .email(email)
                .password(passwordEncoder.encode(request.getPassword() != null ? request.getPassword() : ""))
                .phone("")
                .address("")
                .avatarUrl("")
                .trustScore(50)
                .vouchCount(0)
                .verificationStatus(VerificationStatus.UNVERIFIED)
                .location(Location.builder().lat(0.0).lng(0.0).build())
                .joinedDate(LocalDateTime.now())
                .status(UserStatus.ACTIVE)
                .role(UserRole.ADMIN)
                .adminScope(parseAdminScope(request.getAdminScope()))
                .build();
        userRepository.save(user);
        String token = tokenProvider.generateToken(user.getEmail());
        return new TokenResponse(token, toUserDTO(user));
    }

    private AdminScope parseAdminScope(String raw) {
        if (raw == null || raw.isBlank()) return AdminScope.FULL;
        try {
            return AdminScope.valueOf(raw.trim().toUpperCase());
        } catch (Exception e) {
            return AdminScope.FULL;
        }
    }

    private void ensureAdminAccess(User user) {
        if (user == null || user.getId() == null) return;
        if (user.getRole() == UserRole.ADMIN) return;
        boolean isPartnerAdmin = !partnerAdminRepository.findAllByUserId(user.getId()).isEmpty();
        if (!isPartnerAdmin) return;
        user.setRole(UserRole.ADMIN);
        if (user.getAdminScope() == null) {
            user.setAdminScope(AdminScope.PARTNER);
        }
        userRepository.save(user);
    }

    private UserDTO toUserDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .avatarUrl(user.getAvatarUrl())
                .trustScore(user.getTrustScore())
                .vouchCount(user.getVouchCount())
                .verificationStatus(user.getVerificationStatus())
                .location(LocationDTO.builder()
                        .x(user.getLocation() != null ? user.getLocation().getLat() : null)
                        .y(user.getLocation() != null ? user.getLocation().getLng() : null)
                        .build())
                .joinedDate(user.getJoinedDate() != null ? user.getJoinedDate().toLocalDate().toString() : null)
                .phone(user.getPhone())
                .address(user.getAddress())
                .twoFactorEnabled(Boolean.TRUE.equals(user.getTwoFactorEnabled()))
                .profileVisible(user.getProfileVisible())
                .showRatings(user.getShowRatings())
                .adminScope(user.getAdminScope())
                .build();
    }
}
