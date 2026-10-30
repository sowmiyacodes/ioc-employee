package com.essp.admin.dto;

public record AdminDashboardDto(
        long totalUsers, long activeUsers, long inactiveUsers,
        long employees, long managers, long hrAdministrators, long systemAdministrators) {
}
