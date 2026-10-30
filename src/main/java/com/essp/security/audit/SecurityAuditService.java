package com.essp.security.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SecurityAuditService {

    private static final Logger auditLogger = LoggerFactory.getLogger("SECURITY_AUDIT");

    public void logLoginSuccess(String username, String role, String ipAddress) {
        auditLogger.info("[AUDIT-SUCCESS] timestamp={}, event=LOGIN_SUCCESS, username={}, role={}, ip={}",
                LocalDateTime.now(), sanitize(username), sanitize(role), sanitize(ipAddress));
    }

    public void logLoginFailure(String username, String reason, String ipAddress) {
        auditLogger.warn("[AUDIT-FAILURE] timestamp={}, event=LOGIN_FAILURE, username={}, reason={}, ip={}",
                LocalDateTime.now(), sanitize(username), sanitize(reason), sanitize(ipAddress));
    }

    public void logLogout(String username, String ipAddress) {
        auditLogger.info("[AUDIT-LOGOUT] timestamp={}, event=LOGOUT, username={}, ip={}",
                LocalDateTime.now(), sanitize(username), sanitize(ipAddress));
    }

    public void logAccessDenied(String username, String requestedUri, String ipAddress) {
        auditLogger.warn("[AUDIT-DENIED] timestamp={}, event=ACCESS_DENIED, username={}, uri={}, ip={}",
                LocalDateTime.now(), sanitize(username), sanitize(requestedUri), sanitize(ipAddress));
    }

    public void logRateLimitExceeded(String ipAddress, String targetIdentifier) {
        auditLogger.warn("[AUDIT-THROTTLED] timestamp={}, event=RATE_LIMIT_EXCEEDED, target={}, ip={}",
                LocalDateTime.now(), sanitize(targetIdentifier), sanitize(ipAddress));
    }

    private String sanitize(String input) {
        if (input == null) return "UNKNOWN";
        // Remove line breaks to prevent log injection
        return input.replaceAll("[\r\n]", "_");
    }
}
