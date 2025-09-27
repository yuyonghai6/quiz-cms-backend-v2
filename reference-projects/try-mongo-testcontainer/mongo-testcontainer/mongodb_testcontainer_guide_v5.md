# MongoDB TestContainers with Spring Boot - Guide v5

A comprehensive guide for reliable MongoDB integration testing with Spring Boot 3.x using TestContainers, featuring detailed explanations of every annotation and complex code pattern used.

## What changed from v4

- Added detailed explanations of every annotation used throughout the guide
- Included comprehensive breakdowns of complex code patterns
- Enhanced understanding of Spring Boot test slice vs integration testing
- Added MongoDB-specific annotation explanations
- Explained TestContainers lifecycle and container management concepts
- Added detailed explanations of Spring Data repository patterns

## Prerequisites

- **Java 21**: Required for modern Spring Boot 3.x features and pattern matching
- **Spring Boot 3.1+**: Minimum version for `@ServiceConnection` support (this project uses 3.5.x)
- **Docker running**: Verify with `docker info` - TestContainers needs Docker daemon access
- **Maven**: Verify with `./mvnw -v` - Maven Wrapper included in Spring Boot projects

## Dependencies Deep Dive

Understanding each dependency's role in the TestContainers ecosystem:

```xml
<dependencies>
    <!--
    spring-boot-starter-data-mongodb:
    - Provides MongoDB driver and Spring Data MongoDB
    - Includes MongoTemplate, repository support, and auto-configuration
    - Enables @Document, @Id, @Indexed annotations
    -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-mongodb</artifactId>
    </dependency>

    <!--
    spring-boot-starter-validation:
    - CRITICAL: Provides Jakarta Bean Validation (JSR-303/380) implementation
    - Enables @NotBlank, @Size, @Valid annotations
    - Often forgotten but essential for entity validation
    - Without this: "package jakarta.validation.constraints does not exist" error
    -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!--
    spring-boot-starter-test:
    - Includes JUnit 5, Mockito, AssertJ, Spring Test support
    - Provides @SpringBootTest, @MockBean, @TestConfiguration
    - Essential for any Spring Boot testing
    -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>

    <!--
    spring-boot-testcontainers:
    - NEW in Spring Boot 3.1+: Provides @ServiceConnection magic
    - Auto-configures database connections from TestContainers
    - Eliminates need for @DynamicPropertySource manual configuration
    -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-testcontainers</artifactId>
        <scope>test</scope>
    </dependency>

    <!--
    testcontainers:junit-jupiter:
    - Provides @Testcontainers and @Container annotations
    - Manages container lifecycle (start/stop) automatically
    - Integrates with JUnit 5 test lifecycle
    -->
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>

    <!--
    testcontainers:mongodb:
    - Provides MongoDBContainer class
    - Knows how to start/configure MongoDB containers
    - Includes proper wait strategies for MongoDB readiness
    -->
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>mongodb</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

## Base Integration Test - Annotation Deep Dive

```java
package com.example.demo.config;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * @SpringBootTest: Loads FULL Spring application context
 * - Starts entire Spring Boot application (like main method)
 * - Loads all @Component, @Service, @Repository beans
 * - Heavier than slice tests but provides complete integration
 * - Use for service layer tests, controller tests, full integration scenarios
 */
@SpringBootTest

/**
 * @Testcontainers: Enables TestContainers JUnit 5 extension
 * - Scans for @Container fields and manages their lifecycle
 * - Automatically starts containers before tests, stops after
 * - Handles cleanup and resource management
 * - Required on any class using @Container
 */
@Testcontainers

/**
 * @ActiveProfiles("test"): Activates Spring profile for testing
 * - Loads application-test.properties instead of application.properties
 * - Enables test-specific configuration (logging, database settings)
 * - CRITICAL: Without this, production config may be used in tests
 * - Can specify multiple profiles: @ActiveProfiles({"test", "integration"})
 */
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    /**
     * @Container: Marks field as TestContainers container
     * - TestContainers will manage this container's lifecycle
     * - Container starts before tests, stops after all tests complete
     * - Must be static for shared container across all test methods
     *
     * @ServiceConnection: NEW Spring Boot 3.1+ feature
     * - Automatically configures Spring Boot to connect to this container
     * - Eliminates need for @DynamicPropertySource manual configuration
     * - Spring Boot reads container's host/port and configures datasource
     * - Magic happens: no manual connection string configuration needed!
     *
     * static: Container shared across ALL test methods in this class
     * - More efficient: one container for entire test class
     * - Container survives between individual test methods
     * - Trade-off: tests must clean up their own data
     */
	@Container
	@ServiceConnection
	static MongoDBContainer mongoDBContainer = new MongoDBContainer(
        // DockerImageName.parse(): Type-safe way to specify Docker images
        // "mongo:8.0": Fixed version (not "latest") for reproducible tests
        DockerImageName.parse("mongo:8.0")
    );
}
```

### Why This Architecture Works

1. **Inheritance Pattern**: Service tests extend this class to get full Spring context + MongoDB
2. **Shared Container**: One MongoDB instance for all service tests (efficient)
3. **Full Integration**: All Spring beans available (services, repositories, configurations)
4. **Automatic Configuration**: `@ServiceConnection` handles all database connection setup

## Repository Slice Test - Annotation Deep Dive

```java
package com.example.demo.repository;

import com.example.demo.model.Question;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Arrays;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @DataMongoTest: Spring Boot TEST SLICE annotation
 * - Loads ONLY MongoDB-related components (repositories, MongoTemplate)
 * - Does NOT load @Service, @Controller, or other non-data components
 * - Much faster startup than @SpringBootTest
 * - Perfect for testing repository layer in isolation
 * - Automatically configures embedded/test MongoDB if no container provided
 *
 * Key difference from @SpringBootTest:
 * - @SpringBootTest = Full application context (heavy, complete)
 * - @DataMongoTest = Slice testing (light, focused)
 */
@DataMongoTest

/**
 * @Testcontainers: Same as before - manages container lifecycle
 * CRITICAL: Required here because we define local container
 */
@Testcontainers

/**
 * @ActiveProfiles("test"): Same as before - loads test configuration
 * CRITICAL: Ensures test database settings are used
 */
@ActiveProfiles("test")
class QuestionRepositoryTest {
    // NOTE: NO INHERITANCE from BaseIntegrationTest
    // This prevents bootstrap conflicts between @DataMongoTest and @SpringBootTest

    /**
     * Local container definition - WHY?
     * 1. @DataMongoTest + BaseIntegrationTest inheritance = BOOTSTRAP CONFLICT
     * 2. @DataMongoTest has its own test context bootstrapper
     * 3. BaseIntegrationTest brings @SpringBootTest bootstrapper
     * 4. Spring can't decide which bootstrapper to use = IllegalStateException
     *
     * Solution: Local container for slice tests, shared container for integration tests
     */
	@Container
	@ServiceConnection
	static MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:8.0"));

    /**
     * @Autowired: Spring dependency injection
     * - Only MongoDB-related beans available in @DataMongoTest slice
     * - QuestionRepository will be auto-implemented by Spring Data
     * - MongoTemplate would also be available if needed
     */
	@Autowired
	private QuestionRepository questionRepository;

    /**
     * @BeforeEach: JUnit 5 lifecycle annotation
     * - Runs before EACH test method
     * - Ensures clean state for every test
     * - Alternative: @Transactional + @Rollback (but MongoDB doesn't support transactions in all setups)
     */
	@BeforeEach
	void setUp() {
		questionRepository.deleteAll();  // Clean slate for each test
	}

    /**
     * @Test: JUnit 5 test method annotation
     * - Marks method as executable test
     * - Method must be non-private, non-static
     */
	@Test
	void shouldFindByAuthor() {
        // Test setup: Create test data
        // Arrays.asList(): Creates immutable list from varargs
        // Alternative: List.of() in Java 9+, but Arrays.asList() more compatible
		questionRepository.saveAll(Arrays.asList(
			new Question("Q1", "C1", "author1", Arrays.asList("t1")),
			new Question("Q2", "C2", "author2", Arrays.asList("t2")),
			new Question("Q3", "C3", "author1", Arrays.asList("t3"))
		));

        // Test execution: Use repository method
        // findByAuthor() will be auto-implemented by Spring Data
        // Spring Data MongoDB converts method name to MongoDB query
		List<Question> results = questionRepository.findByAuthor("author1");

        // Test verification: Use AssertJ for fluent assertions
        // assertThat() more readable than JUnit's assertEquals()
        // hasSize(2) checks collection size with clear error messages
		assertThat(results).hasSize(2);
	}
}
```

### Critical Architecture Decision: Why No Inheritance?

```java
// ❌ WRONG - Causes bootstrap conflict
@DataMongoTest
class QuestionRepositoryTest extends BaseIntegrationTest {
    // This creates multiple @BootstrapWith annotations:
    // 1. @DataMongoTest brings DataMongoTestContextBootstrapper
    // 2. BaseIntegrationTest brings SpringBootTestContextBootstrapper
    // Result: IllegalStateException - Spring can't choose
}

// ✅ CORRECT - Clean slice test
@DataMongoTest
@Testcontainers
@ActiveProfiles("test")
class QuestionRepositoryTest {
    @Container
    @ServiceConnection
    static MongoDBContainer mongoDBContainer = new MongoDBContainer(...);
    // Clean, focused, fast slice test
}
```

## Service Integration Test - Annotation Deep Dive

```java
package com.example.demo.service;

import com.example.demo.config.BaseIntegrationTest;
import com.example.demo.model.Question;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @ActiveProfiles("test"): Activates test profile
 * - Even though inherited from BaseIntegrationTest, explicit is better
 * - Ensures test configuration is loaded
 * - Good practice for clarity and maintainability
 */
@ActiveProfiles("test")
class QuestionServiceTest extends BaseIntegrationTest {
    // Inheritance from BaseIntegrationTest provides:
    // 1. @SpringBootTest - Full Spring context
    // 2. @Testcontainers - Container management
    // 3. @ServiceConnection MongoDBContainer - Database connectivity

    /**
     * @Autowired: Dependency injection in test
     * - Service layer testing requires full Spring context
     * - QuestionService depends on QuestionRepository
     * - Repository depends on MongoDB connection
     * - Full integration: Service -> Repository -> Database
     */
	@Autowired
	private QuestionService questionService;

	@BeforeEach
	void setUp() {
        // Service-level cleanup using business methods
        // More realistic than direct repository.deleteAll()
        // Tests service behavior, not just repository
		questionService.findAll().forEach(q -> questionService.deleteById(q.getId()));
	}

	@Test
	void shouldCreateAndRetrieveQuestion() {
        // Create domain object with realistic data
        // Tests business logic, validation, and persistence together
		Question q = new Question("Title", "Content more than ten chars", "author", Arrays.asList("t1"));

        // Test service method (not repository directly)
        // This exercises full integration: Service -> Repository -> MongoDB
        Question saved = questionService.save(q);

        // Verify business behavior
        // ID should be generated by MongoDB
        assertThat(saved.getId()).isNotNull();
	}
}
```

### Service vs Repository Testing Strategy

| Aspect | Repository Test (@DataMongoTest) | Service Test (@SpringBootTest) |
|--------|----------------------------------|-------------------------------|
| **Scope** | Data layer only | Full application integration |
| **Speed** | Fast (slice) | Slower (full context) |
| **Components** | Repositories, MongoTemplate | All beans (services, repositories, configs) |
| **Use For** | Query testing, data mapping | Business logic, validation, transactions |
| **Container** | Local per test class | Shared via BaseIntegrationTest |

## Test Properties Deep Dive

```properties
# MongoDB Test Configuration
# spring.data.mongodb.database: Specifies test database name
# - Keeps test data separate from any local dev database
# - Container creates this database automatically
spring.data.mongodb.database=question_test_db

# spring.data.mongodb.auto-index-creation: Creates indexes automatically
# - Enables @Indexed annotations in entities
# - Useful for testing index-dependent queries
# - In production, consider manual index management
spring.data.mongodb.auto-index-creation=true

# Logging configuration for tests
# logging.level.org.testcontainers=INFO: TestContainers lifecycle logging
# - Shows container start/stop events
# - Helpful for debugging container issues
logging.level.org.testcontainers=INFO

# logging.level.org.springframework.data.mongodb=DEBUG: MongoDB operation logging
# - Shows generated MongoDB queries
# - Useful for debugging query issues
# - Can be verbose, adjust as needed
logging.level.org.springframework.data.mongodb=DEBUG

# logging.level.com.example.demo=DEBUG: Application logging
# - Shows your application's debug messages
# - Useful for tracing test execution
logging.level.com.example.demo=DEBUG

# logging.level.org.springframework.boot.web=WARN: Reduce web noise
# - Suppresses HTTP/web-related startup messages
# - Tests don't need web server logging
logging.level.org.springframework.boot.web=WARN

# Suppress MongoDB driver monitor exceptions (CRITICAL for clean output)
# WHY: MongoDB driver runs background monitor threads
# During container shutdown, these threads log benign connection exceptions
# These are NOT test failures, just teardown noise
# Setting to ERROR suppresses this noise while keeping real errors visible
logging.level.org.mongodb.driver.client=ERROR
logging.level.org.mongodb.driver.cluster=ERROR
```

## Complex Code Pattern Explanations

### MongoDBContainer Configuration Deep Dive

```java
// Complex pattern breakdown:
@Container
@ServiceConnection
static MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:8.0"));

/**
 * DockerImageName.parse("mongo:8.0") explanation:
 *
 * DockerImageName: Type-safe wrapper around Docker image names
 * - Validates image name format
 * - Prevents typos in image names
 * - Better than raw strings
 *
 * "mongo:8.0": Fixed version specification
 * - NOT "mongo:latest" - versions can change unexpectedly
 * - "8.0" ensures consistent behavior across environments
 * - Reproducible builds and tests
 *
 * Alternative patterns:
 * DockerImageName.parse("mongo").withTag("8.0")  // Same result
 * DockerImageName.parse("mongo:8.0-jammy")       // Specific OS variant
 */

/**
 * Container lifecycle with static:
 *
 * static: ONE container per test CLASS (not per method)
 * - Container starts before first test method
 * - Survives between test methods
 * - Stops after last test method
 * - More efficient than per-method containers
 *
 * Trade-offs:
 * ✅ Faster: Avoid container restart overhead
 * ✅ Realistic: Simulates persistent database
 * ⚠️ Cleanup required: Tests must clean their own data
 * ⚠️ Isolation: Tests can affect each other if cleanup missing
 */
```

### Spring Data Repository Method Name Translation

```java
// Spring Data MongoDB auto-implements these methods:
List<Question> findByAuthor(String author);
List<Question> findByTitleContainingIgnoreCase(String title);

/**
 * Method name -> MongoDB query translation:
 *
 * findByAuthor(String author)
 * ├── "findBy" -> SELECT operation
 * ├── "Author" -> Field name (question.author)
 * └── Parameter -> Query value
 * Result: db.questions.find({"author": "parameterValue"})
 *
 * findByTitleContainingIgnoreCase(String title)
 * ├── "findBy" -> SELECT operation
 * ├── "Title" -> Field name (question.title)
 * ├── "Containing" -> Partial match ($regex)
 * ├── "IgnoreCase" -> Case insensitive ($options: "i")
 * └── Parameter -> Query value
 * Result: db.questions.find({"title": {$regex: "parameterValue", $options: "i"}})
 */
```

### Custom Query Patterns

```java
// Complex custom query explanation:
@Query("{'tags': { $in: [?0] }}")
List<Question> findByTagsContaining(String tag);

/**
 * @Query annotation breakdown:
 *
 * "{'tags': { $in: [?0] }}": Raw MongoDB query
 * ├── 'tags': Field name (use quotes for safety)
 * ├── $in: MongoDB operator (element in array)
 * ├── [?0]: Parameter placeholder array
 * │   ├── ?: Parameter placeholder prefix
 * │   ├── 0: First parameter index (zero-based)
 * │   └── []: Array notation for $in operator
 * └── Result: Finds documents where tags array contains the parameter value
 *
 * Alternative Spring Data method (auto-generated):
 * List<Question> findByTagsContaining(String tag);  // Same result, no @Query needed
 *
 * When to use @Query:
 * ✅ Complex queries Spring Data can't generate
 * ✅ Performance-critical queries needing optimization
 * ✅ MongoDB-specific operators not supported by method names
 * ❌ Simple queries Spring Data can auto-generate
 */
```

## Comprehensive Bug Traps and Solutions

### 🚨 **Critical Compilation Errors**

#### Error 1: Missing Validation Dependencies
```
ERROR: package jakarta.validation.constraints does not exist
ERROR: cannot find symbol: class NotBlank
ERROR: cannot find symbol: class Size
```

**Root Cause**: Missing `spring-boot-starter-validation` dependency

**Why This Happens**:
- Validation annotations (`@NotBlank`, `@Size`) are NOT part of Spring Boot core
- They're part of Jakarta Bean Validation (JSR-380)
- Spring Boot 3.x uses Jakarta namespace (not javax)
- Without the starter, validation annotations are unavailable at compile time

**Solution**: Add validation starter to pom.xml:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

### 🚨 **Critical Runtime Bootstrap Conflicts**

#### Error 2: Multiple @BootstrapWith Detected
```
java.lang.IllegalStateException: Configuration error: found multiple declarations of @BootstrapWith for test class [QuestionRepositoryTest]:
[@org.springframework.test.context.BootstrapWith(value=org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTestContextBootstrapper.class),
@org.springframework.test.context.BootstrapWith(value=org.springframework.boot.test.context.SpringBootTestContextBootstrapper.class)]
```

**Root Cause Deep Dive**:
- `@DataMongoTest` internally uses `@BootstrapWith(DataMongoTestContextBootstrapper.class)`
- `@SpringBootTest` (in BaseIntegrationTest) uses `@BootstrapWith(SpringBootTestContextBootstrapper.class)`
- When repository test extends BaseIntegrationTest, both annotations are present
- Spring Test Framework can't decide which bootstrapper to use
- Result: IllegalStateException at test startup

**Architecture Understanding**:
```java
// What's happening under the hood:

@DataMongoTest  // This annotation includes:
@BootstrapWith(DataMongoTestContextBootstrapper.class)  // Slice test context
class QuestionRepositoryTest extends BaseIntegrationTest {  // This brings:
    // @SpringBootTest -> @BootstrapWith(SpringBootTestContextBootstrapper.class)
    // CONFLICT: Two different bootstrappers!
}
```

**WRONG Pattern** ❌:
```java
@DataMongoTest
@Import(BaseIntegrationTest.class)  // NEVER DO THIS - imports @SpringBootTest context
class QuestionRepositoryTest extends BaseIntegrationTest {  // NEVER DO THIS - brings @SpringBootTest
    // Double bootstrap conflict
}
```

**CORRECT Pattern** ✅:
```java
@DataMongoTest          // Slice test bootstrapper
@Testcontainers         // Container management
@ActiveProfiles("test") // Test configuration
class QuestionRepositoryTest {  // NO INHERITANCE - clean slice test

    @Container
    @ServiceConnection
    static MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:8.0"));
    // Local container avoids bootstrap conflicts
}
```

#### Error 3: Test Suite Timeouts and Container Conflicts
**Symptoms**:
- Individual test classes pass perfectly: `./mvnw test -Dtest="QuestionRepositoryTest"` ✅
- Full suite hangs/times out: `./mvnw test` ❌
- Container connection refused errors
- MongoDB socket read exceptions during startup

**Root Cause Analysis**:
- Repository test and Service test both try to start MongoDB containers
- Container port conflicts when running simultaneously
- Shared BaseIntegrationTest container + local slice test container = resource contention
- Docker resource limits reached with multiple containers

**Solution Architecture**:
```java
// Correct pattern - each test type gets its own container strategy:

// Repository tests (slice): Fast, local containers
@DataMongoTest
class QuestionRepositoryTest {
    @Container @ServiceConnection
    static MongoDBContainer mongoDBContainer = new MongoDBContainer(...);
    // Isolated, lightweight, fast
}

// Service tests (integration): Shared containers via inheritance
@ActiveProfiles("test")
class QuestionServiceTest extends BaseIntegrationTest {
    // Inherits shared container from BaseIntegrationTest
    // Full Spring context with all dependencies
}
```

### 🚨 **Container Configuration Issues**

#### Error 4: Unstable Test Results
```
Container connection inconsistencies between test runs
Different behavior in CI vs local environment
Flaky tests that sometimes pass, sometimes fail
```

**Root Cause**: Using `mongo:latest` instead of fixed version

**Why `latest` is problematic**:
- `latest` tag can point to different versions over time
- Local Docker cache might have different `latest` than CI
- MongoDB behavior changes between major versions
- Debugging becomes impossible when version is unknown

**Solution**: Always use fixed MongoDB versions:
```java
// BAD ❌ - Unpredictable behavior
new MongoDBContainer(DockerImageName.parse("mongo:latest"))
// Could be 6.0, 8.0, 8.0 depending on when image was pulled

// GOOD ✅ - Predictable, reproducible
new MongoDBContainer(DockerImageName.parse("mongo:8.0"))
// Always MongoDB 8.0, consistent across all environments
```

#### Error 5: Container Reuse Warnings
```
WARN: Reuse was requested but the environment does not support the reuse of containers
TestContainers will ignore the reuse request and create a new container
```

**Root Cause**: Using `.withReuse(true)` without proper global configuration

**Container Reuse Deep Dive**:
- `withReuse(true)` tells TestContainers to reuse containers between test runs
- Requires global configuration in user's home directory
- Without global config, TestContainers ignores reuse request
- Can cause confusion and warnings

**Solutions**:
1. **Remove reuse** (recommended for most cases):
```java
// Clean approach - no reuse complications
static MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:8.0"));
// TestContainers handles lifecycle properly
```

2. **Or configure global reuse** in `~/.testcontainers.properties`:
```properties
testcontainers.reuse.enable=true
# Enables reuse globally for faster development iterations
```

**When to use reuse**:
- ✅ Local development with frequent test runs
- ✅ Developer machines where startup time matters
- ❌ CI/CD environments (isolation more important)
- ❌ Production-like testing (clean state required)

### 🚨 **Test Profile and Configuration Issues**

#### Error 6: Test Properties Not Applied
**Symptoms**:
- Default database used instead of `question_test_db`
- Production logging levels appearing in tests
- Test-specific configuration completely ignored
- Connection to wrong MongoDB instance

**Root Cause**: Missing `@ActiveProfiles("test")` annotation

**Spring Profiles Deep Dive**:
```java
// Without @ActiveProfiles("test"):
// ├── application.properties loaded (production config)
// ├── Logging: production levels
// ├── Database: default settings
// └── Result: Tests run against wrong configuration

// With @ActiveProfiles("test"):
// ├── application.properties loaded (base config)
// ├── application-test.properties loaded (overrides)
// ├── Logging: test-specific levels
// ├── Database: test-specific settings
// └── Result: Tests run with proper test configuration
```

**Solution**: Always add profile activation:
```java
@ActiveProfiles("test")  // CRITICAL - activates test profile
class QuestionServiceTest extends BaseIntegrationTest {
    // Now application-test.properties will be loaded
    // Test-specific database, logging, and other configs active
}
```

#### Error 7: Noisy MongoDB Driver Logs
**Symptoms**:
```
MongoSocketReadException: Prematurely reached end of stream
MongoSocketReadException: Exception receiving message
Connection reset exceptions during test shutdown
java.net.SocketException: Connection reset
```

**Root Cause Analysis**:
- MongoDB Java driver runs background monitor threads
- These threads periodically check server health
- During TestContainers shutdown, container stops abruptly
- Monitor threads encounter closed connections and log exceptions
- These are NOT test failures - just shutdown noise

**Why This Happens**:
```java
// TestContainers lifecycle:
// 1. Container starts -> MongoDB ready
// 2. Tests run -> Driver connects, starts monitor threads
// 3. Tests complete -> TestContainers stops container immediately
// 4. Monitor threads still running -> Try to connect to stopped container
// 5. Connection refused -> Exception logged (but tests already passed)
```

**Solution**: Add driver logging suppression in test properties:
```properties
# Suppress MongoDB driver monitor exceptions (noise during container shutdown)
# This ONLY affects logging, not actual test execution
# Real errors (during test execution) still appear as test failures
logging.level.org.mongodb.driver.client=ERROR
logging.level.org.mongodb.driver.cluster=ERROR
```

### 🚨 **Advanced Runtime Issues**

#### Error 8: Auditing Not Working
**Symptoms**:
- `@CreatedDate` and `@LastModifiedDate` fields remain null
- Timestamp fields not automatically populated
- Manual timestamp setting works, but annotations don't

**Root Cause**: Missing MongoDB auditing configuration

**MongoDB Auditing Deep Dive**:
```java
// Entity with auditing annotations:
@Document(collection = "questions")
public class Question {
    @CreatedDate
    private LocalDateTime createdAt;    // Expects automatic population

    @LastModifiedDate
    private LocalDateTime updatedAt;    // Expects automatic population
}

// Without @EnableMongoAuditing:
// ├── Spring Data MongoDB ignores auditing annotations
// ├── No automatic timestamp population
// ├── Fields remain null despite annotations
// └── No error thrown - silent failure

// With @EnableMongoAuditing:
// ├── Spring Data MongoDB registers auditing infrastructure
// ├── Intercepts save operations
// ├── Populates timestamp fields automatically
// └── @CreatedDate and @LastModifiedDate work as expected
```

**Solution**: Enable auditing in configuration:
```java
@Configuration
@EnableMongoAuditing  // Enables auditing infrastructure
public class MongoConfig {
    // This configuration bean enables:
    // - @CreatedDate automatic population
    // - @LastModifiedDate automatic updates
    // - Integration with Spring Data lifecycle events
}
```

#### Error 9: Custom Queries Failing
**Symptoms**:
- Repository methods with `@Query` annotation not working
- Complex MongoDB queries returning unexpected results
- Syntax errors in custom queries

**Common Query Issues and Solutions**:
```java
// Issue 1: Improper JSON formatting
@Query("{tags: { $in: [?0] }}")    // ❌ May fail - missing quotes
@Query("{'tags': { $in: [?0] }}")  // ✅ Correct - quoted field names

// Issue 2: Parameter binding errors
@Query("{'tags': ?0}")             // ❌ Wrong - expects array but gets string
@Query("{'tags': { $in: [?0] }}")  // ✅ Correct - wraps parameter in array

// Issue 3: Case sensitivity in regex
@Query("{'title': { $regex: ?0 }}")                    // ❌ Case sensitive
@Query("{'title': { $regex: ?0, $options: 'i' }}")    // ✅ Case insensitive

// Issue 4: Complex queries better as method names
@Query("{'author': ?0}")                               // ❌ Unnecessary @Query
List<Question> findByAuthor(String author);           // ✅ Spring Data auto-generates

// When to use @Query vs method names:
// ✅ Use @Query for: Complex aggregations, MongoDB-specific operators, performance tuning
// ✅ Use method names for: Simple queries, standard operations, readability
```

## Advanced Patterns and Best Practices

### Container Lifecycle Management

```java
// Static vs Non-static containers:

// Static (Shared) - RECOMMENDED for most cases:
@Container
@ServiceConnection
static MongoDBContainer mongoDBContainer = new MongoDBContainer(...);
/**
 * Lifecycle: One container per test CLASS
 * ├── Before first test method: Container starts
 * ├── Between test methods: Container survives
 * ├── After last test method: Container stops
 * ├── Performance: Faster (no restart overhead)
 * └── Isolation: Tests must clean their own data
 */

// Non-static (Per-method) - Use only when necessary:
@Container
@ServiceConnection
MongoDBContainer mongoDBContainer = new MongoDBContainer(...);
/**
 * Lifecycle: One container per test METHOD
 * ├── Before each test method: Fresh container starts
 * ├── After each test method: Container stops and removed
 * ├── Performance: Slower (restart overhead)
 * └── Isolation: Perfect (each test gets fresh database)
 */
```

### Test Data Management Strategies

```java
// Strategy 1: Repository-level cleanup (slice tests)
@BeforeEach
void setUp() {
    questionRepository.deleteAll();  // Fast, direct database operation
}

// Strategy 2: Service-level cleanup (integration tests)
@BeforeEach
void setUp() {
    questionService.findAll().forEach(q -> questionService.deleteById(q.getId()));
    // Tests business logic, includes validation and business rules
}

// Strategy 3: Transactional rollback (if supported)
@Test
@Transactional
@Rollback
void shouldTestSomething() {
    // Changes rolled back after test
    // Note: MongoDB transactions require replica set or sharded cluster
}
```

## Performance Optimization Deep Dive

### Test Execution Speed Analysis

| Test Type | Startup Time | Container Strategy | Use Case |
|-----------|-------------|-------------------|-----------|
| **@DataMongoTest** | ~2-3 seconds | Local, static | Repository testing, query validation |
| **@SpringBootTest** | ~5-8 seconds | Shared via base class | Service integration, business logic |
| **Full Suite** | ~10-15 seconds | Multiple containers | Complete application validation |

### Memory and Resource Management

```java
// Efficient container configuration:
@Container
@ServiceConnection
static MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:8.0"))
    // Optional optimizations:
    .withStartupTimeout(Duration.ofSeconds(60))    // Prevent hanging on slow systems
    .withStartupAttempts(1);                       // Fail fast instead of retrying

// Resource cleanup in CI environments:
@AfterAll
static void tearDown() {
    // Optional explicit cleanup (TestContainers handles this automatically)
    if (mongoDBContainer != null && mongoDBContainer.isRunning()) {
        mongoDBContainer.stop();
    }
}
```

## Do's and Don'ts Summary

### ✅ **Critical Do's**

1. **Architecture Separation**:
   - Use `@DataMongoTest` for repository slice tests with local containers
   - Use `@SpringBootTest` via `BaseIntegrationTest` for service integration tests
   - Never mix slice and integration test annotations

2. **Container Management**:
   - Use fixed MongoDB versions (`mongo:8.0`) not `latest`
   - Define containers locally in slice tests, shared in base class for integration tests
   - Avoid `.withReuse(true)` unless properly configured globally

3. **Configuration**:
   - Always include `@ActiveProfiles("test")`
   - Add validation dependencies for entity annotations
   - Enable MongoDB auditing if using `@CreatedDate`/`@LastModifiedDate`

4. **Logging**:
   - Suppress MongoDB driver monitor logs in test properties
   - Use appropriate logging levels for debugging vs clean output

### ❌ **Critical Don'ts**

1. **Never extend `BaseIntegrationTest` in `@DataMongoTest` classes** - causes bootstrap conflicts
2. **Never use `mongo:latest` in tests** - always use fixed versions for reproducibility
3. **Never forget `spring-boot-starter-validation`** if using validation annotations
4. **Never mix `@Import(BaseIntegrationTest.class)` with slice tests** - creates configuration conflicts
5. **Never assume test properties apply** without `@ActiveProfiles("test")`

## Testing Commands and Workflow

```bash
# Development workflow:

# 1. Test individual components during development
./mvnw test -Dtest="QuestionRepositoryTest"    # Fast repository slice tests
./mvnw test -Dtest="QuestionServiceTest"       # Service integration tests

# 2. Run full suite before commit
./mvnw test                                    # All tests with proper isolation

# 3. Clean build for CI/deployment
./mvnw clean test                             # Fresh build with all tests

# 4. Debug specific issues
./mvnw test -Dtest="QuestionRepositoryTest#shouldFindByAuthor" -X  # Verbose output
```

## Success Criteria

After implementing this guide correctly:
- ✅ `./mvnw test` completes successfully with all tests passing (10/10 tests)
- ✅ No bootstrap configuration conflicts or IllegalStateException errors
- ✅ Clean test output without excessive MongoDB driver noise
- ✅ Fast repository slice tests (~2-3s) and comprehensive service integration tests (~5-8s)
- ✅ Reproducible results across different environments (local, CI, different developers)
- ✅ Clear separation between slice tests (focused, fast) and integration tests (comprehensive, realistic)

This guide provides complete understanding of every annotation, pattern, and decision in MongoDB TestContainers implementation with Spring Boot 3.x, ensuring both immediate success and long-term maintainability.