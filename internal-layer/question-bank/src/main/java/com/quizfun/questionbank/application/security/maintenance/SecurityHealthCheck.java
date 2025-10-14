package com.quizfun.questionbank.application.security.maintenance;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SecurityHealthCheck represents a comprehensive security framework health assessment.
 *
 * US-027: Security Framework Maintenance and Evolution
 * AC-027.1: Security effectiveness metrics must be continuously monitored and reported
 * AC-027.3: Performance impact of security measures must be tracked over time
 * AC-027.4: Security configuration drift must be detected and corrected automatically
 *
 * This class aggregates:
 * - Current security metrics and performance data
 * - Baseline comparisons for trend analysis
 * - Configuration drift detection results
 * - Compliance validation status
 * - Alerts for issues requiring attention
 *
 * @see SecurityFrameworkMaintenanceService
 */
public class SecurityHealthCheck {

    private final Instant timestamp;
    private final Map<String, Object> metrics;
    private final Map<String, Object> analysis;
    private final Map<String, Object> compliance;
    private final List<SecurityAlert> alerts;
    private HealthStatus overallStatus;

    public SecurityHealthCheck() {
        this.timestamp = Instant.now();
        this.metrics = new HashMap<>();
        this.analysis = new HashMap<>();
        this.compliance = new HashMap<>();
        this.alerts = new ArrayList<>();
        this.overallStatus = HealthStatus.HEALTHY;
    }

    /**
     * Adds metrics data to the health check.
     *
     * @param category Metrics category (e.g., "current_performance", "validator_stats")
     * @param data Metrics data object
     */
    public void addMetrics(String category, Object data) {
        this.metrics.put(category, data);
    }

    /**
     * Adds analysis results to the health check.
     *
     * @param category Analysis category (e.g., "baseline_comparison", "trend_analysis")
     * @param data Analysis results
     */
    public void addAnalysis(String category, Object data) {
        this.analysis.put(category, data);
    }

    /**
     * Adds compliance validation results.
     *
     * @param category Compliance category (e.g., "regulatory_compliance", "policy_adherence")
     * @param data Compliance data
     */
    public void addCompliance(String category, Object data) {
        this.compliance.put(category, data);
    }

    /**
     * Adds an alert to the health check.
     *
     * @param category Alert category (e.g., "configuration_drift", "performance_degradation")
     * @param issues List of issues detected
     */
    public void addAlert(String category, List<String> issues) {
        SecurityAlert alert = new SecurityAlert(category, issues, Instant.now());
        this.alerts.add(alert);

        // Downgrade overall status if we have alerts
        if (overallStatus == HealthStatus.HEALTHY) {
            overallStatus = HealthStatus.DEGRADED;
        }
    }

    /**
     * Marks the health check as unhealthy (critical issues detected).
     */
    public void markUnhealthy() {
        this.overallStatus = HealthStatus.UNHEALTHY;
    }

    // Getters

    public Instant getTimestamp() {
        return timestamp;
    }

    public Map<String, Object> getMetrics() {
        return new HashMap<>(metrics);
    }

    public Map<String, Object> getAnalysis() {
        return new HashMap<>(analysis);
    }

    public Map<String, Object> getCompliance() {
        return new HashMap<>(compliance);
    }

    public List<SecurityAlert> getAlerts() {
        return new ArrayList<>(alerts);
    }

    public HealthStatus getOverallStatus() {
        return overallStatus;
    }

    public boolean isHealthy() {
        return overallStatus == HealthStatus.HEALTHY;
    }

    public boolean hasCriticalIssues() {
        return overallStatus == HealthStatus.UNHEALTHY;
    }

    /**
     * Health status enumeration.
     */
    public enum HealthStatus {
        HEALTHY,    // All systems operating normally
        DEGRADED,   // Some issues detected but system operational
        UNHEALTHY   // Critical issues requiring immediate attention
    }

    /**
     * Security alert record.
     */
    public static class SecurityAlert {
        private final String category;
        private final List<String> issues;
        private final Instant detectedAt;

        public SecurityAlert(String category, List<String> issues, Instant detectedAt) {
            this.category = category;
            this.issues = new ArrayList<>(issues);
            this.detectedAt = detectedAt;
        }

        public String getCategory() {
            return category;
        }

        public List<String> getIssues() {
            return new ArrayList<>(issues);
        }

        public Instant getDetectedAt() {
            return detectedAt;
        }
    }
}
