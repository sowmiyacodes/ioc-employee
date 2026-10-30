package com.essp.admin.dto;

import com.essp.security.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record AdminUserUpdateRequest(
        @NotBlank @Email @Size(max = 100) String email,
        @NotNull Role role,
        @NotBlank @Size(max = 30) String employeeId,
        @NotBlank @Size(max = 150) String legalName,
        @NotBlank @Size(max = 100) String department,
        @NotBlank @Size(max = 100) String designation,
        @NotNull LocalDate joiningDate,
        Long managerId) {
}
