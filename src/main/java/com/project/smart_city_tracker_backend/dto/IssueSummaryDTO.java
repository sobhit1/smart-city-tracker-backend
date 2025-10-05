package com.project.smart_city_tracker_backend.dto;

import com.project.smart_city_tracker_backend.model.Issue;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IssueSummaryDTO {

    private Long id;
    private String title;
    private String category;
    private String status;
    private String priority;
    private Instant reportedAt;
    private UserSummaryDTO reporter;
    private UserSummaryDTO assignee;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSummaryDTO {
        private Long id;
        private String name;
    }

    public IssueSummaryDTO(Issue issue) {
        this.id = issue.getId();
        this.title = issue.getTitle();
        this.reportedAt = issue.getCreatedAt();

        if (issue.getCategory() != null) {
            this.category = issue.getCategory().getName();
        }
        if (issue.getStatus() != null) {
            this.status = issue.getStatus().getName();
        } else {
            this.status = "OPEN";
        }
        if (issue.getPriority() != null) {
            this.priority = issue.getPriority().getName();
        } else {
            this.priority = "Medium";
        }
        if (issue.getReporter() != null) {
            this.reporter = new UserSummaryDTO(issue.getReporter().getId(), issue.getReporter().getFullName());
        }
        if (issue.getAssignee() != null) {
            this.assignee = new UserSummaryDTO(issue.getAssignee().getId(), issue.getAssignee().getFullName());
        }
    }
}