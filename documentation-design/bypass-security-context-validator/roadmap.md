# Implementation Roadmap: SecurityContextValidator Bypass for K6 Functional Testing

## Document Information
- **Feature**: Property-Based SecurityContextValidator Bypass
- **Related Design**: `design.md` (Property-Based Conditional Validation - Option 2)
- **Target**: Enable K6 functional tests in development environment
- **Approach**: Single bean method with property-driven conditional logic
- **Property Name**: `security.context.validator.enabled`
- **Created**: 2025-10-10
- **Status**: Planning Phase

---

## Overview

This roadmap provides a step-by-step implementation guide for adding property-based conditional validation to bypass SecurityContextValidator during K6 functional testing in development mode.

**Key Principle**: Clean code approach using a single bean method with if/else logic based on configuration property, avoiding bean duplication.

---

## Implementation Phases

### Phase 1: Configuration Setup (15 minutes)

Add configuration properties to control SecurityContextValidator behavior across environments.

#### Task 1.1: Update Production Configuration
**File**: `orchestration-layer/src/main/resources/application.properties`

**Action**: Add the following configuration at the end of the file:

```properties
# ============================================================
# SecurityContextValidator Configuration
# ============================================================
# Controls whether SecurityContextValidator is included in the validation chain.
#
# PRODUCTION DEFAULT: true (SecurityContextValidator ACTIVE)
# - Enforces JWT authentication checks
# - Validates path parameter manipulation
# - Logs security audit events
#
# DEVELOPMENT OVERRIDE: false (SecurityContextValidator BYPASSED)
# - Set to false in application-dev.properties for K6 functional testing
# - Business validations (ownership, taxonomy, data integrity) remain active
#
# ⚠️ WARNING: Never set to false in production environments
security.context.validator.enabled=true
```

**Verification**:
```bash
grep "security.context.validator.enabled" orchestration-layer/src/main/resources/application.properties
```
Expected output: `security.context.validator.enabled=true`

---

#### Task 1.2: Update Development Configuration
**File**: `orchestration-layer/src/main/resources/application-dev.properties`

**Action**: Add the following configuration at the end of the file:

```properties
# ============================================================
# SecurityContextValidator Configuration (Development Override)
# ============================================================
# DEVELOPMENT MODE: Bypass SecurityContextValidator for K6 functional testing
#
# This allows K6 tests to call endpoints without JWT authentication while
# still enforcing business validations:
# ✓ QuestionBankOwnershipValidator - Ensures user owns the question bank
# ✓ TaxonomyReferenceValidator - Validates taxonomy relationships
# ✓ QuestionDataIntegrityValidator - Enforces data integrity rules
#
# ⚠️ This override is ONLY for development and testing environments
# ⚠️ Never use this configuration in production
security.context.validator.enabled=false
```

**Verification**:
```bash
grep "security.context.validator.enabled" orchestration-layer/src/main/resources/application-dev.properties
```
Expected output: `security.context.validator.enabled=false`

---

### Phase 2: ValidationChainConfig Modification (30 minutes)

Modify the validation chain configuration to conditionally include SecurityContextValidator based on the property value.

#### Task 2.1: Add Property Injection
**File**: `internal-layer/question-bank/src/main/java/com/quizfun/questionbank/infrastructure/configuration/ValidationChainConfig.java`

**Action**: Add the property injection field at the class level (after Logger declaration):

```java
@Value("${security.context.validator.enabled:true}")
private boolean securityContextValidatorEnabled;
```

**Location**: Add after the Logger field declaration, before the bean methods.

**Code Context**:
```java
public class ValidationChainConfig {

    private static final Logger logger = LoggerFactory.getLogger(ValidationChainConfig.class);

    // ADD THIS FIELD HERE
    @Value("${security.context.validator.enabled:true}")
    private boolean securityContextValidatorEnabled;

    @Bean
    @Primary
    @Qualifier("questionUpsertValidationChain")
    public ValidationHandler questionUpsertValidationChain(...) {
        // ... method implementation
    }
}
```

**Verification**: Compile the module to ensure no syntax errors:
```bash
mvn clean compile -pl internal-layer/question-bank
```

---

#### Task 2.2: Modify questionUpsertValidationChain Method
**File**: `internal-layer/question-bank/src/main/java/com/quizfun/questionbank/infrastructure/configuration/ValidationChainConfig.java`

**Action**: Replace the existing `questionUpsertValidationChain` method with conditional logic:

**Current Code** (to be replaced):
```java
@Bean
@Primary
@Qualifier("questionUpsertValidationChain")
public ValidationHandler questionUpsertValidationChain(
        SecurityContextValidator securityValidator,
        QuestionBankOwnershipValidator ownershipValidator,
        TaxonomyReferenceValidator taxonomyValidator,
        QuestionDataIntegrityValidator dataValidator) {

    logger.info("🔗 Configuring Question Upsert Validation Chain:");
    logger.info("   1. SecurityContextValidator (JWT & Path Parameter Validation)");
    logger.info("   2. QuestionBankOwnershipValidator");
    logger.info("   3. TaxonomyReferenceValidator");
    logger.info("   4. QuestionDataIntegrityValidator");

    securityValidator
        .setNext(ownershipValidator)
        .setNext(taxonomyValidator)
        .setNext(dataValidator);

    return securityValidator;
}
```

**New Code** (with conditional logic):
```java
@Bean
@Primary
@Qualifier("questionUpsertValidationChain")
public ValidationHandler questionUpsertValidationChain(
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
        logger.info("   1. ✅ SecurityContextValidator (JWT & Path Parameter Validation)");
        logger.info("   2. ✅ QuestionBankOwnershipValidator");
        logger.info("   3. ✅ TaxonomyReferenceValidator");
        logger.info("   4. ✅ QuestionDataIntegrityValidator");

        securityValidator
            .setNext(ownershipValidator)
            .setNext(taxonomyValidator)
            .setNext(dataValidator);

        return securityValidator;

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
        logger.warn("   1. ⏭️  SecurityContextValidator (SKIPPED)");
        logger.warn("   2. ✅ QuestionBankOwnershipValidator");
        logger.warn("   3. ✅ TaxonomyReferenceValidator");
        logger.warn("   4. ✅ QuestionDataIntegrityValidator");

        ownershipValidator
            .setNext(taxonomyValidator)
            .setNext(dataValidator);

        return ownershipValidator;
    }
}
```

**Key Changes**:
- Added if/else conditional based on `securityContextValidatorEnabled`
- Production mode (true): Chain starts with SecurityContextValidator
- Development mode (false): Chain starts with QuestionBankOwnershipValidator
- Enhanced logging to clearly indicate which mode is active
- Warning logs in development mode to prevent accidental production use

**Verification**: Compile the module to ensure no syntax errors:
```bash
mvn clean compile -pl internal-layer/question-bank
```

---

### Phase 3: Integration Testing (20 minutes)

Verify that the property-based conditional validation works correctly in both modes.

#### Task 3.1: Test Production Mode (Default Behavior)
**Objective**: Verify SecurityContextValidator is active when property is true or not set.

**Steps**:
1. Start Spring Boot with default profile (production mode):
```bash
mvn spring-boot:run -pl orchestration-layer
```

2. Check startup logs for validation chain configuration:
```bash
# Expected log output:
🔗 Configuring Question Upsert Validation Chain: PRODUCTION MODE
   security.context.validator.enabled = true
   1. ✅ SecurityContextValidator (JWT & Path Parameter Validation)
   2. ✅ QuestionBankOwnershipValidator
   3. ✅ TaxonomyReferenceValidator
   4. ✅ QuestionDataIntegrityValidator
```

3. Verify SecurityContextValidator is active by attempting API call without authentication:
```bash
curl -X POST http://localhost:8765/api/users/1760085803933/questionbanks/1760085804015000/questions \
  -H "Content-Type: application/json" \
  -d '{
    "source_question_id": "test-production-check",
    "question_type": "mcq",
    "title": "Test Production Mode",
    "content": "<p>Test</p>",
    "status": "draft",
    "mcq_data": {
      "options": [
        {"id": 1, "text": "A", "is_correct": true, "explanation": "Correct"}
      ],
      "shuffle_options": false,
      "allow_multiple_correct": false
    }
  }'
```

**Expected Response** (401 or Authentication Error):
```json
{
  "success": false,
  "message": "Authentication required",
  "data": null
}
```

**Success Criteria**:
- ✅ Startup log shows "PRODUCTION MODE"
- ✅ API call rejected with authentication error
- ✅ SecurityContextValidator is enforcing authentication

---

#### Task 3.2: Test Development Mode (Dev Profile)
**Objective**: Verify SecurityContextValidator is bypassed when property is false.

**Steps**:
1. Stop the Spring Boot application (Ctrl+C)

2. Start Spring Boot with dev profile:
```bash
mvn spring-boot:run -pl orchestration-layer -Dspring-boot.run.profiles=dev
```

3. Check startup logs for validation chain configuration:
```bash
# Expected log output:
⚠️ ═══════════════════════════════════════════════════════════════
⚠️ VALIDATION CHAIN: DEVELOPMENT MODE - SecurityContextValidator BYPASSED
⚠️ security.context.validator.enabled = false
⚠️ ═══════════════════════════════════════════════════════════════
⚠️ This configuration MUST NOT be used in production
⚠️ Business validations remain active:
   1. ⏭️  SecurityContextValidator (SKIPPED)
   2. ✅ QuestionBankOwnershipValidator
   3. ✅ TaxonomyReferenceValidator
   4. ✅ QuestionDataIntegrityValidator
```

4. Verify API call succeeds without authentication:
```bash
curl -X POST http://localhost:8765/api/users/1760085803933/questionbanks/1760085804015000/questions \
  -H "Content-Type: application/json" \
  -d '{
    "source_question_id": "test-dev-check-'"$(date +%s)"'",
    "question_type": "mcq",
    "title": "Test Development Mode",
    "content": "<p>What is 2+2?</p>",
    "status": "draft",
    "solution_explanation": "<p>Basic math</p>",
    "mcq_data": {
      "options": [
        {"id": 1, "text": "3", "is_correct": false, "explanation": "Incorrect"},
        {"id": 2, "text": "4", "is_correct": true, "explanation": "Correct"}
      ],
      "shuffle_options": false,
      "allow_multiple_correct": false
    },
    "taxonomy": {
      "categories": {
        "level_1": {"id": "math", "name": "Math", "slug": "math", "parent_id": null}
      },
      "tags": [
        {"id": "easy", "name": "Easy", "color": "#28a745"}
      ],
      "difficulty_level": {
        "level": "easy",
        "numeric_value": 1,
        "description": "Basic"
      }
    },
    "metadata": {
      "created_source": "roadmap-integration-test",
      "last_modified": "'"$(date -u +"%Y-%m-%dT%H:%M:%SZ")"'",
      "version": 1,
      "author_id": 1760085803933
    }
  }'
```

**Expected Response** (200 OK):
```json
{
  "success": true,
  "message": "Question created successfully",
  "data": {
    "questionId": "...",
    "sourceQuestionId": "test-dev-check-...",
    "operation": "created",
    "taxonomyRelationshipsCount": 2
  }
}
```

**Success Criteria**:
- ✅ Startup log shows "DEVELOPMENT MODE" with warning symbols
- ✅ API call succeeds with 200 OK response
- ✅ SecurityContextValidator is bypassed
- ✅ Business validations still execute (ownership, taxonomy, data integrity)

---

#### Task 3.3: Run K6 Functional Test
**Objective**: Verify K6 test passes with development profile active.

**Steps**:
1. Ensure Spring Boot is running with dev profile:
```bash
mvn spring-boot:run -pl orchestration-layer -Dspring-boot.run.profiles=dev
```

2. Run K6 functional test:
```bash
k6 run api-system-test/test-upsert-question.js
```

3. Verify test output:
```bash
# Expected output:
📝 Testing Happy Path - Create MCQ with source_question_id: test-mcq-...
✅ Happy Path (Create MCQ): ALL CHECKS PASSED
   Created questionId: ...
   Source questionId: test-mcq-...
   Operation: created
   Taxonomy relationships: 2
   Message: Question created successfully

✓ status is 200 OK
✓ content-type is JSON
✓ response has success field
✓ success is true
✓ message indicates success
✓ data object exists
✓ questionId exists
✓ sourceQuestionId matches request
✓ operation is created
✓ taxonomyRelationshipsCount exists
✓ header X-Operation exists
✓ header X-Question-Id exists

checks.........................: 100.00% ✓ 11      ✗ 0
```

**Success Criteria**:
- ✅ K6 test completes without authentication errors
- ✅ All checks pass (100% success rate)
- ✅ Response indicates successful question creation
- ✅ MongoDB contains the created question

---

### Phase 4: Business Validation Verification (15 minutes)

Verify that business validations (ownership, taxonomy, data integrity) remain active even when SecurityContextValidator is bypassed.

#### Task 4.1: Test Ownership Validation
**Objective**: Verify QuestionBankOwnershipValidator rejects requests for non-existent question banks.

**Test Case**: Attempt to create question in non-existent question bank.

**Steps**:
1. Spring Boot running with dev profile

2. Create K6 test for invalid ownership:
```bash
# Create temporary test file
cat > api-system-test/test-validation-ownership.js << 'EOF'
import { check } from 'k6';
import http from 'k6/http';

export const options = {
  vus: 1,
  iterations: 1,
};

const BASE_URL = 'http://localhost:8765';
const TEST_USER_ID = 1760085803933;
const INVALID_QUESTION_BANK_ID = 9999999999999; // Non-existent

export default function () {
  const payload = JSON.stringify({
    source_question_id: `test-invalid-ownership-${Date.now()}`,
    question_type: 'mcq',
    title: 'Test Ownership Validation',
    content: '<p>This should fail</p>',
    status: 'draft',
    mcq_data: {
      options: [
        { id: 1, text: 'A', is_correct: true, explanation: 'A' }
      ],
      shuffle_options: false,
      allow_multiple_correct: false
    }
  });

  const res = http.post(
    `${BASE_URL}/api/users/${TEST_USER_ID}/questionbanks/${INVALID_QUESTION_BANK_ID}/questions`,
    payload,
    { headers: { 'Content-Type': 'application/json' } }
  );

  check(res, {
    '✓ status is 404 or 403 (ownership validation)': (r) => r.status === 404 || r.status === 403,
    '✓ success is false': (r) => {
      try { return r.json().success === false; } catch (e) { return false; }
    }
  });

  console.log('Response Status:', res.status);
  console.log('Response Body:', res.body);
}
EOF

# Run test
k6 run api-system-test/test-validation-ownership.js
```

**Expected Result**: Test passes with 404 or 403 status, indicating ownership validation is active.

**Success Criteria**:
- ✅ Request rejected with 404 or 403 status
- ✅ QuestionBankOwnershipValidator is enforcing ownership rules

---

#### Task 4.2: Test Taxonomy Validation
**Objective**: Verify TaxonomyReferenceValidator rejects invalid taxonomy references.

**Test Case**: Attempt to create question with invalid taxonomy category.

**Steps**:
1. Spring Boot running with dev profile

2. Create K6 test for invalid taxonomy:
```bash
# Create temporary test file
cat > api-system-test/test-validation-taxonomy.js << 'EOF'
import { check } from 'k6';
import http from 'k6/http';

export const options = {
  vus: 1,
  iterations: 1,
};

const BASE_URL = 'http://localhost:8765';
const TEST_USER_ID = 1760085803933;
const TEST_QUESTION_BANK_ID = 1760085804015000;

export default function () {
  const payload = JSON.stringify({
    source_question_id: `test-invalid-taxonomy-${Date.now()}`,
    question_type: 'mcq',
    title: 'Test Taxonomy Validation',
    content: '<p>This should fail</p>',
    status: 'draft',
    mcq_data: {
      options: [
        { id: 1, text: 'A', is_correct: true, explanation: 'A' }
      ],
      shuffle_options: false,
      allow_multiple_correct: false
    },
    taxonomy: {
      categories: {
        level_1: {
          id: 'non-existent-category-99999',
          name: 'Invalid',
          slug: 'invalid',
          parent_id: null
        }
      }
    },
    metadata: {
      version: 1,
      author_id: TEST_USER_ID
    }
  });

  const res = http.post(
    `${BASE_URL}/api/users/${TEST_USER_ID}/questionbanks/${TEST_QUESTION_BANK_ID}/questions`,
    payload,
    { headers: { 'Content-Type': 'application/json' } }
  );

  check(res, {
    '✓ status is 400 or 404 (taxonomy validation)': (r) => r.status === 400 || r.status === 404,
    '✓ success is false': (r) => {
      try { return r.json().success === false; } catch (e) { return false; }
    }
  });

  console.log('Response Status:', res.status);
  console.log('Response Body:', res.body);
}
EOF

# Run test
k6 run api-system-test/test-validation-taxonomy.js
```

**Expected Result**: Test passes with 400 or 404 status, indicating taxonomy validation is active.

**Success Criteria**:
- ✅ Request rejected with 400 or 404 status
- ✅ TaxonomyReferenceValidator is enforcing taxonomy rules

---

#### Task 4.3: Test Data Integrity Validation
**Objective**: Verify QuestionDataIntegrityValidator enforces data integrity rules.

**Test Case**: Attempt to create MCQ question with missing required fields.

**Steps**:
1. Spring Boot running with dev profile

2. Create K6 test for invalid data:
```bash
# Create temporary test file
cat > api-system-test/test-validation-data-integrity.js << 'EOF'
import { check } from 'k6';
import http from 'k6/http';

export const options = {
  vus: 1,
  iterations: 1,
};

const BASE_URL = 'http://localhost:8765';
const TEST_USER_ID = 1760085803933;
const TEST_QUESTION_BANK_ID = 1760085804015000;

export default function () {
  const payload = JSON.stringify({
    source_question_id: `test-invalid-data-${Date.now()}`,
    question_type: 'mcq',
    title: 'Test Data Integrity Validation',
    content: '<p>This should fail</p>',
    status: 'draft',
    mcq_data: {
      options: [], // EMPTY OPTIONS - should fail validation
      shuffle_options: false,
      allow_multiple_correct: false
    },
    metadata: {
      version: 1,
      author_id: TEST_USER_ID
    }
  });

  const res = http.post(
    `${BASE_URL}/api/users/${TEST_USER_ID}/questionbanks/${TEST_QUESTION_BANK_ID}/questions`,
    payload,
    { headers: { 'Content-Type': 'application/json' } }
  );

  check(res, {
    '✓ status is 400 (data integrity validation)': (r) => r.status === 400,
    '✓ success is false': (r) => {
      try { return r.json().success === false; } catch (e) { return false; }
    }
  });

  console.log('Response Status:', res.status);
  console.log('Response Body:', res.body);
}
EOF

# Run test
k6 run api-system-test/test-validation-data-integrity.js
```

**Expected Result**: Test passes with 400 status, indicating data integrity validation is active.

**Success Criteria**:
- ✅ Request rejected with 400 status
- ✅ QuestionDataIntegrityValidator is enforcing data integrity rules

---

### Phase 5: Documentation and Cleanup (10 minutes)

#### Task 5.1: Update Project Documentation
**File**: `CLAUDE.md` or create `documentation-design/bypass-security-context-validator/USAGE.md`

**Action**: Document the property-based bypass feature for future developers.

**Content**:
```markdown
## SecurityContextValidator Bypass for Functional Testing

### Overview
The validation chain supports property-based conditional inclusion of SecurityContextValidator.

### Configuration Property
`security.context.validator.enabled` (default: `true`)

### Usage

**Development/Testing Mode** (SecurityContextValidator BYPASSED):
```bash
mvn spring-boot:run -pl orchestration-layer -Dspring-boot.run.profiles=dev
```

**Production Mode** (SecurityContextValidator ACTIVE):
```bash
mvn spring-boot:run -pl orchestration-layer
# OR
mvn spring-boot:run -pl orchestration-layer -Dspring-boot.run.profiles=prod
```

### Security Notes
- ⚠️ **NEVER** set `security.context.validator.enabled=false` in production
- ⚠️ Dev profile is for K6 functional testing and local development only
- ✅ Business validations (ownership, taxonomy, data integrity) remain active in both modes
- ✅ Production defaults to `true` (SecurityContextValidator enforced)

### Validation Chain Behavior

**Production Mode** (`security.context.validator.enabled=true`):
1. ✅ SecurityContextValidator (JWT & Path Parameter Validation)
2. ✅ QuestionBankOwnershipValidator
3. ✅ TaxonomyReferenceValidator
4. ✅ QuestionDataIntegrityValidator

**Development Mode** (`security.context.validator.enabled=false`):
1. ⏭️  SecurityContextValidator (SKIPPED)
2. ✅ QuestionBankOwnershipValidator
3. ✅ TaxonomyReferenceValidator
4. ✅ QuestionDataIntegrityValidator

### Related Files
- `design.md`: Technical design document
- `roadmap.md`: Implementation roadmap
- `ValidationChainConfig.java`: Validation chain configuration
```

---

#### Task 5.2: Clean Up Temporary Test Files
**Action**: Remove temporary validation test files created during Phase 4 (optional).

```bash
rm -f api-system-test/test-validation-ownership.js
rm -f api-system-test/test-validation-taxonomy.js
rm -f api-system-test/test-validation-data-integrity.js
```

**Note**: These files can be kept if you want to maintain regression tests for validation behavior.

---

### Phase 6: Final Verification and Sign-Off (10 minutes)

#### Task 6.1: Final Integration Test
**Objective**: Complete end-to-end verification of the feature.

**Steps**:
1. Stop Spring Boot application

2. Run full test suite to ensure no regressions:
```bash
mvn clean test -pl internal-layer/question-bank
```

3. Start Spring Boot with dev profile:
```bash
mvn spring-boot:run -pl orchestration-layer -Dspring-boot.run.profiles=dev
```

4. Run K6 functional test suite:
```bash
k6 run api-system-test/test-create-default-question-bank.js
k6 run api-system-test/test-upsert-question.js
```

5. Verify startup logs show development mode warning

6. Stop Spring Boot and restart with default profile (production mode):
```bash
# Ctrl+C to stop
mvn spring-boot:run -pl orchestration-layer
```

7. Verify startup logs show production mode (no warnings)

8. Verify authentication is enforced in production mode

**Success Criteria**:
- ✅ All unit tests pass
- ✅ K6 tests pass in dev mode
- ✅ Authentication enforced in production mode
- ✅ Startup logs clearly indicate active mode

---

#### Task 6.2: Git Commit
**Objective**: Commit the implementation with clear commit message.

**Steps**:
```bash
git add orchestration-layer/src/main/resources/application.properties
git add orchestration-layer/src/main/resources/application-dev.properties
git add internal-layer/question-bank/src/main/java/com/quizfun/questionbank/infrastructure/configuration/ValidationChainConfig.java
git add documentation-design/bypass-security-context-validator/

git commit -m "feat: Add property-based SecurityContextValidator bypass for K6 functional testing

- Added security.context.validator.enabled property (default: true)
- Modified ValidationChainConfig to conditionally include SecurityContextValidator
- Created application-dev.properties with bypass configuration
- Business validations (ownership, taxonomy, data integrity) remain active
- Added comprehensive design and roadmap documentation

Related to Use Case 2: Upsert Question with Taxonomy
Enables K6 functional tests without JWT authentication in dev mode

🤖 Generated with Claude Code (https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

## Timeline Summary

| Phase | Duration | Tasks |
|-------|----------|-------|
| Phase 1: Configuration Setup | 15 minutes | Add properties to application.properties and application-dev.properties |
| Phase 2: ValidationChainConfig Modification | 30 minutes | Add @Value field and modify validation chain method |
| Phase 3: Integration Testing | 20 minutes | Test production mode, dev mode, and K6 test execution |
| Phase 4: Business Validation Verification | 15 minutes | Verify ownership, taxonomy, and data integrity validations |
| Phase 5: Documentation and Cleanup | 10 minutes | Update project documentation |
| Phase 6: Final Verification and Sign-Off | 10 minutes | Full integration test and git commit |
| **Total** | **100 minutes (~1.5 hours)** | |

---

## Success Criteria

### Functional Requirements
- ✅ Property `security.context.validator.enabled` controls SecurityContextValidator inclusion
- ✅ Default value is `true` (production safe)
- ✅ Dev profile overrides to `false` for K6 testing
- ✅ K6 functional tests pass without authentication in dev mode
- ✅ Business validations remain active in both modes
- ✅ Authentication enforced in production mode

### Non-Functional Requirements
- ✅ Clean code: Single bean method with if/else (no bean duplication)
- ✅ Clear startup logging indicates active mode
- ✅ Warning logs in development mode prevent accidental production use
- ✅ No regression in existing tests
- ✅ Comprehensive documentation provided

### Security Requirements
- ✅ Production defaults to secured mode (SecurityContextValidator active)
- ✅ Development bypass clearly marked with warnings
- ✅ Business validations not affected by bypass
- ✅ No security vulnerabilities introduced

---

## Rollback Procedure

If issues arise during implementation, follow these rollback steps:

### Rollback Step 1: Restore ValidationChainConfig
```bash
git checkout HEAD -- internal-layer/question-bank/src/main/java/com/quizfun/questionbank/infrastructure/configuration/ValidationChainConfig.java
```

### Rollback Step 2: Remove Configuration Properties
```bash
# Remove the security.context.validator.enabled property from both files
git checkout HEAD -- orchestration-layer/src/main/resources/application.properties
git checkout HEAD -- orchestration-layer/src/main/resources/application-dev.properties
```

### Rollback Step 3: Rebuild and Restart
```bash
mvn clean compile -pl internal-layer/question-bank
mvn spring-boot:run -pl orchestration-layer
```

### Rollback Step 4: Verify System Stability
```bash
mvn test -pl internal-layer/question-bank
```

---

## Known Limitations and Trade-offs

### Limitations
1. **Manual Profile Selection**: Developer must remember to use `-Dspring-boot.run.profiles=dev`
2. **No Runtime Toggle**: Cannot switch between modes without restarting application
3. **Global Setting**: Property affects all validation chains (currently only one exists)

### Trade-offs
1. **Security vs Testability**: Bypassing SecurityContextValidator in dev mode reduces security for easier testing
   - **Mitigation**: Business validations remain active, clear warnings in logs
2. **Configuration Management**: Additional property to manage across environments
   - **Mitigation**: Safe default (true), clear documentation
3. **Code Complexity**: Conditional logic in bean configuration
   - **Mitigation**: Well-documented, enhanced logging, single method approach

---

## Future Enhancements

### Potential Improvements
1. **Startup Validation**: Add `@PostConstruct` method to validate property value and environment consistency
2. **Environment-Aware Validation**: Automatically detect test/dev/prod environment and warn if misconfigured
3. **Metrics**: Add metrics/monitoring for validation chain execution in different modes
4. **Integration Test Profile**: Create dedicated `application-integration-test.properties` for CI/CD
5. **Property Validation**: Use Spring Boot's `@ConfigurationProperties` with JSR-303 validation

### Example Startup Validation
```java
@PostConstruct
public void validateConfiguration() {
    if (!securityContextValidatorEnabled) {
        String environment = System.getProperty("spring.profiles.active", "default");
        if (environment.contains("prod")) {
            throw new IllegalStateException(
                "FATAL: security.context.validator.enabled=false in production environment! " +
                "This is a security violation. Application startup aborted."
            );
        }
        logger.warn("⚠️⚠️⚠️ SECURITY WARNING ⚠️⚠️⚠️");
        logger.warn("⚠️ SecurityContextValidator is DISABLED");
        logger.warn("⚠️ This configuration MUST NOT be used in production");
        logger.warn("⚠️ Current profile: {}", environment);
    }
}
```

---

## Appendix A: Quick Reference Commands

### Start Spring Boot (Development Mode)
```bash
mvn spring-boot:run -pl orchestration-layer -Dspring-boot.run.profiles=dev
```

### Start Spring Boot (Production Mode)
```bash
mvn spring-boot:run -pl orchestration-layer
```

### Run K6 Functional Tests
```bash
k6 run api-system-test/test-create-default-question-bank.js
k6 run api-system-test/test-upsert-question.js
```

### Run Unit Tests
```bash
mvn test -pl internal-layer/question-bank
```

### Compile Specific Module
```bash
mvn clean compile -pl internal-layer/question-bank
```

### Check Property Value
```bash
grep "security.context.validator.enabled" orchestration-layer/src/main/resources/application*.properties
```

---

## Appendix B: Troubleshooting Guide

### Issue 1: K6 Test Still Fails with Authentication Error
**Symptoms**: K6 test returns "Authentication required" even with dev profile active

**Possible Causes**:
1. Spring Boot not started with dev profile
2. application-dev.properties not loaded
3. Property value incorrectly set

**Resolution Steps**:
```bash
# 1. Stop Spring Boot
# 2. Verify application-dev.properties exists and has correct property
grep "security.context.validator.enabled" orchestration-layer/src/main/resources/application-dev.properties
# Expected: security.context.validator.enabled=false

# 3. Start with dev profile explicitly
mvn spring-boot:run -pl orchestration-layer -Dspring-boot.run.profiles=dev

# 4. Check startup logs for "DEVELOPMENT MODE"
# If you see "PRODUCTION MODE", the property is not being loaded correctly
```

---

### Issue 2: Startup Logs Show Production Mode Instead of Development Mode
**Symptoms**: Logs show "PRODUCTION MODE" even when starting with dev profile

**Possible Causes**:
1. Profile not activated correctly
2. application-dev.properties not in correct location
3. Syntax error in properties file

**Resolution Steps**:
```bash
# 1. Verify file location
ls -la orchestration-layer/src/main/resources/application-dev.properties

# 2. Verify property syntax (no spaces around =)
cat orchestration-layer/src/main/resources/application-dev.properties | grep security.context

# 3. Start with explicit profile and verbose logging
mvn spring-boot:run -pl orchestration-layer -Dspring-boot.run.profiles=dev -Ddebug

# 4. Check Spring Boot logs for profile activation:
# "The following profiles are active: dev"
```

---

### Issue 3: Business Validations Not Working
**Symptoms**: Requests succeed even with invalid data (wrong user, invalid taxonomy, etc.)

**Possible Causes**:
1. Validation chain not properly configured
2. Validators not registered as Spring beans
3. ValidationChainConfig modification error

**Resolution Steps**:
```bash
# 1. Check compilation errors
mvn clean compile -pl internal-layer/question-bank

# 2. Run unit tests to verify validators
mvn test -pl internal-layer/question-bank -Dtest=ValidationChainConfigTest

# 3. Check Spring Boot logs for validator bean registration
# Look for:
# "Creating shared instance of singleton bean 'questionBankOwnershipValidator'"
# "Creating shared instance of singleton bean 'taxonomyReferenceValidator'"
# "Creating shared instance of singleton bean 'questionDataIntegrityValidator'"

# 4. Test with intentionally invalid data (see Phase 4 tests)
```

---

### Issue 4: Property Not Being Injected (@Value)
**Symptoms**: NullPointerException or default value always used

**Possible Causes**:
1. @Value annotation syntax error
2. Property name mismatch
3. Field not properly initialized

**Resolution Steps**:
```bash
# 1. Verify @Value annotation syntax in ValidationChainConfig.java
# Should be: @Value("${security.context.validator.enabled:true}")

# 2. Verify property name matches exactly in application*.properties
grep -r "security.context.validator.enabled" orchestration-layer/src/main/resources/

# 3. Add debug logging in ValidationChainConfig constructor
@PostConstruct
public void init() {
    logger.info("🔍 DEBUG: securityContextValidatorEnabled = {}", securityContextValidatorEnabled);
}

# 4. Restart and check logs for debug output
```

---

## Appendix C: Related Documentation

- **design.md**: Comprehensive technical design document
- **API-CONTRACT.md**: API contract for upsert question endpoint
- **CLAUDE.md**: Project architecture and development guidelines
- **ValidationChainConfig.java**: Validation chain configuration implementation
- **application.properties**: Production configuration
- **application-dev.properties**: Development configuration

---

## Document Changelog

| Date | Version | Changes |
|------|---------|---------|
| 2025-10-10 | 1.0 | Initial roadmap with property-based approach |

---

**End of Roadmap**
