package com.project.smart_city_tracker_backend.security;

import com.project.smart_city_tracker_backend.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    /**
     * Gets the currently authenticated user from the Security Context.
     *
     * @return The authenticated User object.
     * @throws IllegalStateException if no user is authenticated.
     */
    public static User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof User)) {
            throw new IllegalStateException("No authenticated user found in the security context.");
        }
        return (User) authentication.getPrincipal();
    }
}