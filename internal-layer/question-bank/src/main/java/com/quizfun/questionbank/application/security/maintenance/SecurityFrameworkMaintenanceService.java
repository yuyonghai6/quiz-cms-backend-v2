package com.quizfun.questionbank.application.security.maintenance;

import com.quizfun.questionbank.application.security.SecurityMetricsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SecurityFrameworkMaintenanceService provides continuous monitoring and
 * maintenance of the security framework.
 *
 * US-027: Security Framework Maintenance and Evolution
 * AC-027.1: Security effectiveness metrics must be continuously monitored and reported
 * AC-027.2: Threat landscape changes must trigger security framework updates
 * AC-027.3: Performance impact of security measures must be tracked over time
 * AC-027.4: Security configuration drift must be detected and corrected automatically
 *
 * This service:
 * - Monitors security framework health every 5 minutes
 * - Checks threat landscape every hour
 * - Detects and corrects configuration drift automatically
 * - Validates compliance continuously
 * - Generates health reports for monitoring dashboards
 *
 * Integration points:
 * - SecurityMetricsCollector (US-025): Provides real-time security metrics
 * - ThreatIntelligenceService: Monitors threat landscape
 * - SecurityConfigurationManager: Manages configuration drift
 * - ComplianceValidator: Validates regulatory compliance
 *
 * @see SecurityMetricsCollector
 * @see ThreatIntelligenceService
 * @see SecurityConfigurationManager
 * @see ComplianceValidator
 */
@Service
public class SecurityFrameworkMaintenanceService {

    private static final Logger logger = LoggerFactory.getLogger(SecurityFrameworkMaintenanceService.class);

    private final SecurityMetricsCollector metricsCollector;
    private final ThreatIntelligenceService threatIntelligence;
    private final SecurityConfigurationManager configManager;
    private final ComplianceValidator complianceValidator;

    // Baseline metrics for comparison
    private final Map<String, Long> baselineMetrics;

    public SecurityFrameworkMaintenanceService(
            SecurityMetricsCollector metricsCollector,
            ThreatIntelligenceService threatIntelligence,
            SecurityConfigurationManager configManager,
            ComplianceValidator complianceValidator) {

        this.metricsCollector = metricsCollector;
        this.threatIntelligence = threatIntelligence;
        this.configManager = configManager;
        this.complianceValidator = complianceValidator;
        this.baselineMetrics = new HashMap<>();

        logger.info("SecurityFrameworkMaintenanceService initialized with continuous monitoring");
    }

    /**
     * Monitors security framework health every 5 minutes.
     * US-027 AC-027.1, AC-027.3, AC-027.4: Continuous health monitoring
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void monitorSecurityHealth() {
        logger.info("=== Starting security health check ===");

        try {
            SecurityHealthCheck healthCheck = new SecurityHealthCheck();

            // 1. Collect current security metrics
            logger.debug("Collecting current security metrics");
            Map<String, Long> currentMetrics = collectCurrentMetrics();
            healthCheck.addMetrics("current_performance", currentMetrics);

            // 2. Compare against baselines
            logger.debug("Comparing metrics against baselines");
            Map<String, Double> baselineComparison = compareAgainstBaselines(currentMetrics);
            healthCheck.addAnalysis("baseline_comparison", baselineComparison);

            // Check for performance degradation
            if (hasPerformanceDegradation(baselineComparison)) {
                logger.warn("Performance degradation detected");
                healthCheck.addAlert("performance_degradation",
                    List.of("Security validation performance has degraded beyond threshold"));
                healthCheck.markUnhealthy();
            }

            // 3. Check for configuration drift
            logger.debug("Detecting configuration drift");
            List<SecurityConfigurationManager.ConfigurationDrift> configDrift =
                configManager.detectConfigurationDrift();

            if (!configDrift.isEmpty()) {
                logger.warn("Configuration drift detected: {} items", configDrift.size());
                healthCheck.addAlert("configuration_drift",
                    configDrift.stream()
                        .map(drift -> String.format("%s: expected='%s', current='%s'",
                            drift.getConfigKey(), drift.getExpectedValue(), drift.getCurrentValue()))
                        .toList());

                // Automatically correct drift
                logger.info("Automatically correcting configuration drift");
                List<SecurityConfigurationManager.DriftCorrection> corrections =
                    configManager.correctConfigurationDrift(configDrift);

                long successfulCorrections = corrections.stream()
                    .filter(SecurityConfigurationManager.DriftCorrection::isSuccess)
                    .count();

                logger.info("Drift correction completed: {}/{} successful",
                    successfulCorrections, corrections.size());
            }

            // 4. Validate compliance status
            logger.debug("Validating compliance status");
            ComplianceValidator.ComplianceStatus complianceStatus =
                complianceValidator.validateCurrentCompliance();
            healthCheck.addCompliance("regulatory_compliance", complianceStatus);

            if (!complianceStatus.isFullyCompliant()) {
                logger.warn("Compliance issues detected: {}/{} requirements met",
                    complianceStatus.getCompliantCount(),
                    complianceStatus.getTotalCount());

                healthCheck.addAlert("compliance_issues",
                    complianceStatus.getNonCompliantRequirements().stream()
                        .map(req -> String.format("%s: %s", req.getId(), req.getDescription()))
                        .toList());
            }

            // Process and log health check results
            processSecurityHealthCheck(healthCheck);

            logger.info("=== Security health check completed: {} ===",
                healthCheck.getOverallStatus());

        } catch (Exception ex) {
            logger.error("Security health check failed with exception", ex);
        }
    }

    /**
     * Checks threat landscape every hour.
     * US-027 AC-027.2: Threat landscape monitoring
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    public void checkThreatLandscape() {
        logger.info("=== Checking threat landscape ===");

        try {
            // 1. Query threat intelligence feeds
            List<ThreatIntelligenceService.ThreatIntelligence> latestThreats =
                threatIntelligence.getLatestThreats();

            logger.info("Retrieved {} threat intelligence items", latestThreats.size());

            // 2. Assess impact on current security posture
            ThreatIntelligenceService.ThreatAssessment threatAssessment =
                threatIntelligence.assessThreatImpact(latestThreats);

            // 3. Recommend framework updates if needed
            if (threatAssessment.requiresFrameworkUpdate()) {
                logger.warn("Threat assessment indicates framework update required");
                generateSecurityUpdateRecommendations(threatAssessment);
            } else {
                logger.info("Current security framework adequately addresses known threats");
            }

            logger.info("=== Threat landscape check completed ===");

        } catch (Exception ex) {
            logger.error("Threat landscape check failed with exception", ex);
        }
    }

    /**
     * Collects current security metrics.
     *
     * @return Map of current metrics
     */
    private Map<String, Long> collectCurrentMetrics() {
        Map<String, Long> metrics = new HashMap<>();

        // Collect metrics from SecurityMetricsCollector (US-025)
        metrics.put("total_events_processed", metricsCollector.getTotalEventsProcessed());

        // Get event type counts
        metricsCollector.getAllEventTypeCounts().forEach((type, count) ->
            metrics.put("event_type_" + type.name().toLowerCase(), count));

        // Get severity counts
        metricsCollector.getAllSeverityCounts().forEach((severity, count) ->
            metrics.put("severity_" + severity.name().toLowerCase(), count));

        return metrics;
    }

    /**
     * Compares current metrics against baselines.
     *
     * @param currentMetrics Current metrics
     * @return Comparison results (percentage change)
     */
    private Map<String, Double> compareAgainstBaselines(Map<String, Long> currentMetrics) {
        Map<String, Double> comparison = new HashMap<>();

        // If no baseline exists, establish it
        if (baselineMetrics.isEmpty()) {
            baselineMetrics.putAll(currentMetrics);
            logger.debug("Baseline metrics established");
            return comparison;
        }

        // Calculate percentage changes
        for (Map.Entry<String, Long> entry : currentMetrics.entrySet()) {
            String metricName = entry.getKey();
            Long currentValue = entry.getValue();
            Long baselineValue = baselineMetrics.getOrDefault(metricName, 0L);

            if (baselineValue > 0) {
                double percentageChange =
                    ((double) (currentValue - baselineValue) / baselineValue) * 100.0;
                comparison.put(metricName, percentageChange);
            }
        }

        return comparison;
    }

    /**
     * Checks if there is performance degradation.
     *
     * @param baselineComparison Baseline comparison results
     * @return true if performance has degraded significantly
     */
    private boolean hasPerformanceDegradation(Map<String, Double> baselineComparison) {
        // Check if critical event counts have increased significantly (>50%)
        double threshold = 50.0;

        for (Map.Entry<String, Double> entry : baselineComparison.entrySet()) {
            if (entry.getKey().contains("severity_high") || entry.getKey().contains("severity_critical")) {
                if (entry.getValue() > threshold) {
                    logger.warn("Performance degradation detected for {}: +{:.2f}%",
                        entry.getKey(), entry.getValue());
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Processes security health check results.
     *
     * @param healthCheck Health check results
     */
    private void processSecurityHealthCheck(SecurityHealthCheck healthCheck) {
        // In production, this would:
        // 1. Send health check to monitoring dashboard
        // 2. Trigger alerts for critical issues
        // 3. Update health status in centralized monitoring
        // 4. Generate compliance reports

        if (healthCheck.hasCriticalIssues()) {
            logger.error("CRITICAL: Security framework has critical issues requiring immediate attention");
            logger.error("Alerts: {}", healthCheck.getAlerts());
        } else if (!healthCheck.isHealthy()) {
            logger.warn("WARNING: Security framework is degraded but operational");
            logger.warn("Alerts: {}", healthCheck.getAlerts());
        } else {
            logger.info("Security framework is healthy");
        }

        // Log metrics for monitoring
        logger.debug("Health check metrics: {}", healthCheck.getMetrics());
        logger.debug("Health check analysis: {}", healthCheck.getAnalysis());
        logger.debug("Compliance status: {}", healthCheck.getCompliance());
    }

    /**
     * Generates security update recommendations based on threat assessment.
     *
     * @param threatAssessment Threat assessment results
     */
    private void generateSecurityUpdateRecommendations(
            ThreatIntelligenceService.ThreatAssessment threatAssessment) {

        logger.info("Generating security framework update recommendations");

        // Critical threats require immediate attention
        if (!threatAssessment.getCriticalThreats().isEmpty()) {
            logger.error("CRITICAL THREATS DETECTED:");
            for (ThreatIntelligenceService.ThreatIntelligence threat :
                    threatAssessment.getCriticalThreats()) {
                logger.error("  - {}: {}", threat.getThreatId(), threat.getName());
                logger.error("    Affected components: {}", threat.getAffectedComponents());
                logger.error("    Recommendation: Immediate framework update required");
            }
        }

        // Unmitigated threats need framework enhancements
        if (!threatAssessment.getUnmitigatedThreats().isEmpty()) {
            logger.warn("UNMITIGATED THREATS:");
            for (ThreatIntelligenceService.ThreatIntelligence threat :
                    threatAssessment.getUnmitigatedThreats()) {
                logger.warn("  - {}: {}", threat.getThreatId(), threat.getName());
                logger.warn("    Description: {}", threat.getDescription());
                logger.warn("    Recommendation: Add security controls for affected components");
            }
        }

        // In production, this would:
        // 1. Create security update tickets
        // 2. Notify security team
        // 3. Schedule framework evolution planning
        // 4. Document threat intelligence findings
    }

    /**
     * Manual trigger for health check (for testing or on-demand monitoring).
     *
     * @return Health check results
     */
    public SecurityHealthCheck performHealthCheck() {
        logger.info("Performing on-demand security health check");
        monitorSecurityHealth();

        // Return a fresh health check
        SecurityHealthCheck healthCheck = new SecurityHealthCheck();
        healthCheck.addMetrics("current_metrics", collectCurrentMetrics());
        return healthCheck;
    }

    /**
     * Gets current framework health status.
     *
     * @return Current health status summary
     */
    public Map<String, Object> getHealthStatus() {
        Map<String, Object> status = new HashMap<>();

        status.put("total_events_processed", metricsCollector.getTotalEventsProcessed());
        status.put("compliance_status", complianceValidator.validateCurrentCompliance());
        status.put("configuration_drift_count",
            configManager.detectConfigurationDrift().size());
        status.put("monitoring_active", true);

        return status;
    }
}
