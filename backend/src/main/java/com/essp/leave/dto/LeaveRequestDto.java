package com.essp.leave.dto;

import com.essp.leave.entity.LeaveRequest;
import com.essp.leave.entity.LeaveStatus;
import com.essp.leave.entity.LeaveType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record LeaveRequestDto(Long id, String employeeId, String employeeName, LeaveType leaveType,
                              LocalDate startDate, LocalDate endDate, int leaveDays, String reason,
                              LeaveStatus status, LocalDateTime submittedAt, LocalDateTime decidedAt,
                              String decisionMaker, String decisionRemarks) {
    public static LeaveRequestDto from(LeaveRequest request) {
        return new LeaveRequestDto(
                request.getId(),
                request.getEmployeeProfile().getEmployeeId(),
                request.getEmployeeProfile().getLegalName(),
                request.getLeaveType(),
                request.getStartDate(),
                request.getEndDate(),
                request.getLeaveDays(),
                request.getReason(),
                request.getStatus(),
                request.getSubmittedAt(),
                request.getDecidedAt(),
                request.getDecisionMaker() == null ? null : request.getDecisionMaker().getUsername(),
                request.getDecisionRemarks()
        );
    }
}
