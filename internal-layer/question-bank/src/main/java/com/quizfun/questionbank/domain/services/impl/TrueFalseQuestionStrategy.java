package com.quizfun.questionbank.domain.services.impl;

import com.quizfun.questionbank.application.commands.UpsertQuestionCommand;
import com.quizfun.questionbank.domain.aggregates.QuestionAggregate;
import com.quizfun.questionbank.domain.entities.QuestionType;
import com.quizfun.questionbank.domain.entities.TrueFalseData;
import com.quizfun.questionbank.domain.services.QuestionTypeStrategy;
import com.quizfun.shared.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Strategy implementation for processing True/False Questions.
 * Handles true/false-specific validation rules and business logic.
 */
@Component
public class TrueFalseQuestionStrategy implements QuestionTypeStrategy {

    private static final Logger logger = LoggerFactory.getLogger(TrueFalseQuestionStrategy.class);
    private static final int MAX_EXPLANATION_LENGTH = 2000;

    @Override
    public Result<QuestionAggregate> processQuestionData(UpsertQuestionCommand command) {
        logger.debug("Processing True/False question data for source ID: {}", command.getSourceQuestionId());

        // Validate True/False-specific data is present
        if (command.getTrueFalseData() == null) {
            logger.warn("True/False data is missing for question with source ID: {}", command.getSourceQuestionId());
            return Result.failure(
                "TRUE_FALSE_DATA_REQUIRED",
                "True/False data is required for true/false questions"
            );
        }

        // Validate True/False data integrity
        var validationResult = validateTrueFalseData(command.getTrueFalseData());
        if (validationResult.isFailure()) {
            logger.warn("True/False data validation failed for source ID {}: {}",
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

            // Set True/False-specific data
            questionAggregate.setTrueFalseData(command.getTrueFalseData());

            // Apply optional metadata after type-specific data is set
            questionAggregate.updateStatusAndMetadata(
                command.getStatus(),
                command.getDisplayOrder(),
                command.getSolutionExplanation()
            );

            logger.debug("Successfully processed True/False question data for source ID: {}",
                        command.getSourceQuestionId());

            return Result.success(questionAggregate);

        } catch (Exception ex) {
            logger.error("Failed to process True/False question data for source ID: {}",
                        command.getSourceQuestionId(), ex);
            return Result.failure(
                "TRUE_FALSE_PROCESSING_ERROR",
                "Failed to process True/False question: " + ex.getMessage()
            );
        }
    }

    /**
     * Validates True/False-specific data according to business rules.
     *
     * @param trueFalseData The true/false data to validate
     * @return Result indicating success or specific validation failure
     */
    private Result<QuestionAggregate> validateTrueFalseData(TrueFalseData trueFalseData) {
        // Note: correctAnswer is a primitive boolean, so it's always specified
        // No null validation needed for correctAnswer

        // Validate explanation if provided
        if (trueFalseData.getExplanation() != null) {
            if (trueFalseData.getExplanation().trim().isEmpty()) {
                return Result.failure(
                    "TRUE_FALSE_EMPTY_EXPLANATION",
                    "True/False explanation cannot be empty if provided"
                );
            }

            if (trueFalseData.getExplanation().length() > MAX_EXPLANATION_LENGTH) {
                return Result.failure(
                    "TRUE_FALSE_EXPLANATION_TOO_LONG",
                    String.format("True/False explanation cannot exceed %d characters", MAX_EXPLANATION_LENGTH)
                );
            }
        }

        // Validate time limit if provided
        if (trueFalseData.getTimeLimitSeconds() != null && trueFalseData.getTimeLimitSeconds() <= 0) {
            return Result.failure(
                "TRUE_FALSE_INVALID_TIME_LIMIT",
                "Time limit must be positive if specified"
            );
        }

        // Validate time limit is reasonable (not more than 1 hour)
        if (trueFalseData.getTimeLimitSeconds() != null && trueFalseData.getTimeLimitSeconds() > 3600) {
            return Result.failure(
                "TRUE_FALSE_TIME_LIMIT_TOO_LONG",
                "Time limit cannot exceed 3600 seconds (1 hour)"
            );
        }

        return Result.success(null);
    }


    @Override
    public boolean supports(QuestionType type) {
        return QuestionType.TRUE_FALSE == type;
    }

    @Override
    public String getStrategyName() {
        return "True/False Strategy";
    }
}