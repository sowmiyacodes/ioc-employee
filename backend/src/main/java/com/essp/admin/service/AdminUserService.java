package com.essp.admin.service;

import com.essp.admin.dto.*;
import com.essp.employee.entity.EmployeeProfile;
import com.essp.employee.repository.EmployeeProfileRepository;
import com.essp.security.audit.SecurityAuditService;
import com.essp.security.entity.Role;
import com.essp.security.entity.User;
import com.essp.security.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.Set;

@Service
public class AdminUserService {
    private final UserRepository userRepository;
    private final EmployeeProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityAuditService auditService;

    public AdminUserService(UserRepository userRepository, EmployeeProfileRepository profileRepository,
                            PasswordEncoder passwordEncoder, SecurityAuditService auditService) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public AdminUserPageDto list(String query, Role role, Boolean active, int page, int size,
                                 String sortBy, String direction) {
        String sortColumn = switch (sortBy) {
            case "username", "email", "role", "active", "employeeId", "name" ->
                    switch (sortBy) {
                        case "employeeId" -> "employeeProfile.employeeId";
                        case "name" -> "employeeProfile.legalName";
                        default -> sortBy;
                    };
            default -> "id";
        };
        Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction)
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        Page<User> users = userRepository.searchUsers(
                query == null || query.isBlank() ? null : query.trim(), role, active,
                PageRequest.of(page, size, Sort.by(sortDirection, sortColumn)));
        return AdminUserPageDto.from(users);
    }

    @Transactional(readOnly = true)
    public AdminUserDto get(long id) {
        return AdminUserDto.from(findUser(id));
    }

    @Transactional
    public AdminUserDto create(String actor, AdminUserCreateRequest input) {
        if (input.managerId() != null && input.role() != Role.EMPLOYEE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only employee accounts can be assigned a reporting manager");
        }
        String username = input.username().trim();
        String email = input.email().trim().toLowerCase();
        if (userRepository.existsByUsername(username) || userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username or email is already in use");
        }
        if (profileRepository.existsByEmployeeId(input.employeeId().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Employee ID is already in use");
        }

        User user = new User(username, email, passwordEncoder.encode(input.password()), input.role());
        user.setActive(true);
        user.setAccountNonLocked(true);
        User saved = userRepository.save(user);
        EmployeeProfile profile = new EmployeeProfile();
        profile.setUser(saved);
        profile.setEmployeeId(input.employeeId().trim());
        profile.setLegalName(input.legalName().trim());
        profile.setDepartment(input.department().trim());
        profile.setDesignation(input.designation().trim());
        profile.setJoiningDate(input.joiningDate());
        profile.setManager(resolveManager(input.managerId(), saved.getId()));
        saved.setEmployeeProfile(profile);
        profileRepository.save(profile);
        auditService.logAdministrationEvent(actor, "USER_CREATED", saved.getId(),
                "role=" + saved.getRole().name());
        return AdminUserDto.from(saved);
    }

    @Transactional
    public AdminUserDto update(String actor, long id, AdminUserUpdateRequest input) {
        if (input.managerId() != null && input.role() != Role.EMPLOYEE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only employee accounts can be assigned a reporting manager");
        }
        User user = findUser(id);
        if (userRepository.existsByEmailAndIdNot(input.email().trim().toLowerCase(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already in use");
        }
        EmployeeProfile profile = user.getEmployeeProfile();
        if (profile == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The user has no employee profile");
        }
        if (profileRepository.existsByEmployeeIdAndIdNot(input.employeeId().trim(), profile.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Employee ID is already in use");
        }
        if (id == userRepository.findByUsername(actor).map(User::getId).orElse(-1L)
                && input.role() != user.getRole()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You cannot change your own role");
        }
        if (user.getRole() == Role.SYSTEM_ADMIN && input.role() != Role.SYSTEM_ADMIN
                && user.isActive() && userRepository.countByRoleAndActiveTrue(Role.SYSTEM_ADMIN) <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "The last active system administrator cannot be demoted");
        }
        if (user.getRole() == Role.MANAGER && input.role() != Role.MANAGER
                && profileRepository.existsByManagerId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Reassign this manager's employees before changing their role");
        }
        user.setEmail(input.email().trim().toLowerCase());
        user.setRole(input.role());
        profile.setEmployeeId(input.employeeId().trim());
        profile.setLegalName(input.legalName().trim());
        profile.setDepartment(input.department().trim());
        profile.setDesignation(input.designation().trim());
        profile.setJoiningDate(input.joiningDate());
        profile.setManager(resolveManager(input.managerId(), user.getId()));
        userRepository.save(user);
        profileRepository.save(profile);
        auditService.logAdministrationEvent(actor, "USER_UPDATED", id,
                "role=" + user.getRole().name() + ";managerId=" + input.managerId());
        return AdminUserDto.from(user);
    }

    @Transactional
    public AdminUserDto setActive(String actor, long id, boolean active) {
        User user = findUser(id);
        if (!active && user.getUsername().equals(actor)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You cannot deactivate your own account");
        }
        if (!active && user.isActive() && user.getRole() == Role.SYSTEM_ADMIN
                && userRepository.countByRoleAndActiveTrue(Role.SYSTEM_ADMIN) <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "The last active system administrator cannot be deactivated");
        }
        if (!active && user.getRole() == Role.MANAGER && profileRepository.existsByManagerId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Reassign this manager's employees before deactivating the account");
        }
        user.setActive(active);
        userRepository.save(user);
        auditService.logAdministrationEvent(actor, active ? "USER_ACTIVATED" : "USER_DEACTIVATED",
                id, "username=" + user.getUsername());
        return AdminUserDto.from(user);
    }

    @Transactional(readOnly = true)
    public AdminDashboardDto dashboard() {
        return new AdminDashboardDto(userRepository.count(), userRepository.countByActiveTrue(),
                userRepository.countByActiveFalse(), userRepository.countByRole(Role.EMPLOYEE),
                userRepository.countByRole(Role.MANAGER), userRepository.countByRole(Role.HR_ADMIN),
                userRepository.countByRole(Role.SYSTEM_ADMIN));
    }

    private User resolveManager(Long managerId, Long userId) {
        if (managerId == null) return null;
        if (managerId.equals(userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A user cannot report to themselves");
        }
        User manager = findUser(managerId);
        if (!manager.isActive() || manager.getRole() != Role.MANAGER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Reporting manager must be an active user with the MANAGER role");
        }
        Set<Long> visited = new HashSet<>();
        User current = manager;
        while (current != null && visited.add(current.getId())) {
            if (current.getId().equals(userId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Reporting manager assignment would create a circular reporting relationship");
            }
            EmployeeProfile currentProfile = current.getEmployeeProfile();
            current = currentProfile == null ? null : currentProfile.getManager();
        }
        return manager;
    }

    private User findUser(long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User was not found"));
    }
}
