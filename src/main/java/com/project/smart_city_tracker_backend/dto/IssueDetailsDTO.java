package com.project.smart_city_tracker_backend.dto;

import com.project.smart_city_tracker_backend.model.Comment;
import com.project.smart_city_tracker_backend.model.Issue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Comparator;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IssueDetailsDTO {

    private Long id;
    private String title;
    private String description;
    private String category;
    private String status;
    private String priority;
    private Double latitude;
    private Double longitude;
    private Instant createdAt;
    private Instant updatedAt;
    private UserSummaryDTO reporter;
    private UserSummaryDTO assignee;
    private Instant startDate;
    private Instant dueDate;
    private List<AttachmentDTO> attachments;
    private List<CommentDTO> comments;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSummaryDTO {
        private Long id;
        private String name;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttachmentDTO {
        private Long id;
        private String url;
        private String fileName;
        private String fileType;
        private Instant createdAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommentDTO {
        private Long id;
        private String text;
        private Instant createdAt;
        private UserSummaryDTO author;
        private List<AttachmentDTO> attachments;
        private Long parentId;
    }

    public IssueDetailsDTO(Issue issue) {
        this.id = issue.getId();
        this.title = issue.getTitle();
        this.description = issue.getDescription();
        this.latitude = issue.getLatitude();
        this.longitude = issue.getLongitude();
        this.createdAt = issue.getCreatedAt();
        this.updatedAt = issue.getUpdatedAt();

        if (issue.getCategory() != null) {
            this.category = issue.getCategory().getName();
        }
        if (issue.getStatus() != null) {
            this.status = issue.getStatus().getName();
        }
        if (issue.getPriority() != null) {
            this.priority = issue.getPriority().getName();
        }
        if (issue.getReporter() != null) {
            this.reporter = new UserSummaryDTO(issue.getReporter().getId(), issue.getReporter().getFullName());
        }
        if (issue.getAssignee() != null) {
            this.assignee = new UserSummaryDTO(issue.getAssignee().getId(), issue.getAssignee().getFullName());
        }

        this.startDate = issue.getStartDate();
        this.dueDate = issue.getDueDate();

        if (issue.getAttachments() != null) {
            this.attachments = issue.getAttachments().stream()
                    .map(attachment -> new AttachmentDTO(
                            attachment.getId(),
                            attachment.getUrl(),
                            attachment.getFileName(),
                            attachment.getFileType(),
                            attachment.getCreatedAt()))
                    .toList();
        } else {
            this.attachments = Collections.emptyList();
        }
        
        if (issue.getComments() != null) {
            this.comments = issue.getComments().stream()
                    .sorted(Comparator.comparing(Comment::getCreatedAt))
                    .map(comment -> {
                        List<AttachmentDTO> commentAttachments = comment.getAttachments().stream()
                            .map(att -> new AttachmentDTO(
                                att.getId(),
                                att.getUrl(),
                                att.getFileName(),
                                att.getFileType(),
                                att.getCreatedAt()
                            ))
                            .toList();

                        return new CommentDTO(
                            comment.getId(),
                            comment.getText(),
                            comment.getCreatedAt(),
                            new UserSummaryDTO(comment.getAuthor().getId(), comment.getAuthor().getFullName()),
                            commentAttachments,
                            comment.getParent() != null ? comment.getParent().getId() : null
                        );
                    })
                    .toList();
        } else {
            this.comments = Collections.emptyList();
        }
    }
}