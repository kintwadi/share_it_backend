package com.vicinity24.api.service;

import com.vicinity24.api.model.Device;
import com.vicinity24.api.model.User;
import com.vicinity24.api.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeviceService {
    private final DeviceRepository deviceRepository;

    public void trackDevice(User user, String userAgent, String ipAddress, boolean trusted) {
        List<Device> devices = deviceRepository.findByUserAndUserAgent(user, userAgent);
        
        if (devices.isEmpty()) {
            // New device
            Device device = Device.builder()
                .id(UUID.randomUUID())
                .user(user)
                .userAgent(userAgent)
                .name(parseDeviceName(userAgent))
                .ipAddress(ipAddress)
                .lastActive(LocalDateTime.now())
                .isTrusted(trusted)
                .build();
            deviceRepository.save(device);
        } else {
            // Update existing device
            Device device = devices.get(0);
            device.setIpAddress(ipAddress);
            device.setLastActive(LocalDateTime.now());
            if (trusted) {
                device.setTrusted(true);
            }
            deviceRepository.save(device);
        }
    }

    private String parseDeviceName(String userAgent) {
        if (userAgent == null) return "Unknown Device";
        if (userAgent.contains("Postman")) return "Postman Runtime";
        if (userAgent.contains("Chrome")) return "Chrome Browser";
        if (userAgent.contains("Firefox")) return "Firefox Browser";
        if (userAgent.contains("Safari")) return "Safari Browser";
        if (userAgent.contains("Edge")) return "Edge Browser";
        return "Unknown Browser";
    }
    
    public List<Device> getUserDevices(User user) {
        return deviceRepository.findByUser(user);
    }
    
    public void deleteDevice(UUID id, User user) {
        deviceRepository.findById(id).ifPresent(device -> {
            if (device.getUser().getId().equals(user.getId())) {
                deviceRepository.delete(device);
            }
        });
    }
}
