package com.quizfun.questionbank.application.validation;

import com.quizfun.questionbank.application.commands.UpsertQuestionCommand;
import com.quizfun.questionbank.application.ports.out.QuestionBanksPerUserRepository;
import com.quizfun.questionbank.domain.validation.ValidationErrorCode;
import com.quizfun.questionbank.infrastructure.utils.RetryHelper;
import com.quizfun.shared.common.Result;
import com.quizfun.shared.validation.ValidationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Validates that the user owns the question bank they are trying to modify.
 * This is the first validator in the chain as it handles security concerns.
 */
@Component
public class QuestionBankOwnershipValidator extends ValidationHandler {

    private static final Logger logger = LoggerFactory.getLogger(QuestionBankOwnershipValidator.class);
    private final QuestionBanksPerUserRepository questionBanksPerUserRepository;
    private final RetryHelper retryHelper;

    public QuestionBankOwnershipValidator(QuestionBanksPerUserRepository questionBanksPerUserRepository,
                                         RetryHelper retryHelper) {
        this.questionBanksPerUserRepository = questionBanksPerUserRepository;
        this.retryHelper = retryHelper;
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