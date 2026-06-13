package com.vicinity24.api.core.controller;

import com.vicinity24.api.core.service.UserService;
import com.vicinity24.api.core.model.Device;
import com.vicinity24.api.core.model.User;
import com.vicinity24.api.core.service.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {
    private final DeviceService deviceService;

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<Device>> getDevices(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal, jakarta.servlet.http.HttpServletRequest httpRequest) {
        User user = userService.getByEmail(principal.getUsername());
        
        // Ensure current device is tracked and trusted
        deviceService.trackDevice(user, httpRequest.getHeader("User-Agent"), httpRequest.getRemoteAddr(), true);
        
        return ResponseEntity.ok(deviceService.getUserDevices(user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revokeDevice(@PathVariable UUID id, @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        User user = userService.getByEmail(principal.getUsername());
        deviceService.deleteDevice(id, user);
        return ResponseEntity.ok().build();
    }
}
