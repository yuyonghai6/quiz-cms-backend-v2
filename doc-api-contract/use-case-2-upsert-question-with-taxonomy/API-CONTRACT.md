# API Contract: Upsert Question with Taxonomy Relationships

## Overview

### Use Case Description
This endpoint enables content creators and educators to create or update questions with complex taxonomy relationships in their question banks through a single HTTP operation. The operation uses the `source_question_id` to determine whether to INSERT (create new) or UPDATE (modify existing) a question.

### Business Purpose
- Enable efficient question management through upsert pattern (single operation for create/update)
- Establish many-to-many relationships between questions and taxonomies
- Ensure data integrity through validation chains (ownership, taxonomy references, data type matching)
- Support multiple question types (MCQ, True/False, Essay) via strategy pattern

### Primary Actors
- **Content Creator/Educator**: Creates and manages questions in their question banks
- **Quiz CMS System**: Validates, processes, and persists question data

### Architecture Pattern
- **CQRS**: Command pattern with mediator routing
- **DDD**: Domain-driven design with multiple aggregates
- **Hexagonal**: Ports and adapters with validation chains
- **Strategy**: Question type-specific processing (McqQuestionStrategy, EssayQuestionStrategy, etc.)

---

## HTTP Endpoint Specification

### Base Information
```http
POST /api/users/{userId}/questionbanks/{questionbankId}/questions
Content-Type: application/json
```

### Path Parameters

| Parameter | Type | Required | Validation | Description |
|-----------|------|----------|------------|-------------|
| `userId` | Long | ✅ Yes | Must be positive (> 0) | User identifier in URL path - **MUST match JWT token userId** when security is enabled (currently bypassed for testing) |
| `questionbankId` | Long | ✅ Yes | Must be positive (> 0) | Target question bank identifier |

**Important Security Note**:
- **Production (Security Enabled)**: The `userId` in the path parameter MUST match the userId claim in the JWT token. The SecurityContextValidator enforces this match to prevent path parameter manipulation attacks. Mismatch results in 403 Forbidden.
- **Current Testing State**: Spring Security is currently disabled/bypassed. Path parameter `userId` is used directly without JWT validation. See "Security Validation" section below for details.

### Request Headers
```http
Content-Type: application/json
```

### Query Parameters
None

---

## Security Validation

### Authentication Architecture

This endpoint implements **multi-layered security validation** using the Chain of Responsibility pattern. Understanding this is critical for K6 test development.

#### Production Environment (Security Enabled)

When Spring Security is fully enabled, the system performs the following validations in order:

**1. JWT Token Extraction** (`SecurityContextValidator.java:76`)
```java
var authContext = SecurityContextHolder.getContext().getAuthentication();
```
- Extracts authentication context from Spring Security
- Requires valid JWT token in `Authorization: Bearer <token>` header

**2. JWT UserId Extraction** (`SecurityContextValidator.java:98`)
```java
Long tokenUserId = extractUserIdFromToken(jwtToken);
```
- Extracts userId from JWT token's `sub` (subject) claim
- Example JWT payload: `{ "sub": "999888777", ... }`

**3. Path Parameter Cross-Reference** (`SecurityContextValidator.java:108-123`)
```java
Long pathUserId = upsertCommand.getUserId();  // From URL path
if (!tokenUserId.equals(pathUserId)) {
    // CRITICAL SECURITY VIOLATION: Path parameter manipulation attack
    logSecurityEvent(SecurityEventType.PATH_PARAMETER_MANIPULATION, ...);
    return Result.failure("UNAUTHORIZED_ACCESS");
}
```
- Compares JWT userId with path parameter userId
- **Prevents path parameter manipulation attacks** (e.g., user 1002 trying to access `/api/users/1001/...`)

**4. Validation Chain Order** (`ValidationChainConfig.java:74-77`)
```java
securityValidator          // Step 1: JWT-path matching
    .setNext(ownershipValidator)      // Step 2: Question bank ownership
    .setNext(taxonomyValidator)       // Step 3: Taxonomy references
    .setNext(dataValidator);          // Step 4: Data integrity
```

#### Current Testing State (Security Bypassed)

**Status**: Spring Security is **currently disabled** for development and testing

**Implications**:
- No JWT token required in requests
- Path parameter `userId` is used directly without validation
- `SecurityContextValidator` may be disabled or returns success without checks
- Allows direct K6 testing without authentication infrastructure

**Documentation Reference**:
> "User is authenticated and authorized (can disable and bypass this part in Spring Security at this moment)"
> — `usecases/upsert-question-with-relations-happy-path/2.usecase-description-and-design.md:12`

### Attack Prevention Example

#### Blocked Attack (Production)
```http
POST /api/users/1001/questionbanks/2001/questions
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMDAyIiwibmFtZSI6IkF0dGFja2VyIn0...
Content-Type: application/json
```

**Result**:
- JWT token contains userId = 1002 (attacker's real identity)
- Path parameter contains userId = 1001 (victim's userId)
- SecurityContextValidator detects mismatch
- Response: **403 Forbidden** with `UNAUTHORIZED_ACCESS` error
- Security event logged with `PATH_PARAMETER_MANIPULATION` type

#### Valid Request (Production)
```http
POST /api/users/1002/questionbanks/5001/questions
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMDAyIiwibmFtZSI6IlVzZXIifQ...
Content-Type: application/json
```

**Result**:
- JWT token userId = 1002
- Path parameter userId = 1002
- Match successful → continues to next validator
- Ownership validator confirms user 1002 owns question bank 5001
- Request proceeds to business logic

### Security Event Logging

When path manipulation is detected, the system logs:

```java
SecurityEvent.builder()
    .type(PATH_PARAMETER_MANIPULATION)
    .userId(1002)  // Real attacker userId from JWT
    .severity(CRITICAL)
    .details({
        "tokenUserId": 1002,
        "pathUserId": 1001,
        "questionBankId": 2001,
        "attackPattern": "USER_ID_MISMATCH",
        "detectionMethod": "JWT_PATH_COMPARISON"
    })
    .build();
```

Logged asynchronously to MongoDB audit trail for compliance and threat analysis.

### Code References

- **Security Validator**: `internal-layer/question-bank/src/main/java/com/quizfun/questionbank/application/security/SecurityContextValidator.java`
- **Validation Chain Config**: `internal-layer/question-bank/src/main/java/com/quizfun/questionbank/infrastructure/configuration/ValidationChainConfig.java`
- **Unhappy Path Design**: `usecases/upsert-question-with-relations-unhappy-path/2.usecase-description-and-design-unhappy-path.md`

---

## Request Schema

### Request Body Structure
```json
{
  "source_question_id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "question_type": "mcq",
  "title": "JavaScript Array Methods",
  "content": "<p>Which method adds elements to the <strong>end</strong> of an array?</p>",

  "taxonomy": {
    "categories": {
      "level_1": { "id": "tech", "name": "Technology", "slug": "technology", "parent_id": null },
      "level_2": { "id": "prog", "name": "Programming", "slug": "programming", "parent_id": "tech" },
      "level_3": { "id": "web_dev", "name": "Web Development", "slug": "web-development", "parent_id": "prog" },
      "level_4": { "id": "javascript", "name": "JavaScript", "slug": "javascript", "parent_id": "web_dev" }
    },
    "tags": [
      { "id": "js-arrays", "name": "javascript", "color": "#f7df1e" },
      { "id": "array-methods", "name": "arrays", "color": "#61dafb" }
    ],
    "quizzes": [
      { "quiz_id": 101, "quiz_name": "JavaScript Fundamentals Quiz", "quiz_slug": "js-fundamentals" }
    ],
    "difficulty_level": { "level": "easy", "numeric_value": 1, "description": "Suitable for beginners" }
  },

  "points": 5,
  "solution_explanation": "<p>The <code>push()</code> method adds elements to the end of an array.</p>",
  "status": "draft",
  "display_order": 1,
  "attachments": [
    {
      "id": "att_001",
      "type": "image",
      "filename": "array_methods_diagram.png",
      "url": "/attachments/array_methods_diagram.png",
      "size": 245760,
      "mime_type": "image/png"
    }
  ],

  "mcq_data": {
    "options": [
      { "id": 1, "text": "push()", "is_correct": true, "explanation": "Correct!" },
      { "id": 2, "text": "pop()", "is_correct": false, "explanation": "Incorrect." }
    ],
    "shuffle_options": false,
    "allow_multiple_correct": false,
    "allow_partial_credit": false,
    "time_limit_seconds": 60
  },

  "question_settings": {
    "randomize_display": false,
    "show_explanation_immediately": true,
    "allow_review": true
  },

  "metadata": {
    "created_source": "manual",
    "last_modified": "2025-09-13T10:30:00Z",
    "version": 1,
    "author_id": 3
  }
}
```

### Core Field Specifications

| Field | Type | Required | Validation | Description |
|-------|------|----------|------------|-------------|
| `source_question_id` | String | ✅ Yes | UUID format recommended | External identifier for upsert logic - if exists, UPDATE; if new, CREATE |
| `question_type` | String | ✅ Yes | Enum: "mcq", "true_false", "essay" | Question type determines which type-specific data is required |
| `title` | String | ✅ Yes | Max 500 chars | Question title for display |
| `content` | String | ✅ Yes | HTML allowed | Question content/prompt |
| `taxonomy` | Object | ✅ Yes | Must contain valid taxonomy references | Taxonomy relationships for categorization |
| `status` | String | ✅ Yes | Enum: "draft", "published", "archived" | Question lifecycle status |
| `points` | Integer | ❌ No | >= 0 | Points awarded for correct answer |
| `solution_explanation` | String | ❌ No | HTML allowed | Explanation shown after answering |
| `display_order` | Integer | ❌ No | >= 0 | Order within question bank |
| `attachments` | Array | ❌ No | - | Media attachments (images, files) |
| `question_settings` | Object | ❌ No | - | Display and behavior settings |
| `metadata` | Object | ❌ No | - | Additional metadata for tracking |

### Taxonomy Field Specifications

| Field | Type | Required | Validation | Description |
|-------|------|----------|------------|-------------|
| `taxonomy.categories` | Object | ✅ Yes | At least level_1 required | Hierarchical categories (level_1 to level_4) |
| `taxonomy.categories.level_1` | Object | ✅ Yes | Must exist in user's taxonomy_sets | Top-level category (e.g., "Technology") |
| `taxonomy.categories.level_2` | Object | ❌ No | Must reference valid parent | Second-level category |
| `taxonomy.categories.level_3` | Object | ❌ No | Must reference valid parent | Third-level category |
| `taxonomy.categories.level_4` | Object | ❌ No | Must reference valid parent | Fourth-level category |
| `taxonomy.tags` | Array | ❌ No | Tag IDs must exist in taxonomy_sets | Tags for filtering and search |
| `taxonomy.quizzes` | Array | ❌ No | Quiz IDs must exist | Quizzes this question belongs to |
| `taxonomy.difficulty_level` | Object | ✅ Yes | Must exist in available_difficulty_levels | Question difficulty |

### Type-Specific Data Fields

#### MCQ Data (when question_type = "mcq")
```json
"mcq_data": {
  "options": [
    {
      "id": 1,
      "text": "Option text",
      "is_correct": true,
      "explanation": "Why this is correct/incorrect"
    }
  ],
  "shuffle_options": false,
  "allow_multiple_correct": false,
  "allow_partial_credit": false,
  "time_limit_seconds": 60
}
```

**Validation**: At least 2 options required, at least 1 must be correct

#### True/False Data (when question_type = "true_false")
```json
"true_false_data": {
  "statement": "The statement to evaluate",
  "correct_answer": true,
  "explanation": "Why this is true or false"
}
```

#### Essay Data (when question_type = "essay")
```json
"essay_data": {
  "prompt": "Essay question prompt",
  "min_words": 100,
  "max_words": 500,
  "rubric": [
    {
      "criteria": "Understanding of concept",
      "max_points": 25,
      "description": "Clear explanation of core concepts"
    }
  ],
  "allow_file_upload": false
}
```

### Example Requests

#### Minimal Request (MCQ Question)
```json
{
  "source_question_id": "simple-mcq-001",
  "question_type": "mcq",
  "title": "Simple MCQ",
  "content": "<p>What is 2+2?</p>",
  "status": "draft",
  "taxonomy": {
    "categories": {
      "level_1": {
        "id": "general",
        "name": "General",
        "slug": "general",
        "parent_id": null
      }
    },
    "difficulty_level": {
      "level": "easy",
      "numeric_value": 1,
      "description": "Suitable for beginners"
    }
  },
  "mcq_data": {
    "options": [
      { "id": 1, "text": "3", "is_correct": false },
      { "id": 2, "text": "4", "is_correct": true }
    ],
    "shuffle_options": false,
    "allow_multiple_correct": false
  }
}
```

#### Full Request (Complete Example)
See the demo file: `usecases/upsert-question-with-relations-happy-path/upsert-question-with-taxonomy-relationship.json`

---

## Response Specifications

### Success Response (200 OK)

#### Response Structure
```json
{
  "success": true,
  "message": "Question processed successfully",
  "data": {
    "questionId": "507f1f77bcf86cd799439014",
    "sourceQuestionId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
    "operation": "created",
    "taxonomyRelationshipsCount": 8
  }
}
```

#### Response Headers
```http
HTTP/1.1 200 OK
Content-Type: application/json
X-Operation: created
X-Question-Id: 507f1f77bcf86cd799439014
```

#### Field Descriptions

| Field | Type | Description |
|-------|------|-------------|
| `questionId` | String | MongoDB ObjectId of the question document |
| `sourceQuestionId` | String | External identifier provided in request |
| `operation` | String | Either "created" (new question) or "updated" (existing modified) |
| `taxonomyRelationshipsCount` | Integer | Number of taxonomy relationships established |

### Error Responses

#### 400 Bad Request - Missing Required Field
```json
{
  "success": false,
  "message": "MISSING_REQUIRED_FIELD: source_question_id is required",
  "data": null
}
```

**When**: Required field is missing from request

#### 400 Bad Request - Invalid Question Type
```json
{
  "success": false,
  "message": "INVALID_QUESTION_TYPE: Question type must be one of: mcq, true_false, essay",
  "data": null
}
```

**When**: question_type is not a valid enum value

#### 400 Bad Request - Type Data Mismatch
```json
{
  "success": false,
  "message": "TYPE_DATA_MISMATCH: MCQ question requires mcq_data to be present",
  "data": null
}
```

**When**: question_type is "mcq" but mcq_data is missing (or similar for other types)

#### 400 Bad Request - Validation Error
```json
{
  "success": false,
  "message": "VALIDATION_ERROR: Request validation failed: title: must not be blank, content: must not be blank",
  "data": null
}
```

**When**: Bean validation annotations fail (@NotBlank, @NotNull, etc.)

#### 400 Bad Request - Invalid Path Parameters
```json
{
  "success": false,
  "message": "CONSTRAINT_VIOLATION: Request constraint violation: userId must be positive",
  "data": null
}
```

**When**: Path parameters fail validation (userId or questionbankId <= 0)

#### 422 Unprocessable Entity - Unauthorized Access
```json
{
  "success": false,
  "message": "UNAUTHORIZED_ACCESS: User does not have access to question bank 123",
  "data": null
}
```

**When**: User does not own the specified question bank

#### 422 Unprocessable Entity - Question Bank Not Found
```json
{
  "success": false,
  "message": "QUESTION_BANK_NOT_FOUND: Question bank 123 does not exist for user 456",
  "data": null
}
```

**When**: Specified question bank doesn't exist in user's collection

#### 422 Unprocessable Entity - Invalid Taxonomy Reference
```json
{
  "success": false,
  "message": "TAXONOMY_REFERENCE_NOT_FOUND: Taxonomy ID 'javascript' not found in user's taxonomy set",
  "data": null
}
```

**When**: Referenced category, tag, quiz, or difficulty level doesn't exist in user's taxonomy_sets

#### 409 Conflict - Duplicate Source Question ID
```json
{
  "success": false,
  "message": "DUPLICATE_SOURCE_QUESTION_ID: Source question ID already exists in a different context",
  "data": null
}
```

**When**: source_question_id already exists but doesn't match upsert criteria

#### 500 Internal Server Error - Database Error
```json
{
  "success": false,
  "message": "DATABASE_ERROR: Failed to persist question data",
  "data": null
}
```

**When**: MongoDB operation fails

#### 500 Internal Server Error - Transaction Failed
```json
{
  "success": false,
  "message": "TRANSACTION_FAILED: Database transaction rollback occurred",
  "data": null
}
```

**When**: MongoDB transaction fails and rolls back

---

## HTTP Status Code Reference

| Status Code | Scenario | Error Code |
|-------------|----------|------------|
| 200 OK | Success - question created or updated | - |
| 400 Bad Request | Missing required field | MISSING_REQUIRED_FIELD |
| 400 Bad Request | Invalid question type | INVALID_QUESTION_TYPE |
| 400 Bad Request | Type data mismatch | TYPE_DATA_MISMATCH |
| 400 Bad Request | Validation error | VALIDATION_ERROR |
| 400 Bad Request | Invalid path parameters | CONSTRAINT_VIOLATION |
| 422 Unprocessable Entity | Unauthorized access | UNAUTHORIZED_ACCESS |
| 422 Unprocessable Entity | Question bank not found | QUESTION_BANK_NOT_FOUND |
| 422 Unprocessable Entity | Invalid taxonomy reference | TAXONOMY_REFERENCE_NOT_FOUND |
| 409 Conflict | Duplicate source question ID | DUPLICATE_SOURCE_QUESTION_ID |
| 500 Internal Server Error | Database error | DATABASE_ERROR |
| 500 Internal Server Error | Transaction failed | TRANSACTION_FAILED |

---

## K6 Test Considerations

### Authentication

#### Current Testing Environment (Security Bypassed)
- **Status**: Spring Security is **currently disabled** for development/testing
- **JWT Required**: ❌ NO - Direct API calls without `Authorization` header
- **UserId Source**: Path parameter used directly without JWT validation
- **SecurityContextValidator**: Disabled or bypassed in current configuration
- **Test Simplicity**: Can test with simple HTTP POST requests (see examples below)

#### Future Production Environment (Security Enabled)
When Spring Security is fully enabled, K6 tests will need to:

**1. Include JWT Token in Every Request**
```javascript
const params = {
  headers: {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer ' + jwtToken  // REQUIRED in production
  }
};
```

**2. Ensure JWT UserId Matches Path Parameter UserId**
```javascript
// JWT payload must have: { "sub": "999888777", ... }
const userId = 999888777;  // Must match JWT's 'sub' claim
const url = `http://localhost:8765/api/users/${userId}/questionbanks/${qbId}/questions`;
```

**3. Test Security Validation**
```javascript
// Scenario: Test path parameter manipulation detection
const jwtUserId = 999888777;  // Real user from JWT
const attackUserId = 111222333;  // Attacker tries different userId in path

// This should return 403 Forbidden with UNAUTHORIZED_ACCESS
const attackUrl = `http://localhost:8765/api/users/${attackUserId}/questionbanks/${qbId}/questions`;
const res = http.post(attackUrl, payload, {
  headers: {
    'Authorization': 'Bearer ' + jwtToken  // Contains userId 999888777
  }
});

check(res, {
  'attack blocked with 403': (r) => r.status === 403,
  'error is UNAUTHORIZED_ACCESS': (r) =>
    JSON.parse(r.body).message.includes('UNAUTHORIZED_ACCESS')
});
```

#### Transition Strategy for K6 Tests

**Phase 1: Current State (Security Disabled)**
- Write K6 tests without authentication
- Focus on business logic, validation, and data integrity
- Use path parameters directly

**Phase 2: Security Enabled (Future)**
- Add JWT token generation/management to K6 setup
- Update all K6 tests to include `Authorization` header
- Add security-specific test scenarios (path manipulation, token validation)
- Ensure JWT userId matches path userId in all requests

**Phase 3: Production Readiness**
- Test with real JWT tokens from authentication service
- Validate security event logging works correctly
- Load test with full security validation overhead

### Prerequisites
Before running K6 tests, ensure:

1. **MongoDB is running** (via Testcontainers or standalone)
2. **Application is started**: `mvn spring-boot:run -pl orchestration-layer`
3. **Port 8765 is accessible**
4. **Test data exists**:
   - User must have a default question bank (create via Use Case 1)
   - Taxonomy set must exist for the user and question bank
   - Test taxonomy IDs must match what's in the database

### Test Data Setup

#### Step 1: Create User's Default Question Bank
```javascript
// First, create a default question bank for the test user
export function setup() {
  const userId = 999888777; // Use a consistent test userId
  const questionbankId = createDefaultQuestionBank(userId);

  return { userId, questionbankId };
}

function createDefaultQuestionBank(userId) {
  const url = 'http://localhost:8765/api/users/default-question-bank';
  const payload = JSON.stringify({ userId: userId });
  const res = http.post(url, payload, {
    headers: { 'Content-Type': 'application/json' }
  });

  const data = JSON.parse(res.body).data;
  return data.questionBankId;
}
```

#### Step 2: Use Test Data in Questions
```javascript
export default function(data) {
  // Use the userId and questionbankId from setup
  const url = `http://localhost:8765/api/users/${data.userId}/questionbanks/${data.questionbankId}/questions`;

  const payload = JSON.stringify({
    source_question_id: `test-question-${__ITER}`,
    question_type: 'mcq',
    title: 'Test Question',
    content: '<p>Test content</p>',
    status: 'draft',
    taxonomy: {
      categories: {
        level_1: {
          id: 'general', // Use 'general' - this is created by default
          name: 'General',
          slug: 'general',
          parent_id: null
        }
      },
      difficulty_level: {
        level: 'easy', // Use 'easy' - this is created by default
        numeric_value: 1,
        description: 'Suitable for beginners'
      }
    },
    mcq_data: {
      options: [
        { id: 1, text: 'Option 1', is_correct: true },
        { id: 2, text: 'Option 2', is_correct: false }
      ],
      shuffle_options: false,
      allow_multiple_correct: false
    }
  });

  const res = http.post(url, payload, {
    headers: { 'Content-Type': 'application/json' }
  });

  check(res, {
    'status is 200': (r) => r.status === 200
  });
}
```

### Common K6 Test Scenarios

#### Scenario 1: Happy Path - Create New Question
```javascript
import http from 'k6/http';
import { check } from 'k6';

export default function() {
  const userId = 999888777;
  const questionbankId = 1730832000000000; // From setup
  const url = `http://localhost:8765/api/users/${userId}/questionbanks/${questionbankId}/questions`;

  const payload = JSON.stringify({
    source_question_id: 'k6-test-question-001',
    question_type: 'mcq',
    title: 'Test MCQ Question',
    content: '<p>What is K6 used for?</p>',
    status: 'draft',
    taxonomy: {
      categories: {
        level_1: {
          id: 'general',
          name: 'General',
          slug: 'general',
          parent_id: null
        }
      },
      tags: [
        { id: 'beginner', name: 'Beginner', color: '#28a745' }
      ],
      difficulty_level: {
        level: 'easy',
        numeric_value: 1,
        description: 'Suitable for beginners'
      }
    },
    mcq_data: {
      options: [
        { id: 1, text: 'Load testing', is_correct: true, explanation: 'Correct!' },
        { id: 2, text: 'Unit testing', is_correct: false, explanation: 'Incorrect' }
      ],
      shuffle_options: false,
      allow_multiple_correct: false
    }
  });

  const params = {
    headers: { 'Content-Type': 'application/json' }
  };

  const res = http.post(url, payload, params);

  check(res, {
    'status is 200': (r) => r.status === 200,
    'success is true': (r) => JSON.parse(r.body).success === true,
    'operation is created': (r) => JSON.parse(r.body).data.operation === 'created',
    'questionId exists': (r) => JSON.parse(r.body).data.questionId !== null,
    'header X-Operation is created': (r) => r.headers['X-Operation'] === 'created'
  });
}
```

#### Scenario 2: Upsert - Update Existing Question
```javascript
export default function() {
  const userId = 999888777;
  const questionbankId = 1730832000000000;
  const url = `http://localhost:8765/api/users/${userId}/questionbanks/${questionbankId}/questions`;

  const sourceQuestionId = 'k6-upsert-test';

  // First call - CREATE
  const createPayload = JSON.stringify({
    source_question_id: sourceQuestionId,
    question_type: 'mcq',
    title: 'Original Title',
    content: '<p>Original content</p>',
    status: 'draft',
    taxonomy: {
      categories: {
        level_1: { id: 'general', name: 'General', slug: 'general', parent_id: null }
      },
      difficulty_level: { level: 'easy', numeric_value: 1, description: 'Easy' }
    },
    mcq_data: {
      options: [
        { id: 1, text: 'A', is_correct: true },
        { id: 2, text: 'B', is_correct: false }
      ],
      shuffle_options: false,
      allow_multiple_correct: false
    }
  });

  const res1 = http.post(url, createPayload, {
    headers: { 'Content-Type': 'application/json' }
  });

  check(res1, {
    'first call - status is 200': (r) => r.status === 200,
    'first call - operation is created': (r) => JSON.parse(r.body).data.operation === 'created'
  });

  // Second call - UPDATE (same source_question_id)
  const updatePayload = JSON.stringify({
    source_question_id: sourceQuestionId,
    question_type: 'mcq',
    title: 'Updated Title', // Changed
    content: '<p>Updated content</p>', // Changed
    status: 'draft',
    taxonomy: {
      categories: {
        level_1: { id: 'general', name: 'General', slug: 'general', parent_id: null }
      },
      difficulty_level: { level: 'easy', numeric_value: 1, description: 'Easy' }
    },
    mcq_data: {
      options: [
        { id: 1, text: 'A', is_correct: true },
        { id: 2, text: 'B', is_correct: false }
      ],
      shuffle_options: false,
      allow_multiple_correct: false
    }
  });

  const res2 = http.post(url, updatePayload, {
    headers: { 'Content-Type': 'application/json' }
  });

  check(res2, {
    'second call - status is 200': (r) => r.status === 200,
    'second call - operation is updated': (r) => JSON.parse(r.body).data.operation === 'updated'
  });
}
```

#### Scenario 3: Validation Error - Missing Required Field
```javascript
export default function() {
  const userId = 999888777;
  const questionbankId = 1730832000000000;
  const url = `http://localhost:8765/api/users/${userId}/questionbanks/${questionbankId}/questions`;

  const payload = JSON.stringify({
    // Missing source_question_id
    question_type: 'mcq',
    title: 'Test',
    content: '<p>Test</p>',
    status: 'draft'
    // Missing taxonomy and mcq_data
  });

  const res = http.post(url, payload, {
    headers: { 'Content-Type': 'application/json' }
  });

  check(res, {
    'status is 400': (r) => r.status === 400,
    'success is false': (r) => JSON.parse(r.body).success === false,
    'error contains VALIDATION_ERROR': (r) => JSON.parse(r.body).message.includes('VALIDATION_ERROR')
  });
}
```

#### Scenario 4: Unauthorized Access - Invalid Question Bank
```javascript
export default function() {
  const userId = 999888777;
  const invalidQuestionbankId = 9999999999; // Doesn't exist
  const url = `http://localhost:8765/api/users/${userId}/questionbanks/${invalidQuestionbankId}/questions`;

  const payload = JSON.stringify({
    source_question_id: 'test',
    question_type: 'mcq',
    title: 'Test',
    content: '<p>Test</p>',
    status: 'draft',
    taxonomy: {
      categories: {
        level_1: { id: 'general', name: 'General', slug: 'general', parent_id: null }
      },
      difficulty_level: { level: 'easy', numeric_value: 1, description: 'Easy' }
    },
    mcq_data: {
      options: [
        { id: 1, text: 'A', is_correct: true },
        { id: 2, text: 'B', is_correct: false }
      ],
      shuffle_options: false,
      allow_multiple_correct: false
    }
  });

  const res = http.post(url, payload, {
    headers: { 'Content-Type': 'application/json' }
  });

  check(res, {
    'status is 422': (r) => r.status === 422,
    'success is false': (r) => JSON.parse(r.body).success === false,
    'error contains UNAUTHORIZED_ACCESS or QUESTION_BANK_NOT_FOUND': (r) => {
      const msg = JSON.parse(r.body).message;
      return msg.includes('UNAUTHORIZED_ACCESS') || msg.includes('QUESTION_BANK_NOT_FOUND');
    }
  });
}
```

#### Scenario 5: Invalid Taxonomy Reference
```javascript
export default function() {
  const userId = 999888777;
  const questionbankId = 1730832000000000;
  const url = `http://localhost:8765/api/users/${userId}/questionbanks/${questionbankId}/questions`;

  const payload = JSON.stringify({
    source_question_id: 'test-invalid-taxonomy',
    question_type: 'mcq',
    title: 'Test',
    content: '<p>Test</p>',
    status: 'draft',
    taxonomy: {
      categories: {
        level_1: {
          id: 'nonexistent-category', // This doesn't exist
          name: 'Nonexistent',
          slug: 'nonexistent',
          parent_id: null
        }
      },
      difficulty_level: { level: 'easy', numeric_value: 1, description: 'Easy' }
    },
    mcq_data: {
      options: [
        { id: 1, text: 'A', is_correct: true },
        { id: 2, text: 'B', is_correct: false }
      ],
      shuffle_options: false,
      allow_multiple_correct: false
    }
  });

  const res = http.post(url, payload, {
    headers: { 'Content-Type': 'application/json' }
  });

  check(res, {
    'status is 422': (r) => r.status === 422,
    'success is false': (r) => JSON.parse(r.body).success === false,
    'error contains TAXONOMY_REFERENCE_NOT_FOUND': (r) =>
      JSON.parse(r.body).message.includes('TAXONOMY_REFERENCE_NOT_FOUND')
  });
}
```

#### Scenario 6: Type Data Mismatch
```javascript
export default function() {
  const userId = 999888777;
  const questionbankId = 1730832000000000;
  const url = `http://localhost:8765/api/users/${userId}/questionbanks/${questionbankId}/questions`;

  const payload = JSON.stringify({
    source_question_id: 'test-type-mismatch',
    question_type: 'mcq', // Says MCQ
    title: 'Test',
    content: '<p>Test</p>',
    status: 'draft',
    taxonomy: {
      categories: {
        level_1: { id: 'general', name: 'General', slug: 'general', parent_id: null }
      },
      difficulty_level: { level: 'easy', numeric_value: 1, description: 'Easy' }
    },
    // But provides essay_data instead of mcq_data
    essay_data: {
      prompt: 'Write an essay',
      min_words: 100,
      max_words: 500
    }
  });

  const res = http.post(url, payload, {
    headers: { 'Content-Type': 'application/json' }
  });

  check(res, {
    'status is 400': (r) => r.status === 400,
    'success is false': (r) => JSON.parse(r.body).success === false,
    'error contains TYPE_DATA_MISMATCH': (r) =>
      JSON.parse(r.body).message.includes('TYPE_DATA_MISMATCH')
  });
}
```

#### Scenario 7: Load Test - Multiple Question Types
```javascript
import { check } from 'k6';

export let options = {
  stages: [
    { duration: '30s', target: 5 },
    { duration: '1m', target: 5 },
    { duration: '30s', target: 0 },
  ],
};

const questionTypes = ['mcq', 'true_false', 'essay'];

export default function() {
  const userId = 999888777;
  const questionbankId = 1730832000000000;
  const url = `http://localhost:8765/api/users/${userId}/questionbanks/${questionbankId}/questions`;

  // Rotate through question types
  const typeIndex = __ITER % questionTypes.length;
  const questionType = questionTypes[typeIndex];

  let typeSpecificData = {};
  if (questionType === 'mcq') {
    typeSpecificData.mcq_data = {
      options: [
        { id: 1, text: 'Option 1', is_correct: true },
        { id: 2, text: 'Option 2', is_correct: false }
      ],
      shuffle_options: false,
      allow_multiple_correct: false
    };
  } else if (questionType === 'true_false') {
    typeSpecificData.true_false_data = {
      statement: 'This is a true/false question',
      correct_answer: true,
      explanation: 'Explanation here'
    };
  } else {
    typeSpecificData.essay_data = {
      prompt: 'Write an essay',
      min_words: 100,
      max_words: 500,
      rubric: [
        { criteria: 'Content', max_points: 50, description: 'Quality of content' }
      ]
    };
  }

  const payload = JSON.stringify({
    source_question_id: `load-test-${questionType}-${__VU}-${__ITER}`,
    question_type: questionType,
    title: `Load Test ${questionType} Question`,
    content: '<p>Load test content</p>',
    status: 'draft',
    taxonomy: {
      categories: {
        level_1: { id: 'general', name: 'General', slug: 'general', parent_id: null }
      },
      difficulty_level: { level: 'easy', numeric_value: 1, description: 'Easy' }
    },
    ...typeSpecificData
  });

  const res = http.post(url, payload, {
    headers: { 'Content-Type': 'application/json' }
  });

  check(res, {
    'status is 200': (r) => r.status === 200,
    'operation is created': (r) => JSON.parse(r.body).data.operation === 'created',
    'response time < 1000ms': (r) => r.timings.duration < 1000
  });
}
```

#### Scenario 8: Security Testing - Path Parameter Manipulation (Future - When Security Enabled)
```javascript
import http from 'k6/http';
import { check } from 'k6';

// This scenario tests the SecurityContextValidator
// Only applicable when Spring Security is enabled

export function setup() {
  // Generate JWT tokens for two different users
  return {
    user1Token: generateJwtToken(999888777),  // Real user
    user2Token: generateJwtToken(111222333),  // Attacker
    user1Id: 999888777,
    user2Id: 111222333,
    questionbankId: 1730832000000000
  };
}

export default function(data) {
  const baseUrl = 'http://localhost:8765';

  // Test 1: Valid request (JWT userId matches path userId)
  const validUrl = `${baseUrl}/api/users/${data.user1Id}/questionbanks/${data.questionbankId}/questions`;
  const validPayload = createMinimalQuestionPayload('valid-test');

  const validRes = http.post(validUrl, validPayload, {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer ' + data.user1Token  // Matches path userId
    }
  });

  check(validRes, {
    'valid request - status is 200': (r) => r.status === 200,
    'valid request - success': (r) => JSON.parse(r.body).success === true
  });

  // Test 2: Attack attempt (JWT userId != path userId)
  const attackUrl = `${baseUrl}/api/users/${data.user1Id}/questionbanks/${data.questionbankId}/questions`;
  const attackPayload = createMinimalQuestionPayload('attack-test');

  const attackRes = http.post(attackUrl, attackPayload, {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer ' + data.user2Token  // MISMATCH: token for user 111222333, path for 999888777
    }
  });

  check(attackRes, {
    'attack blocked - status is 403': (r) => r.status === 403,
    'attack blocked - UNAUTHORIZED_ACCESS': (r) =>
      JSON.parse(r.body).message.includes('UNAUTHORIZED_ACCESS'),
    'attack blocked - success is false': (r) => JSON.parse(r.body).success === false
  });

  // Test 3: Verify security event was logged
  // (Would require separate API to query security audit logs)
}

function generateJwtToken(userId) {
  // In real tests, call your auth service to get a valid JWT
  // For now, this is a placeholder
  // JWT payload should have: { "sub": "<userId>", ... }
  return 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...';
}

function createMinimalQuestionPayload(sourceId) {
  return JSON.stringify({
    source_question_id: sourceId,
    question_type: 'mcq',
    title: 'Security Test Question',
    content: '<p>Test</p>',
    status: 'draft',
    taxonomy: {
      categories: {
        level_1: { id: 'general', name: 'General', slug: 'general', parent_id: null }
      },
      difficulty_level: { level: 'easy', numeric_value: 1, description: 'Easy' }
    },
    mcq_data: {
      options: [
        { id: 1, text: 'A', is_correct: true },
        { id: 2, text: 'B', is_correct: false }
      ],
      shuffle_options: false,
      allow_multiple_correct: false
    }
  });
}
```

**Expected Results When Security is Enabled:**
- ✅ Test 1: Valid request succeeds (200 OK)
- ✅ Test 2: Attack blocked (403 Forbidden with UNAUTHORIZED_ACCESS)
- ✅ Test 3: Security event logged to MongoDB audit trail

**Current State (Security Bypassed):**
- Both Test 1 and Test 2 would succeed (200 OK) since SecurityContextValidator is disabled
- Use this scenario to prepare for future security enablement

### Key Metrics to Track
- **Response time** (target: < 1000ms for complex operations)
- **Success rate** (should be 100% for valid requests)
- **Operation distribution** (created vs updated ratio)
- **Taxonomy relationship count** (verify all relationships are created)
- **Validation chain performance** (ownership, taxonomy, data integrity checks)

---

## Code References

### Controller
- **File**: `orchestration-layer/src/main/java/com/quizfun/orchestrationlayer/controllers/QuestionController.java`
- **Method**: `upsertQuestion` (line 62)
- **Important**: userId extracted from `@PathVariable` at line 63

### Test Files
- **Unit Test**: `orchestration-layer/src/test/java/com/quizfun/orchestrationlayer/controllers/QuestionControllerUnitTest.java`
- **Reference Test Method**: `shouldHandleSuccessfulQuestionCreationRequest()` (line 86)

### Documentation
- **Use Case Design**: `usecases/upsert-question-with-relations-happy-path/2.usecase-description-and-design.md`
- **Architecture Overview**: `usecases/upsert-question-with-relations-happy-path/1.architecture-overview-for-this-usecase.md`
- **Demo Request**: `usecases/upsert-question-with-relations-happy-path/upsert-question-with-taxonomy-relationship.json`

### Domain Components
- **Command**: `UpsertQuestionCommand`
- **Aggregates**: `QuestionAggregate`, `TaxonomySetAggregate`, `QuestionTaxonomyRelationshipAggregate`, `QuestionBanksPerUserAggregate`
- **Strategies**: `McqQuestionStrategy`, `TrueFalseQuestionStrategy`, `EssayQuestionStrategy`
- **Validators**: `QuestionBankOwnershipValidator`, `TaxonomyReferenceValidator`, `QuestionDataIntegrityValidator`

---

## Validation Chain Details

The system uses **Chain of Responsibility pattern** for validation. All validations must pass before question processing:

### Validation Order
1. **QuestionBankOwnershipValidator**: Verifies user owns the question bank
2. **TaxonomyReferenceValidator**: Verifies all taxonomy IDs exist in user's taxonomy_sets
3. **QuestionDataIntegrityValidator**: Verifies question type matches provided type-specific data

### Validation Failures Result In
- Early termination (fail-fast)
- 400 Bad Request or 422 Unprocessable Entity
- Detailed error message indicating which validation failed

---

## MongoDB Collections Modified

### questions
```javascript
{
  "_id": ObjectId("507f1f77bcf86cd799439014"),
  "user_id": 999888777,
  "question_bank_id": 1730832000000000,
  "source_question_id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "question_type": "mcq",
  "title": "JavaScript Array Methods",
  "content": "<p>Which method adds elements to the end of an array?</p>",
  "status": "draft",
  "points": 5,
  "solution_explanation": "<p>The push() method...</p>",
  "display_order": 1,
  "mcq_data": {
    "options": [...],
    "shuffle_options": false,
    "allow_multiple_correct": false
  },
  "created_at": ISODate("2025-10-09T10:30:00Z"),
  "updated_at": ISODate("2025-10-09T10:30:00Z")
}
```

### question_taxonomy_relationships
```javascript
// Multiple documents created (one per taxonomy relationship)
{
  "_id": ObjectId("507f1f77bcf86cd799439015"),
  "user_id": 999888777,
  "question_bank_id": 1730832000000000,
  "question_id": ObjectId("507f1f77bcf86cd799439014"),
  "taxonomy_type": "category",
  "taxonomy_id": "javascript",
  "created_at": ISODate("2025-10-09T10:30:00Z")
}
```

---

## Business Rules Summary

1. **Upsert Logic**: Use source_question_id to determine INSERT vs UPDATE
2. **Taxonomy Validation**: All referenced taxonomy IDs must exist in user's taxonomy_sets
3. **Bank Ownership**: User can only modify questions in their own question banks
4. **Data Integrity**: Question type must match provided type-specific data (mcq_data, essay_data, etc.)
5. **Transactional Consistency**: Question and relationships created/updated atomically
6. **Hierarchical Taxonomy**: Category hierarchy must be valid (level_2 requires level_1, etc.)
7. **Minimum Options**: MCQ questions require at least 2 options with at least 1 correct
