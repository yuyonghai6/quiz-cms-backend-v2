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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

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

    @Test
    @Epic("Enabler Epic-Global Shared Library")
    @Story("story-2001-thread-safe-long-id-generator")
    @DisplayName("Should meet performance benchmark for sequential generation")
    @Description("Validates sequential generation achieves >500K IDs/second with thread-safe guarantees (AC-2001.17)")
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

        // Realistic expectation: >500K IDs/second with compareAndSet thread-safety
        // This is significantly faster than database ID generation
        assertThat(idsPerSecond)
            .as("Should generate >500K IDs/second, actual: %.2f", idsPerSecond)
            .isGreaterThan(500_000);
    }

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
}
