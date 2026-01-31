package com.nearshare.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.time.Duration;

@Service
public class S3Service {
    private final S3Client s3Client;
    private final S3Presigner presigner;
    private final String bucket;
    private final boolean publicRead;

    public S3Service(S3Client s3Client, S3Presigner presigner, @Value("${aws.s3.bucket}") String bucket, @Value("${aws.s3.publicRead:true}") boolean publicRead) {
        this.s3Client = s3Client;
        this.presigner = presigner;
        this.bucket = bucket;
        this.publicRead = publicRead;
    }

    public String uploadBytes(String key, byte[] bytes, String contentType) {
        PutObjectRequest.Builder builder = PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType);
        if (publicRead) builder.acl(ObjectCannedACL.PUBLIC_READ);
        PutObjectRequest req = builder.build();
        s3Client.putObject(req, RequestBody.fromBytes(bytes));
        return objectUrl(key);
    }

    public String presignPutUrl(String key, String contentType, Duration ttl) {
        PutObjectRequest put = PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).build();
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder().signatureDuration(ttl).putObjectRequest(put).build();
        return presigner.presignPutObject(presignRequest).url().toString();
    }

    public String presignGetUrl(String key, Duration ttl) {
        GetObjectRequest get = GetObjectRequest.builder().bucket(bucket).key(key).build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder().signatureDuration(ttl).getObjectRequest(get).build();
        return presigner.presignGetObject(presignRequest).url().toString();
    }

    public String objectUrl(String key) {
        String regionName = s3Client.serviceClientConfiguration().region().id();
        return URI.create("https://" + bucket + ".s3." + regionName + ".amazonaws.com/" + key).toString();
    }
}