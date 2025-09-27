package com.quizfun.questionbank.infrastructure.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Metrics collection for validation chain performance monitoring.
 * Simple logging-based implementation that can be enhanced with Micrometer when available.
 */
@Component
public class ValidationChainMetrics {

    private static final Logger logger = LoggerFactory.getLogger(ValidationChainMetrics.class);

    // Simple counters using atomic operations
    private final java.util.concurrent.atomic.AtomicLong successCount = new java.util.concurrent.atomic.AtomicLong(0);
    private final java.util.concurrent.atomic.AtomicLong failureCount = new java.util.concurrent.atomic.AtomicLong(0);
    private final java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.atomic.AtomicLong> errorCodeCounts = new java.util.concurrent.ConcurrentHashMap<>();

    public ValidationChainMetrics() {
        logger.info("ValidationChainMetrics initialized with simple logging-based metrics");
    }

    /**
     * Records a successful validation operation
     */
    public void recordSuccess() {
        long count = successCount.incrementAndGet();
        if (count % 100 == 0) { // Log every 100 successful validations
            logger.info("Validation success count reached: {}", count);
        }
    }

    /**
     * Records a failed validation operation with specific error code
     *
     * @param errorCode The error code that caused the validation failure
     */
    public void recordFailure(String errorCode) {
        long count = failureCount.incrementAndGet();
        String code = errorCode != null ? errorCode : "UNKNOWN";

        errorCodeCounts.computeIfAbsent(code, k -> new java.util.concurrent.atomic.AtomicLong(0))
                      .incrementAndGet();

        logger.warn("Validation failure recorded. Error code: {}, Total failures: {}", code, count);
    }

    /**
     * Records a failed validation operation with error code and validator type
     *
     * @param errorCode The error code that caused the validation failure
     * @param validatorType The type of validator that failed (ownership, taxonomy, data_integrity)
     */
    public void recordFailure(String errorCode, String validatorType) {
        recordFailure(errorCode); // Record the basic failure
        logger.warn("Validation failure in {} validator. Error code: {}",
                   validatorType != null ? validatorType : "unknown",
                   errorCode != null ? errorCode : "unknown");
    }

    /**
     * Simple timing mechanism using System.nanoTime()
     *
     * @return Start time in nanoseconds
     */
    public long startTimer() {
        return System.nanoTime();
    }

    /**
     * Stops a timer and logs the duration
     *
     * @param startTime The start time from startTimer()
     * @param operationName Name of the operation for logging
     */
    public void stopTimer(long startTime, String operationName) {
        long duration = System.nanoTime() - startTime;
        long milliseconds = duration / 1_000_000;

        if (milliseconds > 100) { // Log slow operations (>100ms)
            logger.warn("Slow validation operation detected: {} took {}ms", operationName, milliseconds);
        } else {
            logger.debug("Validation operation completed: {} took {}ms", operationName, milliseconds);
        }
    }

    /**
     * Records the number of taxonomy references validated
     *
     * @param count The number of taxonomy references processed
     */
    public void recordTaxonomyReferenceCount(int count) {
        logger.debug("Processed {} taxonomy references for validation", count);
        if (count > 20) { // Log when processing many references
            logger.info("Large taxonomy validation: processing {} references", count);
        }
    }

    /**
     * Records the validation chain step where validation failed
     *
     * @param step The validation step where failure occurred (ownership, taxonomy, data_integrity)
     */
    public void recordValidationFailureStep(String step) {
        logger.info("Validation failed at step: {}", step != null ? step : "unknown");
    }

    /**
     * Gets current metrics summary
     *
     * @return Summary of validation metrics
     */
    public String getMetricsSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Validation Metrics Summary:\n");
        summary.append("  - Successes: ").append(successCount.get()).append("\n");
        summary.append("  - Failures: ").append(failureCount.get()).append("\n");
        summary.append("  - Error codes: ");

        errorCodeCounts.forEach((code, count) ->
            summary.append(code).append("(").append(count.get()).append(") ")
        );

        return summary.toString();
    }
}