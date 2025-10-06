# Use Case Design: Create Default Question Bank on New User

## HTTP API Contract

### Endpoint
```
POST /api/users/default-question-bank
```

### Request Headers
```http
Content-Type: application/json
X-User-Management-System: external-user-service
X-Request-ID: usr-req-550e8400-e29b-41d4-a716-446655440000
```

### Request Body

#### Schema
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

#### Example Request
```json
{
  "userId": 123456789,
  "userEmail": "john.doe@example.com",
  "metadata": {
    "createdBy": "user-management-system-v2",
    "createdAt": "2025-10-06T10:30:00Z",
    "requestId": "usr-req-550e8400-e29b-41d4-a716-446655440000"
  }
}
```

#### Minimal Request (Required Fields Only)
```json
{
  "userId": 123456789
}
```

### Request Validation Rules

| Field | Type | Required | Validation Rules |
|-------|------|----------|------------------|
| `userId` | Long | Yes | - Must be positive (> 0)<br>- Must be non-null<br>- Must not already have question banks |
| `userEmail` | String | No | - Valid email format (if provided)<br>- Max 255 characters |
| `metadata.createdBy` | String | No | - Max 100 characters |
| `metadata.createdAt` | ISO-8601 String | No | - Valid ISO-8601 timestamp |
| `metadata.requestId` | String | No | - Max 100 characters<br>- Used for idempotency tracking |

### Response Specifications

#### Success Response (201 Created)
```json
{
  "success": true,
  "message": "Default question bank created successfully for user 123456789",
  "data": {
    "userId": 123456789,
    "questionBankId": 1730832000000000,
    "questionBankName": "Default Question Bank",
    "description": "Your default question bank for getting started with quiz creation",
    "isActive": true,
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
    "createdAt": "2025-10-06T10:30:15.123Z"
  }
}
```

#### Error Responses

##### 400 Bad Request - Invalid User ID
```json
{
  "success": false,
  "message": "VALIDATION_ERROR: userId must be a positive integer greater than 0",
  "data": null,
  "errorCode": "INVALID_USER_ID",
  "timestamp": "2025-10-06T10:30:15.123Z"
}
```

##### 400 Bad Request - Missing Required Field
```json
{
  "success": false,
  "message": "VALIDATION_ERROR: userId is required",
  "data": null,
  "errorCode": "MISSING_REQUIRED_FIELD",
  "timestamp": "2025-10-06T10:30:15.123Z"
}
```

##### 409 Conflict - User Already Has Question Bank
```json
{
  "success": false,
  "message": "DUPLICATE_USER: User 123456789 already has a default question bank",
  "data": {
    "existingQuestionBankId": 1730800000000000,
    "userId": 123456789
  },
  "errorCode": "DUPLICATE_USER",
  "timestamp": "2025-10-06T10:30:15.123Z"
}
```

##### 500 Internal Server Error - Database Transaction Failed
```json
{
  "success": false,
  "message": "DATABASE_ERROR: Failed to create default question bank due to transaction failure",
  "data": null,
  "errorCode": "TRANSACTION_FAILED",
  "timestamp": "2025-10-06T10:30:15.123Z"
}
```

##### 500 Internal Server Error - Template Processing Failed
```json
{
  "success": false,
  "message": "INTERNAL_ERROR: Failed to process document templates",
  "data": null,
  "errorCode": "TEMPLATE_PROCESSING_ERROR",
  "timestamp": "2025-10-06T10:30:15.123Z"
}
```

### HTTP Status Code Mapping

| Status Code | Scenario | Error Code |
|-------------|----------|------------|
| 201 Created | Success | - |
| 400 Bad Request | Invalid userId (null, zero, negative) | INVALID_USER_ID |
| 400 Bad Request | Missing required field | MISSING_REQUIRED_FIELD |
| 400 Bad Request | Invalid email format | INVALID_EMAIL_FORMAT |
| 409 Conflict | User already has question bank | DUPLICATE_USER |
| 500 Internal Server Error | MongoDB transaction failed | TRANSACTION_FAILED |
| 500 Internal Server Error | Template processing error | TEMPLATE_PROCESSING_ERROR |
| 500 Internal Server Error | ID generation error | ID_GENERATION_ERROR |
| 500 Internal Server Error | Unexpected system error | INTERNAL_ERROR |

## Use Case Specification

### Use Case Name
**Create Default Question Bank on New User Registration**

### Actors
- **Primary Actor**: External User Management System
- **Supporting Actor**: Quiz CMS System

### Preconditions
1. User has been created in the external user management system
2. User ID is a valid positive Long integer
3. User does NOT already have question banks in Quiz CMS system
4. MongoDB is available (Testcontainers running in test environment)
5. LongIdGenerator service is available

### Postconditions

#### Success Postconditions
1. `question_banks_per_user` document created in MongoDB with:
   - User ID stored
   - Default question bank entry in embedded array
   - Default question bank ID set as default
2. `taxonomy_sets` document created in MongoDB with:
   - User ID and question bank ID stored
   - Default categories (level_1: "general")
   - Default tags ("beginner", "practice", "quick-test")
   - Default difficulty levels (easy, medium, hard)
3. Both documents created atomically (MongoDB transaction)
4. HTTP 201 Created response returned with question bank details

#### Failure Postconditions
1. No documents created in MongoDB (transaction rollback)
2. HTTP error response returned with appropriate status code
3. Error logged for debugging

### Main Success Scenario (Happy Path)

#### Step-by-Step Flow

**Step 1: External System Sends HTTP Request**
```
External User Management System → Quiz CMS
POST /api/users/default-question-bank
Content-Type: application/json
{
  "userId": 123456789,
  "userEmail": "john.doe@example.com"
}
```

**Step 2: Controller Receives and Validates Request**
```java
DefaultQuestionBankController.createDefaultQuestionBank()
- Validate request body structure
- Extract userId from request
- Create CreateDefaultQuestionBankRequestDto
```

**Step 3: Controller Creates Command**
```java
OnNewUserCreateDefaultQuestionBankCommand command =
    new OnNewUserCreateDefaultQuestionBankCommand(
        requestDto.getUserId(),
        requestDto.getUserEmail(),
        requestDto.getMetadata()
    );
```

**Step 4: Controller Sends Command via Mediator**
```java
Result<DefaultQuestionBankResponseDto> result = mediator.send(command);
```

**Step 5: Mediator Routes to Handler**
```java
MediatorImpl → OnNewUserCreateDefaultQuestionBankCommandHandler
- Auto-discovery via Spring ApplicationContext
- Type-safe routing based on command type
```

**Step 6: Handler Validates Business Rules**
```java
OnNewUserCreateDefaultQuestionBankCommandHandler.handle()
- Validate userId > 0
- Check user doesn't already have question banks
  → repository.checkUserExists(userId) → false
```

**Step 7: Handler Generates IDs**
```java
// Generate question bank ID
Long questionBankId = longIdGenerator.generateQuestionBankId();
// Result: 1730832000000000

// Generate MongoDB document IDs
ObjectId questionBanksDocId = new ObjectId();
ObjectId taxonomySetDocId = new ObjectId();
```

**Step 8: Handler Loads JSON Templates**
```java
String questionBanksTemplate = loadTemplate("question_banks_per_user.json_template");
String taxonomySetTemplate = loadTemplate("taxonomy_sets.json_template");
```

**Step 9: Handler Replaces Template Variables**
```java
Instant now = Instant.now();

Map<String, String> variables = Map.of(
    "{{NEW_USER_ID}}", String.valueOf(userId),                      // 123456789
    "{{GENERATED_DEFAULT_BANK_ID}}", String.valueOf(questionBankId), // 1730832000000000
    "{{GENERATED_OBJECT_ID}}", questionBanksDocId.toString(),
    "{{CURRENT_TIMESTAMP}}", now.toString()
);

String questionBanksJson = replaceVariables(questionBanksTemplate, variables);
String taxonomySetJson = replaceVariables(taxonomySetTemplate, variables);
```

**Step 10: Handler Creates Domain Aggregates**
```java
QuestionBanksPerUserAggregate questionBanksAggregate =
    QuestionBanksPerUserAggregate.create(
        questionBanksDocId,
        userId,
        questionBankId,
        List.of(new QuestionBank(
            questionBankId,
            "Default Question Bank",
            "Your default question bank for getting started with quiz creation",
            true, // isActive
            now,
            now
        ))
    );

TaxonomySetAggregate taxonomyAggregate =
    TaxonomySetAggregate.create(
        taxonomySetDocId,
        userId,
        questionBankId,
        createDefaultCategories(),
        createDefaultTags(),
        List.of(), // empty quizzes
        createDefaultDifficulty(),
        createAvailableDifficulties()
    );
```

**Step 11: Handler Validates Aggregates**
```java
// Domain validations (enforced by aggregate constructors)
- userId must be positive
- questionBankId must be positive
- At least one active question bank
- Category hierarchy valid (level_1 only, no gaps)
- Tag IDs unique
- Difficulty level exists in available levels
```

**Step 12: Handler Starts MongoDB Transaction**
```java
MongoTemplate mongoTemplate = ...;
ClientSession session = mongoTemplate.getMongoClient().startSession();

session.withTransaction(() -> {
    // Transaction scope begins
    ...
}, txOptions);
```

**Step 13: Repository Checks User Existence (Within Transaction)**
```java
boolean userExists = repository.checkUserExists(userId, session);
if (userExists) {
    throw new DuplicateUserException("User already has question banks");
}
```

**Step 14: Repository Inserts question_banks_per_user Document**
```java
QuestionBanksPerUserDocument doc = mapper.toDocument(questionBanksAggregate);
mongoTemplate.insert(doc, "question_banks_per_user", session);
```

**Step 15: Repository Inserts taxonomy_sets Document**
```java
TaxonomySetDocument taxDoc = mapper.toDocument(taxonomyAggregate);
mongoTemplate.insert(taxDoc, "taxonomy_sets", session);
```

**Step 16: Transaction Commits**
```java
// If both inserts succeed, transaction auto-commits
session.commitTransaction();
```

**Step 17: Handler Builds Response DTO**
```java
DefaultQuestionBankResponseDto response = DefaultQuestionBankResponseDto.builder()
    .userId(userId)
    .questionBankId(questionBankId)
    .questionBankName("Default Question Bank")
    .description("Your default question bank...")
    .isActive(true)
    .taxonomySetCreated(true)
    .availableTaxonomy(extractTaxonomy(taxonomyAggregate))
    .createdAt(now)
    .build();
```

**Step 18: Handler Returns Success Result**
```java
return Result.success(
    "Default question bank created successfully for user " + userId,
    response
);
```

**Step 19: Controller Maps to HTTP Response**
```java
if (result.success()) {
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .header("X-Question-Bank-ID", result.data().getQuestionBankId().toString())
        .body(result);
}
```

**Step 20: External System Receives Response**
```
HTTP/1.1 201 Created
Content-Type: application/json
X-Question-Bank-ID: 1730832000000000

{
  "success": true,
  "message": "Default question bank created successfully for user 123456789",
  "data": { ... }
}
```

### Alternative Flows

#### Alternative Flow 1: User Already Exists (409 Conflict)
**Diverges at Step 13**

**Step 13a: Repository Detects Existing User**
```java
boolean userExists = repository.checkUserExists(userId, session);
if (userExists) {
    // Transaction not started or immediately aborted
    throw new DuplicateUserException("User 123456789 already has question banks");
}
```

**Step 13b: Handler Catches Exception**
```java
catch (DuplicateUserException ex) {
    return Result.failure(
        "DUPLICATE_USER: User " + userId + " already has a default question bank"
    );
}
```

**Step 13c: Controller Returns 409 Conflict**
```java
if (!result.success() && result.message().startsWith("DUPLICATE_USER")) {
    return ResponseEntity
        .status(HttpStatus.CONFLICT)
        .body(result);
}
```

**Response:**
```json
{
  "success": false,
  "message": "DUPLICATE_USER: User 123456789 already has a default question bank",
  "data": null,
  "errorCode": "DUPLICATE_USER"
}
```

#### Alternative Flow 2: Invalid User ID (400 Bad Request)
**Diverges at Step 6**

**Step 6a: Command Validation Fails**
```java
// In command constructor
if (userId == null || userId <= 0) {
    throw new IllegalArgumentException("userId must be a positive integer");
}
```

**Step 6b: Controller Catches Exception**
```java
catch (IllegalArgumentException ex) {
    return ResponseEntity
        .badRequest()
        .body(Result.failure("VALIDATION_ERROR: " + ex.getMessage()));
}
```

**Response:**
```json
{
  "success": false,
  "message": "VALIDATION_ERROR: userId must be a positive integer",
  "errorCode": "INVALID_USER_ID"
}
```

#### Alternative Flow 3: MongoDB Transaction Failed (500 Internal Server Error)
**Diverges at Step 14 or Step 15**

**Step 14a: Insert Fails (e.g., network error)**
```java
try {
    mongoTemplate.insert(doc, "question_banks_per_user", session);
    mongoTemplate.insert(taxDoc, "taxonomy_sets", session);
} catch (MongoException ex) {
    // Transaction auto-rollback
    session.abortTransaction();
    throw new DatabaseTransactionException("Transaction failed", ex);
}
```

**Step 14b: Handler Catches Exception**
```java
catch (DatabaseTransactionException ex) {
    logger.error("MongoDB transaction failed for user {}", userId, ex);
    return Result.failure(
        "DATABASE_ERROR: Failed to create default question bank due to transaction failure"
    );
}
```

**Step 14c: Controller Returns 500 Error**
```java
if (!result.success() && result.message().startsWith("DATABASE_ERROR")) {
    return ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(result);
}
```

**Response:**
```json
{
  "success": false,
  "message": "DATABASE_ERROR: Failed to create default question bank due to transaction failure",
  "errorCode": "TRANSACTION_FAILED"
}
```

#### Alternative Flow 4: Template Loading Failed (500 Internal Server Error)
**Diverges at Step 8**

**Step 8a: Template File Not Found**
```java
try {
    String template = loadTemplate("question_banks_per_user.json_template");
} catch (IOException ex) {
    throw new TemplateProcessingException("Failed to load template", ex);
}
```

**Step 8b: Handler Catches Exception**
```java
catch (TemplateProcessingException ex) {
    logger.error("Template processing failed for user {}", userId, ex);
    return Result.failure(
        "INTERNAL_ERROR: Failed to process document templates"
    );
}
```

**Response:**
```json
{
  "success": false,
  "message": "INTERNAL_ERROR: Failed to process document templates",
  "errorCode": "TEMPLATE_PROCESSING_ERROR"
}
```

### Business Rules

#### BR-001: User Uniqueness
- **Rule**: Each user can have only ONE `question_banks_per_user` document
- **Enforcement**: Unique index on `user_id` field in MongoDB
- **Validation**: Check user existence before insert
- **Error**: 409 Conflict if user already exists

#### BR-002: Question Bank ID Uniqueness
- **Rule**: Question bank IDs must be unique across the system
- **Enforcement**: LongIdGenerator guarantees uniqueness via timestamp + sequence
- **Validation**: No explicit check needed (generator guarantees)

#### BR-003: Default Question Bank Reference
- **Rule**: `default_question_bank_id` must reference an active bank in `question_banks` array
- **Enforcement**: Domain validation in `QuestionBanksPerUserAggregate`
- **Validation**: Check during aggregate creation

#### BR-004: Taxonomy Hierarchy Validity
- **Rule**: Category hierarchy must have no gaps (if level_2 exists, level_1 must exist)
- **Enforcement**: Domain validation in `TaxonomySetAggregate`
- **Validation**: For default creation, only level_1 is created (no gaps possible)

#### BR-005: Tag ID Uniqueness
- **Rule**: Tag IDs must be unique within the `tags` array
- **Enforcement**: Domain validation in `TaxonomySetAggregate`
- **Validation**: Template ensures unique IDs ("beginner", "practice", "quick-test")

#### BR-006: Difficulty Level Existence
- **Rule**: `current_difficulty_level` must exist in `available_difficulty_levels`
- **Enforcement**: Domain validation in `TaxonomySetAggregate`
- **Validation**: Template ensures "easy" exists in available levels

#### BR-007: Atomic Document Creation
- **Rule**: Both `question_banks_per_user` and `taxonomy_sets` must be created together
- **Enforcement**: MongoDB transaction (session.withTransaction)
- **Validation**: Transaction rollback on any failure

#### BR-008: User ID Validity
- **Rule**: User ID must be a positive Long integer (> 0)
- **Enforcement**: Command validation and domain validation
- **Validation**: Check in command constructor and aggregate constructor

### Domain Model

#### Aggregates

**QuestionBanksPerUserAggregate**
- **Aggregate Root**: Manages user's question bank collection
- **Entities**:
  - QuestionBank (embedded entity, not aggregate root)
- **Value Objects**:
  - userId (Long)
  - defaultQuestionBankId (Long)
- **Invariants**:
  - At least one active question bank
  - Default bank ID must reference an active bank
  - User ID must be positive

**TaxonomySetAggregate**
- **Aggregate Root**: Manages taxonomy structure for a question bank
- **Entities**: None (all embedded value objects)
- **Value Objects**:
  - CategoryLevels (categories hierarchy)
  - Tag (id, name, color)
  - DifficultyLevel (level, numericValue, description)
  - Quiz (quizId, name)
- **Invariants**:
  - Category hierarchy has no gaps
  - Tag IDs unique
  - Current difficulty exists in available levels

### MongoDB Document Structures

#### question_banks_per_user Collection
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
      "created_at": ISODate("2025-10-06T10:30:15.123Z"),
      "updated_at": ISODate("2025-10-06T10:30:15.123Z")
    }
  ],
  "created_at": ISODate("2025-10-06T10:30:15.123Z"),
  "updated_at": ISODate("2025-10-06T10:30:15.123Z")
}
```

#### taxonomy_sets Collection
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
  "quizzes": [],
  "current_difficulty_level": {
    "level": "easy",
    "numeric_value": 1,
    "description": "Suitable for beginners and initial learning"
  },
  "available_difficulty_levels": [
    {
      "level": "easy",
      "numeric_value": 1,
      "description": "Suitable for beginners and initial learning"
    },
    {
      "level": "medium",
      "numeric_value": 2,
      "description": "Intermediate knowledge required"
    },
    {
      "level": "hard",
      "numeric_value": 3,
      "description": "Advanced understanding needed"
    }
  ],
  "created_at": ISODate("2025-10-06T10:30:15.123Z"),
  "updated_at": ISODate("2025-10-06T10:30:15.123Z")
}
```

### MongoDB Indexes

#### question_banks_per_user Indexes
```javascript
// Primary unique index for user lookup
db.question_banks_per_user.createIndex(
  { user_id: 1 },
  { unique: true, name: "idx_user_id_unique" }
);

// Index for bank lookup within user's banks
db.question_banks_per_user.createIndex(
  { user_id: 1, "question_banks.bank_id": 1 },
  { name: "idx_user_bank_lookup" }
);

// Index for default bank reference
db.question_banks_per_user.createIndex(
  { user_id: 1, default_question_bank_id: 1 },
  { name: "idx_user_default_bank" }
);
```

#### taxonomy_sets Indexes
```javascript
// Unique compound index (one taxonomy set per user+bank)
db.taxonomy_sets.createIndex(
  { user_id: 1, question_bank_id: 1 },
  { unique: true, name: "idx_user_bank_unique" }
);

// Index for category lookups
db.taxonomy_sets.createIndex(
  { user_id: 1, question_bank_id: 1, "categories.level_1.id": 1 },
  { name: "idx_category_level1_lookup" }
);

// Index for tag validation
db.taxonomy_sets.createIndex(
  { user_id: 1, question_bank_id: 1, "tags.id": 1 },
  { name: "idx_tag_lookup" }
);

// Index for difficulty level queries
db.taxonomy_sets.createIndex(
  { user_id: 1, question_bank_id: 1, "current_difficulty_level.level": 1 },
  { name: "idx_difficulty_lookup" }
);
```

## Non-Functional Requirements

### Performance
- **Command execution time**: < 500ms (p95)
- **ID generation time**: < 1ms (LongIdGenerator)
- **MongoDB transaction time**: < 200ms (p95)
- **Template processing time**: < 50ms

### Scalability
- Support concurrent user creation (thread-safe ID generation)
- MongoDB transaction isolation prevents race conditions
- LongIdGenerator tested at >500K IDs/sec throughput

### Reliability
- **Transaction atomicity**: 100% (all-or-nothing guarantee)
- **Idempotency**: Duplicate user requests return 409 (no side effects)
- **Error recovery**: Transaction rollback on any failure

### Security
- User ID validation (prevent injection, negative IDs)
- Template variable sanitization (prevent injection)
- No sensitive data in logs
- MongoDB connection via authenticated session (in production)

### Testability
- Testcontainers MongoDB for integration testing
- Mockito for unit testing
- All components injectable via Spring DI
- TDD approach (tests written before implementation)
