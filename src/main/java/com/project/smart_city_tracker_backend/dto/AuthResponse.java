package com.project.smart_city_tracker_backend.dto;

import com.project.smart_city_tracker_backend.model.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class AuthResponse {

    private String fullName;
    private String userName;
    private List<String> roles;
    private String accessToken;

    public AuthResponse(User user, String accessToken) {
        this.fullName = user.getFullName();
        this.userName = user.getUsername();
        this.roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        this.accessToken = accessToken;
    }
}