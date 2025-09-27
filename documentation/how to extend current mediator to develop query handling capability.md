# 🔍 How to Extend Current Mediator to Develop Query Handling Capability

## 📋 Overview

This guide demonstrates how to extend the existing command-based mediator pattern to support CQRS (Command Query Responsibility Segregation) by adding query and query handler capabilities. The extension maintains perfect consistency with the existing command pattern while enabling read-only operations through the same automatic registration system.

## 🏗️ Architecture Overview

```
📦 Existing Command Pattern
├── 🔧 ICommand<T> (marker interface)
├── ⚙️ ICommandHandler<TCommand, TResult> (processing interface)
├── 🎯 IMediator.send(ICommand<T>) (routing method)
└── 🤖 MediatorImpl (automatic Spring registration)

📦 New Query Pattern (Mirror Structure)
├── 🔍 IQuery<T> (marker interface)
├── 📊 IQueryHandler<TQuery, TResult> (processing interface)
├── 🎯 IMediator.send(IQuery<T>) (routing method)
└── 🤖 MediatorImpl (extended automatic registration)
```

---

## 🚀 Step 1: Create Query Interfaces in Global Shared Library

### 1.1 Create Query Marker Interface

**File:** `global-shared-library/src/main/java/com/quizfun/globalshared/mediator/IQuery.java`

```java
package com.quizfun.globalshared.mediator;

public interface IQuery<T> {
}
```

**💡 Design Note:** This mirrors the `ICommand<T>` interface exactly, maintaining consistency.

### 1.2 Create Query Handler Interface

**File:** `global-shared-library/src/main/java/com/quizfun/globalshared/mediator/IQueryHandler.java`

```java
package com.quizfun.globalshared.mediator;

public interface IQueryHandler<TQuery extends IQuery<TResult>, TResult> {
    Result<TResult> handle(TQuery query);
}
```

**💡 Design Note:** Follows the exact same signature pattern as `ICommandHandler` for consistency.

---

## 🔧 Step 2: Extend the Mediator Interface

### 2.1 Add Query Support to IMediator

**File:** `global-shared-library/src/main/java/com/quizfun/globalshared/mediator/IMediator.java`

```java
package com.quizfun.globalshared.mediator;

public interface IMediator {
    <T> Result<T> send(ICommand<T> command);
    <T> Result<T> send(IQuery<T> query);    // ← Add this line
}
```

**💡 Design Note:** Method overloading allows the same `send()` method name for both commands and queries.

---

## ⚙️ Step 3: Extend MediatorImpl for Query Handling

### 3.1 Add Query Handler Registry

**In:** `global-shared-library/src/main/java/com/quizfun/globalshared/mediator/MediatorImpl.java`

```java
@Service
public class MediatorImpl implements IMediator {

    private final Map<Class<?>, ICommandHandler<?, ?>> handlerRegistry = new HashMap<>();
    private final Map<Class<?>, IQueryHandler<?, ?>> queryHandlerRegistry = new HashMap<>();  // ← Add this
    private final ApplicationContext applicationContext;

    // ... existing code
}
```

### 3.2 Update Constructor to Register Query Handlers

```java
@Autowired
public MediatorImpl(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
    registerHandlers();
    registerQueryHandlers();  // ← Add this line
}
```

### 3.3 Add Query Send Method

```java
@SuppressWarnings("unchecked")
@Override
public <T> Result<T> send(IQuery<T> query) {
    Class<?> queryType = query.getClass();
    IQueryHandler<IQuery<T>, T> handler = (IQueryHandler<IQuery<T>, T>) queryHandlerRegistry.get(queryType);

    if (handler == null) {
        return Result.failure("No handler found for query: " + queryType.getSimpleName());
    }

    try {
        return handler.handle(query);
    } catch (Exception e) {
        return Result.failure("Error handling query: " + e.getMessage());
    }
}
```

### 3.4 Add Query Handler Registration Method

```java
@SuppressWarnings("rawtypes")
private void registerQueryHandlers() {
    Map<String, IQueryHandler> handlers = applicationContext.getBeansOfType(IQueryHandler.class);

    for (IQueryHandler<?, ?> handler : handlers.values()) {
        Class<?> queryType = getQueryTypeFromHandler(handler);
        if (queryType != null) {
            queryHandlerRegistry.put(queryType, handler);
        }
    }
}

private Class<?> getQueryTypeFromHandler(IQueryHandler<?, ?> handler) {
    Type[] genericInterfaces = handler.getClass().getGenericInterfaces();

    for (Type genericInterface : genericInterfaces) {
        if (genericInterface instanceof ParameterizedType parameterizedType) {
            if (parameterizedType.getRawType().equals(IQueryHandler.class)) {
                Type[] typeArguments = parameterizedType.getActualTypeArguments();
                if (typeArguments.length > 0 && typeArguments[0] instanceof Class<?>) {
                    return (Class<?>) typeArguments[0];
                }
            }
        }
    }

    return null;
}
```

**💡 Design Note:** This mirrors the existing command handler registration logic exactly.

---

## 📝 Step 4: Create Query and Query Handler in Orchestration Layer

### 4.1 Create Query Record

**File:** `orchestration-layer/src/main/java/com/quizfun/orchestrationlayer/queries/GetUserByIdQuery.java`

```java
package com.quizfun.orchestrationlayer.queries;

import com.quizfun.globalshared.mediator.IQuery;

public record GetUserByIdQuery(
    Long userId
) implements IQuery<GetUserByIdResult> {
}
```

### 4.2 Create Result Record

**File:** `orchestration-layer/src/main/java/com/quizfun/orchestrationlayer/queries/GetUserByIdResult.java`

```java
package com.quizfun.orchestrationlayer.queries;

public record GetUserByIdResult(
    Long userId,
    String username,
    String email,
    String status
) {
}
```

### 4.3 Create Query Handler

**File:** `orchestration-layer/src/main/java/com/quizfun/orchestrationlayer/handlers/GetUserByIdQueryHandler.java`

```java
package com.quizfun.orchestrationlayer.handlers;

import com.quizfun.globalshared.mediator.IQueryHandler;
import com.quizfun.globalshared.mediator.Result;
import com.quizfun.internallayer.service.UserValidationService;
import com.quizfun.orchestrationlayer.queries.GetUserByIdQuery;
import com.quizfun.orchestrationlayer.queries.GetUserByIdResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GetUserByIdQueryHandler implements IQueryHandler<GetUserByIdQuery, GetUserByIdResult> {

    private final UserValidationService userValidationService;

    @Autowired
    public GetUserByIdQueryHandler(UserValidationService userValidationService) {
        this.userValidationService = userValidationService;
    }

    @Override
    public Result<GetUserByIdResult> handle(GetUserByIdQuery query) {
        if (query.userId() == null || query.userId() <= 0) {
            return Result.failure("Invalid user ID provided");
        }

        // Simulate data retrieval (replace with actual data access)
        GetUserByIdResult result = new GetUserByIdResult(
            query.userId(),
            "john_doe",
            "john@example.com",
            "ACTIVE"
        );

        return Result.success("User retrieved successfully", result);
    }
}
```

**🔑 Key Patterns:**
- **@Service annotation** enables automatic discovery
- **Constructor injection** for dependencies
- **Validation logic** in the handler
- **Result pattern** for consistent responses

---

## 🌐 Step 5: Add Query Endpoint to Controller

### 5.1 Update MediatorController

**File:** `orchestration-layer/src/main/java/com/quizfun/orchestrationlayer/controller/MediatorController.java`

```java
// Add imports
import com.quizfun.orchestrationlayer.queries.GetUserByIdQuery;
import com.quizfun.orchestrationlayer.queries.GetUserByIdResult;

// Add method to existing controller
@GetMapping("/get-user/{userId}")
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

**💡 REST Convention:** Use GET for queries vs POST for commands.

---

## 🔨 Step 6: Build and Test

### 6.1 Fix Test Ambiguity (If Needed)

If you encounter compilation errors in tests due to method overloading ambiguity:

**File:** `global-shared-library/src/test/java/com/quizfun/globalshared/mediator/MediatorImplTest.java`

```java
// Change this ambiguous call:
Result<String> result = mediator.send(null);

// To this explicit call:
Result<String> result = mediator.send((ICommand<String>) null);
```

### 6.2 Build Project

```bash
# Build with coverage skip if needed
mvn clean install -Djacoco.skip=true

# Or fix coverage by adding query handler tests
mvn clean install
```

### 6.3 Start Application

```bash
mvn spring-boot:run -pl orchestration-layer
```

### 6.4 Test Commands and Queries

```bash
# Test existing command (should still work)
curl -X POST "http://localhost:8765/api/mediator/create-user?username=john&email=john@example.com"

# Expected: {"success":true,"message":"User created successfully","data":{"userId":6066,"username":"john","email":"john@example.com","status":"CREATED"}}

# Test new query functionality
curl "http://localhost:8765/api/mediator/get-user/123"

# Expected: {"success":true,"message":"User retrieved successfully","data":{"userId":123,"username":"john_doe","email":"john@example.com","status":"ACTIVE"}}

# Test query validation
curl "http://localhost:8765/api/mediator/get-user/0"

# Expected: {"success":false,"message":"Invalid user ID provided","data":null}
```

---

## ✨ Step 7: Adding More Queries (The Magic!)

### 7.1 Create New Query

```java
// Just create a new query record
public record GetUsersByStatusQuery(String status) implements IQuery<List<GetUsersByStatusResult>> {
}
```

### 7.2 Create New Handler

```java
// Add @Service - Spring Boot auto-discovers it!
@Service
public class GetUsersByStatusQueryHandler implements IQueryHandler<GetUsersByStatusQuery, List<GetUsersByStatusResult>> {
    @Override
    public Result<List<GetUsersByStatusResult>> handle(GetUsersByStatusQuery query) {
        // Your query logic here
        return Result.success(List.of(/* your results */));
    }
}
```

### 7.3 Use in Controller

```java
// No registration needed - mediator automatically routes!
@GetMapping("/users")
public ResponseEntity<Result<List<GetUsersByStatusResult>>> getUsersByStatus(@RequestParam String status) {
    GetUsersByStatusQuery query = new GetUsersByStatusQuery(status);
    return ResponseEntity.ok(mediator.send(query));
}
```

**✨ That's it! No manual registration required.**

---

## 🎯 Key Design Principles

### 1. **Perfect Pattern Consistency**
- Commands and queries follow identical patterns
- Same automatic registration mechanism
- Same error handling and result structure

### 2. **CQRS Separation**
- **Commands**: Mutate state (POST endpoints)
- **Queries**: Read data (GET endpoints)
- **Clear Intent**: Separate interfaces make purpose obvious

### 3. **Zero Configuration**
- Spring Boot automatic discovery via `@Service`
- Reflection-based type mapping
- No manual registration needed

### 4. **Type Safety**
- Compile-time checking of query→handler relationships
- Generic interfaces prevent runtime casting errors
- IDE support for refactoring and navigation

### 5. **Cross-Module Integration**
- Queries can use existing services (e.g., `UserValidationService`)
- Clean dependency injection across modules
- Maintains existing architecture patterns

---

## 🚀 Benefits of This Approach

### 1. **Developer Experience**
- **Familiar Pattern**: Mirrors existing command structure exactly
- **Auto-Discovery**: Just add `@Service` and it works
- **Type Safety**: Compile-time validation prevents errors

### 2. **Maintainability**
- **Consistent Structure**: All queries follow same pattern
- **Easy Testing**: Each handler is independently testable
- **Clear Separation**: Commands vs queries have distinct purposes

### 3. **Performance**
- **Optimized Queries**: Read-only operations can be optimized differently
- **Separate Handlers**: Query logic separated from command logic
- **Caching Ready**: Easy to add caching to query handlers

### 4. **Scalability**
- **Independent Evolution**: Queries can evolve separately from commands
- **Multiple Handlers**: Different queries can have different data sources
- **Read Replicas**: Query handlers can point to read-only databases

---

## 📊 Architecture Flow

```
HTTP GET Request → Controller → Mediator.send(query) → Query Handler Registry → Specific Query Handler → Business Logic → Result → Response
     ↓                ↓               ↓                       ↓                      ↓                ↓           ↓           ↓
   REST API      Query DTO      Auto-Discovery         Type Mapping          @Service        Data Access   Response DTO   JSON
```

---

## 🎯 Next Steps

1. **Add More Queries**: Follow the pattern to add pagination, filtering, sorting queries
2. **Add Validation Framework**: Create query validation attributes
3. **Add Caching**: Implement caching strategies for frequently accessed data
4. **Add Metrics**: Monitor query performance and usage patterns
5. **Add Authorization**: Implement query-level security checks

This extension provides a solid foundation for a comprehensive CQRS implementation while maintaining the simplicity and power of Spring Boot's dependency injection system.