package com.nearshare.api.service;

import com.nearshare.api.dto.LocationDTO;
import com.nearshare.api.dto.LoginRequest;
import com.nearshare.api.dto.RegisterRequest;
import com.nearshare.api.dto.TokenResponse;
import com.nearshare.api.dto.UserDTO;
import com.nearshare.api.model.User;
import com.nearshare.api.model.embeddable.Location;
import com.nearshare.api.model.enums.UserRole;
import com.nearshare.api.model.enums.UserStatus;
import com.nearshare.api.model.enums.VerificationStatus;
import com.nearshare.api.repository.UserRepository;
import com.nearshare.api.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final boolean allowAdminToggle;

    private final TwoFactorService twoFactorService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider tokenProvider, @Value("${nearshare.allowAdminToggle:false}") boolean allowAdminToggle, TwoFactorService twoFactorService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.allowAdminToggle = allowAdminToggle;
        this.twoFactorService = twoFactorService;
    }

    public TokenResponse verify2faLogin(String email, String code) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("user_not_found"));
        if (!twoFactorService.verify(user.getTwoFactorSecret(), code)) {
            throw new RuntimeException("invalid_code");
        }
        String token = tokenProvider.generateToken(user.getEmail());
        return new TokenResponse(token, toUserDTO(user));
    }

    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> new RuntimeException("invalid_credentials"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) throw new RuntimeException("invalid_credentials");
        
        if (Boolean.TRUE.equals(user.getTwoFactorEnabled())) {
            String token = tokenProvider.generateToken(user.getEmail(), true); // Generate pre-auth token
            return new TokenResponse(token, null, true);
        }

        String token = tokenProvider.generateToken(user.getEmail());
        return new TokenResponse(token, toUserDTO(user));
    }

    public UserDTO register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) throw new IllegalArgumentException("email_exists");
        UserRole role = (Boolean.TRUE.equals(request.getIsAdmin()) && allowAdminToggle) ? UserRole.ADMIN : UserRole.MEMBER;
        User user = User.builder().id(UUID.randomUUID()).name(request.getName()).email(request.getEmail()).password(passwordEncoder.encode(request.getPassword())).phone(request.getPhone()).address(request.getAddress()).avatarUrl(request.getAvatarUrl()).trustScore(50).vouchCount(0).verificationStatus(VerificationStatus.UNVERIFIED).location(Location.builder().lat(request.getLat()).lng(request.getLng()).build()).joinedDate(LocalDateTime.now()).status(UserStatus.ACTIVE).role(role).build();
        userRepository.save(user);
        return toUserDTO(user);
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
                .location(LocationDTO.builder().x(user.getLocation() != null ? user.getLocation().getLat() : null).y(user.getLocation() != null ? user.getLocation().getLng() : null).build())
                .joinedDate(user.getJoinedDate() != null ? user.getJoinedDate().toLocalDate().toString() : null)
                .phone(user.getPhone())
                .address(user.getAddress())
                .twoFactorEnabled(Boolean.TRUE.equals(user.getTwoFactorEnabled()))
                .build();
    }
}