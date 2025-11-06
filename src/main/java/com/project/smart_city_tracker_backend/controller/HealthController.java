package com.project.smart_city_tracker_backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    /**
     * Returns application health status.
     *
     * @return HTTP 200 with "OK" message if the service is running.
     */
    @GetMapping("/health")
    @Operation(
        summary = "Health Check",
        description = "Public endpoint used for uptime monitoring. Returns 200 OK.",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Service is up",
                content = @Content(mediaType = "text/plain")
            )
        }
    )
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
