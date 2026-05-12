package com.nearshare.api.controller;

import com.nearshare.api.dto.StorageDTOs;
import com.nearshare.api.model.User;
import com.nearshare.api.storage.StorageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.nearshare.api.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/storage")
public class StorageController {
    private final StorageManager storageManager;
    private final UserService userService;
    private final Logger log = LoggerFactory.getLogger(StorageController.class);

    public StorageController(StorageManager storageManager, UserService userService) {
        this.storageManager = storageManager;
        this.userService = userService;
    }

    @PostMapping("/presign-upload")
    public ResponseEntity<StorageDTOs.PresignUploadResponse> presignUpload(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal, @RequestBody StorageDTOs.PresignUploadRequest req) {
        User u = userService.getByEmail(principal.getUsername());
        String key = u.getId() + "/" + UUID.randomUUID() + "/" + req.getFilename();
        String uploadUrl = storageManager.presignPutUrl(key, req.getContentType(), Duration.ofMinutes(15));
        String objectUrl = storageManager.objectUrl(key);
        log.info("Presigned upload for user={} key={} objectUrl={}", u.getId(), key, objectUrl);
        return ResponseEntity.ok(new StorageDTOs.PresignUploadResponse(key, uploadUrl, objectUrl));
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> upload(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal, @RequestParam("file") MultipartFile file) throws Exception {
        User u = userService.getByEmail(principal.getUsername());
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
