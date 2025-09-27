## Solution: Fix Testcontainers MongoDB connection refused during mvn test

### Symptoms

- **Observed**: Intermittent ApplicationContext failures; logs show "MongoDBContainer should be started first" and connection refused to the container.
- **Modules affected**: `internal-layer/question-bank` tests using MongoDB via Testcontainers.

### Root cause

- **Conflicting lifecycle management**: The current version mixed a singleton container (`SingletonMongoContainer` with `withReuse(true)` and a shutdown hook), manual `start()` calls, and `@Container` in multiple places (config and tests). This caused race conditions where Spring created beans (e.g., `MongoClient`) before the container was guaranteed to be running.
- **Over-configuration**: Aggressive custom `MongoClientSettings` and ad-hoc readiness loops increased the chance of premature failures.

### Working fix (what we changed)

- **Single, simple container** owned by the test configuration class, no reuse:

```java
@Container
static MongoDBContainer mongoContainer = new MongoDBContainer("mongo:8.0")
        .withExposedPorts(27017)
        .withReuse(false);
```

- **Start container and register properties in one place** using `@DynamicPropertySource` (no extra waits, no manual logging):

```java
@DynamicPropertySource
static void configureProperties(DynamicPropertyRegistry registry) {
    mongoContainer.start();
    registry.add("spring.data.mongodb.uri", mongoContainer::getReplicaSetUrl);
    registry.add("spring.data.mongodb.database", () -> "quiz_cms_test");
}
```

- **Simplify client and add a small safety guard** to ensure the container is up before building the client:

```java
@Bean
@Primary
public MongoClient testMongoClient() {
    if (!mongoContainer.isRunning()) {
        mongoContainer.start();
    }
    return MongoClients.create(mongoContainer.getReplicaSetUrl());
}
```

- **Remove sources of conflict**:
  - Deleted `SingletonMongoContainer.java` and its shutdown hook.
  - Removed `@Container` declaration from tests that referenced the same container; tests now reference the container via the config only.
  - Dropped the custom `MongoClientSettings` and readiness loop.

### Why this works

- **Single lifecycle owner** prevents races: Spring initializes the context, `@DynamicPropertySource` starts the container and publishes the URI before any data-access beans are created.
- **No reuse by default** ensures a clean, predictable container each run, matching the previously working version’s behavior.

### Verification

- Ran focused failing tests: passed after the guard was added to `testMongoClient()`.
- Ran full module and multi-module builds:
  - `internal-layer/question-bank`: `mvn clean test` succeeded.
  - Workspace root: `mvn test` succeeded across modules.
- Remaining error logs in output are from tests that intentionally simulate repository failures and do not indicate infrastructure issues.

### Optional hardening (future)

- If you need faster builds with container reuse, enable it explicitly by adding `testcontainers.reuse.enable=true` to `~/.testcontainers.properties`, then you may opt-in to `.withReuse(true)` (remove any shutdown hooks and keep a single lifecycle owner).
- If CI environments are slow, you can add a longer startup wait with a log-based wait strategy; keep it in the configuration class, not tests.

### Files touched

- Updated: `internal-layer/question-bank/src/test/java/com/quizfun/questionbank/config/TestContainersConfig.java`
- Removed: `internal-layer/question-bank/src/test/java/com/quizfun/questionbank/config/SingletonMongoContainer.java`
- Updated (container reference only): `internal-layer/question-bank/src/test/java/com/quizfun/questionbank/config/TestContainersMongoDBConfigurationTest.java`


===============
# Addon, Why this Static method mongoContainer work?

