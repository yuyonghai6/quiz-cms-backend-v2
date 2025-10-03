package com.quizfun.questionbank.application.validation;

import com.quizfun.questionbank.application.commands.UpsertQuestionCommand;
import com.quizfun.questionbank.application.ports.out.QuestionBanksPerUserRepository;
import com.quizfun.questionbank.application.security.SecurityAuditLogger;
import com.quizfun.questionbank.application.security.SecurityEvent;
import com.quizfun.questionbank.application.security.SecurityEventType;
import com.quizfun.questionbank.application.security.SeverityLevel;
import com.quizfun.questionbank.domain.validation.ValidationErrorCode;
import com.quizfun.questionbank.infrastructure.utils.RetryHelper;
import com.quizfun.shared.common.Result;
import com.quizfun.shared.validation.ValidationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Validates that the user owns the question bank they are trying to modify.
 *
 * Enhanced with US-023 Token Privilege Escalation Prevention:
 * - Deep ownership validation with retry mechanisms
 * - Active status verification for question banks
 * - Privilege escalation detection and logging
 * - Integration with US-021 SecurityAuditLogger for comprehensive audit trails
 *
 * This validator is positioned early in the chain as it handles critical security concerns.
 *
 * @see SecurityAuditLogger
 * @see SecurityEvent
 */
@Component
public class QuestionBankOwnershipValidator extends ValidationHandler {

    private static final Logger logger = LoggerFactory.getLogger(QuestionBankOwnershipValidator.class);
    private final QuestionBanksPerUserRepository questionBanksPerUserRepository;
    private final RetryHelper retryHelper;
    private final SecurityAuditLogger securityAuditLogger;

    public QuestionBankOwnershipValidator(QuestionBanksPerUserRepository questionBanksPerUserRepository,
                                         RetryHelper retryHelper,
                                         SecurityAuditLogger securityAuditLogger) {
        this.questionBanksPerUserRepository = questionBanksPerUserRepository;
        this.retryHelper = retryHelper;
        this.securityAuditLogger = securityAuditLogger;
    }

    @Override
    public Result<Void> validate(Object command) {
        if (!(command instanceof UpsertQuestionCommand)) {
            return checkNext(command);
        }

        var upsertCommand = (UpsertQuestionCommand) command;

        logger.debug("Validating question bank ownership for user {} and question bank {}",
                    upsertCommand.getUserId(), upsertCommand.getQuestionBankId());

        try {
            // Validate ownership with retry mechanism for temporary failures
            Result<Boolean> ownershipResult = retryHelper.executeWithRetry(
                () -> questionBanksPerUserRepository.validateOwnership(
                    upsertCommand.getUserId(),
                    upsertCommand.getQuestionBankId()
                ),
                "validateOwnership"
            );

            if (ownershipResult.isFailure()) {
                logger.error("Repository error during ownership validation: {}", ownershipResult.getError());
                return Result.failure(
                    ownershipResult.getErrorCode() != null ? ownershipResult.getErrorCode() : "REPOSITORY_ERROR",
                    ownershipResult.getError()
                );
            }

            Boolean owns = ownershipResult.getValue();
            if (owns == null || !owns) {
                logger.warn("Unauthorized access attempt: User {} does not own question bank {}",
                           upsertCommand.getUserId(), upsertCommand.getQuestionBankId());

                // US-023: Log privilege escalation attempt (ownership violation)
                securityAuditLogger.logSecurityEventAsync(SecurityEvent.builder()
                    .type(SecurityEventType.TOKEN_PRIVILEGE_ESCALATION)
                    .userId(upsertCommand.getUserId())
                    .severity(SeverityLevel.CRITICAL)
                    .details(java.util.Map.of(
                        "questionBankId", upsertCommand.getQuestionBankId(),
                        "violationType", "OWNERSHIP_VIOLATION",
                        "attemptedOperation", "UPSERT_QUESTION",
                        "sourceQuestionId", upsertCommand.getSourceQuestionId()
                    ))
                    .build());

                return Result.failure(
                    ValidationErrorCode.UNAUTHORIZED_ACCESS.name(),
                    String.format("User %d doesn't own question bank %d",
                                upsertCommand.getUserId(), upsertCommand.getQuestionBankId())
                );
            }

            // Additional validation: Check if question bank is active with retry
            Result<Boolean> activeResult = retryHelper.executeWithRetry(
                () -> questionBanksPerUserRepository.isQuestionBankActive(
                    upsertCommand.getUserId(),
                    upsertCommand.getQuestionBankId()
                ),
                "isQuestionBankActive"
            );

            if (activeResult.isFailure()) {
                logger.error("Repository error during active status validation: {}", activeResult.getError());
                return Result.failure(
                    activeResult.getErrorCode() != null ? activeResult.getErrorCode() : "REPOSITORY_ERROR",
                    activeResult.getError()
                );
            }

            if (!activeResult.getValue()) {
                logger.warn("Attempt to modify inactive question bank: User {} question bank {}",
                           upsertCommand.getUserId(), upsertCommand.getQuestionBankId());

                // US-023: Log privilege escalation attempt (inactive resource access)
                securityAuditLogger.logSecurityEventAsync(SecurityEvent.builder()
                    .type(SecurityEventType.TOKEN_PRIVILEGE_ESCALATION)
                    .userId(upsertCommand.getUserId())
                    .severity(SeverityLevel.HIGH)
                    .details(java.util.Map.of(
                        "questionBankId", upsertCommand.getQuestionBankId(),
                        "violationType", "INACTIVE_RESOURCE_ACCESS",
                        "attemptedOperation", "UPSERT_QUESTION",
                        "sourceQuestionId", upsertCommand.getSourceQuestionId()
                    ))
                    .build());

                return Result.failure(
                    ValidationErrorCode.UNAUTHORIZED_ACCESS.name(),
                    String.format("Question bank %d is not active for user %d",
                                upsertCommand.getQuestionBankId(), upsertCommand.getUserId())
                );
            }

            logger.debug("Question bank ownership validation passed for user {} and question bank {}",
                        upsertCommand.getUserId(), upsertCommand.getQuestionBankId());

            return checkNext(command);

        } catch (Exception e) {
            logger.error("Unexpected error during ownership validation for user {} and question bank {}: {}",
                        upsertCommand.getUserId(), upsertCommand.getQuestionBankId(), e.getMessage(), e);

            return Result.failure(
                "OWNERSHIP_VALIDATION_ERROR",
                "An error occurred while validating question bank ownership"
            );
        }
    }
}