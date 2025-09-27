package com.quizfun.questionbank.infrastructure.configuration;

import com.quizfun.questionbank.application.validation.QuestionBankOwnershipValidator;
import com.quizfun.questionbank.application.validation.TaxonomyReferenceValidator;
import com.quizfun.questionbank.application.validation.QuestionDataIntegrityValidator;
import com.quizfun.shared.validation.ValidationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration for the validation chain used in question upsert operations.
 * The chain follows this order:
 * 1. QuestionBankOwnershipValidator - Security check (most critical)
 * 2. TaxonomyReferenceValidator - Reference integrity check
 * 3. QuestionDataIntegrityValidator - Data validation check (least expensive to fail)
 */
@Configuration
public class ValidationChainConfig {

    private static final Logger logger = LoggerFactory.getLogger(ValidationChainConfig.class);

    /**
     * Creates and configures the validation chain for question upsert operations.
     * The chain is ordered by importance and performance characteristics.
     *
     * @param ownershipValidator Validates question bank ownership and security
     * @param taxonomyValidator Validates taxonomy reference integrity
     * @param dataValidator Validates question data integrity and business rules
     * @return Configured validation chain starting with ownership validation
     */
    @Bean
    @Primary
    @Qualifier("questionUpsertValidationChain")
    public ValidationHandler questionUpsertValidationChain(
            QuestionBankOwnershipValidator ownershipValidator,
            TaxonomyReferenceValidator taxonomyValidator,
            QuestionDataIntegrityValidator dataValidator) {

        logger.info("Configuring question upsert validation chain");

        // Chain order: Ownership -> Taxonomy -> Data Integrity
        // This order ensures:
        // 1. Security first (ownership)
        // 2. Reference integrity second (taxonomy)
        // 3. Data validation last (most detailed, potentially expensive)
        ownershipValidator
            .setNext(taxonomyValidator)
            .setNext(dataValidator);

        logger.info("Question upsert validation chain configured: {} -> {} -> {}",
                   ownershipValidator.getClass().getSimpleName(),
                   taxonomyValidator.getClass().getSimpleName(),
                   dataValidator.getClass().getSimpleName());

        return ownershipValidator;
    }

    /**
     * Alternative validation chain for lightweight operations that only need data integrity checks.
     * This bypasses ownership and taxonomy validation.
     *
     * @param dataValidator The data integrity validator
     * @return Validation chain with only data integrity validation
     */
    @Bean
    @Qualifier("lightweightValidationChain")
    public ValidationHandler lightweightValidationChain(
            QuestionDataIntegrityValidator dataValidator) {

        logger.info("Configuring lightweight validation chain for data integrity only");
        return dataValidator;
    }

    /**
     * Creates a validation chain for ownership-only checks.
     * Useful for operations that only need to verify user access rights.
     *
     * @param ownershipValidator The ownership validator
     * @return Validation chain with only ownership validation
     */
    @Bean
    @Qualifier("ownershipOnlyValidationChain")
    public ValidationHandler ownershipOnlyValidationChain(
            QuestionBankOwnershipValidator ownershipValidator) {

        logger.info("Configuring ownership-only validation chain");
        return ownershipValidator;
    }
}