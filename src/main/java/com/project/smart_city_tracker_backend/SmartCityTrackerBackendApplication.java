package com.project.smart_city_tracker_backend;

import com.project.smart_city_tracker_backend.model.Category;
import com.project.smart_city_tracker_backend.model.Priority;
import com.project.smart_city_tracker_backend.model.Role;
import com.project.smart_city_tracker_backend.model.Status;
import com.project.smart_city_tracker_backend.repository.CategoryRepository;
import com.project.smart_city_tracker_backend.repository.PriorityRepository;
import com.project.smart_city_tracker_backend.repository.RoleRepository;
import com.project.smart_city_tracker_backend.repository.StatusRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootApplication
public class SmartCityTrackerBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartCityTrackerBackendApplication.class, args);
    }

    @Bean
    CommandLineRunner run(RoleRepository roleRepository,
            CategoryRepository categoryRepository,
            StatusRepository statusRepository,
            PriorityRepository priorityRepository) {
        return args -> {
            if (roleRepository.count() == 0) {
                roleRepository.saveAll(List.of(
                        new Role(null, "ROLE_CITIZEN"),
                        new Role(null, "ROLE_STAFF"),
                        new Role(null, "ROLE_ADMIN")));
            }
            if (categoryRepository.count() == 0) {
                categoryRepository.saveAll(List.of(
                        new Category(null, "Roads"),
                        new Category(null, "Waste Management"),
                        new Category(null, "Streetlights"),
                        new Category(null, "Water Supply"),
                        new Category(null, "Parks & Trees"),
                        new Category(null, "Other")));
            }
            if (statusRepository.count() == 0) {
                statusRepository.saveAll(List.of(
                        new Status(null, "OPEN"),
                        new Status(null, "IN_PROGRESS"),
                        new Status(null, "RESOLVED")));
            }
            if (priorityRepository.count() == 0) {
                priorityRepository.saveAll(List.of(
                    new Priority(null, "Highest", 5),
                    new Priority(null, "High", 4),
                    new Priority(null, "Medium", 3),
                    new Priority(null, "Low", 2),
                    new Priority(null, "Lowest", 1)
                ));
            }
        };
    }
}