package com.project.smart_city_tracker_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Username cannot be empty")
    private String userName;

    @NotBlank(message = "Password cannot be empty")
    private String password;
}