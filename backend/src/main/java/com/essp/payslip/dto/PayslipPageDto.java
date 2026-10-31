package com.essp.payslip.dto;

import com.essp.payslip.entity.Payslip;
import org.springframework.data.domain.Page;

import java.util.List;

public record PayslipPageDto(List<PayslipDto> content, int page, int size,
                             long totalElements, int totalPages) {
    public static PayslipPageDto from(Page<Payslip> payslips) {
        return new PayslipPageDto(payslips.getContent().stream()
                .map(payslip -> new PayslipDto(payslip.getId(), payslip.getPayPeriodStart(),
                        payslip.getPayPeriodEnd(), payslip.getIssuedAt()))
                .toList(), payslips.getNumber(), payslips.getSize(),
                payslips.getTotalElements(), payslips.getTotalPages());
    }
}
