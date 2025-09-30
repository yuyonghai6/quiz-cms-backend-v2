package com.quizfun.questionbank.application.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Simple implementation of SecurityAuditLogger using SLF4J logging.
 *
 * Note: This implementation uses structured logging instead of MongoDB persistence
 * due to project constraints (TestContainers only environment).
 *
 * In a production environment, this would typically write to:
 * - MongoDB audit collection
 * - External SIEM systems
 * - Compliance logging databases
 */
@Component
public class SecurityAuditLoggerImpl implements SecurityAuditLogger {

    private static final Logger securityLogger = LoggerFactory.getLogger("SECURITY_AUDIT");

    @Override
    public void logSecurityViolation(String eventType, Long tokenUserId, String details) {
        securityLogger.warn(
            "SECURITY_VIOLATION: eventType={}, tokenUserId={}, details={}, timestamp={}",
            eventType,
            tokenUserId,
            details,
            Instant.now()
        );

        // In production, this would also:
        // 1. Write to MongoDB audit collection
        // 2. Send alerts to security monitoring systems
        // 3. Update security metrics and dashboards
        // 4. Trigger defensive measures if needed
    }

    @Override
    public void logSuccessfulAccess(Long userId, Long questionBankId, String operation) {
        securityLogger.info(
            "SUCCESSFUL_ACCESS: userId={}, questionBankId={}, operation={}, timestamp={}",
            userId,
            questionBankId,
            operation,
            Instant.now()
        );

        // In production, this would also:
        // 1. Write to MongoDB audit collection for compliance
        // 2. Update access patterns for anomaly detection
        // 3. Track usage metrics for monitoring
    }
}