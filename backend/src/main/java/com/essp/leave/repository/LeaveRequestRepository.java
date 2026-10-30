package com.essp.leave.repository;

import com.essp.leave.entity.LeaveRequest;
import com.essp.leave.entity.LeaveStatus;
import com.essp.leave.entity.LeaveType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    long countByStatus(LeaveStatus status);
    Page<LeaveRequest> findByEmployeeProfileId(Long profileId, Pageable pageable);

    Page<LeaveRequest> findByEmployeeProfileIdAndStatus(Long profileId, LeaveStatus status, Pageable pageable);

    Page<LeaveRequest> findByEmployeeProfileManagerIdAndStatus(Long managerId, LeaveStatus status, Pageable pageable);
    Page<LeaveRequest> findByEmployeeProfileManagerId(Long managerId, Pageable pageable);
    long countByEmployeeProfileManagerIdAndStatus(Long managerId, LeaveStatus status);
    Page<LeaveRequest> findByStatus(LeaveStatus status, Pageable pageable);

    @Query("select count(request) > 0 from LeaveRequest request " +
            "where request.employeeProfile.id = :profileId " +
            "and request.status in :statuses " +
            "and request.startDate <= :endDate and request.endDate >= :startDate")
    boolean existsOverlappingRequest(@Param("profileId") Long profileId,
                                     @Param("startDate") LocalDate startDate,
                                     @Param("endDate") LocalDate endDate,
                                     @Param("statuses") List<LeaveStatus> statuses);

    @Query("select coalesce(sum(request.leaveDays), 0) from LeaveRequest request " +
            "where request.employeeProfile.id = :profileId and request.leaveType = :leaveType " +
            "and request.status = :status")
    long sumDaysByProfileAndTypeAndStatus(@Param("profileId") Long profileId,
                                          @Param("leaveType") LeaveType leaveType,
                                          @Param("status") LeaveStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select request from LeaveRequest request where request.id = :id")
    Optional<LeaveRequest> findForUpdate(@Param("id") Long id);
}
