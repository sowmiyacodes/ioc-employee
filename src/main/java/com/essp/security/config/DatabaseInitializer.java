package com.essp.security.config;

<<<<<<< HEAD
=======
import com.essp.employee.entity.EmployeeProfile;
import com.essp.employee.repository.EmployeeProfileRepository;
>>>>>>> 5d95ee7 (implemented employee profile and address management)
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
<<<<<<< HEAD
    public CommandLineRunner initDatabase(UserRepository userRepository, PasswordEncoder passwordEncoder) {
=======
    public CommandLineRunner initDatabase(UserRepository userRepository,
                                          EmployeeProfileRepository profileRepository,
                                          PasswordEncoder passwordEncoder) {
>>>>>>> 5d95ee7 (implemented employee profile and address management)
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

<<<<<<< HEAD
=======
                createProfile(profileRepository, emp, "EMP-0001", "Alex Johnson",
                        "Engineering", "Software Engineer", null);
                createProfile(profileRepository, mgr, "MGR-0001", "Morgan Smith",
                        "Engineering", "Engineering Manager", mgr);
                createProfile(profileRepository, hr, "HR-0001", "Taylor Brown",
                        "Human Resources", "HR Administrator", null);
                createProfile(profileRepository, admin, "ADM-0001", "Jordan Lee",
                        "IT Administration", "System Administrator", null);

>>>>>>> 5d95ee7 (implemented employee profile and address management)
                log.info("Sample accounts successfully seeded for EMPLOYEE, MANAGER, HR_ADMIN, and SYSTEM_ADMIN.");
            }
        };
    }
<<<<<<< HEAD
=======

    private void createProfile(EmployeeProfileRepository repository, User user, String employeeId,
                               String legalName, String department, String designation, User manager) {
        EmployeeProfile profile = new EmployeeProfile();
        profile.setUser(user);
        profile.setEmployeeId(employeeId);
        profile.setLegalName(legalName);
        profile.setDepartment(department);
        profile.setDesignation(designation);
        profile.setJoiningDate(java.time.LocalDate.of(2022, 1, 10));
        profile.setManager(manager);
        user.setEmployeeProfile(profile);
        repository.save(profile);
    }
>>>>>>> 5d95ee7 (implemented employee profile and address management)
}
