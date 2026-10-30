package com.essp.leave.controller;

import com.essp.leave.dto.*;
import com.essp.leave.entity.LeaveStatus;
import com.essp.leave.service.LeaveService;
import com.essp.security.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.util.List;

@RestController
@RequestMapping("/api/leaves")
@SecurityRequirement(name = "bearerAuth")
public class LeaveController {
    private final LeaveService leaveService;

    public LeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    @GetMapping("/balance")
    public ResponseEntity<ApiResponse<List<LeaveBalanceDto>>> getBalances(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok("Leave balances retrieved",
                leaveService.getBalances(authentication.getName())));
    }

    @PostMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<LeaveRequestDto>> createRequest(
            @Valid @RequestBody LeaveRequestCreateRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok("Leave request submitted",
                leaveService.createRequest(authentication.getName(), request)));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<LeaveRequestsPageDto>> getMyRequests(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) LeaveStatus status) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Page must be non-negative and size must be between 1 and 100");
        }
        return ResponseEntity.ok(ApiResponse.ok("Leave requests retrieved",
                leaveService.getMyRequests(authentication.getName(), page, size, status)));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<LeaveRequestsPageDto>> getPendingRequests(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Page must be non-negative and size must be between 1 and 100");
        }
        return ResponseEntity.ok(ApiResponse.ok("Pending leave requests retrieved",
                leaveService.getPendingRequests(authentication.getName(), page, size)));
    }

    @PatchMapping("/{id}/decision")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<LeaveRequestDto>> decide(
            @PathVariable Long id, @Valid @RequestBody LeaveDecisionRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok("Leave request decision recorded",
                leaveService.decide(authentication.getName(), id, request)));
    }

    @PutMapping("/balance")
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<LeaveBalanceDto>> setBalance(
            @Valid @RequestBody LeaveBalanceUpdateRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok("Leave balance configured",
                leaveService.setBalance(authentication.getName(), request)));
    }
}
