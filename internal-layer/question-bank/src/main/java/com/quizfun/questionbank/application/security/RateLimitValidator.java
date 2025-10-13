package com.quizfun.questionbank.application.security;

import com.quizfun.questionbank.application.commands.UpsertQuestionCommand;
import com.quizfun.questionbank.infrastructure.monitoring.ValidationChainMetrics;
import com.quizfun.questionbank.infrastructure.utils.RetryHelper;
import com.quizfun.shared.common.Result;
import com.quizfun.shared.validation.ValidationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate Limit Validator for preventing request flooding and DDoS attacks.
 *
 * Implements sliding window rate limiting to:
 * - Limit requests per user per time window
 * - Detect rate limit bypass attempts
 * - Prevent resource exhaustion attacks
 * - Track and log excessive request patterns
 *
 * This is a simplified implementation using in-memory storage.
 * Production systems should use Redis with sliding window counters.
 *
 * Rate Limits:
 * - Default: 100 requests per minute per user
 * - Burst: 20 requests per 10 seconds per user
 */
@Component
public class RateLimitValidator extends ValidationHandler {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitValidator.class);

    // Rate limit configuration
    private static final int MAX_REQUESTS_PER_MINUTE = 100;
    private static final int MAX_BURST_REQUESTS = 20;  // Per 10 seconds
    private static final long WINDOW_SIZE_MS = 60_000;  // 1 minute
    private static final long BURST_WINDOW_MS = 10_000; // 10 seconds

    private final SecurityAuditLogger auditLogger;
    private final RetryHelper retryHelper;
    private final ValidationChainMetrics metrics;

    // In-memory rate limit store (use Redis in production)
    private final Map<String, RateLimitCounter> rateLimitStore = new ConcurrentHashMap<>();

    public RateLimitValidator(SecurityAuditLogger auditLogger,
                             RetryHelper retryHelper,
                             ValidationChainMetrics metrics) {
        this.auditLogger = auditLogger;
        this.retryHelper = retryHelper;
        this.metrics = metrics;
        logger.info("RateLimitValidator initialized with limits: {} req/min, {} burst req/10s",
            MAX_REQUESTS_PER_MINUTE, MAX_BURST_REQUESTS);
    }

    @Override
    public Result<Void> validate(Object command) {
        if (!(command instanceof UpsertQuestionCommand upsertCommand)) {
            return checkNext(command);
        }

        long startTime = System.nanoTime();

        try {
            Long userId = upsertCommand.getUserId();
            logger.debug("Checking rate limit for user {}", userId);

            // Check rate limit
            Result<Void> rateLimitCheck = checkRateLimit(userId);
            if (rateLimitCheck.isFailure()) {
                logRateLimitViolation(userId, rateLimitCheck.getError());
                return rateLimitCheck;
            }

            // Pass to next validator
            return checkNext(command);

        } finally {
            metrics.stopTimer(startTime, "RateLimitValidator");
        }
    }

    /**
     * Checks if the user has exceeded rate limits.
     */
    private Result<Void> checkRateLimit(Long userId) {
        String rateLimitKey = "rate_limit:user:" + userId;
        Instant now = Instant.now();

        // Get or create rate limit counter
        RateLimitCounter counter = rateLimitStore.computeIfAbsent(
            rateLimitKey,
            k -> new RateLimitCounter()
        );

        // Clean up expired entries to prevent memory leak
        counter.cleanupExpiredEntries(now);

        // Check burst limit (10 seconds)
        int burstCount = counter.getRequestCountInWindow(now, BURST_WINDOW_MS);
        if (burstCount >= MAX_BURST_REQUESTS) {
            logger.warn("Burst rate limit exceeded for user {}: {} requests in 10s",
                userId, burstCount);

            auditLogger.logSecurityEvent(
                SecurityEvent.builder()
                    .type(SecurityEventType.RATE_LIMIT_EXCEEDED)
                    .severity(SeverityLevel.HIGH)
                    .userId(userId)
                    .details(Map.of(
                        "limitType", "BURST",
                        "requestCount", burstCount,
                        "limit", MAX_BURST_REQUESTS,
                        "windowSeconds", 10
                    ))
                    .build()
            );

            return Result.failure("RATE_LIMIT_EXCEEDED",
                String.format("Burst rate limit exceeded: %d requests in 10 seconds (limit: %d)",
                    burstCount, MAX_BURST_REQUESTS));
        }

        // Check normal rate limit (1 minute)
        int minuteCount = counter.getRequestCountInWindow(now, WINDOW_SIZE_MS);
        if (minuteCount >= MAX_REQUESTS_PER_MINUTE) {
            logger.warn("Rate limit exceeded for user {}: {} requests in 1 minute",
                userId, minuteCount);

            auditLogger.logSecurityEvent(
                SecurityEvent.builder()
                    .type(SecurityEventType.RATE_LIMIT_EXCEEDED)
                    .severity(SeverityLevel.HIGH)
                    .userId(userId)
                    .details(Map.of(
                        "limitType", "NORMAL",
                        "requestCount", minuteCount,
                        "limit", MAX_REQUESTS_PER_MINUTE,
                        "windowSeconds", 60
                    ))
                    .build()
            );

            return Result.failure("RATE_LIMIT_EXCEEDED",
                String.format("Rate limit exceeded: %d requests per minute (limit: %d)",
                    minuteCount, MAX_REQUESTS_PER_MINUTE));
        }

        // Increment counter
        counter.incrementRequests(now);

        logger.debug("Rate limit check passed for user {}: {} requests in window",
            userId, minuteCount + 1);

        return Result.success(null);
    }

    /**
     * Logs rate limit violation to audit logger.
     */
    private void logRateLimitViolation(Long userId, String error) {
        auditLogger.logSecurityEvent(
            SecurityEvent.builder()
                .type(SecurityEventType.RATE_LIMIT_EXCEEDED)
                .severity(SeverityLevel.CRITICAL)
                .userId(userId)
                .details(Map.of("error", error))
                .build()
        );
    }

    /**
     * Rate limit counter using sliding window algorithm.
     * Tracks timestamps of requests for accurate rate limiting.
     */
    private static class RateLimitCounter {
        // Store timestamps of recent requests
        private final ConcurrentHashMap<Long, AtomicInteger> requestTimestamps = new ConcurrentHashMap<>();

        /**
         * Increments request count for current timestamp (second granularity).
         */
        void incrementRequests(Instant now) {
            long timestampSeconds = now.toEpochMilli() / 1000;
            requestTimestamps.computeIfAbsent(timestampSeconds, k -> new AtomicInteger(0))
                .incrementAndGet();
        }

        /**
         * Gets total request count within the specified time window.
         */
        int getRequestCountInWindow(Instant now, long windowMs) {
            long windowStart = now.toEpochMilli() - windowMs;
            long windowStartSeconds = windowStart / 1000;

            return requestTimestamps.entrySet().stream()
                .filter(entry -> entry.getKey() >= windowStartSeconds)
                .mapToInt(entry -> entry.getValue().get())
                .sum();
        }

        /**
         * Removes expired entries to prevent memory leak.
         * Keeps last 2 minutes of data.
         */
        void cleanupExpiredEntries(Instant now) {
            long cutoffTime = (now.toEpochMilli() - (2 * 60 * 1000)) / 1000;
            requestTimestamps.entrySet().removeIf(entry -> entry.getKey() < cutoffTime);
        }
    }
}
