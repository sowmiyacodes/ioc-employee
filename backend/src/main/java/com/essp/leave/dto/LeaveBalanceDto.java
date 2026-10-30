package com.essp.leave.dto;

import com.essp.leave.entity.LeaveBalance;
import com.essp.leave.entity.LeaveType;

public record LeaveBalanceDto(LeaveType leaveType, int entitledDays, int usedDays,
                              long pendingDays, long availableDays) {
    public static LeaveBalanceDto from(LeaveBalance balance, long pendingDays) {
        return new LeaveBalanceDto(
                balance.getLeaveType(),
                balance.getEntitledDays(),
                balance.getUsedDays(),
                pendingDays,
                Math.max(0L, (long) balance.getEntitledDays() - balance.getUsedDays() - pendingDays)
        );
    }
}
