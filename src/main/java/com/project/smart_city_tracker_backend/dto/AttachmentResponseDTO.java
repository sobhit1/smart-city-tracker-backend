package com.project.smart_city_tracker_backend.dto;

import com.project.smart_city_tracker_backend.model.Attachment;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentResponseDTO {

    private Long id;
    private String url;
    private String fileName;
    private String fileType;
    private Instant createdAt;

    private Long issueId;

    private Long commentId;

    public AttachmentResponseDTO(Attachment attachment) {
        this.id = attachment.getId();
        this.url = attachment.getUrl();
        this.fileName = attachment.getFileName();
        this.fileType = attachment.getFileType();
        this.createdAt = attachment.getCreatedAt();

        if (attachment.getIssue() != null) {
            this.issueId = attachment.getIssue().getId();
            this.commentId = null;
        } else if (attachment.getComment() != null) {
            this.commentId = attachment.getComment().getId();
            this.issueId = attachment.getComment().getIssue().getId();
        }
    }
}