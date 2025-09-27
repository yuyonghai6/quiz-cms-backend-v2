package com.quizfun.globalshared.mediator;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Result Record Tests")
class ResultTest {

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-000.shared-module-infrastructure-setup")
    @DisplayName("ResultTest.Happy Path: Should create success result with data using static factory")
    @Description("Validates that Result.success() factory method creates a successful result containing the provided data with correct success state and default success message")
    void shouldCreateSuccessResultWithData() {
        // Given
        String testData = "test data";

        // When
        Result<String> result = Result.success(testData);

        // Then
        assertThat(result.success()).isTrue();
        assertThat(result.message()).isEqualTo("Operation completed successfully");
        assertThat(result.data()).isEqualTo(testData);
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-000.shared-module-infrastructure-setup")
    @DisplayName("ResultTest.Happy Path: Should create success result with custom message and data")
    @Description("Validates that Result.success() factory method with custom message parameter creates a successful result with the specified message instead of the default one")
    void shouldCreateSuccessResultWithCustomMessageAndData() {
        // Given
        String testData = "test data";
        String customMessage = "Custom success message";

        // When
        Result<String> result = Result.success(customMessage, testData);

        // Then
        assertThat(result.success()).isTrue();
        assertThat(result.message()).isEqualTo(customMessage);
        assertThat(result.data()).isEqualTo(testData);
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-000.shared-module-infrastructure-setup")
    @DisplayName("ResultTest.Happy Path: Should create failure result with message")
    @Description("Validates that Result.failure() factory method creates a failed result with the specified error message and null data")
    void shouldCreateFailureResultWithMessage() {
        // Given
        String errorMessage = "Something went wrong";

        // When
        Result<String> result = Result.failure(errorMessage);

        // Then
        assertThat(result.success()).isFalse();
        assertThat(result.message()).isEqualTo(errorMessage);
        assertThat(result.data()).isNull();
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-000.shared-module-infrastructure-setup")
    @DisplayName("ResultTest.Happy Path: Should create failure result with message and data")
    @Description("Validates that Result.failure() factory method can create a failed result containing both an error message and associated error data for context")
    void shouldCreateFailureResultWithMessageAndData() {
        // Given
        String errorMessage = "Validation failed";
        String errorData = "Invalid email format";

        // When
        Result<String> result = Result.failure(errorMessage, errorData);

        // Then
        assertThat(result.success()).isFalse();
        assertThat(result.message()).isEqualTo(errorMessage);
        assertThat(result.data()).isEqualTo(errorData);
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-000.shared-module-infrastructure-setup")
    @DisplayName("ResultTest.Happy Path: Should work with different data types")
    @Description("Validates that the Result generic type system works correctly with different data types including primitives and custom record types")
    void shouldWorkWithDifferentDataTypes() {
        // Test with Integer
        Result<Integer> intResult = Result.success(42);
        assertThat(intResult.success()).isTrue();
        assertThat(intResult.data()).isEqualTo(42);

        // Test with custom object
        record User(String name, String email) {}
        User user = new User("John", "john@example.com");
        Result<User> userResult = Result.success(user);

        assertThat(userResult.success()).isTrue();
        assertThat(userResult.data()).isEqualTo(user);
        assertThat(userResult.data().name()).isEqualTo("John");
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-000.shared-module-infrastructure-setup")
    @DisplayName("ResultTest.Happy Path: Should handle null data gracefully")
    @Description("Validates that Result wrapper handles null data values gracefully in both success and failure cases without throwing exceptions")
    void shouldHandleNullDataGracefully() {
        // When
        Result<String> successWithNull = Result.success((String) null);
        Result<String> failureWithNull = Result.failure("Error", null);

        // Then
        assertThat(successWithNull.success()).isTrue();
        assertThat(successWithNull.data()).isNull();

        assertThat(failureWithNull.success()).isFalse();
        assertThat(failureWithNull.data()).isNull();
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-000.shared-module-infrastructure-setup")
    @DisplayName("ResultTest.Happy Path: Should support record equality")
    @Description("Validates that Result implements value-based equality semantics where two Results with the same state and data are considered equal")
    void shouldSupportRecordEquality() {
        // Given
        Result<String> result1 = Result.success("test");
        Result<String> result2 = Result.success("test");
        Result<String> result3 = Result.failure("error");

        // Then
        assertThat(result1).isEqualTo(result2);
        assertThat(result1).isNotEqualTo(result3);
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-000.shared-module-infrastructure-setup")
    @DisplayName("ResultTest.Happy Path: Should have meaningful toString representation")
    @Description("Validates that Result.toString() provides a meaningful string representation containing success state, message, and data for debugging purposes")
    void shouldHaveMeaningfulToStringRepresentation() {
        // Given
        Result<String> result = Result.success("Custom message", "test data");

        // When
        String toString = result.toString();

        // Then
        assertThat(toString).contains("true");
        assertThat(toString).contains("Custom message");
        assertThat(toString).contains("test data");
    }
}