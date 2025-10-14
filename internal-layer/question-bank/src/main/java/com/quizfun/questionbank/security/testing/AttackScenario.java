package com.quizfun.questionbank.security.testing;

import com.quizfun.questionbank.application.commands.UpsertQuestionCommand;
import com.quizfun.questionbank.application.dto.UpsertQuestionRequestDto;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Represents a specific attack scenario for security testing.
 *
 * An attack scenario combines an attack vector with specific
 * parameters and context to simulate a real-world attack attempt.
 *
 * Used by SecurityTestingFramework to generate comprehensive
 * penetration tests covering all security validators (US-020 to US-024).
 */
@Data
@Builder
public class AttackScenario {
    /**
     * The type of attack being simulated.
     */
    private AttackVector attackVector;

    /**
     * Human-readable name for this specific scenario.
     */
    private String scenarioName;

    /**
     * Detailed description of what this scenario tests.
     */
    private String description;

    /**
     * The legitimate user ID (from JWT token).
     */
    private Long authenticatedUserId;

    /**
     * The target user ID in the path parameter (may be different for attacks).
     */
    private Long targetUserId;

    /**
     * The question bank ID being targeted.
     */
    private Long questionBankId;

    /**
     * Session ID for session-based attacks (SESSION_HIJACKING, CONCURRENT_SESSION_VIOLATION).
     * Used to simulate different sessions for the same user.
     */
    private String sessionId;

    /**
     * Additional attack-specific parameters.
     */
    private Map<String, Object> attackParameters;

    /**
     * Expected outcome of this attack scenario.
     */
    private ExpectedOutcome expectedOutcome;

    /**
     * Creates a malicious command that simulates this attack scenario.
     *
     * The key to simulating attacks is the mismatch between:
     * - authenticatedUserId: The user ID from the JWT token (set in SecurityContext)
     * - targetUserId: The user ID in the command path parameter
     *
     * Security validators (US-020 to US-024) will compare these values.
     *
     * @return UpsertQuestionCommand configured for this attack scenario
     */
    public UpsertQuestionCommand createMaliciousCommand() {
        // Create a minimal valid request DTO (avoids business validation errors)
        UpsertQuestionRequestDto requestDto = SecurityTestDataBuilder.createMinimalValidRequest();

        // Customize based on attack scenario
        if (attackParameters != null && attackParameters.containsKey("sourceQuestionId")) {
            requestDto.setSourceQuestionId((String) attackParameters.get("sourceQuestionId"));
        } else {
            // Use unique source question ID based on attack vector
            requestDto.setSourceQuestionId("attack-test-" + attackVector.name().toLowerCase() + "-001");
        }

        // Create command with targetUserId (from path parameter)
        // Note: The authenticatedUserId will be set separately in SecurityContext
        // during test execution, creating the attack scenario
        return new UpsertQuestionCommand(
            targetUserId,      // Path parameter userId (potentially manipulated)
            questionBankId,    // Question bank ID being targeted
            requestDto         // Minimal valid request data
        );
    }

    /**
     * Expected outcome of an attack scenario.
     */
    public enum ExpectedOutcome {
        /**
         * Attack should be blocked by security validators.
         */
        BLOCKED,

        /**
         * Attack should be detected and logged but not blocked (for monitoring).
         */
        DETECTED_AND_LOGGED,

        /**
         * Attack should be allowed (for baseline testing).
         */
        ALLOWED
    }

    /**
     * Creates a path parameter manipulation attack scenario.
     */
    public static AttackScenario createPathParameterManipulation(Long authenticatedUserId, Long targetUserId, Long questionBankId) {
        return AttackScenario.builder()
            .attackVector(AttackVector.PATH_PARAMETER_MANIPULATION)
            .scenarioName("Path Parameter Manipulation - User ID Mismatch")
            .description("Authenticated user " + authenticatedUserId + " attempts to access user " + targetUserId + "'s resources")
            .authenticatedUserId(authenticatedUserId)
            .targetUserId(targetUserId)
            .questionBankId(questionBankId)
            .expectedOutcome(ExpectedOutcome.BLOCKED)
            .build();
    }

    /**
     * Creates a token privilege escalation attack scenario.
     */
    public static AttackScenario createPrivilegeEscalation(Long authenticatedUserId, Long restrictedQuestionBankId) {
        return AttackScenario.builder()
            .attackVector(AttackVector.TOKEN_PRIVILEGE_ESCALATION)
            .scenarioName("Token Privilege Escalation - Unauthorized Question Bank")
            .description("User " + authenticatedUserId + " attempts to access question bank " + restrictedQuestionBankId + " without ownership")
            .authenticatedUserId(authenticatedUserId)
            .targetUserId(authenticatedUserId)
            .questionBankId(restrictedQuestionBankId)
            .expectedOutcome(ExpectedOutcome.BLOCKED)
            .build();
    }

    /**
     * Creates an unauthorized access attack scenario.
     */
    public static AttackScenario createUnauthorizedAccess(Long targetUserId, Long questionBankId) {
        return AttackScenario.builder()
            .attackVector(AttackVector.UNAUTHORIZED_ACCESS)
            .scenarioName("Unauthorized Access - Missing Authentication")
            .description("Attempt to access resources without authentication token")
            .authenticatedUserId(null) // No authentication
            .targetUserId(targetUserId)
            .questionBankId(questionBankId)
            .expectedOutcome(ExpectedOutcome.BLOCKED)
            .build();
    }
}
