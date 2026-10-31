package com.essp.security.controller;

import com.essp.security.dto.ApiResponse;
import com.essp.security.dto.LoginRequest;
import com.essp.security.dto.LoginResponse;
import com.essp.security.dto.UserProfileDto;
import com.essp.security.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request,
                                                            HttpServletRequest servletRequest) {
        String clientIp = extractClientIp(servletRequest);
        LoginResponse response = authService.authenticate(request, clientIp);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest servletRequest,
                                                    Authentication authentication) {
        String authHeader = servletRequest.getHeader("Authorization");
        String username = authentication != null ? authentication.getName() : null;
        String clientIp = extractClientIp(servletRequest);

        authService.logout(authHeader, username, clientIp);
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully"));
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<UserProfileDto>> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
        }
        UserProfileDto profile = authService.getCurrentUser(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok("Current user profile retrieved", profile));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "Authentication Service (Member 64)"));
    }

    private String extractClientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
