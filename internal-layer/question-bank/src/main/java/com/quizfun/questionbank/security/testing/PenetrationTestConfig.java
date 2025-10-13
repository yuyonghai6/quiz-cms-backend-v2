package com.quizfun.questionbank.security.testing;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Configuration for penetration testing execution.
 * Defines which endpoints, attack vectors, and security levels to test.
 */
@Data
@Builder
public class PenetrationTestConfig {
    /**
     * List of API endpoints to target in penetration tests.
     */
    private List<String> targetEndpoints;

    /**
     * Attack vectors to test against the system.
     */
    private List<AttackVector> attackVectors;

    /**
     * Expected security level that the system should achieve.
     */
    private SecurityLevel expectedSecurityLevel;

    /**
     * Number of attack attempts per attack vector.
     * Default: 10 attempts per vector for comprehensive testing.
     */
    @Builder.Default
    private int attackAttemptsPerVector = 10;

    /**
     * Whether to include advanced attack scenarios.
     * Advanced scenarios test edge cases and sophisticated attacks.
     */
    @Builder.Default
    private boolean includeAdvancedScenarios = true;

    /**
     * Security levels representing system security maturity.
     */
    public enum SecurityLevel {
        LOW,      // Basic security controls
        MEDIUM,   // Standard security controls
        HIGH,     // Advanced security controls (US-020 to US-025)
        CRITICAL  // Maximum security with all enhancements
    }
}
