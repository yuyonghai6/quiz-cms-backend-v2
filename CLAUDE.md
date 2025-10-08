# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Architecture

This is a Maven multi-module Spring Boot project with a layered architecture:

- **Root module** (`maven-submodule-base`): Parent POM that manages dependencies and plugins
- **orchestration-layer**: Spring Boot application module with web and security dependencies
- **internal-layer**: Internal business logic module with sub-modules following CQRS:
  - `question-bank`: Question command operations (write/update)
  - `question-bank-query`: Question query operations (read)
  - `shared`: Shared DDD building blocks (AggregateRoot, DomainEvent, Result, ValidationHandler)
- **external-service-proxy**: External service integration module
- **global-shared-library**: Shared utilities and mediator pattern implementation
  - `mediator/`: CQRS mediator implementation (IMediator, ICommand, IQuery, handlers)
  - `utils/`: Shared utilities (LongIdGenerator for internal IDs, thread-safe with >500K IDs/sec)

The project uses Java 21 and Spring Boot 3.5.6 with dependency management centralized in the parent POM.

## CQRS Architecture

The project implements Command Query Responsibility Segregation (CQRS) to separate read and write operations:

### Command Side (question-bank module)
- Handles write operations: create, update, delete questions
- Uses domain aggregates (QuestionAggregate, QuestionBanksPerUserAggregate, etc.)
- Enforces business rules and validation through domain logic
- Publishes domain events for state changes
- MongoDB persistence with transactional support

### Query Side (question-bank-query module)
- Handles read operations with optimized read models
- Separate from command models for independent scaling
- Can use different data structures/indexes optimized for queries
- Query handlers implement `IQueryHandler<Q, T>` and are auto-registered by mediator
- Package structure mirrors command side but focuses on data retrieval

### Mediator Pattern Integration
The mediator routes commands and queries to appropriate handlers:
- Commands: `mediator.send(ICommand<T>)` → command handlers in question-bank
- Queries: `mediator.send(IQuery<T>)` → query handlers in question-bank-query
- Handlers auto-registered via Spring's ApplicationContext with reflection-based type resolution

## Common Commands

### Build Commands
```bash
# Build entire project (from root)
mvn clean compile

# Build specific module
mvn clean compile -pl orchestration-layer

# Package all modules
mvn clean package

# Install to local repository
mvn clean install
```

### Running the Application
```bash
# Run the main Spring Boot application
mvn spring-boot:run -pl orchestration-layer

# Alternative using Maven wrapper
./orchestration-layer/mvnw spring-boot:run

# Run with specific profile
mvn spring-boot:run -pl orchestration-layer -Dspring-boot.run.profiles=dev
```

### Testing
```bash
# Run all tests
mvn test

# Run tests for specific module
mvn test -pl orchestration-layer

# Run integration tests (uses Testcontainers)
mvn verify -pl orchestration-layer

# Generate and check coverage reports
mvn verify

# Run coverage check only (fails if below 70%)
mvn jacoco:check

# Run specific test class
mvn test -pl internal-layer/question-bank -Dtest=QuestionApplicationServiceIntegrationTest

# Run specific test method
mvn test -pl internal-layer/shared -Dtest=ResultTest#shouldCreateSuccessfulResultWithValue

# Run tests without JaCoCo coverage (faster for development)
mvn test -pl internal-layer/question-bank -Djacoco.skip=true
```

### Allure Reporting
```bash
# Run tests and generate Allure results
mvn clean test

# Generate Allure report and open in browser automatically
mvn allure:serve

# Generate static HTML report (for CI/CD or sharing)
mvn allure:report

# View static HTML report
open target/allure-report/index.html

# Clean old results before new test run (recommended)
rm -rf target/allure-results

# Using Makefile for cross-module Allure reporting
make test                    # Run tests across all modules
make allure-generate         # Generate consolidated Allure report
make allure-open            # Open the generated report
make allure                 # Run tests, generate and open report
```

## Development Notes

- The `orchestration-layer` is the main runnable Spring Boot application (runs on port 8765 by default)
- Other modules are libraries that can be dependencies of the orchestration layer
- Lombok is configured for annotation processing in the orchestration layer
- Testcontainers is set up for integration testing
- Spring Security is enabled in the orchestration layer
- Reference projects are available in `reference-projects/` for MongoDB Testcontainer examples

### MongoDB Setup
**Local Development**:
Configure MongoDB connection in `orchestration-layer/src/main/resources/application.properties`:
```properties
spring.data.mongodb.uri=mongodb://localhost:27017/quizfun
```

**Integration Tests**:
- Testcontainers automatically provisions MongoDB containers for tests
- No manual MongoDB setup required for testing
- question-bank module configures Surefire for stability:
  - Single fork execution (`forkCount=1`)
  - No parallel test execution (`parallel=none`)
  - Container reuse enabled (`testcontainers.reuse.enable=true`)
  - Increased metaspace (`-XX:MaxMetaspaceSize=512m`)

## Hexagonal Architecture (Ports and Adapters)

The `question-bank` command module follows hexagonal architecture with clear separation of concerns:

### Application Layer (`application/`)
- **commands/**: Command objects implementing `ICommand<T>` for CQRS
- **dto/**: Data transfer objects for cross-layer communication
- **ports/out/**: Repository interfaces (output ports) - domain contracts for persistence
- **services/**: Application service implementations coordinating domain logic
- **validation/**: Business validation logic (ownership, data integrity, taxonomy references)
- **security/**: Security context validation and audit logging

### Domain Layer (`domain/`)
Framework-agnostic core business logic:
- **aggregates/**: Aggregate roots extending `AggregateRoot` (QuestionAggregate, QuestionBanksPerUserAggregate, TaxonomySetAggregate)
- **entities/**: Domain entities and value objects (QuestionType, DifficultyLevel, McqData, EssayData, etc.)
- **events/**: Domain events for state changes (QuestionCreatedEvent, QuestionUpdatedEvent, etc.)
- **services/**: Domain services and strategy pattern implementations (QuestionTypeStrategyFactory)

### Infrastructure Layer (`infrastructure/`)
External concerns and framework-specific implementations:
- **persistence/documents/**: MongoDB document models (separate from domain models)
- **persistence/repositories/**: MongoDB repository implementations (adapters implementing domain ports)
- **persistence/mappers/**: Bidirectional mappers between domain aggregates and persistence documents
- **configuration/**: Spring configuration (transactions, async, validation chains)
- **templates/**: Template processing utilities (TemplateVariableReplacer)

This structure ensures domain logic remains independent of infrastructure concerns, enabling easier testing and framework changes.

## Cross-Module Development

### Component Scanning Configuration
The orchestration layer is configured with cross-module component scanning:
```java
@SpringBootApplication(scanBasePackages = {
    "com.quizfun.orchestrationlayer",
    "com.quizfun.internallayer",
    "com.quizfun.globalshared",
    "com.quizfun.questionbank"  // Explicitly scan question-bank command module
})
```
This enables Spring dependency injection across modules. When adding new modules that contain Spring components, update the `scanBasePackages` array.

**Package Structure Note**: The question-bank module uses `com.quizfun.questionbank` as its root package (not `com.quizfun.internallayer.questionbank`). This is why it requires explicit scanning configuration.

### Module Dependencies
- `internal-layer`: Parent module containing sub-modules for business logic
  - `question-bank`: Command-side operations with MongoDB persistence
  - `question-bank-query`: Query-side operations (read models)
  - `shared`: DDD building blocks shared across internal modules
- `external-service-proxy`: Currently minimal, intended for external service integrations
- `global-shared-library`: Shared utilities and mediator pattern implementation for CQRS
- `orchestration-layer`: Main application with web, security, and dependencies on internal modules and global-shared-library

### Adding New Services Across Modules
1. Create service in target module with `@Service` annotation
2. Add module dependency in orchestration-layer's pom.xml
3. Update `scanBasePackages` if new module package root is different
4. Use constructor injection with `@Autowired` in orchestration layer

### Compiler Configuration
The orchestration layer has `-parameters` enabled for proper `@RequestParam` binding without explicit `name` attributes.

## Mediator Pattern Architecture

The project implements a CQRS-style mediator pattern in the `global-shared-library` module that routes commands and queries to separate modules:

### Core Components
- **IMediator**: Central interface for sending commands (`send(ICommand<T> command)`) and queries (`send(IQuery<T> query)`)
- **ICommand<T>**: Marker interface for commands that return type T
- **ICommandHandler<C, T>**: Handles commands of type C and returns Result<T>
- **IQuery<T>**: Marker interface for queries that return type T
- **IQueryHandler<Q, T>**: Handles queries of type Q and returns Result<T>
- **Result<T>**: Wrapper for success/failure responses with error handling
- **MediatorImpl**: Spring service that auto-registers handlers via reflection

### Handler Registration and CQRS Routing
The mediator automatically discovers and registers all `@Service` beans implementing `ICommandHandler` and `IQueryHandler` using Spring's `ApplicationContext`. Handler registration uses reflection to extract generic type parameters, enabling type-safe routing:

- **Command Handlers** → Registered from `question-bank` module (write operations)
- **Query Handlers** → Registered from `question-bank-query` module (read operations)

This separation allows:
- Independent scaling of read vs write workloads
- Different optimization strategies per side
- Clear separation of concerns between state changes and data retrieval

### Usage Patterns
**Commands (for operations that modify state):**
1. Create command class implementing `ICommand<ReturnType>` in question-bank module
2. Create handler class implementing `ICommandHandler<YourCommand, ReturnType>` with `@Service`
3. Inject `IMediator` in orchestration-layer and call `mediator.send(command)`

**Queries (for read-only operations):**
1. Create query class implementing `IQuery<ReturnType>` in question-bank-query module
2. Create handler class implementing `IQueryHandler<YourQuery, ReturnType>` with `@Service`
3. Inject `IMediator` in orchestration-layer and call `mediator.send(query)`

## Documentation and Examples

### Cross-Module Integration Guide
The repository includes comprehensive documentation on creating services across modules:
- See `documentation/how to create a service in internal layer service submodule and being called from orchestration layer.md` for step-by-step cross-module service creation
- Reference implementation examples available in `reference-projects/try-mongo-testcontainer/`

### Coverage and Quality

The project enforces **70% line coverage** using JaCoCo across all modules. Coverage reports are generated during the `test` phase and validated during `verify`. All modules inherit JaCoCo configuration from the parent POM.

## Testing and Reporting

### Allure Integration
The project is fully configured with Allure 2.25.0 for comprehensive test reporting. All 37 test methods across 8 test classes have been annotated with the proper Allure hierarchy:

**Current Test Organization:**
- **Epic**: "Core Infrastructure" (applied to all test classes)
- **Feature**: "Main Path" (applied to all test classes)
- **Story**: "000.shared-module-infrastructure-setup" (applied to all test methods)
- **@DisplayName**: Human-readable test descriptions

**Report Structure:**
```
📊 Epic: Core Infrastructure
  └─ 📁 Feature: Main Path
      └─ 📖 Story: 000.shared-module-infrastructure-setup
          ├─ ✅ Should add domain event to uncommitted events
          ├─ ✅ Should successfully execute command with registered handler
          ├─ ✅ Should create successful result with value
          └─ ... (all 37 test methods)
```

**Viewing Reports:**
- **Behavior Hierarchy**: Navigate via Epic → Feature → Story → Individual Tests
- **Suite Hierarchy**: View by package/class structure
- **Overview Tab**: Statistics, pass rates, trends, timeline

See `documentation/step by step guide allure spring boot.md` for complete setup details.

### Domain-Driven Design Elements
The `internal-layer/shared` module provides foundational DDD building blocks:
- `AggregateRoot`: Base class for aggregate roots
- `DomainEvent` and `BaseDomainEvent`: Domain event infrastructure
- `Result<T>`: Functional result wrapper for error handling
- `ValidationHandler`: Centralized validation logic

### ID Generation
The `global-shared-library/utils` module provides ID generation utilities:
- **LongIdGenerator**: Thread-safe generator for internal entity IDs (question_bank_id, etc.)
  - Format: `[timestamp_ms * 1000] + [sequence]` for collision-resistant IDs
  - Performance: >500K IDs/sec with zero collisions under high concurrency
  - Thread-safe via synchronized methods
  - Inject via Spring's `@Component` for use in services and aggregates
  - Validation method `isValidGeneratedId(Long id)` for API input validation

## important-instruction-reminders
Do what has been asked; nothing more, nothing less.
NEVER create files unless they're absolutely necessary for achieving your goal.
ALWAYS prefer editing an existing file to creating a new one.
NEVER proactively create documentation files (*.md) or README files. Only create documentation files if explicitly requested by the User.