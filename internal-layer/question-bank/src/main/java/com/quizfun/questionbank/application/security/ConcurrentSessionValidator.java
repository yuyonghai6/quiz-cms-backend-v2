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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Concurrent Session Validator for enforcing session limits.
 *
 * Prevents account sharing and session abuse by:
 * - Limiting the number of concurrent active sessions per user
 * - Tracking active session IDs per user
 * - Detecting concurrent session violations
 * - Optionally invalidating oldest sessions when limit exceeded
 *
 * This is a simplified implementation using in-memory storage.
 * Production systems should use Redis or a distributed session store.
 *
 * Session Limits:
 * - Default: 3 concurrent sessions per user
 * - Sessions timeout after 30 minutes of inactivity
 */
@Component
public class ConcurrentSessionValidator extends ValidationHandler {

    private static final Logger logger = LoggerFactory.getLogger(ConcurrentSessionValidator.class);

    // Configuration
    private static final int MAX_CONCURRENT_SESSIONS = 3;
    private static final long SESSION_TIMEOUT_MS = 30 * 60 * 1000;  // 30 minutes

    private final SecurityAuditLogger auditLogger;
    private final RetryHelper retryHelper;
    private final ValidationChainMetrics metrics;

    // In-memory session registry (use Redis in production)
    private final Map<Long, UserSessionRegistry> sessionRegistry = new ConcurrentHashMap<>();

    public ConcurrentSessionValidator(SecurityAuditLogger auditLogger,
                                     RetryHelper retryHelper,
                                     ValidationChainMetrics metrics) {
        this.auditLogger = auditLogger;
        this.retryHelper = retryHelper;
        this.metrics = metrics;
        logger.info("ConcurrentSessionValidator initialized with max {} concurrent sessions",
            MAX_CONCURRENT_SESSIONS);
    }

    @Override
    public Result<Void> validate(Object command) {
        if (!(command instanceof UpsertQuestionCommand upsertCommand)) {
            return checkNext(command);
        }

        long startTime = System.nanoTime();

        try {
            Long userId = upsertCommand.getUserId();
            logger.debug("Checking concurrent session limit for user {}", userId);

            // Extract session information from JWT
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                Jwt jwt = jwtAuth.getToken();
                String sessionId = extractSessionId(jwt);

                // Validate concurrent session limit
                Result<Void> validationResult = validateConcurrentSessions(userId, sessionId);
                if (validationResult.isFailure()) {
                    logSessionViolation(userId, sessionId, validationResult.getError());
                    return validationResult;
                }
            }

            // Pass to next validator
            return checkNext(command);

        } finally {
            metrics.stopTimer(startTime, "ConcurrentSessionValidator");
        }
    }

    /**
     * Validates that user hasn't exceeded concurrent session limit.
     */
    private Result<Void> validateConcurrentSessions(Long userId, String sessionId) {
        if (sessionId == null) {
            // No session ID means testing mode or stateless operation
            return Result.success(null);
        }

        Instant now = Instant.now();

        // Get or create user's session registry
        UserSessionRegistry registry = sessionRegistry.computeIfAbsent(
            userId,
            k -> new UserSessionRegistry()
        );

        // Clean up expired sessions
        int expiredCount = registry.cleanupExpiredSessions(now);
        if (expiredCount > 0) {
            logger.debug("Cleaned up {} expired sessions for user {}", expiredCount, userId);
        }

        // Check if this is a known session
        if (registry.hasSession(sessionId)) {
            // Update last activity time for existing session
            registry.updateSessionActivity(sessionId, now);
            logger.debug("Existing session {} for user {} updated", sessionId, userId);
            return Result.success(null);
        }

        // New session - check if limit exceeded
        int activeSessionCount = registry.getActiveSessionCount();

        if (activeSessionCount >= MAX_CONCURRENT_SESSIONS) {
            logger.warn("Concurrent session limit exceeded for user {}: {} active sessions",
                userId, activeSessionCount);

            // Log security event
            auditLogger.logSecurityEvent(
                SecurityEvent.builder()
                    .type(SecurityEventType.CONCURRENT_SESSION_VIOLATION)
                    .severity(SeverityLevel.HIGH)
                    .userId(userId)
                    .sessionId(sessionId)
                    .details(Map.of(
                        "activeSessions", activeSessionCount,
                        "maxSessions", MAX_CONCURRENT_SESSIONS,
                        "newSessionId", sessionId,
                        "action", "REJECT_NEW_SESSION"
                    ))
                    .build()
            );

            return Result.failure("TOO_MANY_SESSIONS",
                String.format("User has %d active sessions (maximum: %d). Please log out from another device.",
                    activeSessionCount, MAX_CONCURRENT_SESSIONS));
        }

        // Register new session
        registry.registerSession(sessionId, now);
        logger.info("New session {} registered for user {} (total: {})",
            sessionId, userId, activeSessionCount + 1);

        // Log session registration (informational)
        auditLogger.logSecurityEvent(
            SecurityEvent.builder()
                .type(SecurityEventType.CONCURRENT_SESSION_VIOLATION)
                .severity(SeverityLevel.INFO)
                .userId(userId)
                .sessionId(sessionId)
                .details(Map.of(
                    "action", "SESSION_REGISTERED",
                    "totalSessions", activeSessionCount + 1,
                    "maxSessions", MAX_CONCURRENT_SESSIONS
                ))
                .build()
        );

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
     * Logs concurrent session violation to audit logger.
     */
    private void logSessionViolation(Long userId, String sessionId, String error) {
        auditLogger.logSecurityEvent(
            SecurityEvent.builder()
                .type(SecurityEventType.CONCURRENT_SESSION_VIOLATION)
                .severity(SeverityLevel.CRITICAL)
                .userId(userId)
                .sessionId(sessionId)
                .details(Map.of("error", error))
                .build()
        );
    }

    /**
     * Registry of active sessions for a single user.
     * Tracks session IDs and their last activity times.
     */
    private static class UserSessionRegistry {
        private final Map<String, Instant> activeSessions = new ConcurrentHashMap<>();

        /**
         * Registers a new session.
         */
        void registerSession(String sessionId, Instant now) {
            activeSessions.put(sessionId, now);
        }

        /**
         * Checks if session exists.
         */
        boolean hasSession(String sessionId) {
            return activeSessions.containsKey(sessionId);
        }

        /**
         * Updates last activity time for a session.
         */
        void updateSessionActivity(String sessionId, Instant now) {
            activeSessions.put(sessionId, now);
        }

        /**
         * Gets count of active sessions.
         */
        int getActiveSessionCount() {
            return activeSessions.size();
        }

        /**
         * Removes expired sessions and returns count of removed sessions.
         */
        int cleanupExpiredSessions(Instant now) {
            long cutoffTime = now.toEpochMilli() - SESSION_TIMEOUT_MS;
            Set<String> expiredSessions = activeSessions.entrySet().stream()
                .filter(entry -> entry.getValue().toEpochMilli() < cutoffTime)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

            expiredSessions.forEach(activeSessions::remove);
            return expiredSessions.size();
        }
    }
}
