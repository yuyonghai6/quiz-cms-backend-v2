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
 * This validator implements defense-in-depth security by:
 * 1. Validating JWT token user ID matches path parameter user ID
 * 2. Comprehensive security event logging for audit compliance
 * 3. Performance-optimized validation (<20ms requirement)
 * 4. Integration with existing US-003 validation chain infrastructure
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
                logSecurityViolation("MISSING_AUTHENTICATION", null,
                    "Request received without authentication context");
                return createSecurityFailureResult(ValidationErrorCode.INVALID_AUTHENTICATION_TOKEN,
                    "Authentication required");
            }

            // 2. Validate JWT authentication token type
            if (!(authContext instanceof JwtAuthenticationToken jwtToken)) {
                logSecurityViolation("INVALID_TOKEN_TYPE", null,
                    "Non-JWT authentication token detected: " + authContext.getClass().getSimpleName());
                return createSecurityFailureResult(ValidationErrorCode.INVALID_AUTHENTICATION_TOKEN,
                    "Invalid authentication token type");
            }

            // 3. Extract user ID from JWT token
            Long tokenUserId = extractUserIdFromToken(jwtToken);
            if (tokenUserId == null) {
                logSecurityViolation("INVALID_TOKEN_SUBJECT", null,
                    "JWT token missing or invalid subject claim");
                return createSecurityFailureResult(ValidationErrorCode.INVALID_AUTHENTICATION_TOKEN,
                    "Invalid token subject");
            }

            // 4. Validate token user ID matches path parameter user ID
            Long pathUserId = upsertCommand.getUserId();
            if (!tokenUserId.equals(pathUserId)) {
                logSecurityViolation("PATH_PARAMETER_MANIPULATION", tokenUserId,
                    String.format("Token user %d attempted access to user %d resources",
                        tokenUserId, pathUserId));
                return createSecurityFailureResult(ValidationErrorCode.UNAUTHORIZED_ACCESS,
                    "Access denied");
            }

            // 5. Log successful security validation for monitoring
            auditLogger.logSuccessfulAccess(tokenUserId, upsertCommand.getQuestionBankId(), "SECURITY_VALIDATION");

            // 6. Continue to next validator in chain
            return checkNext(command);

        } catch (Exception e) {
            // Handle unexpected errors during security validation
            logSecurityViolation("SECURITY_VALIDATION_ERROR", null,
                "Unexpected error during security validation: " + e.getMessage());
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
     * Logs security violations with comprehensive audit information.
     */
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