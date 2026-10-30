package com.essp.payslip;

import com.essp.employee.entity.EmployeeProfile;
import com.essp.employee.repository.EmployeeProfileRepository;
import com.essp.payslip.entity.Payslip;
import com.essp.payslip.repository.PayslipRepository;
import com.essp.security.audit.AuditEventRepository;
import com.essp.security.entity.Role;
import com.essp.security.entity.User;
import com.essp.security.jwt.JwtTokenProvider;
import com.essp.security.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:essp_payslip_test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "app.seed-demo-data=true",
        "APP_DEMO_PASSWORD=Password1234!",
        "app.payslip.storage-directory=target/test-payslips"
})
class PayslipIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider tokenProvider;
    @Autowired private UserRepository userRepository;
    @Autowired private EmployeeProfileRepository profileRepository;
    @Autowired private PayslipRepository payslipRepository;
    @Autowired private AuditEventRepository auditEventRepository;

    private User employee;
    private User otherEmployee;
    private EmployeeProfile employeeProfile;
    private EmployeeProfile otherEmployeeProfile;
    private final Path storageDirectory = Path.of("target/test-payslips").toAbsolutePath().normalize();

    @BeforeEach
    void setUp() {
        payslipRepository.deleteAll();
        employee = userRepository.findByUsername("employee1").orElseThrow();
        employeeProfile = profileRepository.findByUserUsername(employee.getUsername()).orElseThrow();
        otherEmployee = userRepository.findByUsername("otheremployee").orElseGet(() ->
                userRepository.save(new User("otheremployee", "otheremployee@essp.local",
                        "not-used-by-this-test", Role.EMPLOYEE)));
        otherEmployeeProfile = profileRepository.findByUserUsername(otherEmployee.getUsername())
                .orElseGet(() -> createOtherEmployeeProfile(otherEmployee));
    }

    @Test
    void employeeCanListOnlyMetadataForOwnPayslips() throws Exception {
        createPayslip(employeeProfile, true);
        createPayslip(otherEmployeeProfile, true);

        mockMvc.perform(get("/api/payslips").header("Authorization", bearer(employee)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL,
                        org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(jsonPath("$.data.length()", is(1)))
                .andExpect(jsonPath("$.data[0].payPeriodStart", is("2030-01-01")))
                .andExpect(jsonPath("$.data[0].storageKey").doesNotExist());

        mockMvc.perform(get("/api/payslips").header("Authorization", bearer(otherEmployee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()", is(1)));
    }

    @Test
    void employeeCanDownloadOwnPayslipAndDownloadIsAuditedAndNotCached() throws Exception {
        byte[] pdf = "%PDF-1.4\nFictional test payslip\n%%EOF".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Payslip payslip = createPayslip(employeeProfile, pdf);

        var result = mockMvc.perform(get("/api/payslips/{id}/download", payslip.getId())
                        .header("Authorization", bearer(employee)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.containsString("payslip-2030-01.pdf")))
                .andReturn();

        assertArrayEquals(pdf, result.getResponse().getContentAsByteArray());
        String correlationId = result.getResponse().getHeader("X-Correlation-ID");
        org.junit.jupiter.api.Assertions.assertNotNull(correlationId);
        assertTrue(auditEventRepository.findTop100ByActorUsernameOrderByOccurredAtDesc("employee1").stream()
                .anyMatch(event -> event.getEventType().equals("PAYSLIP_DOWNLOADED")
                        && event.getTargetId().equals(payslip.getId())
                        && correlationId.equals(event.getCorrelationId())));
    }

    @Test
    void employeeCannotDownloadAnotherEmployeesPayslip() throws Exception {
        Payslip payslip = createPayslip(otherEmployeeProfile, true);

        mockMvc.perform(get("/api/payslips/{id}/download", payslip.getId())
                        .header("Authorization", bearer(employee)))
                .andExpect(status().isNotFound());
    }

    @Test
    void requestRequiresAuthenticationAndEmployeeRole() throws Exception {
        mockMvc.perform(get("/api/payslips"))
                .andExpect(status().isUnauthorized());

        User manager = userRepository.findByUsername("manager1").orElseThrow();
        mockMvc.perform(get("/api/payslips").header("Authorization", bearer(manager)))
                .andExpect(status().isForbidden());
    }

    @Test
    void missingAndInvalidStorageReferencesDoNotExposeFiles() throws Exception {
        Payslip missingFile = createPayslip(employeeProfile, false);
        mockMvc.perform(get("/api/payslips/{id}/download", missingFile.getId())
                        .header("Authorization", bearer(employee)))
                .andExpect(status().isNotFound());

        missingFile.setStorageKey("..%2f..%2fsecret.pdf");
        payslipRepository.save(missingFile);
        mockMvc.perform(get("/api/payslips/{id}/download", missingFile.getId())
                        .header("Authorization", bearer(employee)))
                .andExpect(status().isNotFound());
    }

    private Payslip createPayslip(EmployeeProfile profile, boolean createFile) throws Exception {
        return createPayslip(profile, createFile
                ? "%PDF-1.4\nFictional test payslip\n%%EOF".getBytes(java.nio.charset.StandardCharsets.UTF_8)
                : null);
    }

    private Payslip createPayslip(EmployeeProfile profile, byte[] pdf) throws Exception {
        String storageKey = UUID.randomUUID() + ".pdf";
        if (pdf != null) {
            Files.createDirectories(storageDirectory);
            Files.write(storageDirectory.resolve(storageKey), pdf);
        }
        Payslip payslip = new Payslip();
        payslip.setEmployeeProfile(profile);
        payslip.setPayPeriodStart(LocalDate.of(2030, 1, 1));
        payslip.setPayPeriodEnd(LocalDate.of(2030, 1, 31));
        payslip.setIssuedAt(LocalDateTime.of(2030, 2, 1, 9, 0));
        payslip.setStorageKey(storageKey);
        return payslipRepository.save(payslip);
    }

    private EmployeeProfile createOtherEmployeeProfile(User user) {
        EmployeeProfile profile = new EmployeeProfile();
        profile.setUser(user);
        profile.setEmployeeId("EMP-TEST-02");
        profile.setLegalName("Fictional Test Employee");
        profile.setDepartment("Testing");
        profile.setDesignation("Test Employee");
        profile.setJoiningDate(LocalDate.of(2028, 1, 1));
        return profileRepository.save(profile);
    }

    private String bearer(User user) {
        return "Bearer " + tokenProvider.generateToken(user.getUsername(), user.getRole().name(), user.getId());
    }
}
