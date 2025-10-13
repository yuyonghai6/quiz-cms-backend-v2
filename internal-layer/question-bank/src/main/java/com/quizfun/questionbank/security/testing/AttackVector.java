package com.quizfun.questionbank.security.testing;

/**
 * Enumeration of attack vectors for security testing.
 * Each attack vector corresponds to security validators from US-020 through US-024.
 *
 * This enum is used by the SecurityTestingFramework to generate
 * comprehensive attack scenarios for penetration testing.
 */
public enum AttackVector {
    /**
     * Path parameter manipulation attacks (US-022).
     * Tests JWT token user ID vs path parameter user ID mismatches.
     */
    PATH_PARAMETER_MANIPULATION("Path Parameter Manipulation",
        "Attempts to access other users' resources by manipulating path parameters",
        "US-022"),

    /**
     * Token privilege escalation attacks (US-023).
     * Tests unauthorized access to resources beyond token scope.
     */
    TOKEN_PRIVILEGE_ESCALATION("Token Privilege Escalation",
        "Attempts to escalate privileges using valid but insufficient tokens",
        "US-023"),

    /**
     * Session hijacking attacks (US-024).
     * Tests IP address and User-Agent consistency violations.
     */
    SESSION_HIJACKING("Session Hijacking",
        "Attempts to hijack sessions through IP or User-Agent manipulation",
        "US-024"),

    /**
     * Rate limit bypass attempts.
     * Tests system resilience against abuse and DoS attacks.
     */
    RATE_LIMIT_BYPASS("Rate Limit Bypass",
        "Attempts to bypass rate limiting controls",
        "US-020"),

    /**
     * Unauthorized access attempts (US-020).
     * Tests missing or invalid authentication tokens.
     */
    UNAUTHORIZED_ACCESS("Unauthorized Access",
        "Attempts to access protected resources without proper authentication",
        "US-020"),

    /**
     * Concurrent session violations (US-024).
     * Tests multiple sessions from different locations simultaneously.
     */
    CONCURRENT_SESSION_VIOLATION("Concurrent Session Violation",
        "Attempts to create multiple concurrent sessions from different locations",
        "US-024");

    private final String displayName;
    private final String description;
    private final String relatedUserStory;

    AttackVector(String displayName, String description, String relatedUserStory) {
        this.displayName = displayName;
        this.description = description;
        this.relatedUserStory = relatedUserStory;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getRelatedUserStory() {
        return relatedUserStory;
    }

    /**
     * Returns the severity level of this attack vector.
     * Used for prioritizing security testing efforts.
     */
    public AttackSeverity getSeverity() {
        return switch (this) {
            case PATH_PARAMETER_MANIPULATION, TOKEN_PRIVILEGE_ESCALATION, SESSION_HIJACKING ->
                AttackSeverity.CRITICAL;
            case UNAUTHORIZED_ACCESS -> AttackSeverity.HIGH;
            case RATE_LIMIT_BYPASS, CONCURRENT_SESSION_VIOLATION -> AttackSeverity.MEDIUM;
        };
    }

    /**
     * Severity levels for attack vectors.
     */
    public enum AttackSeverity {
        CRITICAL, HIGH, MEDIUM, LOW
    }
}
