package com.nearshare.api.storage;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.utils.AttributeMap;

import java.net.URI;

public class R2Storage implements StorageProvider {
    private final S3Client s3Client;
    private final S3Presigner presigner;
    private final String bucket;
    private final String publicBaseUrl;
    private final String endpoint;

    public R2Storage(String endpoint, String bucket, String accessKeyId, String secretAccessKey, String publicBaseUrl) {
        this.bucket = bucket;
        this.publicBaseUrl = publicBaseUrl;
        this.endpoint = endpoint;
        var creds = StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey));
        var s3conf = S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build();
        this.s3Client = S3Client.builder()
                .region(Region.of("auto"))
                .serviceConfiguration(s3conf)
                .credentialsProvider(creds)
                .endpointOverride(URI.create(endpoint))
                .build();
        this.presigner = S3Presigner.builder()
                .region(Region.of("auto"))
                .credentialsProvider(creds)
                .endpointOverride(URI.create(endpoint))
                .build();
    }

    @Override
    public String uploadBytes(String key, byte[] bytes, String contentType) {
        PutObjectRequest put = PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).build();
        try {
            s3Client.putObject(put, RequestBody.fromBytes(bytes));
            return objectUrl(key);
        } catch (Exception e) {
            throw new RuntimeException("R2 upload failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String presignPutUrl(String key, String contentType, java.time.Duration ttl) {
        PutObjectRequest put = PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).build();
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder().signatureDuration(ttl).putObjectRequest(put).build();
        return presigner.presignPutObject(presignRequest).url().toString();
    }

    @Override
    public String presignGetUrl(String key, java.time.Duration ttl) {
        GetObjectRequest get = GetObjectRequest.builder().bucket(bucket).key(key).build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder().signatureDuration(ttl).getObjectRequest(get).build();
        return presigner.presignGetObject(presignRequest).url().toString();
    }

    @Override
    public String objectUrl(String key) {
        if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
            String base = publicBaseUrl.endsWith("/") ? publicBaseUrl : (publicBaseUrl + "/");
            // If using Cloudflare r2.dev bucket domain, the bucket is already mapped; do not include bucket in path
            if (isBucketDomain(publicBaseUrl)) {
                return base + key;
            }
            return base + bucket + "/" + key;
        }
        // Fallback to path-style URL via endpoint: {endpoint}/{bucket}/{key}
        return (key.startsWith("/") ? (endpointBase() + bucket + key) : (endpointBase() + bucket + "/" + key));
    }

    private String endpointBase() {
        if (endpoint == null || endpoint.isBlank()) return "";
        return endpoint.endsWith("/") ? endpoint : (endpoint + "/");
    }

    private boolean isBucketDomain(String url) {
        String u = url.toLowerCase();
        return u.contains(".r2.dev");
    }
}

