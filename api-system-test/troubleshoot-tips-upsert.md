# Troubleshooting Guide: Upsert Question with Taxonomy API

## Overview
This guide documents the troubleshooting process and solutions for fixing the upsert question with taxonomy API system tests. The k6 test suite initially had 4 failures out of 42 checks (90.47% pass rate), which were resolved to achieve 100% pass rate.

## Test Failures Encountered

### 1. Upsert Operation Not Working (Second Call Returns "created" Instead of "updated")

#### Problem
- **Test**: Happy Path - Upsert (Update Existing Question)
- **Expected**: Second call with same `source_question_id` should return `operation: "updated"`
- **Actual**: Second call returns `operation: "created"` with status 200
- **Impact**: Upsert functionality was creating duplicates instead of updating existing questions

#### Root Cause
The `MongoQuestionRepository.upsertBySourceQuestionId()` method was preserving the MongoDB `_id` when finding existing documents but NOT preserving the `createdAt` timestamp. The `QuestionApplicationService.determineOperation()` method compares `createdAt` vs `updatedAt` to determine if the operation was "created" or "updated". Since both timestamps were being reset to the current time, they were always equal, resulting in "created" being returned.

#### Solution
**File**: `internal-layer/question-bank/src/main/java/com/quizfun/questionbank/infrastructure/persistence/repositories/MongoQuestionRepository.java`

Modified the `upsertBySourceQuestionId()` method to preserve both `id` and `createdAt` from existing documents:

```java
QuestionDocument existing = mongoTemplate.findOne(query, QuestionDocument.class);
if (existing != null && existing.getId() != null) {
    // preserve _id and createdAt for update scenario
    try {
        java.lang.reflect.Field idField = com.quizfun.questionbank.domain.aggregates.QuestionAggregate.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(aggregate, existing.getId());

        // Preserve createdAt timestamp so update detection works correctly
        java.lang.reflect.Field createdAtField = com.quizfun.shared.domain.AggregateRoot.class.getDeclaredField("createdAt");
        createdAtField.setAccessible(true);
        createdAtField.set(aggregate, existing.toAggregate().getCreatedAt());
    } catch (Exception ignored) {}
}
```

#### Key Insight
When implementing upsert operations, always preserve immutable metadata (like creation timestamps) from the existing entity. Comparing `createdAt` vs `updatedAt` is a simple but effective way to determine if an entity was created or updated.

---

### 2. Invalid Question Bank Returns 400 Instead of 422

#### Problem
- **Test**: Unhappy Path - Invalid Question Bank (Non-existent)
- **Expected**: Status 422 (Unprocessable Entity) with business rule violation error
- **Actual**: Status 400 (Bad Request) with message: `"User 1760085803933 doesn't own question bank 9999999999999"`
- **Impact**: HTTP status code doesn't properly distinguish business rule violations from validation errors

#### Root Cause
The controller's `createErrorResponse()` method checks if the error message **starts with** specific error codes to map to the correct HTTP status:

```java
if (error.startsWith("UNAUTHORIZED_ACCESS") || error.startsWith("QUESTION_BANK_NOT_FOUND")) {
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(result);
}
```

However, the `QuestionBankOwnershipValidator` was returning error messages without the error code prefix, so the controller defaulted to 400 (Bad Request).

#### Solution
**File**: `internal-layer/question-bank/src/main/java/com/quizfun/questionbank/application/validation/QuestionBankOwnershipValidator.java`

Added error code prefix to all error messages:

```java
return Result.failure(
    ValidationErrorCode.UNAUTHORIZED_ACCESS.name(),
    String.format("UNAUTHORIZED_ACCESS: User %d doesn't own question bank %d",
                upsertCommand.getUserId(), upsertCommand.getQuestionBankId())
);
```

And for inactive question banks:

```java
return Result.failure(
    ValidationErrorCode.UNAUTHORIZED_ACCESS.name(),
    String.format("UNAUTHORIZED_ACCESS: Question bank %d is not active for user %d",
                upsertCommand.getQuestionBankId(), upsertCommand.getUserId())
);
```

#### Key Insight
When using string-based error message matching in controllers, ensure validators consistently format error messages with error code prefixes. This enables proper HTTP status code mapping (400 for validation errors, 422 for business rule violations).

---

### 3. Invalid Taxonomy Reference Accepted (Returns 200 Instead of 422)

#### Problem
- **Test**: Unhappy Path - Invalid Taxonomy Reference
- **Expected**: Status 422 with `TAXONOMY_REFERENCE_NOT_FOUND` error
- **Actual**: Status 200 (success) - Question created with invalid taxonomy IDs
- **Impact**: System accepts invalid taxonomy references, violating data integrity

#### Root Cause
Similar to issue #2, the `TaxonomyReferenceValidator` was not including the error code prefix in error messages, causing the controller to not recognize the error and default to treating it as success.

#### Solution
**File**: `internal-layer/question-bank/src/main/java/com/quizfun/questionbank/application/validation/TaxonomyReferenceValidator.java`

Added error code prefix to taxonomy validation error messages:

```java
String invalidReferencesMessage;
if (invalidReferencesResult.isSuccess() && !invalidReferencesResult.getValue().isEmpty()) {
    List<String> invalidRefs = invalidReferencesResult.getValue();
    invalidReferencesMessage = String.format("TAXONOMY_REFERENCE_NOT_FOUND: Invalid taxonomy references found: %s",
        String.join(", ", invalidRefs));
} else {
    invalidReferencesMessage = "TAXONOMY_REFERENCE_NOT_FOUND: Some taxonomy references are invalid";
}

return Result.failure(
    ValidationErrorCode.TAXONOMY_REFERENCE_NOT_FOUND.name(),
    invalidReferencesMessage
);
```

#### Key Insight
The taxonomy validation logic in `MongoTaxonomySetRepository` was already correct and properly checking taxonomy existence. The issue was purely in message formatting for controller error mapping.

---

## Development Environment Challenges

### Challenge: Spring Boot DevTools Caching Old Classes

#### Problem
After making code changes to validators in the `internal-layer/question-bank` module, the Spring Boot application continued to use old class versions even after recompilation. Error messages remained unchanged despite verified source code modifications.

#### Root Cause Analysis
1. **Module Dependency Issue**: The `orchestration-layer` module depends on `question-bank` module via Maven dependency
2. **DevTools Limitation**: Spring Boot DevTools only auto-reloads classes from the current module (`orchestration-layer/target/classes`), not from dependency JARs in `.m2/repository`
3. **Stale Process**: Old Spring Boot processes (started at different times) were still running and serving requests

#### Solution Steps

**Step 1**: Verify code changes in source files
```bash
grep -A 3 "doesn't own question bank" internal-layer/question-bank/src/main/java/com/quizfun/questionbank/application/validation/QuestionBankOwnershipValidator.java
```

**Step 2**: Check compiled bytecode
```bash
find internal-layer/question-bank/target/classes -name "QuestionBankOwnershipValidator.class" -exec javap -c {} \; | grep -A 5 "UNAUTHORIZED_ACCESS"
```

**Step 3**: Install updated module to local Maven repository
```bash
mvn install -pl internal-layer/question-bank -DskipTests -q
```

**Step 4**: Kill ALL Spring Boot processes (important!)
```bash
ps aux | grep "[O]rchestrationLayerApplication" | awk '{print $2}' | xargs -r kill -9
```

**Step 5**: Start fresh Spring Boot instance
```bash
mvn spring-boot:run -pl orchestration-layer -Dspring-boot.run.profiles=dev &
```

#### Key Lessons
1. **Always check for stale processes**: Use `ps aux | grep OrchestrationLayerApplication` to find ALL running instances
2. **Module changes require Maven install**: When modifying dependency modules, run `mvn install` to update local repository
3. **Verify bytecode**: Use `javap -c` to confirm compiled classes contain expected changes
4. **Clean restart**: Kill all processes and start fresh when dependency modules are updated

---

## Testing Best Practices

### K6 System Test Execution

**Before Running Tests**:
1. Ensure MongoDB is running and accessible
2. Clean test data if needed: `db.questions.deleteMany({}); db.question_taxonomy_relationships.deleteMany({});`
3. Verify Spring Boot application is running: `curl http://localhost:8765/actuator/health`
4. Check no stale Spring Boot processes are running

**Running Tests**:
```bash
k6 run api-system-test/test-upsert-question-with-taxonomy.js
```

**Expected Results**:
- Total checks: 42
- Success rate: 100%
- All happy path scenarios: ✅
- All unhappy path scenarios: ✅

### Debugging Failed Tests

**Step 1**: Check specific test output
```bash
k6 run api-system-test/test-upsert-question-with-taxonomy.js 2>&1 | grep -A 2 "❌"
```

**Step 2**: Verify Spring Boot logs
```bash
# Check for validation errors
tail -f orchestration-layer/logs/application.log | grep -i "validation\|error"
```

**Step 3**: Test individual endpoints
```bash
# Test invalid question bank
curl -X POST http://localhost:8765/api/users/1760085803933/questionbanks/9999999999999/questions \
  -H "Content-Type: application/json" \
  -d '{"source_question_id":"test-uuid", ...}'
```

---

## Summary of Fixes

| Issue | File Modified | Change Type | Result |
|-------|--------------|-------------|--------|
| Upsert returns "created" | `MongoQuestionRepository.java` | Preserve `createdAt` timestamp | ✅ Returns "updated" correctly |
| Invalid bank returns 400 | `QuestionBankOwnershipValidator.java` | Add error code prefix | ✅ Returns 422 status |
| Invalid taxonomy returns 200 | `TaxonomyReferenceValidator.java` | Add error code prefix | ✅ Returns 422 status |

**Final Test Results**: 42/42 checks passed (100% success rate)

---

## Architecture Insights

### Error Handling Pattern

The system uses a two-layer error handling approach:

1. **Domain Layer** (`Result<T>` with error codes):
   ```java
   Result.failure(errorCode, errorMessage)
   ```

2. **Controller Layer** (HTTP status mapping):
   ```java
   if (error.startsWith("UNAUTHORIZED_ACCESS")) {
       return ResponseEntity.status(422).body(result);
   }
   ```

**Critical Pattern**: Error messages MUST start with the error code for proper HTTP status mapping.

### CQRS and Hexagonal Architecture Benefits

- **Separation of Concerns**: Validators in application layer, domain logic in aggregates, persistence in infrastructure
- **Testability**: Each layer can be tested independently
- **Maintainability**: Clear boundaries make issues easier to locate and fix

---

## Quick Reference Commands

```bash
# Build specific module
mvn clean compile -pl internal-layer/question-bank -q

# Install module to local repository
mvn install -pl internal-layer/question-bank -DskipTests

# Run Spring Boot with dev profile
mvn spring-boot:run -pl orchestration-layer -Dspring-boot.run.profiles=dev

# Kill all Spring Boot instances
ps aux | grep "[O]rchestrationLayerApplication" | awk '{print $2}' | xargs -r kill -9

# Run k6 tests
k6 run api-system-test/test-upsert-question-with-taxonomy.js

# Check application health
curl http://localhost:8765/actuator/health
```

---

**Document Version**: 1.0
**Last Updated**: 2025-10-11
**Test Success Rate**: 100% (42/42 checks passing)
