package com.project.smart_city_tracker_backend;

import com.project.smart_city_tracker_backend.model.Role;
import com.project.smart_city_tracker_backend.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SmartCityTrackerBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartCityTrackerBackendApplication.class, args);
    }

    /**
     * A CommandLineRunner bean that seeds the database with initial roles upon application startup.
     *
     * @param roleRepository The repository for accessing Role data.
     * @return A CommandLineRunner instance that executes the seeding logic.
     */
    @Bean
	CommandLineRunner run(RoleRepository roleRepository) {
		return args -> {
			addRoleIfNotExists(roleRepository, "ROLE_CITIZEN");
			addRoleIfNotExists(roleRepository, "ROLE_STAFF");
			addRoleIfNotExists(roleRepository, "ROLE_ADMIN");
		};
	}

	private void addRoleIfNotExists(RoleRepository roleRepository, String roleName) {
		roleRepository.findByName(roleName)
			.ifPresentOrElse(
				role -> {},
				() -> roleRepository.save(new Role(null, roleName))
			);
	}
}