# Question Bank Data Modeling v3 (MongoDB)

## Overview and Design Philosophy

This schema design supports a multi-tenant question bank system where users can create, manage, and organize questions within their own isolated question banks. The design prioritizes **performance**, **data isolation**, and **operational simplicity** while maintaining flexibility for complex taxonomy relationships.

**This document covers both the MongoDB schema design and the corresponding domain layer implementation** that provides business logic validation and entity management.

### Core Design Principles

1. **Multi-tenant Isolation**: Every collection includes `user_id` and `question_bank_id` for complete data separation
2. **Performance-First Indexing**: Compound indexes designed to support the most common query patterns
3. **Minimal Joins**: Strategic embedding to reduce cross-collection queries
4. **Flexible Taxonomy**: Separate relationship modeling allows complex many-to-many taxonomy associations
5. **Operational Efficiency**: Schema supports efficient upsert operations for question management

## Collections Design

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

**Key Design Decisions**:
- **bank_id vs _id**: Using separate `bank_id` field instead of ObjectId for cleaner referencing across collections
- **Default Tracking**: `default_question_bank_id` provides O(1) default bank identification without array iteration
- **Embedded Metadata**: Bank-level metadata (name, description, timestamps) embedded for atomic updates

### 2. `taxonomy_sets`

**Purpose**: Complete taxonomy definitions for each question bank

**Why This Approach**:
- **JSON Payload Compatibility**: Structure directly matches incoming controller JSON, eliminating transformation overhead
- **Single Category Per Level**: Each level represents the current category selection for this question bank context, not all possible categories
- **Flexible Difficulty Handling**: `current_difficulty_level` stores the contextual difficulty while `available_difficulty_levels` maintains bank-wide options
- **Efficient Direct Mapping**: No preprocessing required to transform JSON structure before storage

```javascript
// Document Structure Example
{
  _id: ObjectId("..."),
  user_id: 12345,
  question_bank_id: 789,
  
  // Modified: Single category objects per level (matches JSON payload)
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
  
  // Modified: Store current difficulty level (matches JSON payload)
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

**Key Design Decisions**:
- **Single Category Objects**: Each level (level_1 through level_4) stores a single category object, matching the JSON payload structure where each level represents the current selection
- **Current vs Available Difficulty**: `current_difficulty_level` stores the specific difficulty for this question bank context, while `available_difficulty_levels` maintains all possible options
- **Direct JSON Mapping**: Structure directly matches incoming controller payload for seamless processing

### 3. `questions`

**Purpose**: Individual question storage with bank-scoped identity

**Why This Approach**:
- **Bank Scoping**: Every question explicitly belongs to a specific user's question bank for data isolation
- **Dual Identity System**: `source_question_id` maps to controller payload ID for idempotent upserts, while `_id` serves as internal MongoDB reference
- **Rich Question Data**: Complete question content, metadata, and type-specific data embedded for single-query retrieval

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

**Key Design Decisions**:
- **Embedded Type Data**: MCQ, true/false, and essay-specific data embedded to avoid additional queries
- **Flexible Attachments**: Array of attachment objects supports multiple file types and metadata
- **Audit Trail**: Creation source, versioning, and editor tracking for operational visibility
- **Status Management**: Draft/published/archived lifecycle with timestamp tracking

### 4. `question_taxonomy_relationships`

**Purpose**: Many-to-many mapping between questions and taxonomy elements

**Why This Approach**:
- **Granular Relationships**: One document per question-taxonomy pair enables precise relationship management
- **Efficient Querying**: Compound indexes support both "find questions by taxonomy" and "find taxonomies for question" patterns
- **Type Safety**: `taxonomy_type` field ensures relationships reference appropriate taxonomy categories
- **Scalable Design**: Separate collection prevents document size limits as questions acquire more taxonomy associations

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

**Key Design Decisions**:
- **Normalized Taxonomy Types**: Standardized taxonomy_type values ensure consistent relationship modeling
- **Bank Scoping**: user_id + question_bank_id included for tenant isolation and query performance
- **Referential Integrity**: taxonomy_id values must exist in corresponding taxonomy_sets document
- **Minimal Payload**: Only essential fields to minimize storage and maximize query performance

## Relationships and Data Flow

### Entity Relationships
```
User (1) ←→ (1) question_banks_per_user
    ↓
Question Banks (array embedded)
    ↓
(user_id, question_bank_id) → taxonomy_sets (1:1)
(user_id, question_bank_id) → questions (1:many)
    ↓
questions._id → question_taxonomy_relationships (1:many)
```

### Data Integrity Rules
1. **Default Bank Constraint**: `default_question_bank_id` must reference an existing `question_banks.bank_id`
2. **Taxonomy Reference Constraint**: All `taxonomy_id` values in relationships must exist in corresponding `taxonomy_sets`
3. **Question Bank Scoping**: All related data (questions, relationships, taxonomies) must share the same `(user_id, question_bank_id)` combination
4. **Unique Source IDs**: `source_question_id` must be unique within each `(user_id, question_bank_id)` scope

## Index Strategy and Rationale

### `question_banks_per_user` Indexes

```javascript
// Primary user lookup - unique constraint ensures one document per user
db.question_banks_per_user.createIndex({ user_id: 1 }, { unique: true, name: "ux_user" });

// Fast access to specific bank within user's array
db.question_banks_per_user.createIndex({ user_id: 1, "question_banks.bank_id": 1 }, { name: "ix_user_bank_in_array" });

// Direct default bank access without array scanning
db.question_banks_per_user.createIndex({ user_id: 1, default_question_bank_id: 1 }, { name: "ix_user_default_bank" });
```

**Rationale**: These indexes support the three primary access patterns: user document retrieval, specific bank access, and default bank identification.

### `taxonomy_sets` Indexes

```javascript
// One taxonomy set per bank - enforces data model constraint
db.taxonomy_sets.createIndex({ user_id: 1, question_bank_id: 1 }, { unique: true, name: "ux_user_bank" });

// Optional: specific taxonomy lookups - updated for single category structure
db.taxonomy_sets.createIndex({ user_id: 1, question_bank_id: 1, "categories.level_1.id": 1 }, { name: "ix_cat_l1" });
db.taxonomy_sets.createIndex({ user_id: 1, question_bank_id: 1, "categories.level_4.id": 1 }, { name: "ix_cat_l4" });
db.taxonomy_sets.createIndex({ user_id: 1, question_bank_id: 1, "tags.id": 1 }, { name: "ix_tags" });
db.taxonomy_sets.createIndex({ user_id: 1, question_bank_id: 1, "quizzes.quiz_id": 1 }, { name: "ix_quizzes" });
db.taxonomy_sets.createIndex({ user_id: 1, question_bank_id: 1, "current_difficulty_level.level": 1 }, { name: "ix_current_difficulty" });
```

**Rationale**: Primary index ensures one taxonomy set per bank. Additional indexes support direct lookup of single category objects and current difficulty level, optimized for the new structure.

### `questions` Indexes

```javascript
// Primary bank-scoped question listing
db.questions.createIndex({ user_id: 1, question_bank_id: 1 }, { name: "ix_user_bank" });

// Idempotent upserts using controller payload ID
db.questions.createIndex({ user_id: 1, question_bank_id: 1, source_question_id: 1 }, { unique: true, name: "ux_user_bank_source_id" });

// Common filtering and sorting patterns
db.questions.createIndex({ user_id: 1, question_bank_id: 1, status: 1 }, { name: "ix_status" });
db.questions.createIndex({ user_id: 1, question_bank_id: 1, created_at: -1 }, { name: "ix_created_desc" });
```

**Rationale**: Indexes support primary access patterns: bank-scoped listing, upsert operations, status filtering, and chronological ordering.

### `question_taxonomy_relationships` Indexes

```javascript
// Find all relationships for a specific question
db.question_taxonomy_relationships.createIndex({ user_id: 1, question_bank_id: 1, question_id: 1 }, { name: "ix_user_bank_question" });

// Prevent duplicate relationships - critical for data integrity
db.question_taxonomy_relationships.createIndex(
  { user_id: 1, question_bank_id: 1, question_id: 1, taxonomy_type: 1, taxonomy_id: 1 },
  { unique: true, name: "ux_user_bank_question_taxonomy" }
);

// Hot path: find questions by taxonomy (most common query pattern)
db.question_taxonomy_relationships.createIndex(
  { user_id: 1, question_bank_id: 1, taxonomy_type: 1, taxonomy_id: 1, question_id: 1 },
  { name: "ix_user_bank_taxonomy_to_questions" }
);
```

**Rationale**: These indexes support the two primary query patterns: finding taxonomies for a question, and finding questions for a taxonomy. The unique constraint prevents duplicate relationships.

## Operational Workflows

### JSON Payload Processing Flow

**Controller JSON → MongoDB Documents**:

1. **Extract User Context**: Controller must inject `user_id` (not present in JSON payload)

2. **Direct Taxonomy Mapping**: JSON taxonomy structure maps directly to `taxonomy_sets`:
   ```javascript
   // JSON payload taxonomy structure matches schema exactly
   const taxonomySet = {
     user_id: extractedUserId,
     question_bank_id: jsonPayload.question_bank_id,
     categories: jsonPayload.taxonomy.categories,  // Direct mapping
     tags: jsonPayload.taxonomy.tags,              // Direct mapping  
     quizzes: jsonPayload.taxonomy.quizzes,        // Direct mapping
     current_difficulty_level: jsonPayload.taxonomy.difficulty_level, // Direct mapping
     updated_at: new Date()
   };
   ```

3. **Question Document Creation**: Map JSON fields to question document:
   ```javascript
   const questionDoc = {
     user_id: extractedUserId,
     question_bank_id: jsonPayload.question_bank_id,
     source_question_id: jsonPayload.id,
     question_type: jsonPayload.question_type,
     title: jsonPayload.title,
     content: jsonPayload.content,
     // ... other fields map directly
     // Only store relevant type-specific data based on question_type
     mcq_data: jsonPayload.question_type === 'mcq' ? jsonPayload.mcq_data : null,
     true_false_data: jsonPayload.question_type === 'true_false' ? jsonPayload.true_false_data : null,
     essay_data: jsonPayload.question_type === 'essay' ? jsonPayload.essay_data : null
   };
   ```

4. **Relationship Documents Creation**: Create one document per taxonomy association:
   ```javascript
   const relationships = [];
   
   // Category relationships (only for non-null categories)
   Object.entries(jsonPayload.taxonomy.categories).forEach(([level, categoryObj]) => {
     if (categoryObj) {
       relationships.push({
         user_id: extractedUserId,
         question_bank_id: jsonPayload.question_bank_id,
         question_id: questionDoc._id,
         taxonomy_type: `category_${level}`,
         taxonomy_id: categoryObj.id
       });
     }
   });
   
   // Tag relationships
   jsonPayload.taxonomy.tags.forEach(tag => {
     relationships.push({
       user_id: extractedUserId,
       question_bank_id: jsonPayload.question_bank_id,
       question_id: questionDoc._id,
       taxonomy_type: "tag",
       taxonomy_id: tag.id
     });
   });
   
   // Quiz relationships
   jsonPayload.taxonomy.quizzes.forEach(quiz => {
     relationships.push({
       user_id: extractedUserId,
       question_bank_id: jsonPayload.question_bank_id,
       question_id: questionDoc._id,
       taxonomy_type: "quiz",
       taxonomy_id: quiz.quiz_id.toString()
     });
   });
   
   // Difficulty relationship
   relationships.push({
     user_id: extractedUserId,
     question_bank_id: jsonPayload.question_bank_id,
     question_id: questionDoc._id,
     taxonomy_type: "difficulty_level",
     taxonomy_id: jsonPayload.taxonomy.difficulty_level.level
   });
   ```

## JSON Payload Compatibility

### ✅ **Fully Compatible Elements**
- **Tags array structure**: Direct 1:1 mapping
- **Quizzes array structure**: Direct 1:1 mapping  
- **Question core fields**: title, content, points, status, etc. map directly
- **MCQ/True-False/Essay data**: Type-specific data structures align perfectly
- **Attachments array**: Structure matches schema design
- **Metadata and timestamps**: Compatible with schema field expectations

### 🔧 **Structural Adaptations Made**
- **Categories**: Modified schema from arrays to single objects per level to match JSON
- **Difficulty Level**: Schema now stores `current_difficulty_level` as single object (matching JSON) plus optional `available_difficulty_levels` array
- **User Context**: Application layer must inject `user_id` (not present in JSON payload)

### 📝 **Processing Requirements**
- **User ID Injection**: Controller/service layer must provide `user_id` context
- **Question Type Filtering**: Only store type-specific data relevant to `question_type`
- **Direct Field Mapping**: Most JSON fields map directly to MongoDB documents without transformation

## Scalability Considerations

### Sharding Strategy
For horizontal scaling, consider sharding on `{ user_id: "hashed", question_bank_id: "hashed" }` to:
- Keep user's data co-located on the same shard
- Distribute load evenly across users
- Maintain query efficiency for bank-scoped operations

### Performance Optimization
- **Connection Pooling**: Use appropriate connection pool sizing for concurrent question operations
- **Bulk Operations**: Utilize MongoDB bulk operations for relationship updates
- **Read Preferences**: Consider read preferences for reporting vs. operational queries

### Monitoring Points
- **Index Usage**: Monitor index hit rates, especially for taxonomy relationship queries
- **Document Size**: Track growth of question_banks_per_user documents as banks are added
- **Query Performance**: Monitor query execution times for complex taxonomy filtering operations

## Schema Validation

```javascript
// Example validation for critical constraints
db.createCollection("question_taxonomy_relationships", {
  validator: {
    $jsonSchema: {
      required: ["user_id", "question_bank_id", "question_id", "taxonomy_type", "taxonomy_id"],
      properties: {
        taxonomy_type: {
          enum: ["category_level_1", "category_level_2", "category_level_3", 
                 "category_level_4", "tag", "quiz", "difficulty_level"]
        },
        user_id: { bsonType: "int" },
        question_bank_id: { bsonType: "int" },
        question_id: { bsonType: "objectId" }
      }
    }
  }
});
```

## Domain Layer Implementation

### Supporting Aggregates

The MongoDB schema is complemented by domain layer aggregates that provide business logic validation and entity management:

#### 1. TaxonomySetAggregate
**Purpose**: Validates taxonomy references and category hierarchy rules

**Key Methods**:
- `validateTaxonomyReferences(List<String> taxonomyIds)` - Validates all provided taxonomy IDs exist in the user's taxonomy set
- `validateSingleTaxonomyReference(String taxonomyId)` - Validates a single taxonomy reference
- `validateCategoryHierarchy()` - Ensures category levels follow proper hierarchy (no gaps)
- `findInvalidTaxonomyReferences(List<String> taxonomyIds)` - Returns list of invalid taxonomy IDs
- `belongsToUser(Long userId)` and `belongsToQuestionBank(Long questionBankId)` - User and question bank isolation

**Business Rules**:
- All taxonomy references must exist in the user's taxonomy set
- Category hierarchy must be valid (Level 2 cannot exist without Level 1, etc.)
- User and question bank isolation is strictly enforced

#### 2. QuestionTaxonomyRelationshipAggregate
**Purpose**: Manages question-taxonomy relationships with bulk creation from commands

**Key Methods**:
- `createFromCommand(ObjectId questionId, UpsertQuestionCommand command)` - Creates all relationships from a complete taxonomy command
- `isValidRelationshipType()` - Validates relationship types (category_level_1, tag, quiz, difficulty_level, etc.)
- Ownership validation methods for user, question bank, and question isolation

**Business Rules**:
- Creates one relationship aggregate per taxonomy element
- Supports relationship types: category_level_1-4, tag, quiz, difficulty_level
- Maintains referential integrity with question and taxonomy IDs

#### 3. QuestionBanksPerUserAggregate
**Purpose**: Manages user's question banks with ownership validation and default bank identification

**Key Methods**:
- `validateOwnership(Long userId, Long questionBankId)` - Validates user owns an active question bank
- `isDefaultQuestionBank(Long questionBankId)` - Identifies the user's default question bank
- `findQuestionBank(Long questionBankId)` - Retrieves a specific question bank
- `getActiveQuestionBanks()` - Returns only active question banks

**Business Rules**:
- Only active question banks are considered valid for ownership
- Maintains embedded question bank array structure matching MongoDB schema
- Supports default question bank identification

### Value Objects

The domain layer includes immutable value objects that mirror the MongoDB document structure:

- **CategoryLevels** & **CategoryLevels.Category** - Category hierarchy with validation
- **Tag** - Tag information with ID, name, and color
- **Quiz** - Quiz reference with ID, name, and slug
- **DifficultyLevel** - Difficulty level with numeric value and description
- **QuestionBank** - Question bank metadata with active status tracking

### Repository Interfaces

Domain-focused repository interfaces provide clean contracts for data access:

- **TaxonomySetRepository** - Taxonomy validation operations
- **QuestionBanksPerUserRepository** - Question bank ownership operations
- **QuestionRepository** - Question CRUD operations (existing)
- **QuestionTaxonomyRelationshipRepository** - Relationship management (existing)

### Integration with CQRS Mediator Pattern

All aggregates integrate seamlessly with the existing CQRS mediator pattern:
- Commands use `UpsertQuestionCommand.extractTaxonomyIds()` for validation
- Aggregates work with existing validation chains
- Repository interfaces return `Result<T>` for consistent error handling

This schema design provides a robust foundation for a multi-tenant question bank system, balancing performance, flexibility, and operational simplicity while maintaining clear data boundaries and supporting complex taxonomy relationships. The domain layer implementation ensures business logic validation and maintains data consistency across all operations.