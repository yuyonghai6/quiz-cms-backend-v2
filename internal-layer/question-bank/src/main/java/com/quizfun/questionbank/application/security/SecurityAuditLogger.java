package com.quizfun.questionbank.application.security;

/**
 * Interface for logging security events and audit information.
 *
 * Note: Due to project constraints (TestContainers only), this implementation
 * uses simplified logging instead of MongoDB persistence.
 */
public interface SecurityAuditLogger {

    /**
     * Logs a security violation event with comprehensive audit information.
     *
     * @param eventType The type of security violation (e.g., "PATH_PARAMETER_MANIPULATION")
     * @param tokenUserId The user ID extracted from the JWT token
     * @param details Additional details about the security violation
     */
    void logSecurityViolation(String eventType, Long tokenUserId, String details);

    /**
     * Logs successful access for monitoring and compliance purposes.
     *
     * @param userId The user ID that successfully accessed the resource
     * @param questionBankId The question bank ID that was accessed
     * @param operation The operation that was performed (e.g., "UPSERT")
     */
    void logSuccessfulAccess(Long userId, Long questionBankId, String operation);
}