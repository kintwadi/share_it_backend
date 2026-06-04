package com.vicinity24.api.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {
    private final Key accessTokenKey;
    private final Key refreshTokenKey;
    private final long validityInMs;

    public JwtTokenProvider(
            @Qualifier("accessTokenKey") Key accessTokenKey,
            @Qualifier("refreshTokenKey") Key refreshTokenKey,
            @Value("${security.jwt.validity-ms}") long validityInMs
    ) {
        this.accessTokenKey = accessTokenKey;
        this.refreshTokenKey = refreshTokenKey;
        this.validityInMs = validityInMs;
    }

    public String generateToken(String subject) {
        return generateToken(subject, false);
    }

    public String generateToken(String subject, boolean isPreAuth) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + (isPreAuth ? 300000 : validityInMs)); // 5 mins for pre-auth
        var builder = Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .claim("tokenType", "access")
                .signWith(accessTokenKey, SignatureAlgorithm.HS256);
        
        if (isPreAuth) {
            builder.claim("scope", "PRE_AUTH_2FA");
        }
        
        return builder.compact();
    }

    public String getSubject(String token) {
        return Jwts.parserBuilder().setSigningKey(accessTokenKey).build().parseClaimsJws(token).getBody().getSubject();
    }

    public boolean isPreAuth(String token) {
        try {
            String scope = Jwts.parserBuilder().setSigningKey(accessTokenKey).build().parseClaimsJws(token).getBody().get("scope", String.class);
            return "PRE_AUTH_2FA".equals(scope);
        } catch (Exception e) {
            return false;
        }
    }

    public String generateRefreshToken(String subject) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + validityInMs);
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .claim("tokenType", "refresh")
                .signWith(refreshTokenKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String getSubjectFromRefreshToken(String refreshToken) {
        return Jwts.parserBuilder().setSigningKey(refreshTokenKey).build().parseClaimsJws(refreshToken).getBody().getSubject();
    }
}
