package com.project.smart_city_tracker_backend.dto;

import com.project.smart_city_tracker_backend.model.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    private Long id;
    private String fullName;
    private String userName;
    private List<String> roles;

    public UserDTO(User user) {
        this.id = user.getId();
        this.fullName = user.getFullName();
        this.userName = user.getUsername();
        this.roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }
}