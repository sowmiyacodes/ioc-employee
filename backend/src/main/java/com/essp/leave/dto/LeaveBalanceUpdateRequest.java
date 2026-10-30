package com.essp.leave.dto;

import com.essp.leave.entity.LeaveType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;

public class LeaveBalanceUpdateRequest {
    @NotNull
    @Min(1)
    private Long employeeProfileId;

    @NotNull
    private LeaveType leaveType;

    @NotNull
    @Min(0)
    @Max(365)
    private Integer entitledDays;

    public Long getEmployeeProfileId() { return employeeProfileId; }
    public void setEmployeeProfileId(Long employeeProfileId) { this.employeeProfileId = employeeProfileId; }
    public LeaveType getLeaveType() { return leaveType; }
    public void setLeaveType(LeaveType leaveType) { this.leaveType = leaveType; }
    public Integer getEntitledDays() { return entitledDays; }
    public void setEntitledDays(Integer entitledDays) { this.entitledDays = entitledDays; }
}
