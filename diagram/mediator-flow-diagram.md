# Mediator Flow Diagram

This diagram illustrates the complete workflow of the mediator pattern implementation, from Spring Boot's auto-registration mechanism to the full request-response lifecycle.

## Complete Mediator Flow

```mermaid
flowchart TD
    %% Spring Boot Startup Phase
    A[Application Startup] --> B[Component Scanning]
    B --> C{Scan Base Packages}
    C --> |orchestrationlayer| D[Find Controllers]
    C --> |orchestrationlayer| E[Find Command Handlers]
    C --> |globalshared| F[Find Mediator Implementation]

    %% Auto-Registration Phase
    D --> G[Register Controllers as Beans]
    E --> H[Register Command Handlers as Beans]
    F --> I[Register MediatorImpl as Bean]

    G --> J[Spring IoC Container]
    H --> J
    I --> J

    %% Dependency Injection Phase
    J --> K[Inject IMediator into Controllers]
    J --> L[Inject Dependencies into Handlers]
    J --> M[Inject ApplicationContext into MediatorImpl]

    %% Runtime Request Flow
    N[HTTP Request] --> O[Spring Boot Web Server]
    O --> P[Controller Method]
    P --> Q[Create Command Object]
    Q --> R["Call mediator.send(command)"]

    %% Mediator Processing
    R --> S["MediatorImpl.send()"]
    S --> T[Get All Handler Beans]
    T --> U["applicationContext.getBeansOfType()"]
    U --> V[Handler Discovery Loop]

    %% Handler Matching Logic
    V --> W{For Each Handler}
    W --> X[Extract Generic Types via Reflection]
    X --> Y{Command Type Matches?}
    Y --> |No| W
    Y --> |Yes| Z[Found Matching Handler]

    %% Command Processing
    Z --> AA[Cast Handler to Correct Type]
    AA --> BB["Call handler.handle(command)"]
    BB --> CC[Execute Business Logic]
    CC --> DD[Validate Input via External Services]
    DD --> EE{Validation Success?}

    %% Result Generation
    EE --> |Yes| FF[Create Success Result]
    EE --> |No| GG[Create Failure Result]
    FF --> HH[Return Result to Mediator]
    GG --> HH

    %% Response Flow
    HH --> II[Return Result to Controller]
    II --> JJ[Convert Result to HTTP Response]
    JJ --> KK[Return HTTP Response]
    KK --> LL[Client Receives Response]

    %% Error Handling
    Y --> |No Handlers Found| MM[Throw IllegalArgumentException]
    BB --> |Handler Exception| NN[Exception Propagates]
    MM --> OO[Return 500 Error]
    NN --> OO

    %% Styling
    classDef startupPhase fill:#e1f5fe
    classDef registrationPhase fill:#f3e5f5
    classDef runtimePhase fill:#e8f5e8
    classDef errorPhase fill:#ffebee

    class A,B,C,D,E,F startupPhase
    class G,H,I,J,K,L,M registrationPhase
    class N,O,P,Q,R,S,T,U,V,W,X,Y,Z,AA,BB,CC,DD,EE,FF,GG,HH,II,JJ,KK,LL runtimePhase
    class MM,NN,OO errorPhase
```

## Detailed Flow Explanation

### 1. Spring Boot Startup Phase (Blue)
- **Component Scanning**: Spring Boot scans specified base packages
- **Bean Discovery**: Finds all `@Component`, `@Service`, `@Controller` annotated classes
- **Auto-Configuration**: Discovers mediator implementation and command handlers

### 2. Auto-Registration Phase (Purple)
- **Bean Registration**: Spring registers all discovered components in IoC container
- **Dependency Graph**: Builds dependency injection graph
- **Singleton Management**: Creates singleton instances of all beans

### 3. Runtime Request Phase (Green)
- **HTTP Request**: User sends request to REST endpoint
- **Controller Processing**: Spring routes request to appropriate controller method
- **Command Creation**: Controller creates command object with request data
- **Mediator Invocation**: Controller calls `mediator.send(command)`

### 4. Handler Discovery and Routing
- **Bean Retrieval**: `applicationContext.getBeansOfType(ICommandHandler.class)`
- **Type Extraction**: Uses reflection to extract generic type information
- **Handler Matching**: Finds handler that can process the specific command type
- **Type Safety**: Ensures compile-time type safety with runtime verification

### 5. Command Execution
- **Business Logic**: Handler executes domain-specific logic
- **External Services**: May call validation services or other dependencies
- **Result Creation**: Returns standardized `Result<T>` object

### 6. Response Generation
- **Result Processing**: Controller receives result from mediator
- **HTTP Mapping**: Converts result to appropriate HTTP status and body
- **Client Response**: Returns structured response to client

## Key Design Benefits

### Auto-Registration Magic
```mermaid
sequenceDiagram
    participant SB as Spring Boot
    participant AC as ApplicationContext
    participant M as MediatorImpl
    participant H as CommandHandler

    SB->>AC: Scan components
    AC->>H: Register as bean
    AC->>M: Inject ApplicationContext

    Note over M: Ready to auto-discover handlers

    M->>AC: getBeansOfType(ICommandHandler.class)
    AC->>M: Return all handler beans
    M->>M: Build type mapping via reflection
```

### Type-Safe Command Routing
```mermaid
sequenceDiagram
    participant C as Controller
    participant M as Mediator
    participant H as Handler

    C->>M: send(CreateUserCommand)
    M->>M: Extract command type
    M->>M: Find matching handler
    M->>H: handle(CreateUserCommand)
    H->>M: Result<CreateUserResult>
    M->>C: Result<CreateUserResult>
```

## Error Handling Scenarios

### No Handler Found
- **Trigger**: Command sent without corresponding handler
- **Response**: `IllegalArgumentException` with descriptive message
- **HTTP Result**: 500 Internal Server Error

### Handler Exception
- **Trigger**: Handler throws runtime exception
- **Response**: Exception propagates to controller
- **HTTP Result**: Depends on exception type and global exception handler

### Validation Failure
- **Trigger**: Business validation fails in handler
- **Response**: `Result.failure()` with error message
- **HTTP Result**: 400 Bad Request or appropriate business error code

## Performance Characteristics

- **Handler Discovery**: One-time cost during first request (cached afterward)
- **Type Resolution**: Reflection overhead minimal due to caching
- **Spring Benefits**: Leverages Spring's optimized bean management
- **Memory Efficiency**: Singleton pattern ensures single instance per handler type

## Integration Points

1. **Spring Security**: Can be applied at controller level
2. **Spring AOP**: Cross-cutting concerns like logging, validation
3. **Spring Profiles**: Environment-specific handler implementations
4. **Spring Boot Actuator**: Health checks and metrics
5. **Spring Data**: Database operations within handlers