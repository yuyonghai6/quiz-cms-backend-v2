package com.quizfun.questionbank.infrastructure.configuration;

import com.quizfun.questionbank.application.security.ConcurrentSessionValidator;
import com.quizfun.questionbank.application.security.RateLimitValidator;
import com.quizfun.questionbank.application.security.SecurityAuditLogger;
import com.quizfun.questionbank.application.security.SecurityContextValidator;
import com.quizfun.questionbank.application.security.SessionManagementValidator;
import com.quizfun.questionbank.application.validation.QuestionBankOwnershipValidator;
import com.quizfun.questionbank.application.validation.TaxonomyReferenceValidator;
import com.quizfun.questionbank.application.validation.QuestionDataIntegrityValidator;
import com.quizfun.questionbank.infrastructure.monitoring.ValidationChainMetrics;
import com.quizfun.questionbank.infrastructure.utils.RetryHelper;
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
 * 1. RateLimitValidator - Prevent request flooding (fastest rejection)
 * 2. ConcurrentSessionValidator - Enforce session limits
 * 3. SessionManagementValidator - Detect session hijacking
 * 4. SecurityContextValidator - JWT token validation
 * 5. QuestionBankOwnershipValidator - Resource ownership check
 * 6. TaxonomyReferenceValidator - Reference integrity check
 * 7. QuestionDataIntegrityValidator - Data validation check
 */
@Configuration
public class ValidationChainConfig {

    private static final Logger logger = LoggerFactory.getLogger(ValidationChainConfig.class);

    /**
     * Creates RateLimitValidator bean for injection into validation chain.
     *
     * @param securityAuditLogger Security audit logger for violation logging
     * @param retryHelper Retry helper for resilient operations
     * @param metrics Validation chain metrics for monitoring
     * @return Configured RateLimitValidator instance
     */
    @Bean
    public RateLimitValidator rateLimitValidator(
            SecurityAuditLogger securityAuditLogger,
            RetryHelper retryHelper,
            ValidationChainMetrics metrics) {
        return new RateLimitValidator(securityAuditLogger, retryHelper, metrics);
    }

    /**
     * Creates ConcurrentSessionValidator bean for injection into validation chain.
     *
     * @param securityAuditLogger Security audit logger for violation logging
     * @param retryHelper Retry helper for resilient operations
     * @param metrics Validation chain metrics for monitoring
     * @return Configured ConcurrentSessionValidator instance
     */
    @Bean
    public ConcurrentSessionValidator concurrentSessionValidator(
            SecurityAuditLogger securityAuditLogger,
            RetryHelper retryHelper,
            ValidationChainMetrics metrics) {
        return new ConcurrentSessionValidator(securityAuditLogger, retryHelper, metrics);
    }

    /**
     * Creates SessionManagementValidator bean for injection into validation chain.
     *
     * @param securityAuditLogger Security audit logger for violation logging
     * @param retryHelper Retry helper for resilient operations
     * @param metrics Validation chain metrics for monitoring
     * @return Configured SessionManagementValidator instance
     */
    @Bean
    public SessionManagementValidator sessionManagementValidator(
            SecurityAuditLogger securityAuditLogger,
            RetryHelper retryHelper,
            ValidationChainMetrics metrics) {
        return new SessionManagementValidator(securityAuditLogger, retryHelper, metrics);
    }

    /**
     * Creates SecurityContextValidator bean for injection into validation chain.
     *
     * @param securityAuditLogger Security audit logger for violation logging
     * @param retryHelper Retry helper from US-003 for resilient operations
     * @param metrics Validation chain metrics for monitoring
     * @return Configured SecurityContextValidator instance
     */
    @Bean
    public SecurityContextValidator securityContextValidator(
            SecurityAuditLogger securityAuditLogger,
            RetryHelper retryHelper,
            ValidationChainMetrics metrics) {
        return new SecurityContextValidator(null, securityAuditLogger, retryHelper, metrics);
    }

    /**
     * Creates and configures the validation chain for question upsert operations.
     * The chain is ordered by security priority and performance characteristics.
     *
     * @param rateLimitValidator Rate limiting validator (fastest rejection)
     * @param concurrentSessionValidator Concurrent session limit validator
     * @param sessionValidator Session hijacking detection validator
     * @param securityValidator JWT token security context validator
     * @param ownershipValidator Question bank ownership validator
     * @param taxonomyValidator Taxonomy reference integrity validator
     * @param dataValidator Question data integrity validator
     * @return Configured validation chain starting with rate limiting
     */
    @Bean
    @Primary
    @Qualifier("questionUpsertValidationChain")
    public ValidationHandler questionUpsertValidationChain(
            RateLimitValidator rateLimitValidator,
            ConcurrentSessionValidator concurrentSessionValidator,
            SessionManagementValidator sessionValidator,
            SecurityContextValidator securityValidator,
            QuestionBankOwnershipValidator ownershipValidator,
            TaxonomyReferenceValidator taxonomyValidator,
            QuestionDataIntegrityValidator dataValidator) {

        logger.info("Configuring question upsert validation chain with comprehensive security");

        // Chain the validators in security-first order
        // Chain order: Rate Limit -> Concurrent Session -> Session Management ->
        //              Security Context -> Ownership -> Taxonomy -> Data Integrity
        // This order ensures:
        // 1. Rate limiting first (fastest rejection, prevents DoS)
        // 2. Concurrent session check second (prevent account sharing)
        // 3. Session management third (detect hijacking)
        // 4. JWT token security fourth (prevents path parameter manipulation)
        // 5. Question bank ownership fifth (business security)
        // 6. Reference integrity sixth (taxonomy validation)
        // 7. Data validation last (most detailed, potentially expensive)
        rateLimitValidator
            .setNext(concurrentSessionValidator)
            .setNext(sessionValidator)
            .setNext(securityValidator)
            .setNext(ownershipValidator)
            .setNext(taxonomyValidator)
            .setNext(dataValidator);

        logger.info("Question upsert validation chain configured: {} -> {} -> {} -> {} -> {} -> {} -> {}",
                   rateLimitValidator.getClass().getSimpleName(),
                   concurrentSessionValidator.getClass().getSimpleName(),
                   sessionValidator.getClass().getSimpleName(),
                   securityValidator.getClass().getSimpleName(),
                   ownershipValidator.getClass().getSimpleName(),
                   taxonomyValidator.getClass().getSimpleName(),
                   dataValidator.getClass().getSimpleName());

        return rateLimitValidator;
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