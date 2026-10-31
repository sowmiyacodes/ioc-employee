package com.essp.payslip.controller;

import com.essp.payslip.dto.PayslipDto;
import com.essp.payslip.dto.PayslipPageDto;
import com.essp.payslip.service.PayslipService;
import com.essp.security.dto.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/payslips")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('EMPLOYEE')")
public class PayslipController {

    private static final DateTimeFormatter FILE_PERIOD = DateTimeFormatter.ofPattern("yyyy-MM");
    private final PayslipService payslipService;

    public PayslipController(PayslipService payslipService) {
        this.payslipService = payslipService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PayslipPageDto>> getPayslips(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(ApiResponse.ok("Payslips retrieved",
                        payslipService.getPayslips(authentication.getName(), page, size)));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id, Authentication authentication,
                                             jakarta.servlet.http.HttpServletRequest request) {
        PayslipService.PayslipDownload download = payslipService.getDownload(
                authentication.getName(), id, request.getRemoteAddr());
        String filename = "payslip-" + download.payslip().getPayPeriodStart().format(FILE_PERIOD) + ".pdf";
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(download.resource());
    }
}
