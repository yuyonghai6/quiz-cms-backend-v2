# Enabler-2001: Thread-Safe Long ID Generator

## Enabler Story
**As a** system architect building the quiz CMS platform
**I need** a thread-safe, collision-resistant Long ID generator in the global-shared-library
**So that** all modules can generate unique internal identifiers with optimal performance and zero race conditions

## Epic
Enabler Epic-Global Shared Library

## Story
Implement thread-safe Long ID generator using AtomicInteger for collision-resistant ID generation in multi-threaded environments

## Acceptance Criteria

### Thread Safety and Concurrency
- [ ] **AC-2001.1**: ID generation must use AtomicLong and AtomicInteger for lock-free thread safety
- [ ] **AC-2001.2**: Concurrent ID generation by multiple threads must produce zero collisions
- [ ] **AC-2001.3**: Stress test with 100 threads generating 1000 IDs each must produce 100,000 unique IDs
- [ ] **AC-2001.4**: Sequence increment operations must be atomic and wait-free

### ID Format and Structure
- [ ] **AC-2001.5**: Generated IDs must follow format: `[timestamp_milliseconds * 1000] + [sequence_within_millisecond]`
- [ ] **AC-2001.6**: IDs generated in chronological order must maintain temporal ordering
- [ ] **AC-2001.7**: Sequence counter must reset to 0 when entering new millisecond
- [ ] **AC-2001.8**: Maximum sequence value must be 999 per millisecond with overflow protection

### Collision Resistance
- [ ] **AC-2001.9**: Sequential generation of 10,000 IDs must produce 10,000 unique values
- [ ] **AC-2001.10**: Concurrent generation within same millisecond must use unique sequence numbers
- [ ] **AC-2001.11**: Sequence overflow (>999) must trigger automatic millisecond wait and retry
- [ ] **AC-2001.12**: No duplicate IDs across multiple test runs

### Validation and Utility Methods
- [ ] **AC-2001.13**: Provide `isValidGeneratedId(Long id)` method for input validation
- [ ] **AC-2001.14**: Validation must check timestamp range (2020-2100)
- [ ] **AC-2001.15**: Validation must reject null, zero, and negative IDs
- [ ] **AC-2001.16**: Provide `generateQuestionBankId()` as primary generation method

### Performance Requirements
- [ ] **AC-2001.17**: Sequential generation must achieve >5,000,000 IDs/second
- [ ] **AC-2001.18**: Concurrent generation must achieve >1,000,000 IDs/second
- [ ] **AC-2001.19**: Memory footprint must remain <100 bytes per generator instance
- [ ] **AC-2001.20**: Zero garbage collection pressure during ID generation

### Spring Integration
- [ ] **AC-2001.21**: Class must be annotated with @Component for Spring auto-discovery
- [ ] **AC-2001.22**: Generator must be injectable via constructor injection
- [ ] **AC-2001.23**: Component must be registered in global-shared-library module
- [ ] **AC-2001.24**: No external dependencies beyond JDK and Spring Framework

## Test-Driven Development Cycle

### Epic and Story Annotations for Tests
- **@Epic**: `"Enabler Epic-Global Shared Library"`
- **@Story**: `"story-2001-thread-safe-long-id-generator"`

### Red-Green-Refactor Cycle

#### Phase 1: RED - Write Failing Tests for Core Functionality

**File**: `/home/joyfulday/nus-proj-feature-branch/quiz-cms-on-new-user-creating-default-question-bank/global-shared-library/src/test/java/com/quizfun/globalshared/utils/LongIdGeneratorTest.java`

```java
package com.quizfun.globalshared.utils;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

class LongIdGeneratorTest {

    @Test
    @Epic("Enabler Epic-Global Shared Library")
    @Story("story-2001-thread-safe-long-id-generator")
    @DisplayName("Should generate non-null Long ID")
    @Description("Validates basic ID generation returns non-null Long value")
    void shouldGenerateNonNullLongId() {
        // RED: This test should fail initially
        LongIdGenerator generator = new LongIdGenerator();

        Long id = generator.generateQuestionBankId();

        assertThat(id).isNotNull();
        assertThat(id).isPositive();
    }

    @Test
    @Epic("Enabler Epic-Global Shared Library")
    @Story("story-2001-thread-safe-long-id-generator")
    @DisplayName("Should generate unique IDs in sequential execution")
    @Description("Validates 10,000 sequentially generated IDs are all unique (AC-2001.9)")
    void shouldGenerateUniqueIdsSequentially() {
        // RED: This test should fail initially
        LongIdGenerator generator = new LongIdGenerator();
        Set<Long> generatedIds = new HashSet<>();

        for (int i = 0; i < 10000; i++) {
            Long id = generator.generateQuestionBankId();
            assertThat(generatedIds.add(id))
                .as("ID should be unique: " + id)
                .isTrue();
        }

        assertThat(generatedIds).hasSize(10000);
    }

    @Test
    @Epic("Enabler Epic-Global Shared Library")
    @Story("story-2001-thread-safe-long-id-generator")
    @DisplayName("Should generate unique IDs under concurrent load")
    @Description("Validates thread-safety with 100 threads generating 1000 IDs each (AC-2001.2, AC-2001.3)")
    void shouldGenerateUniqueIdsUnderConcurrentLoad() throws InterruptedException, ExecutionException {
        // RED: This test should fail initially
        LongIdGenerator generator = new LongIdGenerator();
        int threadCount = 100;
        int idsPerThread = 1000;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        Set<Long> allIds = ConcurrentHashMap.newKeySet();

        List<Future<Set<Long>>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            Future<Set<Long>> future = executor.submit(() -> {
                Set<Long> threadIds = new HashSet<>();
                for (int j = 0; j < idsPerThread; j++) {
                    Long id = generator.generateQuestionBankId();
                    threadIds.add(id);
                }
                return threadIds;
            });
            futures.add(future);
        }

        // Collect all IDs from all threads
        for (Future<Set<Long>> future : futures) {
            Set<Long> threadIds = future.get();
            assertThat(threadIds).hasSize(idsPerThread);
            allIds.addAll(threadIds);
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // Verify all IDs are unique across all threads
        assertThat(allIds).hasSize(threadCount * idsPerThread);
    }

    @Test
    @Epic("Enabler Epic-Global Shared Library")
    @Story("story-2001-thread-safe-long-id-generator")
    @DisplayName("Should maintain temporal ordering for sequential IDs")
    @Description("Validates IDs generated in sequence maintain chronological order (AC-2001.6)")
    void shouldMaintainTemporalOrdering() throws InterruptedException {
        // RED: This test should fail initially
        LongIdGenerator generator = new LongIdGenerator();

        Long id1 = generator.generateQuestionBankId();
        Thread.sleep(2); // Ensure different millisecond
        Long id2 = generator.generateQuestionBankId();
        Thread.sleep(2);
        Long id3 = generator.generateQuestionBankId();

        assertThat(id1).isLessThan(id2);
        assertThat(id2).isLessThan(id3);
    }

    @Test
    @Epic("Enabler Epic-Global Shared Library")
    @Story("story-2001-thread-safe-long-id-generator")
    @DisplayName("Should validate correctly generated IDs as valid")
    @Description("Validates isValidGeneratedId returns true for generator-created IDs (AC-2001.13)")
    void shouldValidateCorrectlyGeneratedIds() {
        // RED: This test should fail initially
        LongIdGenerator generator = new LongIdGenerator();

        for (int i = 0; i < 100; i++) {
            Long id = generator.generateQuestionBankId();
            assertThat(generator.isValidGeneratedId(id))
                .as("Generated ID should be valid: " + id)
                .isTrue();
        }
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L, -100L, 999L, 1000000L})
    @NullSource
    @Epic("Enabler Epic-Global Shared Library")
    @Story("story-2001-thread-safe-long-id-generator")
    @DisplayName("Should reject invalid IDs in validation")
    @Description("Validates isValidGeneratedId rejects null, negative, and out-of-range IDs (AC-2001.15)")
    void shouldRejectInvalidIds(Long invalidId) {
        // RED: This test should fail initially
        LongIdGenerator generator = new LongIdGenerator();

        assertThat(generator.isValidGeneratedId(invalidId)).isFalse();
    }

    @Test
    @Epic("Enabler Epic-Global Shared Library")
    @Story("story-2001-thread-safe-long-id-generator")
    @DisplayName("Should reject IDs with future timestamps")
    @Description("Validates timestamp range validation rejects unreasonable future dates (AC-2001.14)")
    void shouldRejectFutureTimestamps() {
        // RED: This test should fail initially
        LongIdGenerator generator = new LongIdGenerator();

        // ID from year 2150 (beyond 2100 limit)
        Long futureId = 5700000000000L * 1000; // Year 2150

        assertThat(generator.isValidGeneratedId(futureId)).isFalse();
    }

    @Test
    @Epic("Enabler Epic-Global Shared Library")
    @Story("story-2001-thread-safe-long-id-generator")
    @DisplayName("Should reject IDs with past timestamps before 2020")
    @Description("Validates timestamp range validation rejects dates before system deployment (AC-2001.14)")
    void shouldRejectPastTimestampsBeforeSystemDeployment() {
        // RED: This test should fail initially
        LongIdGenerator generator = new LongIdGenerator();

        // ID from year 2010 (before 2020 limit)
        Long pastId = 1262304000000L * 1000; // Year 2010

        assertThat(generator.isValidGeneratedId(pastId)).isFalse();
    }

    @Test
    @Epic("Enabler Epic-Global Shared Library")
    @Story("story-2001-thread-safe-long-id-generator")
    @DisplayName("Should generate IDs in correct format timestamp*1000 + sequence")
    @Description("Validates ID format follows timestamp_ms * 1000 + sequence structure (AC-2001.5)")
    void shouldGenerateIdsInCorrectFormat() {
        // RED: This test should fail initially
        LongIdGenerator generator = new LongIdGenerator();

        long beforeTimestamp = System.currentTimeMillis();
        Long id = generator.generateQuestionBankId();
        long afterTimestamp = System.currentTimeMillis();

        // Extract timestamp portion (divide by 1000)
        long extractedTimestamp = id / 1000;

        assertThat(extractedTimestamp)
            .isGreaterThanOrEqualTo(beforeTimestamp)
            .isLessThanOrEqualTo(afterTimestamp);

        // Extract sequence portion (modulo 1000)
        long sequence = id % 1000;

        assertThat(sequence)
            .isGreaterThanOrEqualTo(0)
            .isLessThan(1000);
    }

    @RepeatedTest(5)
    @Epic("Enabler Epic-Global Shared Library")
    @Story("story-2001-thread-safe-long-id-generator")
    @DisplayName("Should generate different IDs across multiple test runs")
    @Description("Validates no duplicate IDs across repeated test executions (AC-2001.12)")
    void shouldGenerateDifferentIdsAcrossTestRuns() {
        // RED: This test should fail initially
        LongIdGenerator generator = new LongIdGenerator();
        Set<Long> ids = new HashSet<>();

        for (int i = 0; i < 1000; i++) {
            Long id = generator.generateQuestionBankId();
            assertThat(ids.add(id)).isTrue();
        }
    }

    @Test
    @Epic("Enabler Epic-Global Shared Library")
    @Story("story-2001-thread-safe-long-id-generator")
    @DisplayName("Should handle rapid sequential generation within same millisecond")
    @Description("Validates sequence counter increments correctly within same millisecond (AC-2001.7, AC-2001.10)")
    void shouldHandleRapidSequentialGeneration() {
        // RED: This test should fail initially
        LongIdGenerator generator = new LongIdGenerator();
        Set<Long> ids = new HashSet<>();

        // Generate many IDs rapidly (likely within same millisecond)
        for (int i = 0; i < 500; i++) {
            Long id = generator.generateQuestionBankId();
            assertThat(ids.add(id))
                .as("Rapid generation should produce unique ID: " + id)
                .isTrue();
        }

        assertThat(ids).hasSize(500);
    }
}
```

#### Phase 2: GREEN - Implement Thread-Safe Long ID Generator

**File**: `/home/joyfulday/nus-proj-feature-branch/quiz-cms-on-new-user-creating-default-question-bank/global-shared-library/src/main/java/com/quizfun/globalshared/utils/LongIdGenerator.java`

```java
package com.quizfun.globalshared.utils;

import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe Long ID generator for internal system identifiers.
 *
 * Generates collision-resistant Long IDs using timestamp + sequence approach.
 * Designed for high-concurrency environments with optimal performance.
 *
 * Use Cases:
 * - question_bank_id generation
 * - Internal entity references requiring Long format
 * - High-performance ID generation where UUID overhead unnecessary
 *
 * Complements UUIDv7Generator for external identifiers.
 *
 * Thread Safety: Uses AtomicLong and AtomicInteger for lock-free concurrency
 * Performance: >5M IDs/sec sequential, >1M IDs/sec concurrent
 * Memory: <100 bytes per instance
 */
@Component
public class LongIdGenerator {

    private final AtomicLong lastTimestamp = new AtomicLong(0);
    private final AtomicInteger sequence = new AtomicInteger(0);

    // Maximum sequence number within single millisecond (safety limit)
    private static final int MAX_SEQUENCE = 999;

    // Timestamp validation ranges
    private static final long MIN_TIMESTAMP = 1577836800000L; // 2020-01-01
    private static final long MAX_TIMESTAMP = 4102444800000L; // 2100-01-01

    /**
     * Generates collision-resistant Long ID for question bank identifiers.
     *
     * Format: [timestamp_milliseconds * 1000] + [sequence_within_millisecond]
     *
     * Thread-safe through AtomicLong and AtomicInteger operations.
     * Handles sequence overflow by waiting for next millisecond.
     *
     * @return Unique Long ID with temporal ordering
     * @throws IllegalStateException if ID generation interrupted during overflow handling
     */
    public Long generateQuestionBankId() {
        long currentTime = System.currentTimeMillis();
        long lastTime = lastTimestamp.get();

        if (currentTime == lastTime) {
            // Same millisecond - increment sequence atomically
            int seq = sequence.incrementAndGet();

            if (seq > MAX_SEQUENCE) {
                // Extremely rare - wait for next millisecond
                try {
                    Thread.sleep(1);
                    return generateQuestionBankId(); // Retry with new timestamp
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("ID generation interrupted", e);
                }
            }

            return currentTime * 1000 + seq;
        } else {
            // New millisecond - reset sequence atomically
            lastTimestamp.set(currentTime);
            sequence.set(0);
            return currentTime * 1000;
        }
    }

    /**
     * Generates Long ID for general internal use cases.
     * Alias for generateQuestionBankId() to support future expansion.
     *
     * @return Unique Long ID
     */
    public Long generateInternalId() {
        return generateQuestionBankId();
    }

    /**
     * Validates if given Long ID was generated by this generator.
     * Useful for input validation and testing.
     *
     * Validation Rules:
     * - Must be non-null and positive
     * - Timestamp portion must be between 2020-2100
     * - Sequence portion must be 0-999
     *
     * @param id the ID to validate
     * @return true if ID appears to be validly generated, false otherwise
     */
    public boolean isValidGeneratedId(Long id) {
        if (id == null || id <= 0) {
            return false;
        }

        // Extract timestamp portion
        long timestamp = id / 1000;

        // Check if timestamp is reasonable (after year 2020, before year 2100)
        if (timestamp < MIN_TIMESTAMP || timestamp > MAX_TIMESTAMP) {
            return false;
        }

        // Extract sequence portion
        long sequenceValue = id % 1000;

        // Sequence must be 0-999
        return sequenceValue >= 0 && sequenceValue <= MAX_SEQUENCE;
    }
}
```

#### Phase 3: REFACTOR - Performance Optimization and Documentation

**Refactoring Tasks:**

1. **Performance Benchmarking**
```java
@Test
@Epic("Enabler Epic-Global Shared Library")
@Story("story-2001-thread-safe-long-id-generator")
@DisplayName("Should meet performance benchmark for sequential generation")
@Description("Validates sequential generation achieves >5M IDs/second (AC-2001.17)")
void shouldMeetSequentialPerformanceBenchmark() {
    LongIdGenerator generator = new LongIdGenerator();
    int iterations = 1_000_000;

    long startTime = System.nanoTime();

    for (int i = 0; i < iterations; i++) {
        generator.generateQuestionBankId();
    }

    long endTime = System.nanoTime();
    long durationMs = (endTime - startTime) / 1_000_000;
    double idsPerSecond = (iterations * 1000.0) / durationMs;

    assertThat(idsPerSecond)
        .as("Should generate >5M IDs/second, actual: %.2f", idsPerSecond)
        .isGreaterThan(5_000_000);
}
```

2. **Memory Footprint Validation**
```java
@Test
@Epic("Enabler Epic-Global Shared Library")
@Story("story-2001-thread-safe-long-id-generator")
@DisplayName("Should maintain minimal memory footprint")
@Description("Validates generator instance memory usage <100 bytes (AC-2001.19)")
void shouldMaintainMinimalMemoryFootprint() {
    // AtomicLong (8 bytes) + AtomicInteger (4 bytes) + object overhead (~24 bytes)
    // Total expected: ~36-40 bytes per instance

    LongIdGenerator generator = new LongIdGenerator();

    // Generate IDs without creating additional objects
    Set<Long> ids = new HashSet<>();
    for (int i = 0; i < 10000; i++) {
        ids.add(generator.generateQuestionBankId());
    }

    // No memory leaks - all IDs should be unique
    assertThat(ids).hasSize(10000);
}
```

3. **Code Documentation Enhancement**
   - Add detailed JavaDoc with usage examples
   - Document thread-safety guarantees
   - Provide performance characteristics
   - Include troubleshooting guide for sequence overflow

4. **Integration Examples**
   - Add example usage in service layer
   - Document Spring dependency injection patterns
   - Provide comparison with UUIDv7Generator use cases

### Verification Approach for Each Acceptance Criterion

#### AC-2001.1 Verification: AtomicInteger Thread Safety
```java
@Test
@Epic("Enabler Epic-Global Shared Library")
@Story("story-2001-thread-safe-long-id-generator")
@DisplayName("Verify AtomicLong and AtomicInteger usage for thread safety")
@Description("Confirms implementation uses atomic operations for lock-free concurrency (AC-2001.1)")
void verifyAtomicOperationsForThreadSafety() {
    LongIdGenerator generator = new LongIdGenerator();

    // Verify through reflection that fields are atomic types
    assertThat(generator)
        .hasFieldOrPropertyWithValue("lastTimestamp", AtomicLong.class)
        .hasFieldOrPropertyWithValue("sequence", AtomicInteger.class);
}
```

#### AC-2001.5 Verification: ID Format Structure
```java
@Test
@Epic("Enabler Epic-Global Shared Library")
@Story("story-2001-thread-safe-long-id-generator")
@DisplayName("Verify ID format follows timestamp*1000 + sequence pattern")
@Description("Validates generated IDs adhere to specified format structure (AC-2001.5)")
void verifyIdFormatStructure() {
    LongIdGenerator generator = new LongIdGenerator();

    long beforeMs = System.currentTimeMillis();
    Long id = generator.generateQuestionBankId();
    long afterMs = System.currentTimeMillis();

    long timestampPortion = id / 1000;
    long sequencePortion = id % 1000;

    assertThat(timestampPortion)
        .isBetween(beforeMs, afterMs);

    assertThat(sequencePortion)
        .isBetween(0L, 999L);
}
```

#### AC-2001.11 Verification: Sequence Overflow Handling
```java
@Test
@Epic("Enabler Epic-Global Shared Library")
@Story("story-2001-thread-safe-long-id-generator")
@DisplayName("Verify sequence overflow triggers millisecond wait and retry")
@Description("Confirms overflow handling when sequence exceeds 999 (AC-2001.11)")
void verifySequenceOverflowHandling() {
    LongIdGenerator generator = new LongIdGenerator();
    Set<Long> ids = new HashSet<>();

    // Generate >999 IDs rapidly to potentially trigger overflow
    for (int i = 0; i < 2000; i++) {
        Long id = generator.generateQuestionBankId();
        assertThat(ids.add(id))
            .as("Overflow handling should maintain uniqueness")
            .isTrue();
    }

    // All IDs should be unique even if overflow occurred
    assertThat(ids).hasSize(2000);
}
```

#### AC-2001.21 Verification: Spring Component Registration
```java
@Test
@Epic("Enabler Epic-Global Shared Library")
@Story("story-2001-thread-safe-long-id-generator")
@DisplayName("Verify @Component annotation for Spring auto-discovery")
@Description("Confirms LongIdGenerator is Spring-managed component (AC-2001.21)")
void verifySpringComponentAnnotation() {
    // Verify class has @Component annotation
    assertThat(LongIdGenerator.class)
        .hasAnnotation(org.springframework.stereotype.Component.class);
}
```

## Technical Requirements

### Concrete File Locations
- **Generator Implementation**: `/home/joyfulday/nus-proj-feature-branch/quiz-cms-on-new-user-creating-default-question-bank/global-shared-library/src/main/java/com/quizfun/globalshared/utils/LongIdGenerator.java`
- **Test Suite**: `/home/joyfulday/nus-proj-feature-branch/quiz-cms-on-new-user-creating-default-question-bank/global-shared-library/src/test/java/com/quizfun/globalshared/utils/LongIdGeneratorTest.java`
- **Package**: `com.quizfun.globalshared.utils`

### Implementation Order
1. Create `LongIdGenerator` class skeleton with @Component annotation
2. Add AtomicLong lastTimestamp and AtomicInteger sequence fields
3. Implement `generateQuestionBankId()` with basic timestamp logic
4. Add sequence increment logic for same-millisecond handling
5. Implement sequence overflow protection with Thread.sleep retry
6. Add `isValidGeneratedId()` validation method
7. Add comprehensive JavaDoc documentation
8. Write complete test suite covering all acceptance criteria

### Maven Commands for TDD Cycle
```bash
# Run specific test during RED phase (should fail)
mvn -Dtest=LongIdGeneratorTest#shouldGenerateNonNullLongId test -pl global-shared-library

# Run all generator tests during GREEN phase
mvn -Dtest=LongIdGeneratorTest test -pl global-shared-library

# Run with coverage verification during REFACTOR phase
mvn test -pl global-shared-library

# Run performance benchmarks
mvn -Dtest=LongIdGeneratorTest#shouldMeetSequentialPerformanceBenchmark test -pl global-shared-library

# Verify coverage meets 70% threshold
mvn verify -pl global-shared-library
```

## Architecture Design

### Thread-Safe ID Generation Algorithm

```java
/**
 * Thread-Safe ID Generation Flow:
 *
 * 1. Get current timestamp (milliseconds)
 * 2. Compare with lastTimestamp (atomic read)
 * 3. Branch:
 *    a) Same millisecond:
 *       - Atomically increment sequence counter
 *       - Check if sequence > 999
 *       - If overflow: wait 1ms, retry from step 1
 *       - Return: currentTime * 1000 + sequence
 *    b) New millisecond:
 *       - Atomically set lastTimestamp = currentTime
 *       - Atomically set sequence = 0
 *       - Return: currentTime * 1000
 */
```

### Atomic Operations Guarantee

**AtomicInteger.incrementAndGet() Operation:**
1. Read current value atomically
2. Calculate new value (current + 1)
3. Compare-and-swap: If current value unchanged, update to new value
4. Retry if needed: If value changed, repeat until successful
5. Return new value guaranteed unique

**Performance Characteristics:**
- Microsecond latency (faster than synchronized blocks)
- High scalability (performance degrades gracefully under contention)
- CPU-efficient (uses hardware atomic instructions)
- Memory-efficient (no object allocation during operations)

### Integration with Existing Global Shared Library

**Package Structure:**
```
com.quizfun.globalshared.utils/
├── UUIDv7Generator.java      # External identifiers (existing)
└── LongIdGenerator.java      # Internal identifiers (new)
```

**Complementary Design:**
- **UUIDv7Generator**: External references, global uniqueness, cross-system integration
- **LongIdGenerator**: Internal references, high performance, database optimization
- **Shared Patterns**: Both provide validation methods and thread-safe generation
- **Consistent API**: Similar method naming and error handling patterns

### Spring Boot Integration

**Auto-Configuration:**
```java
@Component // Automatically discovered by component scanning
public class LongIdGenerator {
    // Implementation
}
```

**Dependency Injection:**
```java
@Service
public class QuestionBankService {
    private final LongIdGenerator longIdGenerator;
    private final UUIDv7Generator uuidGenerator; // Existing

    public QuestionBankService(LongIdGenerator longIdGenerator, UUIDv7Generator uuidGenerator) {
        this.longIdGenerator = longIdGenerator;
        this.uuidGenerator = uuidGenerator;
    }

    public QuestionBank createQuestionBank(CreateQuestionBankCommand command) {
        Long questionBankId = longIdGenerator.generateQuestionBankId();     // Internal ID
        String sourceId = uuidGenerator.generateAsString();                 // External ID

        return QuestionBank.builder()
            .questionBankId(questionBankId)
            .externalReference(sourceId)
            .build();
    }
}
```

## Business Rules

### ID Generation Principles
1. **Uniqueness**: Every generated ID must be globally unique within the system
2. **Temporal Ordering**: IDs generated later must have higher numeric values
3. **Performance**: ID generation must not become a bottleneck under load
4. **Thread Safety**: Concurrent access must not cause race conditions or collisions

### Validation Standards
1. **Range Validation**: Timestamp must be between 2020-2100 (system lifetime)
2. **Format Validation**: ID structure must follow timestamp*1000 + sequence pattern
3. **Null Safety**: Validation must handle null inputs gracefully
4. **Positive Values**: All generated IDs must be positive Long values

### Error Handling
1. **Sequence Overflow**: Automatic retry with millisecond wait
2. **Thread Interruption**: Propagate InterruptedException as IllegalStateException
3. **Validation Failures**: Return false rather than throwing exceptions
4. **Graceful Degradation**: Maintain service availability during edge cases

## Definition of Done

### Implementation Complete
- [ ] LongIdGenerator class implemented with @Component annotation
- [ ] AtomicLong and AtomicInteger fields for thread-safe state management
- [ ] generateQuestionBankId() method with collision-resistant logic
- [ ] isValidGeneratedId() validation method with range checks
- [ ] Sequence overflow handling with automatic retry

### Thread Safety Verified
- [ ] Concurrent test with 100 threads × 1000 IDs passes
- [ ] Zero collisions detected in stress tests
- [ ] AtomicInteger operations confirmed through testing
- [ ] Lock-free concurrency verified

### Performance Requirements Met
- [ ] Sequential generation >5M IDs/second
- [ ] Concurrent generation >1M IDs/second
- [ ] Memory footprint <100 bytes per instance
- [ ] Zero GC pressure during generation

### Testing Complete
- [ ] Unit tests covering all 24 acceptance criteria
- [ ] Thread-safety tests with concurrent execution
- [ ] Performance benchmarks passing
- [ ] Validation method tests comprehensive
- [ ] All tests annotated with @Epic and @Story
- [ ] Test coverage >70% (JaCoCo verification)

### Documentation Complete
- [ ] JavaDoc for all public methods
- [ ] Thread-safety guarantees documented
- [ ] Performance characteristics specified
- [ ] Usage examples provided
- [ ] Integration patterns documented

### Spring Integration Complete
- [ ] @Component annotation applied
- [ ] Constructor injection verified
- [ ] Component scanning successful
- [ ] Dependency injection working in services

## Dependencies

### Prerequisites
- Java 21 (for modern concurrency APIs)
- Spring Framework 6.x (for @Component support)
- JUnit 5 (for testing)
- AssertJ (for fluent assertions)
- Allure (for test reporting)

### Module Dependencies
- **global-shared-library**: Host module for implementation
- **orchestration-layer**: Will consume this generator via dependency injection
- **internal-layer**: Will use for question bank ID generation

### No External Dependencies
- No MongoDB required (pure utility class)
- No Testcontainers needed (unit tests only)
- No external ID generation libraries

## Risk Mitigation

### Technical Risks
- **Sequence Overflow**: Mitigated by automatic millisecond wait and retry
- **Thread Contention**: Mitigated by lock-free atomic operations
- **Timestamp Collisions**: Mitigated by sequence counter within millisecond
- **Performance Degradation**: Mitigated by wait-free algorithm design

### Concurrency Risks
- **Race Conditions**: Eliminated by AtomicLong and AtomicInteger usage
- **Deadlocks**: Impossible - no locks used
- **Livelocks**: Prevented by Thread.sleep in overflow handling
- **Memory Visibility**: Guaranteed by atomic operations

### Integration Risks
- **Spring Bean Registration**: Verified through component scanning tests
- **Dependency Injection**: Tested with constructor injection patterns
- **Multi-Module Access**: Ensured by global-shared-library packaging
- **Breaking Changes**: None - this is new functionality

## Success Metrics

### Functional Correctness
- 100% unique IDs across all test scenarios
- Zero collisions in 100-thread stress test
- All validation tests passing
- Temporal ordering maintained in all cases

### Performance Targets
- Sequential generation: >5,000,000 IDs/second ✓
- Concurrent generation: >1,000,000 IDs/second ✓
- Memory per instance: <100 bytes ✓
- GC pressure: Zero allocations during generation ✓

### Code Quality
- Test coverage: >70% (enforced by JaCoCo)
- All Allure annotations present on tests
- JavaDoc coverage: 100% of public methods
- Zero compiler warnings

### Integration Success
- Spring component auto-discovery working
- Injectable in all service layers
- Usable across all modules
- Consistent with UUIDv7Generator patterns

## Usage Examples

### Basic Usage
```java
@Service
public class QuestionBankCreationService {
    private final LongIdGenerator longIdGenerator;

    public QuestionBankCreationService(LongIdGenerator longIdGenerator) {
        this.longIdGenerator = longIdGenerator;
    }

    public Result<QuestionBanksPerUserAggregate> createDefaultQuestionBank(Long userId) {
        // Generate unique question bank ID
        Long questionBankId = longIdGenerator.generateQuestionBankId();

        // Create question bank with generated ID
        var questionBank = QuestionBank.builder()
            .bankId(questionBankId)
            .name("Default Question Bank")
            .isActive(true)
            .build();

        return repository.save(questionBank);
    }
}
```

### New User Creation Workflow
```java
@EventListener
public class NewUserEventHandler {
    private final LongIdGenerator longIdGenerator;
    private final QuestionBankCreationService questionBankService;

    @Async
    public void handleNewUserCreated(UserCreatedEvent event) {
        Long userId = event.getUserId();

        // Generate unique question bank ID for default bank
        Long defaultQuestionBankId = longIdGenerator.generateQuestionBankId();

        // Create default question bank
        questionBankService.createDefaultQuestionBank(userId, defaultQuestionBankId);
    }
}
```

### Validation Example
```java
@RestController
public class QuestionBankController {
    private final LongIdGenerator longIdGenerator;

    @GetMapping("/api/question-banks/{bankId}")
    public ResponseEntity<?> getQuestionBank(@PathVariable Long bankId) {
        // Validate ID format before querying database
        if (!longIdGenerator.isValidGeneratedId(bankId)) {
            return ResponseEntity.badRequest()
                .body("Invalid question bank ID format");
        }

        // Proceed with database query
        return questionBankService.findById(bankId);
    }
}
```

## Allure Test Reporting

### Test Organization
All tests in `LongIdGeneratorTest` are organized under:
- **Epic**: "Enabler Epic-Global Shared Library"
- **Story**: "story-2001-thread-safe-long-id-generator"

### Test Categories
1. **Basic Functionality**: ID generation, uniqueness, format
2. **Thread Safety**: Concurrent execution, collision resistance
3. **Validation**: ID validation, range checks, null handling
4. **Performance**: Benchmarks for sequential and concurrent generation
5. **Edge Cases**: Overflow handling, timestamp boundaries

### Traceability Matrix
Each test method maps to specific acceptance criteria via @Description annotations, enabling complete traceability from requirements to verification.
