package com.essp.security;

import com.essp.security.entity.Role;
import com.essp.security.entity.User;
import com.essp.security.jwt.JwtTokenProvider;
import com.essp.security.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.TestPropertySource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:essp_security_test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "app.seed-demo-data=true",
        "APP_DEMO_PASSWORD=Password1234!"
})
class SecurityAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Unauthenticated request to protected endpoint returns 401 Unauthorized")
    void testUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/api/employees/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Employee role can access employee dashboard")
    void testEmployeeAccessAuthorized() throws Exception {
        String token = tokenProvider.generateToken("employee1", Role.EMPLOYEE.name(), 1L);
        mockMvc.perform(get("/api/employees/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Employee role cannot access System Admin endpoint (returns 403 Forbidden)")
    void testEmployeeForbiddenOnAdminEndpoint() throws Exception {
        String token = tokenProvider.generateToken("employee1", Role.EMPLOYEE.name(), 1L);
        mockMvc.perform(get("/api/leaves/pending")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("System Admin role can access Admin endpoint")
    void testAdminAccessAllowed() throws Exception {
        String token = tokenProvider.generateToken("sysadmin1", Role.SYSTEM_ADMIN.name(), 4L);
        mockMvc.perform(get("/api/leaves/pending")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("A signed token cannot elevate an employee using a client-supplied role claim")
    void testRoleClaimIsResolvedFromDatabase() throws Exception {
        String token = tokenProvider.generateToken("employee1", Role.SYSTEM_ADMIN.name(), 1L);
        mockMvc.perform(get("/api/leaves/pending")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Disabled accounts cannot continue using an otherwise valid token")
    void testDisabledAccountTokenIsRejected() throws Exception {
        User employee = userRepository.findByUsername("employee1").orElseThrow();
        employee.setActive(false);
        userRepository.save(employee);
        try {
            String token = tokenProvider.generateToken(
                    employee.getUsername(), employee.getRole().name(), employee.getId());
            mockMvc.perform(get("/api/employees/me")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized());
        } finally {
            employee.setActive(true);
            userRepository.save(employee);
        }
    }
}
