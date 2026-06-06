package com.vicinity24.api.controller;

import com.vicinity24.api.dto.StorageDTOs;
import com.vicinity24.api.model.User;
import com.vicinity24.api.config.RuntimeSettingsService;
import com.vicinity24.api.storage.StorageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.vicinity24.api.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/storage")
public class StorageController {
    private final StorageManager storageManager;
    private final UserService userService;
    private final RuntimeSettingsService runtimeSettingsService;
    private final Logger log = LoggerFactory.getLogger(StorageController.class);

    public StorageController(StorageManager storageManager, UserService userService, RuntimeSettingsService runtimeSettingsService) {
        this.storageManager = storageManager;
        this.userService = userService;
        this.runtimeSettingsService = runtimeSettingsService;
    }

    private void validateUpload(String filename, String contentType, long byteSize) {
        int maxMb = runtimeSettingsService.getInt("image.max.size.mb", 5);
        long maxBytes = Math.max(1L, (long) maxMb) * 1024L * 1024L;
        if (byteSize > maxBytes) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "file_too_large");
        }

        String allowed = runtimeSettingsService.getString("allowed.image.types", "jpg,jpeg,png,gif,webp");
        final String ext = extractExt(filename);
        boolean extAllowed = Arrays.stream(String.valueOf(allowed).split(","))
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .anyMatch(s -> s.equals(ext));
        if (!extAllowed) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "file_type_not_allowed");
        }

        String ct = String.valueOf(contentType == null ? "" : contentType).toLowerCase(Locale.ROOT).trim();
        if (!ct.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "file_type_not_allowed");
        }
    }

    private static String extractExt(String filename) {
        if (filename == null) return "";
        String f = filename.trim();
        if (f.isEmpty()) return "";
        int dot = f.lastIndexOf('.');
        if (dot < 0 || dot >= f.length() - 1) return "";
        return f.substring(dot + 1).trim().toLowerCase(Locale.ROOT);
    }

    @PostMapping("/presign-upload")
    public ResponseEntity<StorageDTOs.PresignUploadResponse> presignUpload(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal, @RequestBody StorageDTOs.PresignUploadRequest req) {
        User u = userService.getByEmail(principal.getUsername());
        validateUpload(req != null ? req.getFilename() : null, req != null ? req.getContentType() : null, 0);
        String key = u.getId() + "/" + UUID.randomUUID() + "/" + req.getFilename();
        String uploadUrl = storageManager.presignPutUrl(key, req.getContentType(), Duration.ofMinutes(15));
        String objectUrl = storageManager.objectUrl(key);
        log.info("Presigned upload for user={} key={} objectUrl={}", u.getId(), key, objectUrl);
        return ResponseEntity.ok(new StorageDTOs.PresignUploadResponse(key, uploadUrl, objectUrl));
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> upload(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal, @RequestParam("file") MultipartFile file) throws Exception {
        User u = userService.getByEmail(principal.getUsername());
        validateUpload(file != null ? file.getOriginalFilename() : null, file != null ? file.getContentType() : null, file != null ? file.getSize() : 0);
        String key = u.getId() + "/" + UUID.randomUUID() + "/" + file.getOriginalFilename();
        String url = storageManager.uploadBytes(key, file.getBytes(), file.getContentType());
        log.info("Uploaded file for user={} key={} url={}", u.getId(), key, url);
        return ResponseEntity.ok(Map.of("key", key, "url", url));
    }

    @GetMapping("/url/{key}")
    public ResponseEntity<Map<String, String>> presignGet(@PathVariable("key") String key) {
        String url = storageManager.presignGetUrl(key, Duration.ofMinutes(15));
        return ResponseEntity.ok(Map.of("url", url));
    }
}
