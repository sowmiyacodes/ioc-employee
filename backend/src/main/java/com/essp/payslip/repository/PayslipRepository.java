package com.essp.payslip.repository;

import com.essp.payslip.entity.Payslip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PayslipRepository extends JpaRepository<Payslip, Long> {
    List<Payslip> findByEmployeeProfileIdOrderByPayPeriodStartDesc(Long employeeProfileId);
    Optional<Payslip> findByIdAndEmployeeProfileId(Long id, Long employeeProfileId);
}
