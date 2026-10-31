package com.essp.admin;

import com.essp.employee.repository.EmployeeProfileRepository;
import com.essp.security.entity.Role;
import com.essp.security.entity.User;
import com.essp.security.jwt.JwtTokenProvider;
import com.essp.security.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:essp_admin_test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "app.seed-demo-data=true",
        "APP_DEMO_PASSWORD=Password1234!"
})
class AdminUserManagementIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider tokenProvider;
    @Autowired private UserRepository userRepository;
    @Autowired private EmployeeProfileRepository profileRepository;

    @Test
    void systemAdministratorCanCreateAccountWithHashedPasswordAndManagerAssignment() throws Exception {
        User manager = userRepository.findByUsername("manager1").orElseThrow();
        String request = """
                {
                  "username":"managed-employee",
                  "email":"managed.employee@example.test",
                  "password":"NewPassword123!",
                  "role":"EMPLOYEE",
                  "employeeId":"EMP-9001",
                  "legalName":"Test Employee",
                  "department":"Engineering",
                  "designation":"Developer",
                  "joiningDate":"2024-01-15",
                  "managerId":%d
                }
                """.formatted(manager.getId());

        mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", bearer("sysadmin1", Role.SYSTEM_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.username", is("managed-employee")))
                .andExpect(jsonPath("$.data.managerId", is(manager.getId().intValue())))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());

        mockMvc.perform(get("/api/admin/users?search=managed-employee&sortBy=name&direction=desc")
                        .header("Authorization", bearer("sysadmin1", Role.SYSTEM_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements", is(1)))
                .andExpect(jsonPath("$.data.content[0].username", is("managed-employee")));

        User created = userRepository.findByUsername("managed-employee").orElseThrow();
        org.junit.jupiter.api.Assertions.assertNotEquals("NewPassword123!", created.getPasswordHash());
        org.junit.jupiter.api.Assertions.assertTrue(profileRepository
                .findByUserUsername("managed-employee").orElseThrow().getManager().getId().equals(manager.getId()));
    }

    @Test
    void nonAdministratorCannotAccessUserManagement() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", bearer("employee1", Role.EMPLOYEE)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/hr/employees")
                        .header("Authorization", bearer("employee1", Role.EMPLOYEE)))
                .andExpect(status().isForbidden());
    }

    @Test
    void searchAndSortParametersCannotInjectQueries() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .queryParam("search", "' OR '1'='1")
                        .queryParam("sortBy", "id desc, (select 1)")
                        .header("Authorization", bearer("sysadmin1", Role.SYSTEM_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements", is(0)));
    }

    @Test
    void administratorCanEditEmployeeProfileAndAssignManager() throws Exception {
        mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", bearer("sysadmin1", Role.SYSTEM_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"profile-edit-user",
                                  "email":"profile.edit@example.test",
                                  "password":"StartPassword123!",
                                  "role":"EMPLOYEE",
                                  "employeeId":"EMP-9002",
                                  "legalName":"Before Edit",
                                  "department":"Operations",
                                  "designation":"Associate",
                                  "joiningDate":"2024-01-15"
                                }
                                """))
                .andExpect(status().isCreated());

        User user = userRepository.findByUsername("profile-edit-user").orElseThrow();
        User manager = userRepository.findByUsername("manager1").orElseThrow();
        mockMvc.perform(patch("/api/admin/users/{id}", user.getId())
                        .header("Authorization", bearer("sysadmin1", Role.SYSTEM_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"profile.edited@example.test",
                                  "role":"EMPLOYEE",
                                  "employeeId":"EMP-9002",
                                  "legalName":"After Edit",
                                  "department":"Engineering",
                                  "designation":"Developer",
                                  "joiningDate":"2024-01-15",
                                  "managerId":%d
                                }
                                """.formatted(manager.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.legalName", is("After Edit")))
                .andExpect(jsonPath("$.data.managerId", is(manager.getId().intValue())));
    }

    @Test
    void lastActiveSystemAdministratorCannotBeDeactivated() throws Exception {
        User admin = userRepository.findByUsername("sysadmin1").orElseThrow();
        mockMvc.perform(patch("/api/admin/users/{id}/status", admin.getId())
                        .header("Authorization", bearer("sysadmin1", Role.SYSTEM_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isConflict());
    }

    @Test
    void hrCanLoadDatabaseBackedOptionsButArbitraryEntitlementsAreRejected() throws Exception {
        Long employeeProfileId = profileRepository.findByUserUsername("employee1").orElseThrow().getId();
        mockMvc.perform(get("/api/hr/entitlement-options")
                        .header("Authorization", bearer("hradmin1", Role.HR_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.entitledDays[0]", is(0)));
        mockMvc.perform(get("/api/hr/employees")
                        .header("Authorization", bearer("hradmin1", Role.HR_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.content[*].employeeId", hasItem("EMP-0001")));

        mockMvc.perform(put("/api/leaves/balance")
                        .header("Authorization", bearer("hradmin1", Role.HR_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"employeeProfileId":%d,"leaveType":"ANNUAL","entitledDays":7}
                                """.formatted(employeeProfileId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void managerCanOnlyReadTheAssignedTeam() throws Exception {
        mockMvc.perform(get("/api/manager/team")
                        .header("Authorization", bearer("manager1", Role.MANAGER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.content[*].employeeId", hasItem("EMP-0001")));
        mockMvc.perform(get("/api/manager/team")
                        .header("Authorization", bearer("hradmin1", Role.HR_ADMIN)))
                .andExpect(status().isForbidden());
    }

    private String bearer(String username, Role role) {
        User user = userRepository.findByUsername(username).orElseThrow();
        return "Bearer " + tokenProvider.generateToken(username, role.name(), user.getId());
    }
}
