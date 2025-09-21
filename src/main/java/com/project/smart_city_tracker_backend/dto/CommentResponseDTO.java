package com.project.smart_city_tracker_backend.dto;

import com.project.smart_city_tracker_backend.model.Comment;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponseDTO {

    private Long id;
    private String text;
    private Instant createdAt;
    private UserSummaryDTO author;
    private List<AttachmentDTO> attachments;
    private Long parentId;

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
    }

    public CommentResponseDTO(Comment comment) {
        this.id = comment.getId();
        this.text = comment.getText();
        this.createdAt = comment.getCreatedAt();

        if (comment.getAuthor() != null) {
            this.author = new UserSummaryDTO(comment.getAuthor().getId(), comment.getAuthor().getFullName());
        }

        if (comment.getParent() != null) {
            this.parentId = comment.getParent().getId();
        }

        if (comment.getAttachments() != null) {
            this.attachments = comment.getAttachments().stream()
                    .map(attachment -> new AttachmentDTO(
                            attachment.getId(),
                            attachment.getUrl(),
                            attachment.getFileName()
                    ))
                    .toList();
        } else {
            this.attachments = Collections.emptyList();
        }
    }
}