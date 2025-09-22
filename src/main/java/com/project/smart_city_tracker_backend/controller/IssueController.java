package com.project.smart_city_tracker_backend.controller;

import com.project.smart_city_tracker_backend.dto.*;
import com.project.smart_city_tracker_backend.exception.*;
import com.project.smart_city_tracker_backend.model.*;
import com.project.smart_city_tracker_backend.service.*;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/issues")
public class IssueController {

    private final IssueService issueService;
    private final CommentService commentService;

    public IssueController(IssueService issueService, CommentService commentService) {
        this.issueService = issueService;
        this.commentService = commentService;
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

    /**
     * Handles fetching a paginated and filtered list of all issues for the dashboard.
     *
     * @param pageable The pagination information (page, size, sort) provided by Spring.
     * @param search   Optional search term to filter issues by title or description.
     * @param category Optional category to filter issues by.
     * @param status   Optional status to filter issues by.
     * @return A ResponseEntity containing a Page of IssueSummaryDTOs.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Get all issues (paginated and filtered)",
        description = "Returns a paginated list of issues, with optional filters for search, category, and status.",
        security = @SecurityRequirement(name = "bearerAuth"),
        parameters = {
            @Parameter(name = "page", description = "Page number (0..N)"),
            @Parameter(name = "size", description = "Number of elements per page"),
            @Parameter(name = "sort", description = "Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported."),
            @Parameter(name = "reportedBy", description = "Filter issues by the current user. Set to 'me'.")
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list of issues"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
        }
    )
    public ResponseEntity<Page<IssueSummaryDTO>> getAllIssues(
            Pageable pageable,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String reportedBy) {
        
        Page<IssueSummaryDTO> issuesPage = issueService.getAllIssues(pageable, search, category, status, reportedBy);
        return ResponseEntity.ok(issuesPage);
    }
    
    /**
     * Handles adding a new comment to a specific issue. This endpoint supports
     * threaded replies and file attachments with the comment.
     *
     * @param issueId The ID of the issue to comment on.
     * @param request The JSON part of the request containing the comment text and optional parentId.
     * @param files   An optional list of files to attach to the comment.
     * @return A ResponseEntity containing the newly created comment.
     */
    @PostMapping(value = "/{issueId}/comments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Add a comment to an issue",
        description = "Adds a new comment to a specific issue. Can include a parentId to be a reply, and can include file attachments.",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "201", description = "Comment created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommentResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request (e.g., validation error)"),
            @ApiResponse(responseCode = "404", description = "Issue or parent comment not found")
        }
    )
    public ResponseEntity<CommentResponseDTO> addComment(
            @PathVariable Long issueId,
            @RequestPart("commentData") @Valid CreateCommentRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        
        Comment newComment = commentService.addComment(issueId, request, files);
        
        return new ResponseEntity<>(new CommentResponseDTO(newComment), HttpStatus.CREATED);
    }

    /**
     * Handles updating the text of an existing comment.
     *
     * @param issueId The ID of the parent issue.
     * @param commentId The ID of the comment to update.
     * @param request The request body containing the new comment text.
     * @return A ResponseEntity containing the updated comment.
     */
    @PutMapping("/{issueId}/comments/{commentId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Update a comment",
        description = "Updates the text of an existing comment. Only the original author can perform this action.",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "Comment updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommentResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request (e.g., validation error)"),
            @ApiResponse(responseCode = "401", description = "Unauthorized (user is not the author)"),
            @ApiResponse(responseCode = "404", description = "Issue or comment not found")
        }
    )
    public ResponseEntity<CommentResponseDTO> updateComment(
            @PathVariable Long issueId,
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateCommentRequest request) {
        
        Comment updatedComment = commentService.updateComment(issueId, commentId, request);
        
        return new ResponseEntity<>(new CommentResponseDTO(updatedComment), HttpStatus.OK);
    }

    /**
     * Handles deleting an existing comment.
     *
     * @param issueId   The ID of the parent issue.
     * @param commentId The ID of the comment to delete.
     * @return A ResponseEntity with a 204 No Content status on success.
     */
    @DeleteMapping("/{issueId}/comments/{commentId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Delete a comment",
        description = "Deletes an existing comment. Only the original author or an admin can perform this action.",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "204", description = "Comment deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized (user is not the author or an admin)"),
            @ApiResponse(responseCode = "404", description = "Issue or comment not found")
        }
    )
    public ResponseEntity<Map<String, String>> deleteComment(
            @PathVariable Long issueId,
            @PathVariable Long commentId) {
        
        commentService.deleteComment(issueId, commentId);
        
        return ResponseEntity.ok(Map.of("message", "Comment deleted successfully."));
    }

    /**
     * Handles adding one or more attachments directly to an existing issue.
     *
     * @param issueId The ID of the issue to add attachments to.
     * @param files   The list of files to upload.
     * @return A ResponseEntity containing a list of the newly created attachment details.
     */
    @PostMapping(value = "/{issueId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Add attachments to an issue",
        description = "Uploads one or more files as attachments (proofs) to an existing issue. Requires Admin, Assignee, or Reporter permissions.",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "201", description = "Attachments added successfully"),
            @ApiResponse(responseCode = "400", description = "Bad Request (e.g., no files provided)"),
            @ApiResponse(responseCode = "403", description = "Forbidden (user is not a staff, admin or reporter)"),
            @ApiResponse(responseCode = "404", description = "Issue not found")
        }
    )
    public ResponseEntity<List<AttachmentResponseDTO>> addAttachmentsToIssue(
            @PathVariable Long issueId,
            @RequestPart("files") List<MultipartFile> files) {

        if (files == null || files.isEmpty() || files.stream().allMatch(MultipartFile::isEmpty)) {
            throw new BadRequestException("At least one file must be provided.");
        }

        List<Attachment> newAttachments = issueService.addAttachmentsToIssue(issueId, files);

        List<AttachmentResponseDTO> responseDTOs = newAttachments.stream()
                .map(AttachmentResponseDTO::new)
                .toList();

        return new ResponseEntity<>(responseDTOs, HttpStatus.CREATED);
    }

    /**
     * Handles adding attachments to an EXISTING comment.
     *
     * @param issueId   The ID of the parent issue.
     * @param commentId The ID of the comment to add attachments to.
     * @param files     The list of files to upload.
     * @return A ResponseEntity with a list of the newly created attachment details.
     */
    @PostMapping(value = "/{issueId}/comments/{commentId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Add attachments to an existing comment", description = "Uploads one or more files to an existing comment. Requires author or admin permissions.")
    public ResponseEntity<List<AttachmentResponseDTO>> addAttachmentsToComment(
            @PathVariable Long issueId,
            @PathVariable Long commentId,
            @RequestPart("files") List<MultipartFile> files) {

        if (files == null || files.isEmpty()) {
            throw new BadRequestException("At least one file must be provided.");
        }
        
        List<Attachment> newAttachments = commentService.addAttachmentsToComment(issueId, commentId, files);
        
        List<AttachmentResponseDTO> responseDTOs = newAttachments.stream()
                .map(AttachmentResponseDTO::new)
                .toList();

        return new ResponseEntity<>(responseDTOs, HttpStatus.CREATED);
    }

    /**
     * Handles deleting an attachment from an issue or a comment.
     *
     * @param issueId      The ID of the parent issue (for verification).
     * @param attachmentId The ID of the attachment to delete.
     * @return A ResponseEntity with a confirmation message on success.
     */
    @DeleteMapping("/{issueId}/attachments/{attachmentId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Delete an attachment",
        description = "Deletes an attachment from an issue or comment. Permissions are based on user role and ownership.",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "Attachment deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden (user does not have permission)"),
            @ApiResponse(responseCode = "404", description = "Issue or attachment not found")
        }
    )
    public ResponseEntity<Map<String, String>> deleteAttachment(
            @PathVariable Long issueId,
            @PathVariable Long attachmentId) {

        issueService.deleteAttachment(issueId, attachmentId);
        
        return ResponseEntity.ok(Map.of("message", "Attachment deleted successfully."));
    }

    /**
     * Handles fetching the full details for a single issue by its ID.
     *
     * @param issueId The ID of the issue to fetch.
     * @return A ResponseEntity containing the comprehensive IssueDetailsDTO.
     */
    @GetMapping("/{issueId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Get a single issue by ID",
        description = "Returns the full details of a single issue, including attachments and comments. Requires authentication.",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved issue details",
                content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = IssueDetailsDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Issue not found")
        }
    )
    public ResponseEntity<IssueDetailsDTO> getIssueById(@PathVariable Long issueId) {
        IssueDetailsDTO issueDetails = issueService.getIssueById(issueId);
        return ResponseEntity.ok(issueDetails);
    }

    /**
     * Handles updating the details of an existing issue.
     *
     * @param issueId The ID of the issue to update.
     * @param request The request body containing the fields to update.
     * @return A ResponseEntity containing the full, updated issue details.
     */
    @PutMapping("/{issueId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Update an issue",
        description = "Updates details of an existing issue (e.g., status, assignee, title). Requires Admin, Assignee, or Reporter permissions.",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "Issue updated successfully",
                content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = IssueDetailsDTO.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request (e.g., validation error, invalid ID)"),
            @ApiResponse(responseCode = "403", description = "Forbidden (user does not have permission)"),
            @ApiResponse(responseCode = "404", description = "Issue not found")
        }
    )
    public ResponseEntity<IssueDetailsDTO> updateIssue(
            @PathVariable Long issueId,
            @Valid @RequestBody UpdateIssueRequest request) {

        Issue updatedIssue = issueService.updateIssue(issueId, request);

        return ResponseEntity.ok(new IssueDetailsDTO(updatedIssue));
    }

    /**
     * Handles deleting an entire issue.
     *
     * @param issueId The ID of the issue to delete.
     * @return A ResponseEntity with a confirmation message.
     */
    @DeleteMapping("/{issueId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Delete an issue",
        description = "Permanently deletes an issue and all of its associated data. Requires ADMIN role or issue ownership.",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "Issue deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden (user does not have permission)"),
            @ApiResponse(responseCode = "404", description = "Issue not found")
        }
    )
    public ResponseEntity<Map<String, String>> deleteIssue(@PathVariable Long issueId) {
        issueService.deleteIssue(issueId);
        
        return ResponseEntity.ok(Map.of("message", "Issue " + issueId + " deleted successfully."));
    }
}