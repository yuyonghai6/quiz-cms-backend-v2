# API Contract: Create Default Question Bank for New User

## Overview

### Use Case Description
This endpoint enables external user management systems to trigger the automatic provisioning of a default question bank and associated taxonomy set when a new user is created in the system.

### Business Purpose
- Provide immediate question bank access for newly registered users
- Ensure consistent onboarding experience with pre-configured taxonomy
- Enable atomic creation of question bank and taxonomy structures via MongoDB transactions

### Primary Actors
- **External System**: User Management System (out of scope, triggers this API)
- **Internal System**: Quiz CMS System (handles question bank provisioning)

### Architecture Pattern
- **CQRS**: Command pattern with mediator routing
- **DDD**: Domain-driven design with aggregates (QuestionBanksPerUserAggregate, TaxonomySetAggregate)
- **Hexagonal**: Ports and adapters architecture

---

## HTTP Endpoint Specification

### Base Information
```http
POST /api/users/default-question-bank
Content-Type: application/json
```

### Request Headers
```http
Content-Type: application/json
X-User-Management-System: external-user-service (optional)
X-Request-ID: usr-req-550e8400-e29b-41d4-a716-446655440000 (optional)
```

### Path Parameters
None

### Query Parameters
None

---

## Request Schema

### Request Body Structure
```json
{
  "userId": Long (required),
  "userEmail": String (optional),
  "metadata": {
    "createdBy": String (optional),
    "createdAt": ISO-8601 String (optional),
    "requestId": String (optional)
  }
}
```

### Field Specifications

| Field | Type | Required | Validation | Description |
|-------|------|----------|------------|-------------|
| `userId` | Long | ✅ Yes | > 0, not null | Unique identifier for the user from external user management system |
| `userEmail` | String | ❌ No | Valid email format, max 255 chars | User's email address for audit purposes |
| `metadata` | Object | ❌ No | - | Additional metadata for tracking and auditing |
| `metadata.createdBy` | String | ❌ No | Max 100 chars | System or service that triggered the request |
| `metadata.createdAt` | String | ❌ No | ISO-8601 format | Timestamp when user was created in external system |
| `metadata.requestId` | String | ❌ No | Max 100 chars | Idempotency tracking identifier |

### Example Requests

#### Minimal Request (Required Fields Only)
```json
{
  "userId": 123456789
}
```

#### Full Request (All Fields)
```json
{
  "userId": 123456789,
  "userEmail": "john.doe@example.com",
  "metadata": {
    "createdBy": "user-management-system-v2",
    "createdAt": "2025-10-09T10:30:00Z",
    "requestId": "usr-req-550e8400-e29b-41d4-a716-446655440000"
  }
}
```

---

## Response Specifications

### Success Response (201 Created)

#### Response Structure
```json
{
  "success": true,
  "message": "Default question bank created successfully for user 123456789",
  "data": {
    "userId": 123456789,
    "questionBankId": 1730832000000000,
    "questionBankName": "Default Question Bank",
    "description": "Your default question bank for getting started with quiz creation",
    "active": true,
    "taxonomySetCreated": true,
    "availableTaxonomy": {
      "categories": [
        {
          "id": "general",
          "name": "General",
          "level": "level_1"
        }
      ],
      "tags": [
        {
          "id": "beginner",
          "name": "Beginner",
          "color": "#28a745"
        },
        {
          "id": "practice",
          "name": "Practice",
          "color": "#007bff"
        },
        {
          "id": "quick-test",
          "name": "Quick Test",
          "color": "#6f42c1"
        }
      ],
      "difficultyLevels": [
        {
          "level": "easy",
          "numericValue": 1,
          "description": "Suitable for beginners and initial learning"
        },
        {
          "level": "medium",
          "numericValue": 2,
          "description": "Intermediate knowledge required"
        },
        {
          "level": "hard",
          "numericValue": 3,
          "description": "Advanced understanding needed"
        }
      ]
    },
    "createdAt": "2025-10-09T10:30:15.123Z"
  }
}
```

#### Response Headers
```http
HTTP/1.1 201 Created
Content-Type: application/json
X-Question-Bank-ID: 1730832000000000
```

### Error Responses

**IMPORTANT**: Spring Boot validation errors (triggered by `@Valid` annotation) return a **different JSON structure** than application-level errors. These are intercepted by Spring before reaching the controller.

#### 400 Bad Request - Spring Validation Errors

**Applies to**: Missing userId, null userId, invalid userId (zero/negative), invalid email format

**Actual Response Structure**:
```json
{
  "timestamp": "2025-10-10T08:44:48.262+00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed for object='createDefaultQuestionBankRequestDto'. Error count: 1",
  "errors": [
    {
      "objectName": "createDefaultQuestionBankRequestDto",
      "field": "userId",
      "rejectedValue": null,
      "defaultMessage": "userId cannot be null",
      "code": "NotNull"
    }
  ],
  "path": "/api/users/default-question-bank"
}
```

**Examples by Scenario**:

**Missing or Null userId**:
- Field: `userId`
- Rejected Value: `null`
- Default Message: `"userId cannot be null"`
- Validation Code: `NotNull`

**Zero or Negative userId**:
- Field: `userId`
- Rejected Value: `0` or `-123456`
- Default Message: `"userId must be positive"`
- Validation Code: `Positive`

**Invalid Email Format**:
- Field: `userEmail`
- Rejected Value: `"invalid-email-format"`
- Default Message: `"userEmail must be a valid email address"`
- Validation Code: `Email`

#### 409 Conflict - User Already Exists
```json
{
  "success": false,
  "message": "DUPLICATE_USER: User 123456789 already has a default question bank",
  "data": null
}
```

**When**: User already has question banks in the system (not idempotent)

#### 500 Internal Server Error - Database Transaction Failed
```json
{
  "success": false,
  "message": "DATABASE_ERROR: Failed to create default question bank due to transaction failure",
  "data": null
}
```

**When**: MongoDB transaction fails (network issue, database unavailable, etc.)

#### 500 Internal Server Error - Template Processing Failed
```json
{
  "success": false,
  "message": "INTERNAL_ERROR: Failed to process document templates",
  "data": null
}
```

**When**: JSON template files cannot be loaded or parsed

---

## HTTP Status Code Reference

| Status Code | Scenario | Error Code |
|-------------|----------|------------|
| 201 Created | Success - question bank created | - |
| 400 Bad Request | Invalid userId (null, zero, negative) | INVALID_USER_ID |
| 400 Bad Request | Missing required field | MISSING_REQUIRED_FIELD |
| 400 Bad Request | Invalid email format | INVALID_EMAIL_FORMAT |
| 409 Conflict | User already has question bank | DUPLICATE_USER |
| 500 Internal Server Error | MongoDB transaction failed | TRANSACTION_FAILED |
| 500 Internal Server Error | Template processing error | TEMPLATE_PROCESSING_ERROR |
| 500 Internal Server Error | ID generation error | ID_GENERATION_ERROR |
| 500 Internal Server Error | Unexpected system error | INTERNAL_ERROR |

---

## Infrastructure Setup & Troubleshooting

### MongoDB Replica Set Configuration (CRITICAL)

**⚠️ IMPORTANT**: This API requires MongoDB running in **replica set mode** with **primary read preference** for transactions to work.

#### Common Error
```
DATABASE_ERROR: Failed to create default question bank - Read preference in a transaction must be primary
```

#### Root Cause
- MongoDB running as standalone instance (not replica set)
- Incorrect read preference configuration
- Hostname resolution issues between Spring Boot and MongoDB containers

#### Solution - application.properties Configuration
```properties
# CORRECT configuration for local development with Docker containers
spring.data.mongodb.uri=mongodb://root:PASSWORD@localhost:27017,localhost:27018/quizfun?replicaSet=rs0&authSource=admin&readPreference=primary&retryWrites=true&w=majority
spring.data.mongodb.read-preference=primary
```

**Key Points**:
- Use `localhost` (not container hostnames like `primary` or `secondary`) when running Spring Boot on host
- Ensure `replicaSet=rs0` is present in URI
- Set `readPreference=primary` for transaction support
- Add explicit `spring.data.mongodb.read-preference=primary` property

#### Verify MongoDB Replica Set
```bash
# Check if MongoDB containers are running
docker ps | grep mongo

# Expected output:
# mongodb_primary    (port 27017)
# mongodb_secondary  (port 27018)
# mongo_express      (port 8081)
```

### Response Field Naming (Jackson Convention)

**⚠️ CRITICAL**: The DTO getter `isActive()` is serialized as `"active"` in JSON, not `"isActive"`.

This is due to **Jackson's JavaBean naming conventions**:
- Boolean getter `isX()` → JSON field `"x"` (removes "is" prefix)
- Non-boolean getter `getX()` → JSON field `"x"`

**Actual Response**:
```json
{
  "data": {
    "active": true,  // NOT "isActive"
    ...
  }
}
```

**Test Assertion**:
```javascript
// CORRECT
check(res, {
  'active field is true': (r) => r.json().data.active === true
});

// INCORRECT
check(res, {
  'isActive is true': (r) => r.json().data.isActive === true  // FAILS
});
```

## K6 Test Considerations

### Authentication
- **Current State**: Authentication is **disabled/bypassed** for testing
- **No JWT Required**: Direct API calls without authentication headers
- **Future**: May require authentication tokens when Spring Security is enabled

### Prerequisites
Before running K6 tests, ensure:

1. **MongoDB replica set is running** (both primary and secondary containers)
2. **Spring Boot application.properties configured** with correct MongoDB URI
3. **Application is started**: `mvn spring-boot:run -pl orchestration-layer`
4. **Port 8765 is accessible**
5. **No existing user data** for test userId (or use unique userIds with `Date.now()`)

### Test Data Setup

#### Clean State Test
```javascript
// K6 test setup - ensure user doesn't exist
export function setup() {
  // Option 1: Use unique userId per test run
  const userId = Date.now(); // e.g., 1728471000000
  return { userId };
}
```

#### Test Idempotency
```javascript
// First call should return 201 Created
// Second call with same userId should return 409 Conflict
export default function(data) {
  let res1 = http.post(url, payload); // 201 Created
  check(res1, { 'status is 201': (r) => r.status === 201 });

  let res2 = http.post(url, payload); // 409 Conflict
  check(res2, { 'status is 409': (r) => r.status === 409 });
}
```

### Common K6 Test Scenarios

#### Scenario 1: Happy Path - Create Default Question Bank
```javascript
import http from 'k6/http';
import { check } from 'k6';

export default function() {
  const url = 'http://localhost:8765/api/users/default-question-bank';
  const payload = JSON.stringify({
    userId: 123456789,
    userEmail: 'test@example.com'
  });
  const params = {
    headers: { 'Content-Type': 'application/json' }
  };

  const res = http.post(url, payload, params);

  check(res, {
    'status is 201': (r) => r.status === 201,
    'success is true': (r) => JSON.parse(r.body).success === true,
    'questionBankId exists': (r) => JSON.parse(r.body).data.questionBankId > 0,
    'header X-Question-Bank-ID exists': (r) => r.headers['X-Question-Bank-Id'] !== undefined
  });
}
```

#### Scenario 2: Validation Error - Missing userId
```javascript
export default function() {
  const url = 'http://localhost:8765/api/users/default-question-bank';
  const payload = JSON.stringify({
    userEmail: 'test@example.com'
    // Missing userId
  });
  const params = {
    headers: { 'Content-Type': 'application/json' }
  };

  const res = http.post(url, payload, params);

  check(res, {
    'status is 400': (r) => r.status === 400,
    'error is Bad Request': (r) => JSON.parse(r.body).error === 'Bad Request',
    'response has errors array': (r) => {
      const body = JSON.parse(r.body);
      return body.errors && body.errors.length > 0;
    }
  });
}
```

**Note**: Spring validation errors return `{error, message, errors[]}` format, NOT `{success, message, data}` format.

#### Scenario 3: Duplicate User - 409 Conflict
```javascript
export default function() {
  const url = 'http://localhost:8765/api/users/default-question-bank';
  const payload = JSON.stringify({
    userId: 999999999 // Use a userId that already exists
  });
  const params = {
    headers: { 'Content-Type': 'application/json' }
  };

  const res = http.post(url, payload, params);

  check(res, {
    'status is 409': (r) => r.status === 409,
    'success is false': (r) => JSON.parse(r.body).success === false,
    'error contains DUPLICATE_USER': (r) => JSON.parse(r.body).message.includes('DUPLICATE_USER')
  });
}
```

#### Scenario 4: Load Test - Concurrent User Creation
```javascript
import { check } from 'k6';

export let options = {
  stages: [
    { duration: '30s', target: 10 },  // Ramp up to 10 VUs
    { duration: '1m', target: 10 },   // Stay at 10 VUs
    { duration: '30s', target: 0 },   // Ramp down
  ],
};

export default function() {
  const url = 'http://localhost:8765/api/users/default-question-bank';
  const userId = 1000000000 + __VU * 10000 + __ITER; // Unique userId per iteration
  const payload = JSON.stringify({
    userId: userId,
    userEmail: `user${userId}@example.com`
  });
  const params = {
    headers: { 'Content-Type': 'application/json' }
  };

  const res = http.post(url, payload, params);

  check(res, {
    'status is 201': (r) => r.status === 201,
    'response time < 500ms': (r) => r.timings.duration < 500
  });
}
```

### Key Metrics to Track
- **Response time** (p95 should be < 500ms per documentation)
- **Success rate** (should be 100% for valid requests)
- **Transaction completion** (MongoDB atomicity)
- **ID generation uniqueness** (no duplicate questionBankIds)

### Actual K6 Test Implementation

A comprehensive functional test suite has been created at:
```
api-system-test/test-create-default-question-bank.js
```

**Test Coverage (7 groups, 35 assertions, 100% pass rate)**:
1. ✅ Happy Path - Create Default Question Bank (201 Created)
2. ✅ Unhappy Path - Missing userId (400 Bad Request)
3. ✅ Unhappy Path - Invalid userId (null) (400 Bad Request)
4. ✅ Unhappy Path - Invalid userId (zero) (400 Bad Request)
5. ✅ Unhappy Path - Invalid userId (negative) (400 Bad Request)
6. ✅ Unhappy Path - Invalid email format (400 Bad Request)
7. ✅ Unhappy Path - Duplicate User (409 Conflict)

**Run Command**:
```bash
k6 run api-system-test/test-create-default-question-bank.js
```

**Expected Output**:
```
checks.........................: 100.00% ✓ 35
✓ ✓ status is 201 Created
✓ ✓ questionBankId exists and is positive
✓ ✓ active field is true
✓ ✓ status is 400 Bad Request
✓ ✓ error is "Bad Request"
✓ ✓ second request: status is 409 Conflict
```

---

## Code References

### Controller
- **File**: `orchestration-layer/src/main/java/com/quizfun/orchestrationlayer/controllers/DefaultQuestionBankController.java`
- **Method**: `createDefaultQuestionBank` (line 62)

### Test Files
- **Unit Test**: `orchestration-layer/src/test/java/com/quizfun/orchestrationlayer/controllers/DefaultQuestionBankControllerTest.java`
- **Reference Test Method**: `shouldReturn201CreatedWhenSuccessful()` (line 57)
- **K6 API System Test**: `api-system-test/test-create-default-question-bank.js`
  - 7 test groups (happy path + 6 unhappy paths)
  - 35 assertions with 100% pass rate
  - Validates actual HTTP responses, status codes, and error formats

### Documentation
- **Use Case Design**: `usecases/on-new-user-creating-default-question-bank/use-case-design.md`
- **Architecture Overview**: `usecases/on-new-user-creating-default-question-bank/architecture-overview.md`

### Domain Components
- **Command**: `OnNewUserCreateDefaultQuestionBankCommand`
- **Aggregates**: `QuestionBanksPerUserAggregate`, `TaxonomySetAggregate`
- **Repository**: `MongoDefaultQuestionBankRepository`

---

## MongoDB Collections Modified

### question_banks_per_user
```javascript
{
  "_id": ObjectId("67029a8c1234567890abcdef"),
  "user_id": 123456789,
  "default_question_bank_id": 1730832000000000,
  "question_banks": [
    {
      "bank_id": 1730832000000000,
      "name": "Default Question Bank",
      "description": "Your default question bank for getting started with quiz creation",
      "is_active": true,
      "created_at": ISODate("2025-10-09T10:30:15.123Z"),
      "updated_at": ISODate("2025-10-09T10:30:15.123Z")
    }
  ],
  "created_at": ISODate("2025-10-09T10:30:15.123Z"),
  "updated_at": ISODate("2025-10-09T10:30:15.123Z")
}
```

### taxonomy_sets
```javascript
{
  "_id": ObjectId("67029a8c1234567890abcdf0"),
  "user_id": 123456789,
  "question_bank_id": 1730832000000000,
  "categories": {
    "level_1": {
      "id": "general",
      "name": "General",
      "slug": "general",
      "parent_id": null
    }
  },
  "tags": [
    { "id": "beginner", "name": "Beginner", "color": "#28a745" },
    { "id": "practice", "name": "Practice", "color": "#007bff" },
    { "id": "quick-test", "name": "Quick Test", "color": "#6f42c1" }
  ],
  "quizzes": [],
  "current_difficulty_level": {
    "level": "easy",
    "numeric_value": 1,
    "description": "Suitable for beginners and initial learning"
  },
  "available_difficulty_levels": [
    { "level": "easy", "numeric_value": 1, "description": "..." },
    { "level": "medium", "numeric_value": 2, "description": "..." },
    { "level": "hard", "numeric_value": 3, "description": "..." }
  ],
  "created_at": ISODate("2025-10-09T10:30:15.123Z"),
  "updated_at": ISODate("2025-10-09T10:30:15.123Z")
}
```

---

## Business Rules Summary

1. **User Uniqueness (BR-001)**: Each user can have only ONE question_banks_per_user document
2. **Question Bank ID Uniqueness (BR-002)**: LongIdGenerator guarantees unique IDs via timestamp + sequence
3. **Default Bank Reference (BR-003)**: default_question_bank_id must reference an active bank
4. **Taxonomy Hierarchy (BR-004)**: Category hierarchy must have no gaps (default only has level_1)
5. **Tag ID Uniqueness (BR-005)**: Tag IDs must be unique within tags array
6. **Difficulty Level Existence (BR-006)**: current_difficulty_level must exist in available_difficulty_levels
7. **Atomic Creation (BR-007)**: Both documents created together in MongoDB transaction
8. **User ID Validity (BR-008)**: User ID must be positive Long integer (> 0)
