package com.essp.manager.dto;

import com.essp.employee.entity.EmployeeProfile;

public record ManagerTeamMemberDto(Long profileId, String employeeId, String name,
                                   String department, String designation, String email) {
    public static ManagerTeamMemberDto from(EmployeeProfile profile) {
        return new ManagerTeamMemberDto(profile.getId(), profile.getEmployeeId(), profile.getLegalName(),
                profile.getDepartment(), profile.getDesignation(), profile.getUser().getEmail());
    }
}
