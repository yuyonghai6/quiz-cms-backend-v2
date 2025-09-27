# 🎯 How to Create Mediator in Global Shared Library and Use in Orchestration Layer

## 📋 Overview

This guide demonstrates how to implement a mediator pattern library in a Spring Boot multi-module project, specifically placing the mediator infrastructure in the `global-shared-library` module and consuming it from the `orchestration-layer`. The mediator leverages Spring Boot's dependency injection for automatic command handler discovery and registration.

## 🏗️ Project Architecture

```
📦 maven-submodule-base (root)
├── 📁 global-shared-library (mediator infrastructure)
│   └── 🔧 IMediator, ICommand, ICommandHandler, Result
├── 📁 orchestration-layer (main application)
│   ├── 🌐 Controllers (HTTP endpoints)
│   ├── 📝 Commands (request DTOs)
│   └── ⚙️ Handlers (business logic)
├── 📁 internal-layer (business services)
└── 📁 external-service-proxy (external integrations)
```

---

## 🚀 Step 1: Setup Module Dependencies

### 1.1 Add Spring Boot Starter to Global Shared Library

**File:** `global-shared-library/pom.xml`

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
    </dependency>
</dependencies>
```

### 1.2 Add Global Shared Library Dependency to Orchestration Layer

**File:** `orchestration-layer/pom.xml`

```xml
<dependencies>
    <!-- Global shared library for mediator -->
    <dependency>
        <groupId>com.quizfun</groupId>
        <artifactId>global-shared-library</artifactId>
        <version>${project.version}</version>
    </dependency>

    <!-- Your existing dependencies... -->
</dependencies>
```

### 1.3 Update Component Scanning

**File:** `orchestration-layer/src/main/java/com/quizfun/orchestrationlayer/OrchestrationLayerApplication.java`

```java
@SpringBootApplication(scanBasePackages = {
    "com.quizfun.orchestrationlayer",
    "com.quizfun.internallayer",
    "com.quizfun.globalshared"  // ← Add this for mediator discovery
})
public class OrchestrationLayerApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrchestrationLayerApplication.class, args);
    }
}
```

---

## 📝 Step 2: Create Core Mediator Infrastructure

### 2.1 Create Result Record Class

**File:** `global-shared-library/src/main/java/com/quizfun/globalshared/mediator/Result.java`

```java
package com.quizfun.globalshared.mediator;

public record Result<T>(
    boolean success,
    String message,
    T data
) {
    public static <T> Result<T> success(T data) {
        return new Result<>(true, "Operation completed successfully", data);
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(true, message, data);
    }

    public static <T> Result<T> failure(String message) {
        return new Result<>(false, message, null);
    }

    public static <T> Result<T> failure(String message, T data) {
        return new Result<>(false, message, data);
    }
}
```

### 2.2 Create Command Interface

**File:** `global-shared-library/src/main/java/com/quizfun/globalshared/mediator/ICommand.java`

```java
package com.quizfun.globalshared.mediator;

public interface ICommand<T> {
}
```

### 2.3 Create Command Handler Interface

**File:** `global-shared-library/src/main/java/com/quizfun/globalshared/mediator/ICommandHandler.java`

```java
package com.quizfun.globalshared.mediator;

public interface ICommandHandler<TCommand extends ICommand<TResult>, TResult> {
    Result<TResult> handle(TCommand command);
}
```

### 2.4 Create Mediator Interface

**File:** `global-shared-library/src/main/java/com/quizfun/globalshared/mediator/IMediator.java`

```java
package com.quizfun.globalshared.mediator;

public interface IMediator {
    <T> Result<T> send(ICommand<T> command);
}
```

---

## ⚙️ Step 3: Implement Spring Boot Auto-Registration Mediator

### 3.1 Create Mediator Implementation

**File:** `global-shared-library/src/main/java/com/quizfun/globalshared/mediator/MediatorImpl.java`

```java
package com.quizfun.globalshared.mediator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

@Service
public class MediatorImpl implements IMediator {

    private final Map<Class<?>, ICommandHandler<?, ?>> handlerRegistry = new HashMap<>();
    private final ApplicationContext applicationContext;

    @Autowired
    public MediatorImpl(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        registerHandlers();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Result<T> send(ICommand<T> command) {
        Class<?> commandType = command.getClass();
        ICommandHandler<ICommand<T>, T> handler = (ICommandHandler<ICommand<T>, T>) handlerRegistry.get(commandType);

        if (handler == null) {
            return Result.failure("No handler found for command: " + commandType.getSimpleName());
        }

        try {
            return handler.handle(command);
        } catch (Exception e) {
            return Result.failure("Error handling command: " + e.getMessage());
        }
    }

    @SuppressWarnings("rawtypes")
    private void registerHandlers() {
        Map<String, ICommandHandler> handlers = applicationContext.getBeansOfType(ICommandHandler.class);

        for (ICommandHandler<?, ?> handler : handlers.values()) {
            Class<?> commandType = getCommandTypeFromHandler(handler);
            if (commandType != null) {
                handlerRegistry.put(commandType, handler);
            }
        }
    }

    private Class<?> getCommandTypeFromHandler(ICommandHandler<?, ?> handler) {
        Type[] genericInterfaces = handler.getClass().getGenericInterfaces();

        for (Type genericInterface : genericInterfaces) {
            if (genericInterface instanceof ParameterizedType parameterizedType) {
                if (parameterizedType.getRawType().equals(ICommandHandler.class)) {
                    Type[] typeArguments = parameterizedType.getActualTypeArguments();
                    if (typeArguments.length > 0 && typeArguments[0] instanceof Class<?>) {
                        return (Class<?>) typeArguments[0];
                    }
                }
            }
        }

        return null;
    }
}
```

**🔑 Key Features:**
- **Auto-Discovery**: Uses `ApplicationContext.getBeansOfType()` to find all command handlers
- **Type Safety**: Reflection extracts generic type information for command→handler mapping
- **Error Handling**: Graceful handling of missing handlers and execution errors
- **Singleton**: Spring manages the mediator as a singleton with cached handler registry

---

## 📦 Step 4: Create Commands and Handlers in Orchestration Layer

### 4.1 Create Command Record

**File:** `orchestration-layer/src/main/java/com/quizfun/orchestrationlayer/commands/CreateUserCommand.java`

```java
package com.quizfun.orchestrationlayer.commands;

import com.quizfun.globalshared.mediator.ICommand;

public record CreateUserCommand(
    String username,
    String email
) implements ICommand<CreateUserResult> {
}
```

### 4.2 Create Result Record

**File:** `orchestration-layer/src/main/java/com/quizfun/orchestrationlayer/commands/CreateUserResult.java`

```java
package com.quizfun.orchestrationlayer.commands;

public record CreateUserResult(
    Long userId,
    String username,
    String email,
    String status
) {
}
```

### 4.3 Create Command Handler

**File:** `orchestration-layer/src/main/java/com/quizfun/orchestrationlayer/handlers/CreateUserCommandHandler.java`

```java
package com.quizfun.orchestrationlayer.handlers;

import com.quizfun.globalshared.mediator.ICommandHandler;
import com.quizfun.globalshared.mediator.Result;
import com.quizfun.internallayer.service.UserValidationService;
import com.quizfun.orchestrationlayer.commands.CreateUserCommand;
import com.quizfun.orchestrationlayer.commands.CreateUserResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Service
public class CreateUserCommandHandler implements ICommandHandler<CreateUserCommand, CreateUserResult> {

    private final UserValidationService userValidationService;

    @Autowired
    public CreateUserCommandHandler(UserValidationService userValidationService) {
        this.userValidationService = userValidationService;
    }

    @Override
    public Result<CreateUserResult> handle(CreateUserCommand command) {
        if (!userValidationService.validateUserData(command.username(), command.email())) {
            return Result.failure("Invalid user data: username or email format is incorrect");
        }

        Long userId = ThreadLocalRandom.current().nextLong(1000, 9999);

        CreateUserResult result = new CreateUserResult(
            userId,
            command.username(),
            command.email(),
            "CREATED"
        );

        return Result.success("User created successfully", result);
    }
}
```

**💡 Integration Notes:**
- Uses existing `UserValidationService` from `internal-layer`
- Demonstrates cross-module dependency injection
- Returns structured `Result<T>` for consistent error handling

---

## 🌐 Step 5: Create HTTP Controller

### 5.1 Create Controller with Mediator Injection

**File:** `orchestration-layer/src/main/java/com/quizfun/orchestrationlayer/controller/MediatorController.java`

```java
package com.quizfun.orchestrationlayer.controller;

import com.quizfun.globalshared.mediator.IMediator;
import com.quizfun.globalshared.mediator.Result;
import com.quizfun.orchestrationlayer.commands.CreateUserCommand;
import com.quizfun.orchestrationlayer.commands.CreateUserResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mediator")
public class MediatorController {

    private final IMediator mediator;

    @Autowired
    public MediatorController(IMediator mediator) {
        this.mediator = mediator;
    }

    @PostMapping("/create-user")
    public ResponseEntity<Result<CreateUserResult>> createUser(
            @RequestParam String username,
            @RequestParam String email) {

        CreateUserCommand command = new CreateUserCommand(username, email);
        Result<CreateUserResult> result = mediator.send(command);

        if (result.success()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }
}
```

**🎯 Controller Benefits:**
- Clean separation: Controller handles HTTP, mediator handles business logic
- Type safety: Strongly-typed command and result objects
- Consistent responses: All endpoints return `Result<T>` format

---

## 🔨 Step 6: Build and Test

### 6.1 Build the Project

```bash
# Build entire project from root directory
./orchestration-layer/mvnw clean install
```

### 6.2 Start the Application

```bash
# Start Spring Boot application
./orchestration-layer/mvnw spring-boot:run -pl orchestration-layer
```

### 6.3 Test with curl

```bash
# Test valid user creation
curl -X POST "http://localhost:8765/api/mediator/create-user?username=john&email=john@example.com"

# Expected Response:
# {"success":true,"message":"User created successfully","data":{"userId":4770,"username":"john","email":"john@example.com","status":"CREATED"}}

# Test invalid email validation
curl -X POST "http://localhost:8765/api/mediator/create-user?username=john&email=invalid-email"

# Expected Response:
# {"success":false,"message":"Invalid user data: username or email format is incorrect","data":null}
```

---

## 🎉 Step 7: Adding New Commands (The Magic!)

### 7.1 Create New Command

```java
// Just create a new command record
public record DeleteUserCommand(Long userId) implements ICommand<DeleteUserResult> {
}
```

### 7.2 Create New Handler

```java
// Add @Service - Spring Boot auto-discovers it!
@Service
public class DeleteUserCommandHandler implements ICommandHandler<DeleteUserCommand, DeleteUserResult> {
    @Override
    public Result<DeleteUserResult> handle(DeleteUserCommand command) {
        // Your business logic here
        return Result.success(new DeleteUserResult(command.userId(), "DELETED"));
    }
}
```

### 7.3 Use in Controller

```java
// No registration needed - mediator automatically routes!
@PostMapping("/delete-user")
public ResponseEntity<Result<DeleteUserResult>> deleteUser(@RequestParam Long userId) {
    DeleteUserCommand command = new DeleteUserCommand(userId);
    return ResponseEntity.ok(mediator.send(command));
}
```

**✨ That's it! No manual registration required.**

---

## 🔧 Key Design Patterns

### Auto-Registration Pattern
```java
// Spring Boot automatically discovers and registers handlers
Map<String, ICommandHandler> handlers = applicationContext.getBeansOfType(ICommandHandler.class);
```

### Type-Safe Command Routing
```java
// Uses reflection to map command types to handlers
private Class<?> getCommandTypeFromHandler(ICommandHandler<?, ?> handler) {
    // Reflection magic to extract generic type information
}
```

### Result Pattern
```java
// Consistent response structure across all operations
public record Result<T>(boolean success, String message, T data) {
    public static <T> Result<T> success(T data) { ... }
    public static <T> Result<T> failure(String message) { ... }
}
```

---

## 🚀 Benefits of This Approach

### 1. **Spring Boot "Magic"**
- Automatic handler discovery and registration
- No manual configuration or XML needed
- Leverages Spring's dependency injection

### 2. **Type Safety**
- Compile-time checking of command→handler relationships
- Generic interfaces prevent runtime casting errors
- IDE support for refactoring and navigation

### 3. **Separation of Concerns**
- Controllers handle HTTP concerns only
- Handlers contain pure business logic
- Mediator manages request routing

### 4. **Cross-Module Integration**
- Clean integration with existing services (`UserValidationService`)
- Modular architecture supports complex enterprise applications
- Easy to extend with new modules

### 5. **AOP Ready**
- Clean entry points for aspect-oriented programming
- Consistent command execution flow for logging/validation
- Perfect foundation for your planned AOP integration

---

## 📊 Architecture Flow

```
HTTP Request → Controller → Mediator → Handler Registry → Specific Handler → Business Logic → Result
     ↓              ↓           ↓              ↓                ↓               ↓           ↓
   REST API    Command DTO    Auto-Discovery  Type Mapping    @Service     Cross-Module   Response
```

---

## 🎯 Next Steps

1. **Add AOP Integration**: Use Spring AOP to add logging and validation aspects
2. **Error Handling**: Implement global exception handling for commands
3. **Metrics**: Add performance monitoring to command execution
4. **Validation**: Create command validation framework
5. **Testing**: Add unit and integration tests for handlers

This mediator pattern provides a solid foundation for complex command-based applications while maintaining the simplicity and power of Spring Boot's dependency injection system.