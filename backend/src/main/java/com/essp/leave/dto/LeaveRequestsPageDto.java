package com.essp.leave.dto;

import com.essp.leave.entity.LeaveRequest;
import org.springframework.data.domain.Page;

import java.util.List;

public record LeaveRequestsPageDto(List<LeaveRequestDto> content, int page, int size,
                                   long totalElements, int totalPages) {
    public static LeaveRequestsPageDto from(Page<LeaveRequest> requests) {
        return new LeaveRequestsPageDto(
                requests.getContent().stream().map(LeaveRequestDto::from).toList(),
                requests.getNumber(),
                requests.getSize(),
                requests.getTotalElements(),
                requests.getTotalPages()
        );
    }
}
