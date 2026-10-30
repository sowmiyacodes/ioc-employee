package com.essp.hr.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record HrEmployeePageDto(
        List<HrEmployeeOptionDto> content, int page, int size, long totalElements, int totalPages) {
    public static HrEmployeePageDto from(Page<com.essp.employee.entity.EmployeeProfile> profiles) {
        return new HrEmployeePageDto(profiles.getContent().stream().map(HrEmployeeOptionDto::from).toList(),
                profiles.getNumber(), profiles.getSize(), profiles.getTotalElements(), profiles.getTotalPages());
    }
}
