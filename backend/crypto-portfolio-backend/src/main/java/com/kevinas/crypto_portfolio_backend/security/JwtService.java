package com.kevinas.crypto_portfolio_backend.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-seconds}")
    private long expirationSeconds;

    public String generateToken(String subject) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + (expirationSeconds * 1000));

        return Jwts.builder()
                .subject(subject)
                .issuedAt(now)
                .expiration(exp)
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    public String extractSubject(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
}
