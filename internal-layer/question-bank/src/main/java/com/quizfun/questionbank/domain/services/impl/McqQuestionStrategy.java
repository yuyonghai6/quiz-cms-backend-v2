package com.quizfun.questionbank.domain.services.impl;

import com.quizfun.questionbank.application.commands.UpsertQuestionCommand;
import com.quizfun.questionbank.domain.aggregates.QuestionAggregate;
import com.quizfun.questionbank.domain.entities.McqData;
import com.quizfun.questionbank.domain.entities.QuestionType;
import com.quizfun.questionbank.domain.services.QuestionTypeStrategy;
import com.quizfun.shared.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Strategy implementation for processing Multiple Choice Questions (MCQ).
 * Handles MCQ-specific validation rules and business logic.
 */
@Component
public class McqQuestionStrategy implements QuestionTypeStrategy {

    private static final Logger logger = LoggerFactory.getLogger(McqQuestionStrategy.class);
    private static final int MIN_OPTIONS = 2;
    private static final int MAX_OPTIONS = 10;

    @Override
    public Result<QuestionAggregate> processQuestionData(UpsertQuestionCommand command) {
        logger.debug("Processing MCQ question data for source ID: {}", command.getSourceQuestionId());

        // Validate MCQ-specific data is present
        if (command.getMcqData() == null) {
            logger.warn("MCQ data is missing for question with source ID: {}", command.getSourceQuestionId());
            return Result.failure(
                "MCQ_DATA_REQUIRED",
                "MCQ data is required for MCQ questions"
            );
        }

        // Validate MCQ data integrity
        var validationResult = validateMcqData(command.getMcqData());
        if (validationResult.isFailure()) {
            logger.warn("MCQ data validation failed for source ID {}: {}",
                       command.getSourceQuestionId(), validationResult.getError());
            return validationResult;
        }

        try {
            // Create QuestionAggregate
            var questionAggregate = QuestionAggregate.createNew(
                command.getUserId(),
                command.getQuestionBankId(),
                command.getSourceQuestionId(),
                command.getQuestionType(),
                command.getTitle(),
                command.getContent(),
                command.getPoints()
            );

            // Set MCQ-specific data
            questionAggregate.setMcqData(command.getMcqData());

            // Apply optional metadata after type-specific data is set
            questionAggregate.updateStatusAndMetadata(
                command.getStatus(),
                command.getDisplayOrder(),
                command.getSolutionExplanation()
            );

            logger.debug("Successfully processed MCQ question data for source ID: {}",
                        command.getSourceQuestionId());

            return Result.success(questionAggregate);

        } catch (Exception ex) {
            logger.error("Failed to process MCQ question data for source ID: {}",
                        command.getSourceQuestionId(), ex);
            return Result.failure(
                "MCQ_PROCESSING_ERROR",
                "Failed to process MCQ question: " + ex.getMessage()
            );
        }
    }

    /**
     * Validates MCQ-specific data according to business rules.
     *
     * @param mcqData The MCQ data to validate
     * @return Result indicating success or specific validation failure
     */
    private Result<QuestionAggregate> validateMcqData(McqData mcqData) {
        // Validate options exist
        if (mcqData.getOptions() == null || mcqData.getOptions().isEmpty()) {
            return Result.failure(
                "MCQ_NO_OPTIONS",
                "MCQ questions must have at least one option"
            );
        }

        // Validate option count
        if (mcqData.getOptions().size() < MIN_OPTIONS) {
            return Result.failure(
                "MCQ_INSUFFICIENT_OPTIONS",
                String.format("MCQ questions must have at least %d options", MIN_OPTIONS)
            );
        }

        if (mcqData.getOptions().size() > MAX_OPTIONS) {
            return Result.failure(
                "MCQ_TOO_MANY_OPTIONS",
                String.format("MCQ questions cannot have more than %d options", MAX_OPTIONS)
            );
        }

        // Validate correct answers exist
        long correctAnswerCount = mcqData.getOptions().stream()
            .mapToLong(option -> option.isCorrect() ? 1 : 0)
            .sum();

        if (correctAnswerCount == 0) {
            return Result.failure(
                "MCQ_NO_CORRECT_ANSWER",
                "MCQ questions must have at least one correct answer"
            );
        }

        // Validate multiple correct answers if not allowed
        if (correctAnswerCount > 1 && !mcqData.isAllowMultipleCorrect()) {
            return Result.failure(
                "MCQ_MULTIPLE_CORRECT_NOT_ALLOWED",
                "Multiple correct answers not allowed for this MCQ configuration"
            );
        }

        // Validate option text
        for (int i = 0; i < mcqData.getOptions().size(); i++) {
            var option = mcqData.getOptions().get(i);
            if (option.getText() == null || option.getText().trim().isEmpty()) {
                return Result.failure(
                    "MCQ_EMPTY_OPTION",
                    String.format("Option %d cannot be empty", i + 1)
                );
            }

            // Validate option text length (reasonable limit)
            if (option.getText().length() > 500) {
                return Result.failure(
                    "MCQ_OPTION_TOO_LONG",
                    String.format("Option %d text cannot exceed 500 characters", i + 1)
                );
            }
        }

        // Validate time limit if provided
        if (mcqData.getTimeLimitSeconds() != null && mcqData.getTimeLimitSeconds() <= 0) {
            return Result.failure(
                "MCQ_INVALID_TIME_LIMIT",
                "Time limit must be positive if specified"
            );
        }

        // Validate time limit is reasonable (not more than 1 hour)
        if (mcqData.getTimeLimitSeconds() != null && mcqData.getTimeLimitSeconds() > 3600) {
            return Result.failure(
                "MCQ_TIME_LIMIT_TOO_LONG",
                "Time limit cannot exceed 3600 seconds (1 hour)"
            );
        }

        return Result.success(null);
    }


    @Override
    public boolean supports(QuestionType type) {
        return QuestionType.MCQ == type;
    }

    @Override
    public String getStrategyName() {
        return "MCQ Strategy";
    }
}