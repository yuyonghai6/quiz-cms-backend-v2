package com.quizfun.questionbank.application.security;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for logging security events and audit information.
 *
 * Enhanced version with MongoDB persistence support as per US-021 requirements.
 * Provides both synchronous and asynchronous logging capabilities for
 * comprehensive audit trails and compliance (GDPR, FERPA).
 *
 * @see SecurityEvent
 * @see SecurityEventType
 */
public interface SecurityAuditLogger {

    /**
     * Logs a security violation event with comprehensive audit information (legacy method).
     *
     * @deprecated Use {@link #logSecurityEvent(SecurityEvent)} instead
     * @param eventType The type of security violation (e.g., "PATH_PARAMETER_MANIPULATION")
     * @param tokenUserId The user ID extracted from the JWT token
     * @param details Additional details about the security violation
     */
    @Deprecated
    void logSecurityViolation(String eventType, Long tokenUserId, String details);

    /**
     * Logs successful access for monitoring and compliance purposes (legacy method).
     *
     * @deprecated Use {@link #logSecurityEvent(SecurityEvent)} instead
     * @param userId The user ID that successfully accessed the resource
     * @param questionBankId The question bank ID that was accessed
     * @param operation The operation that was performed (e.g., "UPSERT")
     */
    @Deprecated
    void logSuccessfulAccess(Long userId, Long questionBankId, String operation);

    /**
     * Logs a security event synchronously to MongoDB.
     *
     * This method persists the security event to MongoDB for audit trails and compliance.
     * Failures are logged but do not propagate to prevent impacting request processing.
     *
     * @param event The security event to log
     */
    void logSecurityEvent(SecurityEvent event);

    /**
     * Logs a security event asynchronously to MongoDB.
     *
     * This method returns immediately and processes the logging in a background thread,
     * ensuring no impact on request processing performance.
     *
     * @param event The security event to log
     * @return CompletableFuture that completes when logging is done
     */
    CompletableFuture<Void> logSecurityEventAsync(SecurityEvent event);
}