package com.quizfun.questionbank.application.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * SecurityEventPublisher publishes security events to the Spring event system
 * for monitoring, alerting, and automated response integration.
 *
 * Enhanced with US-025: Security Monitoring Integration
 * - Asynchronous event publishing to avoid blocking request processing
 * - Integration with SecurityMetricsCollector for analytics
 * - Support for automated response triggers
 * - Graceful failure handling to prevent impact on core operations
 *
 * This publisher acts as a bridge between US-021 SecurityAuditLogger and
 * downstream monitoring/response systems.
 *
 * Performance: Event publishing <5ms as required by US-025
 *
 * @see SecurityEvent
 * @see SecurityMetricsCollector
 * @see SecurityAlertService
 */
@Component
public class SecurityEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(SecurityEventPublisher.class);

    private final ApplicationEventPublisher eventPublisher;

    /**
     * Constructor with Spring ApplicationEventPublisher injection.
     *
     * @param eventPublisher Spring event publisher for internal event distribution
     */
    public SecurityEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
        logger.info("SecurityEventPublisher initialized with Spring event system integration");
    }

    /**
     * Publishes a security event asynchronously to the Spring event system.
     * US-025 AC-025.1, AC-025.2: Events published asynchronously without blocking.
     *
     * @param event The security event to publish
     */
    @Async
    public void publishEventAsync(SecurityEvent event) {
        try {
            logger.debug("Publishing security event asynchronously: type={}, severity={}, userId={}",
                event.getType(), event.getSeverity(), event.getUserId());

            // Publish to Spring event system for downstream listeners
            eventPublisher.publishEvent(event);

            logger.debug("Security event published successfully: type={}", event.getType());

        } catch (Exception ex) {
            // Log error but don't propagate to prevent impact on request processing
            logger.error("Failed to publish security event: type={}, error={}",
                event.getType(), ex.getMessage(), ex);
        }
    }

    /**
     * Publishes a security event and returns a CompletableFuture for tracking.
     * US-025 AC-025.2: Asynchronous processing with completion tracking.
     *
     * @param event The security event to publish
     * @return CompletableFuture that completes when publishing is done
     */
    @Async
    public CompletableFuture<Void> publishEventWithTracking(SecurityEvent event) {
        return CompletableFuture.runAsync(() -> {
            try {
                eventPublisher.publishEvent(event);
                logger.debug("Security event published with tracking: type={}", event.getType());
            } catch (Exception ex) {
                logger.error("Failed to publish tracked security event: {}", ex.getMessage(), ex);
                throw new RuntimeException("Event publishing failed", ex);
            }
        });
    }

    /**
     * Publishes a security event synchronously (for testing or special cases).
     *
     * @param event The security event to publish
     */
    public void publishEventSync(SecurityEvent event) {
        try {
            logger.debug("Publishing security event synchronously: type={}", event.getType());
            eventPublisher.publishEvent(event);
        } catch (Exception ex) {
            logger.error("Failed to publish security event synchronously: {}", ex.getMessage(), ex);
        }
    }
}
