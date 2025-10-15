# Use Case Design: Query List of Questions from Question Bank

## HTTP API Contract

### Endpoint
```
GET /api/users/{userId}/questionbanks/{questionbankId}/questions
```

### Request Headers
```http
Accept: application/json
Authorization: Bearer <JWT_TOKEN>  (when security is enabled)
```

### Path Parameters

| Parameter | Type | Required | Validation | Description |
|-----------|------|----------|------------|-------------|
| `userId` | Long | ✅ Yes | Must be positive (> 0) | User identifier - **MUST match JWT token userId** when security is enabled |
| `questionbankId` | Long | ✅ Yes | Must be positive (> 0) | Target question bank identifier |

### Query Parameters (All Optional)

#### Taxonomy Filters

| Parameter | Type | Format | Description | Example |
|-----------|------|--------|-------------|---------|
| `category_level_1` | String | Category ID | Filter by level 1 category | `category_level_1=general` |
| `category_level_2` | String | Category ID | Filter by level 2 category | `category_level_2=programming` |
| `category_level_3` | String | Category ID | Filter by level 3 category | `category_level_3=web_dev` |
| `category_level_4` | String | Category ID | Filter by level 4 category | `category_level_4=javascript` |
| `tags` | String | Comma-separated | Filter by tag IDs (OR logic) | `tags=beginner,practice` |
| `quizzes` | String | Comma-separated | Filter by quiz IDs (OR logic) | `quizzes=101,102` |
| `difficulty_level` | String | Level string | Filter by difficulty level | `difficulty_level=easy` |

#### Question Property Filters

| Parameter | Type | Format | Description | Example |
|-----------|------|--------|-------------|---------|
| `question_type` | String | Enum | Filter by question type | `question_type=mcq` |
| `status` | String | Enum | Filter by lifecycle status | `status=published` |
| `search` | String | Text | Full-text search in title/content | `search=array methods` |

**Valid Enum Values**:
- `question_type`: `mcq`, `true_false`, `essay`
- `status`: `draft`, `published`, `archived`

#### Pagination Parameters

| Parameter | Type | Default | Constraints | Description |
|-----------|------|---------|-------------|-------------|
| `page` | Integer | 0 | >= 0 | Zero-indexed page number |
| `size` | Integer | 20 | 1-100 | Number of items per page |
| `sort` | String | `created_at,desc` | field,direction | Sort criteria (comma-separated) |

**Sort Format**:
- Format: `field,direction` where direction is `asc` or `desc`
- Supported fields: `title`, `created_at`, `updated_at`, `display_order`, `points`
- Multiple sorts: Use multiple `sort` parameters

**Sort Examples**:
```
?sort=title,asc                    # Sort by title ascending
?sort=created_at,desc              # Sort by creation date descending (default)
?sort=display_order,asc&sort=title,asc  # Multi-field sort
```

### Request Examples

#### Example 1: Basic Query - Get First Page
```http
GET /api/users/999888777/questionbanks/1730832000000000/questions
```

**Description**: Returns first 20 questions sorted by creation date (most recent first)

#### Example 2: Filter by Category Hierarchy
```http
GET /api/users/999888777/questionbanks/1730832000000000/questions?category_level_1=technology&category_level_2=programming
```

**Description**: Returns questions in Technology → Programming category hierarchy

#### Example 3: Filter by Multiple Tags (OR Logic)
```http
GET /api/users/999888777/questionbanks/1730832000000000/questions?tags=beginner,practice
```

**Description**: Returns questions that have **either** "beginner" **OR** "practice" tags

#### Example 4: Filter by Question Type and Status
```http
GET /api/users/999888777/questionbanks/1730832000000000/questions?question_type=mcq&status=published
```

**Description**: Returns only published MCQ questions

#### Example 5: Full-Text Search
```http
GET /api/users/999888777/questionbanks/1730832000000000/questions?search=array methods
```

**Description**: Returns questions containing "array methods" in title or content

#### Example 6: Complex Combined Filters
```http
GET /api/users/999888777/questionbanks/1730832000000000/questions?
    category_level_1=technology&
    tags=beginner,practice&
    question_type=mcq&
    difficulty_level=easy&
    status=published&
    page=0&
    size=10&
    sort=title,asc
```

**Filter Logic**:
- Category: technology (AND)
- Tags: beginner OR practice (OR)
- Type: MCQ (AND)
- Difficulty: easy (AND)
- Status: published (AND)
- Page 1, 10 items, sorted by title ascending

#### Example 7: Pagination - Second Page
```http
GET /api/users/999888777/questionbanks/1730832000000000/questions?page=1&size=50
```

**Description**: Returns items 51-100 (page is zero-indexed)

## Response Specifications

### Success Response (200 OK)

```json
{
  "success": true,
  "message": "Questions retrieved successfully",
  "data": {
    "questions": [
      {
        "questionId": "507f1f77bcf86cd799439014",
        "sourceQuestionId": "q-uuid-123-abc",
        "questionType": "mcq",
        "title": "JavaScript Array Methods",
        "content": "<p>Which method adds elements to the <strong>end</strong> of an array?</p>",
        "status": "published",
        "points": 5,
        "displayOrder": 1,
        "solutionExplanation": "<p>The push() method adds elements to the end.</p>",

        "taxonomy": {
          "categories": {
            "level1": {
              "id": "technology",
              "name": "Technology",
              "slug": "technology"
            },
            "level2": {
              "id": "programming",
              "name": "Programming",
              "slug": "programming"
            },
            "level3": {
              "id": "javascript",
              "name": "JavaScript",
              "slug": "javascript"
            }
          },
          "tags": [
            { "id": "js-arrays", "name": "JavaScript Arrays", "color": "#f7df1e" },
            { "id": "beginner", "name": "Beginner", "color": "#28a745" }
          ],
          "quizzes": [
            { "quizId": 101, "quizName": "JavaScript Fundamentals", "quizSlug": "js-fundamentals" }
          ],
          "difficultyLevel": {
            "level": "easy",
            "numericValue": 1,
            "description": "Suitable for beginners"
          }
        },

        "mcqData": {
          "options": [
            {
              "id": 1,
              "text": "push()",
              "isCorrect": true,
              "explanation": "Correct! Adds to end."
            },
            {
              "id": 2,
              "text": "pop()",
              "isCorrect": false,
              "explanation": "Removes from end."
            },
            {
              "id": 3,
              "text": "shift()",
              "isCorrect": false,
              "explanation": "Removes from beginning."
            }
          ],
          "shuffleOptions": false,
          "allowMultipleCorrect": false,
          "timeLimitSeconds": 60
        },

        "attachments": [
          {
            "id": "att_001",
            "type": "image",
            "filename": "array_diagram.png",
            "url": "/attachments/array_diagram.png",
            "size": 245760,
            "mimeType": "image/png"
          }
        ],

        "metadata": {
          "createdSource": "manual",
          "version": 1,
          "authorId": 999888777,
          "lastEditorId": 999888777
        },

        "createdAt": "2025-10-09T10:30:00Z",
        "updatedAt": "2025-10-09T15:45:00Z",
        "publishedAt": "2025-10-09T16:00:00Z",
        "archivedAt": null
      }
    ],
    "pagination": {
      "currentPage": 0,
      "pageSize": 20,
      "totalElements": 156,
      "totalPages": 8,
      "isFirst": true,
      "isLast": false,
      "hasNext": true,
      "hasPrevious": false
    },
    "filters": {
      "appliedFilters": {
        "categoryLevel1": "technology",
        "tags": ["beginner", "practice"],
        "questionType": "mcq",
        "difficultyLevel": "easy",
        "status": "published"
      },
      "resultCount": 156
    }
  }
}
```

### Response Headers
```http
HTTP/1.1 200 OK
Content-Type: application/json
X-Total-Count: 156
X-Page-Number: 0
X-Page-Size: 20
```

### Empty Result Response (200 OK)
```json
{
  "success": true,
  "message": "No questions found matching the criteria",
  "data": {
    "questions": [],
    "pagination": {
      "currentPage": 0,
      "pageSize": 20,
      "totalElements": 0,
      "totalPages": 0,
      "isFirst": true,
      "isLast": true,
      "hasNext": false,
      "hasPrevious": false
    },
    "filters": {
      "appliedFilters": {
        "categoryLevel1": "nonexistent"
      },
      "resultCount": 0
    }
  }
}
```

**Important**: Empty results are **NOT an error** - they return 200 OK with an empty array.

## Error Responses

### 400 Bad Request - Invalid Query Parameters
```json
{
  "success": false,
  "message": "INVALID_QUERY_PARAMETER: Invalid page size. Must be between 1 and 100",
  "data": null
}
```

**When**: Query parameter validation fails
**Examples**:
- `page < 0`
- `size < 1` or `size > 100`
- Invalid `question_type` enum value
- Invalid `status` enum value
- Invalid sort field name

### 400 Bad Request - Invalid Path Parameters
```json
{
  "success": false,
  "message": "CONSTRAINT_VIOLATION: Path parameter userId must be positive",
  "data": null
}
```

**When**: Path parameters fail validation (userId or questionbankId <= 0)

### 422 Unprocessable Entity - Unauthorized Access
```json
{
  "success": false,
  "message": "UNAUTHORIZED_ACCESS: User does not have access to question bank 123",
  "data": null
}
```

**When**: User does not own the specified question bank

### 422 Unprocessable Entity - Question Bank Not Found
```json
{
  "success": false,
  "message": "QUESTION_BANK_NOT_FOUND: Question bank 123 does not exist for user 456",
  "data": null
}
```

**When**: Specified question bank doesn't exist in user's collection

### 500 Internal Server Error - Database Error
```json
{
  "success": false,
  "message": "DATABASE_ERROR: Failed to retrieve questions from database",
  "data": null
}
```

**When**: MongoDB query execution fails

## HTTP Status Code Reference

| Status Code | Scenario | Error Code |
|-------------|----------|------------|
| 200 OK | Success - questions retrieved (even if empty) | - |
| 400 Bad Request | Invalid query parameters | INVALID_QUERY_PARAMETER |
| 400 Bad Request | Invalid path parameters | CONSTRAINT_VIOLATION |
| 422 Unprocessable Entity | Unauthorized access | UNAUTHORIZED_ACCESS |
| 422 Unprocessable Entity | Question bank not found | QUESTION_BANK_NOT_FOUND |
| 500 Internal Server Error | Database error | DATABASE_ERROR |

## Use Case Specification

### Use Case Name
**Query List of Questions from User's Question Bank with Filters**

### Actors
- **Primary Actor**: Content Creator/Educator
- **Supporting Actor**: Quiz CMS System

### Preconditions
1. User has been authenticated (when security is enabled)
2. User ID is a valid positive Long integer
3. Question bank exists and belongs to the user
4. MongoDB is available (Testcontainers in test, replica set in production)

### Postconditions

#### Success Postconditions
1. List of questions returned matching filter criteria
2. Pagination metadata included in response
3. Applied filters summarized in response
4. HTTP 200 OK returned even if no questions match
5. Questions include full taxonomy relationships
6. Type-specific data (MCQ/Essay/TrueFalse) included

#### Failure Postconditions
1. HTTP error response returned with appropriate status code
2. Error logged for debugging
3. No data modification (read-only operation)

### Main Success Scenario (Happy Path)

#### Step 1: User Sends HTTP GET Request
```
Content Creator → Quiz CMS
GET /api/users/999888777/questionbanks/1730832000000000/questions?category_level_1=technology&page=0&size=20
```

#### Step 2: Controller Receives and Validates Request
```java
QuestionQueryController.queryQuestions()
- Extract path parameters (userId, questionbankId)
- Extract query parameters (filters, pagination)
- Validate path parameters (positive integers)
```

#### Step 3: Controller Creates Query Object
```java
QueryQuestionsQuery query = QueryQuestionsQuery.builder()
    .userId(userId)
    .questionBankId(questionbankId)
    .filters(QuestionFilters.builder()
        .categoryLevel1("technology")
        .build())
    .pagination(PaginationParams.builder()
        .page(0)
        .size(20)
        .sort("created_at,desc")
        .build())
    .build();
```

#### Step 4: Controller Sends Query via Mediator
```java
Result<QuestionListResponseDto> result = mediator.send(query);
```

#### Step 5: Mediator Routes to Query Handler
```java
MediatorImpl → QueryQuestionsQueryHandler
- Auto-discovery via Spring ApplicationContext
- Type-safe routing based on query type
```

#### Step 6: Query Handler Invokes Application Service
```java
QueryQuestionsQueryHandler.handle()
- Inject QueryQuestionsService (Port IN)
- Delegate to application service
return queryService.queryQuestions(
    query.getUserId(),
    query.getQuestionBankId(),
    query.getFilters(),
    query.getPagination()
);
```

#### Step 7: Application Service Validates Ownership
```java
DefaultQueryQuestionsService.queryQuestions()
- Check user owns question bank
boolean hasAccess = questionBanksRepository.validateOwnership(userId, questionBankId);
if (!hasAccess) {
    return Result.failure("UNAUTHORIZED_ACCESS");
}
```

#### Step 8: Application Service Invokes Repository
```java
- Build MongoDB query criteria from filters
- Apply pagination parameters
var pagedResult = questionQueryRepository.findQuestions(
    userId,
    questionBankId,
    filters,
    pagination
);
```

#### Step 9: Repository Builds MongoDB Query
```java
MongoQuestionQueryRepository.findQuestions()
- Build base criteria (user_id, question_bank_id)
Criteria criteria = Criteria
    .where("user_id").is(userId)
    .and("question_bank_id").is(questionBankId);

- Add filter criteria
if (filters.getCategoryLevel1() != null) {
    // Will need to join with question_taxonomy_relationships
}
if (filters.getStatus() != null) {
    criteria.and("status").is(filters.getStatus());
}
if (filters.getQuestionType() != null) {
    criteria.and("question_type").is(filters.getQuestionType());
}
```

#### Step 10: Repository Executes Query
```java
- Create Query object
Query query = Query.query(criteria);

- Apply pagination
query.skip((long) pagination.getPage() * pagination.getSize());
query.limit(pagination.getSize());

- Apply sorting
query.with(Sort.by(
    pagination.getSortDirection(),
    pagination.getSortField()
));

- Execute query
List<QuestionReadDocument> documents = mongoTemplate.find(
    query,
    QuestionReadDocument.class,
    "questions"
);

- Count total results
Long totalCount = mongoTemplate.count(Query.query(criteria), "questions");
```

#### Step 11: Repository Maps Documents to DTOs
```java
List<QuestionQueryResultDto> results = documents.stream()
    .map(doc -> QuestionQueryResultDto.builder()
        .questionId(doc.getId().toString())
        .sourceQuestionId(doc.getSourceQuestionId())
        .questionType(doc.getQuestionType())
        .title(doc.getTitle())
        .content(doc.getContent())
        .status(doc.getStatus())
        .points(doc.getPoints())
        .displayOrder(doc.getDisplayOrder())
        // ... map all fields including taxonomy
        .build())
    .collect(Collectors.toList());
```

#### Step 12: Repository Returns Paged Result
```java
return Result.success(PagedResult.builder()
    .data(results)
    .currentPage(pagination.getPage())
    .pageSize(pagination.getSize())
    .totalElements(totalCount)
    .totalPages((int) Math.ceil((double) totalCount / pagination.getSize()))
    .build());
```

#### Step 13: Application Service Builds Response DTO
```java
QuestionListResponseDto response = QuestionListResponseDto.builder()
    .questions(pagedResult.getData())
    .pagination(PaginationDto.builder()
        .currentPage(pagedResult.getCurrentPage())
        .pageSize(pagedResult.getPageSize())
        .totalElements(pagedResult.getTotalElements())
        .totalPages(pagedResult.getTotalPages())
        .isFirst(pagedResult.getCurrentPage() == 0)
        .isLast(pagedResult.getCurrentPage() == pagedResult.getTotalPages() - 1)
        .hasNext(pagedResult.getCurrentPage() < pagedResult.getTotalPages() - 1)
        .hasPrevious(pagedResult.getCurrentPage() > 0)
        .build())
    .filters(AppliedFiltersDto.builder()
        .appliedFilters(buildAppliedFiltersMap(filters))
        .resultCount(pagedResult.getTotalElements())
        .build())
    .build();

return Result.success(response);
```

#### Step 14: Query Handler Returns to Controller
```java
return Result.success(response);
```

#### Step 15: Controller Maps to HTTP Response
```java
if (result.success()) {
    return ResponseEntity
        .ok()
        .header("X-Total-Count", result.data().getPagination().getTotalElements().toString())
        .header("X-Page-Number", result.data().getPagination().getCurrentPage().toString())
        .header("X-Page-Size", result.data().getPagination().getPageSize().toString())
        .body(result);
}
```

#### Step 16: User Receives Response
```
HTTP/1.1 200 OK
Content-Type: application/json
X-Total-Count: 156
X-Page-Number: 0
X-Page-Size: 20

{
  "success": true,
  "message": "Questions retrieved successfully",
  "data": { ... }
}
```

### Alternative Flows

#### Alternative Flow 1: Unauthorized Access (422 Unprocessable Entity)
**Diverges at Step 7**

**Step 7a: Ownership Validation Fails**
```java
boolean hasAccess = questionBanksRepository.validateOwnership(userId, questionBankId);
if (!hasAccess) {
    return Result.failure("UNAUTHORIZED_ACCESS: User does not have access to question bank");
}
```

**Step 7b: Controller Returns 422**
```java
if (!result.success() && result.message().startsWith("UNAUTHORIZED_ACCESS")) {
    return ResponseEntity
        .status(HttpStatus.UNPROCESSABLE_ENTITY)
        .body(result);
}
```

**Response:**
```json
{
  "success": false,
  "message": "UNAUTHORIZED_ACCESS: User does not have access to question bank 123",
  "data": null
}
```

#### Alternative Flow 2: Invalid Query Parameters (400 Bad Request)
**Diverges at Step 2**

**Step 2a: Parameter Validation Fails**
```java
// In controller
if (page < 0) {
    throw new IllegalArgumentException("page must be >= 0");
}
if (size < 1 || size > 100) {
    throw new IllegalArgumentException("size must be between 1 and 100");
}
if (!isValidQuestionType(questionType)) {
    throw new IllegalArgumentException("Invalid question type: " + questionType);
}
```

**Step 2b: Controller Catches Exception**
```java
catch (IllegalArgumentException ex) {
    return ResponseEntity
        .badRequest()
        .body(Result.failure("INVALID_QUERY_PARAMETER: " + ex.getMessage()));
}
```

**Response:**
```json
{
  "success": false,
  "message": "INVALID_QUERY_PARAMETER: size must be between 1 and 100",
  "data": null
}
```

#### Alternative Flow 3: Empty Results (200 OK)
**Diverges at Step 10 - Normal Flow**

**Step 10a: Query Returns No Results**
```java
List<QuestionReadDocument> documents = mongoTemplate.find(query, ...);
// documents is empty list

Long totalCount = mongoTemplate.count(Query.query(criteria), "questions");
// totalCount is 0
```

**Step 10b: Repository Returns Empty Paged Result**
```java
return Result.success(PagedResult.builder()
    .data(Collections.emptyList())
    .totalElements(0L)
    .totalPages(0)
    .build());
```

**Response:**
```json
{
  "success": true,
  "message": "No questions found matching the criteria",
  "data": {
    "questions": [],
    "pagination": {
      "totalElements": 0,
      "totalPages": 0
    }
  }
}
```

**Important**: Empty results are **SUCCESS** (200 OK), not error (404 NOT FOUND)

#### Alternative Flow 4: Database Query Error (500 Internal Server Error)
**Diverges at Step 10**

**Step 10a: MongoDB Query Fails**
```java
try {
    List<QuestionReadDocument> documents = mongoTemplate.find(query, ...);
} catch (MongoException ex) {
    logger.error("MongoDB query failed", ex);
    return Result.failure("DATABASE_ERROR: Failed to retrieve questions from database");
}
```

**Step 10b: Application Service Propagates Failure**
```java
var pagedResult = questionQueryRepository.findQuestions(...);
if (pagedResult.isFailure()) {
    return Result.failure(pagedResult.getError());
}
```

**Step 10c: Controller Returns 500**
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
  "message": "DATABASE_ERROR: Failed to retrieve questions from database",
  "data": null
}
```

## Business Rules

### BR-001: User Isolation
- **Rule**: Users can only query questions from their own question banks
- **Enforcement**: Ownership validation in application service
- **Validation**: Check via `question_banks_per_user` collection
- **Error**: 422 Unprocessable Entity if validation fails

### BR-002: Pagination Limits
- **Rule**: Maximum page size is 100 to prevent memory issues
- **Enforcement**: Controller-level validation
- **Validation**: Check `size` parameter: `1 <= size <= 100`
- **Error**: 400 Bad Request if validation fails

### BR-003: Taxonomy Filter Logic
- **Rule**: Categories use AND logic, Tags/Quizzes use OR logic
- **Enforcement**: Repository query building logic
- **Validation**: Build appropriate MongoDB query
- **Example**:
  - `category_level_1=tech AND category_level_2=prog` (both must match)
  - `tags=beginner,practice` (at least one must match)

### BR-004: Empty Results Are Success
- **Rule**: No matching questions is a valid outcome, not an error
- **Enforcement**: Return 200 OK with empty array
- **Response**: Include pagination metadata showing totalElements=0
- **Error**: Never return 404 NOT FOUND for empty results

### BR-005: Sort Field Whitelist
- **Rule**: Only specific fields allowed for sorting (security)
- **Enforcement**: Controller validation
- **Valid Fields**: `title`, `created_at`, `updated_at`, `display_order`, `points`
- **Error**: 400 Bad Request if invalid field specified

### BR-006: Read Consistency
- **Rule**: Use primary read preference for consistent reads
- **Enforcement**: MongoDB connection configuration
- **Configuration**: `spring.data.mongodb.read-preference=primary`
- **Note**: Can use `secondaryPreferred` in production for scalability

### BR-007: Response Size
- **Rule**: Questions include full taxonomy and type-specific data
- **Enforcement**: Repository mapping logic
- **Trade-off**: Larger response size vs fewer round trips
- **Future**: Consider projection or GraphQL for field selection

### BR-008: Text Search Relevance
- **Rule**: Title weighted higher than content in search results
- **Enforcement**: MongoDB text index weights
- **Configuration**: `{ title: 10, content: 5 }`
- **Sort**: Search results sorted by text score by default

### BR-009: Status Filtering
- **Rule**: Archived questions only visible when explicitly filtered
- **Enforcement**: Default filter excludes archived questions
- **Override**: User can explicitly request `status=archived`

### BR-010: Question Bank Existence
- **Rule**: Question bank must exist in user's collection
- **Enforcement**: Ownership validation checks existence
- **Error**: 422 Unprocessable Entity if question bank not found

## MongoDB Query Patterns

### Pattern 1: Simple Query (No Taxonomy Filters)
```javascript
// Use case: Filter by question properties only
db.questions.find({
  user_id: 999888777,
  question_bank_id: 1730832000000000,
  status: "published",
  question_type: "mcq"
})
.sort({ created_at: -1 })
.skip(0)
.limit(20);
```

**Performance**: Uses compound index `ix_query_primary`

### Pattern 2: Complex Query (With Taxonomy Filters)
```javascript
// Step 1: Find question IDs matching taxonomy
var taxonomyQuestionIds = db.question_taxonomy_relationships.aggregate([
  {
    $match: {
      user_id: 999888777,
      question_bank_id: 1730832000000000,
      $or: [
        { taxonomy_type: "category_level_1", taxonomy_id: "technology" },
        { taxonomy_type: "tag", taxonomy_id: { $in: ["beginner", "practice"] } }
      ]
    }
  },
  {
    $group: {
      _id: "$question_id",
      taxonomies: { $addToSet: { type: "$taxonomy_type", id: "$taxonomy_id" } }
    }
  },
  {
    $match: {
      // Ensure category is present (AND logic)
      "taxonomies.type": "category_level_1"
      // Tags will be OR logic (already handled in $or)
    }
  }
]).map(doc => doc._id);

// Step 2: Query questions with those IDs
db.questions.find({
  _id: { $in: taxonomyQuestionIds },
  user_id: 999888777,
  question_bank_id: 1730832000000000
})
.sort({ title: 1 })
.skip(0)
.limit(20);
```

**Performance**: Uses `ix_taxonomy_query` index for relationships

### Pattern 3: Full-Text Search Query
```javascript
db.questions.find({
  $text: { $search: "array methods" },
  user_id: 999888777,
  question_bank_id: 1730832000000000
})
.projection({ score: { $meta: "textScore" } })
.sort({ score: { $meta: "textScore" } })
.skip(0)
.limit(20);
```

**Performance**: Uses text index `ix_full_text_search`

### Pattern 4: Count Query for Pagination
```javascript
db.questions.countDocuments({
  user_id: 999888777,
  question_bank_id: 1730832000000000,
  status: "published"
});
```

**Performance**: Uses same indexes as find queries

## Non-Functional Requirements

### Performance
- **Response Time**: p95 < 500ms, p99 < 1000ms
- **Throughput**: Support 100 concurrent read requests
- **MongoDB Query Time**: < 200ms (p95)
- **Index Utilization**: 100% (no collection scans)

### Scalability
- **Read Replicas**: Use secondaryPreferred in production
- **Connection Pooling**: Minimum 10, maximum 100 connections
- **Page Size Limit**: Maximum 100 items per page
- **Caching**: Future enhancement for taxonomy sets

### Reliability
- **Read Consistency**: Primary read preference ensures consistency
- **Timeout**: 30 seconds query timeout
- **Error Recovery**: Graceful degradation on MongoDB failure
- **Monitoring**: Track slow queries (> 1 second)

### Security
- **Authentication**: JWT token validation (when enabled)
- **Authorization**: User can only access own question banks
- **Input Validation**: Sanitize query parameters
- **SQL Injection**: Not applicable (MongoDB)
- **NoSQL Injection**: Parameterized queries prevent injection

### Testability
- **Testcontainers**: MongoDB integration testing
- **Test Data**: Comprehensive fixture data
- **Mocking**: Repository mocks for unit tests
- **E2E Tests**: Full HTTP → MongoDB flow

## Query Optimization Strategy

### Index Coverage Analysis
```javascript
// Check if query uses index
db.questions.find({
  user_id: 999888777,
  question_bank_id: 1730832000000000,
  status: "published"
}).explain("executionStats");

// Expected output:
// - executionStages.stage: "IXSCAN" (index scan)
// - totalKeysExamined: should equal nReturned (covered query)
// - totalDocsExamined: 0 (fully covered by index)
```

### Query Performance Metrics
- **Index Hit Rate**: Target > 99%
- **Collection Scan Rate**: Target < 1%
- **Query Execution Time**: Monitor via MongoDB slow query log
- **Memory Usage**: Ensure queries fit in working set

### Caching Strategy (Future)
```java
@Cacheable(value = "questions", key = "#userId + '_' + #questionBankId + '_' + #filters.hashCode()")
public Result<QuestionListResponseDto> queryQuestions(...) {
    // Query implementation
}
```

**Cache Invalidation**:
- Invalidate on question create/update/delete
- TTL: 5 minutes for frequently accessed data
- Cache size: Maximum 1000 entries per user

This comprehensive use case design provides all specifications needed for implementing the query functionality with clear business rules, error handling, and performance requirements.
