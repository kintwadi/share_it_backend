package com.nearshare.api.config;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;

@Configuration
public class JwtKeystoreConfig {
    private final ResourceLoader resourceLoader;

    public JwtKeystoreConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Bean(name = "accessTokenKey")
    public Key accessTokenKey(
            @Value("${server.ssl.key-store:}") String keyStoreLocation,
            @Value("${SSL_PASSWORD:}") String keyStorePassword,
            @Value("${KEYSTORE_ACCESS_TOKEN_ALIAS:${KEYSTORE_ACCESS_Token_Alias:}}") String alias,
            @Value("${KEYSTORE_ACCESS_TOKEN_PW:${KEYSTORE_ACCESS_Token_PW:}}") String password
    ) {
        return loadOrGenerateKey(keyStoreLocation, keyStorePassword, alias, password);
    }

    @Bean(name = "refreshTokenKey")
    public Key refreshTokenKey(
            @Value("${server.ssl.key-store:}") String keyStoreLocation,
            @Value("${SSL_PASSWORD:}") String keyStorePassword,
            @Value("${KEYSTORE_REFRESH_TOKEN_ALIAS:${KEYSTORE_REFRESH_Token_Alias:}}") String alias,
            @Value("${KEYSTORE_REFRESH_TOKEN_PW:${KEYSTORE_REFRESH_Token_PW:}}") String password
    ) {
        return loadOrGenerateKey(keyStoreLocation, keyStorePassword, alias, password);
    }

    private Key loadOrGenerateKey(String keyStoreLocation, String keyStorePassword, String alias, String password) {
        if (isBlank(keyStoreLocation) || isBlank(keyStorePassword) || isBlank(alias) || isBlank(password)) {
            return Keys.secretKeyFor(SignatureAlgorithm.HS256);
        }
        try {
            Resource resource = resourceLoader.getResource(keyStoreLocation);
            if (!resource.exists()) {
                throw new IllegalStateException("jwt_keystore_not_found");
            }
            KeyStore ks = KeyStore.getInstance("PKCS12");
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

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
