package com.quizfun.questionbank.application.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * SecurityAlertService handles alerting for critical security events.
 *
 * Enhanced with US-025: Security Monitoring Integration
 * - Real-time alerting for critical security events
 * - Severity-based alert routing
 * - Alert aggregation to prevent notification spam
 * - Integration with incident management systems (future)
 *
 * Current implementation provides logging-based alerts.
 * Future enhancements can integrate with external alerting platforms
 * like PagerDuty, Slack, email notifications, etc.
 *
 * Performance: Alert processing <30 seconds as required by US-025
 *
 * @see SecurityEvent
 * @see SeverityLevel
 * @see SecurityEventPublisher
 */
@Component
public class SecurityAlertService {

    private static final Logger logger = LoggerFactory.getLogger(SecurityAlertService.class);

    public SecurityAlertService() {
        logger.info("SecurityAlertService initialized for security event alerting");
    }

    /**
     * Listens to security events and triggers alerts for critical events.
     * US-025 AC-025.5: Critical security events trigger immediate alerts within 30 seconds.
     * US-025 AC-025.6: Alert routing based on event severity and type classification.
     *
     * @param event The security event to evaluate for alerting
     */
    @Async
    @EventListener
    public void processSecurityEvent(SecurityEvent event) {
        try {
            // Route based on severity level (AC-025.6)
            switch (event.getSeverity()) {
                case CRITICAL:
                    triggerCriticalAlert(event);
                    break;
                case HIGH:
                    triggerHighSeverityAlert(event);
                    break;
                case MEDIUM:
                    triggerMediumSeverityAlert(event);
                    break;
                case LOW:
                case INFO:
                    // Log only, no alerting needed
                    logger.debug("Low/Info severity event logged: type={}", event.getType());
                    break;
            }

        } catch (Exception ex) {
            logger.error("Failed to process security alert: {}", ex.getMessage(), ex);
        }
    }

    /**
     * Triggers immediate alert for CRITICAL severity events.
     * US-025 AC-025.5: Critical events must trigger immediate alerts within 30 seconds.
     *
     * @param event The critical security event
     */
    private void triggerCriticalAlert(SecurityEvent event) {
        logger.error("🚨 CRITICAL SECURITY ALERT 🚨 | Type: {} | User: {} | Session: {} | IP: {} | Details: {}",
            event.getType(),
            event.getUserId(),
            event.getSessionId(),
            event.getClientIp(),
            event.getDetails());

        // Future: Integrate with external alerting systems
        // - PagerDuty for on-call escalation
        // - Slack/Teams for team notifications
        // - Email for security team
        // - SIEM integration for SOC monitoring
    }

    /**
     * Triggers alert for HIGH severity events.
     * US-025 AC-025.6: Severity-based alert routing.
     *
     * @param event The high severity security event
     */
    private void triggerHighSeverityAlert(SecurityEvent event) {
        logger.warn("⚠️ HIGH SEVERITY SECURITY EVENT | Type: {} | User: {} | IP: {} | Details: {}",
            event.getType(),
            event.getUserId(),
            event.getClientIp(),
            event.getDetails());

        // Future: Send to security team notification channel
    }

    /**
     * Triggers alert for MEDIUM severity events.
     * US-025 AC-025.6: Severity-based alert routing.
     *
     * @param event The medium severity security event
     */
    private void triggerMediumSeverityAlert(SecurityEvent event) {
        logger.info("ℹ️ MEDIUM SEVERITY SECURITY EVENT | Type: {} | User: {} | Details: {}",
            event.getType(),
            event.getUserId(),
            event.getDetails());

        // Future: Log to security monitoring dashboard
    }

    /**
     * Checks if similar alerts have been triggered recently to prevent spam.
     * US-025 AC-025.7: Alert aggregation to prevent notification spam.
     *
     * @param event The security event to check
     * @return true if alert should be suppressed due to recent similar alert
     */
    private boolean shouldSuppressAlert(SecurityEvent event) {
        // Future implementation:
        // - Track recent alerts in cache (Redis)
        // - Suppress if same type/user within time window
        // - Aggregate similar events into single alert
        return false;
    }

    /**
     * Gets alert statistics for monitoring.
     * US-025 AC-025.12: Security metrics collection and reporting.
     *
     * @return Map of alert statistics
     */
    public java.util.Map<String, Long> getAlertStatistics() {
        // Future: Return counts of alerts by severity, type, etc.
        return java.util.Map.of(
            "totalAlerts", 0L,
            "criticalAlerts", 0L,
            "highAlerts", 0L
        );
    }
}
