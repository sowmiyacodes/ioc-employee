package com.essp.hr.controller;

import com.essp.employee.repository.EmployeeProfileRepository;
import com.essp.hr.dto.*;
import com.essp.leave.dto.LeaveBalanceDto;
import com.essp.leave.entity.LeaveStatus;
import com.essp.leave.entity.LeaveType;
import com.essp.leave.repository.LeaveBalanceRepository;
import com.essp.leave.repository.LeaveRequestRepository;
import com.essp.leave.service.LeaveService;
import com.essp.leave.service.LeaveEntitlementPolicy;
import com.essp.security.dto.ApiResponse;
import com.essp.security.entity.Role;
import com.essp.security.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RestController
@RequestMapping("/api/hr")
@Transactional(readOnly = true)
public class HrManagementController {
    private final EmployeeProfileRepository profileRepository;
    private final LeaveBalanceRepository balanceRepository;
    private final LeaveRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final LeaveService leaveService;
    private final LeaveEntitlementPolicy entitlementPolicy;

    public HrManagementController(EmployeeProfileRepository profileRepository,
                                  LeaveBalanceRepository balanceRepository,
                                  LeaveRequestRepository requestRepository,
                                  UserRepository userRepository, LeaveService leaveService,
                                  LeaveEntitlementPolicy entitlementPolicy) {
        this.profileRepository = profileRepository;
        this.balanceRepository = balanceRepository;
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.leaveService = leaveService;
        this.entitlementPolicy = entitlementPolicy;
    }

    @GetMapping("/employees")
    public ResponseEntity<ApiResponse<HrEmployeePageDto>> employees(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "100") int size,
            @RequestParam(required = false) String search) {
        validatePage(page, size);
        return ResponseEntity.ok(ApiResponse.ok("Active employees retrieved",
                HrEmployeePageDto.from(profileRepository.searchActiveEmployees(Role.EMPLOYEE,
                search == null || search.isBlank() ? null : search.trim(),
                PageRequest.of(page, size, Sort.by("legalName").ascending())))));
    }

    @GetMapping("/entitlement-options")
    public ResponseEntity<ApiResponse<EntitlementOptionsDto>> options() {
        return ResponseEntity.ok(ApiResponse.ok("Leave entitlement options retrieved",
                new EntitlementOptionsDto(List.of(LeaveType.values()), entitlementPolicy.allowedDays())));
    }

    @GetMapping("/leave-balances")
    public ResponseEntity<ApiResponse<List<LeaveBalanceDto>>> balances(
            @RequestParam long employeeProfileId) {
        var profile = profileRepository.findById(employeeProfileId).orElse(null);
        if (employeeProfileId < 1 || profile == null || !profile.getUser().isActive()
                || profile.getUser().getRole() != Role.EMPLOYEE) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee profile was not found");
        }
        return ResponseEntity.ok(ApiResponse.ok("Employee leave balances retrieved",
                leaveService.getBalancesForProfile(employeeProfileId)));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<HrDashboardDto>> dashboard() {
        return ResponseEntity.ok(ApiResponse.ok("HR dashboard loaded", new HrDashboardDto(
                userRepository.countByRole(Role.EMPLOYEE),
                userRepository.countByRoleAndActiveTrue(Role.EMPLOYEE),
                balanceRepository.countEmployeesWithEntitlements(),
                requestRepository.countByStatus(LeaveStatus.PENDING),
                requestRepository.countByStatus(LeaveStatus.APPROVED),
                requestRepository.countByStatus(LeaveStatus.REJECTED))));
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Page must be non-negative and size must be between 1 and 100");
        }
    }

    public record EntitlementOptionsDto(List<LeaveType> leaveTypes, List<Integer> entitledDays) {
    }
}
