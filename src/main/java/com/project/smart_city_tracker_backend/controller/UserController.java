package com.project.smart_city_tracker_backend.controller;

import com.project.smart_city_tracker_backend.dto.UserDTO;
import com.project.smart_city_tracker_backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Fetches a list of users, with an optional filter by role.
     *
     * @param role Optional role to filter by (e.g., "STAFF", "ADMIN").
     * @return A ResponseEntity containing a list of UserDTOs.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Get all users",
        description = "Returns a list of all users. Can be filtered by role. Requires authentication.",
        security = @SecurityRequirement(name = "bearerAuth"),
        parameters = {
            @Parameter(name = "role", description = "Optional role to filter by (e.g., 'STAFF'). Case-insensitive.")
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list of users"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
        }
    )
    public ResponseEntity<List<UserDTO>> getAllUsers(@RequestParam(required = false) String role) {
        List<UserDTO> users = userService.getAllUsers(role);
        return ResponseEntity.ok(users);
    }
}