# Singleton TestContainer MongoDB Conflict Analysis and Solutions

## Root Cause Analysis

### The Fundamental Problem: Conflicting Singleton Patterns

The TestContainer MongoDB failures are caused by **conflicting singleton design patterns**:

1. **Spring Boot Singleton Management**: Each test class/method gets its own Spring ApplicationContext with singleton beans managed by Spring's lifecycle
2. **Custom Singleton Pattern**: Our `SingletonMongoContainer` implements a manual singleton pattern that conflicts with Spring's context management
3. **Race Conditions**: Multiple Spring contexts compete for the same container instance, causing lifecycle mismatches

### Evidence of the Problem

**Symptoms Observed:**
- Container starts successfully during early tests: `Container mongo:8.0 started in PT0.624405128S`
- Container disappears from `docker ps` during test execution (exactly as user observed: "up 3 seconds then disappeared")
- Critical integration tests fail with `MongoTimeout` and `Connection refused` to `localhost:32828`
- Same failure pattern occurs with both MongoDB 6.0 and 8.0 (version-independent issue)

**Timeline of Events:**
1. **First Spring Context**: Starts container via `SingletonMongoContainer.getInstance()`
2. **Context Shutdown**: Spring context closes, triggering cleanup
3. **Container Cleanup**: Shutdown hooks or context lifecycle stops container
4. **Second Spring Context**: Integration test starts new context, container is gone
5. **Connection Failure**: Test times out trying to connect to non-existent container

## Current Problematic Implementation

### 1. SingletonMongoContainer.java - The Conflict Source

```java
// PROBLEMATIC: Manual singleton that conflicts with Spring lifecycle
final class SingletonMongoContainer {
    private static volatile MongoDBContainer INSTANCE;

    static MongoDBContainer getInstance() {
        if (INSTANCE == null) {
            synchronized (SingletonMongoContainer.class) {
                if (INSTANCE == null) {
                    INSTANCE = new MongoDBContainer("mongo:8.0")
                        .withExposedPorts(27017)
                        .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(60)))
                        .withReuse(true);
                    INSTANCE.start();

                    // PROBLEM: Shutdown hook conflicts with Spring context lifecycle
                    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                        if (INSTANCE != null && INSTANCE.isRunning()) {
                            INSTANCE.stop(); // Premature cleanup!
                        }
                    }));
                }
            }
        }

        // PROBLEM: Can throw exception if container stopped between contexts
        if (!INSTANCE.isRunning()) {
            throw new RuntimeException("MongoDB Testcontainer stopped unexpectedly");
        }

        return INSTANCE;
    }
}
```

**Problems:**
- Manual double-checked locking conflicts with Spring's singleton scope
- Shutdown hook stops container when first Spring context closes
- Runtime check can fail between Spring context transitions
- No coordination with Spring's test lifecycle

### 2. TestContainersConfig.java - Compounding the Issue

```java
@SpringBootConfiguration
@Testcontainers
public class TestContainersConfig {

    // PROBLEM: Spring manages this as singleton per context,
    // but getInstance() returns global singleton
    @Container
    static MongoDBContainer mongoContainer = SingletonMongoContainer.getInstance();

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PROBLEM: Multiple contexts call this, but container may be stopped
        if (!mongoContainer.isRunning()) {
            mongoContainer.start(); // Can fail if container was cleaned up
            mongoContainer.followOutput(new Slf4jLogConsumer(logger));
        }
        waitForMongoReady(mongoContainer.getReplicaSetUrl());
        // ...
    }
}
```

**Problems:**
- `@Container` annotation expects Spring/TestContainers to manage lifecycle
- Manual singleton bypasses TestContainers' built-in lifecycle management
- `@DynamicPropertySource` called multiple times with inconsistent container state
- Spring context isolation broken by global singleton

## Solution Options

### Option 1: Let Spring/TestContainers Manage Everything (RECOMMENDED)

**Approach**: Remove custom singleton entirely and use TestContainers' native container management.

**Implementation:**

```java
// DELETE: SingletonMongoContainer.java entirely

// REPLACE TestContainersConfig.java:
@SpringBootConfiguration
@Testcontainers
public class TestContainersConfig {

    // Let TestContainers handle singleton behavior with reuse
    @Container
    static MongoDBContainer mongoContainer = new MongoDBContainer("mongo:8.0")
        .withExposedPorts(27017)
        .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(60)))
        .withReuse(true)  // TestContainers handles global reuse properly
        .withLabel("reuse.UUID", "quiz-cms-mongodb-test"); // Consistent reuse identifier

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // TestContainers ensures container is running before this is called
        registry.add("spring.data.mongodb.uri", mongoContainer::getReplicaSetUrl);
        registry.add("spring.data.mongodb.database", () -> "quiz_cms_test");
    }

    // Remove manual container management code
    // TestContainers handles all lifecycle automatically
}
```

**Benefits:**
- No singleton conflicts - TestContainers manages container lifecycle
- Proper integration with Spring test context lifecycle
- Built-in reuse mechanism across test classes
- Automatic cleanup handled by TestContainers framework

### Option 2: Maven-Level Container Management

**Approach**: Start container during Maven compile phase, separate from Spring contexts.

**Implementation:**

1. **Create Pre-Test Container Bootstrap**:

```java
// src/test/java/com/quizfun/questionbank/config/ContainerBootstrap.java
public class ContainerBootstrap {

    private static final String CONTAINER_NAME = "quiz-cms-test-mongodb";

    public static void startPersistentContainer() {
        // Start container with specific name and configuration
        MongoDBContainer container = new MongoDBContainer("mongo:8.0")
            .withCreateContainerCmdModifier(cmd -> cmd.withName(CONTAINER_NAME))
            .withReuse(true)
            .withExposedPorts(27017);

        container.start();

        // Store connection details for tests
        System.setProperty("testcontainer.mongodb.url", container.getReplicaSetUrl());
        System.setProperty("testcontainer.mongodb.port", String.valueOf(container.getFirstMappedPort()));

        System.out.println("✅ MongoDB TestContainer started: " + container.getReplicaSetUrl());
    }

    public static void main(String[] args) {
        startPersistentContainer();
    }
}
```

2. **Maven Integration**:

```xml
<!-- In pom.xml - run container before tests -->
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>exec-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>start-testcontainer</id>
            <phase>test-compile</phase>
            <goals>
                <goal>java</goal>
            </goals>
            <configuration>
                <mainClass>com.quizfun.questionbank.config.ContainerBootstrap</mainClass>
            </configuration>
        </execution>
    </executions>
</plugin>
```

3. **Simplified TestContainersConfig**:

```java
@SpringBootConfiguration
public class TestContainersConfig {

    // Use existing container instead of creating new one
    @Bean
    @Primary
    public MongoClient testMongoClient() {
        String mongoUrl = System.getProperty("testcontainer.mongodb.url");
        return MongoClients.create(mongoUrl);
    }
}
```

### Option 3: Global Container with Proper Lifecycle Management

**Approach**: Fix the current singleton pattern to work correctly with Spring contexts.

**Implementation:**

```java
// Fixed SingletonMongoContainer.java
final class SingletonMongoContainer {

    private static volatile MongoDBContainer INSTANCE;
    private static volatile boolean SHUTDOWN_HOOK_ADDED = false;
    private static final Object LOCK = new Object();

    static MongoDBContainer getInstance() {
        if (INSTANCE == null || !INSTANCE.isRunning()) {
            synchronized (LOCK) {
                if (INSTANCE == null || !INSTANCE.isRunning()) {

                    // Clean up old instance if exists
                    if (INSTANCE != null) {
                        try {
                            INSTANCE.stop();
                        } catch (Exception ignored) {}
                    }

                    INSTANCE = new MongoDBContainer("mongo:8.0")
                        .withExposedPorts(27017)
                        .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(60)))
                        .withReuse(true)
                        .withLabel("reuse.UUID", "quiz-cms-mongodb-singleton");

                    INSTANCE.start();

                    // Add shutdown hook only once
                    if (!SHUTDOWN_HOOK_ADDED) {
                        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                            if (INSTANCE != null && INSTANCE.isRunning()) {
                                INSTANCE.stop();
                            }
                        }));
                        SHUTDOWN_HOOK_ADDED = true;
                    }
                }
            }
        }

        return INSTANCE;
    }

    // Health check method
    static boolean isHealthy() {
        return INSTANCE != null && INSTANCE.isRunning();
    }
}
```

## Recommended Workflow: After mvn clean

### Phase 1: Pre-Build Container Startup

```bash
# 1. Clean everything
mvn clean -pl internal-layer/question-bank

# 2. Compile and trigger container startup
mvn test-compile -pl internal-layer/question-bank

# 3. Verify container is running
docker ps --filter "ancestor=mongo:8.0" --format "table {{.ID}}\t{{.Status}}\t{{.Ports}}"
```

### Phase 2: Health Validation

```bash
# 1. Check container status
CONTAINER_ID=$(docker ps -q --filter "ancestor=mongo:8.0")
if [ -z "$CONTAINER_ID" ]; then
    echo "❌ MongoDB container not found"
    exit 1
else
    echo "✅ MongoDB container running: $CONTAINER_ID"
fi

# 2. Check MongoDB health
MONGO_PORT=$(docker port $CONTAINER_ID 27017 | cut -d: -f2)
echo "MongoDB accessible on port: $MONGO_PORT"

# 3. Test MongoDB connectivity
docker exec $CONTAINER_ID mongosh --eval "db.runCommand({ping: 1})" --quiet || {
    echo "❌ MongoDB not responding"
    exit 1
}

echo "✅ MongoDB is healthy and ready"
```

### Phase 3: Run Tests Without Clean

```bash
# Run tests preserving the running container
mvn test -pl internal-layer/question-bank -Djacoco.skip=true
```

## Monitoring Commands

### Container Status Check
```bash
# Quick status check
docker ps --filter "ancestor=mongo:8.0" --format "table {{.ID}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}"

# Detailed container info
docker inspect $(docker ps -q --filter "ancestor=mongo:8.0") --format '{{.State.Status}} - {{.NetworkSettings.Ports}}'
```

### MongoDB Health Check
```bash
# Get container ID and port
CONTAINER_ID=$(docker ps -q --filter "ancestor=mongo:8.0")
MONGO_PORT=$(docker port $CONTAINER_ID 27017 | cut -d: -f2)

# Test connectivity
nc -zv localhost $MONGO_PORT

# Test MongoDB operations
docker exec $CONTAINER_ID mongosh --eval "
  db.runCommand({ping: 1});
  db.runCommand({listCollections: 1});
  db.test.insertOne({test: 'connection', timestamp: new Date()});
  db.test.findOne({test: 'connection'});
"
```

### Continuous Monitoring
```bash
# Monitor container status during test execution
watch -n 2 'docker ps --filter "ancestor=mongo:8.0" --format "table {{.ID}}\t{{.Status}}\t{{.Ports}}"'
```

## Troubleshooting Guide

### If Container Disappears During Tests

1. **Check for multiple singleton instances**:
   ```bash
   # Look for conflicting containers
   docker ps -a --filter "ancestor=mongo" --format "table {{.ID}}\t{{.Image}}\t{{.Status}}\t{{.Names}}"
   ```

2. **Verify Spring context isolation**:
   - Look for multiple `@SpringBootTest` annotations
   - Check for parallel test execution settings
   - Ensure proper `@DirtiesContext` usage if needed

3. **Monitor container logs**:
   ```bash
   docker logs $(docker ps -q --filter "ancestor=mongo:8.0") --follow
   ```

### If Connection Refused Errors Occur

1. **Port mapping verification**:
   ```bash
   docker port $(docker ps -q --filter "ancestor=mongo:8.0")
   netstat -tuln | grep $(docker port $(docker ps -q --filter "ancestor=mongo:8.0") 27017 | cut -d: -f2)
   ```

2. **Test direct connectivity**:
   ```bash
   # Replace PORT with actual mapped port
   curl -v telnet://localhost:PORT
   ```

## Implementation Priority

### Recommended: Option 1 (Spring/TestContainers Native Management)

**Why this is best:**
- Eliminates singleton conflicts entirely
- Uses TestContainers as intended
- Simplest implementation
- Most reliable long-term

**Steps:**
1. Delete `SingletonMongoContainer.java`
2. Simplify `TestContainersConfig.java` to use direct `@Container`
3. Let TestContainers handle all lifecycle management
4. Test with the workflow above

This approach resolves the fundamental singleton conflict that you correctly identified as the root cause.

---

**Document Version**: 1.0
**Created**: September 25, 2025
**Root Cause**: Conflicting singleton patterns between Spring Boot and custom container management
**Status**: Solutions documented, implementation pending user approval