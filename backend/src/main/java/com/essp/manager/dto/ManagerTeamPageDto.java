package com.essp.manager.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record ManagerTeamPageDto(
        List<ManagerTeamMemberDto> content, int page, int size, long totalElements, int totalPages) {
    public static ManagerTeamPageDto from(Page<com.essp.employee.entity.EmployeeProfile> profiles) {
        return new ManagerTeamPageDto(profiles.getContent().stream().map(ManagerTeamMemberDto::from).toList(),
                profiles.getNumber(), profiles.getSize(), profiles.getTotalElements(), profiles.getTotalPages());
    }
}
