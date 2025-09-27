package com.quizfun.questionbank.domain.services;

import com.quizfun.questionbank.application.commands.UpsertQuestionCommand;
import com.quizfun.questionbank.domain.aggregates.QuestionAggregate;
import com.quizfun.questionbank.domain.entities.QuestionType;
import com.quizfun.shared.common.Result;

/**
 * Strategy interface for processing different types of questions.
 * Each question type (MCQ, Essay, True/False) has its own implementation
 * with type-specific validation and processing logic.
 */
public interface QuestionTypeStrategy {

    /**
     * Processes question data and creates a QuestionAggregate with type-specific validation.
     * This method handles the business logic and validation rules specific to each question type.
     *
     * @param command The command containing question data to process
     * @return Result containing the created QuestionAggregate or error details
     */
    Result<QuestionAggregate> processQuestionData(UpsertQuestionCommand command);

    /**
     * Determines if this strategy supports the given question type.
     *
     * @param type The question type to check
     * @return true if this strategy can handle the given type, false otherwise
     */
    boolean supports(QuestionType type);

    /**
     * Returns a human-readable name for this strategy for logging and debugging.
     *
     * @return The strategy name
     */
    String getStrategyName();
}