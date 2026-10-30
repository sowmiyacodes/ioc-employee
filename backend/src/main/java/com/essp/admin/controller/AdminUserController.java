package com.essp.admin.controller;

import com.essp.admin.dto.*;
import com.essp.admin.service.AdminUserService;
import com.essp.security.dto.ApiResponse;
import com.essp.security.entity.Role;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin")
public class AdminUserController {
    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<AdminDashboardDto>> dashboard() {
        return ResponseEntity.ok(ApiResponse.ok("Administrator dashboard loaded", adminUserService.dashboard()));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<AdminUserPageDto>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "username") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Page must be non-negative and size must be between 1 and 100");
        }
        return ResponseEntity.ok(ApiResponse.ok("Users retrieved",
                adminUserService.list(search, role, active, page, size, sortBy, direction)));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<AdminUserDto>> get(@PathVariable long id) {
        return ResponseEntity.ok(ApiResponse.ok("User retrieved", adminUserService.get(id)));
    }

    @PostMapping("/users")
    public ResponseEntity<ApiResponse<AdminUserDto>> create(
            @Valid @RequestBody AdminUserCreateRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("User created",
                adminUserService.create(authentication.getName(), request)));
    }

    @PatchMapping("/users/{id}")
    public ResponseEntity<ApiResponse<AdminUserDto>> update(
            @PathVariable long id, @Valid @RequestBody AdminUserUpdateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok("User updated",
                adminUserService.update(authentication.getName(), id, request)));
    }

    @PatchMapping("/users/{id}/status")
    public ResponseEntity<ApiResponse<AdminUserDto>> status(
            @PathVariable long id, @Valid @RequestBody AdminUserStatusRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok("User status updated",
                adminUserService.setActive(authentication.getName(), id, request.active())));
    }
}
