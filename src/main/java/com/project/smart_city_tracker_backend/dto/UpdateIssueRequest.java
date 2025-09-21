package com.project.smart_city_tracker_backend.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
public class UpdateIssueRequest {

    @Size(min = 10, max = 255, message = "Title must be between 10 and 255 characters.")
    private String title;

    @Size(min = 20, message = "Description must be at least 20 characters long.")
    private String description;

    private Integer categoryId;

    private Integer statusId;

    private Integer priorityId;

    private Long assigneeId;

    private Instant startDate;

    private Instant dueDate;
}