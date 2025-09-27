package com.quizfun.questionbank.application.validation;

import com.quizfun.questionbank.application.commands.UpsertQuestionCommand;
import com.quizfun.questionbank.application.ports.out.TaxonomySetRepository;
import com.quizfun.questionbank.domain.validation.ValidationErrorCode;
import com.quizfun.shared.common.Result;
import com.quizfun.shared.validation.ValidationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Validates that all taxonomy references in the command exist in the user's taxonomy set.
 * This includes categories, tags, quizzes, and difficulty levels.
 */
@Component
public class TaxonomyReferenceValidator extends ValidationHandler {

    private static final Logger logger = LoggerFactory.getLogger(TaxonomyReferenceValidator.class);
    private final TaxonomySetRepository taxonomySetRepository;

    public TaxonomyReferenceValidator(TaxonomySetRepository taxonomySetRepository) {
        this.taxonomySetRepository = taxonomySetRepository;
    }

    @Override
    public Result<Void> validate(Object command) {
        if (!(command instanceof UpsertQuestionCommand)) {
            return checkNext(command);
        }

        var upsertCommand = (UpsertQuestionCommand) command;

        logger.debug("Validating taxonomy references for user {} and question bank {}",
                    upsertCommand.getUserId(), upsertCommand.getQuestionBankId());

        try {
            // Extract all taxonomy IDs from the command
            List<String> taxonomyIds = upsertCommand.extractTaxonomyIds();

            if (taxonomyIds.isEmpty()) {
                logger.warn("No taxonomy references found in command for question {}",
                           upsertCommand.getSourceQuestionId());
                return Result.failure(
                    ValidationErrorCode.MISSING_REQUIRED_FIELD.name(),
                    "At least one taxonomy reference is required"
                );
            }

            logger.debug("Found {} taxonomy references to validate: {}",
                        taxonomyIds.size(), taxonomyIds);

            // Validate all taxonomy references exist
            Result<Boolean> validationResult = taxonomySetRepository.validateTaxonomyReferences(
                upsertCommand.getUserId(),
                upsertCommand.getQuestionBankId(),
                taxonomyIds
            );

            if (validationResult.isFailure()) {
                logger.error("Repository error during taxonomy validation: {}", validationResult.getError());
                return Result.failure(
                    validationResult.getErrorCode() != null ? validationResult.getErrorCode() : "REPOSITORY_ERROR",
                    validationResult.getError()
                );
            }

            if (!validationResult.getValue()) {
                // Find specific invalid references for detailed error message
                Result<List<String>> invalidReferencesResult = findInvalidReferences(
                    upsertCommand.getUserId(),
                    upsertCommand.getQuestionBankId(),
                    taxonomyIds
                );

                String invalidReferencesMessage;
                if (invalidReferencesResult.isSuccess() && !invalidReferencesResult.getValue().isEmpty()) {
                    List<String> invalidRefs = invalidReferencesResult.getValue();
                    invalidReferencesMessage = String.format("Invalid taxonomy references found: %s",
                        String.join(", ", invalidRefs));

                    logger.warn("Invalid taxonomy references: {} for user {} and question bank {}",
                               invalidRefs, upsertCommand.getUserId(), upsertCommand.getQuestionBankId());
                } else {
                    invalidReferencesMessage = "Some taxonomy references are invalid";
                    logger.warn("Taxonomy validation failed but could not determine specific invalid references for user {} and question bank {}",
                               upsertCommand.getUserId(), upsertCommand.getQuestionBankId());
                }

                return Result.failure(
                    ValidationErrorCode.TAXONOMY_REFERENCE_NOT_FOUND.name(),
                    invalidReferencesMessage
                );
            }

            logger.debug("Taxonomy reference validation passed for user {} and question bank {} with {} valid references",
                        upsertCommand.getUserId(), upsertCommand.getQuestionBankId(), taxonomyIds.size());

            return checkNext(command);

        } catch (Exception e) {
            logger.error("Unexpected error during taxonomy validation for user {} and question bank {}: {}",
                        upsertCommand.getUserId(), upsertCommand.getQuestionBankId(), e.getMessage(), e);

            return Result.failure(
                "TAXONOMY_VALIDATION_ERROR",
                "An error occurred while validating taxonomy references"
            );
        }
    }

    private Result<List<String>> findInvalidReferences(Long userId, Long questionBankId, List<String> taxonomyIds) {
        try {
            return taxonomySetRepository.getInvalidTaxonomyReferences(userId, questionBankId, taxonomyIds);
        } catch (Exception e) {
            logger.error("Error finding invalid taxonomy references for user {} and question bank {}: {}",
                        userId, questionBankId, e.getMessage(), e);
            return Result.failure("INVALID_REFERENCE_CHECK_ERROR",
                                 "Could not determine which taxonomy references are invalid");
        }
    }
}