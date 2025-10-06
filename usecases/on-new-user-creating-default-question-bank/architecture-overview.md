# Architecture Overview: Default Question Bank Creation

## System Context

### External System Integration
```
┌─────────────────────────────────┐
│  User Management System         │
│  (External - Out of Scope)      │
└────────────┬────────────────────┘
             │ HTTP POST
             │ OnNewUserCreateDefaultQuestionBank
             ▼
┌─────────────────────────────────────────────────────────────┐
│                    Quiz CMS System                          │
│  ┌───────────────────────────────────────────────────────┐  │
│  │         Orchestration Layer                           │  │
│  │  ┌─────────────────────────────────────────────────┐  │  │
│  │  │  DefaultQuestionBankController                  │  │  │
│  │  │  - POST /api/users/default-question-bank       │  │  │
│  │  │  - Extract command from HTTP request           │  │  │
│  │  │  - Validate request                             │  │  │
│  │  └──────────────┬──────────────────────────────────┘  │  │
│  └─────────────────┼──────────────────────────────────────┘  │
│                    │ IMediator.send(command)                 │
│  ┌─────────────────▼──────────────────────────────────────┐  │
│  │         Global Shared Library (Mediator)              │  │
│  │  ┌─────────────────────────────────────────────────┐  │  │
│  │  │  MediatorImpl                                   │  │  │
│  │  │  - Route command to handler                    │  │  │
│  │  └──────────────┬──────────────────────────────────┘  │  │
│  └─────────────────┼──────────────────────────────────────┘  │
│                    │                                         │
│  ┌─────────────────▼──────────────────────────────────────┐  │
│  │         Internal Layer - Question Bank                │  │
│  │                                                        │  │
│  │  ┌──────────────────────────────────────────────────┐ │  │
│  │  │  Application Layer                               │ │  │
│  │  │  ┌────────────────────────────────────────────┐  │ │  │
│  │  │  │ OnNewUserCreateDefaultQuestionBank        │  │ │  │
│  │  │  │ CommandHandler                             │  │ │  │
│  │  │  │ - Validate business rules                  │  │ │  │
│  │  │  │ - Coordinate domain + infrastructure       │  │ │  │
│  │  │  └───┬──────────────────────────────┬─────────┘  │ │  │
│  │  │      │                              │             │ │  │
│  │  │      │                              │             │ │  │
│  │  │  ┌───▼──────────────────────┐  ┌───▼──────────┐  │ │  │
│  │  │  │ DefaultQuestionBank      │  │ Port Out     │  │ │  │
│  │  │  │ ApplicationService       │  │ Interfaces   │  │ │  │
│  │  │  │ - Template replacement   │  │              │  │ │  │
│  │  │  │ - ID generation          │  │              │  │ │  │
│  │  │  │ - Transaction coord.     │  │              │  │ │  │
│  │  │  └──────────────────────────┘  └──────────────┘  │ │  │
│  │  └──────────────────────────────────────────────────┘ │  │
│  │                                                        │  │
│  │  ┌──────────────────────────────────────────────────┐ │  │
│  │  │  Domain Layer                                    │ │  │
│  │  │  ┌────────────────────────────────────────────┐  │ │  │
│  │  │  │ QuestionBanksPerUserAggregate             │  │ │  │
│  │  │  │ - Validate ownership rules                 │  │ │  │
│  │  │  │ - Enforce business invariants              │  │ │  │
│  │  │  └────────────────────────────────────────────┘  │ │  │
│  │  │  ┌────────────────────────────────────────────┐  │ │  │
│  │  │  │ TaxonomySetAggregate                       │  │ │  │
│  │  │  │ - Validate category hierarchy              │  │ │  │
│  │  │  │ - Enforce taxonomy rules                   │  │ │  │
│  │  │  └────────────────────────────────────────────┘  │ │  │
│  │  │  ┌────────────────────────────────────────────┐  │ │  │
│  │  │  │ Domain Entities                            │  │ │  │
│  │  │  │ - QuestionBank (value object)              │  │ │  │
│  │  │  │ - Tag, DifficultyLevel, etc.               │  │ │  │
│  │  │  └────────────────────────────────────────────┘  │ │  │
│  │  └──────────────────────────────────────────────────┘ │  │
│  │                                                        │  │
│  │  ┌──────────────────────────────────────────────────┐ │  │
│  │  │  Infrastructure Layer                            │ │  │
│  │  │  ┌────────────────────────────────────────────┐  │ │  │
│  │  │  │ MongoDefaultQuestionBankRepository         │  │ │  │
│  │  │  │ - Implements port out interfaces           │  │ │  │
│  │  │  │ - MongoDB transactions                     │  │ │  │
│  │  │  │ - Document persistence                     │  │ │  │
│  │  │  └──────────────┬─────────────────────────────┘  │ │  │
│  │  │                 │                                 │ │  │
│  │  │  ┌──────────────▼─────────────────────────────┐  │ │  │
│  │  │  │ MongoDB Documents                          │  │ │  │
│  │  │  │ - QuestionBanksPerUserDocument             │  │ │  │
│  │  │  │ - TaxonomySetDocument                      │  │ │  │
│  │  │  └────────────────────────────────────────────┘  │ │  │
│  │  └──────────────────────────────────────────────────┘ │  │
│  └────────────────────┬───────────────────────────────────┘  │
│                       │                                      │
│                       ▼                                      │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  MongoDB (Testcontainers Only)                      │    │
│  │  Collections:                                       │    │
│  │  - question_banks_per_user                         │    │
│  │  - taxonomy_sets                                    │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

## Architectural Layers

### 1. Orchestration Layer
**Responsibility**: HTTP boundary and request/response transformation

**Components**:
- `DefaultQuestionBankController`: REST endpoint handler
- HTTP DTOs: `CreateDefaultQuestionBankRequestDto`, `CreateDefaultQuestionBankResponseDto`
- Exception handlers for HTTP error mapping

**Key Decisions**:
- Separate endpoint from existing question CRUD operations
- Route: `POST /api/users/default-question-bank` (not user-scoped to avoid path conflicts)
- Validates HTTP-level concerns only (JSON structure, content-type)

### 2. Global Shared Library (Mediator Pattern)
**Responsibility**: Command routing and handler orchestration

**Components**:
- `IMediator`: Central command dispatcher
- `ICommand<T>`: Command marker interface
- `ICommandHandler<C, T>`: Handler contract
- `Result<T>`: Functional result wrapper

**Key Decisions**:
- CQRS pattern separates commands from queries
- Type-safe command routing via generics
- Auto-registration of handlers via Spring ApplicationContext

### 3. Internal Layer - Question Bank Module

#### 3.1 Application Layer
**Responsibility**: Use case orchestration and business workflow

**Components**:
- `OnNewUserCreateDefaultQuestionBankCommand`: Command object with user ID
- `OnNewUserCreateDefaultQuestionBankCommandHandler`: Main use case orchestrator
- `DefaultQuestionBankApplicationService`: Domain + infrastructure coordination
- Port Out interfaces: Repository contracts

**Key Decisions**:
- Command handler is stateless, injected via Spring
- Service coordinates transaction boundaries
- Uses LongIdGenerator for question_bank_id generation

#### 3.2 Domain Layer
**Responsibility**: Business logic and invariant enforcement

**Components**:
- `QuestionBanksPerUserAggregate`: User's question bank collection
- `TaxonomySetAggregate`: Taxonomy structure and validation
- `QuestionBank` (entity): Individual bank within aggregate
- Domain events (future): `DefaultQuestionBankCreatedEvent`

**Key Decisions**:
- Aggregates enforce business rules (no duplicate banks, valid taxonomy)
- Immutable value objects (QuestionBank, Tag, DifficultyLevel)
- No database concerns in domain layer

#### 3.3 Infrastructure Layer
**Responsibility**: Persistence and external system integration

**Components**:
- `MongoDefaultQuestionBankRepository`: Repository implementation
- `QuestionBanksPerUserDocument`: MongoDB document model
- `TaxonomySetDocument`: MongoDB document model
- `MongoTransactionManager`: Transaction coordination

**Key Decisions**:
- MongoDB transactions for atomic multi-document inserts
- Document models separate from domain models (anti-corruption layer)
- Template-based default data from JSON files
- Testcontainers MongoDB only (no standalone MongoDB)

## Cross-Cutting Concerns

### ID Generation Strategy
```
┌─────────────────────────────────────────────────────────┐
│  LongIdGenerator (Global Shared Library)               │
│  - Thread-safe timestamp + sequence approach           │
│  - Format: [timestamp_ms * 1000] + [sequence]          │
│  - Example: 1730832000000000                           │
│  - Used for: question_bank_id                          │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│  MongoDB ObjectId (MongoDB Driver)                      │
│  - 12-byte unique identifier                           │
│  - Used for: MongoDB document _id fields               │
└─────────────────────────────────────────────────────────┘
```

### Transaction Management
```
MongoDB Transaction Scope:
┌─────────────────────────────────────────────────────┐
│  session.withTransaction(() -> {                    │
│    1. Check user doesn't already have banks         │
│    2. Generate question_bank_id                     │
│    3. Insert question_banks_per_user document       │
│    4. Insert taxonomy_sets document                 │
│    5. Return success                                │
│  })                                                 │
│                                                     │
│  On Failure: Rollback all changes                  │
│  On Success: Commit atomically                      │
└─────────────────────────────────────────────────────┘
```

### Template Variable Replacement
```java
// Template variables from JSON files:
{{NEW_USER_ID}}                 → Provided in command (e.g., 123456789)
{{GENERATED_DEFAULT_BANK_ID}}   → Generated via LongIdGenerator
{{GENERATED_OBJECT_ID}}         → Generated via new ObjectId()
{{CURRENT_TIMESTAMP}}           → Instant.now()
```

## Data Flow

### Happy Path Sequence
```
1. External System → POST /api/users/default-question-bank
   Body: { "userId": 123456789 }

2. Controller → Extract request → Create Command
   Command: OnNewUserCreateDefaultQuestionBankCommand(userId=123456789)

3. Mediator → Route to Handler
   Handler: OnNewUserCreateDefaultQuestionBankCommandHandler

4. Handler → Check duplicate user
   Repository: checkUserExists(123456789) → false

5. Handler → Generate IDs
   LongIdGenerator: generateQuestionBankId() → 1730832000000000
   ObjectId: new ObjectId() → "67029a8c..."

6. Handler → Load templates
   Files: question_banks_per_user.json_template
          taxonomy_sets.json_template

7. Handler → Replace template variables
   {{NEW_USER_ID}} → 123456789
   {{GENERATED_DEFAULT_BANK_ID}} → 1730832000000000
   {{GENERATED_OBJECT_ID}} → "67029a8c..."
   {{CURRENT_TIMESTAMP}} → "2025-10-06T10:30:00Z"

8. Handler → Start MongoDB transaction
   session.withTransaction(() -> {
     8a. Insert question_banks_per_user document
     8b. Insert taxonomy_sets document
   })

9. Handler → Return success
   Result<DefaultQuestionBankResponseDto>(
     success=true,
     data={ userId, questionBankId, ... }
   )

10. Controller → Map to HTTP response
    HTTP 201 Created
    Body: { "success": true, "data": {...} }
```

### Error Handling Flow
```
Duplicate User (409 Conflict):
  Repository.checkUserExists() → true
  → Return Result.failure("DUPLICATE_USER")
  → HTTP 409 Conflict

Validation Error (400 Bad Request):
  userId <= 0 or null
  → Return Result.failure("VALIDATION_ERROR")
  → HTTP 400 Bad Request

Transaction Failure (500 Internal Server Error):
  MongoDB transaction rollback
  → Return Result.failure("DATABASE_ERROR")
  → HTTP 500 Internal Server Error
```

## Technology Stack

### Core Technologies
- **Java 21**: Language runtime
- **Spring Boot 3.5.6**: Application framework
- **Spring Data MongoDB**: MongoDB integration
- **MongoDB**: Document database (Testcontainers only)
- **Maven**: Build tool

### Testing Technologies
- **JUnit 5**: Testing framework
- **Mockito**: Mocking framework
- **Testcontainers**: MongoDB integration testing
- **AssertJ**: Fluent assertions

### Design Patterns Applied
1. **CQRS**: Command Query Responsibility Segregation
2. **Mediator**: Decoupled command routing
3. **Hexagonal Architecture**: Ports and adapters
4. **Domain-Driven Design**: Aggregates, entities, value objects
5. **Repository Pattern**: Data access abstraction
6. **Template Method**: JSON template processing

## Key Architectural Constraints

### 1. MongoDB Transaction Requirement
- **Must** use MongoDB transactions for atomic document creation
- Both `question_banks_per_user` and `taxonomy_sets` must be created together
- Rollback on any failure

### 2. Testcontainers Only
- **No** standalone MongoDB instances
- All tests use Testcontainers MongoDB
- CICD pipeline compatible
- No localhost:27017 connections in tests

### 3. ID Generation
- **Must** use `LongIdGenerator` for question_bank_id
- Sequential, collision-resistant IDs
- Thread-safe generation

### 4. Template-Based Defaults
- Default data defined in JSON templates
- Variable replacement at runtime
- Consistent across all new users

### 5. Idempotency
- Check user existence before creation
- Return 409 Conflict if user already has question banks
- No duplicate question bank creation

## MongoDB Collections Schema

### Collection: question_banks_per_user
```javascript
{
  _id: ObjectId,                    // MongoDB document ID
  user_id: Long,                    // User identifier (unique index)
  default_question_bank_id: Long,   // Reference to default bank
  question_banks: [                 // Embedded array
    {
      bank_id: Long,                // Question bank ID
      name: String,                 // "Default Question Bank"
      description: String,
      is_active: Boolean,           // true
      created_at: ISODate,
      updated_at: ISODate
    }
  ],
  created_at: ISODate,
  updated_at: ISODate
}

// Unique index: { user_id: 1 }
```

### Collection: taxonomy_sets
```javascript
{
  _id: ObjectId,
  user_id: Long,
  question_bank_id: Long,
  categories: {
    level_1: { id: "general", name: "General", slug: "general", parent_id: null }
  },
  tags: [
    { id: "beginner", name: "Beginner", color: "#28a745" },
    { id: "practice", name: "Practice", color: "#007bff" },
    { id: "quick-test", name: "Quick Test", color: "#6f42c1" }
  ],
  quizzes: [],
  current_difficulty_level: { level: "easy", numeric_value: 1, description: "..." },
  available_difficulty_levels: [
    { level: "easy", numeric_value: 1, description: "..." },
    { level: "medium", numeric_value: 2, description: "..." },
    { level: "hard", numeric_value: 3, description: "..." }
  ],
  created_at: ISODate,
  updated_at: ISODate
}

// Unique compound index: { user_id: 1, question_bank_id: 1 }
```

## Deployment Considerations

### CICD Pipeline Integration
- Testcontainers MongoDB auto-starts during test phase
- No external MongoDB dependency
- Docker must be available in CI environment
- Tests clean up containers automatically

### Environment Configuration
```yaml
# application-test.yml (for tests only)
spring:
  data:
    mongodb:
      # Connection string provided by Testcontainers
      # No hardcoded localhost:27017
```

### Monitoring & Observability
- Logging via SLF4J
- Command execution metrics (future)
- Transaction success/failure tracking
- ID generation performance monitoring

## Future Enhancements

### Phase 2: Domain Events
```java
@DomainEvent
public class DefaultQuestionBankCreatedEvent {
  private Long userId;
  private Long questionBankId;
  private Instant createdAt;
}
```

### Phase 3: Async Processing
- Queue-based command processing
- Retry mechanism for transient failures
- Dead letter queue for failed commands

### Phase 4: Audit Logging
- Track all default question bank creation events
- User audit trail
- Compliance reporting
