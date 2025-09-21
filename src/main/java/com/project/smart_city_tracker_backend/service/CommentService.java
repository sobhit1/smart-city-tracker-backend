package com.project.smart_city_tracker_backend.service;

import com.project.smart_city_tracker_backend.dto.CreateCommentRequest;
import com.project.smart_city_tracker_backend.dto.UpdateCommentRequest;
import com.project.smart_city_tracker_backend.exception.BadRequestException;
import com.project.smart_city_tracker_backend.exception.ResourceNotFoundException;
import com.project.smart_city_tracker_backend.exception.UnauthorizedException;
import com.project.smart_city_tracker_backend.model.Attachment;
import com.project.smart_city_tracker_backend.model.Comment;
import com.project.smart_city_tracker_backend.model.Issue;
import com.project.smart_city_tracker_backend.model.User;
import com.project.smart_city_tracker_backend.repository.AttachmentRepository;
import com.project.smart_city_tracker_backend.repository.CommentRepository;
import com.project.smart_city_tracker_backend.repository.IssueRepository;
import com.project.smart_city_tracker_backend.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
     *
     * @param issueId The ID of the issue to which the comment is being added.
     * @param request The DTO containing the comment's text and optional parentId.
     * @param files   An optional list of files to be attached to the comment.
     * @return The newly created and saved Comment entity.
     */
    @Transactional
    public Comment addComment(Long issueId, CreateCommentRequest request, List<MultipartFile> files) {
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

        return commentRepository.save(newComment);
    }

    /**
     * Updates the text of an existing comment.
     *
     * @param issueId   The ID of the parent issue.
     * @param commentId The ID of the comment to update.
     * @param request   The DTO containing the new text for the comment.
     * @return The updated and saved Comment entity.
     */
    @Transactional
    public Comment updateComment(Long issueId, Long commentId, UpdateCommentRequest request) {
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

        return commentRepository.save(comment);
    }

    /**
     * Deletes an existing comment.
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

        commentRepository.delete(comment);
    }

    /**
     * Adds one or more attachments to an existing comment.
     *
     * @param issueId   The ID of the parent issue (for validation).
     * @param commentId The ID of the comment to add attachments to.
     * @param files     The list of files to upload.
     * @return A list of the newly created Attachment entities.
     */
    @Transactional
    public List<Attachment> addAttachmentsToComment(Long issueId, Long commentId, List<MultipartFile> files) {
        User currentUser = SecurityUtils.getCurrentUser();

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));

        if (!Objects.equals(comment.getIssue().getId(), issueId)) {
            throw new BadRequestException("Comment does not belong to the specified issue.");
        }

        boolean isAuthor = Objects.equals(comment.getAuthor().getId(), currentUser.getId());
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        if (!isAuthor && !isAdmin) {
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
                throw new BadRequestException("Failed to upload file: " + file.getOriginalFilename(), e);
            }
        }
        
        return attachmentRepository.saveAll(newAttachments);
    }
}