package com.project.smart_city_tracker_backend.service;

import com.project.smart_city_tracker_backend.dto.UserDTO;
import com.project.smart_city_tracker_backend.model.User;
import com.project.smart_city_tracker_backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Stream;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Fetches a list of all users, with an option to filter by role.
     *
     * @param role An optional role name (e.g., "STAFF") to filter the user list.
     * @return A list of UserDTOs.
     */
    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers(String role) {
        Stream<User> usersStream = userRepository.findAll().stream();

        if (StringUtils.hasText(role)) {
            String roleName = "ROLE_" + role.toUpperCase();
            usersStream = usersStream.filter(user -> user.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals(roleName)));
        }

        return usersStream
                .map(UserDTO::new)
                .toList();
    }
}