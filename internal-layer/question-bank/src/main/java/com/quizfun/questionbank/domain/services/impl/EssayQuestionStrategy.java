package com.quizfun.questionbank.domain.services.impl;

import com.quizfun.questionbank.application.commands.UpsertQuestionCommand;
import com.quizfun.questionbank.domain.aggregates.QuestionAggregate;
import com.quizfun.questionbank.domain.entities.EssayData;
import com.quizfun.questionbank.domain.entities.EssayRubric;
import com.quizfun.questionbank.domain.entities.QuestionType;
import com.quizfun.questionbank.domain.services.QuestionTypeStrategy;
import com.quizfun.shared.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Strategy implementation for processing Essay Questions.
 * Handles essay-specific validation rules and business logic including
 * word count constraints and rubric validation.
 */
@Component
public class EssayQuestionStrategy implements QuestionTypeStrategy {

    private static final Logger logger = LoggerFactory.getLogger(EssayQuestionStrategy.class);
    private static final int MAX_PROMPT_LENGTH = 5000;
    private static final int MAX_RUBRIC_TOTAL_POINTS = 1000;
    private static final int MAX_WORD_COUNT_LIMIT = 10000;

    @Override
    public Result<QuestionAggregate> processQuestionData(UpsertQuestionCommand command) {
        logger.debug("Processing Essay question data for source ID: {}", command.getSourceQuestionId());

        // Validate Essay-specific data is present
        if (command.getEssayData() == null) {
            logger.warn("Essay data is missing for question with source ID: {}", command.getSourceQuestionId());
            return Result.failure(
                "ESSAY_DATA_REQUIRED",
                "Essay data is required for essay questions"
            );
        }

        // Validate Essay data integrity
        var validationResult = validateEssayData(command.getEssayData());
        if (validationResult.isFailure()) {
            logger.warn("Essay data validation failed for source ID {}: {}",
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

            // Set Essay-specific data
            questionAggregate.setEssayData(command.getEssayData());

            // Apply optional metadata after type-specific data is set
            questionAggregate.updateStatusAndMetadata(
                command.getStatus(),
                command.getDisplayOrder(),
                command.getSolutionExplanation()
            );

            logger.debug("Successfully processed Essay question data for source ID: {}",
                        command.getSourceQuestionId());

            return Result.success(questionAggregate);

        } catch (Exception ex) {
            logger.error("Failed to process Essay question data for source ID: {}",
                        command.getSourceQuestionId(), ex);
            return Result.failure(
                "ESSAY_PROCESSING_ERROR",
                "Failed to process Essay question: " + ex.getMessage()
            );
        }
    }

    /**
     * Validates Essay-specific data according to business rules.
     *
     * @param essayData The essay data to validate
     * @return Result indicating success or specific validation failure
     */
    private Result<QuestionAggregate> validateEssayData(EssayData essayData) {
        // Note: Essay prompt is typically stored in the question content, not in EssayData
        // EssayData focuses on constraints like word counts and rubrics

        // Validate word count requirements
        if (essayData.getMinWordCount() != null && essayData.getMinWordCount() < 0) {
            return Result.failure(
                "ESSAY_NEGATIVE_MIN_WORDS",
                "Minimum word count cannot be negative"
            );
        }

        if (essayData.getMaxWordCount() != null && essayData.getMaxWordCount() <= 0) {
            return Result.failure(
                "ESSAY_INVALID_MAX_WORDS",
                "Maximum word count must be positive"
            );
        }

        if (essayData.getMaxWordCount() != null && essayData.getMaxWordCount() > MAX_WORD_COUNT_LIMIT) {
            return Result.failure(
                "ESSAY_MAX_WORDS_TOO_HIGH",
                String.format("Maximum word count cannot exceed %d", MAX_WORD_COUNT_LIMIT)
            );
        }

        if (essayData.getMinWordCount() != null && essayData.getMaxWordCount() != null) {
            if (essayData.getMinWordCount() > essayData.getMaxWordCount()) {
                return Result.failure(
                    "ESSAY_INVALID_WORD_COUNT_RANGE",
                    "Minimum word count cannot be greater than maximum word count"
                );
            }
        }

        // Validate rubric if provided
        if (essayData.getRubric() != null) {
            var rubricValidation = validateRubric(essayData.getRubric());
            if (rubricValidation.isFailure()) {
                return rubricValidation;
            }
        }

        return Result.success(null);
    }

    /**
     * Validates essay rubric according to business rules.
     *
     * @param rubric The rubric to validate
     * @return Result indicating success or specific validation failure
     */
    private Result<QuestionAggregate> validateRubric(EssayRubric rubric) {
        // Check if rubric has criteria
        if (rubric.getCriteria() == null || rubric.getCriteria().isEmpty()) {
            return Result.failure(
                "ESSAY_RUBRIC_NO_CRITERIA",
                "Essay rubric must have at least one criteria"
            );
        }

        // Validate each criteria (criteria are stored as strings in the actual entity)
        for (int i = 0; i < rubric.getCriteria().size(); i++) {
            var criteria = rubric.getCriteria().get(i);

            // Validate criteria is not empty
            if (criteria == null || criteria.trim().isEmpty()) {
                return Result.failure(
                    "ESSAY_EMPTY_RUBRIC_CRITERIA",
                    String.format("Rubric criteria %d cannot be empty", i + 1)
                );
            }

            // Validate criteria length
            if (criteria.length() > 1000) {
                return Result.failure(
                    "ESSAY_RUBRIC_CRITERIA_TOO_LONG",
                    String.format("Rubric criteria %d cannot exceed 1000 characters", i + 1)
                );
            }
        }

        // Validate rubric max points if specified
        if (rubric.getMaxPoints() != null) {
            if (rubric.getMaxPoints() <= 0) {
                return Result.failure(
                    "ESSAY_INVALID_RUBRIC_POINTS",
                    "Rubric max points must be positive"
                );
            }

            if (rubric.getMaxPoints() > MAX_RUBRIC_TOTAL_POINTS) {
                return Result.failure(
                    "ESSAY_RUBRIC_TOO_MANY_POINTS",
                    String.format("Rubric points cannot exceed %d", MAX_RUBRIC_TOTAL_POINTS)
                );
            }
        }

        return Result.success(null);
    }


    @Override
    public boolean supports(QuestionType type) {
        return QuestionType.ESSAY == type;
    }

    @Override
    public String getStrategyName() {
        return "Essay Strategy";
    }
}