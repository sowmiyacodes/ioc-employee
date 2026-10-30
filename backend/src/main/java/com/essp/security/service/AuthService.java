package com.essp.security.service;

import com.essp.security.audit.SecurityAuditService;
import com.essp.security.dto.LoginRequest;
import com.essp.security.dto.LoginResponse;
import com.essp.security.dto.UserProfileDto;
import com.essp.security.entity.User;
import com.essp.security.jwt.JwtTokenProvider;
import com.essp.security.ratelimit.LoginRateLimiterService;
import com.essp.security.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final LoginRateLimiterService rateLimiterService;
    private final SecurityAuditService auditService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       LoginRateLimiterService rateLimiterService,
                       SecurityAuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.rateLimiterService = rateLimiterService;
        this.auditService = auditService;
    }

    public LoginResponse authenticate(LoginRequest request, String clientIp) {
        String identifier = request.getUsername().trim();

        // 1. Rate Limiting check per Client IP and Account Identifier
        String rateLimitKey = clientIp + "_" + identifier.toLowerCase();
        if (!rateLimiterService.tryConsume(rateLimitKey)) {
            auditService.logRateLimitExceeded(clientIp, identifier);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many login attempts. Please try again later.");
        }

        // 2. Fetch User by username or email
        Optional<User> userOpt = userRepository.findByUsernameOrEmail(identifier, identifier);
        if (userOpt.isEmpty()) {
            auditService.logLoginFailure(identifier, "USER_NOT_FOUND", clientIp);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        User user = userOpt.get();

        // 3. Check Account Status
        if (!user.isActive()) {
            auditService.logLoginFailure(identifier, "ACCOUNT_INACTIVE", clientIp);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is disabled. Contact your administrator.");
        }

        if (!user.isAccountNonLocked()) {
            auditService.logLoginFailure(identifier, "ACCOUNT_LOCKED", clientIp);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is locked. Contact your administrator.");
        }

        // 4. Verify Password Hash using BCrypt
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            auditService.logLoginFailure(identifier, "INVALID_PASSWORD", clientIp);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        // 5. Successful Login: generate JWT token with trusted server-side role
        String roleName = user.getRole().name();
        String token = tokenProvider.generateToken(user.getUsername(), roleName, user.getId());

        auditService.logLoginSuccess(user.getUsername(), roleName, clientIp);

        return new LoginResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                roleName,
                "Authentication successful"
        );
    }

    public void logout(String authHeader, String username, String clientIp) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            tokenProvider.invalidateToken(token);
        }
        auditService.logLogout(username != null ? username : "ANONYMOUS", clientIp);
    }

    public UserProfileDto getCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return new UserProfileDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                user.isActive()
        );
    }
}
