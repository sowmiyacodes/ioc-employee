package com.essp.employee.repository;

import com.essp.employee.entity.EmployeeProfile;
import com.essp.security.entity.Role;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EmployeeProfileRepository extends JpaRepository<EmployeeProfile, Long> {
    Optional<EmployeeProfile> findByUserUsername(String username);
    boolean existsByUserId(Long userId);
    boolean existsByEmployeeId(String employeeId);
    boolean existsByEmployeeIdAndIdNot(String employeeId, Long id);
    boolean existsByManagerId(Long managerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select profile from EmployeeProfile profile where profile.id = :id")
    Optional<EmployeeProfile> findByIdForUpdate(@Param("id") Long id);

    java.util.List<EmployeeProfile> findByUserActiveTrueOrderByLegalNameAsc();
    Page<EmployeeProfile> findByUserActiveTrueAndUserRoleOrderByLegalNameAsc(Role role, Pageable pageable);
    @Query("""
            select profile from EmployeeProfile profile
            where profile.user.active = true and profile.user.role = :role
              and (:search is null or :search = ''
                   or lower(profile.employeeId) like lower(concat('%', :search, '%'))
                   or lower(profile.legalName) like lower(concat('%', :search, '%'))
                   or lower(profile.user.username) like lower(concat('%', :search, '%'))
                   or lower(profile.user.email) like lower(concat('%', :search, '%')))
            """)
    Page<EmployeeProfile> searchActiveEmployees(@Param("role") Role role,
                                                 @Param("search") String search,
                                                 Pageable pageable);
    java.util.List<EmployeeProfile> findByManagerIdAndUserActiveTrueOrderByLegalNameAsc(Long managerId);
    Page<EmployeeProfile> findByManagerIdAndUserActiveTrueOrderByLegalNameAsc(
            Long managerId, Pageable pageable);
    long countByManagerIdAndUserActiveTrue(Long managerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select profile from EmployeeProfile profile where profile.user.username = :username")
    Optional<EmployeeProfile> findByUsernameForUpdate(@Param("username") String username);
}
