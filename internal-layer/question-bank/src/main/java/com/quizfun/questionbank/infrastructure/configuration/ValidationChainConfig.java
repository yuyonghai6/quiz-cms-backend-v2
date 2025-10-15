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
import org.springframework.beans.factory.annotation.Value;
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
     * Controls whether SecurityContextValidator is included in the validation chain.
     * - true (default): SecurityContextValidator is active (production mode)
     * - false: SecurityContextValidator is bypassed (development/testing mode)
     */
    @Value("${security.context.validator.enabled:true}")
    private boolean securityContextValidatorEnabled;

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
     * <p>
     * The chain can operate in two modes based on security.context.validator.enabled property:
     * <ul>
     *   <li>PRODUCTION MODE (true): Security -> Ownership -> Taxonomy -> Data Integrity</li>
     *   <li>DEVELOPMENT MODE (false): Ownership -> Taxonomy -> Data Integrity (Security bypassed)</li>
     * </ul>
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

        if (securityContextValidatorEnabled) {
            // ═══════════════════════════════════════════════════════════════
            // PRODUCTION MODE: Include SecurityContextValidator
            // ═══════════════════════════════════════════════════════════════
            logger.info("🔗 Configuring Question Upsert Validation Chain: PRODUCTION MODE");
            logger.info("   security.context.validator.enabled = true");
            logger.info("    ✅ rateLimitValidator");
            logger.info("    ✅ concurrentSessionValidator");
            logger.info("    ✅ sessionValidator");
            logger.info("    ✅ SecurityContextValidator (JWT & Path Parameter Validation)");
            logger.info("    ✅ QuestionBankOwnershipValidator");
            logger.info("    ✅ TaxonomyReferenceValidator");
            logger.info("    ✅ QuestionDataIntegrityValidator");

            // Chain order: Security -> Ownership -> Taxonomy -> Data Integrity
            // This order ensures:
            // 1. JWT token security first (prevents path parameter manipulation attacks)
            // 2. Question bank ownership second (business security)
            // 3. Reference integrity third (taxonomy validation)
            // 4. Data validation last (most detailed, potentially expensive)
            rateLimitValidator
                .setNext(concurrentSessionValidator)
                .setNext(sessionValidator)
                .setNext(securityValidator)
                .setNext(ownershipValidator)
                .setNext(taxonomyValidator)
                .setNext(dataValidator);

            return rateLimitValidator;

        } else {
            // ═══════════════════════════════════════════════════════════════
            // DEVELOPMENT MODE: Skip SecurityContextValidator
            // ═══════════════════════════════════════════════════════════════
            logger.warn("⚠️ ═══════════════════════════════════════════════════════════════");
            logger.warn("⚠️ VALIDATION CHAIN: DEVELOPMENT MODE - SecurityContextValidator BYPASSED");
            logger.warn("⚠️ security.context.validator.enabled = false");
            logger.warn("⚠️ ═══════════════════════════════════════════════════════════════");
            logger.warn("⚠️ This configuration MUST NOT be used in production");
            logger.warn("⚠️ Business validations remain active:");
            logger.warn("   1. ⏭️  Seires Security Validator (SKIPPED)");
            logger.warn("   2. ✅ QuestionBankOwnershipValidator");
            logger.warn("   3. ✅ TaxonomyReferenceValidator");
            logger.warn("   4. ✅ QuestionDataIntegrityValidator");
            logger.warn("   5. ✅ rateLimitValidator");

            // Chain order: Ownership -> Taxonomy -> Data Integrity
            // SecurityContextValidator is bypassed for K6 functional testing
            rateLimitValidator
                .setNext(ownershipValidator)
                .setNext(taxonomyValidator)
                .setNext(dataValidator);

            return rateLimitValidator;
        }
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