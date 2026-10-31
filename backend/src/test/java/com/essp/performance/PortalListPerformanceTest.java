package com.essp.performance;

import com.essp.employee.entity.EmployeeProfile;
import com.essp.employee.repository.EmployeeProfileRepository;
import com.essp.payslip.entity.Payslip;
import com.essp.payslip.repository.PayslipRepository;
import com.essp.security.entity.Role;
import com.essp.security.entity.User;
import com.essp.security.jwt.JwtTokenProvider;
import com.essp.security.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:essp_performance_test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "app.seed-demo-data=false",
        "app.payslip.storage-directory=target/performance-payslips"
})
class PortalListPerformanceTest {

    private static final int CONCURRENT_USERS = 25;
    private static final int REQUESTS_PER_USER = 10;

    private record BenchmarkUser(String token, Role role) {}
    private record RequestTiming(String endpoint, long durationMillis) {}

    @LocalServerPort
    private int port;

    @Autowired private UserRepository userRepository;
    @Autowired private EmployeeProfileRepository profileRepository;
    @Autowired private PayslipRepository payslipRepository;
    @Autowired private JwtTokenProvider tokenProvider;

    @Test
    void normalPaginatedListsMeetTwoSecondP95WithTwentyFiveConcurrentUsers() throws Exception {
        List<BenchmarkUser> users = createUsersAndPayslips();
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        CountDownLatch ready = new CountDownLatch(CONCURRENT_USERS);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_USERS);
        List<Future<List<RequestTiming>>> futures = new ArrayList<>();

        try {
            for (BenchmarkUser user : users) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out waiting for benchmark start");
                    }
                    List<RequestTiming> timings = new ArrayList<>(REQUESTS_PER_USER);
                    for (int requestNumber = 0; requestNumber < REQUESTS_PER_USER; requestNumber++) {
                        String path = pathFor(user.role(), requestNumber);
                        HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create("http://localhost:" + port + path))
                                .header("Authorization", "Bearer " + user.token())
                                .timeout(Duration.ofSeconds(10))
                                .GET()
                                .build();
                        long startedAt = System.nanoTime();
                        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                        timings.add(new RequestTiming(endpointName(path),
                                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)));
                        assertEquals(200, response.statusCode(), response.body());
                        if (path.startsWith("/api/payslips")) {
                            assertTrue(response.body().contains("\"totalElements\":20"), response.body());
                        }
                    }
                    return timings;
                }));
            }

            assertTrue(ready.await(10, TimeUnit.SECONDS), "All benchmark users should be ready");
            start.countDown();
            Map<String, List<Long>> timingsByEndpoint = new HashMap<>();
            for (Future<List<RequestTiming>> future : futures) {
                for (RequestTiming timing : future.get(60, TimeUnit.SECONDS)) {
                    timingsByEndpoint.computeIfAbsent(timing.endpoint(), ignored -> new ArrayList<>())
                            .add(timing.durationMillis());
                }
            }

            timingsByEndpoint.forEach((endpoint, timings) -> {
                timings.sort(Comparator.naturalOrder());
                long p95Millis = timings.get((int) Math.ceil(timings.size() * 0.95) - 1);
                System.out.printf("List benchmark: endpoint=%s, users=%d, requests=%d, p95=%dms%n",
                        endpoint, CONCURRENT_USERS, timings.size(), p95Millis);
                assertTrue(p95Millis < 2_000,
                        "Expected p95 below 2000ms for " + endpoint + " with 25 concurrent users, got "
                                + p95Millis + "ms");
            });
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private List<BenchmarkUser> createUsersAndPayslips() {
        List<User> accounts = new ArrayList<>(CONCURRENT_USERS);
        for (int userIndex = 0; userIndex < CONCURRENT_USERS; userIndex++) {
            String username = "performance-user-" + userIndex;
            accounts.add(userRepository.save(new User(username, username + "@essp.test",
                    "not-used-by-this-test", roleForUser(userIndex))));
        }

        User manager = accounts.stream().filter(user -> user.getRole() == Role.MANAGER)
                .findFirst().orElseThrow();
        List<BenchmarkUser> users = new ArrayList<>(CONCURRENT_USERS);
        List<Payslip> payslips = new ArrayList<>(12 * 20);
        int accountIndex = 0;
        for (User user : accounts) {
            EmployeeProfile profile = new EmployeeProfile();
            profile.setUser(user);
            profile.setEmployeeId("PERF-" + String.format("%04d", accountIndex++));
            profile.setLegalName("Performance User " + user.getUsername());
            profile.setDepartment("Performance Test");
            profile.setDesignation(user.getRole().name());
            profile.setJoiningDate(LocalDate.of(2020, 1, 1));
            if (user.getRole() == Role.EMPLOYEE) {
                profile.setManager(manager);
            }
            profile = profileRepository.save(profile);

            users.add(new BenchmarkUser(
                    tokenProvider.generateToken(user.getUsername(), user.getRole().name(), user.getId()),
                    user.getRole()));
            if (user.getRole() == Role.EMPLOYEE) {
                for (int slipIndex = 0; slipIndex < 20; slipIndex++) {
                    Payslip payslip = new Payslip();
                    payslip.setEmployeeProfile(profile);
                    payslip.setPayPeriodStart(LocalDate.of(2025, 1, 1).plusMonths(slipIndex));
                    payslip.setPayPeriodEnd(payslip.getPayPeriodStart().plusDays(29));
                    payslip.setStorageKey(UUID.randomUUID() + ".pdf");
                    payslips.add(payslip);
                }
            }
        }
        payslipRepository.saveAllAndFlush(payslips);
        return users;
    }

    private Role roleForUser(int index) {
        if (index < 12) return Role.EMPLOYEE;
        if (index < 16) return Role.MANAGER;
        if (index < 21) return Role.HR_ADMIN;
        return Role.SYSTEM_ADMIN;
    }

    private String pathFor(Role role, int requestNumber) {
        return switch (role) {
            case EMPLOYEE -> requestNumber % 2 == 0
                    ? "/api/payslips?page=0&size=20"
                    : "/api/leaves/my?page=0&size=20";
            case MANAGER -> "/api/manager/team/leave-requests?page=0&size=20";
            case HR_ADMIN -> "/api/hr/employees?page=0&size=20";
            case SYSTEM_ADMIN -> "/api/admin/users?page=0&size=20";
        };
    }

    private String endpointName(String path) {
        return path.substring(0, path.indexOf('?'));
    }
}
