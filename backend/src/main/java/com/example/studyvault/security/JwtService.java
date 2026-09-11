package com.example.studyvault.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final SecretKey key;
    private final Duration lifetime;

    public JwtService(@Value("${studyvault.jwt.secret}") String secret, @Value("${studyvault.jwt.expiration-seconds:86400}") long expirationSeconds) {
        if (secret == null || secret.length() < 32) throw new IllegalStateException("JWT_SECRET must be at least 32 characters");
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.lifetime = Duration.ofSeconds(expirationSeconds);
    }
    public String issue(Long userId, String username) {
        Instant now = Instant.now();
        return Jwts.builder().subject(userId.toString()).claim("username", username)
                .issuedAt(Date.from(now)).expiration(Date.from(now.plus(lifetime))).signWith(key).compact();
    }
    public Claims parse(String token) { return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload(); }
}
