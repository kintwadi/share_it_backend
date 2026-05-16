package com.nearshare.api.config;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.util.Base64;

@Configuration
public class JwtKeystoreConfig {
    private final ResourceLoader resourceLoader;

    public JwtKeystoreConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Bean(name = "accessTokenKey")
    public Key accessTokenKey(
            @Value("${security.jwt.secret:}") String jwtSecret,
            @Value("${security.jwt.keystore.location:}") String keyStoreLocation,
            @Value("${security.jwt.keystore.password:}") String keyStorePassword,
            @Value("${security.jwt.keystore.type:PKCS12}") String keyStoreType,
            @Value("${security.jwt.access-token.alias:}") String alias,
            @Value("${security.jwt.access-token.password:}") String password
    ) {
        return resolveKey(jwtSecret, "access", keyStoreLocation, keyStorePassword, keyStoreType, alias, password);
    }

    @Bean(name = "refreshTokenKey")
    public Key refreshTokenKey(
            @Value("${security.jwt.secret:}") String jwtSecret,
            @Value("${security.jwt.keystore.location:}") String keyStoreLocation,
            @Value("${security.jwt.keystore.password:}") String keyStorePassword,
            @Value("${security.jwt.keystore.type:PKCS12}") String keyStoreType,
            @Value("${security.jwt.refresh-token.alias:}") String alias,
            @Value("${security.jwt.refresh-token.password:}") String password
    ) {
        return resolveKey(jwtSecret, "refresh", keyStoreLocation, keyStorePassword, keyStoreType, alias, password);
    }

    private Key resolveKey(String jwtSecret, String purpose, String keyStoreLocation, String keyStorePassword, String keyStoreType, String alias, String password) {
        if (!isBlank(keyStoreLocation)) {
            return loadOrGenerateKey(keyStoreLocation, keyStorePassword, keyStoreType, alias, password);
        }
        if (!isBlank(jwtSecret)) {
            return keyFromSecret(jwtSecret, purpose);
        }
        return Keys.secretKeyFor(SignatureAlgorithm.HS256);
    }

    private Key loadOrGenerateKey(String keyStoreLocation, String keyStorePassword, String keyStoreType, String alias, String password) {
        if (isBlank(keyStoreLocation) || isBlank(keyStorePassword) || isBlank(alias) || isBlank(password)) {
            return Keys.secretKeyFor(SignatureAlgorithm.HS256);
        }
        try {
            Resource resource = resourceLoader.getResource(keyStoreLocation);
            if (!resource.exists()) {
                throw new IllegalStateException("jwt_keystore_not_found");
            }
            String effectiveType = isBlank(keyStoreType) ? "PKCS12" : keyStoreType.trim();
            KeyStore ks = KeyStore.getInstance(effectiveType);
            try (InputStream in = resource.getInputStream()) {
                ks.load(in, keyStorePassword.toCharArray());
            }
            Key key = ks.getKey(alias, password.toCharArray());
            if (key == null) {
                throw new IllegalStateException("jwt_keystore_key_not_found");
            }
            return key;
        } catch (Exception e) {
            throw new IllegalStateException("jwt_keystore_key_load_failed", e);
        }
    }

    private Key keyFromSecret(String secret, String purpose) {
        String material = secret + ":" + purpose;
        byte[] raw = decodeMaybeBase64(material);
        byte[] bytes = ensureMin32Bytes(raw);
        return Keys.hmacShaKeyFor(bytes);
    }

    private byte[] decodeMaybeBase64(String input) {
        String s = input.trim();
        if (s.isEmpty()) return new byte[0];
        try {
            return Base64.getDecoder().decode(s);
        } catch (IllegalArgumentException e) {
            return s.getBytes(StandardCharsets.UTF_8);
        }
    }

    private byte[] ensureMin32Bytes(byte[] bytes) {
        if (bytes != null && bytes.length >= 32) return bytes;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(bytes == null ? new byte[0] : bytes);
        } catch (Exception e) {
            return Keys.secretKeyFor(SignatureAlgorithm.HS256).getEncoded();
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
