## Table of Contents

### [1. Domain-Driven Design (DDD) Patterns](#1-domain-driven-design-ddd-patterns)

#### [1.1 Aggregate Root Pattern](#11-aggregate-root-pattern)
QuestionAggregate serves as the transaction boundary and consistency guardian, ensuring all question modifications maintain domain invariants and generate appropriate audit events.

#### [1.2 Domain Events Pattern](#12-domain-events-pattern)
QuestionCreatedEvent and QuestionUpdatedEvent provide loose coupling between domain operations and cross-cutting concerns like audit logging and integration notifications.

#### [1.3 Repository Pattern](#13-repository-pattern)
Clean repository interfaces isolate the domain from MongoDB persistence details while providing specialized upsert operations that maintain referential integrity across collections.

#### [1.4 Value Objects Pattern](#14-value-objects-pattern)
Type-specific data containers (McqData, EssayData, TrueFalseData) encapsulate question format rules and validation logic, ensuring data integrity for each question type.

#### [1.5 Entity Pattern](#15-entity-pattern)
QuestionAggregate maintains identity across its lifecycle, enabling proper tracking of question modifications and ensuring consistency in multi-step operations.

#### [1.6 Bounded Context Pattern](#16-bounded-context-pattern)
Clear module boundaries separate question management concerns from other system domains, enabling independent evolution and reducing coupling.

### [2. CQRS and Messaging Patterns](#2-cqrs-and-messaging-patterns)

#### [2.1 Command Query Responsibility Segregation (CQRS)](#21-command-query-responsibility-segregation-cqrs)
UpsertQuestionCommand focuses exclusively on write operations, enabling optimized question creation and updates without query complexity interference.

#### [2.2 Mediator Pattern](#22-mediator-pattern)
IMediator decouples HTTP controllers from business logic, enabling clean request routing and reducing dependencies between presentation and domain layers.

#### [2.3 Command Pattern](#23-command-pattern)
UpsertQuestionCommand encapsulates all request data as an immutable object, enabling validation, queuing, and standardized processing across different entry points.

#### [2.4 Command Handler Pattern](#24-command-handler-pattern)
UpsertQuestionCommandHandler provides a single point of responsibility for command processing, enabling consistent business logic execution and error handling.

### [3. Behavioral Patterns](#3-behavioral-patterns)

#### [3.1 Strategy Pattern](#31-strategy-pattern)
Question type strategies (MCQ, Essay, True/False) enable type-specific validation and processing while maintaining a consistent interface for adding new question formats.

#### [3.2 Chain of Responsibility Pattern](#32-chain-of-responsibility-pattern)
Sequential validation handlers (ownership, taxonomy, data integrity) provide fail-fast error detection and modular validation logic that can be easily extended or reordered.

#### [3.3 Template Method Pattern](#33-template-method-pattern)
Base ValidationHandler defines the validation algorithm skeleton while allowing concrete validators to implement specific validation rules for their domain concerns.

#### [3.4 Factory Pattern](#34-factory-pattern)
QuestionTypeStrategyFactory automatically discovers and instantiates appropriate strategy implementations, enabling runtime strategy selection based on question type.

#### [3.5 State Pattern](#35-state-pattern)
Question status management (draft, published, archived) controls question behavior and available operations based on its current lifecycle state.

### [4. Structural Patterns](#4-structural-patterns)

#### [4.1 Adapter Pattern](#41-adapter-pattern)
Document mapping classes convert between MongoDB documents and domain aggregates, allowing persistence technology changes without affecting domain logic.

#### [4.2 Facade Pattern](#42-facade-pattern)
QuestionApplicationService provides a simplified interface that coordinates validation, strategy processing, and persistence operations into a single cohesive business operation.

#### [4.3 Data Mapper Pattern](#43-data-mapper-pattern)
Bidirectional mapping between aggregates and MongoDB documents maintains clean separation between domain objects and their persistent representation.

#### [4.4 Builder Pattern](#44-builder-pattern)
Response DTO builders and test data builders simplify construction of complex objects with multiple optional properties and maintain fluent, readable code.

### [5. Error Handling Patterns](#5-error-handling-patterns)

#### [5.1 Result Pattern](#51-result-pattern)
Result<T> wrapper eliminates exception-based error handling, providing explicit success/failure states with detailed error messages throughout all layers.

#### [5.2 Fail-Fast Pattern](#52-fail-fast-pattern)
Validation chain terminates at the first error, providing immediate feedback to users and preventing unnecessary processing of invalid requests.

### [6. Integration Patterns](#6-integration-patterns)

#### [6.1 RESTful API Pattern](#61-restful-api-pattern)
HTTP API follows REST conventions with proper status codes and resource design, enabling intuitive integration with frontend applications and external systems.

#### [6.2 Data Transfer Object (DTO) Pattern](#62-data-transfer-object-dto-pattern)
Request and response DTOs provide clean data contracts between HTTP layer and application layer while supporting JSON serialization and validation.

#### [6.3 Transaction Script Pattern](#63-transaction-script-pattern)
MongoDB transactions ensure ACID compliance across question and taxonomy relationship operations, maintaining data consistency even during complex multi-collection updates.

### [7. Infrastructure Patterns](#7-infrastructure-patterns)

#### [7.1 Dependency Injection Pattern](#71-dependency-injection-pattern)
Constructor-based injection throughout all layers enables loose coupling, testability, and easy substitution of implementations for different environments.

#### [7.2 Test Data Builder Pattern](#72-test-data-builder-pattern)
QuestionBankTestDataLoader provides reliable, isolated test data management with automatic cleanup, ensuring consistent test execution across different environments.

#### [7.3 TestContainers Pattern](#73-testcontainers-pattern)
MongoDB containers provide isolated integration testing environments that closely mirror production while enabling CI/CD automation without external dependencies.

### [8. Test-Driven Development Patterns](#8-test-driven-development-patterns)

#### [8.1 Red-Green-Refactor Pattern](#81-red-green-refactor-pattern)
Systematic TDD cycles across all user stories ensure comprehensive test coverage and drive clean design while maintaining working software at each increment.

#### [8.2 Test Isolation Pattern](#82-test-isolation-pattern)
Per-test cleanup and data isolation ensure reliable, repeatable test execution without side effects, enabling parallel test execution and deterministic results.