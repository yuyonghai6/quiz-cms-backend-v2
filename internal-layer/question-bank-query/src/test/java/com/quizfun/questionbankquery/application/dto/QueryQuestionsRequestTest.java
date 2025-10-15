package com.quizfun.questionbankquery.application.dto;

import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("Use Case Query List of Questions of Question Bank")
@Story("1011.query-dtos-and-pagination-logic")
@DisplayName("QueryQuestionsRequest Validation Tests")
class QueryQuestionsRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Should create valid request with all fields")
    void shouldCreateValidRequestWithAllFields() {
        QueryQuestionsRequest request = QueryQuestionsRequest.builder()
                .userId(123456L)
                .questionBankId(789012L)
                .categories(List.of("Math", "Algebra"))
                .tags(List.of("equations", "beginner"))
                .quizzes(List.of("midterm-2024"))
                .searchText("solve for x")
                .page(0)
                .size(20)
                .sortBy("createdAt")
                .sortDirection("desc")
                .build();

        Set<ConstraintViolation<QueryQuestionsRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation when userId is null")
    void shouldFailValidationWhenUserIdIsNull() {
        QueryQuestionsRequest request = QueryQuestionsRequest.builder()
                .userId(null)
                .questionBankId(789012L)
                .build();

        Set<ConstraintViolation<QueryQuestionsRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("must not be null");
    }

    @Test
    @DisplayName("Should fail validation when questionBankId is null")
    void shouldFailValidationWhenQuestionBankIdIsNull() {
        QueryQuestionsRequest request = QueryQuestionsRequest.builder()
                .userId(123456L)
                .questionBankId(null)
                .build();

        Set<ConstraintViolation<QueryQuestionsRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("must not be null");
    }

    @Test
    @DisplayName("Should fail validation when page is negative")
    void shouldFailValidationWhenPageIsNegative() {
        QueryQuestionsRequest request = QueryQuestionsRequest.builder()
                .userId(123456L)
                .questionBankId(789012L)
                .page(-1)
                .size(20)
                .build();

        Set<ConstraintViolation<QueryQuestionsRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("greater than or equal to 0");
    }

    @Test
    @DisplayName("Should fail validation when size exceeds maximum")
    void shouldFailValidationWhenSizeExceedsMaximum() {
        QueryQuestionsRequest request = QueryQuestionsRequest.builder()
                .userId(123456L)
                .questionBankId(789012L)
                .page(0)
                .size(150)
                .build();

        Set<ConstraintViolation<QueryQuestionsRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("less than or equal to 100");
    }

    @Test
    @DisplayName("Should fail validation when size is less than minimum")
    void shouldFailValidationWhenSizeIsLessThanMinimum() {
        QueryQuestionsRequest request = QueryQuestionsRequest.builder()
                .userId(123456L)
                .questionBankId(789012L)
                .page(0)
                .size(0)
                .build();

        Set<ConstraintViolation<QueryQuestionsRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("greater than or equal to 1");
    }

    @Test
    @DisplayName("Should use default values when optional fields are null")
    void shouldUseDefaultValuesWhenOptionalFieldsAreNull() {
        QueryQuestionsRequest request = QueryQuestionsRequest.builder()
                .userId(123456L)
                .questionBankId(789012L)
                .build();

        Set<ConstraintViolation<QueryQuestionsRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();

        assertThat(request.getPage()).isEqualTo(0);
        assertThat(request.getSize()).isEqualTo(20);
        assertThat(request.getSortBy()).isEqualTo("createdAt");
        assertThat(request.getSortDirection()).isEqualTo("desc");
    }

    @Test
    @DisplayName("Should accept empty filter lists")
    void shouldAcceptEmptyFilterLists() {
        QueryQuestionsRequest request = QueryQuestionsRequest.builder()
                .userId(123456L)
                .questionBankId(789012L)
                .categories(List.of())
                .tags(List.of())
                .quizzes(List.of())
                .build();

        Set<ConstraintViolation<QueryQuestionsRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should validate sortBy field values")
    void shouldValidateSortByFieldValues() {
        List<String> validSortByValues = List.of("createdAt", "updatedAt", "title", "content", "relevance");
        for (String sortBy : validSortByValues) {
            QueryQuestionsRequest request = QueryQuestionsRequest.builder()
                    .userId(123456L)
                    .questionBankId(789012L)
                    .sortBy(sortBy)
                    .build();
            Set<ConstraintViolation<QueryQuestionsRequest>> violations = validator.validate(request);
            assertThat(violations).isEmpty();
        }
    }

    @Test
    @DisplayName("Should validate sortDirection values")
    void shouldValidateSortDirectionValues() {
        List<String> validDirections = List.of("asc", "desc");
        for (String direction : validDirections) {
            QueryQuestionsRequest request = QueryQuestionsRequest.builder()
                    .userId(123456L)
                    .questionBankId(789012L)
                    .sortDirection(direction)
                    .build();
            Set<ConstraintViolation<QueryQuestionsRequest>> violations = validator.validate(request);
            assertThat(violations).isEmpty();
        }
    }
}
