package com.project.smart_city_tracker_backend.dto;

import com.project.smart_city_tracker_backend.model.Status;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StatusDTO {
    private Integer id;
    private String name;

    public StatusDTO(Status status) {
        this.id = status.getId();
        this.name = status.getName();
    }
}