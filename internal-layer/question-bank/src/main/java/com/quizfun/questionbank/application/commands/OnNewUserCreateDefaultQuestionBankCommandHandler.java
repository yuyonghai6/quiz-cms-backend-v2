package com.quizfun.questionbank.application.commands;

import com.quizfun.globalshared.mediator.ICommandHandler;
import com.quizfun.globalshared.mediator.Result;
import com.quizfun.questionbank.application.dto.DefaultQuestionBankResponseDto;
import com.quizfun.questionbank.application.services.DefaultQuestionBankApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Handler for OnNewUserCreateDefaultQuestionBankCommand.
 *
 * This handler implements the Command pattern in CQRS architecture,
 * routing the command to the appropriate application service.
 *
 * Responsibilities:
 * - Extract userId from command
 * - Delegate to DefaultQuestionBankApplicationService
 * - Return result without transformation
 *
 * Note: Email and metadata fields in the command are currently ignored
 * since DEFAULT question bank creation uses system-defined values.
 * These fields may be used in future enhancements for customization.
 */
@Service
public class OnNewUserCreateDefaultQuestionBankCommandHandler
        implements ICommandHandler<OnNewUserCreateDefaultQuestionBankCommand, DefaultQuestionBankResponseDto> {

    private static final Logger logger = LoggerFactory.getLogger(OnNewUserCreateDefaultQuestionBankCommandHandler.class);

    private final DefaultQuestionBankApplicationService applicationService;

    /**
     * Constructor injection for dependencies.
     *
     * @param applicationService Service handling default question bank creation logic
     */
    public OnNewUserCreateDefaultQuestionBankCommandHandler(
            DefaultQuestionBankApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    /**
     * Handles the command by delegating to application service.
     *
     * @param command The command containing userId and optional metadata
     * @return Result containing the created question bank details or error
     */
    @Override
    public Result<DefaultQuestionBankResponseDto> handle(OnNewUserCreateDefaultQuestionBankCommand command) {
        logger.info("Handling OnNewUserCreateDefaultQuestionBankCommand for userId: {}",
            command.getUserId());

        // Delegate to application service
        // Currently only userId is used; email and metadata are for future customization
        Result<DefaultQuestionBankResponseDto> result =
            applicationService.createDefaultQuestionBank(command.getUserId());

        if (result.success()) {
            logger.info("Successfully handled command for userId: {}", command.getUserId());
        } else {
            logger.warn("Failed to handle command for userId: {}. Reason: {}",
                command.getUserId(), result.message());
        }

        return result;
    }
}
