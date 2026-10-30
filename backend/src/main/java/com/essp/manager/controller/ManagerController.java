package com.essp.manager.controller;

import com.essp.leave.dto.LeaveRequestsPageDto;
import com.essp.leave.entity.LeaveStatus;
import com.essp.leave.repository.LeaveRequestRepository;
import com.essp.manager.dto.ManagerDashboardDto;
import com.essp.manager.dto.ManagerTeamMemberDto;
import com.essp.manager.dto.ManagerTeamPageDto;
import com.essp.employee.repository.EmployeeProfileRepository;
import com.essp.security.dto.ApiResponse;
import com.essp.security.entity.User;
import com.essp.security.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/manager")
@PreAuthorize("hasRole('MANAGER')")
@Transactional(readOnly = true)
public class ManagerController {
    private final EmployeeProfileRepository profileRepository;
    private final LeaveRequestRepository requestRepository;
    private final UserRepository userRepository;

    public ManagerController(EmployeeProfileRepository profileRepository,
                             LeaveRequestRepository requestRepository, UserRepository userRepository) {
        this.profileRepository = profileRepository;
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/team")
    public ResponseEntity<ApiResponse<ManagerTeamPageDto>> team(
            Authentication authentication, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        validatePage(page, size);
        User manager = findManager(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok("Reporting employees retrieved",
                ManagerTeamPageDto.from(profileRepository.findByManagerIdAndUserActiveTrueOrderByLegalNameAsc(
                        manager.getId(), PageRequest.of(page, size, Sort.by("legalName").ascending())))));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<ManagerDashboardDto>> dashboard(Authentication authentication) {
        User manager = findManager(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok("Manager dashboard loaded", new ManagerDashboardDto(
                profileRepository.countByManagerIdAndUserActiveTrue(manager.getId()),
                requestRepository.countByEmployeeProfileManagerIdAndStatus(manager.getId(), LeaveStatus.PENDING),
                requestRepository.countByEmployeeProfileManagerIdAndStatus(manager.getId(), LeaveStatus.APPROVED),
                requestRepository.countByEmployeeProfileManagerIdAndStatus(manager.getId(), LeaveStatus.REJECTED))));
    }

    @GetMapping("/team/leave-requests")
    public ResponseEntity<ApiResponse<LeaveRequestsPageDto>> teamLeaveRequests(
            Authentication authentication, @RequestParam(required = false) LeaveStatus status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Page must be non-negative and size must be between 1 and 100");
        }
        User manager = findManager(authentication.getName());
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "submittedAt"));
        var requests = status == null
                ? requestRepository.findByEmployeeProfileManagerId(manager.getId(), pageable)
                : requestRepository.findByEmployeeProfileManagerIdAndStatus(manager.getId(), status, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Team leave requests retrieved", LeaveRequestsPageDto.from(requests)));
    }

    private User findManager(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Manager was not found"));
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Page must be non-negative and size must be between 1 and 100");
        }
    }
}
