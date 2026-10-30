package com.essp.security;

import com.essp.security.entity.Role;
import com.essp.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Test
    @DisplayName("Unauthenticated request to protected endpoint returns 401 Unauthorized")
    void testUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/api/employee/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Employee role can access employee dashboard")
    void testEmployeeAccessAuthorized() throws Exception {
        String token = tokenProvider.generateToken("emp1", Role.EMPLOYEE.name(), 1L);
        mockMvc.perform(get("/api/employee/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Employee role cannot access System Admin endpoint (returns 403 Forbidden)")
    void testEmployeeForbiddenOnAdminEndpoint() throws Exception {
        String token = tokenProvider.generateToken("emp1", Role.EMPLOYEE.name(), 1L);
        mockMvc.perform(get("/api/admin/system-status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("System Admin role can access Admin endpoint")
    void testAdminAccessAllowed() throws Exception {
        String token = tokenProvider.generateToken("admin1", Role.SYSTEM_ADMIN.name(), 4L);
        mockMvc.perform(get("/api/admin/system-status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
