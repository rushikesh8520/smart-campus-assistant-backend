
package edu.smartcampus.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expiry;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expiry) {

        this.key = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8));
        this.expiry = expiry;
    }

    public String generate(String email) {
        return generate(email, "STUDENT");
    }

    public String generate(String email, String role) {
        long now = System.currentTimeMillis();

        return Jwts.builder()
                .subject(email)
                .claim("role", role == null
                        ? "STUDENT"
                        : role.toUpperCase())
                .issuedAt(new Date(now))
                .expiration(new Date(now + expiry))
                .signWith(key)
                .compact();
    }

    public String subject(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public String role(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String role = claims.get("role", String.class);

        return role == null ? "STUDENT" : role.toUpperCase();
    }
}
