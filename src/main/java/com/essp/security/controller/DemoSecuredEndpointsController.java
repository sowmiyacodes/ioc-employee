package com.essp.security.controller;

import com.essp.security.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Example Resource Controller demonstrating how other members (Member 70, 96, 114)
 * secure their endpoints using Member 64's Role-Based Access Control (RBAC).
 */
@RestController
@RequestMapping("/api")
public class DemoSecuredEndpointsController {

    @GetMapping("/employee/dashboard")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> employeeDashboard() {
        return ResponseEntity.ok(ApiResponse.ok("Employee area accessed successfully", Map.of("scope", "All Authenticated Roles")));
    }

    @GetMapping("/manager/team-overview")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> managerOverview() {
        return ResponseEntity.ok(ApiResponse.ok("Manager portal accessed", Map.of("scope", "Manager and higher")));
    }

    @GetMapping("/hr/staff-directory")
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> hrDirectory() {
        return ResponseEntity.ok(ApiResponse.ok("HR portal accessed", Map.of("scope", "HR_ADMIN & SYSTEM_ADMIN")));
    }

    @GetMapping("/admin/system-status")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> systemAdminStatus() {
        return ResponseEntity.ok(ApiResponse.ok("System Admin console accessed", Map.of("scope", "SYSTEM_ADMIN only")));
    }
}
