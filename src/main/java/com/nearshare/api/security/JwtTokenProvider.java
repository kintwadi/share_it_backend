package com.nearshare.api.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {
    private final Key key;
    private final long validityInMs;

    public JwtTokenProvider(@Value("${security.jwt.secret}") String secret, @Value("${security.jwt.validity-ms}") long validityInMs) {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (Exception ex) {
            keyBytes = secret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
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
                .signWith(key, SignatureAlgorithm.HS256);
        
        if (isPreAuth) {
            builder.claim("scope", "PRE_AUTH_2FA");
        }
        
        return builder.compact();
    }

    public String getSubject(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().getSubject();
    }

    public boolean isPreAuth(String token) {
        try {
            String scope = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().get("scope", String.class);
            return "PRE_AUTH_2FA".equals(scope);
        } catch (Exception e) {
            return false;
        }
    }
}