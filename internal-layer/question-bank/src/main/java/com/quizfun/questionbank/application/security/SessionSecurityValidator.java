package com.quizfun.questionbank.application.security;

import com.quizfun.questionbank.application.commands.UpsertQuestionCommand;
import com.quizfun.questionbank.domain.validation.ValidationErrorCode;
import com.quizfun.shared.common.Result;
import com.quizfun.shared.validation.ValidationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SessionSecurityValidator validates session security to detect session hijacking attempts.
 *
 * Enhanced with US-024: Session Hijacking Detection System
 * - IP address consistency validation
 * - User-Agent string consistency validation
 * - Session hijacking detection and logging
 * - Integration with US-021 SecurityAuditLogger for comprehensive audit trails
 *
 * This validator implements defense-in-depth security by:
 * 1. Validating session IP address consistency
 * 2. Validating session User-Agent consistency
 * 3. Detecting session hijacking attempts
 * 4. Logging security events with full context
 * 5. Integration with existing validation chain infrastructure
 *
 * Performance: Validation completes in <30ms as required by US-024
 *
 * @see SessionContext
 * @see SecurityEvent
 * @see SecurityEventType
 * @see SecurityAuditLogger
 */
public class SessionSecurityValidator extends ValidationHandler {

    private static final Logger logger = LoggerFactory.getLogger(SessionSecurityValidator.class);

    private final SecurityAuditLogger auditLogger;

    /**
     * Constructor for SessionSecurityValidator with dependency injection.
     *
     * @param next The next validator in the chain (can be null for end of chain)
     * @param auditLogger Security audit logger for compliance logging
     */
    public SessionSecurityValidator(ValidationHandler next,
                                   SecurityAuditLogger auditLogger) {
        this.next = next;
        this.auditLogger = auditLogger;
    }

    /**
     * Validates session security for the given command.
     *
     * @param command The command object to validate
     * @return Result<Void> indicating success or failure with appropriate error codes
     */
    @Override
    public Result<Void> validate(Object command) {
        // Skip validation for non-UpsertQuestionCommand types
        if (!(command instanceof UpsertQuestionCommand)) {
            return checkNext(command);
        }

        // For basic validation without session context, skip session checks
        logger.debug("Session security validation skipped - no session context provided");
        return checkNext(command);
    }

    /**
     * Validates session security with full session context.
     * This is the main validation method for US-024.
     *
     * @param command The command object to validate
     * @param sessionContext The session context with creation metadata
     * @param currentClientIp The current request's client IP address
     * @param currentUserAgent The current request's User-Agent string
     * @return Result<Void> indicating success or failure
     */
    public Result<Void> validate(UpsertQuestionCommand command,
                                 SessionContext sessionContext,
                                 String currentClientIp,
                                 String currentUserAgent) {

        // If no session context provided, skip session validation
        if (sessionContext == null) {
            logger.debug("Session security validation skipped - no session context");
            return checkNext(command);
        }

        logger.debug("Validating session security for user {} session {}",
            command.getUserId(), sessionContext.getSessionId());

        // 1. Validate IP address consistency (US-024 AC-024.1)
        if (!validateIPConsistency(sessionContext, currentClientIp)) {
            logger.warn("Session hijacking detected: IP mismatch for user {} session {}. " +
                    "Session IP: {}, Request IP: {}",
                command.getUserId(), sessionContext.getSessionId(),
                sessionContext.getClientIp(), currentClientIp);

            // Log security event
            logSessionHijackingAttempt(sessionContext, currentClientIp, currentUserAgent,
                "IP_MISMATCH",
                java.util.Map.of(
                    "sessionIp", sessionContext.getClientIp() != null ? sessionContext.getClientIp() : "null",
                    "requestIp", currentClientIp != null ? currentClientIp : "null",
                    "questionBankId", command.getQuestionBankId()
                ));

            return Result.failure(
                ValidationErrorCode.SESSION_SECURITY_VIOLATION.name(),
                "Session security violation detected"
            );
        }

        // 2. Validate User-Agent consistency (US-024 AC-024.2)
        if (!validateUserAgentConsistency(sessionContext, currentUserAgent)) {
            logger.warn("Session hijacking detected: User-Agent mismatch for user {} session {}",
                command.getUserId(), sessionContext.getSessionId());

            // Log security event
            logSessionHijackingAttempt(sessionContext, currentClientIp, currentUserAgent,
                "USER_AGENT_MISMATCH",
                java.util.Map.of(
                    "sessionUserAgent", sessionContext.getUserAgent() != null ? sessionContext.getUserAgent() : "null",
                    "requestUserAgent", currentUserAgent != null ? currentUserAgent : "null",
                    "questionBankId", command.getQuestionBankId()
                ));

            return Result.failure(
                ValidationErrorCode.SESSION_SECURITY_VIOLATION.name(),
                "Session security violation detected"
            );
        }

        logger.debug("Session security validation passed for user {} session {}",
            command.getUserId(), sessionContext.getSessionId());

        return checkNext(command);
    }

    /**
     * Validates IP address consistency between session creation and current request.
     * US-024 AC-024.1: IP address consistency must be validated against session creation IP.
     *
     * @param sessionContext The session context with original IP
     * @param currentClientIp The current request's IP address
     * @return true if IP addresses match, false otherwise
     */
    private boolean validateIPConsistency(SessionContext sessionContext, String currentClientIp) {
        String sessionIp = sessionContext.getClientIp();

        // If session doesn't have IP stored, allow (backward compatibility)
        if (sessionIp == null || sessionIp.trim().isEmpty()) {
            return true;
        }

        // If current request doesn't have IP, reject
        if (currentClientIp == null || currentClientIp.trim().isEmpty()) {
            return false;
        }

        // Check for exact match
        return sessionIp.equals(currentClientIp);
    }

    /**
     * Validates User-Agent string consistency between session creation and current request.
     * US-024 AC-024.2: User-Agent string must be validated for consistency throughout session.
     *
     * @param sessionContext The session context with original User-Agent
     * @param currentUserAgent The current request's User-Agent string
     * @return true if User-Agent strings match, false otherwise
     */
    private boolean validateUserAgentConsistency(SessionContext sessionContext, String currentUserAgent) {
        String sessionUserAgent = sessionContext.getUserAgent();

        // If session doesn't have User-Agent stored, allow (backward compatibility)
        if (sessionUserAgent == null || sessionUserAgent.trim().isEmpty()) {
            return true;
        }

        // If current request doesn't have User-Agent, reject
        if (currentUserAgent == null || currentUserAgent.trim().isEmpty()) {
            return false;
        }

        // Check for exact match
        return sessionUserAgent.equals(currentUserAgent);
    }

    /**
     * Logs session hijacking attempt using SecurityAuditLogger.
     * US-024 AC-024.11: Session hijacking attempts must be logged via US-021 SecurityAuditLogger.
     *
     * @param sessionContext The session context
     * @param currentClientIp The current request IP
     * @param currentUserAgent The current request User-Agent
     * @param violationType The type of violation detected
     * @param additionalDetails Additional context for the security event
     */
    private void logSessionHijackingAttempt(SessionContext sessionContext,
                                           String currentClientIp,
                                           String currentUserAgent,
                                           String violationType,
                                           java.util.Map<String, Object> additionalDetails) {
        if (auditLogger != null) {
            // Merge violation details
            var details = new java.util.HashMap<String, Object>();
            details.put("violationType", violationType);
            details.put("sessionCreatedAt", sessionContext.getSessionCreatedAt());
            details.put("sessionLastAccessedAt", sessionContext.getLastAccessedAt());
            details.putAll(additionalDetails);

            SecurityEvent event = SecurityEvent.builder()
                .type(SecurityEventType.SESSION_HIJACKING_ATTEMPT)
                .userId(sessionContext.getUserId())
                .sessionId(sessionContext.getSessionId())
                .severity(SeverityLevel.CRITICAL)
                .clientIp(currentClientIp)
                .userAgent(currentUserAgent)
                .details(details)
                .build();

            // Log asynchronously to avoid impacting request performance (US-024: <30ms)
            auditLogger.logSecurityEventAsync(event);
        }
    }
}
