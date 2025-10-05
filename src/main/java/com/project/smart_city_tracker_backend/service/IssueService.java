package com.project.smart_city_tracker_backend.service;

import com.project.smart_city_tracker_backend.dto.*;
import com.project.smart_city_tracker_backend.exception.*;
import com.project.smart_city_tracker_backend.model.*;
import com.project.smart_city_tracker_backend.repository.*;
import com.project.smart_city_tracker_backend.security.SecurityUtils;
import com.project.smart_city_tracker_backend.utils.FilterParser;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class IssueService {

    private final IssueRepository issueRepository;
    private final CloudinaryService cloudinaryService;
    private final AttachmentRepository attachmentRepository;
    private final CategoryRepository categoryRepository;
    private final StatusRepository statusRepository;
    private final PriorityRepository priorityRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;

    public IssueService(IssueRepository issueRepository, CloudinaryService cloudinaryService,
            AttachmentRepository attachmentRepository, CategoryRepository categoryRepository,
            StatusRepository statusRepository, PriorityRepository priorityRepository,
            UserRepository userRepository, CommentRepository commentRepository) {
        this.issueRepository = issueRepository;
        this.cloudinaryService = cloudinaryService;
        this.attachmentRepository = attachmentRepository;
        this.categoryRepository = categoryRepository;
        this.statusRepository = statusRepository;
        this.priorityRepository = priorityRepository;
        this.userRepository = userRepository;
        this.commentRepository = commentRepository;
    }

    /**
     * Creates a new issue, uploads associated files to Cloudinary, and saves everything to the database.
     *
     * @param request The DTO containing the issue's details (title, description, etc.).
     * @param files   A non-empty list of files to be uploaded as attachments.
     * @return The newly created and saved Issue entity.
     */
    @Transactional
    public IssueDetailsDTO createIssue(CreateIssueRequest request, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new BadRequestException("Cannot create an issue without at least one attachment.");
        }

        User reporter = SecurityUtils.getCurrentUser();

        Category category = categoryRepository.findByName(request.getCategory())
                .orElseThrow(() -> new BadRequestException("Invalid category provided: " + request.getCategory()));

        Status initialStatus = statusRepository.findByName("OPEN")
                .orElseThrow(() -> new BadRequestException("Default status 'OPEN' is not configured in the database."));

        Priority defaultPriority = priorityRepository.findByName("Medium")
                .orElse(null);

        Issue issue = new Issue();
        issue.setTitle(request.getTitle());
        issue.setDescription(request.getDescription());
        issue.setLatitude(request.getLatitude());
        issue.setLongitude(request.getLongitude());
        issue.setReporter(reporter);
        issue.setCategory(category);
        issue.setStatus(initialStatus);
        issue.setPriority(defaultPriority);

        for (MultipartFile file : files) {
            try {
                Map<String, String> uploadResult = cloudinaryService.uploadFile(file);
                Attachment attachment = new Attachment(
                        uploadResult.get("url"),
                        uploadResult.get("publicId"),
                        file.getOriginalFilename(),
                        file.getContentType(),
                        issue
                );
                issue.addAttachment(attachment);
            } catch (IOException e) {
                throw new BadRequestException("Failed to upload file: " + file.getOriginalFilename(), e);
            }
        }

        Issue savedIssue = issueRepository.save(issue);

        return new IssueDetailsDTO(savedIssue);
    }

    /**
     * Fetches a paginated and filtered list of issues.
     *
     * @param pageable The pagination information (page number, size, sort).
     * @param search   An optional search term to filter by title or description.
     * @param category An optional category name to filter by.
     * @param status   An optional status name to filter by.
     * @return A Page of IssueSummaryDTOs.
     */
    @Transactional(readOnly = true)
    public Page<IssueSummaryDTO> getAllIssues(Pageable pageable, String search, String category, String status, String reportedBy, String assignedTo, List<String> advancedFilters) {
        Pageable correctedPageable = remapSortProperty(pageable, "priority", "priority.sortOrder");

        if ("me".equalsIgnoreCase(assignedTo)) {
            User currentUser = SecurityUtils.getCurrentUser();
            boolean isStaffOrAdmin = currentUser.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_STAFF")
                            || auth.getAuthority().equals("ROLE_ADMIN"));

            if (!isStaffOrAdmin) {
                throw new UnauthorizedException("You do not have permission to use the 'assignedTo' filter.");
            }
        }

        Specification<Issue> spec = buildSpecification(search, category, status, reportedBy, assignedTo, advancedFilters);
        Page<Issue> issuesPage = issueRepository.findAll(spec, correctedPageable);
        return issuesPage.map(IssueSummaryDTO::new);
    }

    /**
     * A private helper method to build a dynamic JPA Specification based on the provided filters.
     * This keeps the main service method clean and readable.
     */
    private Specification<Issue> buildSpecification(String search, String category, String status, String reportedBy, String assignedTo, List<String> advancedFilters) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(search)) {
                Predicate titlePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), "%" + search.toLowerCase() + "%");
                Predicate descriptionPredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), "%" + search.toLowerCase() + "%");
                predicates.add(criteriaBuilder.or(titlePredicate, descriptionPredicate));
            }

            if (StringUtils.hasText(category) && !category.equalsIgnoreCase("All")) {
                predicates.add(criteriaBuilder.equal(root.join("category").get("name"), category));
            }

            if (StringUtils.hasText(status) && !status.equalsIgnoreCase("All")) {
                predicates.add(criteriaBuilder.equal(root.join("status").get("name"), status));
            }

            if ("me".equalsIgnoreCase(reportedBy)) {
                User currentUser = SecurityUtils.getCurrentUser();
                predicates.add(criteriaBuilder.equal(root.get("reporter").get("id"), currentUser.getId()));
            }

            if ("me".equalsIgnoreCase(assignedTo)) {
                User currentUser = SecurityUtils.getCurrentUser();
                predicates.add(criteriaBuilder.equal(root.get("assignee").get("id"), currentUser.getId()));
            }

            List<FilterParser.FilterCriteria> criteriaList = FilterParser.parse(advancedFilters);
            for (FilterParser.FilterCriteria criteria : criteriaList) {
                String field = criteria.getField();
                String value = criteria.getValue();

                if (field.equalsIgnoreCase("priority")) {
                    Join<Object, Object> join = root.join(field);
                    predicates.add(criteriaBuilder.equal(join.get("name"), value));
                } else {
                    switch (criteria.getOperator().toLowerCase()) {
                        case "equals":
                            predicates.add(criteriaBuilder.equal(root.get(field), value));
                            break;
                        case "notequal":
                            predicates.add(criteriaBuilder.notEqual(root.get(field), value));
                            break;
                        case "contains":
                            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get(field)), "%" + value.toLowerCase() + "%"));
                            break;
                        case "doesnotcontain":
                            predicates.add(criteriaBuilder.notLike(criteriaBuilder.lower(root.get(field)), "%" + value.toLowerCase() + "%"));
                            break;
                        case "startswith":
                            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get(field)), value.toLowerCase() + "%"));
                            break;
                        case "endswith":
                             predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get(field)), "%" + value.toLowerCase()));
                            break;
                        case "isempty":
                            predicates.add(criteriaBuilder.or(
                                    criteriaBuilder.isNull(root.get(field)),
                                    criteriaBuilder.equal(root.get(field), "")));
                            break;
                        case "isnotempty":
                            predicates.add(criteriaBuilder.and(
                                    criteriaBuilder.isNotNull(root.get(field)),
                                    criteriaBuilder.notEqual(root.get(field), "")));
                            break;
                        default:
                            throw new BadRequestException("Unsupported filter operator: " + criteria.getOperator());
                    }
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * A utility to remap a sort property for logical sorting (e.g., priority).
     */
    private Pageable remapSortProperty(Pageable pageable, String fromProperty, String toProperty) {
        List<Sort.Order> correctedOrders = pageable.getSort().stream()
                .map(order -> {
                    if (order.getProperty().equalsIgnoreCase(fromProperty)) {
                        return new Sort.Order(order.getDirection(), toProperty);
                    }
                    return order;
                })
                .collect(Collectors.toList());
        Sort correctedSort = Sort.by(correctedOrders);
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), correctedSort);
    }

    /**
     * Adds attachments to an existing issue based on detailed permissions.
     *
     * @param issueId The ID of the issue to add attachments to.
     * @param files   The list of files to upload.
     * @return A list of the newly created Attachment entities.
     */
    @Transactional
    public List<Attachment> addAttachmentsToIssue(Long issueId, List<MultipartFile> files) {
        User currentUser = SecurityUtils.getCurrentUser();
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", "id", issueId));

        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        boolean isStaffAndAssignee = currentUser.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_STAFF")) && issue.getAssignee() != null
                && Objects.equals(issue.getAssignee().getId(), currentUser.getId());
        boolean isReporter = Objects.equals(issue.getReporter().getId(), currentUser.getId());

        if (!isAdmin && !isStaffAndAssignee && !isReporter) {
            throw new UnauthorizedException("You do not have permission to add attachments to this issue.");
        }

        List<Attachment> newAttachments = new ArrayList<>();
        for (MultipartFile file : files) {
            try {
                Map<String, String> uploadResult = cloudinaryService.uploadFile(file);
                Attachment attachment = new Attachment(
                        uploadResult.get("url"),
                        uploadResult.get("publicId"),
                        file.getOriginalFilename(),
                        file.getContentType(),
                        issue
                );
                newAttachments.add(attachment);
            } catch (IOException e) {
                throw new BadRequestException("Failed to upload file: " + file.getOriginalFilename(), e);
            }
        }

        return attachmentRepository.saveAll(newAttachments);
    }

    /**
     * Deletes an attachment from an issue or a comment.
     * This method handles deletion from both Cloudinary and the database with
     * proper error handling.
     *
     * @param issueId      The ID of the parent issue (for verification).
     * @param attachmentId The ID of the attachment to delete.
     */
    @Transactional
    public void deleteAttachment(Long issueId, Long attachmentId) {
        User currentUser = SecurityUtils.getCurrentUser();

        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment", "id", attachmentId));

        boolean isAuthorized = false;
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        if (attachment.getIssue() != null) {
            if (!Objects.equals(attachment.getIssue().getId(), issueId)) {
                throw new BadRequestException("Attachment does not belong to the specified issue.");
            }

            boolean isReporter = Objects.equals(attachment.getIssue().getReporter().getId(), currentUser.getId());

            boolean isStaffAndAssignee = currentUser.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_STAFF"))
                    && attachment.getIssue().getAssignee() != null
                    && Objects.equals(attachment.getIssue().getAssignee().getId(), currentUser.getId());

            if (isAdmin || isReporter || isStaffAndAssignee) {
                isAuthorized = true;
            }

        } else if (attachment.getComment() != null) {
            if (!Objects.equals(attachment.getComment().getIssue().getId(), issueId)) {
                throw new BadRequestException("Attachment's parent comment does not belong to the specified issue.");
            }

            boolean isCommentAuthor = Objects.equals(attachment.getComment().getAuthor().getId(), currentUser.getId());

            if (isAdmin || isCommentAuthor) {
                isAuthorized = true;
            }
        } else {
            throw new IllegalStateException("Attachment is not linked to any issue or comment.");
        }

        if (!isAuthorized) {
            throw new UnauthorizedException("You do not have permission to delete this attachment.");
        }

        try {
            cloudinaryService.deleteFile(attachment.getPublicId());
        } catch (IOException e) {
            System.err.println("Failed to delete file from Cloudinary for publicId: " + attachment.getPublicId() + ". Error: " + e.getMessage());
        }

        attachmentRepository.delete(attachment);
    }

    /**
     * Fetches a single issue by its ID and converts it to a detailed DTO.
     *
     * @param issueId The ID of the issue to fetch.
     * @return An IssueDetailsDTO containing the full details of the issue.
     */
    @Transactional(readOnly = true)
    public IssueDetailsDTO getIssueById(Long issueId) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", "id", issueId));

        return new IssueDetailsDTO(issue);
    }

    /**
     * Updates an existing issue with the provided data. This method handles partial updates
     * and performs all necessary security and business rule validations.
     *
     * @param issueId The ID of the issue to update.
     * @param request The DTO containing the fields to be updated.
     * @return The updated and saved Issue entity.
     */
    @Transactional
    public IssueDetailsDTO updateIssue(Long issueId, UpdateIssueRequest request) {
        User currentUser = SecurityUtils.getCurrentUser();
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", "id", issueId));

        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        boolean isStaffAndAssignee = currentUser.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_STAFF")) && issue.getAssignee() != null
                && Objects.equals(issue.getAssignee().getId(), currentUser.getId());
        boolean isReporter = Objects.equals(issue.getReporter().getId(), currentUser.getId());

        if (!isAdmin && !isStaffAndAssignee && !isReporter) {
            throw new UnauthorizedException("You do not have permission to update this issue.");
        }

        if (request.getTitle() != null) {
            issue.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            issue.setDescription(request.getDescription());
        }
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new BadRequestException("Invalid Category ID: " + request.getCategoryId()));
            issue.setCategory(category);
        }
        if (request.getStatusId() != null) {
            Status status = statusRepository.findById(request.getStatusId())
                    .orElseThrow(() -> new BadRequestException("Invalid Status ID: " + request.getStatusId()));
            issue.setStatus(status);
        }
        if (request.getPriorityId() != null) {
            Priority priority = priorityRepository.findById(request.getPriorityId())
                    .orElseThrow(() -> new BadRequestException("Invalid Priority ID: " + request.getPriorityId()));
            issue.setPriority(priority);
        }
        if (request.getAssigneeId() != null) {
            if (request.getAssigneeId() == 0) {
                issue.setAssignee(null);
            } else {
                User assignee = userRepository.findById(request.getAssigneeId())
                        .orElseThrow(() -> new BadRequestException("Invalid Assignee ID: " + request.getAssigneeId()));
                issue.setAssignee(assignee);
            }
        }
        if (request.getStartDate() != null) {
            issue.setStartDate(request.getStartDate());
        }
        if (request.getDueDate() != null) {
            Instant effectiveStartDate = (request.getStartDate() != null) ? request.getStartDate()
                    : issue.getStartDate();
            if (effectiveStartDate != null && request.getDueDate().isBefore(effectiveStartDate)) {
                throw new BadRequestException("Due date cannot be before the start date.");
            }
            issue.setDueDate(request.getDueDate());
        }
        if (request.getLatitude() != null) {
            issue.setLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            issue.setLongitude(request.getLongitude());
        }

        Issue updatedIssue = issueRepository.save(issue);

        return new IssueDetailsDTO(updatedIssue);
    }

    /**
     * Deletes an entire issue along with all its attachments and comments from both Cloudinary and the database.
     * This method handles the complete cleanup of all related entities including
     * nested comments and their attachments.
     *
     * @param issueId The ID of the issue to delete.
     */
    @Transactional
    public void deleteIssue(Long issueId) {
        User currentUser = SecurityUtils.getCurrentUser();
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", "id", issueId));

        boolean isReporter = Objects.equals(issue.getReporter().getId(), currentUser.getId());
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        if (!isReporter && !isAdmin) {
            throw new UnauthorizedException("You do not have permission to delete this issue.");
        }

        deleteIssueAttachmentsFromCloudinary(issue);

        deleteAllCommentsWithAttachments(issue);

        issueRepository.delete(issue);
    }

    /**
     * Deletes all attachments associated with an issue from Cloudinary storage.
     * This method ensures that all files are properly removed from Cloudinary.
     *
     * @param issue The issue whose attachments need to be deleted.
     */
    private void deleteIssueAttachmentsFromCloudinary(Issue issue) {
        Set<Attachment> issueAttachments = issue.getAttachments();
        if (issueAttachments != null && !issueAttachments.isEmpty()) {
            for (Attachment attachment : issueAttachments) {
                try {
                    cloudinaryService.deleteFile(attachment.getPublicId());
                } catch (IOException e) {
                    System.err.println("Failed to delete issue attachment from Cloudinary with public_id: " +
                            attachment.getPublicId() + ". Error: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Deletes all comments associated with an issue, including nested comments and their attachments.
     * This method handles the complete comment tree deletion with proper Cloudinary cleanup.
     *
     * @param issue The issue whose comments need to be deleted.
     */
    private void deleteAllCommentsWithAttachments(Issue issue) {
        Set<Comment> comments = issue.getComments();
        if (comments != null && !comments.isEmpty()) {
            List<Comment> topLevelComments = comments.stream()
                    .filter(comment -> comment.getParent() == null)
                    .collect(Collectors.toList());

            for (Comment comment : topLevelComments) {
                deleteCommentAndNestedAttachments(comment);
            }
        }
    }

    /**
     * Recursively deletes a comment and all its nested comments (replies) along with their attachments.
     * This method ensures that the entire comment tree is properly cleaned up from both database and Cloudinary.
     *
     * @param comment The comment to delete along with its nested structure.
     */
    private void deleteCommentAndNestedAttachments(Comment comment) {
        if (comment.getReplies() != null && !comment.getReplies().isEmpty()) {
            Set<Comment> repliesCopy = new HashSet<>(comment.getReplies());
            for (Comment reply : repliesCopy) {
                deleteCommentAndNestedAttachments(reply);
            }
        }

        deleteCommentAttachmentsFromCloudinary(comment);

        commentRepository.delete(comment);
    }

    /**
     * Deletes all attachments associated with a comment from Cloudinary storage.
     * This method handles the actual file deletion from Cloudinary.
     *
     * @param comment The comment whose attachments need to be deleted.
     */
    private void deleteCommentAttachmentsFromCloudinary(Comment comment) {
        Set<Attachment> attachments = comment.getAttachments();
        if (attachments != null && !attachments.isEmpty()) {
            for (Attachment attachment : attachments) {
                try {
                    cloudinaryService.deleteFile(attachment.getPublicId());
                } catch (IOException e) {
                    System.err.println("Failed to delete comment attachment from Cloudinary for publicId: " +
                            attachment.getPublicId() + ". Error: " + e.getMessage());
                }
            }
        }
    }
}