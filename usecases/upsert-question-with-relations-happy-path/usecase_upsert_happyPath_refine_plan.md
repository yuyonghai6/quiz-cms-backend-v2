# Use Case Refinement Plan: Upsert Question with Taxonomies (Happy Path)

## Executive Summary

This document presents a comprehensive refinement plan for the "Upsert Question with Taxonomies Happy Path" use case. The current document has good technical architecture understanding but needs clearer business logic definition, API specification consistency, and better separation of concerns.

## Issues Identified & Solutions

### 1. Use Case Clarity Enhancement

**Issue**: The document lacks clear success criteria and business rules definition.

**Solution**: Add structured use case definition section
```markdown
## Use Case Definition

### Primary Goal
Enable content creators to create or update questions with complex taxonomy relationships in their question banks.

### Primary Actor
- Content creator/educator with question bank access

### Preconditions
- User is authenticated and authorized (can disable and by pass this part in spring security at this moment)
- Target question bank exists and belongs to user (need test data loader to achieve it at section below)
- Request contains structurally valid JSON

### Success Criteria
- Question is created (if source_question_id doesn't exist) or updated (if exists)
- All taxonomy relationships are properly established
- Response contains question ID and operation type (created/updated)

### Business Rules
- **Upsert Logic**: Use `source_question_id` to determine INSERT vs UPDATE
- **Taxonomy Validation**: All referenced taxonomy IDs must exist in user's taxonomy_sets
- **Bank Ownership**: User can only modify questions in their own question banks
- **Data Integrity**: Question type must match the provided type-specific data (mcq_data, essay_data, etc.)
```

**Your Feedback Needed**:
- Do these business rules align with your requirements? yes
- Are there additional validation rules we should include? no

---

### 2. API Specification Consistency Fix

**Issue**: Inconsistency between URL path parameters and request JSON payload.

**Solution**: Clarify API contract and data flow
```markdown
## API Specification Refinement

### Current Inconsistency
- Endpoint: `POST /api/users/{userId}/questionbanks/{questionbankId}/questions`
- Request JSON contains: `"user_id": 3, "question_bank_id": 123`

### Proposed Solution
**Option A: Use Path Parameters (Recommended)**
- Remove `user_id` and `question_bank_id` from request JSON (I did it myself just now)
- Controller extracts from path: `@PathVariable Long userId, @PathVariable Long questionbankId`
- Inject these values when creating the command object

**Option B: Keep JSON Fields**
- Validate that path parameters match JSON values
- Use JSON values as primary source

### Recommended Implementation (Option A)
```java
@PostMapping("/api/users/{userId}/questionbanks/{questionbankId}/questions")
public ResponseEntity<?> upsertQuestion(
    @PathVariable Long userId,
    @PathVariable Long questionbankId,
    @RequestBody UpsertQuestionRequestJson request) {

    var command = new UpsertQuestionCommand(userId, questionbankId, request);
    return mediator.send(command);
}
```
```

**Your Feedback Needed**:
- Which option do you prefer: Path parameters (A) or JSON validation (B)? A
- Should we maintain both for backward compatibility? Option A only

---

### 3. Aggregate Interaction Clarification

**Issue**: Four aggregates mentioned but their coordination unclear.

**Solution**: Define clear aggregate responsibilities and interaction flow
```markdown
## Aggregate Coordination Strategy

### Primary Aggregate: QuestionAggregate
- **Responsibility**: Question content lifecycle (CRUD)
- **Business Rules**: Question validation, content integrity
- **Repository**: QuestionRepository (questions collection)

### Supporting Aggregates & Their Roles:

#### TaxonomySetAggregate
- **Responsibility**: Taxonomy reference validation
- **Operation**: Verify all taxonomy IDs exist in user's taxonomy_sets
- **Repository**: TaxonomySetRepository (taxonomy_sets collection)

#### QuestionTaxonomyRelationshipAggregate
- **Responsibility**: Manage many-to-many relationships
- **Operations**: Create/update/delete taxonomy-question associations
- **Repository**: QuestionTaxonomyRelationshipRepository (question_taxonomy_relationships collection)

#### QuestionBanksPerUserAggregate
- **Responsibility**: Question bank ownership validation
- **Operation**: Verify user owns the target question bank
- **Repository**: QuestionBanksPerUserRepository (question_banks_per_user collection)

### Coordination Flow in Application Service
```java
public class UpsertQuestionApplicationService {

    public Result<QuestionDto> upsertQuestion(UpsertQuestionCommand command) {
        // 1. Validate question bank ownership, I prefer clain of responsibility pattern to achieve it, chain of validator class
        //wrong way:var bankValidation = questionBanksPerUserRepo.validateOwnership(
            //command.getUserId(), command.getQuestionBankId());

        // 2. Validate taxonomy references, I prefer clain of responsibility pattern to achieve it,chain of validator class
        //wrong way: var taxonomyValidation = taxonomySetRepo.validateTaxonomyReferences(
            //command.getUserId(), command.getQuestionBankId(), command.getTaxonomyIds());

        // 3. Perform question upsert
        // wrong way: var questionResult = questionRepo.upsertBySourceId(command.toQuestionAggregate());
        //right way: please instantiate QuestionAggregate object, QuestionAggregate need to extends AggregateRoot.java which is inside internal-service submodule shared dir,then store the QuestionAggregate object to mongodb.

        // 4. Update taxonomy relationships
        // please instantiate QuestionTaxonomyRelationshipAggregate object for this
          // this part AI agent please add code to do insert multiple documents to mongodb: for each question to every single taxonomy mapping, add one document. therofore, you likely need to add multiple documents of relationship to this question_taxonomy_relationships collection, even it is for one question insertion operation.

        return Result.success(questionResult);
    }
}
```
note: because this above UpsertQuestionApplicationService update 2 collection at a same time, please use transaction to make sure ACID
questionBanksPerUserRepo is a implementation of port outs interface, remember to code the interface file, and inject it using spring IOC using constructor injection
```

**Your Feedback Needed**:
- Does this coordination approach align with your DDD vision? yes
- Should any aggregate be the "root" that coordinates others? yes, AggregateRoot.java file is needed

---

### 4. Persistence Operations Clarification

**Issue**: Incomplete list of affected collections.

**Solution**: Complete persistence operation mapping
```markdown
## Complete Persistence Operations

### Collections Affected (Corrected List)

#### Primary Operations:
1. **questions** collection
   - **Upsert**: Based on (user_id, question_bank_id, source_question_id)
   - **Operation**: Create new or update existing question document

2. **question_taxonomy_relationships** collection
   - **Delete**: Remove existing relationships for the question
   - **Insert**: Create new relationship documents for each taxonomy association

#### Secondary Operations:
3. **taxonomy_sets** collection
   - **Read**: Validate taxonomy references exist
   - **Potential Upsert**: If taxonomy references don't exist, should we create them or fail?

4. **question_banks_per_user** collection
   - **Read**: Validate user owns the question bank
   - **No Modification**: Read-only for ownership validation

### Repository Method Signatures
```java
// QuestionRepository
Question upsertBySourceQuestionId(Long userId, Long questionBankId, String sourceQuestionId, QuestionAggregate question);

// QuestionTaxonomyRelationshipRepository
void replaceRelationshipsForQuestion(ObjectId questionId, List<TaxonomyRelationship> relationships);

// TaxonomySetRepository
boolean validateTaxonomyReferences(Long userId, Long questionBankId, List<String> taxonomyIds);

// QuestionBanksPerUserRepository
boolean validateQuestionBankOwnership(Long userId, Long questionBankId);
```
```

**Your Feedback Needed**:
- Should we auto-create missing taxonomy references or fail validation? fail validation
- Are there other collections that should be affected? no

---

### 5. Test Data Pre-loading Specification

**Issue**: Vague test data requirements.

**Solution**: Detailed test data structure and loading strategy
```markdown
## Test Data Loading Strategy

### Test Data Structure
```
internal-layer/
  shared/
    src/test/resources/
      test-data/
        question-banks-per-user.json
        taxonomy-sets.json
        existing-questions.json
        existing-question-taxonomy-relationships.json
    src/test/java/
      com/quizfun/shared/testdata/
        TestDataLoader.java
        QuestionBankTestDataLoader.java
```

### Required Test Data Files

#### 1. question-banks-per-user.json
```json
{
  "_id": {"$oid": "507f1f77bcf86cd799439011"},
  "user_id": 3,
  "default_question_bank_id": 123,
  "question_banks": [
    {
      "bank_id": 123,
      "name": "JavaScript Fundamentals Test Bank",
      "description": "Test question bank for JavaScript questions",
      "is_active": true,
      "created_at": {"$date": "2025-09-13T10:00:00Z"},
      "updated_at": {"$date": "2025-09-13T10:00:00Z"}
    }
  ],
  "created_at": {"$date": "2025-09-13T10:00:00Z"},
  "updated_at": {"$date": "2025-09-13T10:00:00Z"}
}
```

#### 2. taxonomy-sets.json
```json
{
  "_id": {"$oid": "507f1f77bcf86cd799439012"},
  "user_id": 3,
  "question_bank_id": 123,
  "categories": {
    "level_1": {"id": "tech", "name": "Technology", "slug": "technology", "parent_id": null},
    "level_2": {"id": "prog", "name": "Programming", "slug": "programming", "parent_id": "tech"},
    "level_3": {"id": "web_dev", "name": "Web Development", "slug": "web-development", "parent_id": "prog"},
    "level_4": {"id": "javascript", "name": "JavaScript", "slug": "javascript", "parent_id": "web_dev"}
  },
  "tags": [
    {"id": "js-arrays", "name": "javascript", "color": "#f7df1e"},
    {"id": "array-methods", "name": "arrays", "color": "#61dafb"},
    {"id": "methods", "name": "methods", "color": "#764abc"},
    {"id": "beginner", "name": "beginner-friendly", "color": "#28a745"}
  ],
  "quizzes": [
    {"quiz_id": 101, "quiz_name": "JavaScript Fundamentals Quiz", "quiz_slug": "js-fundamentals"},
    {"quiz_id": 205, "quiz_name": "Array Methods Mastery", "quiz_slug": "array-methods-mastery"}
  ],
  "current_difficulty_level": {
    "level": "easy",
    "numeric_value": 1,
    "description": "Suitable for beginners"
  },
  "created_at": {"$date": "2025-09-13T10:00:00Z"},
  "updated_at": {"$date": "2025-09-13T10:00:00Z"}
}
```

#### 3. existing-questions.json (for update test scenarios)
```json
{
  "_id": {"$oid": "507f1f77bcf86cd799439013"},
  "user_id": 3,
  "question_bank_id": 123,
  "source_question_id": "existing-question-for-update-test",
  "question_type": "mcq",
  "title": "Existing Question Title",
  "content": "<p>This question exists for update testing</p>",
  "status": "draft",
  "created_at": {"$date": "2025-09-13T09:00:00Z"},
  "updated_at": {"$date": "2025-09-13T09:00:00Z"}
}
```
#### 4. existing-question-taxonomy-relationships.json
todo: AI agent, please generate for me


### TestDataLoader Implementation
```java
@Component
public class QuestionBankTestDataLoader {

    @Autowired
    private MongoTemplate mongoTemplate;

    public void loadTestData() {
        loadQuestionBanksPerUser();
        loadTaxonomySets();
        loadExistingQuestions();
        loadExistingQuestionTaxonomyRelationship();
    }

    private void loadQuestionBanksPerUser() {
        // Load from question-banks-per-user.json
    }

    private void loadTaxonomySets() {
        // Load from taxonomy-sets.json
    }

    private void loadExistingQuestions() {
        // Load from existing-questions.json
    }

    public void cleanupTestData() {
        // Remove test data after tests
    }

    public void loadExistingQuestionTaxonomyRelationship(){

    }
}
```
```

**Your Feedback Needed**:
- Should test data be loaded per test method or per test class? per test class
- Do you want test data as JSON files or Java objects? json

---

### 6. Error Handling Strategy

**Issue**: No error handling specification.

**Solution**: Comprehensive error handling framework
```markdown
## Error Handling Strategy

### Error Categories & HTTP Status Codes

#### 1. Validation Errors (400 Bad Request)
- **Invalid JSON structure**: Malformed request payload
- **Missing required fields**: source_question_id, question_type, etc.
- **Invalid question type**: Not in [mcq, true_false, essay]
- **Type-data mismatch**: question_type=mcq but no mcq_data provided

#### 2. Business Rule Violations (422 Unprocessable Entity)
- **Taxonomy reference not found**: Referenced taxonomy ID doesn't exist in user's taxonomy_sets
- **Question bank not found**: question_bank_id doesn't exist for user
- **Unauthorized access**: User doesn't own the question bank

#### 3. System Errors (500 Internal Server Error)
- **Database connection issues**
- **Unexpected exceptions during processing**

### Error Response Format
```json
{
  "status": "error",
  "error_code": "TAXONOMY_REFERENCE_NOT_FOUND",
  "message": "Taxonomy reference 'invalid-tag-id' not found in user's taxonomy set",
  "details": {
    "field": "taxonomy.tags[2].id",
    "value": "invalid-tag-id",
    "user_id": 3,
    "question_bank_id": 123
  },
  "timestamp": "2025-09-13T10:30:00Z"
}
```

### Error Code Enumeration
```java
public enum QuestionUpsertErrorCode {
    INVALID_JSON_STRUCTURE("Invalid JSON structure"),
    MISSING_REQUIRED_FIELD("Required field is missing"),
    INVALID_QUESTION_TYPE("Invalid question type"),
    TYPE_DATA_MISMATCH("Question type doesn't match provided data"),
    TAXONOMY_REFERENCE_NOT_FOUND("Taxonomy reference not found"),
    QUESTION_BANK_NOT_FOUND("Question bank not found"),
    UNAUTHORIZED_ACCESS("User doesn't own the question bank"),
    DUPLICATE_SOURCE_QUESTION_ID("Source question ID already exists"),
    DATABASE_ERROR("Database operation failed");
}
```
```

**Your Feedback Needed**:
- Should we fail fast on first error or collect all validation errors? fail fast
- Do you prefer detailed error messages or simple codes for security? detailed error messages

---

### 7. Implementation Phases

**Solution**: Structured implementation approach
```markdown
## Implementation Phases

### Phase 1: Foundation Setup (TDD)
**Duration**: 2-3 days
**Scope**: Basic infrastructure and core domain
- [ ] Create command/query objects and handlers
- [ ] Implement basic Question aggregate
- [ ] Setup repository interfaces
- [ ] Create unit tests for core domain logic
- [ ] Setup TestContainers MongoDB configuration

### Phase 2: Core Upsert Logic (TDD)
**Duration**: 3-4 days
**Scope**: Question CRUD operations
- [ ] Implement QuestionRepository with upsert logic
- [ ] Add validation for question data integrity
- [ ] Create integration tests for question operations
- [ ] Implement basic error handling

### Phase 3: Taxonomy Integration (TDD)
**Duration**: 4-5 days
**Scope**: Taxonomy validation and relationships
todo: redesign and implement validation cahin using chain of responsibility design pattern
- [ ] Implement TaxonomySetRepository for validation
- [ ] Implement QuestionTaxonomyRelationshipRepository
- [ ] Add taxonomy reference validation logic
- [ ] Create comprehensive relationship management tests

### Phase 4: End-to-End Integration (TDD)
**Duration**: 2-3 days
**Scope**: Controller and full flow testing
- [ ] Implement controller with proper error handling
- [ ] Create end-to-end integration tests
- [ ] Add comprehensive error scenario testing
- [ ] Performance testing with larger datasets

### Phase 5: Documentation & Cleanup
**Duration**: 1-2 days
**Scope**: Documentation and code cleanup
- [ ] Generate JaCoCo coverage reports (target: >70%)
- [ ] Add Allure test reporting
- [ ] Code review and refactoring
- [ ] Update architectural documentation
```

**Your Feedback Needed**:
- Does this phase breakdown match your timeline expectations? somewhat yes, need redesign according to what I modified above
- Should we implement all question types (mcq, essay, true_false) in parallel or sequentially? please use strategy design pattern

---

## Consolidated Recommendations

### 1. Document Structure Improvements
- **Separate**: Architecture overview from use case specification
- **Add**: Sequence diagrams for multi-module flow visualization
- **Include**: Business rule validation matrix
- **Specify**: Transaction boundaries and consistency requirements

### 2. Technical Architecture Refinements
- **Clarify**: Aggregate root designation and coordination
- **Define**: Repository method contracts upfront
- **Specify**: Event sourcing strategy (if applicable)
- **Document**: Error propagation between layers

### 3. Testing Strategy Enhancements
- **Test Pyramid**: Unit tests (domain) → Integration tests (repository) → E2E tests (controller)
- **Coverage Goals**: 70% minimum with JaCoCo, focus on business logic
- **Allure Reporting**: Detailed test scenarios with business context
- **Performance Testing**: Response time targets for upsert operations

## Next Steps

1. **Review this plan**: Please provide feedback on each section
2. **Prioritize changes**: Identify which issues are critical vs. nice-to-have
3. **Approve implementation phases**: Confirm timeline and scope expectations
4. **Begin Phase 1**: Start with foundation setup once plan is approved

---

## Your Response Template

Please respond with your feedback on each section:

```markdown
### 1. Use Case Clarity Enhancement
**Decision**: [YES/NO]
**Comments**: [Your feedback]

### 2. API Specification Consistency Fix
**Decision**: [YES/NO - Option A/B preference]
**Comments**: [Your feedback]

### 3. Aggregate Interaction Clarification
**Decision**: [YES/NO]
**Comments**: [Your feedback]

### 4. Persistence Operations Clarification
**Decision**: [YES/NO]
**Comments**: [Your feedback]

### 5. Test Data Pre-loading Specification
**Decision**: [YES/NO]
**Comments**: [Your feedback]

### 6. Error Handling Strategy
**Decision**: [YES/NO]
**Comments**: [Your feedback]

### 7. Implementation Phases
**Decision**: [YES/NO]
**Comments**: [Your feedback]
```