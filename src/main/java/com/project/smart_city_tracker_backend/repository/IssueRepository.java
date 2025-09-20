package com.project.smart_city_tracker_backend.repository;

import com.project.smart_city_tracker_backend.model.Issue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IssueRepository extends JpaRepository<Issue, Long> {

}