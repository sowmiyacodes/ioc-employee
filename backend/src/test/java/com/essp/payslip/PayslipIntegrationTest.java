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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
        "app.security.rate-limit.payslip-upload-capacity=10",
        "app.payslip.storage-directory=target/test-payslips"
})
class PayslipIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider tokenProvider;
    @Autowired private UserRepository userRepository;
    @Autowired private EmployeeProfileRepository profileRepository;
    @Autowired private PayslipRepository payslipRepository;
    @Autowired private AuditEventRepository auditEventRepository;
    @Autowired private ObjectMapper objectMapper;

    private User employee;
    private User otherEmployee;
    private User hrAdmin;
    private EmployeeProfile employeeProfile;
    private EmployeeProfile otherEmployeeProfile;
    private final Path storageDirectory = Path.of("target/test-payslips").toAbsolutePath().normalize();

    @BeforeEach
    void setUp() {
        payslipRepository.deleteAll();
        employee = userRepository.findByUsername("employee1").orElseThrow();
        hrAdmin = userRepository.findByUsername("hradmin1").orElseThrow();
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
                .andExpect(jsonPath("$.data.content.length()", is(1)))
                .andExpect(jsonPath("$.data.content[0].payPeriodStart", is("2030-01-01")))
                .andExpect(jsonPath("$.data.content[0].storageKey").doesNotExist());

        mockMvc.perform(get("/api/payslips").header("Authorization", bearer(otherEmployee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()", is(1)));
    }

    @Test
    void payslipListingIsPaginatedAndRejectsInvalidPageParameters() throws Exception {
        byte[] pdf = "%PDF-1.4\nFictional test payslip\n%%EOF".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        createPayslip(employeeProfile, pdf, LocalDate.of(2030, 1, 1), LocalDate.of(2030, 1, 31));
        createPayslip(employeeProfile, pdf, LocalDate.of(2030, 2, 1), LocalDate.of(2030, 2, 28));

        mockMvc.perform(get("/api/payslips?page=0&size=1").header("Authorization", bearer(employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()", is(1)))
                .andExpect(jsonPath("$.data.page", is(0)))
                .andExpect(jsonPath("$.data.size", is(1)))
                .andExpect(jsonPath("$.data.totalElements", is(2)))
                .andExpect(jsonPath("$.data.totalPages", is(2)))
                .andExpect(jsonPath("$.data.content[0].payPeriodStart", is("2030-02-01")));

        mockMvc.perform(get("/api/payslips?page=1&size=1").header("Authorization", bearer(employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].payPeriodStart", is("2030-01-01")));

        mockMvc.perform(get("/api/payslips?page=-1").header("Authorization", bearer(employee)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
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
        User systemAdmin = userRepository.findByUsername("sysadmin1").orElseThrow();
        mockMvc.perform(get("/api/payslips").header("Authorization", bearer(systemAdmin)))
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

    @Test
    void hrCanProvisionPdfAndEmployeeCanListAndDownloadIt() throws Exception {
        byte[] pdf = "%PDF-1.4\nFictional test payslip\n%%EOF".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        var response = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart("/api/hr/payslips")
                        .file(new MockMultipartFile("file", "private-name.pdf", "application/pdf", pdf))
                        .param("employeeProfileId", employeeProfile.getId().toString())
                        .param("payPeriodStart", "2030-01-01")
                        .param("payPeriodEnd", "2030-01-31")
                        .header("Authorization", bearer(hrAdmin)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.payPeriodStart", is("2030-01-01")))
                .andExpect(jsonPath("$.correlationId").isNotEmpty())
                .andReturn();

        Long payslipId = objectMapper.readTree(response.getResponse().getContentAsString())
                .path("data").path("id").asLong();
        Payslip provisioned = payslipRepository.findById(payslipId).orElseThrow();
        assertTrue(provisioned.getStorageKey().matches(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.pdf"));
        assertArrayEquals(pdf, Files.readAllBytes(storageDirectory.resolve(provisioned.getStorageKey())));
        assertTrue(auditEventRepository.findTop100ByActorUsernameOrderByOccurredAtDesc("hradmin1").stream()
                .anyMatch(event -> event.getEventType().equals("PAYSLIP_PROVISIONED")
                        && event.getTargetId().equals(payslipId)));

        mockMvc.perform(get("/api/payslips").header("Authorization", bearer(employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()", is(1)));
        var download = mockMvc.perform(get("/api/payslips/{id}/download", payslipId)
                        .header("Authorization", bearer(employee)))
                .andExpect(status().isOk())
                .andReturn();
        assertArrayEquals(pdf, download.getResponse().getContentAsByteArray());
    }

    @Test
    void onlyHrCanProvisionAndInvalidOrDuplicatePdfsAreRejected() throws Exception {
        byte[] pdf = "%PDF-1.4\nFictional test payslip\n%%EOF".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        mockMvc.perform(provisionRequest(employee, "2030-02-01", "2030-02-28", pdf, "slip.pdf"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
        User systemAdmin = userRepository.findByUsername("sysadmin1").orElseThrow();
        mockMvc.perform(provisionRequest(systemAdmin, "2030-02-01", "2030-02-28", pdf, "slip.pdf"))
                .andExpect(status().isForbidden());

        mockMvc.perform(provisionRequest(hrAdmin, "2030-02-01", "2030-02-28",
                        "not actually a pdf".getBytes(java.nio.charset.StandardCharsets.US_ASCII), "not-a-pdf.pdf"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(provisionRequest(hrAdmin, "2030-02-01", "2030-02-28", pdf, "slip.pdf"))
                .andExpect(status().isCreated());
        mockMvc.perform(provisionRequest(hrAdmin, "2030-02-01", "2030-02-28", pdf, "duplicate.pdf"))
                .andExpect(status().isConflict());
    }

    @Test
    void provisioningRejectsOversizedFilesAndReversedPayPeriods() throws Exception {
        byte[] pdf = "%PDF-1.4\nFictional test payslip\n%%EOF".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        mockMvc.perform(provisionRequest(hrAdmin, "2030-03-01", "2030-03-31",
                        new byte[10 * 1024 * 1024 + 1], "oversized.pdf"))
                .andExpect(status().isPayloadTooLarge());
        mockMvc.perform(provisionRequest(hrAdmin, "2030-03-31", "2030-03-01", pdf, "invalid-period.pdf"))
                .andExpect(status().isBadRequest());
        org.junit.jupiter.api.Assertions.assertEquals(0, payslipRepository.count());
    }

    @Test
    void payslipProvisioningIsRateLimitedAndAudited() throws Exception {
        User limitedHr = userRepository.save(new User("limited-hr", "limited-hr@essp.local",
                "not-used-by-this-test", Role.HR_ADMIN));
        byte[] pdf = "%PDF-1.4\nFictional test payslip\n%%EOF".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        for (int month = 1; month <= 10; month++) {
            LocalDate start = LocalDate.of(2030, 1, 1).plusMonths(month - 1L);
            mockMvc.perform(provisionRequest(limitedHr, start.toString(),
                            start.plusDays(27).toString(), pdf, "period-" + month + ".pdf"))
                    .andExpect(status().isCreated());
        }
        mockMvc.perform(provisionRequest(limitedHr, "2031-01-01", "2031-01-28", pdf, "eleventh.pdf"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.correlationId").isNotEmpty());

        assertTrue(auditEventRepository.findTop100ByActorUsernameOrderByOccurredAtDesc("limited-hr").stream()
                .anyMatch(event -> event.getEventType().equals("RATE_LIMIT_EXCEEDED")));
    }

    @Test
    void concurrentDuplicateUploadsCreateOnlyOnePayslip() throws Exception {
        User raceHr = userRepository.save(new User("race-hr", "race-hr@essp.local",
                "not-used-by-this-test", Role.HR_ADMIN));
        byte[] pdf = "%PDF-1.4\nFictional test payslip\n%%EOF".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Future<Integer>> results = new ArrayList<>();

        try {
            for (int attempt = 0; attempt < 2; attempt++) {
                String filename = "concurrent-" + attempt + ".pdf";
                results.add(executor.submit(() -> {
                    if (!start.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out waiting to start concurrent upload");
                    }
                    return mockMvc.perform(provisionRequest(
                                    raceHr, "2032-01-01", "2032-01-31", pdf, filename))
                            .andReturn().getResponse().getStatus();
                }));
            }
            start.countDown();
            List<Integer> statuses = List.of(
                    results.get(0).get(30, TimeUnit.SECONDS),
                    results.get(1).get(30, TimeUnit.SECONDS));

            org.junit.jupiter.api.Assertions.assertEquals(1,
                    statuses.stream().filter(status -> status == 201).count());
            org.junit.jupiter.api.Assertions.assertEquals(1,
                    statuses.stream().filter(status -> status == 409).count());
            org.junit.jupiter.api.Assertions.assertEquals(1, payslipRepository.count());
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder provisionRequest(
            User actor, String start, String end, byte[] pdf, String filename) {
        return org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .multipart("/api/hr/payslips")
                .file(new MockMultipartFile("file", filename, "application/pdf", pdf))
                .param("employeeProfileId", employeeProfile.getId().toString())
                .param("payPeriodStart", start)
                .param("payPeriodEnd", end)
                .header("Authorization", bearer(actor));
    }

    private Payslip createPayslip(EmployeeProfile profile, boolean createFile) throws Exception {
        return createPayslip(profile, createFile
                ? "%PDF-1.4\nFictional test payslip\n%%EOF".getBytes(java.nio.charset.StandardCharsets.UTF_8)
                : null);
    }

    private Payslip createPayslip(EmployeeProfile profile, byte[] pdf) throws Exception {
        return createPayslip(profile, pdf, LocalDate.of(2030, 1, 1), LocalDate.of(2030, 1, 31));
    }

    private Payslip createPayslip(EmployeeProfile profile, byte[] pdf,
                                  LocalDate periodStart, LocalDate periodEnd) throws Exception {
        String storageKey = UUID.randomUUID() + ".pdf";
        if (pdf != null) {
            Files.createDirectories(storageDirectory);
            Files.write(storageDirectory.resolve(storageKey), pdf);
        }
        Payslip payslip = new Payslip();
        payslip.setEmployeeProfile(profile);
        payslip.setPayPeriodStart(periodStart);
        payslip.setPayPeriodEnd(periodEnd);
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
