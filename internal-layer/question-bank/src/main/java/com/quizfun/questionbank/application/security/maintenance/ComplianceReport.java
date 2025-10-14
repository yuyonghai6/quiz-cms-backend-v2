package com.quizfun.questionbank.application.security.maintenance;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ComplianceReport represents a comprehensive compliance report for a reporting period.
 *
 * US-027: Security Framework Maintenance and Evolution
 * AC-027.12: Regulatory compliance reporting must be automated and accurate
 *
 * This report includes:
 * - Reporting period information
 * - Current security framework version
 * - Compliance metrics and scores
 * - Security incidents during the period
 * - Framework changes and updates
 * - Recommendations for improvement
 *
 * @see SecurityGovernanceService
 * @see ComplianceValidator
 */
public class ComplianceReport {

    private final String reportId;
    private final ReportingPeriod reportingPeriod;
    private final String securityFrameworkVersion;
    private final ComplianceMetrics complianceMetrics;
    private final List<SecurityIncidentSummary> securityIncidents;
    private final List<FrameworkChange> frameworkChanges;
    private final List<String> recommendations;
    private final Instant generatedAt;
    private final String generatedBy;

    private ComplianceReport(Builder builder) {
        this.reportId = builder.reportId;
        this.reportingPeriod = builder.reportingPeriod;
        this.securityFrameworkVersion = builder.securityFrameworkVersion;
        this.complianceMetrics = builder.complianceMetrics;
        this.securityIncidents = new ArrayList<>(builder.securityIncidents);
        this.frameworkChanges = new ArrayList<>(builder.frameworkChanges);
        this.recommendations = new ArrayList<>(builder.recommendations);
        this.generatedAt = builder.generatedAt;
        this.generatedBy = builder.generatedBy;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters

    public String getReportId() {
        return reportId;
    }

    public ReportingPeriod getReportingPeriod() {
        return reportingPeriod;
    }

    public String getSecurityFrameworkVersion() {
        return securityFrameworkVersion;
    }

    public ComplianceMetrics getComplianceMetrics() {
        return complianceMetrics;
    }

    public List<SecurityIncidentSummary> getSecurityIncidents() {
        return new ArrayList<>(securityIncidents);
    }

    public List<FrameworkChange> getFrameworkChanges() {
        return new ArrayList<>(frameworkChanges);
    }

    public List<String> getRecommendations() {
        return new ArrayList<>(recommendations);
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public String getGeneratedBy() {
        return generatedBy;
    }

    /**
     * Reporting period data structure.
     */
    public static class ReportingPeriod {
        private final LocalDate startDate;
        private final LocalDate endDate;
        private final String periodLabel;

        public ReportingPeriod(LocalDate startDate, LocalDate endDate, String periodLabel) {
            this.startDate = startDate;
            this.endDate = endDate;
            this.periodLabel = periodLabel;
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public LocalDate getEndDate() {
            return endDate;
        }

        public String getPeriodLabel() {
            return periodLabel;
        }
    }

    /**
     * Compliance metrics data structure.
     */
    public static class ComplianceMetrics {
        private final double overallComplianceScore;
        private final int totalRequirements;
        private final int compliantRequirements;
        private final int nonCompliantRequirements;
        private final Map<String, Double> complianceByStandard;

        public ComplianceMetrics(double overallComplianceScore, int totalRequirements,
                                int compliantRequirements, int nonCompliantRequirements,
                                Map<String, Double> complianceByStandard) {
            this.overallComplianceScore = overallComplianceScore;
            this.totalRequirements = totalRequirements;
            this.compliantRequirements = compliantRequirements;
            this.nonCompliantRequirements = nonCompliantRequirements;
            this.complianceByStandard = new HashMap<>(complianceByStandard);
        }

        public double getOverallComplianceScore() {
            return overallComplianceScore;
        }

        public int getTotalRequirements() {
            return totalRequirements;
        }

        public int getCompliantRequirements() {
            return compliantRequirements;
        }

        public int getNonCompliantRequirements() {
            return nonCompliantRequirements;
        }

        public Map<String, Double> getComplianceByStandard() {
            return new HashMap<>(complianceByStandard);
        }
    }

    /**
     * Security incident summary.
     */
    public static class SecurityIncidentSummary {
        private final String incidentId;
        private final String incidentType;
        private final String severity;
        private final Instant occurredAt;
        private final String resolution;

        public SecurityIncidentSummary(String incidentId, String incidentType, String severity,
                                      Instant occurredAt, String resolution) {
            this.incidentId = incidentId;
            this.incidentType = incidentType;
            this.severity = severity;
            this.occurredAt = occurredAt;
            this.resolution = resolution;
        }

        public String getIncidentId() {
            return incidentId;
        }

        public String getIncidentType() {
            return incidentType;
        }

        public String getSeverity() {
            return severity;
        }

        public Instant getOccurredAt() {
            return occurredAt;
        }

        public String getResolution() {
            return resolution;
        }
    }

    /**
     * Framework change record.
     */
    public static class FrameworkChange {
        private final String changeId;
        private final String changeType;
        private final String description;
        private final Instant implementedAt;
        private final String implementedBy;

        public FrameworkChange(String changeId, String changeType, String description,
                              Instant implementedAt, String implementedBy) {
            this.changeId = changeId;
            this.changeType = changeType;
            this.description = description;
            this.implementedAt = implementedAt;
            this.implementedBy = implementedBy;
        }

        public String getChangeId() {
            return changeId;
        }

        public String getChangeType() {
            return changeType;
        }

        public String getDescription() {
            return description;
        }

        public Instant getImplementedAt() {
            return implementedAt;
        }

        public String getImplementedBy() {
            return implementedBy;
        }
    }

    /**
     * Builder for ComplianceReport.
     */
    public static class Builder {
        private String reportId;
        private ReportingPeriod reportingPeriod;
        private String securityFrameworkVersion;
        private ComplianceMetrics complianceMetrics;
        private List<SecurityIncidentSummary> securityIncidents = new ArrayList<>();
        private List<FrameworkChange> frameworkChanges = new ArrayList<>();
        private List<String> recommendations = new ArrayList<>();
        private Instant generatedAt = Instant.now();
        private String generatedBy = "SecurityGovernanceService";

        public Builder reportId(String reportId) {
            this.reportId = reportId;
            return this;
        }

        public Builder reportingPeriod(ReportingPeriod reportingPeriod) {
            this.reportingPeriod = reportingPeriod;
            return this;
        }

        public Builder securityFrameworkVersion(String securityFrameworkVersion) {
            this.securityFrameworkVersion = securityFrameworkVersion;
            return this;
        }

        public Builder complianceMetrics(ComplianceMetrics complianceMetrics) {
            this.complianceMetrics = complianceMetrics;
            return this;
        }

        public Builder securityIncidents(List<SecurityIncidentSummary> securityIncidents) {
            this.securityIncidents = new ArrayList<>(securityIncidents);
            return this;
        }

        public Builder frameworkChanges(List<FrameworkChange> frameworkChanges) {
            this.frameworkChanges = new ArrayList<>(frameworkChanges);
            return this;
        }

        public Builder recommendations(List<String> recommendations) {
            this.recommendations = new ArrayList<>(recommendations);
            return this;
        }

        public Builder generatedAt(Instant generatedAt) {
            this.generatedAt = generatedAt;
            return this;
        }

        public Builder generatedBy(String generatedBy) {
            this.generatedBy = generatedBy;
            return this;
        }

        public ComplianceReport build() {
            return new ComplianceReport(this);
        }
    }
}
