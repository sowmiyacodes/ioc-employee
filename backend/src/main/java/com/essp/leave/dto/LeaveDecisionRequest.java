package com.essp.leave.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class LeaveDecisionRequest {
    public enum Action {
        APPROVE,
        REJECT
    }

    @NotNull
    private Action action;

    @Size(max = 1000)
    private String remarks;

    public Action getAction() { return action; }
    public void setAction(Action action) { this.action = action; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
