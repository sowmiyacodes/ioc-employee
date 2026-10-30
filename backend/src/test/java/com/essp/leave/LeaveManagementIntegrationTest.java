package com.essp.leave;

import com.essp.security.audit.AuditEventRepository;
import com.essp.employee.entity.EmployeeProfile;
import com.essp.employee.repository.EmployeeProfileRepository;
import com.essp.leave.entity.LeaveBalance;
import com.essp.leave.entity.LeaveRequest;
import com.essp.leave.entity.LeaveStatus;
import com.essp.leave.entity.LeaveType;
import com.essp.leave.repository.LeaveBalanceRepository;
import com.essp.leave.repository.LeaveRequestRepository;
import com.essp.security.entity.Role;
import com.essp.security.entity.User;
import com.essp.security.jwt.JwtTokenProvider;
import com.essp.security.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:essp_leave_test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "app.seed-demo-data=true",
        "APP_DEMO_PASSWORD=Password1234!"
})
class LeaveManagementIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider tokenProvider;
    @Autowired private UserRepository userRepository;
    @Autowired private EmployeeProfileRepository profileRepository;
    @Autowired private LeaveBalanceRepository balanceRepository;
    @Autowired private LeaveRequestRepository requestRepository;
    @Autowired private AuditEventRepository auditEventRepository;

    private User employee;
    private User manager;
    private EmployeeProfile employeeProfile;

    @BeforeEach
    void setUp() {
        requestRepository.deleteAll();
        balanceRepository.deleteAll();
        auditEventRepository.deleteAll();
        employee = userRepository.findByUsername("employee1").orElseThrow();
        manager = userRepository.findByUsername("manager1").orElseThrow();
        employeeProfile = profileRepository.findByUserUsername(employee.getUsername()).orElseThrow();

        LeaveBalance balance = new LeaveBalance();
        balance.setEmployeeProfile(employeeProfile);
        balance.setLeaveType(LeaveType.ANNUAL);
        balance.setEntitledDays(20);
        balance.setUsedDays(0);
        balanceRepository.save(balance);
    }

    @Test
    void employeeCanSubmitCalendarDayRequestAndReadOwnHistory() throws Exception {
        mockMvc.perform(post("/api/leaves")
                        .header("Authorization", bearer(employee))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("2030-04-10", "2030-04-12")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.leaveDays", is(3)))
                .andExpect(jsonPath("$.data.status", is("PENDING")));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/leaves/my")
                        .header("Authorization", bearer(employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()", is(1)))
                .andExpect(jsonPath("$.data.totalElements", is(1)));
        org.junit.jupiter.api.Assertions.assertTrue(
                auditEventRepository.findTop100ByActorUsernameOrderByOccurredAtDesc("employee1").stream()
                        .anyMatch(event -> event.getEventType().equals("LEAVE_REQUESTED")));
    }

    @Test
    void overlappingActiveRequestIsRejected() throws Exception {
        submitRequest("2030-04-10", "2030-04-12");

        mockMvc.perform(post("/api/leaves")
                        .header("Authorization", bearer(employee))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("2030-04-12", "2030-04-14")))
                .andExpect(status().isConflict());
    }

    @Test
    void leaveHistoryRejectsInvalidPaginationParameters() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/leaves/my?page=-1")
                        .header("Authorization", bearer(employee)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidDateRangeIsRejected() throws Exception {
        mockMvc.perform(post("/api/leaves")
                        .header("Authorization", bearer(employee))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("2030-04-13", "2030-04-10")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void insufficientBalanceIsRejected() throws Exception {
        LeaveBalance balance = balanceRepository.findByEmployeeProfileIdAndLeaveType(
                employeeProfile.getId(), LeaveType.ANNUAL).orElseThrow();
        balance.setEntitledDays(1);
        balanceRepository.save(balance);

        mockMvc.perform(post("/api/leaves")
                        .header("Authorization", bearer(employee))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("2030-04-10", "2030-04-11")))
                .andExpect(status().isConflict());
    }

    @Test
    void onlyReportingManagerCanApproveAndApprovalConsumesBalance() throws Exception {
        long requestId = submitRequest("2030-04-10", "2030-04-11");

        mockMvc.perform(patch("/api/leaves/{id}/decision", requestId)
                        .header("Authorization", bearer(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"APPROVE\",\"remarks\":\"Approved\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("APPROVED")));

        LeaveBalance updated = balanceRepository.findByEmployeeProfileIdAndLeaveType(
                employeeProfile.getId(), LeaveType.ANNUAL).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(2, updated.getUsedDays());
    }

    @Test
    void employeeCannotDecideLeaveRequest() throws Exception {
        long requestId = submitRequest("2030-04-10", "2030-04-11");

        mockMvc.perform(patch("/api/leaves/{id}/decision", requestId)
                        .header("Authorization", bearer(employee))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"APPROVE\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectionDoesNotConsumeLeaveBalance() throws Exception {
        long requestId = submitRequest("2030-04-10", "2030-04-11");

        mockMvc.perform(patch("/api/leaves/{id}/decision", requestId)
                        .header("Authorization", bearer(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"REJECT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("REJECTED")));

        LeaveBalance unchanged = balanceRepository.findByEmployeeProfileIdAndLeaveType(
                employeeProfile.getId(), LeaveType.ANNUAL).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(0, unchanged.getUsedDays());
    }

    @Test
    void managerCannotDecideRequestOutsideReportingScope() throws Exception {
        submitRequest("2030-04-10", "2030-04-11");
        User hrUser = userRepository.findByUsername("hradmin1").orElseThrow();
        EmployeeProfile otherProfile = profileRepository.findByUserUsername(hrUser.getUsername()).orElseThrow();
        LeaveRequest request = new LeaveRequest();
        request.setEmployeeProfile(otherProfile);
        request.setLeaveType(LeaveType.ANNUAL);
        request.setStartDate(LocalDate.of(2030, 4, 10));
        request.setEndDate(LocalDate.of(2030, 4, 11));
        request.setLeaveDays(2);
        request.setStatus(LeaveStatus.PENDING);
        request.setSubmittedAt(LocalDateTime.now());
        long requestId = requestRepository.save(request).getId();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/leaves/pending?page=0&size=10")
                        .header("Authorization", bearer(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()", is(1)))
                .andExpect(jsonPath("$.data.totalElements", is(1)));

        mockMvc.perform(patch("/api/leaves/{id}/decision", requestId)
                        .header("Authorization", bearer(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"APPROVE\"}"))
                .andExpect(status().isForbidden());
    }

    private long submitRequest(String start, String end) throws Exception {
        String response = mockMvc.perform(post("/api/leaves")
                        .header("Authorization", bearer(employee))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(start, end)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return new com.fasterxml.jackson.databind.ObjectMapper().readTree(response)
                .get("data").get("id").asLong();
    }

    private String bearer(User user) {
        return "Bearer " + tokenProvider.generateToken(user.getUsername(), user.getRole().name(), user.getId());
    }

    private String requestJson(String start, String end) {
        return "{\"leaveType\":\"ANNUAL\",\"startDate\":\"" + start
                + "\",\"endDate\":\"" + end + "\",\"reason\":\"Family event\"}";
    }
}
