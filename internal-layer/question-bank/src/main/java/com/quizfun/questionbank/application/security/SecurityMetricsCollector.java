package com.quizfun.questionbank.application.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * SecurityMetricsCollector listens to security events and collects metrics
 * for monitoring, analysis, and compliance reporting.
 *
 * Enhanced with US-025: Security Monitoring Integration
 * - Real-time metrics collection from security events
 * - Event type and severity level tracking
 * - User-based security violation tracking
 * - Trend analysis support for threat detection
 *
 * This collector provides data for:
 * - Security dashboards and monitoring
 * - Compliance reporting
 * - Threat pattern analysis
 * - Automated response decision-making
 *
 * @see SecurityEvent
 * @see SecurityEventPublisher
 * @see SecurityMetricsService
 */
@Component
public class SecurityMetricsCollector {

    private static final Logger logger = LoggerFactory.getLogger(SecurityMetricsCollector.class);

    // Metrics storage using concurrent data structures for thread safety
    private final Map<SecurityEventType, AtomicLong> eventTypeCounts = new ConcurrentHashMap<>();
    private final Map<SeverityLevel, AtomicLong> severityCounts = new ConcurrentHashMap<>();
    private final Map<Long, AtomicLong> userViolationCounts = new ConcurrentHashMap<>();
    private final AtomicLong totalEventsProcessed = new AtomicLong(0);

    public SecurityMetricsCollector() {
        // Initialize counters for all event types and severity levels
        for (SecurityEventType type : SecurityEventType.values()) {
            eventTypeCounts.put(type, new AtomicLong(0));
        }
        for (SeverityLevel severity : SeverityLevel.values()) {
            severityCounts.put(severity, new AtomicLong(0));
        }
        logger.info("SecurityMetricsCollector initialized with metrics tracking");
    }

    /**
     * Listens to security events and collects metrics asynchronously.
     * US-025 AC-025.1, AC-025.12: Security metrics collection for monitoring.
     *
     * @param event The security event to process
     */
    @Async
    @EventListener
    public void collectMetrics(SecurityEvent event) {
        try {
            logger.debug("Collecting metrics for security event: type={}, severity={}",
                event.getType(), event.getSeverity());

            // Increment total events counter
            totalEventsProcessed.incrementAndGet();

            // Increment event type counter
            eventTypeCounts.computeIfAbsent(event.getType(), k -> new AtomicLong(0))
                .incrementAndGet();

            // Increment severity level counter
            severityCounts.computeIfAbsent(event.getSeverity(), k -> new AtomicLong(0))
                .incrementAndGet();

            // Track user-specific violations
            if (event.getUserId() != null) {
                userViolationCounts.computeIfAbsent(event.getUserId(), k -> new AtomicLong(0))
                    .incrementAndGet();
            }

            logger.debug("Metrics collected successfully for event type: {}", event.getType());

        } catch (Exception ex) {
            logger.error("Failed to collect metrics for security event: {}", ex.getMessage(), ex);
        }
    }

    /**
     * Gets the count of events for a specific event type.
     * US-025 AC-025.10: Trend analysis and pattern recognition.
     *
     * @param eventType The security event type
     * @return The count of events for this type
     */
    public long getEventTypeCount(SecurityEventType eventType) {
        return eventTypeCounts.getOrDefault(eventType, new AtomicLong(0)).get();
    }

    /**
     * Gets the count of events for a specific severity level.
     * US-025 AC-025.12: Security metrics for compliance reporting.
     *
     * @param severity The severity level
     * @return The count of events for this severity
     */
    public long getSeverityCount(SeverityLevel severity) {
        return severityCounts.getOrDefault(severity, new AtomicLong(0)).get();
    }

    /**
     * Gets the count of security violations for a specific user.
     * US-025 AC-025.14: Progressive rate limiting support.
     *
     * @param userId The user ID
     * @return The count of violations for this user
     */
    public long getUserViolationCount(Long userId) {
        return userViolationCounts.getOrDefault(userId, new AtomicLong(0)).get();
    }

    /**
     * Gets the total number of security events processed.
     * US-025 AC-025.12: Overall security metrics.
     *
     * @return Total events processed
     */
    public long getTotalEventsProcessed() {
        return totalEventsProcessed.get();
    }

    /**
     * Gets all event type counts as a map.
     * US-025 AC-025.10: Comprehensive trend analysis.
     *
     * @return Map of event types to their counts
     */
    public Map<SecurityEventType, Long> getAllEventTypeCounts() {
        Map<SecurityEventType, Long> counts = new ConcurrentHashMap<>();
        eventTypeCounts.forEach((type, counter) -> counts.put(type, counter.get()));
        return counts;
    }

    /**
     * Gets all severity level counts as a map.
     * US-025 AC-025.12: Severity-based metrics.
     *
     * @return Map of severity levels to their counts
     */
    public Map<SeverityLevel, Long> getAllSeverityCounts() {
        Map<SeverityLevel, Long> counts = new ConcurrentHashMap<>();
        severityCounts.forEach((severity, counter) -> counts.put(severity, counter.get()));
        return counts;
    }

    /**
     * Resets all metrics counters (for testing or scheduled resets).
     */
    public void resetMetrics() {
        logger.info("Resetting all security metrics");
        totalEventsProcessed.set(0);
        eventTypeCounts.values().forEach(counter -> counter.set(0));
        severityCounts.values().forEach(counter -> counter.set(0));
        userViolationCounts.clear();
    }
}
