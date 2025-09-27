package com.quizfun.questionbank.application.validation;

import com.quizfun.questionbank.application.commands.UpsertQuestionCommand;
import com.quizfun.questionbank.domain.entities.QuestionType;
import com.quizfun.questionbank.domain.entities.McqData;
import com.quizfun.questionbank.domain.entities.EssayData;
import com.quizfun.questionbank.domain.entities.TrueFalseData;
import com.quizfun.questionbank.domain.validation.ValidationErrorCode;
import com.quizfun.shared.common.Result;
import com.quizfun.shared.validation.ValidationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Validates question data integrity including required fields, type-specific data,
 * and business rules. This is the final validator in the chain.
 */
@Component
public class QuestionDataIntegrityValidator extends ValidationHandler {

    private static final Logger logger = LoggerFactory.getLogger(QuestionDataIntegrityValidator.class);
    private static final int MAX_TITLE_LENGTH = 500;
    private static final int MAX_CONTENT_LENGTH = 10000;
    private static final int MAX_POINTS = 1000;

    @Override
    public Result<Void> validate(Object command) {
        if (!(command instanceof UpsertQuestionCommand)) {
            return checkNext(command);
        }

        var upsertCommand = (UpsertQuestionCommand) command;

        logger.debug("Validating question data integrity for question {}",
                    upsertCommand.getSourceQuestionId());

        try {
            // Validate required fields
            var requiredFieldResult = validateRequiredFields(upsertCommand);
            if (requiredFieldResult.isFailure()) {
                return requiredFieldResult;
            }

            // Validate field constraints
            var constraintResult = validateFieldConstraints(upsertCommand);
            if (constraintResult.isFailure()) {
                return constraintResult;
            }

            // Validate type-specific data
            var typeDataResult = validateTypeSpecificData(upsertCommand);
            if (typeDataResult.isFailure()) {
                return typeDataResult;
            }

            // Validate business rules
            var businessRuleResult = validateBusinessRules(upsertCommand);
            if (businessRuleResult.isFailure()) {
                return businessRuleResult;
            }

            logger.debug("Question data integrity validation passed for question {}",
                        upsertCommand.getSourceQuestionId());

            return checkNext(command);

        } catch (Exception e) {
            logger.error("Unexpected error during data integrity validation for question {}: {}",
                        upsertCommand.getSourceQuestionId(), e.getMessage(), e);

            return Result.failure(
                "DATA_INTEGRITY_VALIDATION_ERROR",
                "An error occurred while validating question data integrity"
            );
        }
    }

    private Result<Void> validateRequiredFields(UpsertQuestionCommand command) {
        if (command.getSourceQuestionId() == null || command.getSourceQuestionId().trim().isEmpty()) {
            return Result.failure(ValidationErrorCode.MISSING_REQUIRED_FIELD.name(),
                                 "source_question_id is required");
        }

        if (command.getQuestionType() == null) {
            return Result.failure(ValidationErrorCode.MISSING_REQUIRED_FIELD.name(),
                                 "question_type is required");
        }

        if (command.getTitle() == null || command.getTitle().trim().isEmpty()) {
            return Result.failure(ValidationErrorCode.MISSING_REQUIRED_FIELD.name(),
                                 "title is required");
        }

        if (command.getContent() == null || command.getContent().trim().isEmpty()) {
            return Result.failure(ValidationErrorCode.MISSING_REQUIRED_FIELD.name(),
                                 "content is required");
        }

        return Result.success(null);
    }

    private Result<Void> validateFieldConstraints(UpsertQuestionCommand command) {
        // Validate title length
        if (command.getTitle().length() > MAX_TITLE_LENGTH) {
            return Result.failure(ValidationErrorCode.TYPE_DATA_MISMATCH.name(),
                                 String.format("Title cannot exceed %d characters", MAX_TITLE_LENGTH));
        }

        // Validate content length
        if (command.getContent().length() > MAX_CONTENT_LENGTH) {
            return Result.failure(ValidationErrorCode.TYPE_DATA_MISMATCH.name(),
                                 String.format("Content cannot exceed %d characters", MAX_CONTENT_LENGTH));
        }

        // Validate points
        if (command.getPoints() != null && command.getPoints() < 0) {
            return Result.failure(ValidationErrorCode.TYPE_DATA_MISMATCH.name(),
                                 "Points cannot be negative");
        }

        if (command.getPoints() != null && command.getPoints() > MAX_POINTS) {
            return Result.failure(ValidationErrorCode.TYPE_DATA_MISMATCH.name(),
                                 String.format("Points cannot exceed %d", MAX_POINTS));
        }

        // Validate status
        if (command.getStatus() != null && !isValidStatus(command.getStatus())) {
            return Result.failure(ValidationErrorCode.TYPE_DATA_MISMATCH.name(),
                                 "Invalid status value. Allowed values: draft, published, archived");
        }

        return Result.success(null);
    }

    private Result<Void> validateTypeSpecificData(UpsertQuestionCommand command) {
        switch (command.getQuestionType()) {
            case MCQ:
                if (command.getMcqData() == null) {
                    return Result.failure(ValidationErrorCode.TYPE_DATA_MISMATCH.name(),
                                         "mcq_data is required for MCQ questions");
                }
                return validateMcqData(command.getMcqData());

            case ESSAY:
                if (command.getEssayData() == null) {
                    return Result.failure(ValidationErrorCode.TYPE_DATA_MISMATCH.name(),
                                         "essay_data is required for essay questions");
                }
                return validateEssayData(command.getEssayData());

            case TRUE_FALSE:
                if (command.getTrueFalseData() == null) {
                    return Result.failure(ValidationErrorCode.TYPE_DATA_MISMATCH.name(),
                                         "true_false_data is required for true/false questions");
                }
                return validateTrueFalseData(command.getTrueFalseData());

            default:
                return Result.failure(ValidationErrorCode.INVALID_QUESTION_TYPE.name(),
                                     "Unsupported question type: " + command.getQuestionType());
        }
    }

    private Result<Void> validateMcqData(McqData mcqData) {
        if (mcqData.getOptions() == null || mcqData.getOptions().isEmpty()) {
            return Result.failure(ValidationErrorCode.TYPE_DATA_MISMATCH.name(),
                                 "MCQ questions must have at least one option");
        }

        if (mcqData.getOptions().size() < 2) {
            return Result.failure(ValidationErrorCode.TYPE_DATA_MISMATCH.name(),
                                 "MCQ questions must have at least 2 options");
        }

        if (mcqData.getOptions().size() > 10) {
            return Result.failure(ValidationErrorCode.TYPE_DATA_MISMATCH.name(),
                                 "MCQ questions cannot have more than 10 options");
        }

        boolean hasCorrectAnswer = mcqData.getOptions().stream()
            .anyMatch(option -> option.isCorrect());

        if (!hasCorrectAnswer) {
            return Result.failure(ValidationErrorCode.TYPE_DATA_MISMATCH.name(),
                                 "MCQ questions must have at least one correct answer");
        }

        // Validate time limit if provided
        if (mcqData.getTimeLimitSeconds() != null && mcqData.getTimeLimitSeconds() <= 0) {
            return Result.failure(ValidationErrorCode.TYPE_DATA_MISMATCH.name(),
                                 "Time limit must be positive");
        }

        return Result.success(null);
    }

    private Result<Void> validateEssayData(EssayData essayData) {
        // Validate word count constraints
        if (essayData.getMinWordCount() != null && essayData.getMinWordCount() < 0) {
            return Result.failure(ValidationErrorCode.TYPE_DATA_MISMATCH.name(),
                                 "Minimum word count cannot be negative");
        }

        if (essayData.getMaxWordCount() != null && essayData.getMaxWordCount() < 0) {
            return Result.failure(ValidationErrorCode.TYPE_DATA_MISMATCH.name(),
                                 "Maximum word count cannot be negative");
        }

        if (essayData.getMinWordCount() != null && essayData.getMaxWordCount() != null &&
            essayData.getMinWordCount() > essayData.getMaxWordCount()) {
            return Result.failure(ValidationErrorCode.TYPE_DATA_MISMATCH.name(),
                                 "Minimum word count cannot exceed maximum word count");
        }

        // Validate rubric if provided
        if (essayData.getRubric() != null) {
            if (essayData.getRubric().getCriteria().isEmpty()) {
                return Result.failure(ValidationErrorCode.TYPE_DATA_MISMATCH.name(),
                                     "Essay rubric must have at least one criteria");
            }

            if (essayData.getRubric().getMaxPoints() != null && essayData.getRubric().getMaxPoints() <= 0) {
                return Result.failure(ValidationErrorCode.TYPE_DATA_MISMATCH.name(),
                                     "Essay rubric must have positive max points");
            }
        }

        return Result.success(null);
    }

    private Result<Void> validateTrueFalseData(TrueFalseData trueFalseData) {
        // TrueFalseData validation - the statement is typically in the question content
        // Here we validate the time limit if provided
        if (trueFalseData.getTimeLimitSeconds() != null && trueFalseData.getTimeLimitSeconds() <= 0) {
            return Result.failure(ValidationErrorCode.TYPE_DATA_MISMATCH.name(),
                                 "Time limit must be positive");
        }

        // Validate explanation if provided
        if (trueFalseData.getExplanation() != null && trueFalseData.getExplanation().length() > MAX_CONTENT_LENGTH) {
            return Result.failure(ValidationErrorCode.TYPE_DATA_MISMATCH.name(),
                                 String.format("True/False explanation cannot exceed %d characters", MAX_CONTENT_LENGTH));
        }

        return Result.success(null);
    }

    private Result<Void> validateBusinessRules(UpsertQuestionCommand command) {
        // Validate display order
        if (command.getDisplayOrder() != null && command.getDisplayOrder() < 0) {
            return Result.failure(ValidationErrorCode.TYPE_DATA_MISMATCH.name(),
                                 "Display order cannot be negative");
        }

        // Validate attachment constraints
        if (command.getAttachments() != null && command.getAttachments().size() > 10) {
            return Result.failure(ValidationErrorCode.TYPE_DATA_MISMATCH.name(),
                                 "Questions cannot have more than 10 attachments");
        }

        // Validate that type-specific data matches question type
        if (!isTypeSpecificDataConsistent(command)) {
            return Result.failure(ValidationErrorCode.TYPE_DATA_MISMATCH.name(),
                                 "Question type-specific data is inconsistent with question type");
        }

        return Result.success(null);
    }

    private boolean isValidStatus(String status) {
        return status != null && (
            "draft".equalsIgnoreCase(status) ||
            "published".equalsIgnoreCase(status) ||
            "archived".equalsIgnoreCase(status)
        );
    }

    private boolean isTypeSpecificDataConsistent(UpsertQuestionCommand command) {
        QuestionType type = command.getQuestionType();

        switch (type) {
            case MCQ:
                return command.getMcqData() != null &&
                       command.getEssayData() == null &&
                       command.getTrueFalseData() == null;
            case ESSAY:
                return command.getEssayData() != null &&
                       command.getMcqData() == null &&
                       command.getTrueFalseData() == null;
            case TRUE_FALSE:
                return command.getTrueFalseData() != null &&
                       command.getMcqData() == null &&
                       command.getEssayData() == null;
            default:
                return false;
        }
    }
}