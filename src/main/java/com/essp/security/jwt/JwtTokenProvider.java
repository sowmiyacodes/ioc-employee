package com.essp.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long jwtExpirationMs;

    // In-memory token blacklist for immediate revocation on logout
    private final Map<String, Long> blacklistedTokens = new ConcurrentHashMap<>();

    public JwtTokenProvider(
            @Value("${app.security.jwt.secret:dGhpcy1pcy1hLXNlY3VyZS0yNTYtYml0LWtleS1mb3ItZXNzcG1lbWJlcjY0LXNlY3VyaXR5LXRlc3RpbmctcHVycG9zZXM=}") String secret,
            @Value("${app.security.jwt.expiration-ms:86400000}") long jwtExpirationMs) {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.jwtExpirationMs = jwtExpirationMs;
    }

    public String generateToken(String username, String role, Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .claim("userId", userId)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    public String getRoleFromToken(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public Long getUserIdFromToken(String token) {
        Object val = parseClaims(token).get("userId");
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        return null;
    }

    public boolean validateToken(String token) {
        if (isBlacklisted(token)) {
            return false;
        }
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public void invalidateToken(String token) {
        try {
            Claims claims = parseClaims(token);
            long expiry = claims.getExpiration().getTime();
            blacklistedTokens.put(token, expiry);
        } catch (Exception e) {
            blacklistedTokens.put(token, System.currentTimeMillis() + jwtExpirationMs);
        }
    }

    public boolean isBlacklisted(String token) {
        Long expiry = blacklistedTokens.get(token);
        if (expiry == null) {
            return false;
        }
        if (System.currentTimeMillis() > expiry) {
            blacklistedTokens.remove(token);
            return false;
        }
        return true;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
