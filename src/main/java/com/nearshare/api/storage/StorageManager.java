package com.nearshare.api.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;

import java.time.Duration;

@Service
public class StorageManager implements StorageProvider {
    private final StorageProvider delegate;
    private final Logger log = LoggerFactory.getLogger(StorageManager.class);

    public StorageManager(
            @Value("${application.storage.type}") String storageType,
            @Value("${aws.region:eu-central-1}") String awsRegion,
            @Value("${aws.s3.bucket:}") String s3Bucket,
            @Value("${aws.accessKeyId:}") String s3AccessKey,
            @Value("${aws.secretAccessKey:}") String s3SecretKey,
            @Value("${aws.s3.publicRead:true}") boolean s3PublicRead,
            @Value("${r2.endpoint:}") String r2Endpoint,
            @Value("${r2.bucket.name:}") String r2Bucket,
            @Value("${r2.access.key.id:}") String r2AccessKey,
            @Value("${r2.secret.access.key:}") String r2SecretKey,
            @Value("${r2.public.url:}") String r2PublicUrl
    ) {
        String mode = storageType == null ? "S3" : storageType.trim().split("\\s+")[0];
        System.out.println("Storage mode: " + mode);
        if ("R2".equalsIgnoreCase(mode)) {
            this.delegate = new R2Storage(r2Endpoint, r2Bucket, r2AccessKey, r2SecretKey, r2PublicUrl);
            log.info("StorageManager initialized with R2 endpoint={}, bucket={}, publicUrl={}", r2Endpoint, r2Bucket, r2PublicUrl);
            System.out.println("StorageManager initialized with R2 endpoint={}, bucket={}, publicUrl={}".formatted(r2Endpoint, r2Bucket, r2PublicUrl));
        } else {
            this.delegate = new S2Storage(Region.of(awsRegion), s3Bucket, s3PublicRead, s3AccessKey, s3SecretKey);
            log.info("StorageManager initialized with S3 region={}, bucket={}, publicRead={}", awsRegion, s3Bucket, s3PublicRead);
    
            System.out.println("StorageManager initialized with S3 region={}, bucket={}, publicRead={}".formatted(awsRegion, s3Bucket, s3PublicRead));
        }
    }

    @Override
    public String uploadBytes(String key, byte[] bytes, String contentType) {
        return delegate.uploadBytes(key, bytes, contentType);
    }

    @Override
    public String presignPutUrl(String key, String contentType, Duration ttl) {
        return delegate.presignPutUrl(key, contentType, ttl);
    }

    @Override
    public String presignGetUrl(String key, Duration ttl) {
        return delegate.presignGetUrl(key, ttl);
    }

    @Override
    public String objectUrl(String key) {
        return delegate.objectUrl(key);
    }
}
