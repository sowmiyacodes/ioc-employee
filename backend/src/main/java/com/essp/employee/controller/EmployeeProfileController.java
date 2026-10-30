package com.essp.employee.controller;

import com.essp.employee.dto.AddressUpdateRequest;
import com.essp.employee.dto.EmployeeProfileDto;
import com.essp.employee.service.EmployeeProfileService;
import com.essp.security.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/employees")
@SecurityRequirement(name = "bearerAuth")
public class EmployeeProfileController {
    private final EmployeeProfileService profileService;

    public EmployeeProfileController(EmployeeProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<EmployeeProfileDto>> getMyProfile(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok("Employee profile retrieved",
                profileService.getMyProfile(authentication.getName())));
    }

    @PatchMapping("/me/address")
    public ResponseEntity<ApiResponse<EmployeeProfileDto>> updateMyAddress(
            @Valid @RequestBody AddressUpdateRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok("Address updated successfully",
                profileService.updateMyAddress(authentication.getName(), request)));
    }
}
