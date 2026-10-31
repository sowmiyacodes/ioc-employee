package com.essp.payslip.repository;

import com.essp.payslip.entity.Payslip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PayslipRepository extends JpaRepository<Payslip, Long> {
    Page<Payslip> findByEmployeeProfileIdOrderByPayPeriodStartDesc(Long employeeProfileId, Pageable pageable);
    Optional<Payslip> findByIdAndEmployeeProfileId(Long id, Long employeeProfileId);
    boolean existsByEmployeeProfileIdAndPayPeriodStart(Long employeeProfileId, LocalDate payPeriodStart);
}
