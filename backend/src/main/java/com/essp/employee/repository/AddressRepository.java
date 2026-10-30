package com.essp.employee.repository;

import com.essp.employee.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {
    Optional<Address> findByEmployeeProfileId(Long employeeProfileId);
}
