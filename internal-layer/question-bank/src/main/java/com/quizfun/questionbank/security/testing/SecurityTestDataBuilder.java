package com.quizfun.questionbank.security.testing;

import com.quizfun.questionbank.application.dto.UpsertQuestionRequestDto;

/**
 * Test data builder for creating UpsertQuestionRequestDto instances
 * used in security testing scenarios.
 *
 * This builder creates minimal valid request DTOs that can be used
 * to test security validators without triggering business validation errors.
 */
public class SecurityTestDataBuilder {

    /**
     * Creates a minimal valid UpsertQuestionRequestDto for security testing.
     * All required fields are populated with test data.
     *
     * @return A valid request DTO suitable for security testing
     */
    public static UpsertQuestionRequestDto createMinimalValidRequest() {
        UpsertQuestionRequestDto request = new UpsertQuestionRequestDto();

        // Required fields for command construction
        request.setSourceQuestionId("security-test-question-001");
        request.setQuestionType("mcq");
        request.setTitle("Security Test Question");
        request.setContent("This is a test question for security validation");

        // Optional but common fields
        request.setPoints(10);
        request.setStatus("draft");

        return request;
    }

    /**
     * Creates a request DTO with a specific source question ID.
     * Useful for testing different attack scenarios.
     */
    public static UpsertQuestionRequestDto createRequestWithSourceId(String sourceQuestionId) {
        UpsertQuestionRequestDto request = createMinimalValidRequest();
        request.setSourceQuestionId(sourceQuestionId);
        return request;
    }
}
