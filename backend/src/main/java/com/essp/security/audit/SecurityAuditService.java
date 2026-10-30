package com.essp.security.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SecurityAuditService {

    private static final Logger auditLogger = LoggerFactory.getLogger("SECURITY_AUDIT");
    private final AuditEventRepository auditEventRepository;

    public SecurityAuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    public void logLoginSuccess(String username, String role, String ipAddress) {
        auditLogger.info("[AUDIT-SUCCESS] timestamp={}, event=LOGIN_SUCCESS, username={}, role={}, ip={}",
                LocalDateTime.now(), sanitize(username), sanitize(role), sanitize(ipAddress));
        record("LOGIN_SUCCESS", username, null, ipAddress, "role=" + sanitize(role));
    }

    public void logLoginFailure(String username, String reason, String ipAddress) {
        auditLogger.warn("[AUDIT-FAILURE] timestamp={}, event=LOGIN_FAILURE, username={}, reason={}, ip={}",
                LocalDateTime.now(), sanitize(username), sanitize(reason), sanitize(ipAddress));
        record("LOGIN_FAILURE", username, null, ipAddress, "reason=" + sanitize(reason));
    }

    public void logLogout(String username, String ipAddress) {
        auditLogger.info("[AUDIT-LOGOUT] timestamp={}, event=LOGOUT, username={}, ip={}",
                LocalDateTime.now(), sanitize(username), sanitize(ipAddress));
        record("LOGOUT", username, null, ipAddress, null);
    }

    public void logAccessDenied(String username, String requestedUri, String ipAddress) {
        auditLogger.warn("[AUDIT-DENIED] timestamp={}, event=ACCESS_DENIED, username={}, uri={}, ip={}",
                LocalDateTime.now(), sanitize(username), sanitize(requestedUri), sanitize(ipAddress));
        record("ACCESS_DENIED", username, null, ipAddress, "uri=" + sanitize(requestedUri));
    }

    public void logRateLimitExceeded(String ipAddress, String targetIdentifier) {
        auditLogger.warn("[AUDIT-THROTTLED] timestamp={}, event=RATE_LIMIT_EXCEEDED, target={}, ip={}",
                LocalDateTime.now(), sanitize(targetIdentifier), sanitize(ipAddress));
        record("RATE_LIMIT_EXCEEDED", targetIdentifier, null, ipAddress, null);
    }

    public void logAddressChange(String username, Map<String, String> before, Map<String, String> after) {
        String details = "before=" + digestSnapshot(before) + ";after=" + digestSnapshot(after);
        auditLogger.info("[AUDIT-ADDRESS] timestamp={}, event=ADDRESS_CHANGED, username={}, {}",
                LocalDateTime.now(), sanitize(username), details);
        record("ADDRESS_CHANGED", username, null, null, details);
    }

    public void logLeaveEvent(String username, String event, Long recordId) {
        auditLogger.info("[AUDIT-LEAVE] timestamp={}, event={}, username={}, recordId={}",
                LocalDateTime.now(), sanitize(event), sanitize(username), recordId);
        record(event, username, recordId, null, null);
    }

    public void logAdministrationEvent(String actor, String event, Long targetId, String details) {
        auditLogger.info("[AUDIT-ADMIN] timestamp={}, event={}, actor={}, targetId={}, details={}",
                LocalDateTime.now(), sanitize(event), sanitize(actor), targetId, sanitize(details));
        record(event, actor, targetId, null, details);
    }

    private void record(String eventType, String actor, Long targetId, String sourceIp, String details) {
        AuditEvent event = new AuditEvent();
        event.setEventType(sanitize(eventType, 60));
        event.setActorUsername(sanitize(actor, 100));
        event.setTargetId(targetId);
        event.setSourceIp(sourceIp == null ? null : sanitize(sourceIp, 45));
        event.setDetails(details == null ? null : sanitize(details, 2000));
        auditEventRepository.save(event);
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

    private String sanitize(String input) {
        return sanitize(input, 2000);
    }

    private String sanitize(String input, int maxLength) {
        if (input == null) return "UNKNOWN";
        String safe = input.replaceAll("[\r\n]", "_");
        return safe.length() > maxLength ? safe.substring(0, maxLength) : safe;
    }
}
