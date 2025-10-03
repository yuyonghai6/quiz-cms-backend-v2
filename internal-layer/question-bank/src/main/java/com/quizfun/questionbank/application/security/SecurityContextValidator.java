package com.quizfun.questionbank.application.security;

import com.quizfun.questionbank.application.commands.UpsertQuestionCommand;
import com.quizfun.questionbank.domain.validation.ValidationErrorCode;
import com.quizfun.questionbank.infrastructure.monitoring.ValidationChainMetrics;
import com.quizfun.questionbank.infrastructure.utils.RetryHelper;
import com.quizfun.shared.common.Result;
import com.quizfun.shared.validation.ValidationHandler;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
/**
 * SecurityContextValidator extends the existing ValidationHandler to provide
 * comprehensive security validation for JWT token consistency with path parameters.
 *
 * Enhanced with US-022 Path Parameter Manipulation Detection:
 * - Sophisticated attack pattern detection
 * - Structured security event logging (SecurityEvent model)
 * - Performance-optimized validation (<15ms requirement for US-022)
 * - Integration with US-021 MongoDB audit trail
 *
 * This validator implements defense-in-depth security by:
 * 1. Validating JWT token user ID matches path parameter user ID
 * 2. Detecting path parameter manipulation attacks (US-022)
 * 3. Comprehensive security event logging with full context
 * 4. Performance monitoring and metrics collection
 * 5. Integration with existing US-003 validation chain infrastructure
 *
 * @see SecurityEvent
 * @see SecurityEventType
 * @see SecurityAuditLogger
 */
public class SecurityContextValidator extends ValidationHandler {

    private final SecurityAuditLogger auditLogger;
    private final RetryHelper retryHelper;
    private final ValidationChainMetrics metrics;

    /**
     * Constructor for SecurityContextValidator with dependency injection.
     *
     * @param next The next validator in the chain (can be null for end of chain)
     * @param auditLogger Security audit logger for compliance logging
     * @param retryHelper Retry helper from US-003 for resilient operations
     * @param metrics Validation chain metrics from US-003 for monitoring
     */
    public SecurityContextValidator(ValidationHandler next,
                                    SecurityAuditLogger auditLogger,
                                    RetryHelper retryHelper,
                                    ValidationChainMetrics metrics) {
        this.next = next;
        this.auditLogger = auditLogger;
        this.retryHelper = retryHelper;
        this.metrics = metrics;
    }

    /**
     * Validates the security context for the given command.
     * Implements the core security validation logic as per US-020 requirements.
     *
     * @param command The command object to validate (must be UpsertQuestionCommand)
     * @return Result<Void> indicating success or failure with appropriate error codes
     */
    @Override
    public Result<Void> validate(Object command) {
        if (!(command instanceof UpsertQuestionCommand upsertCommand)) {
            return Result.failure(
                ValidationErrorCode.INVALID_QUESTION_TYPE.name(),
                "Invalid command type for security validation"
            );
        }

        Long timer = startSecurityValidationTimer();

        try {
            // 1. Extract and validate authentication context
            var authContext = SecurityContextHolder.getContext().getAuthentication();
            if (authContext == null) {
                logSecurityEvent(SecurityEventType.MISSING_AUTHENTICATION, null,
                    SeverityLevel.HIGH, null,
                    java.util.Map.of("error", "Request received without authentication context"));
                return createSecurityFailureResult(ValidationErrorCode.INVALID_AUTHENTICATION_TOKEN,
                    "Authentication required");
            }

            // 2. Validate JWT authentication token type
            if (!(authContext instanceof JwtAuthenticationToken jwtToken)) {
                logSecurityEvent(SecurityEventType.INVALID_TOKEN_TYPE, null,
                    SeverityLevel.HIGH, null,
                    java.util.Map.of(
                        "error", "Non-JWT authentication token detected",
                        "tokenType", authContext.getClass().getSimpleName()
                    ));
                return createSecurityFailureResult(ValidationErrorCode.INVALID_AUTHENTICATION_TOKEN,
                    "Invalid authentication token type");
            }

            // 3. Extract user ID from JWT token
            Long tokenUserId = extractUserIdFromToken(jwtToken);
            if (tokenUserId == null) {
                logSecurityEvent(SecurityEventType.INVALID_TOKEN_SUBJECT, null,
                    SeverityLevel.HIGH, null,
                    java.util.Map.of("error", "JWT token missing or invalid subject claim"));
                return createSecurityFailureResult(ValidationErrorCode.INVALID_AUTHENTICATION_TOKEN,
                    "Invalid token subject");
            }

            // 4. US-022: Detect path parameter manipulation attacks
            Long pathUserId = upsertCommand.getUserId();
            if (!tokenUserId.equals(pathUserId)) {
                // Critical security violation: Path parameter manipulation detected
                logSecurityEvent(SecurityEventType.PATH_PARAMETER_MANIPULATION, tokenUserId,
                    SeverityLevel.CRITICAL, upsertCommand.getQuestionBankId(),
                    java.util.Map.of(
                        "tokenUserId", tokenUserId,
                        "pathUserId", pathUserId,
                        "questionBankId", upsertCommand.getQuestionBankId(),
                        "sourceQuestionId", upsertCommand.getSourceQuestionId(),
                        "attackPattern", "USER_ID_MISMATCH",
                        "detectionMethod", "JWT_PATH_COMPARISON"
                    ));
                return createSecurityFailureResult(ValidationErrorCode.UNAUTHORIZED_ACCESS,
                    "Access denied");
            }

            // 5. Log successful security validation for baseline monitoring
            logSecurityEvent(SecurityEventType.SECURITY_VALIDATION_SUCCESS, tokenUserId,
                SeverityLevel.INFO, upsertCommand.getQuestionBankId(),
                java.util.Map.of(
                    "operation", "SECURITY_VALIDATION",
                    "questionBankId", upsertCommand.getQuestionBankId()
                ));

            // 6. Continue to next validator in chain
            return checkNext(command);

        } catch (Exception e) {
            // Handle unexpected errors during security validation
            logSecurityEvent(SecurityEventType.SECURITY_VALIDATION_ERROR, null,
                SeverityLevel.HIGH, null,
                java.util.Map.of(
                    "error", "Unexpected error during security validation",
                    "exceptionType", e.getClass().getSimpleName(),
                    "message", e.getMessage() != null ? e.getMessage() : "Unknown error"
                ));
            return createSecurityFailureResult(ValidationErrorCode.INVALID_AUTHENTICATION_TOKEN,
                "Security validation failed");
        } finally {
            endSecurityValidationTimer(timer);
        }
    }

    /**
     * Extracts user ID from JWT token subject claim.
     * Handles both string and numeric user IDs safely.
     */
    private Long extractUserIdFromToken(JwtAuthenticationToken jwtToken) {
        try {
            String subject = jwtToken.getName(); // Gets the 'sub' claim
            if (subject == null || subject.trim().isEmpty()) {
                return null;
            }
            return Long.valueOf(subject.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Creates a standardized security failure result with proper error codes.
     */
    private Result<Void> createSecurityFailureResult(ValidationErrorCode errorCode, String message) {
        return Result.failure(errorCode.name(), message);
    }

    /**
     * Logs security events using the new SecurityEvent model from US-021.
     * Enhanced for US-022 with comprehensive context information.
     *
     * @param eventType The type of security event
     * @param userId The user ID involved (can be null for unauthenticated requests)
     * @param severity The severity level of the event
     * @param questionBankId The question bank ID (can be null)
     * @param details Additional context as key-value pairs
     */
    private void logSecurityEvent(SecurityEventType eventType, Long userId,
                                  SeverityLevel severity, Long questionBankId,
                                  java.util.Map<String, Object> details) {
        if (auditLogger != null) {
            SecurityEvent event = SecurityEvent.builder()
                .type(eventType)
                .userId(userId)
                .severity(severity)
                .details(details)
                .build();

            // Log asynchronously to avoid impacting request performance (US-022: <15ms)
            auditLogger.logSecurityEventAsync(event);
        }
    }

    /**
     * Legacy method for backward compatibility.
     * @deprecated Use {@link #logSecurityEvent} instead
     */
    @Deprecated
    private void logSecurityViolation(String eventType, Long tokenUserId, String details) {
        if (auditLogger != null) {
            auditLogger.logSecurityViolation(eventType, tokenUserId, details);
        }
    }

    /**
     * Starts security validation timing for performance monitoring.
     */
    private Long startSecurityValidationTimer() {
        if (metrics != null) {
            return metrics.startTimer();
        }
        return System.nanoTime();
    }

    /**
     * Ends security validation timing and records metrics.
     */
    private void endSecurityValidationTimer(Long timer) {
        if (metrics != null && timer != null) {
            metrics.stopTimer(timer, "security_context_validation");
        }
    }
}