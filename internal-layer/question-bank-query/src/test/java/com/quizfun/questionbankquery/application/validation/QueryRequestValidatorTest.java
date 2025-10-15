package com.quizfun.questionbankquery.application.validation;

import com.quizfun.questionbankquery.application.dto.QueryQuestionsRequest;
import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("Use Case Query List of Questions of Question Bank")
@Story("1015.query-application-service")
@DisplayName("Query Request Validator Tests")
class QueryRequestValidatorTest {

    @Test
    @DisplayName("Should pass validation for valid request")
    void shouldPassValidationForValidRequest() {
        // GIVEN: Valid request
        QueryQuestionsRequest request = QueryQuestionsRequest.builder()
                .userId(12345L)
                .questionBankId(67890L)
                .page(0)
                .size(20)
                .build();

        // WHEN: Validating
        List<String> errors = QueryRequestValidator.validate(request);

        // THEN: Should have no errors
        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation when request is null")
    void shouldFailValidationWhenRequestIsNull() {
        // WHEN: Validating null request
        List<String> errors = QueryRequestValidator.validate(null);

        // THEN: Should have error
        assertThat(errors).hasSize(1);
        assertThat(errors.getFirst()).contains("cannot be null");
    }

    @Test
    @DisplayName("Should fail validation when userId is null")
    void shouldFailValidationWhenUserIdIsNull() {
        // GIVEN: Request with null userId
        QueryQuestionsRequest request = QueryQuestionsRequest.builder()
                .userId(null)
                .questionBankId(67890L)
                .build();

        // WHEN: Validating
        List<String> errors = QueryRequestValidator.validate(request);

        // THEN: Should have error
        assertThat(errors).isNotEmpty();
        assertThat(errors).anyMatch(e -> e.contains("User ID"));
    }

    @Test
    @DisplayName("Should fail validation when questionBankId is null")
    void shouldFailValidationWhenQuestionBankIdIsNull() {
        // GIVEN: Request with null questionBankId
        QueryQuestionsRequest request = QueryQuestionsRequest.builder()
                .userId(12345L)
                .questionBankId(null)
                .build();

        // WHEN: Validating
        List<String> errors = QueryRequestValidator.validate(request);

        // THEN: Should have error
        assertThat(errors).isNotEmpty();
        assertThat(errors).anyMatch(e -> e.contains("Question Bank ID"));
    }

    @Test
    @DisplayName("Should fail validation when page is negative")
    void shouldFailValidationWhenPageIsNegative() {
        // GIVEN: Request with negative page
        QueryQuestionsRequest request = QueryQuestionsRequest.builder()
                .userId(12345L)
                .questionBankId(67890L)
                .page(-1)
                .size(20)
                .build();

        // WHEN: Validating
        List<String> errors = QueryRequestValidator.validate(request);

        // THEN: Should have error
        assertThat(errors).isNotEmpty();
        assertThat(errors).anyMatch(e -> e.contains("Page number"));
    }

    @Test
    @DisplayName("Should fail validation when size is out of range")
    void shouldFailValidationWhenSizeIsOutOfRange() {
        // GIVEN: Request with invalid size
        QueryQuestionsRequest request1 = QueryQuestionsRequest.builder()
                .userId(12345L)
                .questionBankId(67890L)
                .page(0)
                .size(0)
                .build();

        QueryQuestionsRequest request2 = QueryQuestionsRequest.builder()
                .userId(12345L)
                .questionBankId(67890L)
                .page(0)
                .size(150)
                .build();

        // WHEN: Validating
        List<String> errors1 = QueryRequestValidator.validate(request1);
        List<String> errors2 = QueryRequestValidator.validate(request2);

        // THEN: Should have errors
        assertThat(errors1).isNotEmpty();
        assertThat(errors2).isNotEmpty();
    }
}
