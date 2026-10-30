package com.essp.leave.repository;

import com.essp.leave.entity.LeaveBalance;
import com.essp.leave.entity.LeaveType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {
    List<LeaveBalance> findByEmployeeProfileIdOrderByLeaveType(Long employeeProfileId);

    Optional<LeaveBalance> findByEmployeeProfileIdAndLeaveType(Long employeeProfileId, LeaveType leaveType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select balance from LeaveBalance balance where balance.employeeProfile.id = :profileId and balance.leaveType = :leaveType")
    Optional<LeaveBalance> findForUpdate(@Param("profileId") Long profileId, @Param("leaveType") LeaveType leaveType);

    List<LeaveBalance> findAllByOrderByEmployeeProfileLegalNameAscLeaveTypeAsc();

    @Query("select count(distinct balance.employeeProfile.id) from LeaveBalance balance")
    long countEmployeesWithEntitlements();
}
