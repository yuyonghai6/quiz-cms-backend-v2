# Global Shared Library - Quick Reference for AI Agents

**Module**: `global-shared-library` | **Package**: `com.quizfun.globalshared`

## 🎯 Quick Decision Tree

```
Need to generate an ID?
│
├─ Internal system identifier (question_bank_id)?
│  └─ Use: LongIdGenerator.generateQuestionBankId()
│     Format: Long (numeric)
│     Example: 1730832000000000L
│
└─ External reference (source_question_id, domain events)?
   └─ Use: UUIDv7Generator.generateAsString()
      Format: String (UUID v7)
      Example: "f47ac10b-58cc-4372-a567-0e02b2c3d479"
```

## 📦 Component Cheat Sheet

| Component | Import | Usage | Returns |
|-----------|--------|-------|---------|
| **LongIdGenerator** | `com.quizfun.globalshared.utils.LongIdGenerator` | Inject via constructor | Long |
| **UUIDv7Generator** | `com.quizfun.globalshared.utils.UUIDv7Generator` | Static methods | UUID/String |
| **IMediator** | `com.quizfun.globalshared.mediator.IMediator` | Inject via constructor | Result<T> |
| **Result<T>** | `com.quizfun.globalshared.mediator.Result` | Return type | Success/Failure |

## 🔧 LongIdGenerator (Internal IDs)

### Methods

| Method | Signature | Use When |
|--------|-----------|----------|
| `generateQuestionBankId()` | `public synchronized Long` | Creating question banks |
| `generateInternalId()` | `public Long` | Future internal entities |
| `isValidGeneratedId(Long)` | `public boolean` | Validating API inputs |

### Quick Usage

```java
// 1. Inject (Spring @Component)
@Service
public class SomeService {
    private final LongIdGenerator longIdGenerator;

    public SomeService(LongIdGenerator longIdGenerator) {
        this.longIdGenerator = longIdGenerator;
    }
}

// 2. Generate
Long questionBankId = longIdGenerator.generateQuestionBankId();
// Result: 1730832000000000L

// 3. Validate
if (!longIdGenerator.isValidGeneratedId(bankId)) {
    return Result.failure("Invalid ID");
}
```

### ID Format
- **Structure**: `timestamp_ms * 1000 + sequence (0-999)`
- **Example 1**: `1730832000000000` (first ID at ms 1730832000000)
- **Example 2**: `1730832000000001` (second ID same ms)
- **Example 3**: `1730832000001000` (first ID next ms)

### Performance
- **Sequential**: >800K IDs/sec
- **Concurrent**: >500K IDs/sec (100 threads tested)
- **Thread-Safe**: Yes (synchronized method)
- **Collisions**: Zero (proven in stress tests)

## 🆔 UUIDv7Generator (External IDs)

### Methods

| Method | Signature | Use When |
|--------|-----------|----------|
| `generate()` | `public static UUID` | Need UUID object |
| `generateAsString()` | `public static String` | Need string (most common) |
| `isValidUUIDv7(String)` | `public static boolean` | Validate string input |
| `isValidUUIDv7(UUID)` | `public static boolean` | Validate UUID object |
| `parseUUIDv7(String)` | `public static UUID` | Safe parsing with validation |

### Quick Usage

```java
// 1. Generate (static - no injection needed)
String sourceQuestionId = UUIDv7Generator.generateAsString();
// Result: "f47ac10b-58cc-4372-a567-0e02b2c3d479"

// 2. Validate
if (!UUIDv7Generator.isValidUUIDv7(sourceId)) {
    throw new InvalidSourceQuestionIdException();
}

// 3. Parse safely
try {
    UUID uuid = UUIDv7Generator.parseUUIDv7(idString);
} catch (IllegalArgumentException e) {
    return Result.failure("Invalid UUID format");
}
```

## 🔀 Mediator Pattern (CQRS)

### Quick Setup

```java
// 1. Create Command
public class CreateQuestionCommand implements ICommand<QuestionDto> {
    private final Long userId;
    private final Long questionBankId;
    // ... fields, constructor, getters
}

// 2. Create Handler
@Service
public class CreateQuestionCommandHandler
    implements ICommandHandler<CreateQuestionCommand, QuestionDto> {

    @Override
    public Result<QuestionDto> handle(CreateQuestionCommand command) {
        // Business logic here
        return Result.success(questionDto);
    }
}

// 3. Use Mediator
@Service
public class SomeService {
    private final IMediator mediator;

    public Result<QuestionDto> createQuestion(/* params */) {
        var command = new CreateQuestionCommand(/* args */);
        return mediator.send(command);
    }
}
```

### Result<T> Pattern

| Method | Usage | Example |
|--------|-------|---------|
| `Result.success(data)` | Return success with data | `Result.success(questionDto)` |
| `Result.success(msg, data)` | Success with message | `Result.success("Created", dto)` |
| `Result.failure(msg)` | Return error | `Result.failure("Not found")` |
| `result.success()` | Check if successful | `if (result.success()) { ... }` |
| `result.data()` | Get data | `var data = result.data();` |
| `result.message()` | Get message | `var msg = result.message();` |

## 🎨 Hybrid ID Strategy Pattern

### When to Use Each Generator

| Scenario | Generator | Reason |
|----------|-----------|--------|
| question_bank_id | **LongIdGenerator** | Database performance, compact storage |
| source_question_id | **UUIDv7Generator** | External reference, global uniqueness |
| Domain event IDs | **UUIDv7Generator** | Temporal ordering, external systems |
| Internal entity IDs | **LongIdGenerator** | High-performance numeric operations |
| User-facing IDs | **UUIDv7Generator** | Security (non-enumerable) |
| API path parameters | **Long** (from LongIdGenerator) | RESTful, numeric, cacheable |

### Combined Usage Example

```java
public class QuestionAggregate extends AggregateRoot {
    private Long questionBankId;        // LongIdGenerator
    private String sourceQuestionId;    // UUIDv7Generator

    public static QuestionAggregate createNew(
            Long userId,
            LongIdGenerator longIdGenerator) {

        var aggregate = new QuestionAggregate();

        // Internal: Long for performance
        aggregate.questionBankId = longIdGenerator.generateQuestionBankId();

        // External: UUID for uniqueness
        aggregate.sourceQuestionId = UUIDv7Generator.generateAsString();

        // Domain event: UUID for temporal ordering
        String eventId = UUIDv7Generator.generateAsString();
        aggregate.addDomainEvent(new QuestionCreatedEvent(eventId, /*...*/));

        return aggregate;
    }
}
```

## 📋 Common Patterns

### Pattern 1: New User Default Question Bank

```java
@EventListener
public class NewUserEventHandler {
    private final LongIdGenerator longIdGenerator;

    @Async
    public void handleNewUserCreated(UserCreatedEvent event) {
        // Generate unique question bank ID
        Long questionBankId = longIdGenerator.generateQuestionBankId();

        // Create default question bank
        questionBankService.createDefault(event.getUserId(), questionBankId);
    }
}
```

### Pattern 2: API Validation

```java
@RestController
public class QuestionBankController {
    private final LongIdGenerator longIdGenerator;

    @GetMapping("/api/question-banks/{bankId}")
    public ResponseEntity<?> get(@PathVariable Long bankId) {
        // Validate before DB query
        if (!longIdGenerator.isValidGeneratedId(bankId)) {
            return ResponseEntity.badRequest()
                .body("Invalid question bank ID");
        }
        return ResponseEntity.ok(service.findById(bankId));
    }
}
```

### Pattern 3: Command Handler with Mediator

```java
@Service
public class UpsertQuestionCommandHandler
    implements ICommandHandler<UpsertQuestionCommand, QuestionResponseDto> {

    private final QuestionApplicationService questionApplicationService;

    @Override
    public Result<QuestionResponseDto> handle(UpsertQuestionCommand command) {
        return questionApplicationService.upsertQuestion(command);
    }
}
```

## 🔌 Spring Integration

### Dependencies (pom.xml)

```xml
<dependency>
    <groupId>com.quizfun</groupId>
    <artifactId>global-shared-library</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### Component Scanning (Already configured)

```java
@SpringBootApplication(scanBasePackages = {
    "com.quizfun.orchestrationlayer",
    "com.quizfun.internallayer",
    "com.quizfun.globalshared"  // ✅ Included
})
```

### Dependency Injection

```java
// Constructor Injection (Recommended)
@Service
public class SomeService {
    private final LongIdGenerator longIdGenerator;
    private final IMediator mediator;

    public SomeService(LongIdGenerator longIdGenerator, IMediator mediator) {
        this.longIdGenerator = longIdGenerator;
        this.mediator = mediator;
    }
}
```

## 📊 Comparison Table

| Feature | LongIdGenerator | UUIDv7Generator |
|---------|----------------|-----------------|
| **Format** | Numeric Long | String UUID |
| **Example** | 1730832000000000L | "f47ac10b-58cc..." |
| **Size** | 8 bytes | 36 characters |
| **Performance** | >500K IDs/sec | Time-ordered UUIDs |
| **Thread-Safe** | Yes (synchronized) | Yes (no shared state) |
| **Injection** | @Component (inject) | Static methods |
| **Use For** | question_bank_id | source_question_id |
| **Database** | Numeric index (fast) | String index |
| **Validation** | isValidGeneratedId() | isValidUUIDv7() |
| **Temporal Order** | Yes | Yes |
| **Global Unique** | No (timestamp-based) | Yes (UUID standard) |
| **Security** | Enumerable | Non-enumerable |

## ⚡ Performance Notes

### LongIdGenerator
- ✅ **Optimized for**: High-frequency ID generation
- ✅ **Thread-Safe**: Synchronized method (proven correct)
- ✅ **Tested**: 100 threads × 1,000 IDs = 100,000 unique (zero collisions)
- ⚠️ **Limit**: 1,000 IDs per millisecond (auto-retries on overflow)

### UUIDv7Generator
- ✅ **Optimized for**: Global uniqueness, external systems
- ✅ **Thread-Safe**: Stateless static methods
- ✅ **Standard**: UUID v7 specification compliant
- ✅ **Sortable**: Natural temporal ordering

## 🚫 Common Mistakes to Avoid

| ❌ Don't | ✅ Do |
|---------|-------|
| Use UUID for question_bank_id | Use LongIdGenerator for internal IDs |
| Use Long for source_question_id | Use UUIDv7Generator for external refs |
| Call static methods on LongIdGenerator | Inject LongIdGenerator as @Component |
| Inject UUIDv7Generator | Use static methods directly |
| Throw exceptions in handlers | Return Result.failure() |
| Create handlers without @Service | Always use @Service annotation |

## 📚 Import Quick Copy

```java
// ID Generators
import com.quizfun.globalshared.utils.LongIdGenerator;
import com.quizfun.globalshared.utils.UUIDv7Generator;

// Mediator Pattern
import com.quizfun.globalshared.mediator.IMediator;
import com.quizfun.globalshared.mediator.ICommand;
import com.quizfun.globalshared.mediator.ICommandHandler;
import com.quizfun.globalshared.mediator.IQuery;
import com.quizfun.globalshared.mediator.IQueryHandler;
import com.quizfun.globalshared.mediator.Result;
```

---

**📖 For detailed documentation**: See `global-shared-library-reference.md`

**🧪 Test Coverage**: >70% (enforced by JaCoCo)

**✅ Production Ready**: All components tested and validated
