package com.vicinity24.api.repository;

import com.vicinity24.api.model.Device;
import com.vicinity24.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface DeviceRepository extends JpaRepository<Device, UUID> {
    List<Device> findByUser(User user);
    void deleteByUser(User user);
    // Find by user and user agent (simplified device tracking)
    List<Device> findByUserAndUserAgent(User user, String userAgent);
}
