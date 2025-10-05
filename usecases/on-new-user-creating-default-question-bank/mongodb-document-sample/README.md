# Default Question Bank MongoDB Document Templates

## Overview

This directory contains JSON templates for MongoDB documents that must be created when a new user is onboarded to the quiz CMS system. These templates ensure consistent default question bank setup while satisfying all domain validation requirements.

## Required Documents

### 1. `question_banks_per_user.json`
**Collection**: `question_banks_per_user`
**Purpose**: Central registry document that establishes the user's question bank ownership and default bank reference.

**Key Features**:
- Single document per user (enforced by unique index on `user_id`)
- Embedded question bank array for atomic operations
- Default question bank reference for quick access
- Active status tracking for business rule compliance

**Template Variables**:
- `{{NEW_USER_ID}}` - User ID from user creation event
- `{{GENERATED_DEFAULT_BANK_ID}}` - Generated default question bank ID
- `{{GENERATED_OBJECT_ID}}` - MongoDB ObjectId for document
- `{{CURRENT_TIMESTAMP}}` - Current timestamp for audit fields

### 2. `taxonomy_sets.json`
**Collection**: `taxonomy_sets`
**Purpose**: Default taxonomy structure that enables question creation and classification.

**Key Features**:
- Minimal category hierarchy (level_1 only) to avoid validation gaps
- Basic starter tags for immediate usability
- Standard difficulty levels (easy, medium, hard)
- Empty quizzes array for future expansion

**Template Variables**:
- `{{NEW_USER_ID}}` - User ID from user creation event
- `{{GENERATED_DEFAULT_BANK_ID}}` - Matching default question bank ID
- `{{GENERATED_OBJECT_ID}}` - MongoDB ObjectId for document
- `{{CURRENT_TIMESTAMP}}` - Current timestamp for audit fields

## Documents NOT Created Initially

### `questions` Collection
**Rationale**: No default questions are created to avoid content pollution and respect user autonomy. Users should create meaningful content for their specific needs.

### `question_taxonomy_relationships` Collection
**Rationale**: No relationships are needed without questions. These will be created automatically as users add questions with taxonomy associations.

## Implementation Guidelines

### Template Variable Replacement
```javascript
// Example: Replace template variables with actual values
const questionBankDoc = template.replace('{{NEW_USER_ID}}', actualUserId)
                              .replace('{{GENERATED_DEFAULT_BANK_ID}}', generatedBankId)
                              .replace('{{GENERATED_OBJECT_ID}}', new ObjectId())
                              .replace('{{CURRENT_TIMESTAMP}}', new Date());
```

### ID Generation Strategy
- **User ID**: Provided from user creation event
- **Question Bank ID**: Generate sequential ID or UUID-based ID
- **Object ID**: Use MongoDB's ObjectId generation
- **Timestamps**: Use current UTC time

### Transaction Safety
Create both documents within a single MongoDB transaction to ensure atomicity:

```javascript
const session = await mongoose.startSession();
await session.withTransaction(async () => {
  await QuestionBanksPerUser.create([questionBankDoc], { session });
  await TaxonomySet.create([taxonomyDoc], { session });
});
```

## Domain Validation Compliance

### QuestionBanksPerUserAggregate Requirements
- ✅ User ID must be positive integer
- ✅ Default question bank ID must reference active bank in array
- ✅ At least one question bank must be active
- ✅ Bank IDs must be unique within question_banks array

### TaxonomySetAggregate Requirements
- ✅ Category hierarchy validation (no gaps between levels)
- ✅ Tag IDs unique within tags array
- ✅ Current difficulty level exists in available levels
- ✅ User and question bank isolation enforced

## Business Logic Integration

### Validation Chain Support
The created documents will satisfy validation requirements for:
- `QuestionBankOwnershipValidator` - Validates user owns the question bank
- `TaxonomyReferenceValidator` - Validates taxonomy IDs exist in user's set

### Available Taxonomy IDs (Post-Creation)
After document creation, these taxonomy IDs will be available for question assignment:
- **Categories**: `"general"` (level_1)
- **Tags**: `"beginner"`, `"practice"`, `"quick-test"`
- **Difficulty**: `"easy"`, `"medium"`, `"hard"`
- **Quizzes**: (none initially - populated as users create quizzes)

## Index Requirements

Ensure these indexes exist before document creation:

```javascript
// question_banks_per_user indexes
db.question_banks_per_user.createIndex({ user_id: 1 }, { unique: true });
db.question_banks_per_user.createIndex({ user_id: 1, "question_banks.bank_id": 1 });
db.question_banks_per_user.createIndex({ user_id: 1, default_question_bank_id: 1 });

// taxonomy_sets indexes
db.taxonomy_sets.createIndex({ user_id: 1, question_bank_id: 1 }, { unique: true });
db.taxonomy_sets.createIndex({ user_id: 1, question_bank_id: 1, "categories.level_1.id": 1 });
db.taxonomy_sets.createIndex({ user_id: 1, question_bank_id: 1, "tags.id": 1 });
```

## Testing the Templates

Validate template compliance with domain aggregates:

```javascript
// Validate question bank document
const questionBankAggregate = QuestionBanksPerUserAggregate.create(/* ... */);
const ownershipValid = questionBankAggregate.validateOwnership(userId, bankId);

// Validate taxonomy document
const taxonomyAggregate = TaxonomySetAggregate.create(/* ... */);
const hierarchyValid = taxonomyAggregate.validateCategoryHierarchy();
const referenceValid = taxonomyAggregate.validateTaxonomyReferences(["general", "beginner"]);
```

This template-based approach ensures consistent, compliant default question bank creation while maintaining the flexibility to customize the initial setup based on specific user requirements or organizational needs.