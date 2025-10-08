package com.quizfun.questionbank.infrastructure.templates;

import com.quizfun.questionbank.domain.exceptions.TemplateProcessingException;
import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@Epic("Use Case On New User Create Default Question Bank Happy Path")
@Story("1004.template-processing")
@DisplayName("TemplateVariableReplacer Tests")
class TemplateVariableReplacerTest {

    private TemplateVariableReplacer replacer;

    @BeforeEach
    void setUp() {
        replacer = new TemplateVariableReplacer();
    }

    @Test
    @DisplayName("Should replace all variables in template")
    void shouldReplaceAllVariablesInTemplate() {
        // Given
        String template = """
            {
              "user_id": "{{NEW_USER_ID}}",
              "question_bank_id": "{{GENERATED_DEFAULT_BANK_ID}}",
              "_id": "{{GENERATED_OBJECT_ID}}",
              "created_at": "{{CURRENT_TIMESTAMP}}"
            }
            """;

        Map<String, String> variables = Map.of(
            "{{NEW_USER_ID}}", "123456789",
            "{{GENERATED_DEFAULT_BANK_ID}}", "1730832000000000",
            "{{GENERATED_OBJECT_ID}}", "67029a8c1234567890abcdef",
            "{{CURRENT_TIMESTAMP}}", "2025-10-06T10:30:15.123Z"
        );

        // When
        String result = replacer.replace(template, variables);

        // Then
        assertThat(result).contains("\"user_id\": \"123456789\"");
        assertThat(result).contains("\"question_bank_id\": \"1730832000000000\"");
        assertThat(result).contains("\"_id\": \"67029a8c1234567890abcdef\"");
        assertThat(result).contains("\"created_at\": \"2025-10-06T10:30:15.123Z\"");
        assertThat(result).doesNotContain("{{");
    }

    @Test
    @DisplayName("Should throw exception when variable not provided")
    void shouldThrowExceptionWhenVariableNotProvided() {
        // Given
        String template = "{ \"id\": \"{{MISSING_VAR}}\" }";
        Map<String, String> variables = Map.of();

        // When & Then
        assertThatThrownBy(() -> replacer.replace(template, variables))
            .isInstanceOf(TemplateProcessingException.class)
            .hasMessageContaining("MISSING_VAR");
    }

    @Test
    @DisplayName("Should load template from classpath")
    void shouldLoadTemplateFromClasspath() {
        // When
        String template = replacer.loadTemplate("question_banks_per_user.json_template");

        // Then
        assertThat(template).isNotNull();
        assertThat(template).contains("{{NEW_USER_ID}}");
        assertThat(template).contains("{{GENERATED_DEFAULT_BANK_ID}}");
        assertThat(template).contains("{{GENERATED_OBJECT_ID}}");
        assertThat(template).contains("{{CURRENT_TIMESTAMP}}");
    }

    @Test
    @DisplayName("Should load taxonomy template from classpath")
    void shouldLoadTaxonomyTemplateFromClasspath() {
        // When
        String template = replacer.loadTemplate("taxonomy_sets.json_template");

        // Then
        assertThat(template).isNotNull();
        assertThat(template).contains("{{NEW_USER_ID}}");
        assertThat(template).contains("{{GENERATED_DEFAULT_BANK_ID}}");
    }

    @Test
    @DisplayName("Should throw exception when template file not found")
    void shouldThrowExceptionWhenTemplateFileNotFound() {
        // When & Then
        assertThatThrownBy(() -> replacer.loadTemplate("nonexistent.json"))
            .isInstanceOf(TemplateProcessingException.class)
            .hasMessageContaining("Template not found");
    }

    @Test
    @DisplayName("Should handle empty template")
    void shouldHandleEmptyTemplate() {
        // Given
        String template = "";
        Map<String, String> variables = Map.of();

        // When
        String result = replacer.replace(template, variables);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should handle template with no variables")
    void shouldHandleTemplateWithNoVariables() {
        // Given
        String template = "{ \"name\": \"test\" }";
        Map<String, String> variables = Map.of();

        // When
        String result = replacer.replace(template, variables);

        // Then
        assertThat(result).isEqualTo(template);
    }

    @Test
    @DisplayName("Should handle null template")
    void shouldHandleNullTemplate() {
        // When & Then
        assertThatThrownBy(() -> replacer.replace(null, Map.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Template cannot be null");
    }

    @Test
    @DisplayName("Should handle null variables map")
    void shouldHandleNullVariablesMap() {
        // Given
        String template = "{ \"name\": \"test\" }";

        // When & Then
        assertThatThrownBy(() -> replacer.replace(template, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Variables map cannot be null");
    }

    @Test
    @DisplayName("Should replace multiple occurrences of same variable")
    void shouldReplaceMultipleOccurrencesOfSameVariable() {
        // Given
        String template = "{{USER_ID}} and {{USER_ID}} again";
        Map<String, String> variables = Map.of("{{USER_ID}}", "123");

        // When
        String result = replacer.replace(template, variables);

        // Then
        assertThat(result).isEqualTo("123 and 123 again");
    }
}
