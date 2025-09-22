package com.project.smart_city_tracker_backend.service;

import com.project.smart_city_tracker_backend.dto.CategoryDTO;
import com.project.smart_city_tracker_backend.dto.PriorityDTO;
import com.project.smart_city_tracker_backend.dto.StatusDTO;
import com.project.smart_city_tracker_backend.repository.CategoryRepository;
import com.project.smart_city_tracker_backend.repository.PriorityRepository;
import com.project.smart_city_tracker_backend.repository.StatusRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LookupService {

    private final CategoryRepository categoryRepository;
    private final StatusRepository statusRepository;
    private final PriorityRepository priorityRepository;

    public LookupService(CategoryRepository categoryRepository,
                         StatusRepository statusRepository,
                         PriorityRepository priorityRepository) {
        this.categoryRepository = categoryRepository;
        this.statusRepository = statusRepository;
        this.priorityRepository = priorityRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryDTO> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(CategoryDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StatusDTO> getAllStatuses() {
        return statusRepository.findAll().stream()
                .map(StatusDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PriorityDTO> getAllPriorities() {
        return priorityRepository.findAll(Sort.by(Sort.Direction.DESC, "sortOrder")).stream()
                .map(PriorityDTO::new)
                .toList();
    }
}