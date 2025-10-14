package com.quizfun.questionbank.application.security.maintenance;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test for SecurityFrameworkVersionManager.
 * Achieves 100% coverage of version management functionality.
 */
@Epic("Core Infrastructure")
@Feature("Security Framework Version Management")
@DisplayName("SecurityFrameworkVersionManager Tests")
class SecurityFrameworkVersionManagerTest {

    private SecurityFrameworkVersionManager versionManager;

    @BeforeEach
    void setUp() {
        versionManager = new SecurityFrameworkVersionManager();
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should initialize with baseline version")
    @Description("Verify initial version 1.0.0-US027 exists")
    void shouldInitializeWithBaselineVersion() {
        // Given/When: Manager is initialized

        // Then: Baseline version should exist
        assertTrue(versionManager.versionExists("1.0.0-US027"));
        Optional<SecurityFrameworkVersionManager.FrameworkVersion> version =
            versionManager.getVersion("1.0.0-US027");
        assertTrue(version.isPresent());
        assertEquals("1.0.0-US027", version.get().getVersion());
        assertNull(version.get().getPreviousVersion());
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should register new version")
    @Description("Test registering a new version with previous version tracking")
    void shouldRegisterNewVersion() {
        // Given: Baseline version exists
        String previousVersion = "1.0.0-US027";
        String newVersion = "1.1.0-US027";

        // When: Register new version
        versionManager.registerVersion(newVersion, previousVersion);

        // Then: New version should exist
        assertTrue(versionManager.versionExists(newVersion));
        Optional<SecurityFrameworkVersionManager.FrameworkVersion> version =
            versionManager.getVersion(newVersion);
        assertTrue(version.isPresent());
        assertEquals(newVersion, version.get().getVersion());
        assertEquals(previousVersion, version.get().getPreviousVersion());
        assertNotNull(version.get().getDeployedAt());
        assertNotNull(version.get().getChangelog());
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should handle version not found")
    @Description("Test getting non-existent version")
    void shouldHandleVersionNotFound() {
        // Given: Non-existent version
        String nonExistentVersion = "9.9.9-NONEXISTENT";

        // When/Then: Version should not exist
        assertFalse(versionManager.versionExists(nonExistentVersion));
        Optional<SecurityFrameworkVersionManager.FrameworkVersion> version =
            versionManager.getVersion(nonExistentVersion);
        assertFalse(version.isPresent());
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should get previous version")
    @Description("Test retrieving previous version from version chain")
    void shouldGetPreviousVersion() {
        // Given: Version chain
        versionManager.registerVersion("1.1.0", "1.0.0-US027");
        versionManager.registerVersion("1.2.0", "1.1.0");

        // When: Get previous version
        Optional<String> previous = versionManager.getPreviousVersion("1.2.0");

        // Then: Should return 1.1.0
        assertTrue(previous.isPresent());
        assertEquals("1.1.0", previous.get());
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should handle no previous version")
    @Description("Test getting previous version for baseline version")
    void shouldHandleNoPreviousVersion() {
        // Given: Baseline version (no previous)

        // When: Get previous version of baseline
        Optional<String> previous = versionManager.getPreviousVersion("1.0.0-US027");

        // Then: Should be empty
        assertFalse(previous.isPresent());
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should get rollback chain")
    @Description("Test getting rollback chain with specified depth")
    void shouldGetRollbackChain() {
        // Given: Version chain: 1.0.0 -> 1.1.0 -> 1.2.0 -> 1.3.0
        versionManager.registerVersion("1.1.0", "1.0.0-US027");
        versionManager.registerVersion("1.2.0", "1.1.0");
        versionManager.registerVersion("1.3.0", "1.2.0");

        // When: Get rollback chain from 1.3.0 with depth 3
        List<String> chain = versionManager.getRollbackChain("1.3.0", 3);

        // Then: Should return [1.3.0, 1.2.0, 1.1.0]
        assertEquals(3, chain.size());
        assertEquals("1.3.0", chain.get(0));
        assertEquals("1.2.0", chain.get(1));
        assertEquals("1.1.0", chain.get(2));
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should limit rollback chain to available versions")
    @Description("Test rollback chain when depth exceeds available versions")
    void shouldLimitRollbackChainToAvailableVersions() {
        // Given: Only 2 versions: 1.0.0 -> 1.1.0
        versionManager.registerVersion("1.1.0", "1.0.0-US027");

        // When: Request rollback chain with depth 10 (more than available)
        List<String> chain = versionManager.getRollbackChain("1.1.0", 10);

        // Then: Should only return available versions
        assertEquals(2, chain.size());
        assertEquals("1.1.0", chain.get(0));
        assertEquals("1.0.0-US027", chain.get(1));
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should get all registered versions")
    @Description("Test retrieving all versions in chronological order")
    void shouldGetAllRegisteredVersions() {
        // Given: Multiple versions
        versionManager.registerVersion("1.1.0", "1.0.0-US027");
        versionManager.registerVersion("1.2.0", "1.1.0");

        // When: Get all versions
        List<SecurityFrameworkVersionManager.FrameworkVersion> allVersions =
            versionManager.getAllVersions();

        // Then: Should include all versions
        assertNotNull(allVersions);
        assertTrue(allVersions.size() >= 3); // At least 1.0.0, 1.1.0, 1.2.0
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should update existing version on re-registration")
    @Description("Test registering the same version twice (update metadata)")
    void shouldUpdateExistingVersionOnReRegistration() {
        // Given: Version already registered
        String version = "1.1.0-TEST";
        versionManager.registerVersion(version, "1.0.0-US027");
        Optional<SecurityFrameworkVersionManager.FrameworkVersion> firstRegistration =
            versionManager.getVersion(version);
        assertTrue(firstRegistration.isPresent());

        // When: Re-register same version
        versionManager.registerVersion(version, "1.0.0-US027");

        // Then: Version should still exist (updated)
        assertTrue(versionManager.versionExists(version));
        Optional<SecurityFrameworkVersionManager.FrameworkVersion> secondRegistration =
            versionManager.getVersion(version);
        assertTrue(secondRegistration.isPresent());
        assertEquals(version, secondRegistration.get().getVersion());
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should handle rollback chain for non-existent version")
    @Description("Test rollback chain when starting version doesn't exist")
    void shouldHandleRollbackChainForNonExistentVersion() {
        // Given: Non-existent version

        // When: Get rollback chain for non-existent version
        List<String> chain = versionManager.getRollbackChain("9.9.9-NONEXISTENT", 5);

        // Then: Should return chain with only the requested version
        assertEquals(1, chain.size());
        assertEquals("9.9.9-NONEXISTENT", chain.get(0));
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should create version with changelog")
    @Description("Test that registered versions include changelog")
    void shouldCreateVersionWithChangelog() {
        // Given/When: Register version
        versionManager.registerVersion("1.1.0-CHANGELOG", "1.0.0-US027");

        // Then: Version should have changelog
        Optional<SecurityFrameworkVersionManager.FrameworkVersion> version =
            versionManager.getVersion("1.1.0-CHANGELOG");
        assertTrue(version.isPresent());
        assertNotNull(version.get().getChangelog());
        assertFalse(version.get().getChangelog().isEmpty());
    }
}
