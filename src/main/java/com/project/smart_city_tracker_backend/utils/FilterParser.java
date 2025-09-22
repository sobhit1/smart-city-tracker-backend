package com.project.smart_city_tracker_backend.utils;

import com.project.smart_city_tracker_backend.exception.BadRequestException;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class FilterParser {

    @Getter
    @AllArgsConstructor
    public static class FilterCriteria {
        private String field;
        private String operator;
        private String value;
    }

    public static List<FilterCriteria> parse(List<String> filters) {
        if (filters == null || filters.isEmpty()) {
            return Collections.emptyList();
        }
        return filters.stream()
                .map(FilterParser::parseSingle)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private static FilterCriteria parseSingle(String filter) {
        if (filter == null || filter.isBlank()) {
            return null;
        }
        String[] parts = filter.split(":", 3);
        if (parts.length != 3) {
            throw new BadRequestException("Invalid filter format. Expected format is field:operator:value, but got: " + filter);
        }
        return new FilterCriteria(parts[0], parts[1], parts[2]);
    }
}