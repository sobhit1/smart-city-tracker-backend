package com.project.smart_city_tracker_backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "attachments")
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "url", nullable = false, length = 512)
    private String url;

    @Column(name = "public_id", nullable = false, unique = true)
    private String publicId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_type", nullable = false)
    private String fileType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id", nullable = true)
    @JsonIgnore
    private Issue issue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = true)
    @JsonIgnore
    private Comment comment;

    public Attachment(String url, String publicId, String fileName, String fileType, Issue issue) {
        this.url = url;
        this.publicId = publicId;
        this.fileName = fileName;
        this.fileType = fileType;
        this.issue = issue;
    }

    public Attachment(String url, String publicId, String fileName, String fileType, Comment comment) {
        this.url = url;
        this.publicId = publicId;
        this.fileName = fileName;
        this.fileType = fileType;
        this.comment = comment;
    }
}