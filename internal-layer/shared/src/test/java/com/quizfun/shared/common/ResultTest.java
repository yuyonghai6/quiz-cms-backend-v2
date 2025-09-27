package com.quizfun.shared.common;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResultTest {

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-000.shared-module-infrastructure-setup")
    @DisplayName("ResultTest.Should create successful result with value")
    @Description("Validates that Result.success() creates a successful result with the provided value and correct state flags for success/failure conditions")
    void shouldCreateSuccessfulResultWithValue() {
        // Test will fail - Result class doesn't exist yet
        var result = Result.success("test-value");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.isFailure()).isFalse();
        assertThat(result.getValue()).isEqualTo("test-value");
        assertThat(result.getError()).isNull();
        assertThat(result.getErrorCode()).isNull();
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-000.shared-module-infrastructure-setup")
    @DisplayName("ResultTest.Should create failure result with error message")
    @Description("Validates that Result.failure() creates a failed result with the specified error message and null value, maintaining correct state flags")
    void shouldCreateFailureResultWithErrorMessage() {
        var result = Result.<String>failure("Something went wrong");

        assertThat(result.isFailure()).isTrue();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getValue()).isNull();
        assertThat(result.getError()).isEqualTo("Something went wrong");
        assertThat(result.getErrorCode()).isNull();
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-000.shared-module-infrastructure-setup")
    @DisplayName("ResultTest.Should create failure result with error code and message")
    @Description("Validates that Result.failure() with error code creates a failed result containing both a categorized error code and descriptive message")
    void shouldCreateFailureResultWithErrorCodeAndMessage() {
        var result = Result.<String>failure("VALIDATION_ERROR", "Invalid input");

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("Invalid input");
        assertThat(result.getErrorCode()).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-000.shared-module-infrastructure-setup")
    @DisplayName("ResultTest.Should support fluent chaining operations")
    @Description("Validates that Result supports functional programming patterns through map() and flatMap() operations for chaining transformations")
    void shouldSupportFluentChainingOperations() {
        var result = Result.success("test")
            .map(String::toUpperCase)
            .flatMap(s -> Result.success(s + "-PROCESSED"));

        assertThat(result.getValue()).isEqualTo("TEST-PROCESSED");
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-000.shared-module-infrastructure-setup")
    @DisplayName("ResultTest.Should handle failure in chain operations")
    @Description("Validates that chained operations short-circuit when encountering a failed result, preserving the original error state without executing subsequent operations")
    void shouldHandleFailureInChainOperations() {
        var result = Result.<String>failure("INITIAL_ERROR", "Failed")
            .map(String::toUpperCase)
            .flatMap(s -> Result.success(s + "-PROCESSED"));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("Failed");
        assertThat(result.getErrorCode()).isEqualTo("INITIAL_ERROR");
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-000.shared-module-infrastructure-setup")
    @DisplayName("ResultTest.Should be immutable")
    @Description("Validates that Result instances are immutable and operations like map() return new instances rather than modifying the original result")
    void shouldBeImmutable() {
        var result = Result.success("original");

        // Should not be able to modify result after creation
        assertThat(result.getValue()).isEqualTo("original");

        // Attempting to get a modified version should return new instance
        var modified = result.map(s -> s + "-modified");
        assertThat(result.getValue()).isEqualTo("original"); // Original unchanged
        assertThat(modified.getValue()).isEqualTo("original-modified");
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-000.shared-module-infrastructure-setup")
    @DisplayName("ResultTest.Should support orElse operation")
    @Description("Validates that Result.orElse() provides fallback values when the result is failed, while returning the actual value for successful results")
    void shouldSupportOrElseOperation() {
        var successResult = Result.success("value");
        var failureResult = Result.<String>failure("error");

        assertThat(successResult.orElse("default")).isEqualTo("value");
        assertThat(failureResult.orElse("default")).isEqualTo("default");
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-000.shared-module-infrastructure-setup")
    @DisplayName("ResultTest.Should support conditional operations")
    @Description("Validates that Result supports conditional execution through ifSuccess() and ifFailure() callbacks for handling results without unwrapping values")
    void shouldSupportConditionalOperations() {
        var executed = new StringBuilder();

        Result.success("value").ifSuccess(v -> executed.append("success:").append(v));
        Result.<String>failure("error").ifFailure(e -> executed.append("failure:").append(e));

        assertThat(executed.toString()).isEqualTo("success:valuefailure:error");
    }
}