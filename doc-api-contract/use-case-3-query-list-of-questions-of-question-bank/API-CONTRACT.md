# API Contract: Query List of Questions from Question Bank

## Overview

### Use Case Description
This endpoint enables content creators and educators to retrieve a paginated, filtered list of questions from their question banks. The API supports advanced filtering by taxonomy elements (categories, tags, quizzes, difficulty), question properties (type, status), and full-text search capabilities.

### Business Purpose
- Enable efficient question discovery and browsing through rich filtering options
- Support content organization by taxonomy (categories, tags, difficulty levels)
- Provide pagination for large question collections
- Enable full-text search across question titles and content
- Optimize query performance through MongoDB compound indexes

### Primary Actors
- **Content Creator/Educator**: Browses and searches questions in their question banks
- **Quiz CMS System**: Retrieves and presents filtered question data

### Architecture Pattern
- **CQRS**: Query pattern with mediator routing (read-side implementation)
- **Hexagonal**: Ports and adapters without domain layer (direct repository access)
- **No DDD**: Query side focuses on data retrieval, not business logic
- **Testcontainer**: MongoDB 8.0 for TDD with secondary instance simulation

---

## HTTP Endpoint Specification

### Base Information
```http
GET /api/users/{userId}/questionbanks/{questionbankId}/questions
```

### Path Parameters

| Parameter | Type | Required | Validation | Description |
|-----------|------|----------|------------|-------------|
| `userId` | Long | ✅ Yes | Must be positive (> 0) | User identifier - **MUST match JWT token userId** when security is enabled |
| `questionbankId` | Long | ✅ Yes | Must be positive (> 0) | Target question bank identifier |

**Security Note**: Same JWT-path matching validation as command endpoints applies when Spring Security is enabled.

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

**Taxonomy Filter Logic**:
- Category filters use **AND logic** across levels (all specified categories must match)
- Tags use **OR logic** (question must have at least one of the specified tags)
- Quizzes use **OR logic** (question must belong to at least one of the specified quizzes)
- All taxonomy filters combined use **AND logic**

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
- Multiple sorts: Use multiple `sort` parameters
- Supported fields: `title`, `created_at`, `updated_at`, `display_order`, `points`

**Examples**:
```
?sort=title,asc                    # Sort by title ascending
?sort=created_at,desc              # Sort by creation date descending (default)
?sort=display_order,asc&sort=title,asc  # Multi-field sort
```

### Request Headers
```http
Accept: application/json
Authorization: Bearer <JWT_TOKEN>  (when security is enabled)
```

---

## Request Examples

### Example 1: Basic Query - Get First Page
```http
GET /api/users/999888777/questionbanks/1730832000000000/questions
```

**Response**: Returns first 20 questions sorted by creation date (most recent first)

### Example 2: Filter by Category Hierarchy
```http
GET /api/users/999888777/questionbanks/1730832000000000/questions?category_level_1=technology&category_level_2=programming&category_level_3=javascript
```

**Logic**: Returns questions tagged with Technology → Programming → JavaScript

### Example 3: Filter by Multiple Tags (OR logic)
```http
GET /api/users/999888777/questionbanks/1730832000000000/questions?tags=beginner,practice
```

**Logic**: Returns questions that have **either** "beginner" **OR** "practice" tags

### Example 4: Filter by Question Type and Status
```http
GET /api/users/999888777/questionbanks/1730832000000000/questions?question_type=mcq&status=published
```

**Logic**: Returns only published MCQ questions

### Example 5: Filter by Difficulty Level
```http
GET /api/users/999888777/questionbanks/1730832000000000/questions?difficulty_level=easy
```

**Logic**: Returns questions marked as "easy" difficulty

### Example 6: Full-Text Search
```http
GET /api/users/999888777/questionbanks/1730832000000000/questions?search=array methods
```

**Logic**: Returns questions containing "array methods" in title or content

### Example 7: Complex Combined Filters
```http
GET /api/users/999888777/questionbanks/1730832000000000/questions?category_level_1=general&tags=beginner,practice&question_type=mcq&difficulty_level=easy&status=published&page=0&size=10&sort=title,asc
```

**Logic**:
- Category: general
- **AND** (tags: beginner **OR** practice)
- **AND** type: MCQ
- **AND** difficulty: easy
- **AND** status: published
- Page 1, 10 items per page, sorted by title ascending

### Example 8: Pagination - Second Page
```http
GET /api/users/999888777/questionbanks/1730832000000000/questions?page=1&size=50
```

**Logic**: Returns items 51-100 (page is zero-indexed)

### Example 9: Filter by Quiz Membership
```http
GET /api/users/999888777/questionbanks/1730832000000000/questions?quizzes=101,102,103
```

**Logic**: Returns questions that belong to quiz 101 **OR** 102 **OR** 103

### Example 10: Sort by Multiple Fields
```http
GET /api/users/999888777/questionbanks/1730832000000000/questions?sort=display_order,asc&sort=title,asc
```

**Logic**: Primary sort by display_order, secondary sort by title (both ascending)

---

## Response Specifications

### Success Response (200 OK)

#### Response Structure
```json
{
  "success": true,
  "message": "Questions retrieved successfully",
  "data": {
    "questions": [
      {
        "questionId": "507f1f77bcf86cd799439014",
        "sourceQuestionId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
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
            { "quizId": 101, "quizName": "JavaScript Fundamentals Quiz", "quizSlug": "js-fundamentals" }
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

#### Field Descriptions

##### Question Object Fields

| Field | Type | Description |
|-------|------|-------------|
| `questionId` | String | MongoDB ObjectId of the question |
| `sourceQuestionId` | String | External identifier for the question |
| `questionType` | String | Question type: "mcq", "true_false", "essay" |
| `title` | String | Question title |
| `content` | String | Question content/prompt (HTML allowed) |
| `status` | String | Lifecycle status: "draft", "published", "archived" |
| `points` | Integer | Points awarded for correct answer (nullable) |
| `displayOrder` | Integer | Display order within question bank (nullable) |
| `solutionExplanation` | String | Explanation shown after answering (nullable) |
| `taxonomy` | Object | Complete taxonomy information with relationships |
| `mcqData` | Object | MCQ-specific data (only present for MCQ questions) |
| `trueFalseData` | Object | True/False data (only present for True/False questions) |
| `essayData` | Object | Essay data (only present for Essay questions) |
| `attachments` | Array | Media attachments (images, files) |
| `metadata` | Object | Additional metadata (version, author, source) |
| `createdAt` | String | ISO-8601 timestamp of creation |
| `updatedAt` | String | ISO-8601 timestamp of last update |
| `publishedAt` | String | ISO-8601 timestamp when published (nullable) |
| `archivedAt` | String | ISO-8601 timestamp when archived (nullable) |

##### Pagination Object Fields

| Field | Type | Description |
|-------|------|-------------|
| `currentPage` | Integer | Current page number (zero-indexed) |
| `pageSize` | Integer | Number of items per page |
| `totalElements` | Long | Total number of questions matching the query |
| `totalPages` | Integer | Total number of pages |
| `isFirst` | Boolean | True if this is the first page |
| `isLast` | Boolean | True if this is the last page |
| `hasNext` | Boolean | True if there is a next page |
| `hasPrevious` | Boolean | True if there is a previous page |

##### Filters Object Fields

| Field | Type | Description |
|-------|------|-------------|
| `appliedFilters` | Object | Map of all applied filters (null values omitted) |
| `resultCount` | Long | Number of questions matching the filters |

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

**Note**: Empty results are **NOT an error** - they return 200 OK with an empty array.

---

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

---

## HTTP Status Code Reference

| Status Code | Scenario | Error Code |
|-------------|----------|------------|
| 200 OK | Success - questions retrieved (even if empty) | - |
| 400 Bad Request | Invalid query parameters | INVALID_QUERY_PARAMETER |
| 400 Bad Request | Invalid path parameters | CONSTRAINT_VIOLATION |
| 422 Unprocessable Entity | Unauthorized access | UNAUTHORIZED_ACCESS |
| 422 Unprocessable Entity | Question bank not found | QUESTION_BANK_NOT_FOUND |
| 500 Internal Server Error | Database error | DATABASE_ERROR |

---

## MongoDB Query Implementation

### Collection Access

The query will primarily read from the `questions` collection, with joins to `question_taxonomy_relationships` collection for taxonomy filtering.

### Compound Index Strategy

#### Primary Query Index
```javascript
db.questions.createIndex(
  {
    user_id: 1,
    question_bank_id: 1,
    status: 1,
    created_at: -1
  },
  { name: "ix_query_primary" }
);
```

**Purpose**: Supports most common query pattern (filter by user, bank, status, sorted by date)

#### Taxonomy Relationship Index
```javascript
db.question_taxonomy_relationships.createIndex(
  {
    user_id: 1,
    question_bank_id: 1,
    taxonomy_type: 1,
    taxonomy_id: 1
  },
  { name: "ix_taxonomy_query" }
);
```

**Purpose**: Efficient taxonomy filtering via joins/aggregations

#### Text Search Index
```javascript
db.questions.createIndex(
  {
    title: "text",
    content: "text"
  },
  {
    name: "ix_full_text_search",
    weights: { title: 10, content: 5 }
  }
);
```

**Purpose**: Full-text search with title weighted higher than content

### Query Pattern Examples

#### Simple Query (No Taxonomy Filters)
```javascript
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

#### Complex Query with Taxonomy Filters
```javascript
// Step 1: Find question IDs matching taxonomy criteria
const taxonomyQuestionIds = db.question_taxonomy_relationships.aggregate([
  {
    $match: {
      user_id: 999888777,
      question_bank_id: 1730832000000000,
      $or: [
        { taxonomy_type: "category_level_1", taxonomy_id: "technology" },
        { taxonomy_type: "tag", taxonomy_id: { $in: ["beginner", "practice"] } },
        { taxonomy_type: "difficulty_level", taxonomy_id: "easy" }
      ]
    }
  },
  {
    $group: {
      _id: "$question_id",
      matchedTaxonomies: { $addToSet: { type: "$taxonomy_type", id: "$taxonomy_id" } }
    }
  },
  {
    $match: {
      // Ensure all required taxonomy types are present (AND logic for categories)
      "matchedTaxonomies.type": { $all: ["category_level_1", "difficulty_level"] }
    }
  }
]).toArray().map(doc => doc._id);

// Step 2: Query questions with those IDs
db.questions.find({
  _id: { $in: taxonomyQuestionIds },
  user_id: 999888777,
  question_bank_id: 1730832000000000,
  question_type: "mcq"
})
.sort({ title: 1 })
.skip(0)
.limit(20);
```

#### Full-Text Search Query
```javascript
db.questions.find({
  $text: { $search: "array methods" },
  user_id: 999888777,
  question_bank_id: 1730832000000000
})
.sort({ score: { $meta: "textScore" } })
.skip(0)
.limit(20);
```

### Performance Considerations

1. **Index Usage**: All queries use compound indexes starting with `user_id` and `question_bank_id`
2. **Pagination**: Use skip/limit for simple pagination; consider cursor-based for very large datasets
3. **Taxonomy Joins**: For complex taxonomy filters, consider using aggregation pipeline
4. **Text Search**: Text indexes have performance overhead; consider separate search service for production
5. **Read Preference**: Use `primary` or `primaryPreferred` for MongoDB replica sets

---

## Package Naming Convention

### Query Module Package Structure

**Root Package**: `com.quizfun.questionbankquery` (note: different from command module's `com.quizfun.questionbank`)

```
com.quizfun.questionbankquery
├── application
│   ├── dto
│   │   ├── QuestionListResponseDto.java
│   │   ├── QuestionQueryResultDto.java
│   │   ├── PaginationDto.java
│   │   └── AppliedFiltersDto.java
│   ├── ports
│   │   ├── in
│   │   │   └── QueryQuestionsService.java (interface)
│   │   └── out
│   │       └── QuestionQueryRepository.java (interface)
│   ├── queries
│   │   └── QueryQuestionsQuery.java (implements IQuery<T>)
│   └── services
│       └── DefaultQueryQuestionsService.java (implements QueryQuestionsService)
├── infrastructure
│   ├── persistence
│   │   ├── documents
│   │   │   └── QuestionReadDocument.java (read-optimized document)
│   │   └── repositories
│   │       └── MongoQuestionQueryRepository.java (implements QuestionQueryRepository)
│   └── configuration
│       └── QueryConfiguration.java
└── orchestration
    └── handlers
        └── QueryQuestionsQueryHandler.java (implements IQueryHandler<Q, T>)
```

**Key Naming Decisions**:
- Use `query` suffix in package root to distinguish from command module
- No `domain` layer since query side doesn't enforce business rules
- Repository reads optimized read documents, not domain aggregates
- Query handlers placed in orchestration submodule's handler package

---

## K6 Test Considerations

### Authentication
Same authentication requirements as command endpoints:
- **Current State**: Security bypassed, no JWT required
- **Future Production**: JWT token required with userId matching path parameter

### Prerequisites
1. **MongoDB is running** with test data
2. **Application started**: `mvn spring-boot:run -pl orchestration-layer`
3. **Test data exists**:
   - User with ID 999888777
   - Question bank with ID 1730832000000000
   - Multiple questions with various taxonomy relationships
   - Mix of question types (MCQ, True/False, Essay)
   - Mix of statuses (draft, published)

### Test Data Setup Script

```javascript
// K6 Setup Function - Create Test Data
export function setup() {
  const BASE_URL = 'http://localhost:8765';
  const userId = 999888777;

  // Step 1: Create default question bank
  const bankRes = http.post(`${BASE_URL}/api/users/default-question-bank`,
    JSON.stringify({ userId: userId }),
    { headers: { 'Content-Type': 'application/json' } }
  );
  const questionbankId = JSON.parse(bankRes.body).data.questionBankId;

  // Step 2: Create sample questions with different properties
  const questionTypes = ['mcq', 'true_false', 'essay'];
  const difficulties = ['easy', 'medium', 'hard'];
  const statuses = ['draft', 'published'];
  const tags = [['beginner'], ['practice'], ['beginner', 'practice']];

  for (let i = 0; i < 30; i++) {
    const questionType = questionTypes[i % 3];
    const difficulty = difficulties[i % 3];
    const status = statuses[i % 2];
    const tagSet = tags[i % 3];

    const payload = createQuestionPayload(
      `test-question-${i}`,
      questionType,
      difficulty,
      status,
      tagSet
    );

    http.post(
      `${BASE_URL}/api/users/${userId}/questionbanks/${questionbankId}/questions`,
      JSON.stringify(payload),
      { headers: { 'Content-Type': 'application/json' } }
    );
  }

  return { userId, questionbankId };
}
```

### Common K6 Test Scenarios

#### Scenario 1: Happy Path - Get First Page
```javascript
import http from 'k6/http';
import { check } from 'k6';

export default function(data) {
  const url = `http://localhost:8765/api/users/${data.userId}/questionbanks/${data.questionbankId}/questions`;

  const res = http.get(url);

  check(res, {
    'status is 200': (r) => r.status === 200,
    'success is true': (r) => JSON.parse(r.body).success === true,
    'has questions array': (r) => Array.isArray(JSON.parse(r.body).data.questions),
    'has pagination info': (r) => JSON.parse(r.body).data.pagination !== null,
    'response time < 500ms': (r) => r.timings.duration < 500
  });
}
```

#### Scenario 2: Filter by Category
```javascript
export default function(data) {
  const url = `http://localhost:8765/api/users/${data.userId}/questionbanks/${data.questionbankId}/questions?category_level_1=general`;

  const res = http.get(url);

  check(res, {
    'status is 200': (r) => r.status === 200,
    'applied filters include category': (r) => {
      const body = JSON.parse(r.body);
      return body.data.filters.appliedFilters.categoryLevel1 === 'general';
    }
  });
}
```

#### Scenario 3: Filter by Multiple Tags (OR Logic)
```javascript
export default function(data) {
  const url = `http://localhost:8765/api/users/${data.userId}/questionbanks/${data.questionbankId}/questions?tags=beginner,practice`;

  const res = http.get(url);

  check(res, {
    'status is 200': (r) => r.status === 200,
    'all questions have at least one tag': (r) => {
      const questions = JSON.parse(r.body).data.questions;
      return questions.every(q =>
        q.taxonomy.tags.some(t => ['beginner', 'practice'].includes(t.id))
      );
    }
  });
}
```

#### Scenario 4: Filter by Question Type and Status
```javascript
export default function(data) {
  const url = `http://localhost:8765/api/users/${data.userId}/questionbanks/${data.questionbankId}/questions?question_type=mcq&status=published`;

  const res = http.get(url);

  check(res, {
    'status is 200': (r) => r.status === 200,
    'all questions are MCQ': (r) => {
      const questions = JSON.parse(r.body).data.questions;
      return questions.every(q => q.questionType === 'mcq');
    },
    'all questions are published': (r) => {
      const questions = JSON.parse(r.body).data.questions;
      return questions.every(q => q.status === 'published');
    }
  });
}
```

#### Scenario 5: Pagination - Navigate Pages
```javascript
export default function(data) {
  const baseUrl = `http://localhost:8765/api/users/${data.userId}/questionbanks/${data.questionbankId}/questions`;

  // First page
  const page1 = http.get(`${baseUrl}?page=0&size=10`);
  check(page1, {
    'page 1 - status is 200': (r) => r.status === 200,
    'page 1 - is first page': (r) => JSON.parse(r.body).data.pagination.isFirst === true,
    'page 1 - has next page': (r) => JSON.parse(r.body).data.pagination.hasNext === true
  });

  // Second page
  const page2 = http.get(`${baseUrl}?page=1&size=10`);
  check(page2, {
    'page 2 - status is 200': (r) => r.status === 200,
    'page 2 - not first page': (r) => JSON.parse(r.body).data.pagination.isFirst === false,
    'page 2 - has previous page': (r) => JSON.parse(r.body).data.pagination.hasPrevious === true
  });
}
```

#### Scenario 6: Full-Text Search
```javascript
export default function(data) {
  const searchTerm = 'array methods';
  const url = `http://localhost:8765/api/users/${data.userId}/questionbanks/${data.questionbankId}/questions?search=${encodeURIComponent(searchTerm)}`;

  const res = http.get(url);

  check(res, {
    'status is 200': (r) => r.status === 200,
    'results contain search term': (r) => {
      const questions = JSON.parse(r.body).data.questions;
      return questions.some(q =>
        q.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
        q.content.toLowerCase().includes(searchTerm.toLowerCase())
      );
    }
  });
}
```

#### Scenario 7: Sort by Different Fields
```javascript
export default function(data) {
  const url = `http://localhost:8765/api/users/${data.userId}/questionbanks/${data.questionbankId}/questions?sort=title,asc`;

  const res = http.get(url);

  check(res, {
    'status is 200': (r) => r.status === 200,
    'results are sorted by title': (r) => {
      const questions = JSON.parse(r.body).data.questions;
      for (let i = 1; i < questions.length; i++) {
        if (questions[i].title < questions[i-1].title) {
          return false;
        }
      }
      return true;
    }
  });
}
```

#### Scenario 8: Invalid Query Parameters
```javascript
export default function(data) {
  const url = `http://localhost:8765/api/users/${data.userId}/questionbanks/${data.questionbankId}/questions?page=-1`;

  const res = http.get(url);

  check(res, {
    'status is 400': (r) => r.status === 400,
    'success is false': (r) => JSON.parse(r.body).success === false,
    'error message contains INVALID_QUERY_PARAMETER': (r) =>
      JSON.parse(r.body).message.includes('INVALID_QUERY_PARAMETER')
  });
}
```

#### Scenario 9: Empty Results (Valid but No Matches)
```javascript
export default function(data) {
  const url = `http://localhost:8765/api/users/${data.userId}/questionbanks/${data.questionbankId}/questions?category_level_1=nonexistent`;

  const res = http.get(url);

  check(res, {
    'status is 200': (r) => r.status === 200,
    'success is true': (r) => JSON.parse(r.body).success === true,
    'questions array is empty': (r) => JSON.parse(r.body).data.questions.length === 0,
    'total elements is 0': (r) => JSON.parse(r.body).data.pagination.totalElements === 0
  });
}
```

#### Scenario 10: Load Test - Concurrent Reads
```javascript
import { check } from 'k6';

export let options = {
  stages: [
    { duration: '30s', target: 20 },  // Ramp up
    { duration: '1m', target: 20 },   // Stay at 20 concurrent users
    { duration: '30s', target: 0 },   // Ramp down
  ],
};

export default function(data) {
  const filters = [
    '?question_type=mcq',
    '?status=published',
    '?difficulty_level=easy',
    '?tags=beginner',
    '?page=0&size=50',
    '?sort=title,asc'
  ];

  const randomFilter = filters[Math.floor(Math.random() * filters.length)];
  const url = `http://localhost:8765/api/users/${data.userId}/questionbanks/${data.questionbankId}/questions${randomFilter}`;

  const res = http.get(url);

  check(res, {
    'status is 200': (r) => r.status === 200,
    'response time < 500ms': (r) => r.timings.duration < 500,
    'has questions': (r) => JSON.parse(r.body).data !== null
  });
}
```

### Key Metrics to Track
- **Response time** (p95 should be < 500ms, p99 < 1000ms)
- **Success rate** (should be 100% for valid requests)
- **Throughput** (requests per second)
- **Pagination accuracy** (totalElements, page counts)
- **Filter accuracy** (results match filter criteria)
- **MongoDB query performance** (via monitoring tools)

---

## Code References (To Be Implemented)

### Query Handler (Orchestration Layer)
- **File**: `orchestration-layer/src/main/java/com/quizfun/orchestrationlayer/handlers/QueryQuestionsQueryHandler.java`
- **Implements**: `IQueryHandler<QueryQuestionsQuery, QuestionListResponseDto>`

### Query Controller (Orchestration Layer)
- **File**: `orchestration-layer/src/main/java/com/quizfun/orchestrationlayer/controllers/QuestionQueryController.java`
- **Method**: `queryQuestions` with @GetMapping

### Query Application Service (Query Module)
- **File**: `internal-layer/question-bank-query/src/main/java/com/quizfun/questionbankquery/application/services/DefaultQueryQuestionsService.java`
- **Implements**: Port-in interface `QueryQuestionsService`

### Query Repository (Query Module)
- **File**: `internal-layer/question-bank-query/src/main/java/com/quizfun/questionbankquery/infrastructure/persistence/repositories/MongoQuestionQueryRepository.java`
- **Implements**: Port-out interface `QuestionQueryRepository`

### Test Configuration
- **File**: `internal-layer/question-bank-query/src/test/java/com/quizfun/questionbankquery/config/TestContainersMongoConfig.java`
- **Purpose**: Testcontainer MongoDB 8.0 setup for integration tests

---

## Business Rules Summary

1. **User Isolation (BR-001)**: Users can only query questions from their own question banks
2. **Ownership Validation (BR-002)**: Question bank must exist and be owned by the user
3. **Pagination Limits (BR-003)**: Maximum page size is 100 to prevent memory issues
4. **Taxonomy Filter Logic (BR-004)**:
   - Categories: AND logic across levels
   - Tags: OR logic within tag list
   - Quizzes: OR logic within quiz list
   - Combined: AND logic between different taxonomy types
5. **Empty Results (BR-005)**: Valid filters with no matches return 200 OK with empty array
6. **Sort Fields (BR-006)**: Only specific fields allowed for sorting (whitelist approach)
7. **Read Consistency (BR-007)**: Use primary read preference for MongoDB replica sets
8. **Response Size (BR-008)**: Questions include full taxonomy and type-specific data
9. **Text Search (BR-009)**: Title weighted higher than content in search relevance
10. **Status Filtering (BR-010)**: Archived questions only visible when explicitly filtered

---

## Implementation Notes

### Testcontainer MongoDB Setup

Unlike the command module, the query module should use a simpler Testcontainer setup:

```java
@Testcontainers
@SpringBootTest
public class QueryQuestionsIntegrationTest {

    @Container
    static MongoDBContainer mongoContainer = new MongoDBContainer("mongo:8.0")
            .withExposedPorts(27018)  // Simulate secondary instance
            .withReuse(false);

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoContainer::getReplicaSetUrl);
        registry.add("spring.data.mongodb.read-preference", () -> "primary");
    }

    // Test methods...
}
```

**Key Differences from Command Module**:
- No need for manual bean creation (MongoClient, MongoDatabaseFactory, MongoTemplate)
- Spring Boot auto-configuration handles MongoDB setup
- Use `@DynamicPropertySource` to hijack application properties
- Simpler configuration for TDD workflow

### Query Optimization Strategy

1. **Denormalize for Read Performance**: Consider storing taxonomy data directly in question documents for faster reads (trade-off: consistency overhead)
2. **Caching Layer**: Implement Redis/Caffeine cache for frequently accessed question lists
3. **Index Monitoring**: Use MongoDB slow query log to identify missing indexes
4. **Aggregation Pipeline**: Use for complex taxonomy joins instead of application-side joins
5. **Projection**: Only select required fields to reduce network transfer
6. **Connection Pooling**: Configure appropriate MongoDB connection pool size for read workloads

### Security Considerations

Same security model as command endpoints:
- JWT token validation when Spring Security is enabled
- Path parameter userId must match JWT subject claim
- Question bank ownership validation via chain of responsibility
- Audit logging for query patterns (optional)

### Future Enhancements

1. **Cursor-Based Pagination**: For better performance on large datasets
2. **GraphQL Support**: For flexible field selection
3. **Export Functionality**: CSV/Excel export of filtered questions
4. **Faceted Search**: Provide taxonomy facet counts alongside results
5. **Saved Filters**: Allow users to save and reuse filter combinations
6. **Real-Time Updates**: WebSocket support for live question list updates

---

## Appendix: Complete Query Parameter Matrix

| Category | Parameter | Type | Required | Default | Example |
|----------|-----------|------|----------|---------|---------|
| **Path** | userId | Long | Yes | - | 999888777 |
| **Path** | questionbankId | Long | Yes | - | 1730832000000000 |
| **Taxonomy** | category_level_1 | String | No | null | general |
| **Taxonomy** | category_level_2 | String | No | null | programming |
| **Taxonomy** | category_level_3 | String | No | null | javascript |
| **Taxonomy** | category_level_4 | String | No | null | arrays |
| **Taxonomy** | tags | String | No | null | beginner,practice |
| **Taxonomy** | quizzes | String | No | null | 101,102 |
| **Taxonomy** | difficulty_level | String | No | null | easy |
| **Filter** | question_type | Enum | No | null | mcq |
| **Filter** | status | Enum | No | null | published |
| **Filter** | search | String | No | null | array methods |
| **Pagination** | page | Integer | No | 0 | 0 |
| **Pagination** | size | Integer | No | 20 | 50 |
| **Pagination** | sort | String | No | created_at,desc | title,asc |

---

**Document Version**: 1.0
**Last Updated**: 2025-10-11
**Status**: Design Complete - Ready for Implementation
