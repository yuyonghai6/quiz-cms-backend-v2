package com.quizfun.orchestrationlayer.application.handlers;

import com.quizfun.globalshared.mediator.ICommandHandler;
import com.quizfun.globalshared.mediator.Result;
import com.quizfun.questionbank.application.commands.UpsertQuestionCommand;
import com.quizfun.questionbank.application.dto.QuestionResponseDto;
import com.quizfun.questionbank.application.services.QuestionApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * CQRS command handler for UpsertQuestionCommand operations.
 *
 * This handler follows the mediator pattern and provides a clean separation between
 * the HTTP layer and the business logic layer. It delegates to the QuestionApplicationService
 * for the actual business processing while maintaining consistent error handling and logging.
 *
 * The handler is responsible for:
 * - Command validation at the CQRS level
 * - Delegation to the appropriate application service
 * - Result type conversion between domain and API layers
 * - Comprehensive logging and monitoring
 */
@Service
public class UpsertQuestionCommandHandler implements ICommandHandler<UpsertQuestionCommand, QuestionResponseDto> {

    private static final Logger logger = LoggerFactory.getLogger(UpsertQuestionCommandHandler.class);

    private final QuestionApplicationService questionApplicationService;

    /**
     * Constructor injection for QuestionApplicationService dependency.
     *
     * @param questionApplicationService The application service that contains the business logic
     */
    public UpsertQuestionCommandHandler(QuestionApplicationService questionApplicationService) {
        this.questionApplicationService = questionApplicationService;
        logger.info("UpsertQuestionCommandHandler initialized with QuestionApplicationService");
    }

    /**
     * Handles the UpsertQuestionCommand by delegating to the application service.
     *
     * @param command The command containing all data required for question upsert
     * @return Result containing QuestionResponseDto with operation details
     */
    @Override
    public Result<QuestionResponseDto> handle(UpsertQuestionCommand command) {
        logger.info("Handling UpsertQuestionCommand for source question ID: {} by user: {} in question bank: {}",
                   command.getSourceQuestionId(), command.getUserId(), command.getQuestionBankId());

        try {
            // Delegate to application service which contains all business logic
            var applicationServiceResult = questionApplicationService.upsertQuestion(command);

            // Convert from internal Result type to mediator Result type
            if (applicationServiceResult.isSuccess()) {
                logger.info("Successfully handled UpsertQuestionCommand for source question ID: {} with operation: {}",
                           command.getSourceQuestionId(), applicationServiceResult.getValue().getOperation());

                recordCommandSuccess(command.getQuestionType().toString());
                return Result.success(applicationServiceResult.getValue());
            } else {
                logger.warn("Failed to handle UpsertQuestionCommand for source question ID: {} with error: {}",
                           command.getSourceQuestionId(), applicationServiceResult.getError());

                recordCommandFailure(command.getQuestionType().toString(), applicationServiceResult.getError());
                return Result.failure(applicationServiceResult.getError());
            }

        } catch (Exception ex) {
            logger.error("Unexpected error while handling UpsertQuestionCommand for source question ID: {}",
                        command.getSourceQuestionId(), ex);

            recordCommandError(command.getQuestionType().toString(), ex.getClass().getSimpleName());

            return Result.failure("COMMAND_HANDLER_ERROR: " + ex.getMessage());
        }
    }

    /**
     * Records successful command execution metrics.
     *
     * @param questionType The type of question that was processed
     */
    private void recordCommandSuccess(String questionType) {
        // Metrics recording for monitoring - implementation depends on monitoring framework
        logger.debug("Recording command success for question type: {}", questionType);
        // TODO: Implement metrics recording (e.g., Micrometer, Prometheus)
    }

    /**
     * Records failed command execution metrics.
     *
     * @param questionType The type of question that failed processing
     * @param error The error message from the failure
     */
    private void recordCommandFailure(String questionType, String error) {
        // Metrics recording for monitoring
        logger.debug("Recording command failure for question type: {} with error: {}", questionType, error);
        // TODO: Implement metrics recording (e.g., Micrometer, Prometheus)
    }

    /**
     * Records command processing errors (unexpected exceptions).
     *
     * @param questionType The type of question that caused the error
     * @param exceptionType The type of exception that occurred
     */
    private void recordCommandError(String questionType, String exceptionType) {
        // Metrics recording for monitoring
        logger.debug("Recording command error for question type: {} with exception: {}", questionType, exceptionType);
        // TODO: Implement metrics recording (e.g., Micrometer, Prometheus)
    }
}