package com.project.smart_city_tracker_backend.controller;

import com.project.smart_city_tracker_backend.dto.CategoryDTO;
import com.project.smart_city_tracker_backend.dto.PriorityDTO;
import com.project.smart_city_tracker_backend.dto.StatusDTO;
import com.project.smart_city_tracker_backend.service.LookupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class LookupController {

    private final LookupService lookupService;

    public LookupController(LookupService lookupService) {
        this.lookupService = lookupService;
    }

    @GetMapping("/categories")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all issue categories", security = @SecurityRequirement(name = "bearerAuth"),
               responses = @ApiResponse(responseCode = "200", description = "Successfully retrieved categories"))
    public ResponseEntity<List<CategoryDTO>> getAllCategories() {
        return ResponseEntity.ok(lookupService.getAllCategories());
    }

    @GetMapping("/statuses")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all issue statuses", security = @SecurityRequirement(name = "bearerAuth"),
               responses = @ApiResponse(responseCode = "200", description = "Successfully retrieved statuses"))
    public ResponseEntity<List<StatusDTO>> getAllStatuses() {
        return ResponseEntity.ok(lookupService.getAllStatuses());
    }

    @GetMapping("/priorities")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all issue priorities", security = @SecurityRequirement(name = "bearerAuth"),
               responses = @ApiResponse(responseCode = "200", description = "Successfully retrieved priorities"))
    public ResponseEntity<List<PriorityDTO>> getAllPriorities() {
        return ResponseEntity.ok(lookupService.getAllPriorities());
    }
}