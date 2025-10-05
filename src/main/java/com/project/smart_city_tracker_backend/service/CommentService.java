package com.project.smart_city_tracker_backend.service;

import com.project.smart_city_tracker_backend.dto.*;
import com.project.smart_city_tracker_backend.exception.*;
import com.project.smart_city_tracker_backend.model.*;
import com.project.smart_city_tracker_backend.repository.*;
import com.project.smart_city_tracker_backend.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final IssueRepository issueRepository;
    private final CloudinaryService cloudinaryService;
    private final AttachmentRepository attachmentRepository;

    public CommentService(CommentRepository commentRepository,
                          IssueRepository issueRepository,
                          CloudinaryService cloudinaryService,
                          AttachmentRepository attachmentRepository) {
        this.commentRepository = commentRepository;
        this.issueRepository = issueRepository;
        this.cloudinaryService = cloudinaryService;
        this.attachmentRepository = attachmentRepository;
    }

    /**
     * Creates a new comment, handles replies, uploads attachments, and saves it to the database.
     * Any authenticated user can create a comment.
     *
     * @param issueId The ID of the issue to which the comment is being added.
     * @param request The DTO containing the comment's text and optional parentId.
     * @param files   An optional list of files to be attached to the comment.
     * @return The newly created and saved Comment entity.
     */
    @Transactional
    public CommentResponseDTO addComment(Long issueId, CreateCommentRequest request, List<MultipartFile> files) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", "id", issueId));

        User author = SecurityUtils.getCurrentUser();

        Comment newComment = new Comment();
        newComment.setText(request.getText());
        newComment.setIssue(issue);
        newComment.setAuthor(author);

        if (request.getParentId() != null) {
            Comment parentComment = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent Comment", "id", request.getParentId()));
            parentComment.addReply(newComment);
        }

        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                try {
                    Map<String, String> uploadResult = cloudinaryService.uploadFile(file);

                    Attachment attachment = new Attachment(
                            uploadResult.get("url"),
                            uploadResult.get("publicId"),
                            file.getOriginalFilename(),
                            file.getContentType(),
                            newComment
                    );
                    
                    newComment.addAttachment(attachment);

                } catch (IOException e) {
                    throw new BadRequestException("Failed to upload file for comment: " + file.getOriginalFilename(), e);
                }
            }
        }
        
        Comment savedComment = commentRepository.save(newComment);
        return new CommentResponseDTO(savedComment);
    }

    /**
     * Updates the text of an existing comment.
     * Only the comment author can update their own comment.
     *
     * @param issueId   The ID of the parent issue.
     * @param commentId The ID of the comment to update.
     * @param request   The DTO containing the new text for the comment.
     * @return The updated and saved Comment entity.
     */
    @Transactional
    public CommentResponseDTO updateComment(Long issueId, Long commentId, UpdateCommentRequest request) {
        User currentUser = SecurityUtils.getCurrentUser();

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));

        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", "id", issueId));

        if (!Objects.equals(comment.getIssue().getId(), issue.getId())) {
            throw new BadRequestException("Comment does not belong to the specified issue.");
        }

        if (!Objects.equals(comment.getAuthor().getId(), currentUser.getId())) {
            throw new UnauthorizedException("You are not authorized to edit this comment.");
        }

        comment.setText(request.getText());
        
        Comment updatedComment = commentRepository.save(comment);
        return new CommentResponseDTO(updatedComment);
    }

    /**
     * Deletes an existing comment and all its attachments from Cloudinary and the database.
     * This method handles nested comments (replies) and their attachments recursively.
     * The comment author can delete their own comment. Admins can delete any comment.
     *
     * @param issueId   The ID of the parent issue.
     * @param commentId The ID of the comment to delete.
     */
    @Transactional
    public void deleteComment(Long issueId, Long commentId) {
        User currentUser = SecurityUtils.getCurrentUser();

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));

        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", "id", issueId));

        if (!Objects.equals(comment.getIssue().getId(), issue.getId())) {
            throw new BadRequestException("Comment does not belong to the specified issue.");
        }

        boolean isAuthor = Objects.equals(comment.getAuthor().getId(), currentUser.getId());
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));

        if (!isAuthor && !isAdmin) {
            throw new UnauthorizedException("You are not authorized to delete this comment.");
        }

        deleteCommentAndNestedAttachments(comment);
    }

    /**
     * Recursively deletes a comment and all its nested comments (replies) along with their attachments.
     * This ensures that when a parent comment is deleted, all child comments and their attachments are also deleted.
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
     * Deletes all attachments associated with a comment from both Cloudinary and the database.
     * This method handles the actual file deletion from Cloudinary storage.
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
                    System.err.println("Failed to delete file from Cloudinary for publicId: " + 
                            attachment.getPublicId() + ". Error: " + e.getMessage());
                }
            }
            attachments.clear();
        }
    }

    /**
     * Adds one or more attachments to an existing comment.
     * Only the comment author can add attachments.
     *
     * @param issueId   The ID of the parent issue (for validation).
     * @param commentId The ID of the comment to add attachments to.
     * @param files     The list of files to upload.
     * @return A list of the newly created Attachment entities.
     */
    @Transactional
    public List<AttachmentResponseDTO> addAttachmentsToComment(Long issueId, Long commentId, List<MultipartFile> files) {
        User currentUser = SecurityUtils.getCurrentUser();

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));

        if (!Objects.equals(comment.getIssue().getId(), issueId)) {
            throw new BadRequestException("Comment does not belong to the specified issue.");
        }

        boolean isAuthor = Objects.equals(comment.getAuthor().getId(), currentUser.getId());

        if (!isAuthor) {
            throw new UnauthorizedException("You do not have permission to add attachments to this comment.");
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
                        comment
                );
                newAttachments.add(attachment);
            } catch (IOException e) {
                throw new BadRequestException("Failed to upload file for comment: " + file.getOriginalFilename(), e);
            }
        }
        
        List<Attachment> savedAttachments = attachmentRepository.saveAll(newAttachments);
        
        return savedAttachments.stream()
                .map(AttachmentResponseDTO::new)
                .collect(Collectors.toList());
    }
}