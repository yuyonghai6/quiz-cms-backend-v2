package com.quizfun.questionbank.application.security;

/**
 * Severity levels for security events.
 *
 * Used to classify the criticality of security events for:
 * - Alert prioritization
 * - Automated response thresholds
 * - Compliance reporting
 * - Security metrics and dashboards
 *
 * @see SecurityEvent
 * @see SecurityEventType
 */
public enum SeverityLevel {

    /**
     * Informational events for monitoring and baseline establishment.
     * Examples: Successful authentication, normal access patterns
     * Response: Log only, no alerts
     */
    INFO,

    /**
     * Low severity events that may indicate potential issues.
     * Examples: Single failed login attempt, minor validation errors
     * Response: Log and monitor for patterns
     */
    LOW,

    /**
     * Medium severity events requiring monitoring.
     * Examples: Rate limit exceeded, unusual access patterns
     * Response: Log, monitor, notify security team during business hours
     */
    MEDIUM,

    /**
     * High severity events requiring immediate attention.
     * Examples: Unauthorized access attempts, repeated failures
     * Response: Log, alert security team, increase monitoring
     */
    HIGH,

    /**
     * Critical security events requiring immediate response.
     * Examples: Path parameter manipulation, session hijacking, privilege escalation
     * Response: Log, immediate alert, automated defensive measures, incident response
     */
    CRITICAL
}
