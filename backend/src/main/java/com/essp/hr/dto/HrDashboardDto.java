package com.essp.hr.dto;

public record HrDashboardDto(long totalEmployees, long activeEmployees, long employeesWithEntitlements,
                             long pendingLeaveRequests, long approvedLeaveRequests,
                             long rejectedLeaveRequests) {
}
