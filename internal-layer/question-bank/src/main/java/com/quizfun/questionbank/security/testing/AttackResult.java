package com.quizfun.questionbank.security.testing;

import com.quizfun.questionbank.application.security.SecurityEventType;
import lombok.Builder;
import lombok.Data;

import java.time.Duration;

/**
 * Result of executing a single attack scenario.
 * Contains information about whether the attack was blocked,
 * logged, and performance metrics.
 */
@Data
@Builder
public class AttackResult {
    /**
     * The attack scenario that was executed.
     */
    private AttackScenario scenario;

    /**
     * Whether the attack was successfully blocked by security validators.
     */
    private boolean blocked;

    /**
     * Whether the attack attempt was logged to audit system.
     */
    private boolean auditLogged;

    /**
     * Time taken to validate and block/allow the attack.
     */
    private Duration responseTime;

    /**
     * Type of security event that was triggered (if any).
     */
    private SecurityEventType securityEventType;

    /**
     * Error message if attack execution encountered issues.
     */
    private String error;

    /**
     * Additional details about the attack execution.
     */
    private String details;

    /**
     * Checks if this attack result represents a security success.
     * Success = attack was blocked AND logged within acceptable time.
     */
    public boolean isSecuritySuccess() {
        return blocked && auditLogged && responseTime.toMillis() < 50; // <50ms for security validation
    }

    /**
     * Returns severity of this attack result.
     * Critical if attack bypassed security, High if not logged, etc.
     */
    public ResultSeverity getSeverity() {
        if (!blocked) {
            return ResultSeverity.CRITICAL;  // Attack bypassed security!
        }
        if (!auditLogged) {
            return ResultSeverity.HIGH;  // Attack blocked but not logged
        }
        if (responseTime.toMillis() > 50) {
            return ResultSeverity.MEDIUM;  // Slow response could indicate issues
        }
        return ResultSeverity.LOW;  // All good
    }

    public enum ResultSeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}
