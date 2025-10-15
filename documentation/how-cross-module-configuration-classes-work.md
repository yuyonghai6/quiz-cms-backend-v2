# How Cross-Module Configuration Classes Work

## Overview

This guide explains how Spring Boot discovers and uses `@Configuration` classes across Maven modules, specifically focusing on MongoDB index configuration classes in the question-bank and question-bank-query modules.

## The Configuration Classes

### Command Side Configuration
**Location**: `internal-layer/question-bank/src/main/java/com/quizfun/questionbank/infrastructure/configuration/MongoIndexConfig.java`

**Purpose**: Creates MongoDB indexes for the COMMAND side of CQRS architecture (write operations)

**Annotations**:
- `@Configuration`: Marks this as a Spring configuration class
- `@Profile("!test")`: Active in all profiles EXCEPT test (TestContainers handle indexes in tests)

**Key Method**: `@PostConstruct initializeCommandIndexes()` runs automatically after Spring creates the bean

### Query Side Configuration
**Location**: `internal-layer/question-bank-query/src/main/java/com/quizfun/questionbankquery/infrastructure/configuration/MongoQueryIndexConfig.java`

**Purpose**: Creates MongoDB indexes for the QUERY side of CQRS architecture (read operations)

**Annotations**:
- `@Configuration`: Marks this as a Spring configuration class
- `@Profile("!test")`: Active in all profiles EXCEPT test

**Key Method**: `@PostConstruct initializeQueryIndexes()` runs automatically after Spring creates the bean

## Discovery Mechanism

### Step 1: Maven Dependency Resolution (Build Time)

The orchestration layer declares dependencies on both modules in `orchestration-layer/pom.xml`:

```xml
<!-- Lines 40-44: Command side -->
<dependency>
    <groupId>com.quizfun</groupId>
    <artifactId>question-bank</artifactId>
    <version>${project.version}</version>
</dependency>

<!-- Lines 46-51: Query side -->
<dependency>
    <groupId>com.quizfun</groupId>
    <artifactId>question-bank-query</artifactId>
    <version>${project.version}</version>
</dependency>
```

**What happens**:
- Maven compiles both modules
- Compiled classes (`.class` files) are added to the orchestration layer's classpath
- All classes from both modules become available to the Spring Boot application

### Step 2: Spring Component Scanning (Runtime)

The main application class configures package scanning in `OrchestrationLayerApplication.java`:

```java
@SpringBootApplication(scanBasePackages = {
    "com.quizfun.orchestrationlayer",
    "com.quizfun.internallayer",
    "com.quizfun.globalshared",
    "com.quizfun.questionbank",        // ← Scans command module
    "com.quizfun.questionbankquery"    // ← Scans query module
})
public class OrchestrationLayerApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrchestrationLayerApplication.class, args);
    }
}
```

**What happens**:
- Spring scans all listed packages for `@Component`, `@Service`, `@Repository`, `@Configuration`, etc.
- Discovers `MongoIndexConfig` in `com.quizfun.questionbank.infrastructure.configuration`
- Discovers `MongoQueryIndexConfig` in `com.quizfun.questionbankquery.infrastructure.configuration`

**Critical Insight**: Spring doesn't care about Maven module boundaries—it only cares about Java package names. The `scanBasePackages` configuration makes cross-module discovery work.

### Step 3: Bean Creation and Initialization

During Spring application context initialization:

```
1. Spring ApplicationContext starts
2. Component scanning finds both @Configuration classes
3. @Profile("!test") is evaluated:
   - If running with test profile → classes are SKIPPED
   - If running with dev/prod/default profile → classes are ACTIVATED
4. Spring creates MongoIndexConfig bean (command side)
   - Constructor injection: MongoTemplate is injected
5. Spring creates MongoQueryIndexConfig bean (query side)
   - Constructor injection: MongoTemplate is injected
6. @PostConstruct methods are called automatically:
   - MongoIndexConfig.initializeCommandIndexes() executes
   - MongoQueryIndexConfig.initializeQueryIndexes() executes
7. MongoDB indexes are created
8. Application startup completes
```

## What Indexes Are Created

### Command Side Indexes (MongoIndexConfig)

**questions collection**:
1. `ux_user_bank_source_id` (UNIQUE compound index)
   - Fields: `user_id`, `question_bank_id`, `source_question_id`
   - Purpose: Enforces uniqueness for upsert operations
   - Prevents duplicate questions with same source ID

2. `ix_user_bank_status` (Performance index)
   - Fields: `user_id`, `question_bank_id`, `status`
   - Purpose: Optimizes queries like "show me all draft questions"

**question_taxonomy_relationships collection**:
1. `ux_user_bank_question_taxonomy` (UNIQUE compound index)
   - Fields: `user_id`, `question_bank_id`, `question_id`, `taxonomy_type`, `taxonomy_id`
   - Purpose: Prevents duplicate taxonomy relationships

2. `ix_user_bank_question` (Performance index)
   - Fields: `user_id`, `question_bank_id`, `question_id`
   - Purpose: Optimizes queries for all relationships of a specific question

### Query Side Indexes (MongoQueryIndexConfig)

**questions collection**:
1. `question_text_text` (TEXT index)
   - Field: `question_text`
   - Purpose: Enables full-text search with MongoDB `$text` queries
   - Supports relevance scoring for search results

## CQRS Index Strategy

The project demonstrates a key CQRS principle: **independent optimization of read and write sides**.

### Command Side Focus
- **Uniqueness constraints**: Prevent data corruption and ensure consistency
- **Write performance**: Indexes support upsert operations and relationship management
- **Data integrity**: Enforces business rules at database level

### Query Side Focus
- **Search performance**: Text indexes for full-text search capabilities
- **Read optimization**: Different index structures optimized for query patterns
- **Independent scaling**: Query-specific indexes don't impact write performance

**Why this matters**: In large-scale systems, command and query sides might use completely different databases. This architecture makes that transition easier by keeping concerns separated.

## Profile Configuration: Why @Profile("!test")?

Both configuration classes use `@Profile("!test")` to prevent index creation during integration tests.

**Reason**:
- Integration tests use **TestContainers** for MongoDB
- TestContainers create **ephemeral** containers that are destroyed after tests
- Tests create indexes programmatically in test setup code
- Automatic index creation would be wasteful and could cause conflicts

**When indexes ARE created**:
- Development profile (`mvn spring-boot:run`)
- Production profile
- Any custom profile that isn't named "test"

**When indexes are SKIPPED**:
- Running tests with `mvn test`
- Integration tests with `mvn verify`
- Any profile named "test"

## Verification

### Check Startup Logs

Run the application:
```bash
mvn spring-boot:run -pl orchestration-layer
```

Look for these log messages in the console:

**Command side**:
```
Initializing MongoDB indexes for question-bank command module
Creating indexes for 'questions' collection (command side)
Ensured unique index 'ux_user_bank_source_id' on questions collection
Ensured listing index 'ix_user_bank_status' on questions collection
Creating indexes for 'question_taxonomy_relationships' collection (command side)
Ensured unique index 'ux_user_bank_question_taxonomy' on question_taxonomy_relationships
Ensured query index 'ix_user_bank_question' on question_taxonomy_relationships
MongoDB command-side index initialization completed successfully
```

**Query side**:
```
Initializing MongoDB indexes for question-bank-query module
Creating full-text search indexes for 'questions' collection (query side)
Successfully ensured text index 'question_text_text' on questions collection
MongoDB query-side index initialization completed successfully
```

### Verify Indexes in MongoDB

Connect to your MongoDB instance and check:

```javascript
// In MongoDB shell or Compass
use quizfun

// Check questions collection indexes
db.questions.getIndexes()

// Should show:
// - _id_ (default)
// - ux_user_bank_source_id (command side, unique)
// - ix_user_bank_status (command side)
// - question_text_text (query side, text index)

// Check taxonomy relationships indexes
db.question_taxonomy_relationships.getIndexes()

// Should show:
// - _id_ (default)
// - ux_user_bank_question_taxonomy (command side, unique)
// - ix_user_bank_question (command side)
```

## Key Takeaways

1. **Maven dependencies** make compiled classes available on the classpath
2. **Component scanning** discovers `@Configuration` classes across module boundaries
3. **Package names matter**, not Maven module structure (Spring works with Java packages)
4. **@PostConstruct methods** run automatically after bean creation
5. **@Profile conditions** control when configurations are active
6. **CQRS architecture** allows independent optimization of read and write sides
7. **Index separation** enables different strategies for commands vs queries

## Related Documentation

- **Cross-module service creation**: `how to create a service in internal layer service submodule and being called from orchestration layer.md`
- **Mediator pattern**: `how to use mediator library.md`
- **CQRS queries**: `how to extend current mediator to develop query handling capability.md`
- **Query aggregation**: `how-query-aggregation-builder-works.md`

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│ orchestration-layer (Spring Boot Application)               │
│                                                               │
│  @SpringBootApplication(scanBasePackages = {                │
│    "com.quizfun.questionbank",        ◄──┐                  │
│    "com.quizfun.questionbankquery"    ◄──┼──┐               │
│  })                                       │  │               │
└───────────────────────────────────────────┼──┼───────────────┘
                                            │  │
        ┌───────────────────────────────────┘  │
        │                                      │
        ▼                                      ▼
┌──────────────────────────┐     ┌──────────────────────────┐
│ question-bank module     │     │ question-bank-query      │
│ (Command Side - Write)   │     │ (Query Side - Read)      │
│                          │     │                          │
│ MongoIndexConfig         │     │ MongoQueryIndexConfig    │
│  @Configuration          │     │  @Configuration          │
│  @Profile("!test")       │     │  @Profile("!test")       │
│  @PostConstruct          │     │  @PostConstruct          │
│                          │     │                          │
│ Creates:                 │     │ Creates:                 │
│ • ux_user_bank_source_id │     │ • question_text_text     │
│ • ix_user_bank_status    │     │   (full-text search)     │
│ • ux_user_bank_question  │     │                          │
│   _taxonomy              │     │                          │
│ • ix_user_bank_question  │     │                          │
└──────────────────────────┘     └──────────────────────────┘
           │                                  │
           └──────────┬───────────────────────┘
                      ▼
              ┌───────────────┐
              │   MongoDB     │
              │  (questions   │
              │  collection)  │
              └───────────────┘
```

## Troubleshooting

### Configuration not being discovered

**Problem**: Indexes are not created on startup

**Checklist**:
1. ✅ Verify Maven dependency is declared in `orchestration-layer/pom.xml`
2. ✅ Verify package is included in `scanBasePackages` in `OrchestrationLayerApplication.java`
3. ✅ Check you're not running with `test` profile (use `dev` or default)
4. ✅ Check startup logs for configuration class initialization messages
5. ✅ Verify MongoTemplate bean is available (Spring Data MongoDB is configured)

### Duplicate index creation errors

**Problem**: Getting errors about indexes already existing

**Solution**: This is normal! MongoDB's `createIndex()` is idempotent—it only creates the index if it doesn't exist. The configuration classes handle this gracefully.

### TestContainers conflicts

**Problem**: Index creation failing during tests

**Solution**: Ensure `@Profile("!test")` is present on configuration classes. TestContainers should create indexes programmatically in test setup methods, not via these configuration classes.
