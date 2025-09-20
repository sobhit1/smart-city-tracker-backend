package com.project.smart_city_tracker_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateIssueRequest {

    @NotBlank(message = "Title is required.")
    @Size(min = 10, max = 255, message = "Title must be between 10 and 255 characters.")
    private String title;

    @NotBlank(message = "Description is required.")
    @Size(min = 20, message = "Description must be at least 20 characters long.")
    private String description;

    @NotBlank(message = "Category is required.")
    private String category;

    @NotNull(message = "Latitude is required.")
    private Double latitude;

    @NotNull(message = "Longitude is required.")
    private Double longitude;
}