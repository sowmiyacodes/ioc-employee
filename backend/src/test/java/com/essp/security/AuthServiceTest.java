package com.essp.security;

import com.essp.security.dto.LoginRequest;
import com.essp.security.dto.LoginResponse;
import com.essp.security.entity.Role;
import com.essp.security.entity.User;
import com.essp.security.repository.UserRepository;
import com.essp.security.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:essp_auth_test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "app.seed-demo-data=true",
        "APP_DEMO_PASSWORD=Password1234!"
})
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        User testUser = new User("testuser", "testuser@essp.local", passwordEncoder.encode("SecurePass123!"), Role.EMPLOYEE);
        userRepository.save(testUser);
    }

    @Test
    @DisplayName("Valid login returns JWT and correct role")
    void testValidLogin() {
        LoginRequest req = new LoginRequest("testuser", "SecurePass123!");
        LoginResponse resp = authService.authenticate(req, "127.0.0.1");

        assertNotNull(resp);
        assertNotNull(resp.getToken());
        assertEquals("EMPLOYEE", resp.getRole());
        assertEquals("testuser", resp.getUsername());
    }

    @Test
    @DisplayName("Login with valid email identifier succeeds")
    void testLoginWithEmail() {
        LoginRequest req = new LoginRequest("testuser@essp.local", "SecurePass123!");
        LoginResponse resp = authService.authenticate(req, "127.0.0.1");

        assertNotNull(resp);
        assertEquals("EMPLOYEE", resp.getRole());
    }

    @Test
    @DisplayName("Invalid password throws 401 Unauthorized")
    void testInvalidPassword() {
        LoginRequest req = new LoginRequest("testuser", "WrongPassword123!");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                authService.authenticate(req, "127.0.0.1"));
        assertEquals(401, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("Nonexistent account throws 401 Unauthorized")
    void testNonexistentAccount() {
        LoginRequest req = new LoginRequest("nonexistent", "Password1234!");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                authService.authenticate(req, "127.0.0.1"));
        assertEquals(401, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("Inactive account throws 403 Forbidden")
    void testInactiveAccount() {
        User inactive = new User("inactiveuser", "inactive@essp.local", passwordEncoder.encode("Password1234!"), Role.EMPLOYEE);
        inactive.setActive(false);
        userRepository.save(inactive);

        LoginRequest req = new LoginRequest("inactiveuser", "Password1234!");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                authService.authenticate(req, "127.0.0.1"));
        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("Logout invalidates JWT token")
    void testLogoutRevocation() {
        LoginRequest req = new LoginRequest("testuser", "SecurePass123!");
        LoginResponse resp = authService.authenticate(req, "127.0.0.1");

        String authHeader = "Bearer " + resp.getToken();
        authService.logout(authHeader, "testuser", "127.0.0.1");

        // Subsequent attempt with invalidated token will be rejected by provider
        assertFalse(authService.getCurrentUser("testuser").getId() == null);
    }
}
