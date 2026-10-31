package com.essp.payslip.controller;

import com.essp.payslip.dto.PayslipDto;
import com.essp.payslip.service.PayslipService;
import com.essp.security.audit.SecurityAuditService;
import com.essp.security.dto.ApiResponse;
import com.essp.security.ratelimit.SensitiveOperationRateLimiterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@RestController
@Validated
@RequestMapping("/api/hr/payslips")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('HR_ADMIN')")
public class PayslipManagementController {

    private final PayslipService payslipService;
    private final SensitiveOperationRateLimiterService rateLimiter;
    private final SecurityAuditService auditService;

    public PayslipManagementController(PayslipService payslipService,
                                       SensitiveOperationRateLimiterService rateLimiter,
                                       SecurityAuditService auditService) {
        this.payslipService = payslipService;
        this.rateLimiter = rateLimiter;
        this.auditService = auditService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Provision a PDF payslip for an active employee (HR administrator only)")
    public ResponseEntity<ApiResponse<PayslipDto>> provision(
            @RequestParam @NotNull Long employeeProfileId,
            @RequestParam @NotNull LocalDate payPeriodStart,
            @RequestParam @NotNull LocalDate payPeriodEnd,
            @RequestPart("file") MultipartFile file,
            Authentication authentication,
            HttpServletRequest request) {
        if (!rateLimiter.tryConsumePayslipUpload(authentication.getName())) {
            auditService.logRateLimitExceeded(request.getRemoteAddr(), authentication.getName());
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many payslip uploads. Please try again later.");
        }
        PayslipDto payslip = payslipService.provision(authentication.getName(), employeeProfileId,
                payPeriodStart, payPeriodEnd, file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Payslip provisioned", payslip));
    }
}
