package com.essp.security.config;

import com.essp.employee.entity.EmployeeProfile;
import com.essp.employee.repository.EmployeeProfileRepository;
import com.essp.security.entity.Role;
import com.essp.security.entity.User;
import com.essp.security.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

@Configuration
public class DatabaseInitializer {
    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    @Value("${app.seed-demo-data:false}")
    private boolean seedDemoData;

    @Value("${APP_DEMO_PASSWORD:}")
    private String demoPassword;

    @Value("${APP_BOOTSTRAP_ADMIN_USERNAME:}")
    private String adminUsername;
    @Value("${APP_BOOTSTRAP_ADMIN_EMAIL:}")
    private String adminEmail;
    @Value("${APP_BOOTSTRAP_ADMIN_PASSWORD:}")
    private String adminPassword;
    @Value("${APP_BOOTSTRAP_ADMIN_EMPLOYEE_ID:}")
    private String adminEmployeeId;
    @Value("${APP_BOOTSTRAP_ADMIN_NAME:}")
    private String adminName;
    @Value("${APP_BOOTSTRAP_ADMIN_DEPARTMENT:}")
    private String adminDepartment;
    @Value("${APP_BOOTSTRAP_ADMIN_DESIGNATION:}")
    private String adminDesignation;
    @Value("${APP_BOOTSTRAP_ADMIN_JOINING_DATE:}")
    private String adminJoiningDate;

    @Bean
    public CommandLineRunner initDatabase(UserRepository userRepository,
                                          EmployeeProfileRepository profileRepository,
                                          PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.count() != 0) return;
            if (seedDemoData) {
                seedDemoAccounts(userRepository, profileRepository, passwordEncoder);
            } else if (hasBootstrapConfiguration()) {
                createBootstrapAdministrator(userRepository, profileRepository, passwordEncoder);
            } else {
                log.warn("No user accounts were provisioned. Configure the initial administrator or explicitly enable demo seeding.");
            }
        };
    }

    private void seedDemoAccounts(UserRepository users, EmployeeProfileRepository profiles,
                                  PasswordEncoder passwordEncoder) {
        requirePassword(demoPassword, "APP_DEMO_PASSWORD");
        String hash = passwordEncoder.encode(demoPassword);
        User manager = new User("manager1", "manager@essp.local", hash, Role.MANAGER);
        User employee = new User("employee1", "employee@essp.local", hash, Role.EMPLOYEE);
        User hr = new User("hradmin1", "hradmin@essp.local", hash, Role.HR_ADMIN);
        User admin = new User("sysadmin1", "sysadmin@essp.local", hash, Role.SYSTEM_ADMIN);
        users.save(manager);
        users.save(employee);
        users.save(hr);
        users.save(admin);
        createProfile(profiles, manager, "MGR-0001", "Morgan Smith",
                "Engineering", "Engineering Manager", null, LocalDate.of(2022, 1, 10));
        createProfile(profiles, employee, "EMP-0001", "Alex Johnson",
                "Engineering", "Software Engineer", manager, LocalDate.of(2022, 1, 10));
        createProfile(profiles, hr, "HR-0001", "Taylor Brown",
                "Human Resources", "HR Administrator", null, LocalDate.of(2022, 1, 10));
        createProfile(profiles, admin, "ADM-0001", "Jordan Lee",
                "IT Administration", "System Administrator", null, LocalDate.of(2022, 1, 10));
        log.warn("Seeded fictional demo accounts because app.seed-demo-data is enabled.");
    }

    private void createBootstrapAdministrator(UserRepository users, EmployeeProfileRepository profiles,
                                              PasswordEncoder passwordEncoder) {
        requireComplete(adminUsername, "APP_BOOTSTRAP_ADMIN_USERNAME",
                adminEmail, "APP_BOOTSTRAP_ADMIN_EMAIL",
                adminEmployeeId, "APP_BOOTSTRAP_ADMIN_EMPLOYEE_ID",
                adminName, "APP_BOOTSTRAP_ADMIN_NAME",
                adminDepartment, "APP_BOOTSTRAP_ADMIN_DEPARTMENT",
                adminDesignation, "APP_BOOTSTRAP_ADMIN_DESIGNATION",
                adminJoiningDate, "APP_BOOTSTRAP_ADMIN_JOINING_DATE");
        requirePassword(adminPassword, "APP_BOOTSTRAP_ADMIN_PASSWORD");
        LocalDate joiningDate;
        try {
            joiningDate = LocalDate.parse(adminJoiningDate);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("APP_BOOTSTRAP_ADMIN_JOINING_DATE must use YYYY-MM-DD format", exception);
        }
        if (users.existsByUsername(adminUsername) || users.existsByEmail(adminEmail)
                || profiles.existsByEmployeeId(adminEmployeeId)) {
            throw new IllegalStateException("Initial administrator identifiers must be unique");
        }
        if (!adminEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalStateException("APP_BOOTSTRAP_ADMIN_EMAIL must be a valid email address");
        }
        User admin = users.save(new User(adminUsername.trim(), adminEmail.trim().toLowerCase(),
                passwordEncoder.encode(adminPassword), Role.SYSTEM_ADMIN));
        createProfile(profiles, admin, adminEmployeeId, adminName,
                adminDepartment, adminDesignation, null, joiningDate);
        log.info("Created initial SYSTEM_ADMIN account from environment configuration.");
    }

    private boolean hasBootstrapConfiguration() {
        return !adminUsername.isBlank() || !adminEmail.isBlank() || !adminPassword.isBlank()
                || !adminEmployeeId.isBlank() || !adminName.isBlank() || !adminDepartment.isBlank()
                || !adminDesignation.isBlank() || !adminJoiningDate.isBlank();
    }

    private void requireComplete(String... nameValues) {
        for (int index = 0; index < nameValues.length; index += 2) {
            if (nameValues[index] == null || nameValues[index].isBlank()) {
                throw new IllegalStateException(nameValues[index + 1] + " is required to bootstrap the administrator");
            }
        }
    }

    private void requirePassword(String password, String variableName) {
        if (password == null || password.length() < 12 || password.length() > 72) {
            throw new IllegalStateException(variableName + " must contain between 12 and 72 characters");
        }
    }

    private void createProfile(EmployeeProfileRepository repository, User user, String employeeId,
                               String legalName, String department, String designation, User manager,
                               LocalDate joiningDate) {
        EmployeeProfile profile = new EmployeeProfile();
        profile.setUser(user);
        profile.setEmployeeId(employeeId);
        profile.setLegalName(legalName);
        profile.setDepartment(department);
        profile.setDesignation(designation);
        profile.setJoiningDate(joiningDate);
        profile.setManager(manager);
        user.setEmployeeProfile(profile);
        repository.save(profile);
    }
}
