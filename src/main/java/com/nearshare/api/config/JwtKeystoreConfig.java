package com.nearshare.api.config;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.util.Base64;

@Configuration
public class JwtKeystoreConfig {
    private static final Logger log = LoggerFactory.getLogger(JwtKeystoreConfig.class);
    private final ResourceLoader resourceLoader;

    public JwtKeystoreConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Bean(name = "accessTokenKey")
    public Key accessTokenKey(
            @Value("${security.jwt.secret:}") String jwtSecret,
            @Value("${security.jwt.keystore.remote-location:}") String remoteKeyStoreLocation,
            @Value("${security.jwt.keystore.location:}") String keyStoreLocation,
            @Value("${security.jwt.keystore.password:}") String keyStorePassword,
            @Value("${security.jwt.keystore.type:PKCS12}") String keyStoreType,
            @Value("${security.jwt.access-token.alias:}") String alias,
            @Value("${security.jwt.access-token.password:}") String password
    ) {
        return resolveKey(jwtSecret, "access", remoteKeyStoreLocation, keyStoreLocation, keyStorePassword, keyStoreType, alias, password);
    }

    @Bean(name = "refreshTokenKey")
    public Key refreshTokenKey(
            @Value("${security.jwt.secret:}") String jwtSecret,
            @Value("${security.jwt.keystore.remote-location:}") String remoteKeyStoreLocation,
            @Value("${security.jwt.keystore.location:}") String keyStoreLocation,
            @Value("${security.jwt.keystore.password:}") String keyStorePassword,
            @Value("${security.jwt.keystore.type:PKCS12}") String keyStoreType,
            @Value("${security.jwt.refresh-token.alias:}") String alias,
            @Value("${security.jwt.refresh-token.password:}") String password
    ) {
        return resolveKey(jwtSecret, "refresh", remoteKeyStoreLocation, keyStoreLocation, keyStorePassword, keyStoreType, alias, password);
    }

    private Key resolveKey(String jwtSecret, String purpose, String remoteKeyStoreLocation, String keyStoreLocation, String keyStorePassword, String keyStoreType, String alias, String password) {
        if (!isBlank(keyStoreLocation)) {
            try {
                return loadOrGenerateKey(keyStoreLocation, keyStorePassword, keyStoreType, alias, password);
            } catch (IllegalStateException localEx) {
                if (!isBlank(remoteKeyStoreLocation)) {
                    try {
                        return loadOrGenerateKey(remoteKeyStoreLocation, keyStorePassword, keyStoreType, alias, password);
                    } catch (IllegalStateException remoteEx) {
                        localEx.addSuppressed(remoteEx);
                    }
                }
                throw localEx;
            }
        }
        if (!isBlank(remoteKeyStoreLocation)) {
            return loadOrGenerateKey(remoteKeyStoreLocation, keyStorePassword, keyStoreType, alias, password);
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
            String normalizedLocation = normalizeLocation(keyStoreLocation);
            log.info("jwt_keystore_loading location={} type={} alias={}", normalizedLocation, keyStoreType, alias);
            Resource resource = resourceLoader.getResource(normalizedLocation);
            if (!resource.exists()) {
                throw new IllegalStateException("jwt_keystore_not_found location=" + normalizedLocation);
            }
            String effectiveType = isBlank(keyStoreType) ? "PKCS12" : keyStoreType.trim();
            byte[] bytes;
            try (InputStream in = resource.getInputStream()) {
                bytes = in.readAllBytes();
            }
            log.info("jwt_keystore_read_bytes location={} bytes={}", normalizedLocation, bytes.length);
            KeyStore ks = KeyStore.getInstance(effectiveType);
            try {
                loadKeyStore(ks, bytes, keyStorePassword);
            } catch (Exception first) {
                String altType = null;
                if ("PKCS12".equalsIgnoreCase(effectiveType)) altType = "JKS";
                else if ("JKS".equalsIgnoreCase(effectiveType)) altType = "PKCS12";
                if (altType != null) {
                    log.warn("jwt_keystore_load_failed_trying_fallback_type location={} type={} altType={} error={}", normalizedLocation, effectiveType, altType, first.getClass().getSimpleName());
                    try {
                        KeyStore alt = KeyStore.getInstance(altType);
                        loadKeyStore(alt, bytes, keyStorePassword);
                        ks = alt;
                    } catch (Exception second) {
                        first.addSuppressed(second);
                        throw first;
                    }
                } else {
                    throw first;
                }
            }
            Key key = ks.getKey(alias, password.toCharArray());
            if (key == null) {
                throw new IllegalStateException("jwt_keystore_key_not_found location=" + normalizedLocation + " alias=" + alias);
            }
            log.info("jwt_keystore_key_loaded location={} type={} alias={}", normalizedLocation, ks.getType(), alias);
            return key;
        } catch (Exception e) {
            throw new IllegalStateException("jwt_keystore_key_load_failed", e);
        }
    }

    private void loadKeyStore(KeyStore ks, byte[] bytes, String keyStorePassword) throws Exception {
        try (ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
            ks.load(in, keyStorePassword.toCharArray());
            return;
        } catch (Exception first) {
            String s = new String(bytes, StandardCharsets.UTF_8).trim();
            if (!s.isEmpty() && looksLikeBase64(s)) {
                byte[] decoded = Base64.getDecoder().decode(s);
                try (ByteArrayInputStream in2 = new ByteArrayInputStream(decoded)) {
                    ks.load(in2, keyStorePassword.toCharArray());
                    return;
                } catch (Exception second) {
                    first.addSuppressed(second);
                }
            }
            String cleaned = s.replaceAll("[^A-Za-z0-9+/=]", "");
            if (!cleaned.isEmpty() && cleaned.length() != s.length()) {
                try {
                    byte[] decoded = Base64.getDecoder().decode(cleaned);
                    try (ByteArrayInputStream in3 = new ByteArrayInputStream(decoded)) {
                        ks.load(in3, keyStorePassword.toCharArray());
                        return;
                    }
                } catch (Exception third) {
                    first.addSuppressed(third);
                }
            }
            throw first;
        }
    }

    private boolean looksLikeBase64(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '+' || c == '/' || c == '=' || c == '\n' || c == '\r') {
                continue;
            }
            return false;
        }
        return true;
    }

    private String normalizeLocation(String rawLocation) {
        String location = rawLocation == null ? "" : rawLocation.trim();
        if (location.isEmpty()) return location;
        if (location.startsWith("classpath:") || location.startsWith("file:")) return location;
        if (location.startsWith("/")) return "file:" + location;
        if (location.length() >= 3 && Character.isLetter(location.charAt(0)) && location.charAt(1) == ':' && (location.charAt(2) == '\\' || location.charAt(2) == '/')) {
            return "file:" + location.replace('\\', '/');
        }
        return location;
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
