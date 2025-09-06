package com.project.smart_city_tracker_backend.controller;

import com.project.smart_city_tracker_backend.dto.AuthResponse;
import com.project.smart_city_tracker_backend.dto.LoginRequest;
import com.project.smart_city_tracker_backend.dto.RegisterRequest;
import com.project.smart_city_tracker_backend.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @Value("${app.jwt.access-expiration-ms}")
    private int jwtAccessExpirationMs;

    @Value("${app.jwt.refresh-expiration-ms}")
    private int jwtRefreshExpirationMs;

    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Authenticate user and set refresh token in HttpOnly cookie.
     * Access token is returned in response body.
     */
    @PostMapping("/login")
    @Operation(summary = "Login user", description = "Authenticate a user, return access token and user details, set refresh token in HttpOnly cookie.", responses = {
            @ApiResponse(responseCode = "200", description = "Login successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<AuthResponse> loginUser(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletResponse response) {

        AuthResponse authResponse = authService.loginUser(loginRequest);

        String refreshToken = authService.generateRefreshToken(loginRequest.getUserName());
        setCookie(response, REFRESH_TOKEN_COOKIE, refreshToken, jwtRefreshExpirationMs);

        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/register")
    @Operation(summary = "Register user", description = "Register a new user and return access token; set refresh token in HttpOnly cookie.", responses = {
            @ApiResponse(responseCode = "201", description = "User registered successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad request (username taken)")
    })
    public ResponseEntity<AuthResponse> registerUser(
            @Valid @RequestBody RegisterRequest registerRequest,
            HttpServletResponse response) {

        AuthResponse authResponse = authService.registerUser(registerRequest);

        String refreshToken = authService.generateRefreshToken(registerRequest.getUserName());
        setCookie(response, REFRESH_TOKEN_COOKIE, refreshToken, jwtRefreshExpirationMs);

        return new ResponseEntity<>(authResponse, HttpStatus.CREATED);
    }

    /**
     * Refresh access token using refresh token from cookie.
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Generate a new access token using refresh token from cookies.")
    public ResponseEntity<Object> refreshAccessToken(
        @CookieValue(value = REFRESH_TOKEN_COOKIE, required = false) String refreshToken) {

        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                    "message", "Refresh token not found. Please login again.",
                    "error", "Unauthorized"
                ));
        }

        AuthResponse authResponse = authService.refreshAccessToken(refreshToken);
        return ResponseEntity.ok(authResponse);
    }

    /**
     * Logout by clearing the refresh token cookie.
     */
    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Clears authentication cookies (refresh token).")
    public ResponseEntity<String> logout(HttpServletResponse response) {
        clearCookie(response, REFRESH_TOKEN_COOKIE);
        return ResponseEntity.ok("Logged out successfully");
    }

    private void setCookie(HttpServletResponse response, String name, String value, int maxAgeMs) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(maxAgeMs / 1000);
        response.addCookie(cookie);
    }

    private void clearCookie(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}