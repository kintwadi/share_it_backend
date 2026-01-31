package com.nearshare.api.service;

import com.nearshare.api.dto.UpdateProfileRequest;
import com.nearshare.api.dto.UserDTO;
import com.nearshare.api.dto.UserSummaryDTO;
import com.nearshare.api.dto.LocationDTO;
import com.nearshare.api.model.User;
import com.nearshare.api.model.enums.UserRole;
import com.nearshare.api.model.enums.UserStatus;
import com.nearshare.api.model.enums.VerificationStatus;
import com.nearshare.api.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User getByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("user_not_found"));
    }

    public User getById(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new RuntimeException("user_not_found"));
    }

    public UserDTO me(User user) {
        return toDTO(user);
    }

    public UserDTO updateProfile(User user, UpdateProfileRequest request) {
        if (request.getName() != null) user.setName(request.getName());
        if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getAddress() != null) user.setAddress(request.getAddress());
        userRepository.save(user);
        return toDTO(user);
    }

    public UserDTO updateAvatar(User user, String avatarUrl) {
        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);
        return toDTO(user);
    }

    public void changePassword(User user, String oldPassword, String newPassword) {
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("invalid_old_password");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public List<UserDTO> allUsers() {
        return userRepository.findAll().stream().map(this::toDTO).toList();
    }

    public List<UserSummaryDTO> contacts(User current) {
        return userRepository.findAll().stream()
                .filter(u -> !u.getId().equals(current.getId()))
                .map(u -> UserSummaryDTO.builder().id(u.getId()).name(u.getName()).trustScore(u.getTrustScore()).avatarUrl(u.getAvatarUrl()).build())
                .toList();
    }

    public UserDTO vouch(UUID id) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("user_not_found"));
        user.setVouchCount(user.getVouchCount() + 1);
        user.setTrustScore(Math.min(100, user.getTrustScore() + 1));
        userRepository.save(user);
        return toDTO(user);
    }

    public UserDTO verificationRequest(User user, String phone, String address) {
        user.setVerificationStatus(VerificationStatus.PENDING);
        user.setPhone(phone);
        user.setAddress(address);
        userRepository.save(user);
        return toDTO(user);
    }

    public UserDTO approveVerification(UUID id) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("user_not_found"));
        user.setVerificationStatus(VerificationStatus.VERIFIED);
        user.setTrustScore(Math.min(100, user.getTrustScore() + 5));
        userRepository.save(user);
        return toDTO(user);
    }

    public UserDTO revokeVerification(UUID id) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("user_not_found"));
        user.setVerificationStatus(VerificationStatus.UNVERIFIED);
        userRepository.save(user);
        return toDTO(user);
    }

    public UserDTO setStatus(UUID id, UserStatus status) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("user_not_found"));
        user.setStatus(status);
        userRepository.save(user);
        return toDTO(user);
    }

    private UserDTO toDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole() != null ? user.getRole() : UserRole.MEMBER)
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