# Global Shared Library Reference

## Overview

The `global-shared-library` module provides shared utilities and infrastructure components that can be used across all modules in the quiz CMS system. This library implements core patterns like CQRS mediator, functional result handling, and common utilities that ensure consistency across the entire application.

**Module**: `global-shared-library`
**Package Root**: `com.quizfun.globalshared`
**Spring Integration**: Full Spring Boot integration with auto-configuration

### Package Structure
```
com.quizfun.globalshared
├── mediator/          # CQRS mediator pattern implementation
│   ├── IMediator.java
│   ├── ICommand.java
│   ├── ICommandHandler.java
│   ├── IQuery.java
│   ├── IQueryHandler.java
│   ├── MediatorImpl.java
│   └── Result.java
└── utils/             # Shared utility classes
    ├── UUIDv7Generator.java     # External/global identifiers (UUID v7)
    └── LongIdGenerator.java     # Internal/numeric identifiers (Long)
```

## Mediator Pattern Components

The global-shared-library implements a complete CQRS (Command Query Responsibility Segregation) mediator pattern that provides clean separation between request handling and business logic execution.

### IMediator Interface

**Purpose**: Central interface for routing commands and queries to their respective handlers.

```java
public interface IMediator {
    <T> Result<T> send(ICommand<T> command);
    <T> Result<T> send(IQuery<T> query);
}
```

**Usage**:
```java
@Service
public class SomeService {
    private final IMediator mediator;

    public SomeService(IMediator mediator) {
        this.mediator = mediator;
    }

    public Result<QuestionDto> createQuestion(CreateQuestionRequest request) {
        var command = new CreateQuestionCommand(request);
        return mediator.send(command);
    }
}
```

### Command Pattern Components

#### ICommand<T>
**Purpose**: Marker interface for commands that modify state and return type T.

```java
public interface ICommand<T> {
    // Marker interface - no methods required
}
```

**Implementation Example**:
```java
public class UpsertQuestionCommand implements ICommand<QuestionResponseDto> {
    private final Long userId;
    private final Long questionBankId;
    private final String sourceQuestionId;
    private final QuestionType questionType;
    private final TaxonomyDto taxonomy;

    // Constructor, getters, and validation methods
    public UpsertQuestionCommand(Long userId, Long questionBankId, /* ... */) {
        this.userId = userId;
        this.questionBankId = questionBankId;
        // ... other assignments
    }

    public List<String> extractTaxonomyIds() {
        // Business logic to extract taxonomy IDs for validation
        var ids = new ArrayList<String>();
        if (taxonomy != null && taxonomy.getCategories() != null) {
            // Extract category IDs, tag IDs, etc.
        }
        return ids;
    }
}
```

#### ICommandHandler<C, T>
**Purpose**: Handles commands of type C and returns Result<T>.

```java
public interface ICommandHandler<TCommand extends ICommand<TResult>, TResult> {
    Result<TResult> handle(TCommand command);
}
```

**Implementation Example**:
```java
@Service
public class UpsertQuestionCommandHandler implements ICommandHandler<UpsertQuestionCommand, QuestionResponseDto> {

    private final QuestionApplicationService questionApplicationService;

    public UpsertQuestionCommandHandler(QuestionApplicationService questionApplicationService) {
        this.questionApplicationService = questionApplicationService;
    }

    @Override
    public Result<QuestionResponseDto> handle(UpsertQuestionCommand command) {
        return questionApplicationService.upsertQuestion(command);
    }
}
```

### Query Pattern Components

#### IQuery<T>
**Purpose**: Marker interface for queries that return type T without modifying state.

```java
public interface IQuery<T> {
    // Marker interface - no methods required
}
```

#### IQueryHandler<Q, T>
**Purpose**: Handles queries of type Q and returns Result<T>.

```java
public interface IQueryHandler<TQuery extends IQuery<TResult>, TResult> {
    Result<TResult> handle(TQuery query);
}
```

**Implementation Example**:
```java
public class GetQuestionsByBankQuery implements IQuery<List<QuestionDto>> {
    private final Long userId;
    private final Long questionBankId;

    // Constructor and getters
}

@Service
public class GetQuestionsByBankQueryHandler implements IQueryHandler<GetQuestionsByBankQuery, List<QuestionDto>> {

    private final QuestionRepository questionRepository;

    @Override
    public Result<List<QuestionDto>> handle(GetQuestionsByBankQuery query) {
        try {
            var questions = questionRepository.findByUserAndBank(
                query.getUserId(), query.getQuestionBankId());
            var dtos = questions.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
            return Result.success(dtos);
        } catch (Exception e) {
            return Result.failure("Failed to retrieve questions: " + e.getMessage());
        }
    }
}
```

### MediatorImpl Implementation

**Purpose**: Spring service that auto-discovers and registers command/query handlers using reflection.

**Key Features**:
- **Auto-Registration**: Automatically finds all `@Service` beans implementing `ICommandHandler` or `IQueryHandler`
- **Type-Safe Routing**: Uses reflection to extract generic type parameters for routing
- **Error Handling**: Provides consistent error handling for missing handlers and execution failures
- **Spring Integration**: Full integration with Spring's `ApplicationContext`

**How It Works**:
1. At startup, scans `ApplicationContext` for handler beans
2. Extracts generic type information using reflection
3. Builds internal registry mapping command/query types to handlers
4. Routes incoming requests to appropriate handlers
5. Handles errors consistently with `Result<T>` pattern

### Result<T> Pattern

**Purpose**: Functional wrapper for success/failure responses with consistent error handling.

```java
public record Result<T>(
    boolean success,
    String message,
    T data
) {
    public static <T> Result<T> success(T data);
    public static <T> Result<T> success(String message, T data);
    public static <T> Result<T> failure(String message);
    public static <T> Result<T> failure(String message, T data);
}
```

**Usage Patterns**:

**Success Cases**:
```java
// Simple success with data
return Result.success(questionDto);

// Success with custom message
return Result.success("Question created successfully", questionDto);
```

**Failure Cases**:
```java
// Simple failure
return Result.failure("Question not found");

// Failure with partial data (e.g., validation errors)
return Result.failure("Validation failed", validationErrors);
```

**Checking Results**:
```java
var result = mediator.send(command);
if (result.success()) {
    var data = result.data();
    // Handle success case
} else {
    var errorMessage = result.message();
    // Handle failure case
}
```

## Utility Components

### Hybrid ID Generation Strategy

The global-shared-library provides TWO complementary ID generators for different use cases:

| Generator | Format | Use Case | Performance | When to Use |
|-----------|--------|----------|-------------|-------------|
| **LongIdGenerator** | Numeric Long | Internal identifiers (question_bank_id) | >500K IDs/sec | Database-optimized, high-performance, numeric operations |
| **UUIDv7Generator** | UUID String | External identifiers (source_question_id) | Time-ordered UUIDs | Global uniqueness, external APIs, security through non-enumeration |

**Design Principle**: Use **Long** for internal system IDs where performance matters. Use **UUID v7** for external references where global uniqueness and security matter.

### LongIdGenerator

**Purpose**: Thread-safe generator for collision-resistant Long IDs optimized for internal system identifiers with high performance and compact storage.

**Package**: `com.quizfun.globalshared.utils.LongIdGenerator`

**Thread Safety**: Uses `synchronized` method with `volatile` fields for guaranteed correctness under concurrent load

**Performance**: >500K IDs/second with zero collisions proven in 100-thread stress tests

**ID Format**: `[timestamp_milliseconds * 1000] + [sequence_within_millisecond]`
- Example: `1730832000000000` (first ID at timestamp 1730832000000)
- Example: `1730832000000001` (second ID same millisecond)
- Example: `1730832000001000` (first ID next millisecond)

#### Core Methods

**generateQuestionBankId()**
```java
public synchronized Long generateQuestionBankId()
```
- Returns: Unique Long ID with temporal ordering
- Format: `timestamp * 1000 + sequence (0-999)`
- Use Case: Primary method for question_bank_id generation
- Thread-Safe: Yes (synchronized)
- Throws: `IllegalStateException` if interrupted during overflow handling

**generateInternalId()**
```java
public Long generateInternalId()
```
- Returns: Unique Long ID (alias for generateQuestionBankId)
- Use Case: General internal ID generation for future entity types
- Thread-Safe: Yes (delegates to synchronized method)

**isValidGeneratedId(Long)**
```java
public boolean isValidGeneratedId(Long id)
```
- Parameters: Long ID to validate
- Returns: true if ID format is valid, false otherwise
- Validation Rules:
  - Must be non-null and positive
  - Timestamp portion (id / 1000) must be between 2020-2100
  - Sequence portion (id % 1000) must be 0-999
- Use Case: Input validation, API request validation

#### Usage Examples

**Question Bank ID Generation**:
```java
@Service
public class QuestionBankCreationService {
    private final LongIdGenerator longIdGenerator;

    public QuestionBankCreationService(LongIdGenerator longIdGenerator) {
        this.longIdGenerator = longIdGenerator;
    }

    public Result<QuestionBanksPerUserAggregate> createDefaultQuestionBank(Long userId) {
        // Generate unique question bank ID
        Long questionBankId = longIdGenerator.generateQuestionBankId();

        // Create question bank with generated ID
        var questionBank = QuestionBank.builder()
            .bankId(questionBankId)
            .name("Default Question Bank")
            .description("Your default question bank")
            .isActive(true)
            .build();

        return repository.save(questionBank);
    }
}
```

**New User Event Handler**:
```java
@EventListener
public class NewUserEventHandler {
    private final LongIdGenerator longIdGenerator;
    private final QuestionBankCreationService questionBankService;

    @Async
    public void handleNewUserCreated(UserCreatedEvent event) {
        Long userId = event.getUserId();

        // Generate unique question bank ID for default bank
        Long defaultQuestionBankId = longIdGenerator.generateQuestionBankId();

        // Create default question bank
        questionBankService.createDefaultQuestionBank(userId, defaultQuestionBankId);
    }
}
```

**API Validation**:
```java
@RestController
public class QuestionBankController {
    private final LongIdGenerator longIdGenerator;

    @GetMapping("/api/question-banks/{bankId}")
    public ResponseEntity<?> getQuestionBank(@PathVariable Long bankId) {
        // Validate ID format before querying database
        if (!longIdGenerator.isValidGeneratedId(bankId)) {
            return ResponseEntity.badRequest()
                .body("Invalid question bank ID format");
        }

        // Proceed with database query
        return questionBankService.findById(bankId);
    }
}
```

**Hybrid ID Strategy in Domain Aggregate**:
```java
public class QuestionAggregate extends AggregateRoot {
    private Long questionBankId;          // Internal: LongIdGenerator
    private String sourceQuestionId;      // External: UUIDv7Generator

    public static QuestionAggregate createNew(
            Long userId,
            Long questionBankId,           // Generated by LongIdGenerator
            QuestionType questionType) {

        var aggregate = new QuestionAggregate();
        aggregate.questionBankId = questionBankId;  // Long for performance
        aggregate.sourceQuestionId = UUIDv7Generator.generateAsString();  // UUID for external ref

        return aggregate;
    }
}
```

#### Performance Characteristics

- **Sequential Generation**: >800K IDs/second in single-threaded tests
- **Concurrent Generation**: >500K IDs/second with 100 threads
- **Memory Footprint**: <100 bytes per instance (2 volatile fields + object overhead)
- **Collision Resistance**: Zero collisions proven in stress tests (100 threads × 1,000 IDs = 100,000 unique IDs)
- **Sequence Capacity**: 1,000 IDs per millisecond (theoretical max: 1 million IDs/second)
- **Overflow Handling**: Automatic millisecond wait + retry when sequence exceeds 999

#### Thread Safety Guarantees

The `synchronized` keyword on `generateQuestionBankId()` ensures:
1. **Atomic Operations**: Timestamp read, sequence increment, and updates are atomic
2. **Memory Visibility**: `volatile` fields ensure visibility across threads
3. **No Race Conditions**: Only one thread can execute generation logic at a time
4. **Correctness Over Performance**: Sacrifices lock-free speed for guaranteed uniqueness

#### Design Decisions

**Why synchronized instead of AtomicInteger?**
- Initial lock-free implementation had subtle race conditions between timestamp update and sequence reset
- `synchronized` provides simpler, provably correct concurrency
- Performance is still excellent (>500K IDs/sec) and sufficient for production workloads

**Why Long instead of UUID?**
- **Database Performance**: Numeric indexes 4-5x faster than string UUID indexes
- **Storage Efficiency**: 8 bytes vs 36 characters (4.5x reduction)
- **Query Optimization**: Numeric comparisons optimized at database level
- **Existing Architecture**: All domain aggregates already use Long questionBankId parameters

### UUIDv7Generator

**Purpose**: Utility class for generating time-ordered UUID version 7 identifiers that are naturally sortable and contain timestamp information.

**Package**: `com.quizfun.globalshared.utils.UUIDv7Generator`

#### Core Methods

**generate()**
```java
public static UUID generate()
```
- Returns: New UUID v7 instance
- Use Case: When you need a UUID object for entity IDs, domain events
- Features: Time-ordered, naturally sortable, contains timestamp

**generateAsString()**
```java
public static String generateAsString()
```
- Returns: UUID v7 as string representation
- Use Case: When you need string identifiers for domain events, source question IDs
- Features: Ready for JSON serialization, database storage

**isValidUUIDv7(String)**
```java
public static boolean isValidUUIDv7(String uuidString)
```
- Parameters: UUID string to validate
- Returns: true if valid UUID v7 format, false otherwise
- Use Case: Input validation, data integrity checks

**isValidUUIDv7(UUID)**
```java
public static boolean isValidUUIDv7(UUID uuid)
```
- Parameters: UUID object to validate
- Returns: true if UUID v7, false otherwise
- Use Case: Runtime validation of UUID objects

**parseUUIDv7(String)**
```java
public static UUID parseUUIDv7(String uuidString)
```
- Parameters: UUID string to parse
- Returns: Parsed UUID v7 object
- Throws: `IllegalArgumentException` if invalid UUID v7 format
- Use Case: Safe parsing with validation

#### Usage Examples

**Domain Event Generation**:
```java
public class QuestionAggregate extends AggregateRoot {

    public static QuestionAggregate createNew(/* parameters */) {
        var aggregate = new QuestionAggregate();

        // Generate domain event with UUID v7
        String eventAggregateId = UUIDv7Generator.generateAsString();
        aggregate.addDomainEvent(new QuestionCreatedEvent(
            eventAggregateId,
            aggregate.getSourceQuestionId(),
            aggregate.getQuestionType(),
            aggregate.getUserId(),
            aggregate.getQuestionBankId()
        ));

        return aggregate;
    }
}
```

**Entity ID Generation**:
```java
public class QuestionAggregate extends AggregateRoot {

    public static QuestionAggregate createNew(/* parameters */) {
        var aggregate = new QuestionAggregate();
        aggregate.id = new ObjectId(); // MongoDB ObjectId
        aggregate.sourceQuestionId = UUIDv7Generator.generateAsString(); // UUID v7 for external reference
        return aggregate;
    }
}
```

**Input Validation**:
```java
private void validateRequiredFields(String sourceQuestionId, /* other params */) {
    if (sourceQuestionId == null || sourceQuestionId.trim().isEmpty()) {
        throw new InvalidSourceQuestionIdException("Source question ID is required");
    }
    if (!UUIDv7Generator.isValidUUIDv7(sourceQuestionId.trim())) {
        throw new InvalidSourceQuestionIdException("Source question ID must be a valid UUID v7 format");
    }
}
```

**Safe Parsing**:
```java
public void processQuestionId(String questionIdString) {
    try {
        UUID questionId = UUIDv7Generator.parseUUIDv7(questionIdString);
        // Process valid UUID v7
    } catch (IllegalArgumentException e) {
        // Handle invalid format
        return Result.failure("Invalid question ID format: " + e.getMessage());
    }
}
```

## Integration Guidelines

### Spring Integration

**Dependency Injection**:
```java
@Service
public class YourService {
    private final IMediator mediator;

    // Constructor injection (recommended)
    public YourService(IMediator mediator) {
        this.mediator = mediator;
    }
}
```

**Component Scanning**:
The orchestration layer is already configured to scan global-shared-library:
```java
@SpringBootApplication(scanBasePackages = {
    "com.quizfun.orchestrationlayer",
    "com.quizfun.internallayer",
    "com.quizfun.globalshared"  // Already included
})
```

### Module Dependencies

**Adding global-shared-library to your module**:
```xml
<dependency>
    <groupId>com.quizfun</groupId>
    <artifactId>global-shared-library</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### Import Statements

**Mediator Pattern**:
```java
import com.quizfun.globalshared.mediator.IMediator;
import com.quizfun.globalshared.mediator.ICommand;
import com.quizfun.globalshared.mediator.ICommandHandler;
import com.quizfun.globalshared.mediator.IQuery;
import com.quizfun.globalshared.mediator.IQueryHandler;
import com.quizfun.globalshared.mediator.Result;
```

**Utilities**:
```java
import com.quizfun.globalshared.utils.UUIDv7Generator;
import com.quizfun.globalshared.utils.LongIdGenerator;
```

### Best Practices

#### Command/Query Design
1. **Commands**: Use for operations that modify state (Create, Update, Delete)
2. **Queries**: Use for read-only operations (Get, List, Search)
3. **Single Responsibility**: One command/query per use case
4. **Immutable Data**: Use records or immutable objects for commands/queries

#### Result<T> Usage
1. **Consistent Error Handling**: Always return `Result<T>` from handlers
2. **Meaningful Messages**: Provide clear, actionable error messages
3. **Avoid Exceptions**: Use `Result.failure()` instead of throwing exceptions in business logic
4. **Success Data**: Always include relevant data in success results

#### UUIDv7 Usage
1. **Domain Events**: Always use UUID v7 for event IDs (temporal ordering)
2. **External References**: Use UUID v7 for source_question_id and similar external identifiers
3. **Validation**: Always validate UUID v7 format in domain aggregates
4. **Consistency**: Use the same UUID generation approach across the system

#### Handler Registration
1. **@Service Annotation**: All handlers must be annotated with `@Service`
2. **Single Handler**: One handler per command/query type
3. **Clear Naming**: Use descriptive names (e.g., `UpsertQuestionCommandHandler`)
4. **Error Handling**: Always return `Result<T>` with appropriate error messages

## Reference Quick Guide

### Component Summary

| Component | Purpose | Package | Usage |
|-----------|---------|---------|-------|
| `IMediator` | Central command/query router | `mediator` | Inject and call `send()` |
| `ICommand<T>` | Command marker interface | `mediator` | Implement for state-changing operations |
| `ICommandHandler<C,T>` | Command handler interface | `mediator` | Implement with `@Service` annotation |
| `IQuery<T>` | Query marker interface | `mediator` | Implement for read-only operations |
| `IQueryHandler<Q,T>` | Query handler interface | `mediator` | Implement with `@Service` annotation |
| `Result<T>` | Functional response wrapper | `mediator` | Return from all handlers |
| `LongIdGenerator` | Thread-safe Long ID generator | `utils` | Inject @Component for question_bank_id generation |
| `UUIDv7Generator` | Time-ordered UUID utility | `utils` | Static methods for UUID v7 generation |

### When to Use Each Component

| Use Case | Component | Example |
|----------|-----------|---------|
| Create/Update operations | `ICommand` + `ICommandHandler` | `UpsertQuestionCommand` |
| Read/Query operations | `IQuery` + `IQueryHandler` | `GetQuestionsByBankQuery` |
| Consistent error handling | `Result<T>` | All handler return types |
| Internal numeric IDs | `LongIdGenerator.generateQuestionBankId()` | question_bank_id generation |
| Validate Long IDs | `LongIdGenerator.isValidGeneratedId()` | API request validation |
| Domain event IDs | `UUIDv7Generator.generateAsString()` | Event aggregate IDs |
| External entity references | `UUIDv7Generator.generateAsString()` | source_question_id |
| Input validation | `UUIDv7Generator.isValidUUIDv7()` | Validate external IDs |
| Safe UUID parsing | `UUIDv7Generator.parseUUIDv7()` | Convert strings to UUID objects |

### Cross-Module Compatibility

**✅ Available in all modules**:
- All mediator pattern components
- UUIDv7Generator utility
- Result<T> pattern

**📋 Requirements**:
- Add `global-shared-library` dependency to module's `pom.xml`
- Ensure Spring component scanning includes `com.quizfun.globalshared`
- Use `@Service` annotation for all handlers

**🔗 Integration**:
- Works seamlessly with existing validation chains
- Compatible with Spring Boot auto-configuration
- Supports transaction management when used with `@Transactional`

This comprehensive reference ensures consistent usage of shared components across all use cases and modules in the quiz CMS system.