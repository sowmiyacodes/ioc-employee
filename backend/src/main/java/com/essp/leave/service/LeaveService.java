package com.essp.leave.service;

import com.essp.employee.entity.EmployeeProfile;
import com.essp.employee.repository.EmployeeProfileRepository;
import com.essp.leave.dto.*;
import com.essp.leave.entity.*;
import com.essp.leave.repository.LeaveBalanceRepository;
import com.essp.leave.repository.LeaveRequestRepository;
import com.essp.security.audit.SecurityAuditService;
import com.essp.security.entity.Role;
import com.essp.security.entity.User;
import com.essp.security.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class LeaveService {
    private static final List<LeaveStatus> ACTIVE_STATUSES = List.of(LeaveStatus.PENDING, LeaveStatus.APPROVED);

    private final EmployeeProfileRepository profileRepository;
    private final LeaveBalanceRepository balanceRepository;
    private final LeaveRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final SecurityAuditService auditService;
    private final LeaveEntitlementPolicy entitlementPolicy;

    public LeaveService(EmployeeProfileRepository profileRepository,
                        LeaveBalanceRepository balanceRepository,
                        LeaveRequestRepository requestRepository,
                        UserRepository userRepository,
                        SecurityAuditService auditService,
                        LeaveEntitlementPolicy entitlementPolicy) {
        this.profileRepository = profileRepository;
        this.balanceRepository = balanceRepository;
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.entitlementPolicy = entitlementPolicy;
    }

    @Transactional(readOnly = true)
    public List<LeaveBalanceDto> getBalances(String username) {
        EmployeeProfile profile = findProfile(username);
        return getBalancesForProfile(profile.getId());
    }

    @Transactional(readOnly = true)
    public List<LeaveBalanceDto> getBalancesForProfile(Long profileId) {
        EmployeeProfile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> notFound("Employee profile was not found"));
        return balanceRepository.findByEmployeeProfileIdOrderByLeaveType(profile.getId()).stream()
                .map(balance -> LeaveBalanceDto.from(balance, pendingDays(profile.getId(), balance.getLeaveType())))
                .toList();
    }

    @Transactional
    public LeaveRequestDto createRequest(String username, LeaveRequestCreateRequest input) {
        EmployeeProfile profile = profileRepository.findByUsernameForUpdate(username)
                .orElseThrow(() -> notFound("Employee profile was not found"));

        if (input.getEndDate().isBefore(input.getStartDate())) {
            throw badRequest("End date must be on or after start date");
        }

        int leaveDays = Math.toIntExact(ChronoUnit.DAYS.between(input.getStartDate(), input.getEndDate()) + 1);
        if (requestRepository.existsOverlappingRequest(
                profile.getId(), input.getStartDate(), input.getEndDate(), ACTIVE_STATUSES)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This request overlaps another pending or approved leave request");
        }

        LeaveBalance balance = balanceRepository.findForUpdate(profile.getId(), input.getLeaveType())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "A balance for this leave type has not been configured"));
        long pending = pendingDays(profile.getId(), input.getLeaveType());
        long available = (long) balance.getEntitledDays() - balance.getUsedDays() - pending;
        if (leaveDays > available) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "The request exceeds the available leave balance");
        }

        LeaveRequest request = new LeaveRequest();
        request.setEmployeeProfile(profile);
        request.setLeaveType(input.getLeaveType());
        request.setStartDate(input.getStartDate());
        request.setEndDate(input.getEndDate());
        request.setLeaveDays(leaveDays);
        request.setReason(cleanOptionalText(input.getReason()));
        request.setStatus(LeaveStatus.PENDING);
        LeaveRequest saved = requestRepository.save(request);
        auditService.logLeaveEvent(username, "LEAVE_REQUESTED", saved.getId());
        return LeaveRequestDto.from(saved);
    }

    @Transactional(readOnly = true)
    public LeaveRequestsPageDto getMyRequests(String username, int page, int size, LeaveStatus status) {
        EmployeeProfile profile = findProfile(username);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "submittedAt"));
        Page<LeaveRequest> requests = status == null
                ? requestRepository.findByEmployeeProfileId(profile.getId(), pageRequest)
                : requestRepository.findByEmployeeProfileIdAndStatus(profile.getId(), status, pageRequest);
        return LeaveRequestsPageDto.from(requests);
    }

    @Transactional(readOnly = true)
    public LeaveRequestsPageDto getPendingRequests(String username, int page, int size) {
        User actor = findUser(username);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "submittedAt"));
        Page<LeaveRequest> requests;
        if (actor.getRole() == Role.MANAGER) {
            requests = requestRepository.findByEmployeeProfileManagerIdAndStatus(
                    actor.getId(), LeaveStatus.PENDING, pageRequest);
        } else {
            requests = requestRepository.findByStatus(LeaveStatus.PENDING, pageRequest);
        }
        return LeaveRequestsPageDto.from(requests);
    }

    @Transactional
    public LeaveRequestDto decide(String username, Long requestId, LeaveDecisionRequest input) {
        User actor = findUser(username);
        LeaveRequest request = requestRepository.findForUpdate(requestId)
                .orElseThrow(() -> notFound("Leave request was not found"));

        if (request.getEmployeeProfile().getUser().getId().equals(actor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot decide your own leave request");
        }
        if (actor.getRole() == Role.MANAGER) {
            User manager = request.getEmployeeProfile().getManager();
            if (manager == null || !manager.getId().equals(actor.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "This leave request is outside your reporting scope");
            }
        }
        if (request.getStatus() != LeaveStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This leave request has already been decided");
        }

        profileRepository.findByUsernameForUpdate(request.getEmployeeProfile().getUser().getUsername())
                .orElseThrow(() -> notFound("Employee profile was not found"));

        if (input.getAction() == LeaveDecisionRequest.Action.APPROVE) {
            LeaveBalance balance = balanceRepository.findForUpdate(
                            request.getEmployeeProfile().getId(), request.getLeaveType())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                            "A balance for this leave type has not been configured"));
            long pending = pendingDays(request.getEmployeeProfile().getId(), request.getLeaveType());
            long pendingOtherRequests = pending - request.getLeaveDays();
            long availableForApproval = (long) balance.getEntitledDays() - balance.getUsedDays()
                    - pendingOtherRequests;
            if (availableForApproval < request.getLeaveDays()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "The leave balance no longer covers this request");
            }
            balance.setUsedDays(balance.getUsedDays() + request.getLeaveDays());
            request.setStatus(LeaveStatus.APPROVED);
        } else {
            request.setStatus(LeaveStatus.REJECTED);
        }

        request.setDecisionMaker(actor);
        request.setDecidedAt(LocalDateTime.now());
        request.setDecisionRemarks(cleanOptionalText(input.getRemarks()));
        LeaveRequest saved = requestRepository.save(request);
        auditService.logLeaveEvent(username, "LEAVE_" + input.getAction().name(), saved.getId());
        return LeaveRequestDto.from(saved);
    }

    @Transactional
    public LeaveBalanceDto setBalance(String actorUsername, LeaveBalanceUpdateRequest input) {
        User actor = findUser(actorUsername);
        EmployeeProfile profile = profileRepository.findByIdForUpdate(input.getEmployeeProfileId())
                .orElseThrow(() -> notFound("Employee profile was not found"));
        if (!profile.getUser().isActive() || profile.getUser().getRole() != Role.EMPLOYEE) {
            throw badRequest("Entitlements can only be assigned to active employee accounts");
        }
        if (!entitlementPolicy.allows(input.getEntitledDays())) {
            throw badRequest("Entitled days must be selected from the configured entitlement options");
        }
        LeaveBalance balance = balanceRepository.findForUpdate(profile.getId(), input.getLeaveType())
                .orElseGet(() -> {
                    LeaveBalance created = new LeaveBalance();
                    created.setEmployeeProfile(profile);
                    created.setLeaveType(input.getLeaveType());
                    return created;
                });
        if (input.getEntitledDays() < balance.getUsedDays()
                + pendingDays(profile.getId(), input.getLeaveType())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Entitlement cannot be less than used and pending leave");
        }
        balance.setEntitledDays(input.getEntitledDays());
        LeaveBalance saved = balanceRepository.save(balance);
        auditService.logAdministrationEvent(actor.getUsername(), "LEAVE_BALANCE_UPDATED", saved.getId(),
                "employeeProfileId=" + profile.getId() + ";leaveType=" + saved.getLeaveType()
                        + ";entitledDays=" + saved.getEntitledDays());
        return LeaveBalanceDto.from(saved, pendingDays(profile.getId(), saved.getLeaveType()));
    }

    private long pendingDays(Long profileId, LeaveType leaveType) {
        return requestRepository.sumDaysByProfileAndTypeAndStatus(profileId, leaveType, LeaveStatus.PENDING);
    }

    private EmployeeProfile findProfile(String username) {
        return profileRepository.findByUserUsername(username)
                .orElseThrow(() -> notFound("Employee profile was not found"));
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> notFound("User was not found"));
    }

    private String cleanOptionalText(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
