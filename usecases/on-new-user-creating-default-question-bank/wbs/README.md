# WBS (Work Breakdown Structure) - Default Question Bank Creation

## Epic
**Use Case On New User Create Default Question Bank Happy Path**

---

## User Stories Overview

This directory contains detailed Work Breakdown Structure (WBS) documents for implementing the "Create Default Question Bank on New User" use case using Test-Driven Development (TDD).

### Story List

| ID | User Story | Estimated Effort | Status | Dependencies |
|----|------------|------------------|--------|--------------|
| 1001 | [Command and Validation](1001.command-and-validation.md) | 0.5 day | ✅ Ready | None |
| 1002 | [Domain Aggregates](1002.domain-aggregates.md) | 0.5 day | ✅ Ready | 1001 |
| 1003 | MongoDB Documents and Mappers | 1 day | 📝 Planned | 1002 |
| 1004 | Template Processing | 1 day | 📝 Planned | None |
| 1005 | [Repository with Transactions](1005.repository-with-transactions.md) | 1 day | ✅ Ready | 1002, 1003, 1004 |
| 1006 | Application Service | 0.5 day | 📝 Planned | 1002, 1004, 1005 |
| 1007 | Command Handler | 0.5 day | 📝 Planned | 1001, 1006 |
| 1008 | REST Controller | 1 day | 📝 Planned | 1007 |

**Total Estimated Effort**: 6.5 days

---

## Allure Annotation Standard

All tests in this use case MUST use the following Allure annotations:

```java
@Epic("Use Case On New User Create Default Question Bank Happy Path")
@Story("[user_story_id].[user_story_name]")
```

### Examples

**Story 1001**:
```java
@Epic("Use Case On New User Create Default Question Bank Happy Path")
@Story("1001.command-and-validation")
```

**Story 1002**:
```java
@Epic("Use Case On New User Create Default Question Bank Happy Path")
@Story("1002.domain-aggregates")
```

**⚠️ IMPORTANT**: NO `@Feature` annotation should be used in this use case.

---

## TDD Approach

Each WBS follows the **Red-Green-Refactor** cycle:

### 1. RED Phase
- Write failing tests BEFORE implementation
- Tests define expected behavior
- Verify tests fail for the right reasons

### 2. GREEN Phase
- Write minimal code to make tests pass
- Focus on functionality, not perfection
- Get to green as quickly as possible

### 3. REFACTOR Phase
- Improve code quality
- Extract constants, methods, classes
- Ensure tests still pass after each change

---

## Story Details

### 1001: Command and Validation ✅
**Focus**: Command object with business rule validation

**Key Deliverables**:
- OnNewUserCreateDefaultQuestionBankCommand class
- userId validation (positive, non-null)
- Email validation (format, optional)
- Immutable metadata map
- DefaultQuestionBankResponseDto

**Test Count**: 10 unit tests
**Coverage**: 100% for command class

---

### 1002: Domain Aggregates ✅
**Focus**: Factory methods for default creation

**Key Deliverables**:
- QuestionBanksPerUserAggregate.createDefault()
- TaxonomySetAggregate.createDefault()
- Domain invariants enforced
- Default values aligned with JSON templates

**Test Count**: 18 unit tests
**Coverage**: 100% for factory methods

---

### 1003: MongoDB Documents and Mappers 📝
**Focus**: Persistence layer document models

**Key Deliverables**:
- QuestionBanksPerUserDocument
- TaxonomySetDocument
- Mappers (aggregate ↔ document)
- MongoDB annotations (@Document, @Field, @Indexed)

**Test Count**: 15+ tests (mappers)
**Coverage**: 100% for mapping logic

**Test Focus**:
- Bidirectional mapping (aggregate → document → aggregate)
- Null handling
- Nested object mapping
- Timestamp preservation

---

### 1004: Template Processing 📝
**Focus**: JSON template loading and variable replacement

**Key Deliverables**:
- TemplateVariableReplacer service
- Template loading from classpath
- Variable substitution logic
- Error handling for missing templates/variables

**Test Count**: 8-10 tests
**Coverage**: 100%

**Test Focus**:
- Load templates from resources/templates/
- Replace all template variables
- Detect unreplaced variables
- Handle missing templates gracefully

---

### 1005: Repository with Transactions ✅
**Focus**: Atomic document creation with MongoDB transactions

**Key Deliverables**:
- MongoDefaultQuestionBankRepository
- MongoDB transaction support
- Duplicate user detection
- Transaction rollback on failure
- Testcontainers integration

**Test Count**: 8-10 integration tests
**Coverage**: 100% with real MongoDB

**Test Focus**:
- Atomic creation (both documents or neither)
- Transaction rollback verification
- Duplicate user handling (409 Conflict)
- Concurrent creation attempts
- Testcontainers connection (NOT localhost:27017)

---

### 1006: Application Service 📝
**Focus**: Use case orchestration

**Key Deliverables**:
- DefaultQuestionBankApplicationService
- ID generation coordination
- Aggregate creation
- Repository coordination
- Error handling

**Test Count**: 10-12 unit tests (with mocks)
**Coverage**: 100%

**Test Focus**:
- Happy path orchestration
- Repository failure handling
- ID generation
- Aggregate validation
- Exception translation to Result<T>

---

### 1007: Command Handler 📝
**Focus**: CQRS mediator integration

**Key Deliverables**:
- OnNewUserCreateDefaultQuestionBankCommandHandler
- ICommandHandler implementation
- Mediator auto-registration
- Command validation
- Service delegation

**Test Count**: 6-8 unit tests
**Coverage**: 100%

**Test Focus**:
- Command handling success
- Command validation failure
- Service error propagation
- Result<T> mapping

---

### 1008: REST Controller 📝
**Focus**: HTTP boundary and error mapping

**Key Deliverables**:
- DefaultQuestionBankController
- POST /api/users/default-question-bank endpoint
- HTTP request/response DTOs
- Error code to HTTP status mapping
- Request validation

**Test Count**: 12-15 tests (MockMvc)
**Coverage**: 100%

**Test Focus**:
- 201 Created (success)
- 400 Bad Request (invalid input)
- 409 Conflict (duplicate user)
- 500 Internal Server Error (system failure)
- Request validation
- Response headers (X-Question-Bank-ID)

---

## Testing Strategy

### Test Distribution

```
Unit Tests:         40-50 tests (Stories 1001, 1002, 1006, 1007)
Integration Tests:  20-25 tests (Stories 1003, 1004, 1005)
Controller Tests:   12-15 tests (Story 1008)
E2E Tests:          5-10 tests  (After all stories complete)
────────────────────────────────────────────────────────────
Total:              77-100 tests
```

### Test Pyramid

```
        /\
       /E2E\         10%  - Full HTTP → MongoDB flow
      /─────\
     /Integ. \       30%  - Real dependencies (MongoDB)
    /_________\
   /           \
  /  Unit Tests \    60%  - Fast, isolated, mocked
 /_______________\
```

### Coverage Goals

- **Overall**: >80% line coverage
- **Commands**: 100% coverage (pure logic)
- **Aggregates**: 100% coverage (domain logic)
- **Repositories**: 100% integration coverage
- **Controllers**: 100% MockMvc coverage

---

## MongoDB Testing Requirements

### ⚠️ CRITICAL: Testcontainers Only

**DO:**
- ✅ Use Testcontainers MongoDB for ALL MongoDB tests
- ✅ Extend `TestContainersConfig` base class
- ✅ Use `@Testcontainers` annotation
- ✅ Clean database before/after each test

**DON'T:**
- ❌ Connect to localhost:27017
- ❌ Assume MongoDB running on host
- ❌ Use standalone MongoDB for tests
- ❌ Use in-memory MongoDB (doesn't support transactions)

### Example Test Setup

```java
@SpringBootTest
@Testcontainers
@TestPropertySource(properties = {
    "spring.data.mongodb.database=test_db"
})
class MyIntegrationTest extends TestContainersConfig {

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection(MyDocument.class);
    }

    @AfterEach
    void tearDown() {
        mongoTemplate.dropCollection(MyDocument.class);
    }

    @Test
    @Epic("Use Case On New User Create Default Question Bank Happy Path")
    @Story("100X.my-story")
    void myTest() {
        // Test using real MongoDB from Testcontainers
    }
}
```

---

## Dependency Graph

```
1001 (Command) ──────────┐
                         ▼
1002 (Aggregates) ──────→ 1005 (Repository) ──┐
                  ┌──────→                     │
1003 (Documents)  ┘                            │
                                               ▼
1004 (Templates) ──────────────────────────→ 1006 (Service)
                                               │
1001 (Command) ────────────────────────────────┼──→ 1007 (Handler)
                                               │         │
                                               └─────────┼──→ 1008 (Controller)
                                                         │
                                                         ▼
                                                      E2E Tests
```

### Recommended Implementation Order

1. **Week 1**: Stories 1001, 1002, 1003, 1004 (foundations)
2. **Week 2**: Story 1005 (repository with transactions)
3. **Week 3**: Stories 1006, 1007, 1008 (service → controller)
4. **Week 4**: E2E tests, integration, polish

---

## Running Tests

### Run Single Story Tests

```bash
# Story 1001 - Command tests
mvn test -pl internal-layer/question-bank \
  -Dtest=OnNewUserCreateDefaultQuestionBankCommandTest

# Story 1002 - Aggregate tests
mvn test -pl internal-layer/question-bank \
  -Dtest=QuestionBanksPerUserAggregateDefaultCreationTest,TaxonomySetAggregateDefaultCreationTest

# Story 1005 - Repository integration tests
mvn test -pl internal-layer/question-bank \
  -Dtest=MongoDefaultQuestionBankRepositoryIntegrationTest
```

### Run All Use Case Tests

```bash
# All tests with coverage
mvn clean verify -pl internal-layer/question-bank

# Generate Allure report
mvn allure:serve
```

### Run Specific Test with Allure

```bash
mvn test -pl internal-layer/question-bank \
  -Dtest=MyTest \
  && mvn allure:serve
```

---

## Code Review Checklist

Before marking a story as complete:

- [ ] All tests written BEFORE implementation (TDD Red)
- [ ] All tests passing (TDD Green)
- [ ] Code refactored for quality (TDD Refactor)
- [ ] Coverage ≥ target (usually 100% for stories)
- [ ] Allure annotations correct (@Epic, @Story)
- [ ] @DisplayName on all tests
- [ ] Javadoc on public methods
- [ ] No hardcoded values (use constants)
- [ ] No localhost:27017 in test code
- [ ] Clean code principles followed
- [ ] Ready for code review

---

## Success Criteria

### Per Story
- ✅ All tests pass
- ✅ Coverage meets target
- ✅ TDD cycle followed
- ✅ Allure report shows correct Epic/Story
- ✅ Code review approved

### Overall Use Case
- ✅ HTTP POST /api/users/default-question-bank works end-to-end
- ✅ MongoDB documents created atomically
- ✅ Duplicate user returns 409 Conflict
- ✅ All validation rules enforced
- ✅ Response time < 500ms (p95)
- ✅ >80% overall coverage
- ✅ All TDD cycles completed
- ✅ CI/CD pipeline green

---

## Additional Resources

### Documentation
- [Architecture Overview](../architecture-overview.md)
- [Use Case Design](../use-case-design.md)
- [Implementation Roadmap](../roadmap.md)
- [Data Modeling](../../../data-modeling/question_bank_data_modeling_v4.md)

### MongoDB Templates
- [question_banks_per_user template](../mongodb-document-sample/question_banks_per_user.json_template)
- [taxonomy_sets template](../mongodb-document-sample/taxonomy_sets.json_template)
- [Template README](../mongodb-document-sample/README.md)

### Testing Tools
- JUnit 5: https://junit.org/junit5/
- AssertJ: https://assertj.github.io/doc/
- Testcontainers: https://www.testcontainers.org/modules/databases/mongodb/
- Allure: https://docs.qameta.io/allure/

---

## Notes

- Each WBS is a self-contained TDD cycle
- Tests define the contract (interface before implementation)
- Testcontainers ensures CI/CD compatibility
- Allure provides visual test reporting
- Stories can be parallelized if dependencies allow
- Integration tests are slower but critical for transaction verification
- Keep tests isolated (no shared state between tests)

---

**Last Updated**: 2025-10-06
**Version**: 1.0
**Status**: In Progress
