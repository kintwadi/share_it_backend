package com.nearshare.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class StorageDTOs {
    @Getter
    @lombok.Setter
    public static class PresignUploadRequest {
        private String filename;
        private String contentType;
    }

    @Getter
    @AllArgsConstructor
    public static class PresignUploadResponse {
        private String key;
        private String uploadUrl;
        private String objectUrl;
    }
}