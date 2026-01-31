package com.nearshare.api.storage;

public interface StorageProvider {
    String uploadBytes(String key, byte[] bytes, String contentType);
    String presignPutUrl(String key, String contentType, java.time.Duration ttl);
    String presignGetUrl(String key, java.time.Duration ttl);
    String objectUrl(String key);
}

