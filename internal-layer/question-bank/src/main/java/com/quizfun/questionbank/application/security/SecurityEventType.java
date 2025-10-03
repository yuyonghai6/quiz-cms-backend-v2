package com.quizfun.questionbank.application.security;

/**
 * Enumeration of security event types for audit logging and threat detection.
 *
 * This enum categorizes different types of security violations and suspicious
 * activities detected by the system. Each event type is used for:
 * - Structured logging in MongoDB
 * - Security monitoring and alerting
 * - Compliance reporting and audit trails
 *
 * @see SecurityEvent
 * @see SecurityAuditLogger
 */
public enum SecurityEventType {

    /**
     * User authentication token is missing or invalid.
     * Severity: HIGH
     * Example: Request without JWT token or expired token
     */
    MISSING_AUTHENTICATION,

    /**
     * Authentication token type is not supported.
     * Severity: HIGH
     * Example: Non-JWT authentication attempt
     */
    INVALID_TOKEN_TYPE,

    /**
     * JWT token has invalid or missing subject claim.
     * Severity: HIGH
     * Example: Malformed JWT token without user ID
     */
    INVALID_TOKEN_SUBJECT,

    /**
     * Unauthorized access attempt detected.
     * Severity: HIGH
     * Example: User attempting to access resources they don't own
     */
    UNAUTHORIZED_ACCESS_ATTEMPT,

    /**
     * Path parameter manipulation detected.
     * Severity: CRITICAL
     * Example: JWT token user ID doesn't match path parameter user ID
     */
    PATH_PARAMETER_MANIPULATION,

    /**
     * Attempt to escalate token privileges.
     * Severity: CRITICAL
     * Example: User trying to access admin-only resources
     */
    TOKEN_PRIVILEGE_ESCALATION,

    /**
     * Session hijacking attempt detected.
     * Severity: CRITICAL
     * Example: Session used from different IP or location
     */
    SESSION_HIJACKING_ATTEMPT,

    /**
     * Invalid or expired session token.
     * Severity: HIGH
     * Example: Session token doesn't match active sessions
     */
    INVALID_SESSION_TOKEN,

    /**
     * Multiple concurrent sessions from different locations.
     * Severity: MEDIUM
     * Example: Same user logged in from US and China simultaneously
     */
    CONCURRENT_SESSION_VIOLATION,

    /**
     * Rate limit threshold exceeded.
     * Severity: MEDIUM
     * Example: User making too many requests in short time
     */
    RATE_LIMIT_EXCEEDED,

    /**
     * Unusual access pattern detected.
     * Severity: MEDIUM
     * Example: Access at unusual hours or from unusual locations
     */
    UNUSUAL_ACCESS_PATTERN,

    /**
     * Repeated authorization failures.
     * Severity: HIGH
     * Example: Multiple failed access attempts in short period
     */
    REPEATED_AUTHORIZATION_FAILURES,

    /**
     * General security validation error.
     * Severity: HIGH
     * Example: Unexpected error during security checks
     */
    SECURITY_VALIDATION_ERROR,

    /**
     * Successful security validation (for monitoring).
     * Severity: INFO
     * Example: Normal successful access for baseline monitoring
     */
    SECURITY_VALIDATION_SUCCESS
}
