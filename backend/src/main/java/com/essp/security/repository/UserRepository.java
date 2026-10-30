package com.essp.security.repository;

import com.essp.security.entity.User;
import com.essp.security.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByUsernameOrEmail(String username, String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByUsernameAndIdNot(String username, Long id);
    boolean existsByEmailAndIdNot(String email, Long id);
    long countByActiveTrue();
    long countByActiveFalse();
    long countByRole(Role role);
    long countByRoleAndActiveTrue(Role role);
    long countByRoleAndActiveFalse(Role role);
    java.util.List<User> findByRoleAndActiveTrueOrderByUsername(Role role);

    @Query("""
            select u from User u
            left join u.employeeProfile profile
            where (:role is null or u.role = :role)
              and (:active is null or u.active = :active)
              and (:search is null or :search = ''
                   or lower(u.username) like lower(concat('%', :search, '%'))
                   or lower(u.email) like lower(concat('%', :search, '%'))
                   or lower(profile.employeeId) like lower(concat('%', :search, '%'))
                   or lower(profile.legalName) like lower(concat('%', :search, '%')))
            """)
    Page<User> searchUsers(@Param("search") String search, @Param("role") Role role,
                           @Param("active") Boolean active, Pageable pageable);
}
