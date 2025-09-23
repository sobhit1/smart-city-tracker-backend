package com.project.smart_city_tracker_backend.security;

import com.project.smart_city_tracker_backend.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.List;

@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${app.jwt.access-expiration-ms}")
    private long accessTokenExpirationInMs;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshTokenExpirationInMs;

    @Value("${app.jwt.key.private}")
    private String privateKeyString;

    @Value("${app.jwt.key.public}")
    private String publicKeyString;

    private PrivateKey privateKey;
    private PublicKey publicKey;

    @PostConstruct
    public void init() {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyString);
            byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyString);

            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);

            this.privateKey = keyFactory.generatePrivate(privateKeySpec);
            this.publicKey = keyFactory.generatePublic(publicKeySpec);

        } catch (Exception e) {
            throw new IllegalStateException("Could not initialize JWT keys. Please check your .env configuration.", e);
        }
    }

    /**
     * Generates an access token using the loaded private key.
     */
    public String generateAccessToken(Authentication authentication) {
        User userPrincipal = (User) authentication.getPrincipal();
        return generateAccessTokenFromUser(userPrincipal);
    }

    /**
     * Generates an access token directly from a User object.
     */
    public String generateAccessTokenFromUser(User userPrincipal) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpirationInMs);

        List<String> roles = userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return Jwts.builder()
                .subject(userPrincipal.getUsername())
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(this.privateKey)
                .compact();
    }

    /**
     * Generates a refresh token for a given authenticated user.
     */
    public String generateRefreshToken(Authentication authentication) {
        User userPrincipal = (User) authentication.getPrincipal();
        return generateRefreshTokenFromUser(userPrincipal);
    }

    /**
     * Generates a refresh token directly from a User object.
     */
    public String generateRefreshTokenFromUser(User userPrincipal) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpirationInMs);

        return Jwts.builder()
                .subject(userPrincipal.getUsername())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(this.privateKey)
                .compact();
    }

    /**
     * Extracts the username from a JWT using the loaded public key.
     */
    public String getUserNameFromJwt(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(this.publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    /**
     * Validates if a JWT is authentic and not expired using the loaded public key.
     */
    public boolean validateToken(String authToken) {
        try {
            Jwts.parser().verifyWith(this.publicKey).build().parseSignedClaims(authToken);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            logger.error("JWT validation error: {}", e.getMessage());
        }
        return false;
    }
}