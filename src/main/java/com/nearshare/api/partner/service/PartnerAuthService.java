package com.nearshare.api.partner.service;

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
import com.nearshare.api.partner.dto.PartnerAuthRegisterRequest;
import com.nearshare.api.partner.dto.PartnerRegistrationRequest;
import com.nearshare.api.partner.model.Partner;
import com.nearshare.api.partner.model.PartnerAdmin;
import com.nearshare.api.partner.model.PartnerAdminRole;
import com.nearshare.api.partner.model.PartnerStatus;
import com.nearshare.api.partner.repository.PartnerAdminRepository;
import com.nearshare.api.partner.repository.PartnerRepository;
import com.nearshare.api.repository.UserRepository;
import com.nearshare.api.security.JwtTokenProvider;
import com.nearshare.api.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PartnerAuthService {
    private final UserRepository userRepository;
    private final PartnerRepository partnerRepository;
    private final PartnerAdminRepository partnerAdminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthService authService;

    public PartnerAuthService(
            UserRepository userRepository,
            PartnerRepository partnerRepository,
            PartnerAdminRepository partnerAdminRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider,
            AuthService authService
    ) {
        this.userRepository = userRepository;
        this.partnerRepository = partnerRepository;
        this.partnerAdminRepository = partnerAdminRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.authService = authService;
    }

    public TokenResponse login(LoginRequest request, String userAgent, String ipAddress) {
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> new RuntimeException("user_not_found"));
        if (partnerAdminRepository.findAllByUserId(user.getId()).isEmpty()) {
            throw new RuntimeException("forbidden");
        }
        ensurePartnerAdminRole(user);
        return authService.login(request, userAgent, ipAddress);
    }

    public TokenResponse verify2faLogin(String email, String code, String userAgent, String ipAddress) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("user_not_found"));
        if (partnerAdminRepository.findAllByUserId(user.getId()).isEmpty()) {
            throw new RuntimeException("forbidden");
        }
        ensurePartnerAdminRole(user);
        return authService.verify2faLogin(email, code, userAgent, ipAddress);
    }

    public TokenResponse register(PartnerAuthRegisterRequest request) {
        if (request == null) {
            throw new RuntimeException("invalid_request");
        }
        PartnerRegistrationRequest partnerReq = request.getPartner();
        if (partnerReq == null || partnerReq.getName() == null || partnerReq.getName().isBlank()) {
            throw new RuntimeException("partner_name_required");
        }
        String email = request.getUserEmail();
        if (email == null || email.isBlank()) {
            email = partnerReq.getEmail();
        }
        if (email == null || email.isBlank()) {
            throw new RuntimeException("email_required");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("email_exists");
        }
        String rawPassword = request.getUserPassword();
        if (rawPassword == null || rawPassword.isBlank()) {
            rawPassword = request.getPartnerPassword();
        }
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new RuntimeException("password_required");
        }
        String userName = request.getUserName();
        if (userName == null || userName.isBlank()) {
            userName = partnerReq.getContactPerson();
        }
        if (userName == null || userName.isBlank()) {
            userName = partnerReq.getName();
        }

        User user = User.builder()
                .id(UUID.randomUUID())
                .name(userName != null ? userName : "")
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .phone("")
                .address("")
                .avatarUrl("")
                .trustScore(50)
                .vouchCount(0)
                .verificationStatus(VerificationStatus.UNVERIFIED)
                .location(Location.builder().lat(0.0).lng(0.0).build())
                .joinedDate(LocalDateTime.now())
                .status(UserStatus.ACTIVE)
                .role(UserRole.MEMBER)
                .build();
        userRepository.save(user);

        Partner partner = Partner.builder()
                .id(UUID.randomUUID())
                .name(partnerReq.getName())
                .email(partnerReq.getEmail())
                .phone(partnerReq.getPhone())
                .address(partnerReq.getAddress())
                .city(partnerReq.getCity())
                .contactPerson(partnerReq.getContactPerson())
                .status(PartnerStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        partnerRepository.save(partner);

        PartnerAdmin admin = PartnerAdmin.builder()
                .id(UUID.randomUUID())
                .partner(partner)
                .user(user)
                .role(PartnerAdminRole.ADMIN)
                .createdAt(LocalDateTime.now())
                .build();
        partnerAdminRepository.save(admin);

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

    private void ensurePartnerAdminRole(User user) {
        if (user == null) return;
        boolean changed = false;
        if (user.getRole() != UserRole.ADMIN) {
            user.setRole(UserRole.ADMIN);
            changed = true;
        }
        if (user.getAdminScope() == null) {
            user.setAdminScope(AdminScope.PARTNER);
            changed = true;
        }
        if (changed) {
            userRepository.save(user);
        }
    }
}
