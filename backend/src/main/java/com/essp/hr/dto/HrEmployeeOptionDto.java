package com.essp.hr.dto;

import com.essp.employee.entity.EmployeeProfile;

public record HrEmployeeOptionDto(Long profileId, String employeeId, String legalName, String username) {
    public static HrEmployeeOptionDto from(EmployeeProfile profile) {
        return new HrEmployeeOptionDto(profile.getId(), profile.getEmployeeId(),
                profile.getLegalName(), profile.getUser().getUsername());
    }
}
