package com.example.javapingpongelo.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration:86400000}")
    private long jwtExpiration;

    @Value("${app.jwt.kiosk-expiration:31536000000}")
    private long kioskExpiration;

    public String generateToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
                   .subject(userPrincipal.getUsername())
                   .issuedAt(now)
                   .expiration(expiryDate)
                   .signWith(getSigningKey())
                   .compact();
    }

    public String generateLongLivedToken(String username) {
        return generateLongLivedToken(username, java.util.UUID.randomUUID().toString());
    }

    public String generateLongLivedToken(String username, String jti) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + kioskExpiration);

        return Jwts.builder()
                   .subject(username)
                   .id(jti)
                   .issuedAt(now)
                   .expiration(expiryDate)
                   .signWith(getSigningKey())
                   .compact();
    }

    public String getJtiFromToken(String token) {
        io.jsonwebtoken.Claims claims = Jwts.parser()
                                            .verifyWith(getSigningKey())
                                            .build()
                                            .parseSignedClaims(token)
                                            .getPayload();
        return claims.getId();
    }

    public long getKioskExpirationMillis() {
        return kioskExpiration;
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                            .verifyWith(getSigningKey())
                            .build()
                            .parseSignedClaims(token)
                            .getPayload();

        return claims.getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
            return true;
        }
        catch (io.jsonwebtoken.security.SecurityException | io.jsonwebtoken.MalformedJwtException ex) {
            log.error("Invalid JWT signature", ex);
        }
        catch (io.jsonwebtoken.ExpiredJwtException ex) {
            log.error("Expired JWT token", ex);
        }
        catch (io.jsonwebtoken.UnsupportedJwtException ex) {
            log.error("Unsupported JWT token", ex);
        }
        catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty", ex);
        }
        catch (Exception ex) {
            log.error("JWT token validation failed", ex);
        }
        return false;
    }
}