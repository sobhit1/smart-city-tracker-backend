package com.project.smart_city_tracker_backend.security;

import com.project.smart_city_tracker_backend.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.security.KeyPair;
import java.util.Date;
import java.util.List;

@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${app.jwt.access-expiration-ms}")
    private long accessTokenExpirationInMs;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshTokenExpirationInMs;

    private KeyPair keyPair;

    @PostConstruct
    public void init() {
        this.keyPair = Jwts.SIG.RS256.keyPair().build();
    }

    /**
     * Generates an access token for a given authenticated user.
     *
     * @param authentication The authentication object from Spring Security context.
     * @return A signed JWT access token.
     */
    public String generateAccessToken(Authentication authentication) {
        User userPrincipal = (User) authentication.getPrincipal();

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
                .signWith(keyPair.getPrivate(), Jwts.SIG.RS256)
                .compact();
    }

    /**
     * Generates an access token directly from a User object.
     * Useful when refreshing token without full Authentication object.
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
                .signWith(keyPair.getPrivate(), Jwts.SIG.RS256)
                .compact();
    }


    /**
     * Generates a refresh token for a given authenticated user.
     *
     * @param authentication The authentication object from Spring Security context.
     * @return A signed JWT refresh token.
     */
    public String generateRefreshToken(Authentication authentication) {
        User userPrincipal = (User) authentication.getPrincipal();

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpirationInMs);

        return Jwts.builder()
                .subject(userPrincipal.getUsername())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(keyPair.getPrivate(), Jwts.SIG.RS256)
                .compact();
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
                .signWith(keyPair.getPrivate(), Jwts.SIG.RS256)
                .compact();
    }


    /**
     * Extracts the username from a given JWT.
     *
     * @param token The JWT string.
     * @return The username contained within the token.
     */
    public String getUserNameFromJwt(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(keyPair.getPublic())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    /**
     * Validates if a JWT is authentic and not expired.
     * @param authToken The JWT string to validate.
     * @return true if the token is valid, false otherwise.
     */
    public boolean validateToken(String authToken) {
        try {
            Jwts.parser().verifyWith(keyPair.getPublic()).build().parseSignedClaims(authToken);
            return true;
        } catch (MalformedJwtException ex) {
            logger.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            logger.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            logger.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            logger.error("JWT claims string is empty.");
        }
        return false;
    }
}