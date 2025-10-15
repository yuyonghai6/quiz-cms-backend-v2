# Implementation Roadmap: Query List of Questions (TDD Approach)

## Overview

This roadmap follows **Test-Driven Development (TDD)** methodology with the **Red-Green-Refactor** cycle specific to query operations. Unlike the command side with its complex DDD architecture, the query side prioritizes **simplicity and read performance**.

### TDD Principles Applied
1. **Red**: Write a failing test that defines desired query behavior
2. **Green**: Write minimal code to make the query test pass
3. **Refactor**: Optimize query performance while keeping tests green
4. **Repeat**: Continue cycle for each query feature

### Key Architectural Principles
- **NO Domain Layer**: Direct repository access from application service
- **Read-Only Operations**: No transactions, no domain events
- **Simplified Testcontainer Setup**: Use `@DynamicPropertySource` instead of manual beans
- **Query Optimization**: Focus on MongoDB indexes and aggregation pipelines

### Testing Strategy
```
                    ▲
                   / \
                  /   \
                 /  E2E \          (10% - K6 + TestRestTemplate)
                /_______\
               /         \
              / Integration\       (40% - Testcontainers MongoDB)
             /___________  \
            /               \
           /   Unit Tests    \     (50% - Service + Repository logic)
          /___________________\
```

---

## Phase 1: Foundation Setup - Days 1-2

### Objectives
- Create query module structure with **NO domain layer**
- Setup simplified Testcontainers MongoDB
- Establish package naming to avoid command module clashes
- Create basic DTOs and interfaces

### Milestone 1.1: Module Structure Creation
**Duration**: 0.5 day
**Test Coverage Goal**: N/A (infrastructure setup)

#### Package Structure
```
internal-layer/question-bank-query/
├── src/main/java/com/quizfun/questionbankquery/
│   ├── application/
│   │   ├── dto/
│   │   │   ├── QuestionListResponseDto.java
│   │   │   ├── QuestionQueryResultDto.java
│   │   │   ├── PaginationDto.java
│   │   │   ├── AppliedFiltersDto.java
│   │   │   ├── QuestionFilters.java
│   │   │   └── PaginationParams.java
│   │   ├── ports/
│   │   │   ├── in/
│   │   │   │   └── QueryQuestionsService.java
│   │   │   └── out/
│   │   │       └── QuestionQueryRepository.java
│   │   ├── queries/
│   │   │   └── QueryQuestionsQuery.java  (implements IQuery<T>)
│   │   └── services/
│   │       └── DefaultQueryQuestionsService.java
│   └── infrastructure/
│       ├── persistence/
│       │   ├── documents/
│       │   │   ├── QuestionReadDocument.java
│       │   │   ├── TaxonomyData.java
│       │   │   └── McqDataDocument.java
│       │   └── repositories/
│       │       └── MongoQuestionQueryRepository.java
│       └── configuration/
│           └── QueryConfiguration.java
└── src/test/java/com/quizfun/questionbankquery/
    ├── config/
    │   └── TestContainersMongoConfig.java
    ├── application/
    │   └── services/
    │       └── DefaultQueryQuestionsServiceTest.java
    └── infrastructure/
        └── repositories/
            └── MongoQuestionQueryRepositoryIntegrationTest.java

orchestration-layer/src/main/java/com/quizfun/orchestrationlayer/
├── controllers/
│   └── QuestionQueryController.java
└── handlers/
    └── QueryQuestionsQueryHandler.java
```

**Deliverables**:
- ✅ Package structure created (note: `questionbankquery` vs `questionbank`)
- ✅ Maven dependencies added to question-bank-query module
- ✅ No domain layer created (intentional simplification)

### Milestone 1.2: Simplified Testcontainers Setup
**Duration**: 0.5 day
**Test Coverage Goal**: Smoke test

#### TDD Cycle 1: Testcontainer Smoke Test

**RED - Write Failing Test**
```java
// File: TestContainersMongoConfig.java
@Testcontainers
@SpringBootTest
public abstract class BaseQueryIntegrationTest {

    @Container
    static MongoDBContainer mongoContainer = new MongoDBContainer("mongo:8.0")
            .withExposedPorts(27017)  // Simulate secondary instance
            .withReuse(false);

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoContainer::getReplicaSetUrl);
        registry.add("spring.data.mongodb.read-preference", () -> "primary");
    }

    @Autowired
    protected MongoTemplate mongoTemplate;  // Auto-configured by Spring Boot

    @Test
    void smokeTest_containerShouldStart() {
        assertTrue(mongoContainer.isRunning());
        assertNotNull(mongoTemplate);
    }
}
```

**GREEN - Run Test**
- Test passes because Spring Boot auto-configures MongoTemplate
- No manual bean creation needed (unlike command module)

**REFACTOR** - Add utility methods
```java
protected void cleanupTestData() {
    mongoTemplate.getCollection("questions").deleteMany(new Document());
    mongoTemplate.getCollection("question_taxonomy_relationships").deleteMany(new Document());
}

protected void insertTestQuestions(List<QuestionReadDocument> questions) {
    mongoTemplate.insertAll(questions);
}
```

**Deliverables**:
- ✅ Testcontainers MongoDB starts successfully
- ✅ MongoTemplate auto-configured and injectable
- ✅ Smoke test passing
- ✅ Test cleanup utilities created

---

## Phase 2: Query DTOs and Filter Logic - Days 3-4

### Objectives
- Create comprehensive DTOs for query requests/responses
- Implement filter building logic
- Implement pagination parameter handling
- No business logic (query side is simple!)

### Milestone 2.1: Query Object and DTOs
**Duration**: 1 day
**Test Coverage Goal**: 100% for DTOs

#### TDD Cycle 2: Query Object Creation

**RED - Write Test**
```java
// File: QueryQuestionsQueryTest.java
@Test
void shouldCreateQueryWithRequiredParameters() {
    // Given
    Long userId = 999888777L;
    Long questionBankId = 1730832000000000L;

    // When
    QueryQuestionsQuery query = QueryQuestionsQuery.builder()
        .userId(userId)
        .questionBankId(questionBankId)
        .build();

    // Then
    assertThat(query.getUserId()).isEqualTo(userId);
    assertThat(query.getQuestionBankId()).isEqualTo(questionBankId);
}

@Test
void shouldCreateQueryWithAllFilters() {
    // Given & When
    QueryQuestionsQuery query = QueryQuestionsQuery.builder()
        .userId(999888777L)
        .questionBankId(1730832000000000L)
        .filters(QuestionFilters.builder()
            .categoryLevel1("technology")
            .categoryLevel2("programming")
            .tags(List.of("beginner", "practice"))
            .quizzes(List.of(101L, 102L))
            .difficultyLevel("easy")
            .questionType("mcq")
            .status("published")
            .search("array methods")
            .build())
        .pagination(PaginationParams.builder()
            .page(0)
            .size(20)
            .sort("title,asc")
            .build())
        .build();

    // Then
    assertThat(query.getFilters()).isNotNull();
    assertThat(query.getFilters().getCategoryLevel1()).isEqualTo("technology");
    assertThat(query.getFilters().getTags()).containsExactly("beginner", "practice");
    assertThat(query.getPagination().getPage()).isEqualTo(0);
}

@Test
void shouldThrowExceptionWhenUserIdIsNull() {
    // When & Then
    assertThatThrownBy(() -> QueryQuestionsQuery.builder()
        .userId(null)
        .questionBankId(123L)
        .build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("userId cannot be null");
}
```

**GREEN - Implement**
```java
// File: QueryQuestionsQuery.java
@Getter
@Builder
public class QueryQuestionsQuery implements IQuery<QuestionListResponseDto> {
    private final Long userId;
    private final Long questionBankId;
    private final QuestionFilters filters;
    private final PaginationParams pagination;

    public QueryQuestionsQuery(Long userId, Long questionBankId,
                               QuestionFilters filters, PaginationParams pagination) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
        if (questionBankId == null || questionBankId <= 0) {
            throw new IllegalArgumentException("questionBankId must be positive");
        }

        this.userId = userId;
        this.questionBankId = questionBankId;
        this.filters = filters != null ? filters : QuestionFilters.builder().build();
        this.pagination = pagination != null ? pagination : PaginationParams.builder().build();
    }
}

// File: QuestionFilters.java
@Getter
@Builder
public class QuestionFilters {
    private final String categoryLevel1;
    private final String categoryLevel2;
    private final String categoryLevel3;
    private final String categoryLevel4;
    private final List<String> tags;
    private final List<Long> quizzes;
    private final String difficultyLevel;
    private final String questionType;
    private final String status;
    private final String search;

    public boolean hasFilters() {
        return categoryLevel1 != null || categoryLevel2 != null ||
               (tags != null && !tags.isEmpty()) || questionType != null ||
               status != null || search != null;
    }

    public boolean hasTaxonomyFilters() {
        return categoryLevel1 != null || categoryLevel2 != null ||
               categoryLevel3 != null || categoryLevel4 != null ||
               (tags != null && !tags.isEmpty()) ||
               (quizzes != null && !quizzes.isEmpty()) ||
               difficultyLevel != null;
    }
}

// File: PaginationParams.java
@Getter
@Builder
public class PaginationParams {
    @Builder.Default
    private final int page = 0;

    @Builder.Default
    private final int size = 20;

    @Builder.Default
    private final String sort = "created_at,desc";

    public PaginationParams(int page, int size, String sort) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }

        this.page = page;
        this.size = size;
        this.sort = sort != null ? sort : "created_at,desc";
    }

    public long getOffset() {
        return (long) page * size;
    }

    public String getSortField() {
        return sort.split(",")[0];
    }

    public Sort.Direction getSortDirection() {
        String direction = sort.split(",").length > 1 ? sort.split(",")[1] : "desc";
        return direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
    }
}
```

**REFACTOR** - Add builder validation

**Deliverables**:
- ✅ QueryQuestionsQuery with validation
- ✅ QuestionFilters with utility methods
- ✅ PaginationParams with sorting logic
- ✅ 100% unit test coverage for DTOs

---

## Phase 3: MongoDB Repository Implementation - Days 5-7

### Objectives
- Implement query repository with MongoDB aggregation
- Handle simple queries (no taxonomy filters)
- Handle complex queries (with taxonomy filters)
- Implement full-text search
- Create comprehensive integration tests

### Milestone 3.1: Simple Query Implementation
**Duration**: 1 day
**Test Coverage Goal**: 100% integration tests

#### TDD Cycle 3: Simple Query Repository

**RED - Write Integration Test**
```java
// File: MongoQuestionQueryRepositoryIntegrationTest.java
@SpringBootTest
@Testcontainers
class MongoQuestionQueryRepositoryIntegrationTest extends BaseQueryIntegrationTest {

    @Autowired
    private QuestionQueryRepository repository;

    @BeforeEach
    void setUp() {
        cleanupTestData();
        insertTestQuestions(createSampleQuestions());
    }

    @AfterEach
    void tearDown() {
        cleanupTestData();
    }

    @Test
    void shouldQueryQuestionsWithBasicFilters() {
        // Given
        Long userId = 999888777L;
        Long questionBankId = 1730832000000000L;
        QuestionFilters filters = QuestionFilters.builder()
            .questionType("mcq")
            .status("published")
            .build();
        PaginationParams pagination = PaginationParams.builder()
            .page(0)
            .size(10)
            .build();

        // When
        var result = repository.findQuestions(userId, questionBankId, filters, pagination);

        // Then
        assertThat(result.success()).isTrue();
        assertThat(result.data().getData()).hasSize(5);  // 5 matching questions
        assertThat(result.data().getData())
            .allMatch(q -> q.getQuestionType().equals("mcq"))
            .allMatch(q -> q.getStatus().equals("published"));
    }

    @Test
    void shouldReturnEmptyListWhenNoQuestionsMatch() {
        // Given
        QuestionFilters filters = QuestionFilters.builder()
            .questionType("nonexistent_type")
            .build();

        // When
        var result = repository.findQuestions(999888777L, 1730832000000000L, filters,
            PaginationParams.builder().build());

        // Then
        assertThat(result.success()).isTrue();
        assertThat(result.data().getData()).isEmpty();
        assertThat(result.data().getTotalElements()).isZero();
    }

    @Test
    void shouldApplyPaginationCorrectly() {
        // Given
        PaginationParams page1 = PaginationParams.builder().page(0).size(5).build();
        PaginationParams page2 = PaginationParams.builder().page(1).size(5).build();

        // When
        var result1 = repository.findQuestions(999888777L, 1730832000000000L,
            QuestionFilters.builder().build(), page1);
        var result2 = repository.findQuestions(999888777L, 1730832000000000L,
            QuestionFilters.builder().build(), page2);

        // Then
        assertThat(result1.data().getData()).hasSize(5);
        assertThat(result2.data().getData()).hasSize(5);
        // Ensure different questions on different pages
        assertThat(result1.data().getData().get(0).getQuestionId())
            .isNotEqualTo(result2.data().getData().get(0).getQuestionId());
    }

    @Test
    void shouldSortByTitleAscending() {
        // Given
        PaginationParams pagination = PaginationParams.builder()
            .page(0)
            .size(10)
            .sort("title,asc")
            .build();

        // When
        var result = repository.findQuestions(999888777L, 1730832000000000L,
            QuestionFilters.builder().build(), pagination);

        // Then
        List<String> titles = result.data().getData().stream()
            .map(QuestionQueryResultDto::getTitle)
            .collect(Collectors.toList());

        assertThat(titles).isSorted();
    }
}
```

**GREEN - Implement Repository**
```java
// File: MongoQuestionQueryRepository.java
@Repository
public class MongoQuestionQueryRepository implements QuestionQueryRepository {

    private final MongoTemplate mongoTemplate;

    public MongoQuestionQueryRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Result<PagedResult<QuestionQueryResultDto>> findQuestions(
            Long userId,
            Long questionBankId,
            QuestionFilters filters,
            PaginationParams pagination) {

        try {
            // Build query criteria
            Criteria criteria = buildCriteria(userId, questionBankId, filters);

            // Create query
            Query query = Query.query(criteria);

            // Apply pagination
            query.skip(pagination.getOffset());
            query.limit(pagination.getSize());

            // Apply sorting
            query.with(Sort.by(pagination.getSortDirection(), pagination.getSortField()));

            // Execute query
            List<QuestionReadDocument> documents = mongoTemplate.find(
                query,
                QuestionReadDocument.class,
                "questions"
            );

            // Count total
            Long totalCount = mongoTemplate.count(Query.query(criteria), "questions");

            // Map to DTOs
            List<QuestionQueryResultDto> results = documents.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

            // Build paged result
            PagedResult<QuestionQueryResultDto> pagedResult = PagedResult.<QuestionQueryResultDto>builder()
                .data(results)
                .currentPage(pagination.getPage())
                .pageSize(pagination.getSize())
                .totalElements(totalCount)
                .totalPages((int) Math.ceil((double) totalCount / pagination.getSize()))
                .build();

            return Result.success(pagedResult);

        } catch (Exception ex) {
            return Result.failure("QUERY_ERROR: Failed to query questions - " + ex.getMessage());
        }
    }

    private Criteria buildCriteria(Long userId, Long questionBankId, QuestionFilters filters) {
        Criteria criteria = Criteria
            .where("user_id").is(userId)
            .and("question_bank_id").is(questionBankId);

        // Add optional filters
        if (filters.getQuestionType() != null) {
            criteria.and("question_type").is(filters.getQuestionType());
        }

        if (filters.getStatus() != null) {
            criteria.and("status").is(filters.getStatus());
        }

        return criteria;
    }

    private QuestionQueryResultDto mapToDto(QuestionReadDocument document) {
        // Mapping logic
        return QuestionQueryResultDto.builder()
            .questionId(document.getId().toString())
            .sourceQuestionId(document.getSourceQuestionId())
            .questionType(document.getQuestionType())
            .title(document.getTitle())
            .content(document.getContent())
            .status(document.getStatus())
            // ... map all fields
            .build();
    }
}
```

**REFACTOR** - Extract query building to separate methods

**Deliverables**:
- ✅ Simple query implementation working
- ✅ Pagination working correctly
- ✅ Sorting working correctly
- ✅ Integration tests passing with Testcontainers
- ✅ Empty result handling correct

### Milestone 3.2: Complex Query with Taxonomy Filters
**Duration**: 1.5 days
**Test Coverage Goal**: 100%

#### TDD Cycle 4: Taxonomy Filter Query

**RED - Write Test**
```java
@Test
void shouldQueryQuestionsWithCategoryFilters() {
    // Given
    QuestionFilters filters = QuestionFilters.builder()
        .categoryLevel1("technology")
        .categoryLevel2("programming")
        .build();

    // When
    var result = repository.findQuestions(999888777L, 1730832000000000L, filters,
        PaginationParams.builder().build());

    // Then
    assertThat(result.success()).isTrue();
    assertThat(result.data().getData()).isNotEmpty();
    // Verify each question has the correct taxonomy
    assertThat(result.data().getData()).allMatch(q ->
        q.getTaxonomy().getCategories().getLevel1().getId().equals("technology") &&
        q.getTaxonomy().getCategories().getLevel2().getId().equals("programming")
    );
}

@Test
void shouldQueryQuestionsWithTagFiltersUsingOrLogic() {
    // Given
    QuestionFilters filters = QuestionFilters.builder()
        .tags(List.of("beginner", "practice"))
        .build();

    // When
    var result = repository.findQuestions(999888777L, 1730832000000000L, filters,
        PaginationParams.builder().build());

    // Then
    assertThat(result.success()).isTrue();
    assertThat(result.data().getData()).allMatch(q ->
        q.getTaxonomy().getTags().stream()
            .anyMatch(tag -> tag.getId().equals("beginner") || tag.getId().equals("practice"))
    );
}
```

**GREEN - Implement Aggregation Pipeline**
```java
private Criteria buildCriteria(Long userId, Long questionBankId, QuestionFilters filters) {
    Criteria criteria = Criteria
        .where("user_id").is(userId)
        .and("question_bank_id").is(questionBankId);

    // If taxonomy filters exist, need to use aggregation
    if (filters.hasTaxonomyFilters()) {
        // Find question IDs matching taxonomy
        List<ObjectId> matchingQuestionIds = findQuestionIdsByTaxonomy(
            userId, questionBankId, filters
        );

        criteria.and("_id").in(matchingQuestionIds);
    }

    // Add non-taxonomy filters
    if (filters.getQuestionType() != null) {
        criteria.and("question_type").is(filters.getQuestionType());
    }

    if (filters.getStatus() != null) {
        criteria.and("status").is(filters.getStatus());
    }

    return criteria;
}

private List<ObjectId> findQuestionIdsByTaxonomy(
        Long userId, Long questionBankId, QuestionFilters filters) {

    // Build taxonomy match criteria
    List<Criteria> taxonomyCriteria = new ArrayList<>();

    if (filters.getCategoryLevel1() != null) {
        taxonomyCriteria.add(Criteria.where("taxonomy_type").is("category_level_1")
            .and("taxonomy_id").is(filters.getCategoryLevel1()));
    }

    if (filters.getCategoryLevel2() != null) {
        taxonomyCriteria.add(Criteria.where("taxonomy_type").is("category_level_2")
            .and("taxonomy_id").is(filters.getCategoryLevel2()));
    }

    if (filters.getTags() != null && !filters.getTags().isEmpty()) {
        taxonomyCriteria.add(Criteria.where("taxonomy_type").is("tag")
            .and("taxonomy_id").in(filters.getTags()));
    }

    // Aggregation pipeline
    Aggregation aggregation = Aggregation.newAggregation(
        Aggregation.match(Criteria.where("user_id").is(userId)
            .and("question_bank_id").is(questionBankId)
            .orOperator(taxonomyCriteria.toArray(new Criteria[0]))),
        Aggregation.group("question_id")
            .addToSet("$$ROOT").as("taxonomies"),
        Aggregation.project()
            .andExpression("_id").as("questionId")
            .andExpression("taxonomies").as("taxonomies")
    );

    AggregationResults<Document> results = mongoTemplate.aggregate(
        aggregation,
        "question_taxonomy_relationships",
        Document.class
    );

    return results.getMappedResults().stream()
        .map(doc -> (ObjectId) doc.get("questionId"))
        .collect(Collectors.toList());
}
```

**REFACTOR** - Optimize aggregation pipeline

**Deliverables**:
- ✅ Category filter working (AND logic)
- ✅ Tag filter working (OR logic)
- ✅ Quiz filter working (OR logic)
- ✅ Difficulty level filter working
- ✅ Combined filters working correctly
- ✅ Integration tests passing

### Milestone 3.3: Full-Text Search Implementation
**Duration**: 0.5 day
**Test Coverage Goal**: 100%

#### TDD Cycle 5: Text Search

**RED - Write Test**
```java
@Test
void shouldSearchQuestionsUsingFullTextSearch() {
    // Given
    QuestionFilters filters = QuestionFilters.builder()
        .search("array methods")
        .build();

    // When
    var result = repository.findQuestions(999888777L, 1730832000000000L, filters,
        PaginationParams.builder().build());

    // Then
    assertThat(result.success()).isTrue();
    assertThat(result.data().getData()).isNotEmpty();
    // Verify results contain search terms
    assertThat(result.data().getData()).anyMatch(q ->
        q.getTitle().toLowerCase().contains("array") ||
        q.getContent().toLowerCase().contains("array")
    );
}
```

**GREEN - Implement Text Search**
```java
private Criteria buildCriteria(Long userId, Long questionBankId, QuestionFilters filters) {
    Criteria criteria = Criteria
        .where("user_id").is(userId)
        .and("question_bank_id").is(questionBankId);

    // Text search
    if (filters.getSearch() != null && !filters.getSearch().isEmpty()) {
        criteria.andOperator(
            new Criteria().orOperator(
                Criteria.where("title").regex(filters.getSearch(), "i"),
                Criteria.where("content").regex(filters.getSearch(), "i")
            )
        );
    }

    // ... rest of filters
    return criteria;
}
```

**REFACTOR** - Use MongoDB text index

**Deliverables**:
- ✅ Full-text search working
- ✅ Search results sorted by relevance
- ✅ Integration tests passing

---

## Phase 4: Application Service Layer - Day 8

### Objectives
- Implement query application service (Port IN)
- Add ownership validation
- Build response DTOs
- NO domain logic (query side is simple!)

### Milestone 4.1: Application Service Implementation
**Duration**: 1 day
**Test Coverage Goal**: 100% with mocks

#### TDD Cycle 6: Application Service

**RED - Write Test**
```java
// File: DefaultQueryQuestionsServiceTest.java
@ExtendWith(MockitoExtension.class)
class DefaultQueryQuestionsServiceTest {

    @Mock
    private QuestionQueryRepository questionRepository;

    @Mock
    private QuestionBanksPerUserRepository questionBanksRepository;

    @InjectMocks
    private DefaultQueryQuestionsService service;

    @Test
    void shouldQueryQuestionsSuccessfully() {
        // Given
        Long userId = 999888777L;
        Long questionBankId = 1730832000000000L;
        QuestionFilters filters = QuestionFilters.builder().build();
        PaginationParams pagination = PaginationParams.builder().build();

        when(questionBanksRepository.validateOwnership(userId, questionBankId))
            .thenReturn(true);
        when(questionRepository.findQuestions(userId, questionBankId, filters, pagination))
            .thenReturn(Result.success(createMockPagedResult()));

        // When
        var result = service.queryQuestions(userId, questionBankId, filters, pagination);

        // Then
        assertThat(result.success()).isTrue();
        assertThat(result.data().getQuestions()).isNotEmpty();
        verify(questionBanksRepository).validateOwnership(userId, questionBankId);
        verify(questionRepository).findQuestions(userId, questionBankId, filters, pagination);
    }

    @Test
    void shouldFailWhenUserDoesNotOwnQuestionBank() {
        // Given
        when(questionBanksRepository.validateOwnership(any(), any()))
            .thenReturn(false);

        // When
        var result = service.queryQuestions(999L, 123L,
            QuestionFilters.builder().build(), PaginationParams.builder().build());

        // Then
        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("UNAUTHORIZED_ACCESS");
        verify(questionRepository, never()).findQuestions(any(), any(), any(), any());
    }

    @Test
    void shouldHandleRepositoryFailure() {
        // Given
        when(questionBanksRepository.validateOwnership(any(), any()))
            .thenReturn(true);
        when(questionRepository.findQuestions(any(), any(), any(), any()))
            .thenReturn(Result.failure("QUERY_ERROR"));

        // When
        var result = service.queryQuestions(999L, 123L,
            QuestionFilters.builder().build(), PaginationParams.builder().build());

        // Then
        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("QUERY_ERROR");
    }
}
```

**GREEN - Implement Service**
```java
// File: DefaultQueryQuestionsService.java
@Service
public class DefaultQueryQuestionsService implements QueryQuestionsService {

    private final QuestionQueryRepository questionRepository;
    private final QuestionBanksPerUserRepository questionBanksRepository;

    public DefaultQueryQuestionsService(
            QuestionQueryRepository questionRepository,
            QuestionBanksPerUserRepository questionBanksRepository) {
        this.questionRepository = questionRepository;
        this.questionBanksRepository = questionBanksRepository;
    }

    @Override
    public Result<QuestionListResponseDto> queryQuestions(
            Long userId,
            Long questionBankId,
            QuestionFilters filters,
            PaginationParams pagination) {

        try {
            // 1. Validate ownership
            if (!questionBanksRepository.validateOwnership(userId, questionBankId)) {
                return Result.failure(
                    "UNAUTHORIZED_ACCESS: User does not have access to question bank " + questionBankId
                );
            }

            // 2. Query questions (NO domain logic - direct repository call)
            var pagedResult = questionRepository.findQuestions(
                userId,
                questionBankId,
                filters,
                pagination
            );

            if (pagedResult.isFailure()) {
                return Result.failure(pagedResult.getError());
            }

            // 3. Build response DTO
            QuestionListResponseDto response = buildResponseDto(
                pagedResult.getValue(),
                filters
            );

            return Result.success(response);

        } catch (Exception ex) {
            return Result.failure("INTERNAL_ERROR: " + ex.getMessage());
        }
    }

    private QuestionListResponseDto buildResponseDto(
            PagedResult<QuestionQueryResultDto> pagedResult,
            QuestionFilters filters) {

        return QuestionListResponseDto.builder()
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
    }

    private Map<String, Object> buildAppliedFiltersMap(QuestionFilters filters) {
        Map<String, Object> appliedFilters = new HashMap<>();

        if (filters.getCategoryLevel1() != null) {
            appliedFilters.put("categoryLevel1", filters.getCategoryLevel1());
        }
        if (filters.getTags() != null && !filters.getTags().isEmpty()) {
            appliedFilters.put("tags", filters.getTags());
        }
        // ... add all non-null filters

        return appliedFilters;
    }
}
```

**REFACTOR** - Improve error messages

**Deliverables**:
- ✅ Application service implemented
- ✅ Ownership validation working
- ✅ 100% unit test coverage with mocks
- ✅ All error scenarios tested

---

## Phase 5: Query Handler and Mediator Integration - Day 9

### Objectives
- Implement query handler in orchestration-layer
- Register handler with mediator
- Ensure type-safe query routing

### Milestone 5.1: Query Handler
**Duration**: 0.5 day
**Test Coverage Goal**: 100%

#### TDD Cycle 7: Query Handler

**RED - Write Test**
```java
// File: QueryQuestionsQueryHandlerTest.java
@ExtendWith(MockitoExtension.class)
class QueryQuestionsQueryHandlerTest {

    @Mock
    private QueryQuestionsService queryService;

    @InjectMocks
    private QueryQuestionsQueryHandler handler;

    @Test
    void shouldHandleQuerySuccessfully() {
        // Given
        QueryQuestionsQuery query = QueryQuestionsQuery.builder()
            .userId(999888777L)
            .questionBankId(1730832000000000L)
            .filters(QuestionFilters.builder().build())
            .pagination(PaginationParams.builder().build())
            .build();

        when(queryService.queryQuestions(any(), any(), any(), any()))
            .thenReturn(Result.success(createMockResponse()));

        // When
        var result = handler.handle(query);

        // Then
        assertThat(result.success()).isTrue();
        verify(queryService).queryQuestions(
            query.getUserId(),
            query.getQuestionBankId(),
            query.getFilters(),
            query.getPagination()
        );
    }
}
```

**GREEN - Implement**
```java
// File: QueryQuestionsQueryHandler.java (in orchestration-layer)
@Service
public class QueryQuestionsQueryHandler
        implements IQueryHandler<QueryQuestionsQuery, QuestionListResponseDto> {

    private final QueryQuestionsService queryService;

    public QueryQuestionsQueryHandler(QueryQuestionsService queryService) {
        this.queryService = queryService;
    }

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

**Deliverables**:
- ✅ Query handler implemented
- ✅ Handler auto-registered with mediator
- ✅ 100% test coverage

---

## Phase 6: Query Controller - Day 10

### Objectives
- Implement GET endpoint controller
- Parse query parameters
- Map errors to HTTP status codes

### Milestone 6.1: Query Controller
**Duration**: 1 day
**Test Coverage Goal**: 100% with MockMvc

#### TDD Cycle 8: Controller

**RED - Write Test**
```java
// File: QuestionQueryControllerTest.java
@WebMvcTest(QuestionQueryController.class)
class QuestionQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IMediator mediator;

    @Test
    void shouldReturn200WhenQuestionsFoundSuccessfully() throws Exception {
        // Given
        when(mediator.send(any(QueryQuestionsQuery.class)))
            .thenReturn(Result.success(createMockResponse()));

        // When & Then
        mockMvc.perform(get("/api/users/999888777/questionbanks/1730832000000000/questions")
                .param("question_type", "mcq")
                .param("status", "published")
                .param("page", "0")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.questions").isArray())
            .andExpect(jsonPath("$.data.pagination").exists());
    }

    @Test
    void shouldReturn200WithEmptyArrayWhenNoQuestionsMatch() throws Exception {
        // Given
        when(mediator.send(any(QueryQuestionsQuery.class)))
            .thenReturn(Result.success(createEmptyResponse()));

        // When & Then
        mockMvc.perform(get("/api/users/999888777/questionbanks/1730832000000000/questions")
                .param("question_type", "nonexistent"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.questions").isEmpty())
            .andExpect(jsonPath("$.data.pagination.totalElements").value(0));
    }

    @Test
    void shouldReturn400WhenInvalidPaginationParameters() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/users/999888777/questionbanks/1730832000000000/questions")
                .param("page", "-1")
                .param("size", "200"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value(containsString("INVALID_QUERY_PARAMETER")));
    }

    @Test
    void shouldReturn422WhenUnauthorizedAccess() throws Exception {
        // Given
        when(mediator.send(any(QueryQuestionsQuery.class)))
            .thenReturn(Result.failure("UNAUTHORIZED_ACCESS: User doesn't own question bank"));

        // When & Then
        mockMvc.perform(get("/api/users/999888777/questionbanks/1730832000000000/questions"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value(containsString("UNAUTHORIZED_ACCESS")));
    }
}
```

**GREEN - Implement Controller**
```java
// File: QuestionQueryController.java (in orchestration-layer)
@RestController
@RequestMapping("/api/users/{userId}/questionbanks/{questionbankId}")
@Validated
public class QuestionQueryController {

    private final IMediator mediator;

    public QuestionQueryController(IMediator mediator) {
        this.mediator = mediator;
    }

    @GetMapping("/questions")
    public ResponseEntity<Result<QuestionListResponseDto>> queryQuestions(
            @PathVariable Long userId,
            @PathVariable Long questionbankId,
            @RequestParam(required = false) String categoryLevel1,
            @RequestParam(required = false) String categoryLevel2,
            @RequestParam(required = false) String categoryLevel3,
            @RequestParam(required = false) String categoryLevel4,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) String quizzes,
            @RequestParam(required = false) String difficultyLevel,
            @RequestParam(required = false) String questionType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "created_at,desc") String sort) {

        try {
            // Build filters
            QuestionFilters filters = QuestionFilters.builder()
                .categoryLevel1(categoryLevel1)
                .categoryLevel2(categoryLevel2)
                .categoryLevel3(categoryLevel3)
                .categoryLevel4(categoryLevel4)
                .tags(parseCommaSeparatedList(tags))
                .quizzes(parseCommaSeparatedLongList(quizzes))
                .difficultyLevel(difficultyLevel)
                .questionType(questionType)
                .status(status)
                .search(search)
                .build();

            // Build pagination
            PaginationParams pagination = PaginationParams.builder()
                .page(page)
                .size(size)
                .sort(sort)
                .build();

            // Create query
            QueryQuestionsQuery query = QueryQuestionsQuery.builder()
                .userId(userId)
                .questionBankId(questionbankId)
                .filters(filters)
                .pagination(pagination)
                .build();

            // Send via mediator
            var result = mediator.send(query);

            if (result.success()) {
                return ResponseEntity
                    .ok()
                    .header("X-Total-Count", result.data().getPagination().getTotalElements().toString())
                    .body(result);
            } else {
                return mapErrorToHttpStatus(result);
            }

        } catch (IllegalArgumentException ex) {
            return ResponseEntity
                .badRequest()
                .body(Result.failure("INVALID_QUERY_PARAMETER: " + ex.getMessage()));
        }
    }

    private ResponseEntity<Result<QuestionListResponseDto>> mapErrorToHttpStatus(
            Result<QuestionListResponseDto> result) {

        if (result.message().startsWith("UNAUTHORIZED_ACCESS") ||
            result.message().startsWith("QUESTION_BANK_NOT_FOUND")) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(result);
        } else if (result.message().startsWith("DATABASE_ERROR") ||
                   result.message().startsWith("INTERNAL_ERROR")) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    private List<String> parseCommaSeparatedList(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        return Arrays.asList(input.split(","));
    }

    private List<Long> parseCommaSeparatedLongList(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        return Arrays.stream(input.split(","))
            .map(Long::parseLong)
            .collect(Collectors.toList());
    }
}
```

**REFACTOR** - Extract parameter parsing

**Deliverables**:
- ✅ GET endpoint working
- ✅ Query parameter parsing working
- ✅ Error mapping correct
- ✅ 100% test coverage with MockMvc

---

## Phase 7: E2E Testing and K6 Integration - Days 11-12

### Objectives
- Create comprehensive E2E tests
- Generate test data for K6
- Integrate with existing K6 test structure
- Verify performance

### Milestone 7.1: E2E Integration Tests
**Duration**: 1 day
**Test Coverage Goal**: All critical paths

#### E2E Test Implementation

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class QueryQuestionsE2ETest extends BaseQueryIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        // Load comprehensive test data
        insertTestQuestions(createFullTestDataSet());
    }

    @Test
    void shouldQueryQuestionsEndToEnd() {
        // Given
        String url = "/api/users/999888777/questionbanks/1730832000000000/questions?page=0&size=20";

        // When
        var response = restTemplate.getForEntity(url, Result.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().success()).isTrue();
        // ... full assertions
    }

    // More comprehensive E2E tests
}
```

**Deliverables**:
- ✅ E2E tests passing
- ✅ All scenarios covered
- ✅ MongoDB state verified

### Milestone 7.2: K6 Integration
**Duration**: 1 day

See existing K6 test at `api-system-test/test-upsert-question-with-taxonomy.js` for reference.

**Deliverables**:
- ✅ K6 test script created
- ✅ Performance benchmarks met (<500ms p95)
- ✅ Reports generated

---

## Phase 8: Documentation and Code Quality - Day 13

### Objectives
- Generate JaCoCo coverage reports (>70% target)
- Generate Allure test reports
- Final code review
- Update README

### Deliverables
- ✅ JaCoCo coverage >70%
- ✅ Allure reports generated
- ✅ Code reviewed
- ✅ README updated

---

## Success Criteria

### Functional
- ✅ GET endpoint returns questions with filters
- ✅ Pagination working correctly
- ✅ Sorting working correctly
- ✅ Taxonomy filters (AND/OR logic) working
- ✅ Full-text search working
- ✅ Empty results return 200 OK
- ✅ All validation rules enforced

### Non-Functional
- ✅ Response time < 500ms (p95)
- ✅ Test coverage >70%
- ✅ Zero critical bugs
- ✅ MongoDB indexes optimized

### Quality
- ✅ All TDD cycles completed (Red-Green-Refactor)
- ✅ 40+ tests written and passing
- ✅ Code reviewed
- ✅ CI/CD pipeline green

---

## Timeline Summary

| Phase | Duration | Dependencies |
|-------|----------|--------------|
| Phase 1: Foundation | 2 days | None |
| Phase 2: DTOs | 1 day | Phase 1 |
| Phase 3: Repository | 3 days | Phase 2 |
| Phase 4: Application Service | 1 day | Phase 3 |
| Phase 5: Query Handler | 0.5 day | Phase 4 |
| Phase 6: Controller | 1 day | Phase 5 |
| Phase 7: E2E & K6 | 2 days | Phase 6 |
| Phase 8: Documentation | 1 day | Phase 7 |
| **Total** | **11.5 days** | - |

**Note**: Timeline assumes 1 developer working full-time.

This simplified roadmap reflects the reduced complexity of query operations compared to command operations, focusing on read optimization and performance rather than domain complexity.
