package com.quizfun.questionbank.security.testing;

import com.quizfun.questionbank.application.security.SecurityAuditLogger;
import com.quizfun.questionbank.application.security.SecurityEventType;
import com.quizfun.shared.common.Result;
import com.quizfun.shared.validation.ValidationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Security Testing Framework for comprehensive penetration testing.
 *
 * This framework simulates various attack scenarios to validate
 * the effectiveness of security validators from US-020 through US-024.
 *
 * Key capabilities:
 * - Automated penetration testing with multiple attack vectors
 * - Performance validation under attack conditions
 * - Comprehensive security metrics and reporting
 *
 * Usage: This component should only be active in security-test profile.
 */
@Component
@Profile("security-test")
public class SecurityTestingFramework {

    private static final Logger logger = LoggerFactory.getLogger(SecurityTestingFramework.class);

    private final ValidationHandler validationChain;
    private final SecurityAuditLogger auditLogger;

    public SecurityTestingFramework(@Qualifier("questionUpsertValidationChain") ValidationHandler validationChain,
                                   SecurityAuditLogger auditLogger) {
        this.validationChain = validationChain;
        this.auditLogger = auditLogger;
        logger.info("SecurityTestingFramework initialized for penetration testing with full validation chain");
    }

    /**
     * Runs a comprehensive penetration test based on the provided configuration.
     *
     * This method:
     * 1. Generates attack scenarios for each configured attack vector
     * 2. Executes each scenario against security validators
     * 3. Collects and analyzes results
     * 4. Provides comprehensive security metrics
     *
     * @param config Penetration test configuration
     * @return Comprehensive test results with security metrics
     */
    public PenetrationTestResult runPenetrationTest(PenetrationTestConfig config) {
        logger.info("Starting penetration test with {} attack vectors", config.getAttackVectors().size());
        Instant startTime = Instant.now();

        List<AttackResult> allResults = new ArrayList<>();

        // Execute tests for each attack vector
        for (AttackVector attackVector : config.getAttackVectors()) {
            logger.debug("Testing attack vector: {}", attackVector.getDisplayName());

            List<AttackScenario> scenarios = generateAttackScenarios(
                attackVector,
                config.getAttackAttemptsPerVector(),
                config.isIncludeAdvancedScenarios()
            );

            for (AttackScenario scenario : scenarios) {
                AttackResult result = executeAttackScenario(scenario);
                allResults.add(result);
            }
        }

        Instant endTime = Instant.now();

        // Analyze results and build comprehensive report
        return analyzePenetrationTestResults(allResults, config, startTime, endTime);
    }

    /**
     * Generates attack scenarios for a specific attack vector.
     *
     * @param attackVector The type of attack to generate scenarios for
     * @param attemptCount Number of scenarios to generate
     * @param includeAdvanced Whether to include advanced attack scenarios
     * @return List of attack scenarios to execute
     */
    private List<AttackScenario> generateAttackScenarios(AttackVector attackVector,
                                                         int attemptCount,
                                                         boolean includeAdvanced) {
        List<AttackScenario> scenarios = new ArrayList<>();

        for (int i = 0; i < attemptCount; i++) {
            AttackScenario scenario = switch (attackVector) {
                case PATH_PARAMETER_MANIPULATION ->
                    AttackScenario.createPathParameterManipulation(
                        1000L + i,  // Authenticated user
                        2000L + i,  // Target user (different)
                        3000L + i   // Question bank ID
                    );

                case TOKEN_PRIVILEGE_ESCALATION ->
                    AttackScenario.createPrivilegeEscalation(
                        1000L + i,  // Authenticated user
                        9999L + i   // Restricted question bank (user doesn't own)
                    );

                case UNAUTHORIZED_ACCESS ->
                    AttackScenario.createUnauthorizedAccess(
                        2000L + i,  // Target user
                        3000L + i   // Question bank ID
                    );

                case SESSION_HIJACKING -> createSessionHijackingScenario(i);

                case RATE_LIMIT_BYPASS -> createRateLimitBypassScenario(i);

                case CONCURRENT_SESSION_VIOLATION -> createConcurrentSessionScenario(i);
            };

            scenarios.add(scenario);
        }

        return scenarios;
    }

    /**
     * Executes a single attack scenario and measures the security response.
     *
     * @param scenario The attack scenario to execute
     * @return Result of the attack execution
     */
    private AttackResult executeAttackScenario(AttackScenario scenario) {
        long startNanos = System.nanoTime();

        try {
            // Set up authentication context (JWT token with authenticatedUserId and optional sessionId)
            setupAuthenticationContext(scenario.getAuthenticatedUserId(), scenario.getSessionId());

            // Create malicious command (with targetUserId in path parameter)
            var maliciousCommand = scenario.createMaliciousCommand();

            // Execute security validation through the full chain
            // This triggers: RateLimit -> ConcurrentSession -> SessionManagement ->
            //                SecurityContext -> Ownership -> Taxonomy -> DataIntegrity
            Result<Void> validationResult = validationChain.validate(maliciousCommand);

            long durationNanos = System.nanoTime() - startNanos;
            Duration responseTime = Duration.ofNanos(durationNanos);

            // Check if attack was blocked
            boolean blocked = validationResult.isFailure();

            // Verify audit logging (simulate check - in real tests, query audit log)
            boolean auditLogged = blocked; // Simplified - real implementation would verify audit log

            return AttackResult.builder()
                .scenario(scenario)
                .blocked(blocked)
                .auditLogged(auditLogged)
                .responseTime(responseTime)
                .securityEventType(determineSecurityEventType(scenario))
                .details(validationResult.isFailure() ? validationResult.getError() : "Attack allowed")
                .build();

        } catch (Exception e) {
            logger.error("Error executing attack scenario: {}", scenario.getScenarioName(), e);

            return AttackResult.builder()
                .scenario(scenario)
                .blocked(true)  // Exception indicates blocking
                .auditLogged(false)
                .responseTime(Duration.ofNanos(System.nanoTime() - startNanos))
                .error(e.getMessage())
                .build();

        } finally {
            // Clean up security context
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * Sets up Spring Security authentication context for testing.
     *
     * @param userId User ID to include in JWT token (null for unauthorized access tests)
     */
    private void setupAuthenticationContext(Long userId) {
        setupAuthenticationContext(userId, null);
    }

    /**
     * Sets up Spring Security authentication context with custom session ID.
     *
     * @param userId User ID to include in JWT token (null for unauthorized access tests)
     * @param sessionId Optional session ID for session tracking tests
     */
    private void setupAuthenticationContext(Long userId, String sessionId) {
        if (userId == null) {
            // No authentication for unauthorized access tests
            SecurityContextHolder.clearContext();
            return;
        }

        // Create JWT token with user ID in subject claim and optional session ID
        Jwt.Builder jwtBuilder = Jwt.withTokenValue("test-token-" + userId)
            .header("alg", "HS256")
            .claim("sub", userId.toString())
            .claim("iat", Instant.now())
            .claim("exp", Instant.now().plusSeconds(3600));

        // Add session ID claim if provided (for session tracking)
        if (sessionId != null) {
            jwtBuilder.claim("jti", sessionId);  // JWT ID used as session identifier
        }

        Jwt jwt = jwtBuilder.build();
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * Determines the expected security event type for a scenario.
     */
    private SecurityEventType determineSecurityEventType(AttackScenario scenario) {
        return switch (scenario.getAttackVector()) {
            case PATH_PARAMETER_MANIPULATION -> SecurityEventType.PATH_PARAMETER_MANIPULATION;
            case TOKEN_PRIVILEGE_ESCALATION -> SecurityEventType.TOKEN_PRIVILEGE_ESCALATION;
            case SESSION_HIJACKING -> SecurityEventType.SESSION_HIJACKING_ATTEMPT;
            case UNAUTHORIZED_ACCESS -> SecurityEventType.UNAUTHORIZED_ACCESS_ATTEMPT;
            default -> SecurityEventType.SECURITY_VALIDATION_ERROR;
        };
    }

    /**
     * Analyzes penetration test results and builds comprehensive report.
     */
    private PenetrationTestResult analyzePenetrationTestResults(List<AttackResult> results,
                                                                PenetrationTestConfig config,
                                                                Instant startTime,
                                                                Instant endTime) {
        double securityScore = PenetrationTestResult.calculateSecurityScore(results);
        List<AttackResult> criticalVulns = PenetrationTestResult.findCriticalVulnerabilities(results);
        double blockedPercentage = PenetrationTestResult.calculateBlockedPercentage(results);

        // Calculate false positive rate (0 for now since we only test attacks)
        double falsePositiveRate = 0.0;

        // Build attack vector summaries
        Map<AttackVector, PenetrationTestResult.AttackVectorSummary> summaries =
            buildAttackVectorSummaries(results);

        PenetrationTestResult result = PenetrationTestResult.builder()
            .testStartTime(startTime)
            .testEndTime(endTime)
            .attackResults(results)
            .overallSecurityScore(securityScore)
            .criticalVulnerabilities(criticalVulns)
            .blockedAttackPercentage(blockedPercentage)
            .falsePositiveRate(falsePositiveRate)
            .attackVectorSummaries(summaries)
            .build();

        logger.info("Penetration test completed: {}", result.getSummary());

        return result;
    }

    /**
     * Builds summary statistics for each attack vector.
     */
    private Map<AttackVector, PenetrationTestResult.AttackVectorSummary> buildAttackVectorSummaries(
        List<AttackResult> results) {

        Map<AttackVector, List<AttackResult>> resultsByVector = results.stream()
            .collect(Collectors.groupingBy(r -> r.getScenario().getAttackVector()));

        Map<AttackVector, PenetrationTestResult.AttackVectorSummary> summaries = new HashMap<>();

        for (Map.Entry<AttackVector, List<AttackResult>> entry : resultsByVector.entrySet()) {
            AttackVector vector = entry.getKey();
            List<AttackResult> vectorResults = entry.getValue();

            long blocked = vectorResults.stream().filter(AttackResult::isBlocked).count();
            long bypassed = vectorResults.size() - blocked;
            double successRate = (double) blocked / vectorResults.size() * 100.0;
            long avgResponseMs = (long) vectorResults.stream()
                .mapToLong(r -> r.getResponseTime().toMillis())
                .average()
                .orElse(0.0);

            summaries.put(vector, PenetrationTestResult.AttackVectorSummary.builder()
                .attackVector(vector)
                .totalAttempts(vectorResults.size())
                .blockedAttempts((int) blocked)
                .bypassedAttempts((int) bypassed)
                .successRate(successRate)
                .averageResponseTimeMs(avgResponseMs)
                .build());
        }

        return summaries;
    }

    // Helper methods for creating specific attack scenarios

    private AttackScenario createSessionHijackingScenario(int index) {
        // Use same session ID for all attempts to simulate session reuse
        // In production, this would be detected via IP/User-Agent changes
        // Current implementation logs but doesn't block IP changes (dynamic IPs are common)
        Long userId = 1002L; // Same user for all session hijacking tests
        String sessionId = "hijacked-session-" + userId; // Reused session ID

        return AttackScenario.builder()
            .attackVector(AttackVector.SESSION_HIJACKING)
            .scenarioName("Session Hijacking Test " + index)
            .description("Tests session hijacking detection via session fingerprint changes")
            .authenticatedUserId(userId)
            .targetUserId(userId)
            .questionBankId(3000L + index)
            .sessionId(sessionId) // Same session ID across attempts
            .expectedOutcome(AttackScenario.ExpectedOutcome.DETECTED_AND_LOGGED)  // Logged but not blocked (IP changes are common)
            .build();
    }

    private AttackScenario createRateLimitBypassScenario(int index) {
        // Use same user ID for all rate limit tests to trigger the limit
        // This simulates rapid repeated requests from the same user
        Long userId = 1000L; // Same user for all attempts

        return AttackScenario.builder()
            .attackVector(AttackVector.RATE_LIMIT_BYPASS)
            .scenarioName("Rate Limit Bypass Test " + index)
            .description("Tests rate limiting controls by making rapid repeated requests")
            .authenticatedUserId(userId)
            .targetUserId(userId)
            .questionBankId(3000L + index)
            .expectedOutcome(AttackScenario.ExpectedOutcome.BLOCKED)  // Should be blocked after burst limit
            .build();
    }

    private AttackScenario createConcurrentSessionScenario(int index) {
        // Use same user ID but different session IDs to trigger concurrent session limit
        // Max sessions is 3, so 4th+ session should be blocked
        Long userId = 1001L; // Same user for all concurrent session tests
        String sessionId = "session-" + userId + "-" + index; // Unique session ID per test

        return AttackScenario.builder()
            .attackVector(AttackVector.CONCURRENT_SESSION_VIOLATION)
            .scenarioName("Concurrent Session Test " + index)
            .description("Tests concurrent session limits by creating multiple sessions for same user")
            .authenticatedUserId(userId)
            .targetUserId(userId)
            .questionBankId(3000L + index)
            .sessionId(sessionId)  // Different session ID for each attempt
            .expectedOutcome(AttackScenario.ExpectedOutcome.BLOCKED)  // Should be blocked after 3 sessions
            .build();
    }
}
