package com.quizfun.globalshared.utils;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class UUIDv7GeneratorTest {

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-000.shared-module-infrastructure-setup")
    @DisplayName("UUIDv7GeneratorTest.Should generate valid UUID v7")
    @Description("Validates that generated UUIDs are version 7 and properly formatted")
    void shouldGenerateValidUUIDv7() {
        UUID uuid = UUIDv7Generator.generate();

        assertThat(uuid).isNotNull();
        assertThat(uuid.version()).isEqualTo(7);
        assertThat(UUIDv7Generator.isValidUUIDv7(uuid)).isTrue();
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-000.shared-module-infrastructure-setup")
    @DisplayName("UUIDv7GeneratorTest.Should generate unique UUIDs")
    @Description("Ensures multiple generated UUIDs are unique from each other")
    void shouldGenerateUniqueUUIDs() {
        Set<UUID> generatedUUIDs = new HashSet<>();

        for (int i = 0; i < 1000; i++) {
            UUID uuid = UUIDv7Generator.generate();
            assertThat(generatedUUIDs.add(uuid))
                .as("UUID should be unique: " + uuid)
                .isTrue();
        }
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-000.shared-module-infrastructure-setup")
    @DisplayName("UUIDv7GeneratorTest.Should generate UUID as string")
    @Description("Validates string generation method produces valid UUID v7 strings")
    void shouldGenerateUUIDAsString() {
        String uuidString = UUIDv7Generator.generateAsString();

        assertThat(uuidString).isNotNull();
        assertThat(uuidString).hasSize(36); // Standard UUID string length
        assertThat(UUIDv7Generator.isValidUUIDv7(uuidString)).isTrue();

        // Should be parseable back to UUID
        UUID parsedUuid = UUID.fromString(uuidString);
        assertThat(parsedUuid.version()).isEqualTo(7);
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-000.shared-module-infrastructure-setup")
    @DisplayName("UUIDv7GeneratorTest.Should validate valid UUID v7 strings")
    @Description("Tests validation of properly formatted UUID v7 strings")
    void shouldValidateValidUUIDv7Strings() {
        // Generate a few UUIDs and validate them
        for (int i = 0; i < 10; i++) {
            String uuidString = UUIDv7Generator.generateAsString();
            assertThat(UUIDv7Generator.isValidUUIDv7(uuidString)).isTrue();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "   ",
        "not-a-uuid",
        "123e4567-e89b-12d3-a456-426614174000", // UUID v1 format
        "550e8400-e29b-41d4-a716-446655440000", // UUID v4 format
        "invalid-format-string"
    })
    @NullSource
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-000.shared-module-infrastructure-setup")
    @DisplayName("UUIDv7GeneratorTest.Should reject invalid UUID strings")
    @Description("Validates rejection of malformed or non-v7 UUID strings")
    void shouldRejectInvalidUUIDStrings(String invalidUuid) {
        assertThat(UUIDv7Generator.isValidUUIDv7(invalidUuid)).isFalse();
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-000.shared-module-infrastructure-setup")
    @DisplayName("UUIDv7GeneratorTest.Should parse valid UUID v7 string")
    @Description("Tests successful parsing of valid UUID v7 strings")
    void shouldParseValidUUIDv7String() {
        String originalString = UUIDv7Generator.generateAsString();

        UUID parsedUuid = UUIDv7Generator.parseUUIDv7(originalString);

        assertThat(parsedUuid).isNotNull();
        assertThat(parsedUuid.version()).isEqualTo(7);
        assertThat(parsedUuid.toString()).isEqualTo(originalString);
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-000.shared-module-infrastructure-setup")
    @DisplayName("UUIDv7GeneratorTest.Should handle whitespace in UUID strings")
    @Description("Validates proper trimming of whitespace in UUID string validation and parsing")
    void shouldHandleWhitespaceInUUIDStrings() {
        String originalString = UUIDv7Generator.generateAsString();
        String paddedString = "  " + originalString + "  ";

        assertThat(UUIDv7Generator.isValidUUIDv7(paddedString)).isTrue();

        UUID parsedUuid = UUIDv7Generator.parseUUIDv7(paddedString);
        assertThat(parsedUuid.toString()).isEqualTo(originalString);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "invalid-uuid",
        "not-uuid-at-all",
        "",
        "550e8400-e29b-41d4-a716-446655440000" // Valid UUID but not v7
    })
    @NullSource
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-000.shared-module-infrastructure-setup")
    @DisplayName("UUIDv7GeneratorTest.Should throw exception for invalid parse attempts")
    @Description("Ensures parseUUIDv7 throws appropriate exceptions for invalid inputs")
    void shouldThrowExceptionForInvalidParseAttempts(String invalidUuid) {
        assertThatThrownBy(() -> UUIDv7Generator.parseUUIDv7(invalidUuid))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid UUID v7 format");
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-000.shared-module-infrastructure-setup")
    @DisplayName("UUIDv7GeneratorTest.Should maintain temporal ordering")
    @Description("Verifies that UUID v7s generated in sequence maintain temporal ordering")
    void shouldMaintainTemporalOrdering() throws InterruptedException {
        UUID uuid1 = UUIDv7Generator.generate();
        Thread.sleep(1); // Small delay to ensure different timestamps
        UUID uuid2 = UUIDv7Generator.generate();

        // UUID v7s should be naturally sortable by time
        // Convert to string and compare lexicographically (UUID v7 property)
        assertThat(uuid1.toString().compareTo(uuid2.toString())).isLessThan(0);
    }
}