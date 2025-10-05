# Question Bank Data Modeling v4 (MongoDB + Domain Layer)

## Overview and Design Philosophy

This schema design supports a multi-tenant question bank system where users can create, manage, and organize questions within their own isolated question banks. The design prioritizes **performance**, **data isolation**, and **operational simplicity** while maintaining flexibility for complex taxonomy relationships.

**This document comprehensively covers both the MongoDB schema design AND the sophisticated domain layer implementation** that provides business logic validation, domain events, and aggregate-based entity management following Domain-Driven Design (DDD) principles.

### Core Design Principles

1. **Multi-tenant Isolation**: Every collection includes `user_id` and `question_bank_id` for complete data separation
2. **Performance-First Indexing**: Compound indexes designed to support the most common query patterns
3. **Minimal Joins**: Strategic embedding to reduce cross-collection queries
4. **Flexible Taxonomy**: Separate relationship modeling allows complex many-to-many taxonomy associations
5. **Operational Efficiency**: Schema supports efficient upsert operations for question management
6. **Domain-Driven Design**: Rich domain layer with aggregates, domain events, and business rule enforcement
7. **CQRS Integration**: Clean separation between command processing and data persistence

## Part I: MongoDB Collections Design

### 1. `question_banks_per_user`

**Purpose**: Central registry of all question banks per user with default bank tracking

**Why This Approach**:
- **Single Document Per User**: Since users typically have few question banks (< 20), embedding them as an array avoids the overhead of a separate collection and eliminates joins
- **Default Bank at Root Level**: Storing `default_question_bank_id` at the document root enables fast default bank lookups without array scanning
- **Array Embedding Benefits**: Atomic updates, reduced storage overhead, and faster retrieval for user's complete question bank list

```javascript
// Document Structure Example
{
  _id: ObjectId("..."),
  user_id: 12345,
  default_question_bank_id: 789,
  question_banks: [
    {
      bank_id: 789,
      name: "JavaScript Fundamentals",
      description: "Core JavaScript concepts and methods",
      is_active: true,
      created_at: ISODate("..."),
      updated_at: ISODate("...")
    },
    {
      bank_id: 790,
      name: "Advanced React Patterns",
      description: "Complex React patterns and hooks",
      is_active: true,
      created_at: ISODate("..."),
      updated_at: ISODate("...")
    }
  ],
  created_at: ISODate("..."),
  updated_at: ISODate("...")
}
```

### 2. `taxonomy_sets`

**Purpose**: Complete taxonomy definitions for each question bank

```javascript
// Document Structure Example
{
  _id: ObjectId("..."),
  user_id: 12345,
  question_bank_id: 789,

  // Single category objects per level (matches JSON payload)
  categories: {
    level_1: { id: "tech", name: "Technology", slug: "technology", parent_id: null },
    level_2: { id: "prog", name: "Programming", slug: "programming", parent_id: "tech" },
    level_3: { id: "web_dev", name: "Web Development", slug: "web-development", parent_id: "prog" },
    level_4: { id: "javascript", name: "JavaScript", slug: "javascript", parent_id: "web_dev" }
  },

  tags: [
    { id: "js-arrays", name: "javascript", color: "#f7df1e" },
    { id: "array-methods", name: "arrays", color: "#61dafb" }
  ],

  quizzes: [
    { quiz_id: 101, quiz_name: "JavaScript Fundamentals Quiz", quiz_slug: "js-fundamentals" }
  ],

  // Store current difficulty level (matches JSON payload)
  current_difficulty_level: {
    level: "easy",
    numeric_value: 1,
    description: "Suitable for beginners"
  },

  // Optional: maintain available difficulty levels for the bank
  available_difficulty_levels: [
    { level: "easy", numeric_value: 1, description: "Suitable for beginners" },
    { level: "medium", numeric_value: 2, description: "Intermediate knowledge required" },
    { level: "hard", numeric_value: 3, description: "Advanced understanding needed" }
  ],

  created_at: ISODate("..."),
  updated_at: ISODate("...")
}
```

### 3. `questions`

**Purpose**: Individual question storage with bank-scoped identity

```javascript
// Document Structure Example
{
  _id: ObjectId("..."),
  user_id: 12345,
  question_bank_id: 789,
  source_question_id: "f47ac10b-58cc-4372-a567-0e02b2c3d479",

  question_type: "mcq",
  title: "JavaScript Array Methods",
  content: "<p>Which method adds elements to the <strong>end</strong> of an array?</p>",
  points: 5,
  solution_explanation: "<p>The <code>push()</code> method adds one or more elements...</p>",
  status: "draft",
  display_order: 1,

  // Type-specific data
  mcq_data: {
    options: [
      { id: 1, text: "push()", is_correct: true, explanation: "Correct! push() adds elements..." },
      { id: 2, text: "pop()", is_correct: false, explanation: "Incorrect. pop() removes..." }
    ],
    shuffle_options: false,
    allow_multiple_correct: false,
    time_limit_seconds: 60
  },

  attachments: [
    {
      id: "att_001",
      type: "image",
      filename: "array_methods_diagram.png",
      url: "/attachments/array_methods_diagram.png",
      size: 245760,
      mime_type: "image/png"
    }
  ],

  question_settings: {
    randomize_display: false,
    show_explanation_immediately: true,
    allow_review: true
  },

  metadata: {
    created_source: "manual",
    version: 1,
    author_id: 3,
    last_editor_id: 2
  },

  created_at: ISODate("..."),
  updated_at: ISODate("..."),
  published_at: null,
  archived_at: null
}
```

### 4. `question_taxonomy_relationships`

**Purpose**: Many-to-many mapping between questions and taxonomy elements

```javascript
// Document Structure Examples
{
  _id: ObjectId("..."),
  user_id: 12345,
  question_bank_id: 789,
  question_id: ObjectId("..."), // References questions._id
  taxonomy_type: "category_level_1",
  taxonomy_id: "tech",
  created_at: ISODate("...")
},
{
  _id: ObjectId("..."),
  user_id: 12345,
  question_bank_id: 789,
  question_id: ObjectId("..."),
  taxonomy_type: "tag",
  taxonomy_id: "js-arrays",
  created_at: ISODate("...")
},
{
  _id: ObjectId("..."),
  user_id: 12345,
  question_bank_id: 789,
  question_id: ObjectId("..."),
  taxonomy_type: "difficulty_level",
  taxonomy_id: "easy",
  created_at: ISODate("...")
}
```

## Part II: Domain Layer Architecture

### Domain-Driven Design Implementation

The domain layer implements sophisticated business logic using Domain-Driven Design principles, providing a rich object model that encapsulates business rules and generates domain events.

### AggregateRoot Base Class

All domain aggregates extend the shared `AggregateRoot` base class from `com.quizfun.shared.domain.AggregateRoot`:

```java
public abstract class AggregateRoot {
    private final List<DomainEvent> domainEvents = new ArrayList<>();
    private Long version;
    private Instant createdAt;
    private Instant updatedAt;

    protected void addDomainEvent(DomainEvent event) {
        domainEvents.add(event);
    }

    public List<DomainEvent> getUncommittedEvents() {
        return new ArrayList<>(domainEvents);
    }

    public void markEventsAsCommitted() {
        domainEvents.clear();
    }

    protected void markCreatedNow() {
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    protected void markUpdatedNow() {
        this.updatedAt = Instant.now();
    }
}
```

### Core Domain Aggregates

#### 1. QuestionAggregate (Primary Aggregate)

**Purpose**: Manages question lifecycle, business rules, and type-specific data validation

**Key Responsibilities**:
- Question content lifecycle management
- Business rule enforcement for question data
- Domain event generation (QuestionCreatedEvent, QuestionUpdatedEvent, QuestionPublishedEvent, QuestionArchivedEvent)
- Version control and audit trail
- Type-specific data validation (MCQ, Essay, True/False)

**Core Business Rules**:
- Source question ID must be valid UUID v7 format
- Title cannot exceed 255 characters
- Content cannot exceed 4,000 characters
- Points cannot be negative
- Type-specific data must match question type
- Only published questions can be archived
- Questions with invalid type-specific data cannot be published

**Factory Method Pattern**:
```java
public static QuestionAggregate createNew(Long userId, Long questionBankId,
                                        String sourceQuestionId, QuestionType questionType,
                                        String title, String content, Integer points) {
    // Validates all required fields first (fail fast)
    // Generates new ObjectId
    // Sets initial status to "draft"
    // Generates QuestionCreatedEvent with UUID v7
}
```

**Domain Events Generated**:
- `QuestionCreatedEvent`: When new question is created
- `QuestionUpdatedEvent`: When question content or metadata is modified
- `QuestionPublishedEvent`: When question status changes to published
- `QuestionArchivedEvent`: When question is archived

**Type-Specific Data Management**:
- Maintains exclusivity (only one type of data can be set)
- Validates data structure matches question type
- Generates update events only when data actually changes

#### 2. TaxonomySetAggregate (Supporting Aggregate)

**Purpose**: Validates taxonomy references and category hierarchy rules

**Key Responsibilities**:
- Taxonomy reference validation for all taxonomy types
- Category hierarchy validation (no gaps in levels)
- User and question bank isolation enforcement
- Single taxonomy set per question bank enforcement

**Core Business Rules**:
- All taxonomy references must exist in the user's taxonomy set
- Category hierarchy must be valid (Level 2 cannot exist without Level 1, etc.)
- User and question bank isolation is strictly enforced
- Each question bank has exactly one taxonomy set

**Key Validation Methods**:
```java
public boolean validateTaxonomyReferences(List<String> taxonomyIds)
public boolean validateSingleTaxonomyReference(String taxonomyId)
public List<String> findInvalidTaxonomyReferences(List<String> taxonomyIds)
public boolean validateCategoryHierarchy()
```

**Supported Taxonomy Types**:
- Category levels (level_1, level_2, level_3, level_4)
- Tags (id-based references)
- Quizzes (quiz_id converted to string)
- Difficulty levels (level string values)

#### 3. QuestionTaxonomyRelationshipAggregate (Supporting Aggregate)

**Purpose**: Manages question-taxonomy relationships with bulk creation from commands

**Key Responsibilities**:
- Many-to-many relationship management between questions and taxonomies
- Bulk relationship creation from UpsertQuestionCommand
- Relationship integrity enforcement
- User, question bank, and question isolation

**Core Business Rules**:
- One relationship aggregate per taxonomy element
- Supports relationship types: category_level_1-4, tag, quiz, difficulty_level
- Maintains referential integrity with question and taxonomy IDs
- Enforces user and question bank isolation

**Bulk Creation Pattern**:
```java
public static List<QuestionTaxonomyRelationshipAggregate> createFromCommand(
        ObjectId questionId, UpsertQuestionCommand command) {
    // Creates relationships for all taxonomy elements in command:
    // - Category levels (1-4)
    // - Tags
    // - Quizzes
    // - Difficulty level
}
```

#### 4. QuestionBanksPerUserAggregate (Supporting Aggregate)

**Purpose**: Manages user's question banks with ownership validation and default bank identification

**Key Responsibilities**:
- Question bank ownership validation
- User isolation enforcement
- Default question bank identification
- Active question bank filtering

**Core Business Rules**:
- Only active question banks are considered valid for ownership
- User can only access their own question banks
- Default question bank must be an active bank owned by the user
- Embedded question bank array structure matches MongoDB schema

**Key Validation Methods**:
```java
public boolean validateOwnership(Long userId, Long questionBankId)
public boolean isDefaultQuestionBank(Long questionBankId)
public Optional<QuestionBank> findQuestionBank(Long questionBankId)
public List<QuestionBank> getActiveQuestionBanks()
```

### Value Objects

The domain layer includes immutable value objects that mirror the MongoDB document structure:

#### CategoryLevels & CategoryLevels.Category
- **Purpose**: Category hierarchy with validation
- **Business Rules**: Validates parent-child relationships, prevents gaps in hierarchy
- **Immutability**: All category data is immutable once created

#### Tag
- **Purpose**: Tag information with ID, name, and color
- **Validation**: Ensures valid tag ID format and color codes
- **Immutability**: Tag objects cannot be modified after creation

#### Quiz
- **Purpose**: Quiz reference with ID, name, and slug
- **Validation**: Ensures valid quiz ID and slug format
- **Immutability**: Quiz references are immutable

#### DifficultyLevel
- **Purpose**: Difficulty level with numeric value and description
- **Validation**: Ensures numeric values are positive and descriptions are meaningful
- **Business Rules**: Difficulty levels must have both level string and numeric value

#### QuestionBank
- **Purpose**: Question bank metadata with active status tracking
- **Business Rules**: Only active question banks participate in operations
- **Immutability**: Question bank metadata is immutable within aggregates

### Domain Events Infrastructure

The domain layer implements a comprehensive event sourcing infrastructure:

#### Domain Event Types

**QuestionCreatedEvent**:
- Generated when new question is created
- Contains: eventAggregateId (UUID v7), sourceQuestionId, questionType, userId, questionBankId

**QuestionUpdatedEvent**:
- Generated when question content or metadata is modified
- Contains: eventAggregateId (UUID v7), sourceQuestionId, questionType, userId, questionBankId, updatedFields list

**QuestionPublishedEvent**:
- Generated when question status changes to published
- Contains: eventAggregateId (UUID v7), sourceQuestionId, userId, questionBankId

**QuestionArchivedEvent**:
- Generated when question is archived
- Contains: eventAggregateId (UUID v7), sourceQuestionId

#### Event Generation Patterns

**Factory Method Events**: Generated during aggregate creation
**Update Events**: Generated only when fields actually change
**Status Change Events**: Generated for significant lifecycle transitions
**UUID v7 Event IDs**: All events use UUID v7 for temporal ordering

### Domain Exceptions

The domain layer implements a comprehensive exception hierarchy:

#### Core Exception Types

**InvalidQuestionDataException**:
- Thrown for business rule violations in question data
- Examples: Invalid title length, negative points, invalid content

**InvalidSourceQuestionIdException**:
- Thrown for invalid source question ID format
- Must be valid UUID v7 format

**Question Type Validation Exceptions**:
- Thrown when type-specific data doesn't match question type
- Ensures data integrity across question types

## Part III: CQRS Integration

### Command Processing Flow

The domain aggregates integrate seamlessly with the CQRS mediator pattern:

#### UpsertQuestionCommand Integration

```java
// Command structure supports aggregate creation
public class UpsertQuestionCommand implements ICommand<QuestionResponseDto> {
    private Long userId;
    private Long questionBankId;
    private String sourceQuestionId;
    private QuestionType questionType;
    private TaxonomyDto taxonomy;
    // ... other fields

    public List<String> extractTaxonomyIds() {
        // Extracts all taxonomy IDs for validation
    }
}
```

#### Command Handler Flow

1. **Validation Chain**: Uses Chain of Responsibility pattern with domain aggregates
2. **Aggregate Creation**: Uses QuestionAggregate.createNew() factory method
3. **Type-Specific Processing**: Uses Strategy pattern with aggregate validation
4. **Relationship Creation**: Uses QuestionTaxonomyRelationshipAggregate.createFromCommand()
5. **Persistence**: Repository pattern with Result<T> error handling

#### Result<T> Pattern Integration

All domain operations return `Result<T>` for consistent error handling:

```java
public Result<QuestionResponseDto> upsertQuestion(UpsertQuestionCommand command) {
    // 1. Validate using domain aggregates
    var validationResult = validationChain.validate(command);
    if (validationResult.isFailure()) {
        return Result.failure(validationResult.getError());
    }

    // 2. Create domain aggregate
    var questionAggregate = QuestionAggregate.createNew(...);

    // 3. Process using domain logic
    var result = questionRepository.upsertBySourceQuestionId(questionAggregate);

    return Result.success(mapToDto(result));
}
```

### Repository Interface Contracts

Domain-focused repository interfaces provide clean contracts for data access:

#### Port OUT Interfaces (Domain Layer)

```java
public interface QuestionRepository {
    Result<QuestionAggregate> upsertBySourceQuestionId(QuestionAggregate aggregate);
    Optional<QuestionAggregate> findBySourceQuestionId(Long userId, Long questionBankId, String sourceQuestionId);
}

public interface QuestionTaxonomyRelationshipRepository {
    void replaceRelationshipsForQuestion(ObjectId questionId, List<QuestionTaxonomyRelationshipAggregate> relationships);
    List<QuestionTaxonomyRelationshipAggregate> findByQuestionId(ObjectId questionId);
}

public interface TaxonomySetRepository {
    boolean validateTaxonomyReferences(Long userId, Long questionBankId, List<String> taxonomyIds);
    Optional<TaxonomySetAggregate> findByUserAndQuestionBank(Long userId, Long questionBankId);
}

public interface QuestionBanksPerUserRepository {
    boolean validateOwnership(Long userId, Long questionBankId);
    Optional<QuestionBanksPerUserAggregate> findByUserId(Long userId);
}
```

#### Repository Implementation Patterns

- **Aggregate-Focused Operations**: Repository methods designed around aggregate boundaries
- **Result<T> Returns**: Consistent error handling patterns
- **Domain Object Mapping**: Infrastructure layer maps between aggregates and documents
- **Transaction Support**: Repository operations support MongoDB transactions

## Part IV: Business Logic Validation

### Comprehensive Validation Rules

#### Question Aggregate Validation

**Required Field Validation**:
- User ID: Must be positive integer
- Question Bank ID: Must be positive integer
- Source Question ID: Must be valid UUID v7 format
- Question Type: Must be valid enum value
- Title: Required, max 255 characters
- Content: Required, max 4,000 characters
- Points: Cannot be negative

**Type-Specific Data Validation**:

**MCQ Questions**:
- Must have at least one option
- Must have at least one correct answer
- Option IDs must be unique within question
- Option text cannot be empty

**Essay Questions**:
- Min word count cannot be greater than max word count
- Word counts must be non-negative
- Rubric validation if provided

**True/False Questions**:
- Must have exactly one correct answer
- Simple boolean validation

#### Taxonomy Validation

**Reference Validation**:
- All taxonomy IDs must exist in user's taxonomy set
- Category hierarchy must be valid (no gaps)
- Tag references must point to existing tags
- Quiz references must point to existing quizzes
- Difficulty level must be valid

**Category Hierarchy Rules**:
- Level 1 can exist independently
- Level 2 requires Level 1 parent
- Level 3 requires Level 2 parent
- Level 4 requires Level 3 parent
- Parent IDs must match actual parent category IDs

#### Ownership and Isolation Validation

**User Isolation**:
- Users can only access their own question banks
- All operations must include user context validation
- Cross-user data access is prevented at domain level

**Question Bank Ownership**:
- User must own the target question bank
- Question bank must be active
- Default question bank logic is enforced

### Validation Chain Integration

The domain aggregates integrate with the Chain of Responsibility validation pattern:

```java
// Validation chain uses domain aggregates for validation
@Component
public class QuestionBankOwnershipValidator extends ValidationHandler {
    private final QuestionBanksPerUserRepository repository;

    @Override
    public Result<Void> validate(UpsertQuestionCommand command) {
        var aggregate = repository.findByUserId(command.getUserId());
        if (!aggregate.validateOwnership(command.getUserId(), command.getQuestionBankId())) {
            return Result.failure("User doesn't own the question bank");
        }
        return checkNext(command);
    }
}

@Component
public class TaxonomyReferenceValidator extends ValidationHandler {
    private final TaxonomySetRepository repository;

    @Override
    public Result<Void> validate(UpsertQuestionCommand command) {
        var taxonomyIds = command.extractTaxonomyIds();
        var aggregate = repository.findByUserAndQuestionBank(
            command.getUserId(), command.getQuestionBankId());

        if (!aggregate.validateTaxonomyReferences(taxonomyIds)) {
            return Result.failure("Invalid taxonomy references");
        }
        return checkNext(command);
    }
}
```

## Part V: Index Strategy and Performance

### MongoDB Index Strategy

```javascript
// question_banks_per_user Indexes
db.question_banks_per_user.createIndex({ user_id: 1 }, { unique: true, name: "ux_user" });
db.question_banks_per_user.createIndex({ user_id: 1, "question_banks.bank_id": 1 }, { name: "ix_user_bank_in_array" });
db.question_banks_per_user.createIndex({ user_id: 1, default_question_bank_id: 1 }, { name: "ix_user_default_bank" });

// taxonomy_sets Indexes
db.taxonomy_sets.createIndex({ user_id: 1, question_bank_id: 1 }, { unique: true, name: "ux_user_bank" });
db.taxonomy_sets.createIndex({ user_id: 1, question_bank_id: 1, "categories.level_1.id": 1 }, { name: "ix_cat_l1" });
db.taxonomy_sets.createIndex({ user_id: 1, question_bank_id: 1, "tags.id": 1 }, { name: "ix_tags" });

// questions Indexes
db.questions.createIndex({ user_id: 1, question_bank_id: 1 }, { name: "ix_user_bank" });
db.questions.createIndex({ user_id: 1, question_bank_id: 1, source_question_id: 1 }, { unique: true, name: "ux_user_bank_source_id" });
db.questions.createIndex({ user_id: 1, question_bank_id: 1, status: 1 }, { name: "ix_status" });

// question_taxonomy_relationships Indexes
db.question_taxonomy_relationships.createIndex({ user_id: 1, question_bank_id: 1, question_id: 1 }, { name: "ix_user_bank_question" });
db.question_taxonomy_relationships.createIndex(
  { user_id: 1, question_bank_id: 1, question_id: 1, taxonomy_type: 1, taxonomy_id: 1 },
  { unique: true, name: "ux_user_bank_question_taxonomy" }
);
```

### Performance Considerations

**Domain Layer Performance**:
- Factory methods validate only required fields for fast creation
- Domain events are generated only when changes occur
- Value objects are immutable to prevent defensive copying
- Aggregate boundaries minimize cross-aggregate queries

**Repository Performance**:
- Aggregate-focused queries reduce unnecessary data loading
- Result<T> pattern avoids exception overhead for expected failures
- Bulk operations for relationship management

## Part VI: Integration Patterns

### Application Service Integration

```java
@Service
@Transactional
public class QuestionApplicationService {
    private final ValidationHandler validationChain;
    private final QuestionTypeStrategyFactory strategyFactory;
    private final QuestionRepository questionRepository;
    private final QuestionTaxonomyRelationshipRepository relationshipRepository;

    public Result<QuestionResponseDto> upsertQuestion(UpsertQuestionCommand command) {
        // 1. Validate using domain aggregates in validation chain
        var validationResult = validationChain.validate(command);
        if (validationResult.isFailure()) {
            return Result.failure(validationResult.getError());
        }

        // 2. Create question aggregate using factory method
        var strategy = strategyFactory.getStrategy(command.getQuestionType());
        var questionAggregateResult = strategy.processQuestionData(command);
        if (questionAggregateResult.isFailure()) {
            return Result.failure(questionAggregateResult.getError());
        }

        // 3. Persist aggregate and generate domain events
        var questionAggregate = questionAggregateResult.getValue();
        var questionResult = questionRepository.upsertBySourceQuestionId(questionAggregate);

        // 4. Create relationship aggregates and persist
        var relationships = QuestionTaxonomyRelationshipAggregate.createFromCommand(
            questionResult.getId(), command);
        relationshipRepository.replaceRelationshipsForQuestion(
            questionResult.getId(), relationships);

        // 5. Return success with domain events committed
        return Result.success(mapToResponseDto(questionResult));
    }
}
```

### Future Event Sourcing Enhancement

The domain layer is designed to support full event sourcing:

```java
// Future event store integration
public interface EventStore {
    void saveEvents(String aggregateId, List<DomainEvent> events, long expectedVersion);
    List<DomainEvent> getEventsForAggregate(String aggregateId);
}

// Aggregate reconstruction from events
public static QuestionAggregate fromHistory(List<DomainEvent> events) {
    // Replay events to reconstruct aggregate state
}
```

## Conclusion

This v4 data modeling documentation provides a comprehensive foundation for the multi-tenant question bank system, covering both the MongoDB schema design and the sophisticated domain layer implementation. The design balances performance, flexibility, and operational simplicity while maintaining clear data boundaries and supporting complex taxonomy relationships through Domain-Driven Design principles, CQRS patterns, and rich business logic validation.

The domain layer provides a robust foundation for business rule enforcement, domain events, and future event sourcing capabilities, while the MongoDB schema ensures optimal performance and data isolation for multi-tenant operations.