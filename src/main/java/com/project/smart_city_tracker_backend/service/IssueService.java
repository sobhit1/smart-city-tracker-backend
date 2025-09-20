package com.project.smart_city_tracker_backend.service;

import com.project.smart_city_tracker_backend.dto.CreateIssueRequest;
import com.project.smart_city_tracker_backend.exception.BadRequestException;
import com.project.smart_city_tracker_backend.model.*;
import com.project.smart_city_tracker_backend.repository.*;
import com.project.smart_city_tracker_backend.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class IssueService {

    private final IssueRepository issueRepository;
    private final CloudinaryService cloudinaryService;
    private final CategoryRepository categoryRepository;
    private final StatusRepository statusRepository;
    private final PriorityRepository priorityRepository;

    public IssueService(IssueRepository issueRepository, CloudinaryService cloudinaryService,
                        CategoryRepository categoryRepository, StatusRepository statusRepository, PriorityRepository priorityRepository) {
        this.issueRepository = issueRepository;
        this.cloudinaryService = cloudinaryService;
        this.categoryRepository = categoryRepository;
        this.statusRepository = statusRepository;
        this.priorityRepository = priorityRepository;
    }

    /**
     * Creates a new issue, uploads associated files to Cloudinary, and saves everything to the database.
     *
     * @param request The DTO containing the issue's details (title, description, etc.).
     * @param files   A non-empty list of files to be uploaded as attachments.
     * @return The newly created and saved Issue entity.
     */
    @Transactional
    public Issue createIssue(CreateIssueRequest request, List<MultipartFile> files) {
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

        return issueRepository.save(issue);
    }
}