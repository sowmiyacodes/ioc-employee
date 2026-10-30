package com.essp.employee.repository;

import com.essp.employee.entity.EmployeeProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployeeProfileRepository extends JpaRepository<EmployeeProfile, Long> {
    Optional<EmployeeProfile> findByUserUsername(String username);
    boolean existsByUserId(Long userId);
}
