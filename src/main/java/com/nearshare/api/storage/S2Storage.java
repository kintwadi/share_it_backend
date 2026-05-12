package com.nearshare.api.storage;

import org.springframework.beans.factory.annotation.Value;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

public class S2Storage implements StorageProvider {
    private final S3Client s3Client;
    private final S3Presigner presigner;
    private final String bucket;
    private final boolean publicRead;
    private final Region region;

    public S2Storage(Region region, String bucket, boolean publicRead, String accessKeyId, String secretAccessKey) {
        this.region = region;
        this.bucket = bucket;
        this.publicRead = publicRead;
        var provider = (accessKeyId != null && !accessKeyId.isBlank() && secretAccessKey != null && !secretAccessKey.isBlank())
                ? StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey))
                : DefaultCredentialsProvider.create();
        this.s3Client = S3Client.builder().region(region).credentialsProvider(provider).build();
        this.presigner = S3Presigner.builder().region(region).credentialsProvider(provider).build();
    }

    @Override
    public String uploadBytes(String key, byte[] bytes, String contentType) {
        PutObjectRequest.Builder builder = PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType);
        if (publicRead) builder.acl(ObjectCannedACL.PUBLIC_READ);
        s3Client.putObject(builder.build(), RequestBody.fromBytes(bytes));
        return objectUrl(key);
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
        String regionName = region.id();
        return "https://" + bucket + ".s3." + regionName + ".amazonaws.com/" + key;
    }
}

