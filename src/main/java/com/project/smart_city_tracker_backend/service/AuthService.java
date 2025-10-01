package com.project.smart_city_tracker_backend.service;

import com.project.smart_city_tracker_backend.dto.AuthResponse;
import com.project.smart_city_tracker_backend.dto.LoginRequest;
import com.project.smart_city_tracker_backend.dto.RegisterRequest;
import com.project.smart_city_tracker_backend.exception.BadRequestException;
import com.project.smart_city_tracker_backend.model.Role;
import com.project.smart_city_tracker_backend.model.User;
import com.project.smart_city_tracker_backend.repository.RoleRepository;
import com.project.smart_city_tracker_backend.repository.UserRepository;
import com.project.smart_city_tracker_backend.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * Authenticates a user and returns a complete AuthResponse.
     */
    public AuthResponse loginUser(LoginRequest loginRequest) {
        try {
            String userName = loginRequest.getUserName().trim();
            String password = loginRequest.getPassword().trim();

            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(userName, password)
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            User user = (User) authentication.getPrincipal();

            String accessToken = jwtTokenProvider.generateAccessToken(authentication);

            return new AuthResponse(user, accessToken);

        } catch (BadCredentialsException ex) {
            throw new BadRequestException("Incorrect username or password");
        }
    }

    /**
     * Registers a new user and returns a complete AuthResponse.
     */
    @Transactional
    public AuthResponse registerUser(RegisterRequest registerRequest) {
        String fullName = registerRequest.getFullName().trim();
        String userName = registerRequest.getUserName().trim();
        String password = registerRequest.getPassword().trim();

        if (fullName.isEmpty() || fullName.length() < 3 || fullName.length() > 200) {
            throw new BadRequestException("Full name must be between 3 and 200 characters and cannot be blank");
        }

        if (userName.isEmpty() || userName.length() < 3 || userName.length() > 50) {
            throw new BadRequestException("Username must be between 3 and 50 characters and cannot be blank");
        }
        if (Boolean.TRUE.equals(userRepository.existsByUserName(userName))) {
            throw new BadRequestException("Username is already taken!");
        }

        if (password.isEmpty() || password.length() < 6 || password.length() > 100) {
            throw new BadRequestException("Password must be between 6 and 100 characters and cannot be blank");
        }

        User user = new User();
        user.setFullName(fullName);
        user.setUserName(userName);
        user.setPassword(passwordEncoder.encode(password));

        Role userRole = roleRepository.findByName("ROLE_CITIZEN")
                .orElseThrow(() -> new BadRequestException(
                        "Default role 'ROLE_CITIZEN' is not configured in the database."
                ));

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        user.setRoles(roles);

        userRepository.save(user);

        String accessToken = jwtTokenProvider.generateAccessTokenFromUser(user);

        return new AuthResponse(user, accessToken);
    }

    /**
     * Generates a refresh token for a given username.
     */
    public String generateRefreshToken(String username) {
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new BadRequestException("User not found with username: " + username));
        return jwtTokenProvider.generateRefreshTokenFromUser(user);
    }

    /**
     * Refreshes an access token and returns a complete AuthResponse, including the user's ID.
     */
    public AuthResponse refreshAccessToken(String refreshToken) {
        try {
            if (!jwtTokenProvider.validateToken(refreshToken)) {
                throw new BadRequestException("Invalid or expired refresh token.");
            }

            String username = jwtTokenProvider.getUserNameFromJwt(refreshToken);
            User user = userRepository.findByUserName(username)
                    .orElseThrow(() -> new BadRequestException("User not found with username: " + username));

            String newAccessToken = jwtTokenProvider.generateAccessTokenFromUser(user);

            return new AuthResponse(user, newAccessToken);

        } catch (Exception ex) {
            throw new BadRequestException("Failed to refresh access token: " + ex.getMessage());
        }
    }
}