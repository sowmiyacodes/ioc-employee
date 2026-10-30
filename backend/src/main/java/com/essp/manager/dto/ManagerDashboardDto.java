package com.essp.manager.dto;

public record ManagerDashboardDto(long teamSize, long pendingRequests,
                                  long approvedRequests, long rejectedRequests) {
}
