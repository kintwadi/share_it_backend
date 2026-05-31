package com.nearshare.api.service;

import com.nearshare.api.dto.LocationDTO;
import com.nearshare.api.dto.LoginRequest;
import com.nearshare.api.dto.RegisterRequest;
import com.nearshare.api.dto.RegisterResponse;
import com.nearshare.api.dto.TokenResponse;
import com.nearshare.api.dto.UserDTO;
import com.nearshare.api.config.RuntimeSettingsService;
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
import com.nearshare.api.service.DeviceService;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final RuntimeSettingsService runtimeSettingsService;
    private final EmailVerificationService emailVerificationService;

    private final TwoFactorService twoFactorService;
    private final DeviceService deviceService;

    @Value("${setting.signup.email.verification.required:true}")
    private boolean signupEmailVerificationRequired;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider tokenProvider, RuntimeSettingsService runtimeSettingsService, EmailVerificationService emailVerificationService, TwoFactorService twoFactorService, DeviceService deviceService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.runtimeSettingsService = runtimeSettingsService;
        this.emailVerificationService = emailVerificationService;
        this.twoFactorService = twoFactorService;
        this.deviceService = deviceService;
    }

    public TokenResponse verify2faLogin(String email, String code, String userAgent, String ipAddress) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("user_not_found"));
        if (!twoFactorService.verify(user.getTwoFactorSecret(), code)) {
            throw new RuntimeException("invalid_code");
        }
        deviceService.trackDevice(user, userAgent, ipAddress, true);
        String token = tokenProvider.generateToken(user.getEmail());
        return new TokenResponse(token, toUserDTO(user));
    }

    public TokenResponse login(LoginRequest request, String userAgent, String ipAddress) {
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> new RuntimeException("user_not_found"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) throw new RuntimeException("invalid_credentials");

        if (signupEmailVerificationRequired && !Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new IllegalArgumentException("email_not_verified");
        }
        
        if (Boolean.TRUE.equals(user.getTwoFactorEnabled())) {
            deviceService.trackDevice(user, userAgent, ipAddress, false);
            String token = tokenProvider.generateToken(user.getEmail(), true); // Generate pre-auth token
            return new TokenResponse(token, null, true);
        }

        deviceService.trackDevice(user, userAgent, ipAddress, true);
        String token = tokenProvider.generateToken(user.getEmail());
        return new TokenResponse(token, toUserDTO(user));
    }

    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) throw new IllegalArgumentException("email_exists");
        boolean verifiedOnCreate = !signupEmailVerificationRequired;
        User user = User.builder().id(UUID.randomUUID()).name(request.getName()).email(request.getEmail()).password(passwordEncoder.encode(request.getPassword())).phone(request.getPhone()).address(request.getAddress()).avatarUrl(request.getAvatarUrl()).trustScore(50).vouchCount(0).verificationStatus(VerificationStatus.UNVERIFIED).location(Location.builder().lat(request.getLat()).lng(request.getLng()).build()).joinedDate(LocalDateTime.now()).status(UserStatus.ACTIVE).role(UserRole.MEMBER).emailVerified(verifiedOnCreate).build();
        userRepository.save(user);
        String token = null;
        if (signupEmailVerificationRequired) {
            token = emailVerificationService.startForUser(user, null);
        }
        return RegisterResponse.builder()
                .user(toUserDTO(user))
                .requiresEmailVerification(signupEmailVerificationRequired)
                .verificationToken(token)
                .build();
    }

    public TokenResponse tokenForUser(User user) {
        if (user == null) throw new RuntimeException("user_not_found");
        String token = tokenProvider.generateToken(user.getEmail());
        return new TokenResponse(token, toUserDTO(user));
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
                .profileVisible(user.getProfileVisible())
                .showRatings(user.getShowRatings())
                .adminScope(user.getAdminScope())
                .emailVerified(Boolean.TRUE.equals(user.getEmailVerified()))
                .build();
    }
}
