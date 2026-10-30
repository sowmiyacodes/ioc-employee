package com.essp.payslip.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PayslipDto(Long id, LocalDate payPeriodStart, LocalDate payPeriodEnd,
                         LocalDateTime issuedAt) {}
