package com.project.smart_city_tracker_backend.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * A custom AuthenticationEntryPoint to handle authentication failures.
 * This component is triggered when an unauthenticated user tries to access a protected resource.
 * Its primary purpose is to override Spring's default 403 Forbidden response for these cases
 * and instead return a clear 401 Unauthorized status, which is what our frontend expects.
 */
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"Authentication token was either missing, expired, or invalid.\"}");
    }
}