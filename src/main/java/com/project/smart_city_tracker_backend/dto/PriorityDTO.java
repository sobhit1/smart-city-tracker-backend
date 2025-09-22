package com.project.smart_city_tracker_backend.dto;

import com.project.smart_city_tracker_backend.model.Priority;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PriorityDTO {
    private Integer id;
    private String name;
    private Integer sortOrder;

    public PriorityDTO(Priority priority) {
        this.id = priority.getId();
        this.name = priority.getName();
        this.sortOrder = priority.getSortOrder();
    }
}