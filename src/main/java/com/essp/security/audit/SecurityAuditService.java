package com.essp.security.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
<<<<<<< HEAD
=======
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.stream.Collectors;
>>>>>>> 5d95ee7 (implemented employee profile and address management)

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

<<<<<<< HEAD
=======
    public void logAddressChange(String username, Map<String, String> before, Map<String, String> after) {
        auditLogger.info("[AUDIT-ADDRESS] timestamp={}, event=ADDRESS_CHANGED, username={}, before={}, after={}",
                LocalDateTime.now(), sanitize(username), digestSnapshot(before), digestSnapshot(after));
    }

    private String digestSnapshot(Map<String, String> values) {
        return values.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + digest(entry.getValue()))
                .collect(Collectors.joining(",", "{", "}"));
    }

    private String digest(String value) {
        if (value == null) return "ABSENT";
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte current : hash) {
                result.append(String.format("%02x", current));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is required for address audit hashing", ex);
        }
    }

>>>>>>> 5d95ee7 (implemented employee profile and address management)
    private String sanitize(String input) {
        if (input == null) return "UNKNOWN";
        // Remove line breaks to prevent log injection
        return input.replaceAll("[\r\n]", "_");
    }
}
