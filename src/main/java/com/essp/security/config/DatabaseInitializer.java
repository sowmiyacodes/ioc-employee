package com.essp.security.config;

import com.essp.security.entity.Role;
import com.essp.security.entity.User;
import com.essp.security.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DatabaseInitializer {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    @Bean
    public CommandLineRunner initDatabase(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.count() == 0) {
                log.info("Initializing default accounts for all 4 required roles...");

                // Password requirement: >= 12 characters as defined in requirements
                // Default test passwords (>=12 chars): Password1234!
                String defaultHash = passwordEncoder.encode("Password1234!");

                // 1. Employee
                User emp = new User("employee1", "employee@essp.local", defaultHash, Role.EMPLOYEE);
                userRepository.save(emp);

                // 2. Manager
                User mgr = new User("manager1", "manager@essp.local", defaultHash, Role.MANAGER);
                userRepository.save(mgr);

                // 3. HR Admin
                User hr = new User("hradmin1", "hradmin@essp.local", defaultHash, Role.HR_ADMIN);
                userRepository.save(hr);

                // 4. System Admin
                User admin = new User("sysadmin1", "sysadmin@essp.local", defaultHash, Role.SYSTEM_ADMIN);
                userRepository.save(admin);

                log.info("Sample accounts successfully seeded for EMPLOYEE, MANAGER, HR_ADMIN, and SYSTEM_ADMIN.");
            }
        };
    }
}
