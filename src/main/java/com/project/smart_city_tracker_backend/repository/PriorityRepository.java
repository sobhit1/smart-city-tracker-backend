package com.project.smart_city_tracker_backend.repository;

import com.project.smart_city_tracker_backend.model.Priority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PriorityRepository extends JpaRepository<Priority, Integer> {
    Optional<Priority> findByName(String name);
}