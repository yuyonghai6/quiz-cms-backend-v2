# Architecture Overview - Query List of Questions Use Case

## System Context

### CQRS Architectural Separation

This use case implements the **Query side** of the CQRS pattern, separate from the command side. The key architectural principle is **read optimization** without the complexity of domain-driven design.

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        Quiz CMS System (CQRS)                           │
│                                                                          │
│  ┌────────────────────────────────────────────────────────────────┐    │
│  │  COMMAND SIDE (Write Operations - DDD)                         │    │
│  │  - internal-layer/question-bank                                │    │
│  │  - Domain layer with aggregates                                │    │
│  │  - Complex business logic                                      │    │
│  │  - Transaction management                                      │    │
│  │  - POST /api/users/{userId}/questionbanks/{qbId}/questions     │    │
│  └────────────────────────────────────────────────────────────────┘    │
│                                                                          │
│  ┌────────────────────────────────────────────────────────────────┐    │
│  │  QUERY SIDE (Read Operations - NO DDD) ← THIS USE CASE        │    │
│  │  - internal-layer/question-bank-query                          │    │
│  │  - NO domain layer                                             │    │
│  │  - Direct repository access                                    │    │
│  │  - Read-optimized queries                                      │    │
│  │  - GET /api/users/{userId}/questionbanks/{qbId}/questions      │    │
│  └────────────────────────────────────────────────────────────────┘    │
│                                                                          │
│  ┌────────────────────────────────────────────────────────────────┐    │
│  │  SHARED INFRASTRUCTURE                                          │    │
│  │  - global-shared-library (Mediator, Result<T>)                 │    │
│  │  - orchestration-layer (Controllers, Handlers)                 │    │
│  └────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────┘
```

## Query-Side Architecture (Simplified)

### High-Level Flow
```
HTTP GET Request with Query Parameters
    │
    ▼
┌─────────────────────────────────────────────────────────────┐
│  orchestration-layer                                        │
│  ┌─────────────────┐                                        │
│  │  Controller     │ 1. Extract path & query params         │
│  │  (GET endpoint) │ 2. Build QueryQuestionsQuery object    │
│  └─────────────────┘                                        │
│           │                                                 │
│           ▼                                                 │
│  ┌─────────────────┐                                        │
│  │    Mediator     │ 3. Route to QueryQuestionsQueryHandler │
│  └─────────────────┘                                        │
│           │                                                 │
│           ▼                                                 │
│  ┌─────────────────┐                                        │
│  │  Query Handler  │ 4. Invoke query application service   │
│  └─────────────────┘                                        │
└─────────────────────────────────────────────────────────────┘
           │
           ▼ (Inject Application Service - Port IN)
┌─────────────────────────────────────────────────────────────┐
│  internal-layer/question-bank-query                         │
│  ┌─────────────────┐                                        │
│  │  Application    │ 5. Validate userId/questionBankId     │
│  │  Service        │ 6. Build MongoDB query/aggregation    │
│  │  (Port IN)      │ 7. Apply filters and pagination       │
│  └─────────────────┘                                        │
│           │                                                 │
│           ▼                                                 │
│  ┌─────────────────┐                                        │
│  │  Repository     │ 8. Execute MongoDB query              │
│  │  (Port OUT)     │ 9. Map documents to DTOs              │
│  │  NO Domain!     │ 10. Return paginated results          │
│  └─────────────────┘                                        │
└─────────────────────────────────────────────────────────────┘
           │
           ▼
┌─────────────────────────────────────────────────────────────┐
│  MongoDB (Testcontainers - Secondary Read Preference)       │
│  ┌─────────────────┬──────────────────┬─────────────────────┐ │
│  │    questions    │ question_taxonomy│ taxonomy_sets      │ │
│  │   (read-only)   │ _relationships   │   (read-only)      │ │
│  │                 │   (read-only)    │                    │ │
│  └─────────────────┴──────────────────┴─────────────────────┘ │
│                                                             │
│  Indexes for Query Performance:                            │
│  - { user_id: 1, question_bank_id: 1, status: 1 }          │
│  - { user_id: 1, question_bank_id: 1, created_at: -1 }     │
│  - Text index on { title: "text", content: "text" }        │
└─────────────────────────────────────────────────────────────┘
```

## Key Architectural Differences from Command Side

### 1. No Domain Layer
```
Command Side (DDD):
internal-layer/question-bank/
├── domain/
│   ├── aggregates/       ← Has complex business logic
│   ├── entities/
│   └── services/
├── application/
└── infrastructure/

Query Side (Simplified):
internal-layer/question-bank-query/
├── application/          ← Direct repository access
│   ├── dto/
│   ├── ports/
│   │   ├── in/          ← QueryQuestionsService
│   │   └── out/         ← QuestionQueryRepository
│   └── services/
└── infrastructure/       ← No domain mapping
    ├── persistence/
    │   ├── documents/   ← Read-optimized documents
    │   └── repositories/
    └── configuration/
```

**Rationale**: Queries don't enforce business rules or modify state, so domain complexity is unnecessary.

### 2. Package Naming Strategy

**Command Module**:
- Root package: `com.quizfun.questionbank`
- Example: `com.quizfun.questionbank.domain.aggregates.QuestionAggregate`

**Query Module** (Avoid name clashes):
- Root package: `com.quizfun.questionbankquery` ← Note: `query` suffix
- Example: `com.quizfun.questionbankquery.application.services.QueryQuestionsService`

**Orchestration Layer** (Query-specific):
- Query controller: `com.quizfun.orchestrationlayer.controllers.QuestionQueryController`
- Query handler: `com.quizfun.orchestrationlayer.handlers.QueryQuestionsQueryHandler`

### 3. Read-Optimized MongoDB Access

#### Command Side (Write Path)
```java
@Transactional
public Result<QuestionResponseDto> upsertQuestion(UpsertQuestionCommand command) {
    // Complex: Aggregates, validation chains, transactions
    var aggregate = QuestionAggregate.createNew(command);
    questionRepository.upsertBySourceQuestionId(aggregate);
    relationshipRepository.replaceRelationshipsForQuestion(...);
}
```

#### Query Side (Read Path)
```java
public Result<QuestionListResponseDto> queryQuestions(QueryQuestionsQuery query) {
    // Simple: Direct MongoDB queries with filters
    Query mongoQuery = buildMongoQuery(query);
    List<QuestionReadDocument> documents = mongoTemplate.find(mongoQuery, ...);
    return Result.success(mapToDto(documents));
}
```

## Hexagonal Architecture (Ports & Adapters)

### Application Layer Ports

#### Port IN (Application Service Interface)
```java
// File: application/ports/in/QueryQuestionsService.java
public interface QueryQuestionsService {
    Result<QuestionListResponseDto> queryQuestions(
        Long userId,
        Long questionBankId,
        QuestionFilters filters,
        PaginationParams pagination
    );
}
```

#### Port OUT (Repository Interface)
```java
// File: application/ports/out/QuestionQueryRepository.java
public interface QuestionQueryRepository {
    Result<PagedResult<QuestionQueryResultDto>> findQuestions(
        Long userId,
        Long questionBankId,
        QuestionFilters filters,
        PaginationParams pagination
    );

    Long countQuestions(Long userId, Long questionBankId, QuestionFilters filters);
}
```

### Infrastructure Layer Adapters

#### MongoDB Repository Adapter
```java
// File: infrastructure/persistence/repositories/MongoQuestionQueryRepository.java
@Repository
public class MongoQuestionQueryRepository implements QuestionQueryRepository {

    private final MongoTemplate mongoTemplate;

    @Override
    public Result<PagedResult<QuestionQueryResultDto>> findQuestions(...) {
        try {
            // Build MongoDB query
            Query query = buildQuery(userId, questionBankId, filters);

            // Apply pagination
            query.skip(pagination.getOffset()).limit(pagination.getSize());

            // Apply sorting
            query.with(Sort.by(pagination.getSortDirection(), pagination.getSortField()));

            // Execute query
            List<QuestionReadDocument> documents = mongoTemplate.find(
                query,
                QuestionReadDocument.class,
                "questions"
            );

            // Map to DTOs
            List<QuestionQueryResultDto> results = documents.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

            return Result.success(new PagedResult<>(results, ...));

        } catch (Exception ex) {
            return Result.failure("QUERY_ERROR: " + ex.getMessage());
        }
    }
}
```

## MongoDB Query Strategy

### Simple Queries (No Taxonomy Filters)

```javascript
// MongoDB query pattern
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

### Complex Queries (With Taxonomy Filters)

When taxonomy filters are applied, use aggregation pipeline:

```javascript
// Step 1: Find question IDs matching taxonomy criteria
db.question_taxonomy_relationships.aggregate([
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
  }
]);

// Step 2: Query questions with those IDs
db.questions.find({
  _id: { $in: matchingQuestionIds },
  user_id: 999888777,
  question_bank_id: 1730832000000000
})
.sort({ title: 1 })
.skip(0)
.limit(20);
```

### Full-Text Search Queries

```javascript
db.questions.find({
  $text: { $search: "array methods" },
  user_id: 999888777,
  question_bank_id: 1730832000000000
})
.sort({ score: { $meta: "textScore" } })
.limit(20);
```

## MongoDB Index Strategy

### Primary Query Index
```javascript
// Supports most common query pattern (user + bank + status + date sort)
db.questions.createIndex(
  {
    user_id: 1,
    question_bank_id: 1,
    status: 1,
    created_at: -1
  },
  { name: "ix_query_primary", background: true }
);
```

### Question Type Filter Index
```javascript
db.questions.createIndex(
  {
    user_id: 1,
    question_bank_id: 1,
    question_type: 1,
    created_at: -1
  },
  { name: "ix_query_type", background: true }
);
```

### Full-Text Search Index
```javascript
db.questions.createIndex(
  {
    title: "text",
    content: "text"
  },
  {
    name: "ix_full_text_search",
    weights: { title: 10, content: 5 },  // Title more important
    background: true
  }
);
```

### Taxonomy Relationship Index
```javascript
db.question_taxonomy_relationships.createIndex(
  {
    user_id: 1,
    question_bank_id: 1,
    taxonomy_type: 1,
    taxonomy_id: 1
  },
  { name: "ix_taxonomy_query", background: true }
);
```

## Testcontainer MongoDB Setup (Simplified)

### Command Module (Complex Setup)
```java
// Command module manually creates beans
@TestConfiguration
public class TestContainersConfig {

    @Bean
    @Primary
    public MongoClient mongoClient() {
        // Manual MongoClient creation
    }

    @Bean
    @Primary
    public MongoDatabaseFactory mongoDatabaseFactory() {
        // Manual factory creation
    }

    @Bean
    @Primary
    public MongoTemplate mongoTemplate() {
        // Manual template creation
    }
}
```

### Query Module (Simplified Setup)
```java
// Query module uses @DynamicPropertySource - much simpler!
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

    @Autowired
    private MongoTemplate mongoTemplate;  // Auto-configured by Spring Boot

    @Test
    void shouldQueryQuestionsWithFilters() {
        // Test implementation
    }
}
```

**Key Benefits**:
- Spring Boot auto-configures MongoClient, MongoDatabaseFactory, MongoTemplate
- No manual bean creation needed
- Simpler, cleaner test setup
- Uses `@DynamicPropertySource` to hijack MongoDB connection properties

## Data Flow - Complete Request/Response Cycle

### 1. HTTP Request (Example)
```http
GET /api/users/999888777/questionbanks/1730832000000000/questions?
    category_level_1=technology&
    tags=beginner,practice&
    question_type=mcq&
    status=published&
    page=0&
    size=20&
    sort=title,asc
```

### 2. Controller Processing
```java
@GetMapping("/questions")
public ResponseEntity<Result<QuestionListResponseDto>> queryQuestions(
        @PathVariable Long userId,
        @PathVariable Long questionbankId,
        @RequestParam(required = false) String categoryLevel1,
        @RequestParam(required = false) String tags,
        @RequestParam(required = false) String questionType,
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "created_at,desc") String sort) {

    // Build query object
    var query = QueryQuestionsQuery.builder()
        .userId(userId)
        .questionBankId(questionbankId)
        .categoryLevel1(categoryLevel1)
        .tags(parseTagsFromQueryParam(tags))
        .questionType(questionType)
        .status(status)
        .page(page)
        .size(size)
        .sort(parseSort(sort))
        .build();

    // Send via mediator
    var result = mediator.send(query);

    return ResponseEntity.ok(result);
}
```

### 3. Query Handler
```java
@Service
public class QueryQuestionsQueryHandler implements IQueryHandler<QueryQuestionsQuery, QuestionListResponseDto> {

    private final QueryQuestionsService queryService;

    @Override
    public Result<QuestionListResponseDto> handle(QueryQuestionsQuery query) {
        return queryService.queryQuestions(
            query.getUserId(),
            query.getQuestionBankId(),
            query.getFilters(),
            query.getPagination()
        );
    }
}
```

### 4. Application Service (Port IN)
```java
@Service
public class DefaultQueryQuestionsService implements QueryQuestionsService {

    private final QuestionQueryRepository questionRepository;
    private final QuestionBanksPerUserRepository questionBanksRepository;

    @Override
    public Result<QuestionListResponseDto> queryQuestions(...) {
        // 1. Validate ownership
        if (!questionBanksRepository.validateOwnership(userId, questionBankId)) {
            return Result.failure("UNAUTHORIZED_ACCESS: User doesn't own question bank");
        }

        // 2. Query questions (NO domain logic)
        var result = questionRepository.findQuestions(
            userId,
            questionBankId,
            filters,
            pagination
        );

        if (result.isFailure()) {
            return Result.failure(result.getError());
        }

        // 3. Build response DTO
        return Result.success(buildResponseDto(result.getValue()));
    }
}
```

### 5. Repository Query Execution
```java
@Repository
public class MongoQuestionQueryRepository implements QuestionQueryRepository {

    @Override
    public Result<PagedResult<QuestionQueryResultDto>> findQuestions(...) {
        // Build query criteria
        Criteria criteria = Criteria
            .where("user_id").is(userId)
            .and("question_bank_id").is(questionBankId);

        // Add filters
        if (filters.getStatus() != null) {
            criteria.and("status").is(filters.getStatus());
        }
        if (filters.getQuestionType() != null) {
            criteria.and("question_type").is(filters.getQuestionType());
        }

        // Execute query
        Query query = Query.query(criteria)
            .skip((long) pagination.getPage() * pagination.getSize())
            .limit(pagination.getSize())
            .with(Sort.by(pagination.getSortDirection(), pagination.getSortField()));

        List<QuestionReadDocument> documents = mongoTemplate.find(
            query,
            QuestionReadDocument.class,
            "questions"
        );

        // Map to DTOs
        List<QuestionQueryResultDto> results = documents.stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());

        Long totalCount = mongoTemplate.count(Query.query(criteria), "questions");

        return Result.success(new PagedResult<>(results, totalCount, pagination));
    }
}
```

### 6. HTTP Response
```json
{
  "success": true,
  "message": "Questions retrieved successfully",
  "data": {
    "questions": [
      {
        "questionId": "507f1f77bcf86cd799439014",
        "sourceQuestionId": "q-uuid-123",
        "questionType": "mcq",
        "title": "JavaScript Array Methods",
        "content": "<p>Which method adds elements?</p>",
        "status": "published",
        "taxonomy": { ... },
        "mcqData": { ... }
      }
    ],
    "pagination": {
      "currentPage": 0,
      "pageSize": 20,
      "totalElements": 156,
      "totalPages": 8
    }
  }
}
```

## Technology Stack

### Core Technologies
- **Java 21**: Language runtime
- **Spring Boot 3.5.6**: Application framework
- **Spring Data MongoDB**: MongoDB integration (auto-configured)
- **MongoDB 8.0**: Document database (Testcontainers)
- **Maven**: Multi-module build tool

### Query-Specific Technologies
- **MongoDB Aggregation Framework**: Complex taxonomy queries
- **MongoDB Text Search**: Full-text search capabilities
- **Spring Data Pageable**: Pagination support

### Testing Technologies
- **JUnit 5**: Testing framework
- **Testcontainers MongoDB**: Integration testing (simplified setup)
- **AssertJ**: Fluent assertions
- **JaCoCo**: Code coverage (>70% target)
- **Allure**: Test reporting

## Cross-Cutting Concerns

### 1. Performance Optimization

**MongoDB Query Performance**:
- Compound indexes for common filter combinations
- Avoid collection scans with proper indexing
- Limit result sets with pagination
- Use projection to return only needed fields

**Caching Strategy (Future)**:
- Cache taxonomy sets per user/bank
- Cache frequent query patterns
- Cache-aside pattern with Redis

### 2. Security

**Read Authorization**:
- Validate user owns question bank before querying
- No JWT validation yet (security disabled in current implementation)
- Path parameter userId must match authenticated user (when security enabled)

### 3. Error Handling

**Query-Specific Errors**:
- `UNAUTHORIZED_ACCESS`: User doesn't own question bank
- `QUESTION_BANK_NOT_FOUND`: Question bank doesn't exist
- `INVALID_QUERY_PARAMETER`: Invalid filter value or pagination params
- `QUERY_ERROR`: MongoDB query execution failed

**Empty Results** (Important!):
- Return 200 OK with empty array (NOT 404)
- Include pagination metadata showing totalElements=0

### 4. Monitoring & Observability

**Query Performance Metrics**:
- Response time percentiles (p50, p95, p99)
- MongoDB query execution time
- Cache hit/miss ratio (future)
- Slow query detection

## Deployment Considerations

### MongoDB Read Preference

**Development/Testing** (Testcontainers):
```yaml
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/quiz_cms_test  # From Testcontainers
      read-preference: primary  # Single instance
```

**Production** (Replica Set):
```yaml
spring:
  data:
    mongodb:
      uri: mongodb://primary:27017,secondary1:27017,secondary2:27017/quiz_cms
      read-preference: secondaryPreferred  # Read from secondaries when available
      replica-set-name: quiz-cms-rs
```

### Index Management

**Pre-Deployment**:
- Create all indexes before deploying query service
- Use `background: true` for production index creation
- Monitor index usage with `explain()` plans

**Post-Deployment**:
- Monitor slow queries
- Add covering indexes for frequent query patterns
- Remove unused indexes

## Future Enhancements

### Phase 2: Advanced Features

1. **Faceted Search**:
   - Return taxonomy facet counts alongside results
   - Example: "MCQ: 50, Essay: 30, True/False: 10"

2. **Saved Filters**:
   - Allow users to save and reuse filter combinations
   - Store in user preferences

3. **Real-Time Updates**:
   - WebSocket support for live question list updates
   - Server-sent events for new questions

4. **Export Functionality**:
   - CSV/Excel export of filtered questions
   - Streaming export for large datasets

5. **GraphQL Support**:
   - Flexible field selection
   - Batch query optimization

This architecture provides a scalable, performant foundation for querying questions while maintaining simplicity by avoiding unnecessary domain complexity. The separation from the command side ensures clear boundaries and independent optimization strategies.
