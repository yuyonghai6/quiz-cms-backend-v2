# Bypass SecurityContextValidator for Functional Testing - Design Document

## Overview

This document provides comprehensive design analysis for bypassing the SecurityContextValidator during functional API testing while maintaining all business validation layers. The solution uses a **property-based conditional approach** to enable/disable SecurityContextValidator via configuration, providing clean code with a single validation chain method.

## Problem Statement

### The Testing Challenge

During K6 functional API testing for the question upsert endpoint (`POST /api/users/{userId}/questionbanks/{questionbankId}/questions`), the system returns authentication errors despite Spring Security being disabled at the web layer:

**Error Response**:
```json
{
  "success": false,
  "message": "Authentication required",
  "data": null
}
```

**Spring Boot Logs**:
```
2025-10-10T17:16:14.192+08:00  INFO 78064 --- [orchestration-layer] [nio-8765-exec-4] c.q.o.a.h.UpsertQuestionCommandHandler   : Handling UpsertQuestionCommand for source question ID: test-mcq-1760087774075
2025-10-10T17:16:14.200+08:00  WARN 78064 --- [orchestration-layer] [nio-8765-exec-4] c.q.q.a.s.QuestionApplicationService     : Validation failed with error: Authentication required
2025-10-10T17:16:14.238+08:00  WARN 78064 --- [orchestration-layer] [onPool-worker-1] SECURITY_AUDIT                           : SECURITY_EVENT: type=MISSING_AUTHENTICATION, userId=null, severity=HIGH
```

### Impact

- **Blocked K6 Testing**: Cannot perform functional API tests without authentication infrastructure
- **Development Velocity**: Slows down TDD workflow for API endpoint testing
- **CI/CD Pipeline**: Prevents automated API testing in continuous integration environments
- **Documentation Accuracy**: API contract states security is "bypassed for testing" but doesn't match reality

### Root Cause Analysis

The issue stems from **multi-layered security architecture**:

**Layer 1: Spring Security (Web Layer)** - ✅ Successfully Disabled
```java
// Spring Security disabled at web layer
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
// OR Spring Security configured to permit all requests
```

**Layer 2: SecurityContextValidator (Application Layer)** - ❌ Still Active
```java
// ValidationChainConfig.java configures validation chain
securityValidator
    .setNext(ownershipValidator)
    .setNext(taxonomyValidator)
    .setNext(dataValidator);
```

**The Disconnect**:
- Spring Security disabled → No JWT tokens in HTTP headers → No authentication in SecurityContext
- SecurityContextValidator still checks `SecurityContextHolder.getContext().getAuthentication()`
- Returns null → Validation fails with "Authentication required"

## Architecture Analysis

### Current Validation Chain Structure

The question upsert operation uses a **Chain of Responsibility** pattern with four validators:

```java
// Current chain order (SecurityContextValidator first)
SecurityContextValidator → QuestionBankOwnershipValidator → TaxonomyReferenceValidator → QuestionDataIntegrityValidator
```

**Validation Flow**:
```
1. SecurityContextValidator (lines 76-83 in SecurityContextValidator.java)
   ├─ Check: SecurityContextHolder.getContext().getAuthentication() != null
   ├─ If null → Fail with "Authentication required" ❌
   └─ If present → Validate JWT userId matches path userId

2. QuestionBankOwnershipValidator (if security passes)
   ├─ Check: User owns the specified question bank
   └─ Queries MongoDB question_banks_per_user collection

3. TaxonomyReferenceValidator (if ownership passes)
   ├─ Check: Referenced taxonomy IDs exist in user's taxonomy_sets
   └─ Validates categories, tags, quizzes, difficulty levels

4. QuestionDataIntegrityValidator (if taxonomy passes)
   ├─ Check: Question type matches type-specific data
   └─ Validates MCQ options, essay rubrics, true/false structure
```

### SecurityContextValidator Implementation

**File**: `internal-layer/question-bank/src/main/java/com/quizfun/questionbank/application/security/SecurityContextValidator.java`

**Key Logic** (lines 76-83):
```java
@Override
public Result<Void> validate(Object command) {
    // 1. Extract and validate authentication context
    var authContext = SecurityContextHolder.getContext().getAuthentication();
    if (authContext == null) {
        logSecurityEvent(SecurityEventType.MISSING_AUTHENTICATION, null,
            SeverityLevel.HIGH, null,
            java.util.Map.of("error", "Request received without authentication context"));
        return createSecurityFailureResult(ValidationErrorCode.INVALID_AUTHENTICATION_TOKEN,
            "Authentication required");  // ❌ FAILS HERE DURING TESTING
    }

    // 2. Validate JWT authentication token type
    if (!(authContext instanceof JwtAuthenticationToken jwtToken)) {
        // Fails if not JWT token
    }

    // 3. Extract user ID from JWT token and validate against path parameter
    Long tokenUserId = extractUserIdFromToken(jwtToken);
    Long pathUserId = upsertCommand.getUserId();
    if (!tokenUserId.equals(pathUserId)) {
        // Critical security violation: Path parameter manipulation detected
    }

    // 4. Continue to next validator
    return checkNext(command);
}
```

**Design Intent**:
- **Production**: Prevents path parameter manipulation attacks (US-022)
- **Security Defense-in-Depth**: Application-level security even if web layer fails
- **Audit Trail**: Logs all security events for compliance (US-021)

### Why Spring Security Disable Doesn't Help

**Spring Security Configuration** affects:
- ✅ HTTP request authentication filters
- ✅ Authorization rules for endpoints
- ✅ JWT token extraction from headers

**Spring Security Configuration does NOT affect**:
- ❌ Application-level validators (like SecurityContextValidator)
- ❌ Validation chain configuration beans
- ❌ SecurityContextHolder state (remains empty without authentication)

**The Gap**:
```
Web Request → Spring Security (DISABLED) → Controller → Mediator → Command Handler
                                                                          ↓
                                            Validation Chain (STILL ACTIVE)
                                                                          ↓
                                            SecurityContextValidator checks SecurityContext
                                                                          ↓
                                                    FAILS: No authentication context
```

## Recommended Solution: Property-Based Conditional Validation

### Approach

Use a **configuration property** (`security.context.validator.enabled`) to control whether SecurityContextValidator is included in the validation chain. This provides:

1. **Clean Code**: Single bean method with conditional logic (no duplicate beans)
2. **Property-Driven**: Easy to toggle via configuration files
3. **Safe Default**: Property defaults to `true` (production mode) if not specified
4. **Profile Integration**: Works seamlessly with Spring profiles (application-dev.properties)

### Why This Is the Best Approach

**Compared to Profile-Based Approach**:
- ✅ **No Code Duplication**: One method instead of two separate bean methods
- ✅ **Single Source of Truth**: Validation chain construction logic in one place
- ✅ **Easier Maintenance**: Changes to chain structure only need updating once
- ✅ **Clear Property Name**: `security.context.validator.enabled` is self-documenting

**Compared to Mock Authentication**:
- ✅ **Simpler Setup**: No need for mock JWT generation infrastructure
- ✅ **True Testing**: Actually tests the "no authentication" scenario
- ✅ **Less Complex**: Doesn't require JWT service mocking

### Implementation Overview

**Property Configuration**:
```properties
# application.properties (production default)
security.context.validator.enabled=true

# application-dev.properties (for K6 testing)
security.context.validator.enabled=false
```

**Java Implementation**:
```java
@Configuration
public class ValidationChainConfig {

    @Value("${security.context.validator.enabled:true}")
    private boolean securityContextValidatorEnabled;

    @Bean
    public ValidationHandler questionUpsertValidationChain(...) {
        if (securityContextValidatorEnabled) {
            // Production: Include SecurityContextValidator
            return securityValidator.setNext(ownershipValidator)...;
        } else {
            // Development: Skip SecurityContextValidator
            return ownershipValidator.setNext(taxonomyValidator)...;
        }
    }
}
```

**Maven Command**:
```bash
# Run with dev profile (loads application-dev.properties)
mvn spring-boot:run -pl orchestration-layer -Dspring-boot.run.profiles=dev
```

### What Gets Bypassed vs. What Remains Active

**Bypassed in Development Mode** (security validation only):
- ❌ JWT token presence check
- ❌ JWT token type validation (JwtAuthenticationToken check)
- ❌ JWT userId extraction
- ❌ Path parameter manipulation detection (US-022)
- ❌ Security audit logging for authentication events

**Remains Active in Development Mode** (all business validations):
- ✅ Question bank ownership validation
- ✅ Taxonomy reference validation (categories, tags, difficulty)
- ✅ Question data integrity validation
- ✅ Question type strategy validation (MCQ, essay, true/false)
- ✅ Business rule enforcement

### Security Risk Assessment

**Development Environment**:
- **Risk Level**: LOW
- **Justification**: Development environment not exposed to external traffic
- **Mitigation**: Ownership validator still prevents cross-user data access
- **Use Case**: Functional API testing with known test data

**Production Environment**:
- **Risk Level**: NONE
- **Justification**: Property defaults to `true` (security enabled)
- **Protection**: Safe default ensures production security even if profile forgotten

**Accidental Deployment Risk**:
- **Scenario**: Deploying with property set to `false` in production
- **Probability**: LOW (property in application.properties defaults to true)
- **Impact**: HIGH (path parameter manipulation attacks possible)
- **Mitigation**:
  - CI/CD check: Scan application.properties for `security.context.validator.enabled=false`
  - Startup validation: Log warning and fail fast if disabled in production profile
  - Environment variable: Can override with `SECURITY_CONTEXT_VALIDATOR_ENABLED=true`

## Detailed Implementation Guide

### Step 1: Update application.properties (Production Default)

**File**: `orchestration-layer/src/main/resources/application.properties`

**Add this property**:
```properties
spring.application.name=orchestration-layer
server.port=8765

# ═══════════════════════════════════════════════════════════
# SecurityContextValidator Configuration (Production Default)
# ═══════════════════════════════════════════════════════════
# Controls whether SecurityContextValidator is included in validation chain
# true = JWT token validation active (PRODUCTION)
# false = JWT token validation bypassed (TESTING ONLY)
#
# This property is read by ValidationChainConfig.java to conditionally
# construct the validation chain with or without SecurityContextValidator.
#
# Default: true (safe for production)
# Override in application-dev.properties for K6 functional testing
# ═══════════════════════════════════════════════════════════
security.context.validator.enabled=true

# MongoDB configuration
spring.data.mongodb.uri=mongodb://root:PASSWORD@localhost:27017,localhost:27018/quizfun?replicaSet=rs0&authSource=admin&readPreference=primary&retryWrites=true&w=majority
spring.data.mongodb.read-preference=primary

# Actuator endpoints
management.endpoints.web.exposure.include=health,info,beans
management.endpoint.health.show-details=always
```

### Step 2: Create/Update application-dev.properties

**File**: `orchestration-layer/src/main/resources/application-dev.properties`

**Content**:
```properties
# ═══════════════════════════════════════════════════════════
# Development Profile Configuration
# ═══════════════════════════════════════════════════════════
# This profile is designed for functional API testing with K6
# SecurityContextValidator is bypassed to enable testing without JWT tokens
# All business validations remain active (ownership, taxonomy, data)
# ═══════════════════════════════════════════════════════════

spring.application.name=orchestration-layer
server.port=8765

# MongoDB configuration (same as production for consistency)
spring.data.mongodb.uri=mongodb://root:bdffe98cbd9f1f134bd48ca3918c1deb38e381e90a1ddc8b582c952f92c2b58e@localhost:27017,localhost:27018/quizfun?replicaSet=rs0&authSource=admin&readPreference=primary&retryWrites=true&w=majority
spring.data.mongodb.read-preference=primary

# Enable actuator endpoints for debugging
management.endpoints.web.exposure.include=health,info,beans,env
management.endpoint.health.show-details=always

# ═══════════════════════════════════════════════════════════
# SecurityContextValidator Configuration - DEVELOPMENT MODE
# ═══════════════════════════════════════════════════════════
# Disable SecurityContextValidator for K6 functional testing
# Set to false to bypass JWT token validation in validation chain
#
# What gets bypassed:
#   - JWT token presence check
#   - JWT token type validation
#   - Path parameter manipulation detection
#
# What remains active:
#   - Question bank ownership validation
#   - Taxonomy reference validation
#   - Question data integrity validation
#
# WARNING: Use in development/testing environments ONLY
# ═══════════════════════════════════════════════════════════
security.context.validator.enabled=false

# Logging for validation chain debugging
logging.level.com.quizfun.questionbank.infrastructure.configuration.ValidationChainConfig=WARN
logging.level.com.quizfun.questionbank.application.validation=DEBUG
logging.level.com.quizfun.questionbank.application.security=DEBUG
```

### Step 3: Modify ValidationChainConfig.java

**File**: `internal-layer/question-bank/src/main/java/com/quizfun/questionbank/infrastructure/configuration/ValidationChainConfig.java`

**Changes needed**:

1. **Add field** to inject the property value
2. **Modify** the `questionUpsertValidationChain` method to use conditional logic

**Complete Implementation**:

```java
package com.quizfun.questionbank.infrastructure.configuration;

import com.quizfun.questionbank.application.security.SecurityAuditLogger;
import com.quizfun.questionbank.application.security.SecurityContextValidator;
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
 *
 * This configuration supports property-based conditional validation chain construction:
 * - Property: security.context.validator.enabled
 * - Default: true (production safe)
 * - Override: false in application-dev.properties for K6 testing
 *
 * PRODUCTION MODE (security.context.validator.enabled=true):
 *   Chain: SecurityContextValidator -> Ownership -> Taxonomy -> Data
 *   - JWT token validation active
 *   - Path parameter manipulation detection active
 *   - Full security audit logging
 *
 * DEVELOPMENT MODE (security.context.validator.enabled=false):
 *   Chain: Ownership -> Taxonomy -> Data (SecurityContextValidator skipped)
 *   - JWT token validation bypassed
 *   - Suitable for K6 functional testing without authentication
 *   - All business validations remain active
 *
 * Maven Command for Development Mode:
 *   mvn spring-boot:run -pl orchestration-layer -Dspring-boot.run.profiles=dev
 */
@Configuration
public class ValidationChainConfig {

    private static final Logger logger = LoggerFactory.getLogger(ValidationChainConfig.class);

    /**
     * Controls whether SecurityContextValidator is included in the validation chain.
     *
     * Property: security.context.validator.enabled
     * Default: true (production safe - includes security validation)
     *
     * Set to false in application-dev.properties to bypass JWT validation for K6 testing.
     *
     * Environment Variable Override: SECURITY_CONTEXT_VALIDATOR_ENABLED
     */
    @Value("${security.context.validator.enabled:true}")
    private boolean securityContextValidatorEnabled;

    /**
     * Creates SecurityContextValidator bean for injection into validation chain.
     * Bean is always created but only used when security.context.validator.enabled=true.
     *
     * @param securityAuditLogger Security audit logger for violation logging
     * @param retryHelper Retry helper for resilient operations
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
     * Chain construction is conditional based on security.context.validator.enabled property.
     *
     * PRODUCTION MODE (security.context.validator.enabled=true):
     *   Chain: SecurityContextValidator -> Ownership -> Taxonomy -> Data
     *   - JWT token presence validated
     *   - JWT token type checked (must be JwtAuthenticationToken)
     *   - JWT userId extracted and compared with path parameter userId
     *   - Path parameter manipulation attacks detected and logged
     *   - Security events logged asynchronously for audit trail
     *
     * DEVELOPMENT MODE (security.context.validator.enabled=false):
     *   Chain: Ownership -> Taxonomy -> Data (SecurityContextValidator skipped)
     *   - JWT token validation bypassed (no authentication required)
     *   - Question bank ownership still validated
     *   - Taxonomy references still validated
     *   - Question data integrity still validated
     *   - Suitable for K6 functional API testing
     *
     * @param securityValidator Validates JWT token security context (conditional - only used if enabled)
     * @param ownershipValidator Validates question bank ownership (always used)
     * @param taxonomyValidator Validates taxonomy reference integrity (always used)
     * @param dataValidator Validates question data integrity (always used)
     * @return Configured validation chain starting with appropriate first validator
     */
    @Bean
    @Primary
    @Qualifier("questionUpsertValidationChain")
    public ValidationHandler questionUpsertValidationChain(
            SecurityContextValidator securityValidator,
            QuestionBankOwnershipValidator ownershipValidator,
            TaxonomyReferenceValidator taxonomyValidator,
            QuestionDataIntegrityValidator dataValidator) {

        if (securityContextValidatorEnabled) {
            // ═══════════════════════════════════════════════════════════
            // PRODUCTION MODE: Include SecurityContextValidator
            // ═══════════════════════════════════════════════════════════
            logger.info("═══════════════════════════════════════════════════════════");
            logger.info("  VALIDATION CHAIN: PRODUCTION MODE");
            logger.info("  SecurityContextValidator: ENABLED");
            logger.info("  Property: security.context.validator.enabled=true");
            logger.info("  JWT Token Validation: ACTIVE");
            logger.info("  Path Parameter Manipulation Detection: ACTIVE");
            logger.info("═══════════════════════════════════════════════════════════");

            // Chain order: Security -> Ownership -> Taxonomy -> Data Integrity
            securityValidator
                .setNext(ownershipValidator)
                .setNext(taxonomyValidator)
                .setNext(dataValidator);

            logger.info("Question upsert validation chain configured: {} -> {} -> {} -> {}",
                       securityValidator.getClass().getSimpleName(),
                       ownershipValidator.getClass().getSimpleName(),
                       taxonomyValidator.getClass().getSimpleName(),
                       dataValidator.getClass().getSimpleName());

            return securityValidator;

        } else {
            // ═══════════════════════════════════════════════════════════
            // DEVELOPMENT MODE: Skip SecurityContextValidator
            // ═══════════════════════════════════════════════════════════
            logger.warn("═══════════════════════════════════════════════════════════");
            logger.warn("  ⚠️  VALIDATION CHAIN: DEVELOPMENT MODE");
            logger.warn("  ⚠️  SecurityContextValidator: DISABLED");
            logger.warn("  ⚠️  Property: security.context.validator.enabled=false");
            logger.warn("  ⚠️  JWT Token Validation: BYPASSED");
            logger.warn("  ⚠️  FOR K6 FUNCTIONAL TESTING ONLY");
            logger.warn("  ⚠️  DO NOT USE IN PRODUCTION ENVIRONMENTS");
            logger.warn("═══════════════════════════════════════════════════════════");

            // Chain order: Ownership -> Taxonomy -> Data Integrity (no security)
            ownershipValidator
                .setNext(taxonomyValidator)
                .setNext(dataValidator);

            logger.info("Question upsert validation chain configured: {} -> {} -> {}",
                       ownershipValidator.getClass().getSimpleName(),
                       taxonomyValidator.getClass().getSimpleName(),
                       dataValidator.getClass().getSimpleName());

            logger.warn("✅ Active validations: Ownership, Taxonomy, Data Integrity");
            logger.warn("⚠️  Bypassed validation: SecurityContextValidator (JWT checks)");
            logger.warn("⚠️  This configuration is suitable for K6 functional testing ONLY");

            return ownershipValidator;
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
```

### Step 4: Running with Development Profile

**Maven Command**:
```bash
# Run with dev profile (loads application-dev.properties)
mvn spring-boot:run -pl orchestration-layer -Dspring-boot.run.profiles=dev

# Alternative syntax (order doesn't matter)
mvn -pl orchestration-layer spring-boot:run -Dspring-boot.run.profiles=dev
```

**Environment Variable Override** (alternative):
```bash
# Override property via environment variable (useful for CI/CD)
export SECURITY_CONTEXT_VALIDATOR_ENABLED=false
mvn spring-boot:run -pl orchestration-layer
```

**Verify Development Mode Active**:

Watch Spring Boot startup logs for this message:
```
═══════════════════════════════════════════════════════════
  ⚠️  VALIDATION CHAIN: DEVELOPMENT MODE
  ⚠️  SecurityContextValidator: DISABLED
  ⚠️  Property: security.context.validator.enabled=false
  ⚠️  JWT Token Validation: BYPASSED
  ⚠️  FOR K6 FUNCTIONAL TESTING ONLY
  ⚠️  DO NOT USE IN PRODUCTION ENVIRONMENTS
═══════════════════════════════════════════════════════════
```

**Run K6 Tests**:
```bash
# Terminal 1: Spring Boot running with dev profile
# Terminal 2: Run K6 tests (no authentication needed)
k6 run api-system-test/test-upsert-question.js
```

## Validation and Testing Strategy

### Verify Production Mode (Default)

**Start without profile**:
```bash
mvn spring-boot:run -pl orchestration-layer
```

**Expected logs**:
```
═══════════════════════════════════════════════════════════
  VALIDATION CHAIN: PRODUCTION MODE
  SecurityContextValidator: ENABLED
  Property: security.context.validator.enabled=true
  JWT Token Validation: ACTIVE
  Path Parameter Manipulation Detection: ACTIVE
═══════════════════════════════════════════════════════════
```

**Test with K6 (should fail)**:
```bash
k6 run api-system-test/test-upsert-question.js
# Expected: "Authentication required" error
```

### Verify Development Mode

**Start with dev profile**:
```bash
mvn spring-boot:run -pl orchestration-layer -Dspring-boot.run.profiles=dev
```

**Expected logs**: Development mode message (see above)

**Test Cases**:

1. **K6 Happy Path** - Should succeed without authentication
2. **Ownership Validation** - Should reject invalid question bank ID
3. **Taxonomy Validation** - Should reject invalid taxonomy reference
4. **Data Integrity Validation** - Should reject type mismatch

### Test Business Validations Still Active

**Ownership Test** (invalid question bank ID):
```bash
# Should return 422 with "QUESTION_BANK_NOT_FOUND" or "UNAUTHORIZED_ACCESS"
curl -X POST http://localhost:8765/api/users/1760085803933/questionbanks/9999999999/questions \
  -H "Content-Type: application/json" \
  -d '{"source_question_id":"test", "question_type":"mcq", ...}'
```

**Taxonomy Test** (invalid category):
```bash
# Should return 422 with "TAXONOMY_REFERENCE_NOT_FOUND"
curl -X POST http://localhost:8765/api/users/1760085803933/questionbanks/1760085804015000/questions \
  -H "Content-Type: application/json" \
  -d '{"taxonomy":{"categories":{"level_1":{"id":"nonexistent"}}}...}'
```

## Production Deployment Safeguards

### CI/CD Pipeline Check

**GitLab CI** (`.gitlab-ci.yml`):
```yaml
production-deployment:
  stage: deploy
  only:
    - main
  script:
    # Fail if SecurityContextValidator is disabled in production config
    - |
      if grep -q "security.context.validator.enabled=false" orchestration-layer/src/main/resources/application.properties; then
        echo "❌ ERROR: SecurityContextValidator disabled in production configuration"
        echo "Property 'security.context.validator.enabled' must be 'true' in application.properties"
        exit 1
      fi
    - echo "✅ Security validation check passed"
    - ./deploy-to-production.sh
```

**GitHub Actions** (`.github/workflows/deploy.yml`):
```yaml
name: Production Deployment
on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Validate Security Configuration
        run: |
          if grep -q "security.context.validator.enabled=false" orchestration-layer/src/main/resources/application.properties; then
            echo "❌ ERROR: SecurityContextValidator disabled in production"
            exit 1
          fi
          echo "✅ Security configuration valid"

      - name: Deploy to Production
        run: ./deploy.sh
```

### Startup Validation Component

**Create startup validator** (optional but recommended):

**File**: `orchestration-layer/src/main/java/com/quizfun/orchestrationlayer/config/SecurityValidatorStartupCheck.java`

```java
package com.quizfun.orchestrationlayer.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Startup validator that checks SecurityContextValidator configuration.
 * Warns if security validation is disabled and fails fast in production.
 */
@Component
public class SecurityValidatorStartupCheck implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger logger = LoggerFactory.getLogger(SecurityValidatorStartupCheck.class);

    @Value("${security.context.validator.enabled:true}")
    private boolean securityContextValidatorEnabled;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (!securityContextValidatorEnabled) {
            // Security validation is disabled - log warning
            logger.warn("═══════════════════════════════════════════════════════════");
            logger.warn("  ⚠️  SECURITY WARNING: SecurityContextValidator is DISABLED");
            logger.warn("  ⚠️  Property: security.context.validator.enabled=false");
            logger.warn("  ⚠️  Active Profile: {}", activeProfile);
            logger.warn("  ⚠️  JWT Token Validation: BYPASSED");
            logger.warn("═══════════════════════════════════════════════════════════");

            // In production environment, fail fast
            if ("prod".equals(activeProfile) || "production".equals(activeProfile)) {
                logger.error("❌ CRITICAL: SecurityContextValidator disabled in production environment");
                logger.error("❌ This is a security violation - application will not start");
                throw new IllegalStateException(
                    "SecurityContextValidator must be enabled in production. " +
                    "Set security.context.validator.enabled=true in application.properties"
                );
            }

            logger.warn("⚠️  Development/Testing environment detected - continuing startup");
            logger.warn("⚠️  DO NOT use this configuration in production");
        } else {
            logger.info("✅ SecurityContextValidator is enabled (security.context.validator.enabled=true)");
            logger.info("✅ JWT token validation active");
            logger.info("✅ Path parameter manipulation detection active");
        }
    }
}
```

## Rollback Plan

### If Issues Occur During Implementation

**Step 1: Revert ValidationChainConfig.java**
```bash
# Reset to original version
git checkout HEAD -- internal-layer/question-bank/src/main/java/com/quizfun/questionbank/infrastructure/configuration/ValidationChainConfig.java

# Rebuild module
mvn clean compile -pl internal-layer/question-bank
```

**Step 2: Remove property from application-dev.properties**
```bash
# Remove the security.context.validator.enabled property
# Or set it to true
sed -i '/security.context.validator.enabled/d' orchestration-layer/src/main/resources/application-dev.properties
```

**Step 3: Restart Spring Boot**
```bash
# Run without dev profile (uses default application.properties)
mvn spring-boot:run -pl orchestration-layer
```

**Step 4: Verify Original Behavior**
```bash
# K6 test should fail with authentication error (original behavior)
k6 run api-system-test/test-upsert-question.js
# Expected: "Authentication required" error
```

### If Wrong Configuration Deployed to Production

**Immediate Action**:
```bash
# Kill running application
kill -9 $(pgrep -f orchestration-layer)

# Fix application.properties (ensure property is true or removed)
echo "security.context.validator.enabled=true" >> orchestration-layer/src/main/resources/application.properties

# OR use environment variable override
export SECURITY_CONTEXT_VALIDATOR_ENABLED=true
mvn spring-boot:run -pl orchestration-layer
```

**Health Check**:
```bash
# Check property value via actuator
curl http://localhost:8765/actuator/env | jq '.propertySources[].properties."security.context.validator.enabled"'

# Verify security validation active in logs
# Should see: "VALIDATION CHAIN: PRODUCTION MODE"
# Should see: "SecurityContextValidator: ENABLED"
```

**Test Authentication Required**:
```bash
# Verify authentication is required
curl -X POST http://localhost:8765/api/users/123/questionbanks/456/questions \
  -H "Content-Type: application/json" \
  -d '{"test": "data"}'

# Expected: {"success":false,"message":"Authentication required","data":null}
```

## Success Criteria

The solution is successful when ALL of the following criteria are met:

### Functional Requirements
- ✅ K6 happy path test passes without JWT authentication (when using dev profile)
- ✅ Ownership validation remains active and rejects invalid question bank IDs
- ✅ Taxonomy validation remains active and rejects invalid taxonomy references
- ✅ Data integrity validation remains active and rejects type mismatches
- ✅ Default configuration (no profile) requires JWT authentication

### Configuration Requirements
- ✅ Property `security.context.validator.enabled` controls SecurityContextValidator inclusion
- ✅ Default value is `true` (production safe)
- ✅ Dev profile sets property to `false`
- ✅ Clear logging indicates which validation mode is active
- ✅ Single bean method with conditional logic (no duplicate beans)

### Security Requirements
- ✅ Production mode uses full security validation chain
- ✅ Development mode bypasses only SecurityContextValidator
- ✅ CI/CD pipeline validates property configuration
- ✅ Startup validation warns about disabled security
- ✅ Production deployment fails if security disabled

### Documentation Requirements
- ✅ Property purpose documented in application.properties
- ✅ Code comments explain conditional logic
- ✅ Maven command documented for dev profile
- ✅ API contract updated with testing configuration

## Benefits of This Approach

1. **Clean Code**: Single bean method with if/else instead of duplicate beans
2. **Clear Property Naming**: `security.context.validator.enabled` is self-documenting
3. **Safe Default**: Property defaults to `true` ensuring production safety
4. **Easy Maintenance**: Validation chain logic in one place
5. **Profile Integration**: Works seamlessly with Spring Boot profiles
6. **Simple Configuration**: One property controls behavior
7. **Production Safe**: Hard to accidentally deploy with wrong configuration
8. **Business Validation Preserved**: All business rules remain enforced

## Conclusion

The **property-based conditional validation** approach provides a clean, maintainable solution for bypassing SecurityContextValidator during functional testing while preserving all business validation logic.

This design:
- ✅ **Maintains Security**: Default property value ensures production safety
- ✅ **Enables Testing**: Dev profile sets property to false for K6 testing
- ✅ **Preserves Business Logic**: Ownership, taxonomy, and data validations remain active
- ✅ **Clean Code**: Single method with conditional logic (no duplication)
- ✅ **Self-Documenting**: Property name clearly indicates purpose
- ✅ **Easy Maintenance**: Changes to chain structure only need updating once
- ✅ **Profile Integration**: Works with Spring Boot's profile system

The implementation is straightforward, reversible, provides clear observability through logging, and follows the principle of **clean code with property-driven configuration**.
