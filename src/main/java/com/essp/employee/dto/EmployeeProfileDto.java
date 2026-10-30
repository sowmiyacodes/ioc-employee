package com.essp.employee.dto;

import com.essp.employee.entity.EmployeeProfile;

import java.time.LocalDate;

public record EmployeeProfileDto(
        String employeeId, String legalName, String department, String designation,
        LocalDate joiningDate, String managerName, AddressDto address) {
    public static EmployeeProfileDto from(EmployeeProfile profile, AddressDto address) {
        return new EmployeeProfileDto(profile.getEmployeeId(), profile.getLegalName(),
                profile.getDepartment(), profile.getDesignation(), profile.getJoiningDate(),
                profile.getManager() == null ? null : profile.getManager().getUsername(), address);
    }
}
