package com.project.smart_city_tracker_backend.controller;

import com.project.smart_city_tracker_backend.dto.CreateIssueRequest;
import com.project.smart_city_tracker_backend.exception.BadRequestException; // Import BadRequestException
import com.project.smart_city_tracker_backend.model.Issue;
import com.project.smart_city_tracker_backend.service.IssueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/issues")
public class IssueController {

    private final IssueService issueService;

    public IssueController(IssueService issueService) {
        this.issueService = issueService;
    }

    /**
     * Handles the creation of a new issue with file attachments.
     *
     * @param request The JSON part of the request, mapped to the CreateIssueRequest DTO.
     * @param files   The list of files uploaded with the request. Must not be empty.
     * @return A ResponseEntity containing the newly created Issue and an HTTP 201 Created status.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Create a new issue",
        description = "Creates a new issue with details and at least one file attachment. Requires authentication.",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "201", description = "Issue created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Issue.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request (e.g., validation error, no files attached)"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
        }
    )
    public ResponseEntity<Issue> createIssue(
            @RequestPart("issueData") @Valid CreateIssueRequest request,
            @RequestPart("files") List<MultipartFile> files) {

        if (files == null || files.isEmpty() || files.stream().allMatch(MultipartFile::isEmpty)) {
            throw new BadRequestException("At least one file attachment is required.");
        }

        Issue newIssue = issueService.createIssue(request, files);
        return new ResponseEntity<>(newIssue, HttpStatus.CREATED);
    }
}