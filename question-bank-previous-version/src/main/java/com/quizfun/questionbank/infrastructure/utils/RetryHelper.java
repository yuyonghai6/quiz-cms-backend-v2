package com.quizfun.questionbank.infrastructure.utils;

import com.quizfun.shared.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Utility class for implementing retry logic with exponential backoff.
 * Differentiates between retryable and non-retryable errors.
 */
@Component
public class RetryHelper {

    private static final Logger logger = LoggerFactory.getLogger(RetryHelper.class);

    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final Duration DEFAULT_INITIAL_DELAY = Duration.ofMillis(100);
    private static final Duration DEFAULT_MAX_DELAY = Duration.ofSeconds(2);

    /**
     * Executes a repository operation with retry logic for temporary failures.
     *
     * @param operation The operation to retry
     * @param operationName Name of the operation for logging purposes
     * @param <T> The type of the operation result
     * @return Result of the operation or failure with retry exhausted message
     */
    public <T> Result<T> executeWithRetry(Supplier<Result<T>> operation, String operationName) {
        return executeWithRetry(operation, operationName, DEFAULT_MAX_RETRIES, DEFAULT_INITIAL_DELAY);
    }

    /**
     * Executes a repository operation with configurable retry parameters.
     *
     * @param operation The operation to retry
     * @param operationName Name of the operation for logging purposes
     * @param maxRetries Maximum number of retry attempts
     * @param initialDelay Initial delay before first retry
     * @param <T> The type of the operation result
     * @return Result of the operation or failure with retry exhausted message
     */
    public <T> Result<T> executeWithRetry(Supplier<Result<T>> operation,
                                         String operationName,
                                         int maxRetries,
                                         Duration initialDelay) {

        Duration currentDelay = initialDelay;
        Exception lastException = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                logger.debug("Executing {} - attempt {} of {}", operationName, attempt + 1, maxRetries + 1);

                Result<T> result = operation.get();

                if (result.isSuccess()) {
                    if (attempt > 0) {
                        logger.info("Operation {} succeeded after {} retries", operationName, attempt);
                    }
                    return result;
                }

                // Check if this is a retryable failure
                if (!isRetryableError(result.getErrorCode(), result.getError())) {
                    logger.debug("Non-retryable error for operation {}: {}", operationName, result.getError());
                    return result; // Return immediately for non-retryable errors
                }

                if (attempt < maxRetries) {
                    logger.warn("Operation {} failed with retryable error (attempt {} of {}): {}. Retrying in {}ms",
                               operationName, attempt + 1, maxRetries + 1, result.getError(), currentDelay.toMillis());

                    sleep(currentDelay);
                    currentDelay = calculateNextDelay(currentDelay);
                } else {
                    logger.error("Operation {} exhausted all {} retry attempts. Final error: {}",
                                operationName, maxRetries + 1, result.getError());

                    return Result.failure(
                        "RETRY_EXHAUSTED",
                        String.format("Operation %s failed after %d attempts. Last error: %s",
                                     operationName, maxRetries + 1, result.getError())
                    );
                }

            } catch (Exception e) {
                lastException = e;

                // Check if this exception is retryable
                if (!isRetryableException(e)) {
                    logger.error("Non-retryable exception for operation {}: {}", operationName, e.getMessage());
                    return Result.failure("NON_RETRYABLE_EXCEPTION",
                                         "Operation failed with non-retryable exception: " + e.getMessage());
                }

                if (attempt < maxRetries) {
                    logger.warn("Operation {} threw retryable exception (attempt {} of {}): {}. Retrying in {}ms",
                               operationName, attempt + 1, maxRetries + 1, e.getMessage(), currentDelay.toMillis());

                    sleep(currentDelay);
                    currentDelay = calculateNextDelay(currentDelay);
                } else {
                    logger.error("Operation {} exhausted all {} retry attempts due to exceptions",
                                operationName, maxRetries + 1, e);
                }
            }
        }

        // If we get here, all retries were exhausted due to exceptions
        return Result.failure(
            "RETRY_EXHAUSTED_EXCEPTION",
            String.format("Operation %s failed after %d attempts with exceptions. Last exception: %s",
                         operationName, maxRetries + 1,
                         lastException != null ? lastException.getMessage() : "Unknown exception")
        );
    }

    /**
     * Determines if an error code/message combination represents a retryable failure.
     * Authorization and validation errors are not retryable.
     *
     * @param errorCode The error code from the Result
     * @param errorMessage The error message from the Result
     * @return true if the error should be retried, false otherwise
     */
    private boolean isRetryableError(String errorCode, String errorMessage) {
        if (errorCode == null && errorMessage == null) {
            return false;
        }

        // Non-retryable error codes (security and validation errors)
        String[] nonRetryableErrorCodes = {
            "UNAUTHORIZED_ACCESS",
            "TAXONOMY_REFERENCE_NOT_FOUND",
            "MISSING_REQUIRED_FIELD",
            "TYPE_DATA_MISMATCH",
            "INVALID_QUESTION_TYPE",
            "DUPLICATE_SOURCE_QUESTION_ID"
        };

        if (errorCode != null) {
            for (String nonRetryable : nonRetryableErrorCodes) {
                if (errorCode.contains(nonRetryable)) {
                    return false;
                }
            }
        }

        // Retryable error codes (infrastructure and temporary issues)
        String[] retryableErrorCodes = {
            "CONNECTION_TIMEOUT",
            "DATABASE_ERROR",
            "REPOSITORY_ERROR",
            "NETWORK_ERROR",
            "TEMPORARY_UNAVAILABLE"
        };

        if (errorCode != null) {
            for (String retryable : retryableErrorCodes) {
                if (errorCode.contains(retryable)) {
                    return true;
                }
            }
        }

        // Check error message for common temporary error patterns
        if (errorMessage != null) {
            String lowerMessage = errorMessage.toLowerCase();
            return lowerMessage.contains("timeout") ||
                   lowerMessage.contains("connection") ||
                   lowerMessage.contains("network") ||
                   lowerMessage.contains("temporary") ||
                   lowerMessage.contains("unavailable");
        }

        // Default: assume non-retryable to avoid infinite loops
        return false;
    }

    /**
     * Determines if an exception represents a retryable failure.
     *
     * @param exception The exception to evaluate
     * @return true if the exception should be retried, false otherwise
     */
    private boolean isRetryableException(Exception exception) {
        if (exception == null) {
            return false;
        }

        // Check exception types that are typically retryable
        String exceptionType = exception.getClass().getSimpleName().toLowerCase();
        String exceptionMessage = exception.getMessage() != null ? exception.getMessage().toLowerCase() : "";

        // Retryable exception patterns
        return exceptionType.contains("timeout") ||
               exceptionType.contains("connection") ||
               exceptionType.contains("socket") ||
               exceptionMessage.contains("timeout") ||
               exceptionMessage.contains("connection refused") ||
               exceptionMessage.contains("connection reset") ||
               exceptionMessage.contains("network") ||
               exceptionMessage.contains("temporary failure");
    }

    /**
     * Calculates the next delay using exponential backoff with jitter.
     *
     * @param currentDelay The current delay duration
     * @return The next delay duration, capped at maximum delay
     */
    private Duration calculateNextDelay(Duration currentDelay) {
        // Exponential backoff: double the delay
        Duration nextDelay = currentDelay.multipliedBy(2);

        // Cap at maximum delay
        if (nextDelay.compareTo(DEFAULT_MAX_DELAY) > 0) {
            nextDelay = DEFAULT_MAX_DELAY;
        }

        // Add jitter (±25%) to prevent thundering herd
        long baseMillis = nextDelay.toMillis();
        long jitterRange = baseMillis / 4; // 25% of base delay
        long jitter = (long) (Math.random() * jitterRange * 2) - jitterRange; // -25% to +25%

        long finalDelayMillis = Math.max(baseMillis + jitter, 10); // Minimum 10ms delay
        return Duration.ofMillis(finalDelayMillis);
    }

    /**
     * Sleeps for the specified duration, handling interruption gracefully.
     *
     * @param duration The duration to sleep
     */
    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            logger.warn("Retry delay interrupted: {}", e.getMessage());
            Thread.currentThread().interrupt(); // Restore interrupted status
        }
    }
}