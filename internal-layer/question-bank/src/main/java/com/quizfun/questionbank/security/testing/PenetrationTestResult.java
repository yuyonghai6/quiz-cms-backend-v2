package com.quizfun.questionbank.security.testing;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Comprehensive result of a penetration test execution.
 * Aggregates results from all attack scenarios and provides
 * security metrics and analysis.
 */
@Data
@Builder
public class PenetrationTestResult {
    /**
     * Timestamp when the penetration test started.
     */
    private Instant testStartTime;

    /**
     * Timestamp when the penetration test completed.
     */
    private Instant testEndTime;

    /**
     * Individual attack results from all scenarios.
     */
    private List<AttackResult> attackResults;

    /**
     * Overall security score (0-100).
     * 100 = all attacks blocked and logged correctly.
     */
    private double overallSecurityScore;

    /**
     * Critical vulnerabilities found (attacks that bypassed security).
     */
    private List<AttackResult> criticalVulnerabilities;

    /**
     * Percentage of attacks that were successfully blocked.
     */
    private double blockedAttackPercentage;

    /**
     * Percentage of false positives (legitimate requests blocked).
     */
    private double falsePositiveRate;

    /**
     * Summary of results by attack vector.
     */
    private Map<AttackVector, AttackVectorSummary> attackVectorSummaries;

    /**
     * Calculates the overall security score based on attack results.
     */
    public static double calculateSecurityScore(List<AttackResult> results) {
        if (results.isEmpty()) {
            return 0.0;
        }

        long successfulDefenses = results.stream()
            .filter(AttackResult::isSecuritySuccess)
            .count();

        return (double) successfulDefenses / results.size() * 100.0;
    }

    /**
     * Identifies critical vulnerabilities from attack results.
     */
    public static List<AttackResult> findCriticalVulnerabilities(List<AttackResult> results) {
        return results.stream()
            .filter(result -> !result.isBlocked())
            .collect(Collectors.toList());
    }

    /**
     * Calculates percentage of blocked attacks.
     */
    public static double calculateBlockedPercentage(List<AttackResult> results) {
        if (results.isEmpty()) {
            return 0.0;
        }

        long blocked = results.stream()
            .filter(AttackResult::isBlocked)
            .count();

        return (double) blocked / results.size() * 100.0;
    }

    /**
     * Summary of attack results for a specific attack vector.
     */
    @Data
    @Builder
    public static class AttackVectorSummary {
        private AttackVector attackVector;
        private int totalAttempts;
        private int blockedAttempts;
        private int bypassedAttempts;
        private double successRate;
        private long averageResponseTimeMs;
    }

    /**
     * Returns a human-readable summary of the penetration test results.
     */
    public String getSummary() {
        return String.format("""
            Penetration Test Summary:
            - Overall Security Score: %.2f/100
            - Attacks Blocked: %.2f%%
            - Critical Vulnerabilities: %d
            - Test Duration: %d seconds
            - Total Attack Scenarios: %d
            """,
            overallSecurityScore,
            blockedAttackPercentage,
            criticalVulnerabilities.size(),
            java.time.Duration.between(testStartTime, testEndTime).getSeconds(),
            attackResults.size()
        );
    }
}
