package com.essp.admin.dto;

import com.essp.employee.entity.EmployeeProfile;
import com.essp.security.entity.User;

import java.time.LocalDate;

public record AdminUserDto(
        Long id, String username, String email, String role, boolean active,
        String employeeId, String legalName, String department, String designation,
        LocalDate joiningDate, Long managerId, String managerName) {

    public static AdminUserDto from(User user) {
        EmployeeProfile profile = user.getEmployeeProfile();
        return new AdminUserDto(
                user.getId(), user.getUsername(), user.getEmail(), user.getRole().name(), user.isActive(),
                profile == null ? null : profile.getEmployeeId(),
                profile == null ? null : profile.getLegalName(),
                profile == null ? null : profile.getDepartment(),
                profile == null ? null : profile.getDesignation(),
                profile == null ? null : profile.getJoiningDate(),
                profile == null || profile.getManager() == null ? null : profile.getManager().getId(),
                profile == null || profile.getManager() == null ? null
                        : profile.getManager().getEmployeeProfile() == null
                        ? profile.getManager().getUsername()
                        : profile.getManager().getEmployeeProfile().getLegalName());
    }
}
