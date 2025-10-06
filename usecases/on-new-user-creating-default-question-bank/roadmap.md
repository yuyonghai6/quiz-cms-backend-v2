# Implementation Roadmap: Default Question Bank Creation (TDD Approach)

## Overview

This roadmap follows **Test-Driven Development (TDD)** methodology with the **Red-Green-Refactor** cycle. Each phase begins with writing failing tests, then implementing code to pass tests, followed by refactoring.

### TDD Principles Applied
1. **Red**: Write a failing test that defines desired behavior
2. **Green**: Write minimal code to make the test pass
3. **Refactor**: Improve code quality while keeping tests green
4. **Repeat**: Continue cycle for each feature

### Testing Pyramid Strategy
```
                    ▲
                   / \
                  /   \
                 /  E2E \          (Few, slow, expensive)
                /_______\
               /         \
              / Integration\       (Some, moderate speed)
             /___________  \
            /               \
           /   Unit Tests    \     (Many, fast, cheap)
          /___________________\
```

- **Unit Tests**: 60% of tests - Fast, isolated, mock dependencies
- **Integration Tests**: 30% of tests - Real MongoDB (Testcontainers), real dependencies
- **E2E Tests**: 10% of tests - Full HTTP → MongoDB flow

---

## Phase 1: Foundation Setup (TDD Setup)

### Milestone 1.1: Project Structure and Dependencies
**Duration**: 1 day
**Test Coverage Goal**: N/A (infrastructure setup)

#### Tasks
1. **Add Maven Dependencies** (No tests required)
   ```xml
   <!-- pom.xml additions -->
   <dependency>
       <groupId>org.testcontainers</groupId>
       <artifactId>mongodb</artifactId>
       <scope>test</scope>
   </dependency>
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-data-mongodb</artifactId>
   </dependency>
   ```

2. **Create Package Structure**
   ```
   internal-layer/question-bank/src/main/java/com/quizfun/questionbank/
   ├── application/
   │   ├── commands/
   │   │   └── OnNewUserCreateDefaultQuestionBankCommand.java
   │   ├── handlers/
   │   │   └── OnNewUserCreateDefaultQuestionBankCommandHandler.java
   │   ├── services/
   │   │   └── DefaultQuestionBankApplicationService.java
   │   ├── dto/
   │   │   ├── DefaultQuestionBankRequestDto.java
   │   │   └── DefaultQuestionBankResponseDto.java
   │   └── ports/out/
   │       └── DefaultQuestionBankRepository.java
   ├── domain/
   │   └── exceptions/
   │       ├── DuplicateUserException.java
   │       └── TemplateProcessingException.java
   └── infrastructure/
       ├── persistence/
       │   ├── documents/
       │   │   ├── QuestionBanksPerUserDocument.java
       │   │   └── TaxonomySetDocument.java
       │   ├── repositories/
       │   │   └── MongoDefaultQuestionBankRepository.java
       │   └── mappers/
       │       ├── QuestionBanksPerUserMapper.java
       │       └── TaxonomySetMapper.java
       └── templates/
           └── TemplateVariableReplacer.java

   orchestration-layer/src/main/java/com/quizfun/orchestrationlayer/
   ├── controllers/
   │   └── DefaultQuestionBankController.java
   └── dto/
       ├── CreateDefaultQuestionBankRequestDto.java
       └── CreateDefaultQuestionBankResponseDto.java
   ```

3. **Setup Testcontainers Configuration** (With smoke test)
   ```java
   // File: BaseIntegrationTest.java
   @Testcontainers
   @SpringBootTest
   public abstract class BaseIntegrationTest {
       @Container
       static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0");

       @DynamicPropertySource
       static void setProperties(DynamicPropertyRegistry registry) {
           registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
       }
   }

   // Smoke test - WRITE THIS FIRST (TDD Red)
   @Test
   void smokeTest_mongoContainerShouldStart() {
       assertTrue(mongoDBContainer.isRunning());
   }
   ```

**Deliverables**:
- ✅ Package structure created
- ✅ Maven dependencies added
- ✅ Testcontainers smoke test passing

---

## Phase 2: Domain Layer (TDD - Core Business Logic)

### Milestone 2.1: Command Object with Validation
**Duration**: 0.5 day
**Test Coverage Goal**: 100%

#### TDD Cycle 1: Command Creation

**RED - Write Failing Test First**
```java
// File: OnNewUserCreateDefaultQuestionBankCommandTest.java
@Test
void shouldCreateCommandWithValidUserId() {
    // Given
    Long userId = 123456789L;

    // When
    OnNewUserCreateDefaultQuestionBankCommand command =
        new OnNewUserCreateDefaultQuestionBankCommand(userId, null, null);

    // Then
    assertThat(command.getUserId()).isEqualTo(userId);
}

@Test
void shouldThrowExceptionWhenUserIdIsNull() {
    // When & Then
    assertThatThrownBy(() -> new OnNewUserCreateDefaultQuestionBankCommand(null, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("userId cannot be null");
}

@Test
void shouldThrowExceptionWhenUserIdIsZero() {
    // When & Then
    assertThatThrownBy(() -> new OnNewUserCreateDefaultQuestionBankCommand(0L, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("userId must be positive");
}

@Test
void shouldThrowExceptionWhenUserIdIsNegative() {
    // When & Then
    assertThatThrownBy(() -> new OnNewUserCreateDefaultQuestionBankCommand(-1L, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("userId must be positive");
}
```

**GREEN - Implement Minimal Code**
```java
// File: OnNewUserCreateDefaultQuestionBankCommand.java
public class OnNewUserCreateDefaultQuestionBankCommand implements ICommand<DefaultQuestionBankResponseDto> {
    private final Long userId;
    private final String userEmail;
    private final Map<String, String> metadata;

    public OnNewUserCreateDefaultQuestionBankCommand(
            Long userId,
            String userEmail,
            Map<String, String> metadata) {

        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }
        if (userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }

        this.userId = userId;
        this.userEmail = userEmail;
        this.metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
    }

    public Long getUserId() { return userId; }
    public String getUserEmail() { return userEmail; }
    public Map<String, String> getMetadata() { return metadata; }
}
```

**REFACTOR - Improve (if needed)**
- Extract validation to private methods
- Add builder pattern if constructor becomes complex

**Deliverables**:
- ✅ Command class with full validation
- ✅ 100% unit test coverage for command
- ✅ All edge cases tested (null, zero, negative)

---

### Milestone 2.2: Aggregate Enhancements (If Needed)
**Duration**: 0.5 day
**Test Coverage Goal**: 100% for new methods

#### TDD Cycle 2: Aggregate Factory Method (if creating new method)

**RED - Write Test**
```java
// File: QuestionBanksPerUserAggregateTest.java
@Test
void shouldCreateDefaultQuestionBankForNewUser() {
    // Given
    Long userId = 123456789L;
    Long questionBankId = 1730832000000000L;
    Instant now = Instant.now();

    // When
    QuestionBanksPerUserAggregate aggregate =
        QuestionBanksPerUserAggregate.createDefault(userId, questionBankId, now);

    // Then
    assertThat(aggregate.getUserId()).isEqualTo(userId);
    assertThat(aggregate.getDefaultQuestionBankId()).isEqualTo(questionBankId);
    assertThat(aggregate.getQuestionBanks()).hasSize(1);
    assertThat(aggregate.getQuestionBanks().get(0).getName())
        .isEqualTo("Default Question Bank");
    assertThat(aggregate.getQuestionBanks().get(0).isActive()).isTrue();
}
```

**GREEN - Implement**
```java
// File: QuestionBanksPerUserAggregate.java
public static QuestionBanksPerUserAggregate createDefault(
        Long userId,
        Long questionBankId,
        Instant timestamp) {

    QuestionBank defaultBank = new QuestionBank(
        questionBankId,
        "Default Question Bank",
        "Your default question bank for getting started with quiz creation",
        true,
        timestamp,
        timestamp
    );

    return create(
        new ObjectId(),
        userId,
        questionBankId,
        List.of(defaultBank)
    );
}
```

**REFACTOR** - Extract constants, improve readability

**Deliverables**:
- ✅ Factory method for default creation (if needed)
- ✅ Tests for factory method
- ✅ Existing aggregate tests still passing

---

## Phase 3: Infrastructure Layer (TDD - Persistence)

### Milestone 3.1: MongoDB Document Models
**Duration**: 1 day
**Test Coverage Goal**: 100% for mappers

#### TDD Cycle 3: Document Mapping

**RED - Write Mapper Tests**
```java
// File: QuestionBanksPerUserMapperTest.java
@Test
void shouldMapAggregateToDocument() {
    // Given
    Long userId = 123456789L;
    Long questionBankId = 1730832000000000L;
    Instant now = Instant.now();
    QuestionBanksPerUserAggregate aggregate =
        QuestionBanksPerUserAggregate.createDefault(userId, questionBankId, now);

    // When
    QuestionBanksPerUserDocument document = mapper.toDocument(aggregate);

    // Then
    assertThat(document.getUserId()).isEqualTo(userId);
    assertThat(document.getDefaultQuestionBankId()).isEqualTo(questionBankId);
    assertThat(document.getQuestionBanks()).hasSize(1);
}

@Test
void shouldMapDocumentToAggregate() {
    // Given
    QuestionBanksPerUserDocument document = createSampleDocument();

    // When
    QuestionBanksPerUserAggregate aggregate = mapper.toAggregate(document);

    // Then
    assertThat(aggregate.getUserId()).isEqualTo(document.getUserId());
    assertThat(aggregate.getDefaultQuestionBankId())
        .isEqualTo(document.getDefaultQuestionBankId());
}
```

**GREEN - Implement Documents and Mappers**
```java
// File: QuestionBanksPerUserDocument.java
@Document(collection = "question_banks_per_user")
public class QuestionBanksPerUserDocument {
    @Id
    private ObjectId id;

    @Indexed(unique = true)
    @Field("user_id")
    private Long userId;

    @Field("default_question_bank_id")
    private Long defaultQuestionBankId;

    @Field("question_banks")
    private List<QuestionBankEmbedded> questionBanks;

    @Field("created_at")
    private Instant createdAt;

    @Field("updated_at")
    private Instant updatedAt;

    // Getters, setters, constructors
}

// File: QuestionBanksPerUserMapper.java
@Component
public class QuestionBanksPerUserMapper {
    public QuestionBanksPerUserDocument toDocument(QuestionBanksPerUserAggregate aggregate) {
        // Mapping logic
    }

    public QuestionBanksPerUserAggregate toAggregate(QuestionBanksPerUserDocument document) {
        // Mapping logic
    }
}
```

**REFACTOR** - Use MapStruct or improve manual mapping

**Deliverables**:
- ✅ QuestionBanksPerUserDocument with annotations
- ✅ TaxonomySetDocument with annotations
- ✅ Mappers with 100% test coverage
- ✅ All mapping edge cases tested

---

### Milestone 3.2: Template Processing
**Duration**: 1 day
**Test Coverage Goal**: 100%

#### TDD Cycle 4: Template Variable Replacement

**RED - Write Test**
```java
// File: TemplateVariableReplacerTest.java
@Test
void shouldReplaceAllVariablesInTemplate() {
    // Given
    String template = """
        {
          "user_id": "{{NEW_USER_ID}}",
          "question_bank_id": "{{GENERATED_DEFAULT_BANK_ID}}",
          "_id": "{{GENERATED_OBJECT_ID}}",
          "created_at": "{{CURRENT_TIMESTAMP}}"
        }
        """;

    Map<String, String> variables = Map.of(
        "{{NEW_USER_ID}}", "123456789",
        "{{GENERATED_DEFAULT_BANK_ID}}", "1730832000000000",
        "{{GENERATED_OBJECT_ID}}", "67029a8c1234567890abcdef",
        "{{CURRENT_TIMESTAMP}}", "2025-10-06T10:30:15.123Z"
    );

    // When
    String result = replacer.replace(template, variables);

    // Then
    assertThat(result).contains("\"user_id\": \"123456789\"");
    assertThat(result).contains("\"question_bank_id\": \"1730832000000000\"");
    assertThat(result).doesNotContain("{{");
}

@Test
void shouldThrowExceptionWhenVariableNotProvided() {
    // Given
    String template = "{ \"id\": \"{{MISSING_VAR}}\" }";
    Map<String, String> variables = Map.of();

    // When & Then
    assertThatThrownBy(() -> replacer.replace(template, variables))
        .isInstanceOf(TemplateProcessingException.class)
        .hasMessageContaining("MISSING_VAR");
}

@Test
void shouldLoadTemplateFromClasspath() throws IOException {
    // When
    String template = replacer.loadTemplate("question_banks_per_user.json_template");

    // Then
    assertThat(template).contains("{{NEW_USER_ID}}");
    assertThat(template).contains("{{GENERATED_DEFAULT_BANK_ID}}");
}
```

**GREEN - Implement**
```java
// File: TemplateVariableReplacer.java
@Component
public class TemplateVariableReplacer {

    public String replace(String template, Map<String, String> variables) {
        String result = template;

        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }

        // Check for unreplaced variables
        if (result.contains("{{")) {
            Pattern pattern = Pattern.compile("\\{\\{([^}]+)\\}\\}");
            Matcher matcher = pattern.matcher(result);
            if (matcher.find()) {
                throw new TemplateProcessingException(
                    "Unreplaced variable: " + matcher.group(1)
                );
            }
        }

        return result;
    }

    public String loadTemplate(String filename) throws IOException {
        ClassPathResource resource = new ClassPathResource("templates/" + filename);
        return Files.readString(resource.getFile().toPath());
    }
}
```

**REFACTOR** - Add caching for loaded templates

**Deliverables**:
- ✅ TemplateVariableReplacer service
- ✅ Template loading from classpath
- ✅ 100% test coverage including edge cases
- ✅ Error handling for missing templates/variables

---

### Milestone 3.3: Repository Implementation with MongoDB Transactions
**Duration**: 2 days
**Test Coverage Goal**: 100% integration tests

#### TDD Cycle 5: Repository with Testcontainers

**RED - Write Integration Test**
```java
// File: MongoDefaultQuestionBankRepositoryIntegrationTest.java
@SpringBootTest
@Testcontainers
class MongoDefaultQuestionBankRepositoryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private DefaultQuestionBankRepository repository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Test
    void shouldCreateDefaultQuestionBankSuccessfully() {
        // Given
        Long userId = 123456789L;
        Long questionBankId = 1730832000000000L;
        QuestionBanksPerUserAggregate aggregate =
            QuestionBanksPerUserAggregate.createDefault(userId, questionBankId, Instant.now());
        TaxonomySetAggregate taxonomyAggregate =
            TaxonomySetAggregate.createDefault(userId, questionBankId, Instant.now());

        // When
        Result<DefaultQuestionBankResponseDto> result =
            repository.createDefaultQuestionBank(aggregate, taxonomyAggregate);

        // Then
        assertThat(result.success()).isTrue();

        // Verify MongoDB documents
        QuestionBanksPerUserDocument doc = mongoTemplate.findOne(
            Query.query(Criteria.where("user_id").is(userId)),
            QuestionBanksPerUserDocument.class
        );
        assertThat(doc).isNotNull();
        assertThat(doc.getUserId()).isEqualTo(userId);

        TaxonomySetDocument taxDoc = mongoTemplate.findOne(
            Query.query(Criteria.where("user_id").is(userId)),
            TaxonomySetDocument.class
        );
        assertThat(taxDoc).isNotNull();
    }

    @Test
    void shouldRollbackTransactionOnFailure() {
        // Given - Create a scenario that causes transaction failure
        // (e.g., duplicate user_id after first insert)

        // When & Then
        // Verify transaction rollback
        // Verify no documents in MongoDB
    }

    @Test
    void shouldReturnFailureWhenUserAlreadyExists() {
        // Given
        Long userId = 123456789L;
        // Insert user first
        repository.createDefaultQuestionBank(...);

        // When - Try to create again
        Result<DefaultQuestionBankResponseDto> result =
            repository.createDefaultQuestionBank(...);

        // Then
        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("DUPLICATE_USER");
    }
}
```

**GREEN - Implement Repository**
```java
// File: MongoDefaultQuestionBankRepository.java
@Repository
public class MongoDefaultQuestionBankRepository implements DefaultQuestionBankRepository {

    private final MongoTemplate mongoTemplate;
    private final QuestionBanksPerUserMapper questionBanksMapper;
    private final TaxonomySetMapper taxonomySetMapper;

    @Override
    @Transactional
    public Result<DefaultQuestionBankResponseDto> createDefaultQuestionBank(
            QuestionBanksPerUserAggregate questionBanksAggregate,
            TaxonomySetAggregate taxonomyAggregate) {

        ClientSession session = mongoTemplate.getMongoClient().startSession();

        try {
            return session.withTransaction(() -> {
                // 1. Check user existence
                boolean exists = checkUserExists(
                    questionBanksAggregate.getUserId(),
                    session
                );
                if (exists) {
                    return Result.failure("DUPLICATE_USER: User already has question banks");
                }

                // 2. Map and insert question_banks_per_user
                QuestionBanksPerUserDocument doc =
                    questionBanksMapper.toDocument(questionBanksAggregate);
                mongoTemplate.insert(doc, "question_banks_per_user");

                // 3. Map and insert taxonomy_sets
                TaxonomySetDocument taxDoc =
                    taxonomySetMapper.toDocument(taxonomyAggregate);
                mongoTemplate.insert(taxDoc, "taxonomy_sets");

                // 4. Build response
                return Result.success(buildResponseDto(doc, taxDoc));
            });
        } finally {
            session.close();
        }
    }

    private boolean checkUserExists(Long userId, ClientSession session) {
        Query query = Query.query(Criteria.where("user_id").is(userId));
        return mongoTemplate.exists(query, QuestionBanksPerUserDocument.class);
    }
}
```

**REFACTOR** - Extract transaction logic, improve error handling

**Deliverables**:
- ✅ Repository implementation with MongoDB transactions
- ✅ Integration tests with Testcontainers (all scenarios)
- ✅ Transaction rollback verified
- ✅ Duplicate user detection working

---

## Phase 4: Application Layer (TDD - Use Case Orchestration)

### Milestone 4.1: Application Service
**Duration**: 1 day
**Test Coverage Goal**: 100%

#### TDD Cycle 6: Application Service

**RED - Write Test**
```java
// File: DefaultQuestionBankApplicationServiceTest.java
@ExtendWith(MockitoExtension.class)
class DefaultQuestionBankApplicationServiceTest {

    @Mock
    private DefaultQuestionBankRepository repository;

    @Mock
    private LongIdGenerator longIdGenerator;

    @Mock
    private TemplateVariableReplacer templateReplacer;

    @InjectMocks
    private DefaultQuestionBankApplicationService service;

    @Test
    void shouldCreateDefaultQuestionBankSuccessfully() {
        // Given
        Long userId = 123456789L;
        Long questionBankId = 1730832000000000L;

        when(longIdGenerator.generateQuestionBankId()).thenReturn(questionBankId);
        when(repository.createDefaultQuestionBank(any(), any()))
            .thenReturn(Result.success(createMockResponse()));

        // When
        Result<DefaultQuestionBankResponseDto> result =
            service.createDefaultQuestionBank(userId, null, null);

        // Then
        assertThat(result.success()).isTrue();
        verify(longIdGenerator).generateQuestionBankId();
        verify(repository).createDefaultQuestionBank(any(), any());
    }

    @Test
    void shouldHandleRepositoryFailure() {
        // Given
        when(longIdGenerator.generateQuestionBankId()).thenReturn(1L);
        when(repository.createDefaultQuestionBank(any(), any()))
            .thenReturn(Result.failure("DUPLICATE_USER"));

        // When
        Result<DefaultQuestionBankResponseDto> result =
            service.createDefaultQuestionBank(123L, null, null);

        // Then
        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("DUPLICATE_USER");
    }
}
```

**GREEN - Implement**
```java
// File: DefaultQuestionBankApplicationService.java
@Service
public class DefaultQuestionBankApplicationService {

    private final DefaultQuestionBankRepository repository;
    private final LongIdGenerator longIdGenerator;
    private final TemplateVariableReplacer templateReplacer;

    public Result<DefaultQuestionBankResponseDto> createDefaultQuestionBank(
            Long userId,
            String userEmail,
            Map<String, String> metadata) {

        try {
            // 1. Generate question bank ID
            Long questionBankId = longIdGenerator.generateQuestionBankId();
            Instant now = Instant.now();

            // 2. Create aggregates
            QuestionBanksPerUserAggregate questionBanksAggregate =
                QuestionBanksPerUserAggregate.createDefault(userId, questionBankId, now);

            TaxonomySetAggregate taxonomyAggregate =
                TaxonomySetAggregate.createDefault(userId, questionBankId, now);

            // 3. Persist via repository
            return repository.createDefaultQuestionBank(
                questionBanksAggregate,
                taxonomyAggregate
            );

        } catch (Exception ex) {
            return Result.failure("INTERNAL_ERROR: " + ex.getMessage());
        }
    }
}
```

**REFACTOR** - Improve exception handling, add logging

**Deliverables**:
- ✅ Application service with full orchestration
- ✅ 100% unit test coverage (mocked dependencies)
- ✅ All error scenarios tested

---

### Milestone 4.2: Command Handler
**Duration**: 0.5 day
**Test Coverage Goal**: 100%

#### TDD Cycle 7: Command Handler

**RED - Write Test**
```java
// File: OnNewUserCreateDefaultQuestionBankCommandHandlerTest.java
@ExtendWith(MockitoExtension.class)
class OnNewUserCreateDefaultQuestionBankCommandHandlerTest {

    @Mock
    private DefaultQuestionBankApplicationService applicationService;

    @InjectMocks
    private OnNewUserCreateDefaultQuestionBankCommandHandler handler;

    @Test
    void shouldHandleCommandSuccessfully() {
        // Given
        OnNewUserCreateDefaultQuestionBankCommand command =
            new OnNewUserCreateDefaultQuestionBankCommand(123456789L, null, null);

        when(applicationService.createDefaultQuestionBank(any(), any(), any()))
            .thenReturn(Result.success(createMockResponse()));

        // When
        Result<DefaultQuestionBankResponseDto> result = handler.handle(command);

        // Then
        assertThat(result.success()).isTrue();
        verify(applicationService).createDefaultQuestionBank(
            123456789L,
            null,
            null
        );
    }
}
```

**GREEN - Implement**
```java
// File: OnNewUserCreateDefaultQuestionBankCommandHandler.java
@Service
public class OnNewUserCreateDefaultQuestionBankCommandHandler
        implements ICommandHandler<OnNewUserCreateDefaultQuestionBankCommand, DefaultQuestionBankResponseDto> {

    private final DefaultQuestionBankApplicationService applicationService;

    @Override
    public Result<DefaultQuestionBankResponseDto> handle(
            OnNewUserCreateDefaultQuestionBankCommand command) {

        return applicationService.createDefaultQuestionBank(
            command.getUserId(),
            command.getUserEmail(),
            command.getMetadata()
        );
    }
}
```

**Deliverables**:
- ✅ Command handler implementation
- ✅ 100% test coverage
- ✅ Handler auto-registered with mediator

---

## Phase 5: Orchestration Layer (TDD - HTTP Boundary)

### Milestone 5.1: REST Controller
**Duration**: 1 day
**Test Coverage Goal**: 100%

#### TDD Cycle 8: Controller with MockMvc

**RED - Write Controller Test**
```java
// File: DefaultQuestionBankControllerTest.java
@WebMvcTest(DefaultQuestionBankController.class)
class DefaultQuestionBankControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IMediator mediator;

    @Test
    void shouldReturn201WhenQuestionBankCreatedSuccessfully() throws Exception {
        // Given
        when(mediator.send(any(OnNewUserCreateDefaultQuestionBankCommand.class)))
            .thenReturn(Result.success(createMockResponse()));

        // When & Then
        mockMvc.perform(post("/api/users/default-question-bank")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "userId": 123456789,
                      "userEmail": "test@example.com"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.userId").value(123456789))
            .andExpect(jsonPath("$.data.questionBankId").exists());
    }

    @Test
    void shouldReturn400WhenUserIdIsInvalid() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/users/default-question-bank")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "userId": -1
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value(containsString("VALIDATION_ERROR")));
    }

    @Test
    void shouldReturn409WhenUserAlreadyExists() throws Exception {
        // Given
        when(mediator.send(any(OnNewUserCreateDefaultQuestionBankCommand.class)))
            .thenReturn(Result.failure("DUPLICATE_USER: User already has question banks"));

        // When & Then
        mockMvc.perform(post("/api/users/default-question-bank")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "userId": 123456789
                    }
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value(containsString("DUPLICATE_USER")));
    }

    @Test
    void shouldReturn500WhenDatabaseErrorOccurs() throws Exception {
        // Given
        when(mediator.send(any(OnNewUserCreateDefaultQuestionBankCommand.class)))
            .thenReturn(Result.failure("DATABASE_ERROR: Transaction failed"));

        // When & Then
        mockMvc.perform(post("/api/users/default-question-bank")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "userId": 123456789
                    }
                    """))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.success").value(false));
    }
}
```

**GREEN - Implement Controller**
```java
// File: DefaultQuestionBankController.java
@RestController
@RequestMapping("/api/users/default-question-bank")
@Validated
public class DefaultQuestionBankController {

    private final IMediator mediator;

    @PostMapping
    public ResponseEntity<Result<DefaultQuestionBankResponseDto>> createDefaultQuestionBank(
            @Valid @RequestBody CreateDefaultQuestionBankRequestDto request) {

        try {
            // Create command
            OnNewUserCreateDefaultQuestionBankCommand command =
                new OnNewUserCreateDefaultQuestionBankCommand(
                    request.getUserId(),
                    request.getUserEmail(),
                    request.getMetadata()
                );

            // Send via mediator
            Result<DefaultQuestionBankResponseDto> result = mediator.send(command);

            if (result.success()) {
                return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .header("X-Question-Bank-ID", result.data().getQuestionBankId().toString())
                    .body(result);
            } else {
                return mapErrorToHttpStatus(result);
            }

        } catch (IllegalArgumentException ex) {
            return ResponseEntity
                .badRequest()
                .body(Result.failure("VALIDATION_ERROR: " + ex.getMessage()));
        }
    }

    private ResponseEntity<Result<DefaultQuestionBankResponseDto>> mapErrorToHttpStatus(
            Result<DefaultQuestionBankResponseDto> result) {

        if (result.message().startsWith("DUPLICATE_USER")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
        } else if (result.message().startsWith("DATABASE_ERROR") ||
                   result.message().startsWith("INTERNAL_ERROR")) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }
}
```

**REFACTOR** - Extract error mapping, add logging

**Deliverables**:
- ✅ REST controller implementation
- ✅ 100% test coverage with MockMvc
- ✅ All HTTP status codes tested
- ✅ Request validation working

---

## Phase 6: End-to-End Integration (TDD - Full Flow)

### Milestone 6.1: E2E Integration Tests
**Duration**: 1 day
**Test Coverage Goal**: All happy paths + critical error paths

#### TDD Cycle 9: Full E2E Test

**RED - Write E2E Test**
```java
// File: DefaultQuestionBankE2ETest.java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class DefaultQuestionBankE2ETest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Test
    void shouldCreateDefaultQuestionBankEndToEnd() {
        // Given
        CreateDefaultQuestionBankRequestDto request =
            CreateDefaultQuestionBankRequestDto.builder()
                .userId(123456789L)
                .userEmail("test@example.com")
                .build();

        // When
        ResponseEntity<Result> response = restTemplate.postForEntity(
            "/api/users/default-question-bank",
            request,
            Result.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().success()).isTrue();

        // Verify MongoDB state
        QuestionBanksPerUserDocument doc = mongoTemplate.findOne(
            Query.query(Criteria.where("user_id").is(123456789L)),
            QuestionBanksPerUserDocument.class
        );
        assertThat(doc).isNotNull();
        assertThat(doc.getQuestionBanks()).hasSize(1);
        assertThat(doc.getQuestionBanks().get(0).getName())
            .isEqualTo("Default Question Bank");

        TaxonomySetDocument taxDoc = mongoTemplate.findOne(
            Query.query(Criteria.where("user_id").is(123456789L)),
            TaxonomySetDocument.class
        );
        assertThat(taxDoc).isNotNull();
        assertThat(taxDoc.getTags()).hasSize(3);
        assertThat(taxDoc.getCategories().getLevel1().getId()).isEqualTo("general");
    }

    @Test
    void shouldReturn409WhenCreatingDuplicateUser() {
        // Given - Create user first time
        CreateDefaultQuestionBankRequestDto request =
            CreateDefaultQuestionBankRequestDto.builder()
                .userId(987654321L)
                .build();
        restTemplate.postForEntity("/api/users/default-question-bank", request, Result.class);

        // When - Try to create again
        ResponseEntity<Result> response = restTemplate.postForEntity(
            "/api/users/default-question-bank",
            request,
            Result.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).contains("DUPLICATE_USER");
    }
}
```

**GREEN - Fix Any Integration Issues**
- Ensure all layers connect properly
- Fix any configuration issues
- Resolve dependency injection problems

**REFACTOR** - Optimize queries, improve performance

**Deliverables**:
- ✅ E2E tests passing (HTTP → MongoDB)
- ✅ All critical paths tested
- ✅ MongoDB state verified in tests
- ✅ Performance acceptable (< 500ms p95)

---

## Phase 7: Documentation and Deployment Preparation

### Milestone 7.1: API Documentation
**Duration**: 0.5 day

#### Tasks
1. **OpenAPI/Swagger Documentation**
   ```java
   @Operation(summary = "Create default question bank for new user")
   @ApiResponses(value = {
       @ApiResponse(responseCode = "201", description = "Successfully created"),
       @ApiResponse(responseCode = "400", description = "Invalid request"),
       @ApiResponse(responseCode = "409", description = "User already exists"),
       @ApiResponse(responseCode = "500", description = "Internal server error")
   })
   @PostMapping
   public ResponseEntity<Result<DefaultQuestionBankResponseDto>> createDefaultQuestionBank(...) {
   ```

2. **README Updates**
   - Add use case to main README
   - Document API endpoint
   - Add cURL examples

**Deliverables**:
- ✅ OpenAPI documentation complete
- ✅ README updated
- ✅ cURL examples provided

---

### Milestone 7.2: Code Quality and Coverage
**Duration**: 0.5 day

#### Tasks
1. **Jacoco Coverage Report**
   ```bash
   mvn clean verify
   mvn jacoco:report
   # Target: >80% line coverage
   ```

2. **SonarQube Analysis** (if available)
   ```bash
   mvn sonar:sonar
   ```

3. **Code Review Preparation**
   - Run `mvn allure:report` for test reporting
   - Generate coverage badges
   - Document any technical debt

**Deliverables**:
- ✅ >80% line coverage achieved
- ✅ All tests passing (unit + integration + E2E)
- ✅ Code quality metrics acceptable
- ✅ Allure report generated

---

## Testing Summary by Phase

| Phase | Test Type | Coverage Goal | Tools |
|-------|-----------|---------------|-------|
| Phase 2 | Unit (Domain) | 100% | JUnit 5, AssertJ |
| Phase 3 | Integration (Infrastructure) | 100% | Testcontainers, Spring Data Test |
| Phase 4 | Unit (Application) | 100% | Mockito, JUnit 5 |
| Phase 5 | Integration (Controller) | 100% | MockMvc, Spring Boot Test |
| Phase 6 | E2E | Happy paths + critical errors | TestRestTemplate, Testcontainers |

**Total Estimated Test Count**: 50-60 tests

---

## TDD Best Practices Applied

### 1. Test Naming Convention
```java
// Pattern: should[ExpectedBehavior]When[Condition]
@Test
void shouldCreateCommandWithValidUserId() { ... }

@Test
void shouldThrowExceptionWhenUserIdIsNull() { ... }

@Test
void shouldRollbackTransactionOnFailure() { ... }
```

### 2. AAA Pattern (Arrange-Act-Assert)
```java
@Test
void exampleTest() {
    // Arrange (Given)
    Long userId = 123456789L;

    // Act (When)
    Result result = service.createDefaultQuestionBank(userId);

    // Assert (Then)
    assertThat(result.success()).isTrue();
}
```

### 3. Test Isolation
- Each test runs in clean state
- Testcontainers provides fresh MongoDB per test class
- `@DirtiesContext` used when needed
- No shared mutable state between tests

### 4. Meaningful Assertions
```java
// Bad
assertTrue(result.success());

// Good
assertThat(result.success()).isTrue();
assertThat(result.data().getQuestionBankId()).isEqualTo(expectedId);
assertThat(result.data().getQuestionBankName()).isEqualTo("Default Question Bank");
```

### 5. Test Data Builders
```java
// Create reusable test data builders
public class QuestionBankTestDataBuilder {
    public static QuestionBanksPerUserAggregate createDefaultAggregate(Long userId) {
        return QuestionBanksPerUserAggregate.createDefault(
            userId,
            1730832000000000L,
            Instant.now()
        );
    }
}
```

---

## Deployment Checklist

### Pre-Deployment
- [ ] All tests passing (unit + integration + E2E)
- [ ] Coverage >80%
- [ ] No critical SonarQube issues
- [ ] MongoDB indexes created
- [ ] API documentation complete
- [ ] Logging configured

### Deployment
- [ ] Testcontainers configuration in test profile only
- [ ] Production MongoDB connection string configured
- [ ] Environment variables set (if needed)
- [ ] Health check endpoint working

### Post-Deployment
- [ ] Smoke test in staging environment
- [ ] Performance test (< 500ms p95)
- [ ] Error monitoring configured
- [ ] Alert thresholds set

---

## Risk Mitigation

### Risk 1: MongoDB Transaction Support
**Risk**: MongoDB transactions require replica set
**Mitigation**:
- Testcontainers MongoDB configured with replica set
- Document in README that production needs replica set

### Risk 2: Template File Not Found
**Risk**: Template files not included in JAR
**Mitigation**:
- Place templates in `src/main/resources/templates/`
- Add tests for template loading
- Include templates in build configuration

### Risk 3: ID Generation Collisions
**Risk**: LongIdGenerator produces duplicate IDs under high concurrency
**Mitigation**:
- Existing tests show >500K IDs/sec with zero collisions
- Thread-safe implementation with synchronized method
- Fallback: retry logic in case of collision

### Risk 4: Testcontainers Failure in CI
**Risk**: CI environment doesn't support Docker
**Mitigation**:
- Document Docker requirement in README
- Provide alternative: in-memory MongoDB (with limitations)
- Add CI pipeline Docker check

---

## Success Criteria

### Functional
- ✅ HTTP POST /api/users/default-question-bank creates question bank
- ✅ MongoDB documents created atomically
- ✅ Duplicate user detection returns 409
- ✅ All validation rules enforced

### Non-Functional
- ✅ Response time < 500ms (p95)
- ✅ Test coverage >80%
- ✅ Zero critical bugs
- ✅ API documentation complete

### Quality
- ✅ All TDD cycles completed (Red-Green-Refactor)
- ✅ 50+ tests written and passing
- ✅ Code review approved
- ✅ CI/CD pipeline green

---

## Timeline Summary

| Phase | Duration | Dependencies |
|-------|----------|--------------|
| Phase 1: Foundation | 1 day | None |
| Phase 2: Domain Layer | 1 day | Phase 1 |
| Phase 3: Infrastructure | 4 days | Phase 2 |
| Phase 4: Application Layer | 1.5 days | Phase 3 |
| Phase 5: Orchestration Layer | 1 day | Phase 4 |
| Phase 6: E2E Integration | 1 day | Phase 5 |
| Phase 7: Documentation | 1 day | Phase 6 |
| **Total** | **10.5 days** | - |

**Note**: Timeline assumes 1 developer working full-time. Adjust for team size and part-time allocation.

---

## Next Steps After Completion

### Phase 8: Future Enhancements (Not in Current Scope)
1. **Domain Events**: Publish `DefaultQuestionBankCreatedEvent`
2. **Async Processing**: Queue-based command processing
3. **Idempotency Keys**: Request ID-based deduplication
4. **Audit Logging**: Track all creation events
5. **Metrics**: Prometheus/Grafana dashboards
6. **Rate Limiting**: Prevent abuse from external system
