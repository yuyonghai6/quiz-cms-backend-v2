# 🚀 Mediator Library User Guide

This comprehensive guide explains how to use the mediator library across different Maven submodules for both human developers and AI agents. The mediator implements a CQRS (Command Query Responsibility Segregation) pattern with automatic handler registration.

## 📋 Table of Contents

1. [Overview & Architecture](#overview--architecture)
2. [Quick Start](#quick-start)
3. [Commands: Changing State](#commands-changing-state)
4. [Queries: Reading Data](#queries-reading-data)
5. [Cross-Module Usage](#cross-module-usage)
6. [Best Practices](#best-practices)
7. [Testing](#testing)
8. [Troubleshooting](#troubleshooting)
9. [Advanced Patterns](#advanced-patterns)

---

## 📐 Overview & Architecture

### What is the Mediator Library?

The mediator library is a Spring Boot-based CQRS implementation that provides:
- **Automatic handler registration** using Spring's dependency injection
- **Type-safe command/query routing** via reflection
- **Consistent error handling** with Result pattern
- **Cross-module communication** in Maven multi-module projects

### Core Components

```java
// Located in global-shared-library module
IMediator              // Central routing interface
ICommand<T>            // Marker for state-changing operations
IQuery<T>              // Marker for data-reading operations
ICommandHandler<C,R>   // Processes commands
IQueryHandler<Q,R>     // Processes queries
Result<T>              // Standardized response wrapper
```

### Architecture Flow

```
HTTP Request → Controller → Mediator → Handler Registry → Specific Handler → Business Logic → Result
```

---

## 🏃‍♀️ Quick Start

### Step 1: Add Dependencies

**In your target module's `pom.xml`:**
```xml
<dependency>
    <groupId>com.quizfun</groupId>
    <artifactId>global-shared-library</artifactId>
    <version>${project.version}</version>
</dependency>
```

### Step 2: Configure Component Scanning

**In your main application class:**
```java
@SpringBootApplication(scanBasePackages = {
    "com.quizfun.orchestrationlayer",    // Your module
    "com.quizfun.globalshared"           // Mediator library
})
public class YourApplication {
    public static void main(String[] args) {
        SpringApplication.run(YourApplication.class, args);
    }
}
```

### Step 3: Inject Mediator

**In your controllers or services:**
```java
@RestController
public class YourController {
    private final IMediator mediator;

    @Autowired
    public YourController(IMediator mediator) {
        this.mediator = mediator;
    }
}
```

---

## 📝 Commands: Changing State

Commands represent operations that modify system state (create, update, delete).

### Creating a Command (Simple Pattern)

Let's start with the actual pattern used in the codebase:

**1. Define Command Record:**
```java
package com.quizfun.orchestrationlayer.commands;

import com.quizfun.globalshared.mediator.ICommand;

// Simple command - matches actual implementation style
public record CreateUserCommand(
    String username,
    String email
) implements ICommand<CreateUserResult> {
}
```

**2. Define Result Record:**
```java
package com.quizfun.orchestrationlayer.commands;

// Simple result - matches actual implementation
public record CreateUserResult(
    Long userId,
    String username,
    String email,
    String status
) {
}
```

**3. Create Command Handler:**
```java
package com.quizfun.orchestrationlayer.handlers;

import com.quizfun.globalshared.mediator.ICommandHandler;
import com.quizfun.globalshared.mediator.Result;
import com.quizfun.internallayer.service.UserValidationService;
import org.springframework.stereotype.Service;
import java.util.concurrent.ThreadLocalRandom;

@Service  // ← This annotation enables auto-discovery
public class CreateUserCommandHandler implements ICommandHandler<CreateUserCommand, CreateUserResult> {

    private final UserValidationService userValidationService;

    public CreateUserCommandHandler(UserValidationService userValidationService) {
        this.userValidationService = userValidationService;
    }

    @Override
    public Result<CreateUserResult> handle(CreateUserCommand command) {
        // Simple validation - matches actual pattern
        if (!userValidationService.validateUserData(command.username(), command.email())) {
            return Result.failure("Invalid user data: username or email format is incorrect");
        }

        // Simple ID generation - matches actual implementation
        Long userId = ThreadLocalRandom.current().nextLong(1000, 9999);

        CreateUserResult result = new CreateUserResult(
            userId,
            command.username(),
            command.email(),
            "CREATED"  // Simple status - matches actual pattern
        );

        return Result.success("User created successfully", result);
    }
}
```

**4. Use in Controller:**
```java
@PostMapping("/create-user")
public ResponseEntity<Result<CreateUserResult>> createUser(
        @RequestParam String username,  // Simple params - matches actual implementation
        @RequestParam String email) {

    CreateUserCommand command = new CreateUserCommand(username, email);
    Result<CreateUserResult> result = mediator.send(command);

    if (result.success()) {
        return ResponseEntity.ok(result);
    } else {
        return ResponseEntity.badRequest().body(result);
    }
}
```

### Creating a More Complex Command (Advanced Pattern)

Once you understand the basic pattern, you can extend it:

**Complex Command Example:**
```java
public record CreateProductCommand(
    String name,
    String description,
    BigDecimal price,
    String category,
    List<String> tags
) implements ICommand<CreateProductResult> {
}

@Service
public class CreateProductCommandHandler implements ICommandHandler<CreateProductCommand, CreateProductResult> {

    private final ProductService productService;
    private final ValidationService validationService;

    @Override
    public Result<CreateProductResult> handle(CreateProductCommand command) {
        // Multi-step validation
        if (command.name() == null || command.name().trim().isEmpty()) {
            return Result.failure("Product name is required");
        }

        if (command.price() == null || command.price().compareTo(BigDecimal.ZERO) <= 0) {
            return Result.failure("Product price must be positive");
        }

        if (!validationService.isValidCategory(command.category())) {
            return Result.failure("Invalid product category: " + command.category());
        }

        // Business logic with error handling
        try {
            Long productId = productService.createProduct(command);

            CreateProductResult result = new CreateProductResult(
                productId,
                command.name(),
                "ACTIVE",
                LocalDateTime.now()
            );

            return Result.success("Product created successfully", result);
        } catch (DuplicateProductException e) {
            return Result.failure("Product with this name already exists");
        } catch (Exception e) {
            return Result.failure("Failed to create product: " + e.getMessage());
        }
    }
}
```

---

## 🔍 Queries: Reading Data

Queries represent read-only operations that don't modify system state.

### Creating a Query (Simple Pattern)

Let's start with the actual pattern used in the codebase:

**1. Define Query Record:**
```java
package com.quizfun.orchestrationlayer.queries;

import com.quizfun.globalshared.mediator.IQuery;

// Simple query - matches actual implementation style
public record GetUserByIdQuery(
    Long userId
) implements IQuery<GetUserByIdResult> {
}
```

**2. Define Result Record:**
```java
package com.quizfun.orchestrationlayer.queries;

// Simple result - matches actual implementation
public record GetUserByIdResult(
    Long userId,
    String username,
    String email,
    String status
) {
}
```

**3. Create Query Handler:**
```java
package com.quizfun.orchestrationlayer.handlers;

import com.quizfun.globalshared.mediator.IQueryHandler;
import com.quizfun.globalshared.mediator.Result;
import com.quizfun.internallayer.service.UserValidationService;
import org.springframework.stereotype.Service;

@Service
public class GetUserByIdQueryHandler implements IQueryHandler<GetUserByIdQuery, GetUserByIdResult> {

    private final UserValidationService userValidationService;

    public GetUserByIdQueryHandler(UserValidationService userValidationService) {
        this.userValidationService = userValidationService;
    }

    @Override
    public Result<GetUserByIdResult> handle(GetUserByIdQuery query) {
        // Simple validation - matches actual pattern
        if (query.userId() == null || query.userId() <= 0) {
            return Result.failure("Invalid user ID provided");
        }

        // Simple data creation - matches actual implementation
        // In real scenarios, you'd query a database here
        GetUserByIdResult result = new GetUserByIdResult(
            query.userId(),
            "john_doe",           // In real app: userService.getUsername(query.userId())
            "john@example.com",   // In real app: userService.getEmail(query.userId())
            "ACTIVE"              // In real app: userService.getStatus(query.userId())
        );

        return Result.success("User retrieved successfully", result);
    }
}
```

**4. Use in Controller:**
```java
@GetMapping("/get-user/{userId}")  // Path variable - matches actual implementation
public ResponseEntity<Result<GetUserByIdResult>> getUserById(@PathVariable Long userId) {
    GetUserByIdQuery query = new GetUserByIdQuery(userId);
    Result<GetUserByIdResult> result = mediator.send(query);

    if (result.success()) {
        return ResponseEntity.ok(result);
    } else {
        return ResponseEntity.badRequest().body(result);
    }
}
```

### Creating a More Complex Query (Advanced Pattern)

Once you understand the basic pattern, you can extend it:

**Complex Query Example:**
```java
public record GetProductsByPriceRangeQuery(
    BigDecimal minPrice,
    BigDecimal maxPrice,
    int page,
    int size,
    String sortBy
) implements IQuery<PagedProductResult> {
}

public record PagedProductResult(
    List<ProductSummary> products,
    int totalCount,
    int pageNumber,
    int pageSize,
    boolean hasNext
) {
}

@Service
public class GetProductsByPriceRangeQueryHandler implements IQueryHandler<GetProductsByPriceRangeQuery, PagedProductResult> {

    private final ProductQueryService productQueryService;

    @Override
    public Result<PagedProductResult> handle(GetProductsByPriceRangeQuery query) {
        // Multi-step validation
        if (query.minPrice() == null || query.minPrice().compareTo(BigDecimal.ZERO) < 0) {
            return Result.failure("Minimum price must be positive");
        }

        if (query.maxPrice() == null || query.maxPrice().compareTo(query.minPrice()) < 0) {
            return Result.failure("Maximum price must be greater than minimum price");
        }

        if (query.page() < 0 || query.size() <= 0 || query.size() > 100) {
            return Result.failure("Invalid pagination parameters");
        }

        // Data retrieval with error handling
        try {
            List<ProductSummary> products = productQueryService.findByPriceRange(
                query.minPrice(),
                query.maxPrice(),
                query.page(),
                query.size(),
                query.sortBy()
            );

            int totalCount = productQueryService.countByPriceRange(query.minPrice(), query.maxPrice());
            boolean hasNext = (query.page() + 1) * query.size() < totalCount;

            PagedProductResult result = new PagedProductResult(
                products,
                totalCount,
                query.page(),
                query.size(),
                hasNext
            );

            return Result.success("Products retrieved successfully", result);
        } catch (Exception e) {
            return Result.failure("Failed to retrieve products: " + e.getMessage());
        }
    }
}
```

---

## 🎯 Understanding the Result Pattern

The `Result<T>` pattern is central to the mediator library. Understanding its nuances is crucial for effective usage.

### Result Pattern Basics

The `Result<T>` record has three components:
```java
public record Result<T>(
    boolean success,    // Whether operation succeeded
    String message,     // Descriptive message
    T data             // The actual result data (null on failure)
) {
    // Static factory methods for common patterns
    public static <T> Result<T> success(T data) { ... }
    public static <T> Result<T> success(String message, T data) { ... }
    public static <T> Result<T> failure(String message) { ... }
    public static <T> Result<T> failure(String message, T data) { ... }
}
```

### When to Use Messages vs. Default Messages

**With Custom Message (Recommended for user-facing operations):**
```java
// Good: Specific success message
return Result.success("User created successfully", result);

// Good: Clear error message
return Result.failure("Invalid user data: username or email format is incorrect");
```

**Without Custom Message (For internal operations):**
```java
// Uses default message: "Operation completed successfully"
return Result.success(result);
```

### Message Guidelines from Actual Implementation

Based on the codebase patterns:

**Success Messages Should:**
- Be action-oriented: "User created successfully", "User retrieved successfully"
- Confirm what happened: "Order processed and payment confirmed"
- Avoid technical jargon: "Record updated" vs "Database entity persisted"

**Error Messages Should:**
- Be specific: "Invalid user ID provided" vs "Validation failed"
- Include context: "Username must be between 3-50 characters" vs "Invalid input"
- Guide next steps: "Email already exists. Please use a different email."

### Result Usage Patterns

**In Handlers:**
```java
@Override
public Result<CreateUserResult> handle(CreateUserCommand command) {
    // Input validation failures
    if (command.username() == null || command.username().trim().isEmpty()) {
        return Result.failure("Username is required");
    }

    // Business rule validation failures
    if (userRepository.existsByUsername(command.username())) {
        return Result.failure("Username already exists. Please choose a different username.");
    }

    try {
        // Success with descriptive message
        User user = userService.createUser(command);
        return Result.success("User created successfully", new CreateUserResult(user));
    } catch (DatabaseException e) {
        // Technical failures with user-friendly message
        return Result.failure("Unable to create user due to system error. Please try again.");
    }
}
```

**In Controllers:**
```java
@PostMapping("/users")
public ResponseEntity<Result<CreateUserResult>> createUser(@RequestBody CreateUserRequest request) {
    CreateUserCommand command = new CreateUserCommand(request.username(), request.email());
    Result<CreateUserResult> result = mediator.send(command);

    // Simple success/failure routing based on result.success()
    if (result.success()) {
        return ResponseEntity.ok(result);           // 200 with success message and data
    } else {
        return ResponseEntity.badRequest().body(result);  // 400 with error message
    }
}
```

**Advanced Controller Response Mapping:**
```java
@PostMapping("/users")
public ResponseEntity<Result<CreateUserResult>> createUser(@RequestBody CreateUserRequest request) {
    CreateUserCommand command = new CreateUserCommand(request.username(), request.email());
    Result<CreateUserResult> result = mediator.send(command);

    // Map specific error types to appropriate HTTP status codes
    if (result.success()) {
        return ResponseEntity.ok(result);
    } else if (result.message().contains("already exists")) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(result);     // 409 Conflict
    } else if (result.message().contains("Invalid") || result.message().contains("required")) {
        return ResponseEntity.badRequest().body(result);                    // 400 Bad Request
    } else {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);  // 500 Internal Server Error
    }
}
```

### JSON Response Examples

**Successful Response:**
```json
{
  "success": true,
  "message": "User created successfully",
  "data": {
    "userId": 1234,
    "username": "john_doe",
    "email": "john@example.com",
    "status": "CREATED"
  }
}
```

**Failure Response:**
```json
{
  "success": false,
  "message": "Invalid user data: username or email format is incorrect",
  "data": null
}
```

### Common Anti-Patterns to Avoid

**❌ Vague Error Messages:**
```java
return Result.failure("Error occurred");              // Too vague
return Result.failure("Something went wrong");        // Not helpful
```

**❌ Technical Error Messages:**
```java
return Result.failure("SQLException: Duplicate key violation"); // Too technical
```

**❌ Missing Success Messages:**
```java
// For user-facing operations, include a message
return Result.success(result);  // Missing context for user
```

**✅ Good Patterns:**
```java
return Result.failure("Username must be between 3-50 characters");
return Result.failure("Email format is invalid. Please use a valid email address.");
return Result.success("User profile updated successfully", result);
```

---

## 🔄 Cross-Module Usage

The mediator library excels at cross-module communication in Maven multi-module projects.

### Scenario: Handler in Different Module

**Actual Module Structure (Matches Project Layout):**
```
📦 maven-submodule-base (root)
├── 📁 orchestration-layer (controllers + some handlers)
├── 📁 internal-layer (business services)
├── 📁 external-service-proxy (external integrations)
└── 📁 global-shared-library (mediator)
```

**Simple Cross-Module Pattern (Based on Actual Implementation):**

**1. Define Command in Orchestration Layer:**
```java
// orchestration-layer/src/main/java/com/quizfun/orchestrationlayer/commands/ValidateUserCommand.java
package com.quizfun.orchestrationlayer.commands;

import com.quizfun.globalshared.mediator.ICommand;

public record ValidateUserCommand(
    String username,
    String email
) implements ICommand<ValidateUserResult> {
}

public record ValidateUserResult(
    boolean isValid,
    String message,
    String validationType
) {
}
```

**2. Create Handler in Internal Layer:**
```java
// internal-layer/src/main/java/com/quizfun/internallayer/handlers/ValidateUserCommandHandler.java
package com.quizfun.internallayer.handlers;

import com.quizfun.globalshared.mediator.ICommandHandler;
import com.quizfun.globalshared.mediator.Result;
import com.quizfun.internallayer.service.UserValidationService;
import com.quizfun.orchestrationlayer.commands.ValidateUserCommand;
import com.quizfun.orchestrationlayer.commands.ValidateUserResult;
import org.springframework.stereotype.Service;

@Service
public class ValidateUserCommandHandler implements ICommandHandler<ValidateUserCommand, ValidateUserResult> {

    private final UserValidationService userValidationService;

    public ValidateUserCommandHandler(UserValidationService userValidationService) {
        this.userValidationService = userValidationService;
    }

    @Override
    public Result<ValidateUserResult> handle(ValidateUserCommand command) {
        // Use existing service from internal-layer
        boolean isValid = userValidationService.validateUserData(command.username(), command.email());

        ValidateUserResult result = new ValidateUserResult(
            isValid,
            isValid ? "User data is valid" : "Invalid user data format",
            "EMAIL_USERNAME_VALIDATION"
        );

        return isValid
            ? Result.success("Validation completed successfully", result)
            : Result.failure("Validation failed", result);
    }
}
```

**3. Update Component Scanning (Critical Step):**
```java
// orchestration-layer/src/main/java/com/quizfun/orchestrationlayer/OrchestrationLayerApplication.java
@SpringBootApplication(scanBasePackages = {
    "com.quizfun.orchestrationlayer",
    "com.quizfun.internallayer",          // ← Add to find handlers in internal-layer
    "com.quizfun.globalshared"            // ← Required for mediator
})
public class OrchestrationLayerApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrchestrationLayerApplication.class, args);
    }
}
```

**4. Add Dependencies to orchestration-layer/pom.xml:**
```xml
<dependencies>
    <!-- Internal layer for business logic -->
    <dependency>
        <groupId>com.quizfun</groupId>
        <artifactId>internal-layer</artifactId>
        <version>${project.version}</version>
    </dependency>

    <!-- Global shared library for mediator -->
    <dependency>
        <groupId>com.quizfun</groupId>
        <artifactId>global-shared-library</artifactId>
        <version>${project.version}</version>
    </dependency>
</dependencies>
```

**5. Use from Controller:**
```java
// orchestration-layer controller - matches actual implementation style
@PostMapping("/validate-user")
public ResponseEntity<Result<ValidateUserResult>> validateUser(
        @RequestParam String username,
        @RequestParam String email) {

    ValidateUserCommand command = new ValidateUserCommand(username, email);
    Result<ValidateUserResult> result = mediator.send(command);

    if (result.success()) {
        return ResponseEntity.ok(result);
    } else {
        return ResponseEntity.badRequest().body(result);
    }
}
```

### Advanced Cross-Module Pattern

For more complex scenarios, you can create handlers that span multiple modules:

**Complex Handler Example:**
```java
// internal-layer handler that uses external services
@Service
public class CreateUserWithNotificationHandler implements ICommandHandler<CreateUserCommand, CreateUserResult> {

    private final UserValidationService userValidationService;    // internal-layer
    private final EmailNotificationService emailService;         // external-service-proxy
    private final UserAuditService auditService;                // internal-layer

    @Override
    public Result<CreateUserResult> handle(CreateUserCommand command) {
        // 1. Validation (internal service)
        if (!userValidationService.validateUserData(command.username(), command.email())) {
            return Result.failure("Invalid user data: username or email format is incorrect");
        }

        try {
            // 2. Create user (internal service)
            Long userId = ThreadLocalRandom.current().nextLong(1000, 9999);

            // 3. Send notification (external service)
            emailService.sendWelcomeEmail(command.email(), command.username());

            // 4. Audit logging (internal service)
            auditService.logUserCreation(userId, command.username());

            CreateUserResult result = new CreateUserResult(userId, command.username(), command.email(), "CREATED");
            return Result.success("User created and welcome email sent", result);

        } catch (EmailServiceException e) {
            return Result.failure("User created but welcome email failed to send");
        } catch (Exception e) {
            return Result.failure("Failed to create user: " + e.getMessage());
        }
    }
}
```

### Module Dependencies

**Ensure proper Maven dependencies:**

**orchestration-layer/pom.xml:**
```xml
<dependencies>
    <dependency>
        <groupId>com.quizfun</groupId>
        <artifactId>business-layer</artifactId>
    </dependency>
    <dependency>
        <groupId>com.quizfun</groupId>
        <artifactId>global-shared-library</artifactId>
    </dependency>
</dependencies>
```

**business-layer/pom.xml:**
```xml
<dependencies>
    <dependency>
        <groupId>com.quizfun</groupId>
        <artifactId>global-shared-library</artifactId>
    </dependency>
    <dependency>
        <groupId>com.quizfun</groupId>
        <artifactId>data-layer</artifactId>
    </dependency>
</dependencies>
```

---

## ✅ Best Practices

### 1. Naming Conventions

**Commands (Actions):**
- `CreateUserCommand`, `UpdateProductCommand`, `DeleteOrderCommand`
- Use verbs indicating state change

**Queries (Questions):**
- `GetUserByIdQuery`, `FindProductsByCategoryQuery`, `CountActiveOrdersQuery`
- Use verbs indicating data retrieval

**Results:**
- `CreateUserResult`, `GetUserByIdResult`
- Match command/query names

### 2. Handler Organization

**Package Structure:**
```
📁 your-module/
├── 📁 commands/
│   ├── CreateUserCommand.java
│   └── CreateUserResult.java
├── 📁 queries/
│   ├── GetUserByIdQuery.java
│   └── GetUserByIdResult.java
└── 📁 handlers/
    ├── CreateUserCommandHandler.java
    └── GetUserByIdQueryHandler.java
```

### 3. Validation Strategy

Based on the actual implementation patterns in the codebase:

**Simple Validation (Matches Actual Implementation):**
```java
@Override
public Result<CreateUserResult> handle(CreateUserCommand command) {
    // Simple service-based validation - matches actual pattern
    if (!userValidationService.validateUserData(command.username(), command.email())) {
        return Result.failure("Invalid user data: username or email format is incorrect");
    }

    // Continue with business logic
    // ...
}

@Override
public Result<GetUserByIdResult> handle(GetUserByIdQuery query) {
    // Simple parameter validation - matches actual pattern
    if (query.userId() == null || query.userId() <= 0) {
        return Result.failure("Invalid user ID provided");
    }

    // Continue with data retrieval
    // ...
}
```

**Advanced Validation (For Complex Scenarios):**
```java
@Override
public Result<CreateProductResult> handle(CreateProductCommand command) {
    // 1. Null/empty checks
    if (command.name() == null || command.name().trim().isEmpty()) {
        return Result.failure("Product name is required");
    }

    // 2. Format validation
    if (command.price() == null || command.price().compareTo(BigDecimal.ZERO) <= 0) {
        return Result.failure("Product price must be positive");
    }

    // 3. External service validation
    if (!validationService.isValidCategory(command.category())) {
        return Result.failure("Invalid product category: " + command.category());
    }

    // 4. Business rule validation
    if (productRepository.existsByName(command.name())) {
        return Result.failure("Product with this name already exists");
    }

    // Continue with business logic
    // ...
}
```

**Validation Best Practices:**
- **Start Simple**: Use service-based validation like the actual implementation
- **Be Specific**: "Invalid user ID provided" vs "Validation failed"
- **Single Responsibility**: One validation check per return statement
- **User-Friendly**: Avoid technical jargon in error messages

### 4. Error Handling

**Message Consistency (Based on Actual Implementation):**
```java
// ✅ Good: Matches actual implementation patterns
return Result.failure("Invalid user data: username or email format is incorrect");
return Result.failure("Invalid user ID provided");

// ✅ Good: Specific and actionable
return Result.failure("Username must be between 3-50 characters");
return Result.failure("Email format is invalid. Please enter a valid email address.");

// ❌ Avoid: Vague and unhelpful
return Result.failure("Error occurred");
return Result.failure("Something went wrong");
```

**Exception Handling Patterns:**
```java
@Override
public Result<CreateUserResult> handle(CreateUserCommand command) {
    try {
        // Business logic
        User user = userService.createUser(command);
        return Result.success("User created successfully", new CreateUserResult(user));
    } catch (DuplicateUserException e) {
        // Known business exceptions -> user-friendly messages
        return Result.failure("Username already exists. Please choose a different username.");
    } catch (ValidationException e) {
        // Validation exceptions -> specific guidance
        return Result.failure("Invalid input: " + e.getMessage());
    } catch (Exception e) {
        // Unknown exceptions -> log and generic message
        log.error("Unexpected error creating user: {}", command, e);
        return Result.failure("Unable to create user. Please try again later.");
    }
}
```

**Error Handling Layers:**
1. **Input Validation**: Check parameters before processing
2. **Business Validation**: Check business rules and constraints
3. **Exception Handling**: Catch and convert technical exceptions
4. **User-Friendly Messages**: Always return helpful error messages

### 5. Return Types

**Use Specific Types:**
```java
// Good: Specific return type
public record GetUsersResult(
    List<UserSummary> users,
    int totalCount,
    int pageNumber,
    int pageSize
) {}

// Avoid: Generic types
public record GetUsersResult(Object data) {}
```

---

## 🧪 Testing

### Unit Testing Handlers

**Test Setup:**
```java
@ExtendWith(MockitoExtension.class)
class CreateUserCommandHandlerTest {

    @Mock
    private UserService userService;

    @Mock
    private ValidationService validationService;

    private CreateUserCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new CreateUserCommandHandler(userService, validationService);
    }

    @Test
    @DisplayName("Should create user successfully with valid data")
    void shouldCreateUserSuccessfully() {
        // Given
        CreateUserCommand command = new CreateUserCommand("john", "john@example.com");
        when(validationService.validateUserData("john", "john@example.com")).thenReturn(true);
        when(userService.createUser(any())).thenReturn(123L);

        // When
        Result<CreateUserResult> result = handler.handle(command);

        // Then
        assertThat(result.success()).isTrue();
        assertThat(result.data().userId()).isEqualTo(123L);
        verify(userService).createUser(any());
    }

    @Test
    @DisplayName("Should fail when validation fails")
    void shouldFailWhenValidationFails() {
        // Given
        CreateUserCommand command = new CreateUserCommand("", "invalid-email");
        when(validationService.validateUserData("", "invalid-email")).thenReturn(false);

        // When
        Result<CreateUserResult> result = handler.handle(command);

        // Then
        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("Invalid user data");
        verify(userService, never()).createUser(any());
    }
}
```

### Integration Testing

**Test Mediator with Real Handlers:**
```java
@SpringBootTest
@TestPropertySource(properties = "spring.profiles.active=test")
class MediatorIntegrationTest {

    @Autowired
    private IMediator mediator;

    @Test
    void shouldRouteCommandToCorrectHandler() {
        // Given
        CreateUserCommand command = new CreateUserCommand("john", "john@example.com");

        // When
        Result<CreateUserResult> result = mediator.send(command);

        // Then
        assertThat(result.success()).isTrue();
        assertThat(result.data()).isNotNull();
    }
}
```

### Testing Coverage Commands

**Run tests with coverage:**
```bash
# Test specific module
mvn test jacoco:report -pl your-module

# Check coverage meets threshold
mvn verify jacoco:check -pl your-module
```

---

## 🔧 Troubleshooting

### Common Issues

**1. Handler Not Found**
```
Error: No handler found for command: CreateUserCommand
```

**Solutions:**
- Verify handler class has `@Service` annotation
- Check component scanning includes handler package
- Ensure handler implements correct interface

**2. Generic Type Resolution Failed**
```
Error: Cannot extract command type from handler
```

**Solutions:**
- Avoid using raw types in handler implementation
- Ensure handler directly implements interface (not through inheritance)
- Check for generic type erasure issues

**3. Dependency Injection Fails**
```
Error: No qualifying bean of type 'IMediator'
```

**Solutions:**
- Add `global-shared-library` dependency to module
- Include `com.quizfun.globalshared` in component scanning
- Verify Spring Boot auto-configuration is enabled

### Debug Commands

**Check registered handlers:**
```bash
# Enable debug logging
logging.level.com.quizfun.globalshared.mediator=DEBUG
```

**Verify component scanning:**
```java
@Component
public class DiagnosticComponent {

    @Autowired
    public DiagnosticComponent(ApplicationContext context) {
        String[] handlerBeans = context.getBeanNamesForType(ICommandHandler.class);
        System.out.println("Found command handlers: " + Arrays.toString(handlerBeans));
    }
}
```

---

## 🚀 Advanced Patterns

### 1. Middleware Pattern

**Create Intercepting Handlers:**
```java
@Service
@Order(1)  // Execute before business handlers
public class LoggingCommandHandler implements ICommandHandler<ICommand<?>, Object> {

    @Override
    public Result<Object> handle(ICommand<?> command) {
        log.info("Executing command: {}", command.getClass().getSimpleName());
        // Delegate to next handler in chain
        return delegate.handle(command);
    }
}
```

### 2. Async Processing

**Async Command Handler:**
```java
@Service
public class AsyncEmailCommandHandler implements ICommandHandler<SendEmailCommand, SendEmailResult> {

    @Async
    @Override
    public CompletableFuture<Result<SendEmailResult>> handle(SendEmailCommand command) {
        return CompletableFuture.supplyAsync(() -> {
            // Long-running email operation
            emailService.sendEmail(command.to(), command.subject(), command.body());
            return Result.success(new SendEmailResult("SENT"));
        });
    }
}
```

### 3. Validation Framework

**Annotation-Based Validation:**
```java
@Component
public class ValidationAspect {

    @Around("@annotation(Validate)")
    public Object validateCommand(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg instanceof ICommand) {
                Result<?> validationResult = validator.validate(arg);
                if (!validationResult.success()) {
                    return validationResult;
                }
            }
        }
        return joinPoint.proceed();
    }
}
```

### 4. Event Sourcing

**Event-Driven Commands:**
```java
@Service
public class EventSourcingCommandHandler implements ICommandHandler<CreateOrderCommand, CreateOrderResult> {

    private final EventStore eventStore;

    @Override
    public Result<CreateOrderResult> handle(CreateOrderCommand command) {
        // Create events instead of directly modifying state
        OrderCreatedEvent event = new OrderCreatedEvent(
            UUID.randomUUID(),
            command.customerId(),
            command.items(),
            Instant.now()
        );

        eventStore.append(event);

        return Result.success(new CreateOrderResult(event.orderId(), "CREATED"));
    }
}
```

---

## 📚 Summary

The mediator library provides a powerful, type-safe way to implement CQRS patterns in Spring Boot applications. Key benefits:

- **Zero Configuration**: Just add `@Service` and it works
- **Type Safety**: Compile-time guarantees with runtime verification
- **Cross-Module**: Clean communication between Maven modules
- **Testable**: Easy to unit test handlers independently
- **Extensible**: Support for middleware, async, and advanced patterns

For more specific examples, see the related documentation:
- [How to create mediator in global shared library and be used in orchestration layer.md](./how%20to%20create%20mediator%20in%20global%20shared%20library%20and%20be%20used%20in%20orchestration%20layer.md)
- [How to extend current mediator to develop query handling capability.md](./how%20to%20extend%20current%20mediator%20to%20develop%20query%20handling%20capability.md)
- [How to create unit test for mediator library and get test coverage report.md](./how%20to%20create%20unit%20test%20for%20mediator%20library%20and%20get%20test%20coverage%20report.md)

The mediator pattern transforms complex multi-module applications into clean, maintainable, and testable codebases.