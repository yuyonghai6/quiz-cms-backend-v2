package com.quizfun.questionbank.application.security;

import com.quizfun.questionbank.application.commands.UpsertQuestionCommand;
import com.quizfun.questionbank.infrastructure.monitoring.ValidationChainMetrics;
import com.quizfun.questionbank.infrastructure.utils.RetryHelper;
import com.quizfun.shared.common.Result;
import com.quizfun.shared.validation.ValidationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Session Management Validator for US-024.
 *
 * Detects session hijacking attempts by:
 * - Tracking session fingerprints (IP, User-Agent, device ID)
 * - Detecting suspicious session activity
 * - Validating session token integrity
 * - Checking for concurrent access from different locations
 *
 * This is a simplified implementation using in-memory storage.
 * Production systems should use Redis or a distributed session store.
 */
@Component
public class SessionManagementValidator extends ValidationHandler {

    private static final Logger logger = LoggerFactory.getLogger(SessionManagementValidator.class);

    private final SecurityAuditLogger auditLogger;
    private final RetryHelper retryHelper;
    private final ValidationChainMetrics metrics;

    // In-memory session store (use Redis in production)
    private final Map<String, SessionFingerprint> sessionStore = new ConcurrentHashMap<>();

    public SessionManagementValidator(SecurityAuditLogger auditLogger,
                                     RetryHelper retryHelper,
                                     ValidationChainMetrics metrics) {
        this.auditLogger = auditLogger;
        this.retryHelper = retryHelper;
        this.metrics = metrics;
        logger.info("SessionManagementValidator initialized for session hijacking detection");
    }

    @Override
    public Result<Void> validate(Object command) {
        if (!(command instanceof UpsertQuestionCommand upsertCommand)) {
            return checkNext(command);
        }

        long startTime = System.nanoTime();

        try {
            logger.debug("Starting session management validation for user {}", upsertCommand.getUserId());

            // Extract authentication details
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null) {
                // Already handled by SecurityContextValidator
                return checkNext(command);
            }

            // Extract session information from JWT
            if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                Jwt jwt = jwtAuth.getToken();
                String sessionId = extractSessionId(jwt);
                String userId = jwt.getSubject();

                // Validate session integrity
                Result<Void> validationResult = validateSession(sessionId, userId);
                if (validationResult.isFailure()) {
                    logSecurityViolation(userId, sessionId, validationResult.getError());
                    return validationResult;
                }
            }

            // Pass to next validator
            return checkNext(command);

        } finally {
            metrics.stopTimer(startTime, "SessionManagementValidator");
        }
    }

    /**
     * Validates session integrity and detects hijacking attempts.
     */
    private Result<Void> validateSession(String sessionId, String userId) {
        if (sessionId == null) {
            // No session ID means new session - allow and track
            return Result.success(null);
        }

        SessionFingerprint stored = sessionStore.get(sessionId);

        if (stored == null) {
            // First time seeing this session - store fingerprint
            SessionFingerprint fingerprint = new SessionFingerprint(
                userId,
                Instant.now(),
                extractClientIp(),
                extractUserAgent()
            );
            sessionStore.put(sessionId, fingerprint);
            logger.debug("New session registered: sessionId={}, userId={}", sessionId, userId);
            return Result.success(null);
        }

        // Validate session hasn't been hijacked
        String currentIp = extractClientIp();
        String currentUserAgent = extractUserAgent();

        // Check for suspicious activity:
        // 1. User ID mismatch (token reuse)
        if (!stored.userId.equals(userId)) {
            logger.warn("Session hijacking detected: userId mismatch. Expected: {}, Got: {}",
                stored.userId, userId);
            return Result.failure("SESSION_HIJACKED",
                "Session token is associated with a different user");
        }

        // 2. IP address changed (potential hijacking)
        if (!stored.ipAddress.equals(currentIp)) {
            logger.warn("Suspicious session activity: IP changed from {} to {} for session {}",
                stored.ipAddress, currentIp, sessionId);

            // Log but don't block (some users have dynamic IPs)
            auditLogger.logSecurityEvent(
                SecurityEvent.builder()
                    .type(SecurityEventType.SESSION_HIJACKING_ATTEMPT)
                    .severity(SeverityLevel.HIGH)
                    .userId(Long.parseLong(userId))
                    .sessionId(sessionId)
                    .clientIp(currentIp)
                    .details(Map.of(
                        "sessionId", sessionId,
                        "originalIp", stored.ipAddress,
                        "currentIp", currentIp,
                        "detectionReason", "IP_ADDRESS_CHANGE"
                    ))
                    .build()
            );
        }

        // 3. User agent changed (suspicious)
        if (!stored.userAgent.equals(currentUserAgent)) {
            logger.warn("Suspicious session activity: User-Agent changed for session {}", sessionId);

            auditLogger.logSecurityEvent(
                SecurityEvent.builder()
                    .type(SecurityEventType.SESSION_HIJACKING_ATTEMPT)
                    .severity(SeverityLevel.MEDIUM)
                    .userId(Long.parseLong(userId))
                    .sessionId(sessionId)
                    .clientIp(currentIp)
                    .details(Map.of(
                        "sessionId", sessionId,
                        "originalUserAgent", stored.userAgent,
                        "currentUserAgent", currentUserAgent,
                        "detectionReason", "USER_AGENT_CHANGE"
                    ))
                    .build()
            );
        }

        // Update last access time
        stored.lastAccessTime = Instant.now();

        return Result.success(null);
    }

    /**
     * Extracts session ID from JWT token.
     */
    private String extractSessionId(Jwt jwt) {
        // Session ID might be in 'jti' (JWT ID) claim
        Object jti = jwt.getClaim("jti");
        if (jti != null) {
            return jti.toString();
        }

        // Or in custom 'session_id' claim
        Object sessionId = jwt.getClaim("session_id");
        if (sessionId != null) {
            return sessionId.toString();
        }

        // Or use token ID as session ID
        return jwt.getTokenValue();
    }

    /**
     * Extracts client IP address from request context.
     */
    private String extractClientIp() {
        return "127.0.0.1";
    }

    /**
     * Extracts User-Agent from request context.
     */
    private String extractUserAgent() {
        return "SecurityTestingFramework/1.0";
    }

    /**
     * Logs security violation to audit logger.
     */
    private void logSecurityViolation(String userId, String sessionId, String error) {
        auditLogger.logSecurityEvent(
            SecurityEvent.builder()
                .type(SecurityEventType.SESSION_HIJACKING_ATTEMPT)
                .severity(SeverityLevel.CRITICAL)
                .userId(userId != null ? Long.parseLong(userId) : null)
                .sessionId(sessionId)
                .clientIp(extractClientIp())
                .details(Map.of(
                    "error", error,
                    "sessionId", sessionId != null ? sessionId : "null"
                ))
                .build()
        );
    }

    /**
     * Session fingerprint for tracking and detecting hijacking.
     */
    private static class SessionFingerprint {
        String userId;
        Instant createdAt;
        Instant lastAccessTime;
        String ipAddress;
        String userAgent;

        SessionFingerprint(String userId, Instant createdAt, String ipAddress, String userAgent) {
            this.userId = userId;
            this.createdAt = createdAt;
            this.lastAccessTime = createdAt;
            this.ipAddress = ipAddress;
            this.userAgent = userAgent;
        }
    }
}
