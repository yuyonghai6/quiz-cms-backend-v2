# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Architecture

This is a Maven multi-module Spring Boot project with a layered architecture:

- **Root module** (`maven-submodule-base`): Parent POM that manages dependencies and plugins
- **orchestration-layer**: Spring Boot application module with web and security dependencies
- **internal-layer**: Internal business logic module with sub-modules:
  - `question-bank`: Question management functionality
  - `quiz-session`: Quiz session management functionality
- **external-service-proxy**: External service integration module
- **global-shared-library**: Shared utilities and mediator pattern implementation

The project uses Java 21 and Spring Boot 3.5.6 with dependency management centralized in the parent POM.

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

# Alternative using Maven wrapper (if available)
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
```

## Development Notes

- The `orchestration-layer` is the main runnable Spring Boot application (runs on port 8080 by default)
- Other modules are libraries that can be dependencies of the orchestration layer
- Lombok is configured for annotation processing in the orchestration layer
- Testcontainers is set up for integration testing
- Spring Security is enabled in the orchestration layer
- Reference projects are available in `reference-projects/` for MongoDB Testcontainer examples

## Cross-Module Development

### Component Scanning Configuration
The orchestration layer is configured with cross-module component scanning:
```java
@SpringBootApplication(scanBasePackages = {
    "com.quizfun.orchestrationlayer",
    "com.quizfun.internallayer",
    "com.quizfun.globalshared"
})
```
This enables Spring dependency injection across modules. When adding new modules that contain Spring components, update the `scanBasePackages` array.

### Module Dependencies
- `internal-layer`: Contains Spring Boot starter, provides business logic services with question-bank and quiz-session sub-modules
- `external-service-proxy`: Currently minimal, intended for external service integrations
- `global-shared-library`: Shared utilities and mediator pattern implementation
- `orchestration-layer`: Main application with web, security, and dependencies on internal-layer and global-shared-library

### Adding New Services Across Modules
1. Create service in target module with `@Service` annotation
2. Add module dependency in orchestration-layer's pom.xml
3. Update `scanBasePackages` if new module package root is different
4. Use constructor injection with `@Autowired` in orchestration layer

### Compiler Configuration
The orchestration layer has `-parameters` enabled for proper `@RequestParam` binding without explicit `name` attributes.

## Mediator Pattern Architecture

The project implements a CQRS-style mediator pattern in the `global-shared-library` module:

### Core Components
- **IMediator**: Central interface for sending commands (`send(ICommand<T> command)`) and queries (`send(IQuery<T> query)`)
- **ICommand<T>**: Marker interface for commands that return type T
- **ICommandHandler<C, T>**: Handles commands of type C and returns Result<T>
- **IQuery<T>**: Marker interface for queries that return type T
- **IQueryHandler<Q, T>**: Handles queries of type Q and returns Result<T>
- **Result<T>**: Wrapper for success/failure responses with error handling
- **MediatorImpl**: Spring service that auto-registers handlers via reflection

### Handler Registration
The mediator automatically discovers and registers all `@Service` beans implementing `ICommandHandler` and `IQueryHandler` using Spring's `ApplicationContext`. Handler registration uses reflection to extract generic type parameters, enabling type-safe command and query routing.

### Usage Patterns
**Commands (for operations that modify state):**
1. Create command class implementing `ICommand<ReturnType>`
2. Create handler class implementing `ICommandHandler<YourCommand, ReturnType>` with `@Service`
3. Inject `IMediator` and call `mediator.send(command)`

**Queries (for read-only operations):**
1. Create query class implementing `IQuery<ReturnType>`
2. Create handler class implementing `IQueryHandler<YourQuery, ReturnType>` with `@Service`
3. Inject `IMediator` and call `mediator.send(query)`

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

## important-instruction-reminders
Do what has been asked; nothing more, nothing less.
NEVER create files unless they're absolutely necessary for achieving your goal.
ALWAYS prefer editing an existing file to creating a new one.
NEVER proactively create documentation files (*.md) or README files. Only create documentation files if explicitly requested by the User.