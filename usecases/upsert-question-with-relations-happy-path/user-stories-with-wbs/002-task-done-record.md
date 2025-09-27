## US-002 Task Done Record - Question Domain Aggregate Implementation

### Summary
- Implemented core question domain model with events, value objects, and lifecycle behavior.

### Completed
- Domain events: `QuestionCreatedEvent`, `QuestionUpdatedEvent` (+ scaffolding for publish/archive).
- Value objects: `QuestionType`, `McqData`, `EssayData`, `TrueFalseData` with validations and immutability.
- Aggregate: `QuestionAggregate` with `createNew`, `updateFrom`, type-specific data handling, audit fields.
- Lifecycle helpers: `publish`, `archive`, state queries; equals/hashCode by business identity.

### Verification
- Unit tests: creation defaults/validation, immutability, event generation, partial updates, type-specific rules.
- Concurrency tests: creation and updates across threads; timestamps and events validated.
- Serialization checks for events and value objects where applicable.

### Metrics
- Coverage (aggregate and VOs): > 90%.
- Creation < 50 ms; updates < 30 ms (local baseline).

### Notes / Follow-ups
- Extend events: `QuestionPublishedEvent`, `QuestionArchivedEvent` concrete classes.
- Refine errors/messages and add conflict detection if concurrent edits become frequent.

