package com.quizfun.questionbank.application.security.maintenance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * SecurityFrameworkVersionManager manages version history and rollback capabilities.
 *
 * US-027: Security Framework Maintenance and Evolution
 * AC-027.6: Version rollback must be possible within 60 seconds
 *
 * This manager:
 * - Maintains version registry with deployment metadata
 * - Tracks version history and relationships
 * - Provides version lookup for rollback operations
 * - Stores version-specific configuration snapshots
 * - Enables fast version retrieval for rapid rollback
 *
 * Integration points:
 * - SecurityFrameworkEvolutionManager: Version registration during deployments
 *
 * @see SecurityFrameworkEvolutionManager
 */
@Component
public class SecurityFrameworkVersionManager {

    private static final Logger logger = LoggerFactory.getLogger(SecurityFrameworkVersionManager.class);

    // Version registry (in production, this would be persistent storage)
    private final Map<String, FrameworkVersion> versionRegistry = new LinkedHashMap<>();

    public SecurityFrameworkVersionManager() {
        // Initialize with baseline version
        registerInitialVersion("1.0.0-US027");
        logger.info("SecurityFrameworkVersionManager initialized with version registry");
    }

    /**
     * Registers initial baseline version.
     *
     * @param version Initial version
     */
    private void registerInitialVersion(String version) {
        FrameworkVersion frameworkVersion = new FrameworkVersion(
            version,
            null,
            Instant.now(),
            "System",
            "Initial security framework baseline",
            new ArrayList<>()
        );

        versionRegistry.put(version, frameworkVersion);
        logger.info("Registered initial version: {}", version);
    }

    /**
     * Registers a new framework version.
     *
     * @param newVersion New version identifier
     * @param previousVersion Previous version identifier
     */
    public void registerVersion(String newVersion, String previousVersion) {
        logger.info("Registering new version: {} (previous: {})", newVersion, previousVersion);

        if (versionRegistry.containsKey(newVersion)) {
            logger.warn("Version {} already registered, updating metadata", newVersion);
        }

        FrameworkVersion frameworkVersion = new FrameworkVersion(
            newVersion,
            previousVersion,
            Instant.now(),
            "SecurityFrameworkEvolutionManager",
            String.format("Deployed from version %s", previousVersion),
            extractChangelog(previousVersion, newVersion)
        );

        versionRegistry.put(newVersion, frameworkVersion);
        logger.info("Version {} registered successfully", newVersion);
    }

    /**
     * Checks if a version exists in the registry.
     *
     * @param version Version to check
     * @return true if version exists
     */
    public boolean versionExists(String version) {
        boolean exists = versionRegistry.containsKey(version);
        logger.debug("Version {} exists: {}", version, exists);
        return exists;
    }

    /**
     * Gets version metadata.
     *
     * @param version Version identifier
     * @return Optional containing version metadata if found
     */
    public Optional<FrameworkVersion> getVersion(String version) {
        FrameworkVersion frameworkVersion = versionRegistry.get(version);
        if (frameworkVersion == null) {
            logger.warn("Version {} not found in registry", version);
            return Optional.empty();
        }

        logger.debug("Retrieved version metadata for {}", version);
        return Optional.of(frameworkVersion);
    }

    /**
     * Gets all registered versions in chronological order.
     *
     * @return List of all framework versions
     */
    public List<FrameworkVersion> getAllVersions() {
        return new ArrayList<>(versionRegistry.values());
    }

    /**
     * Gets the previous version for a given version.
     *
     * @param currentVersion Current version
     * @return Optional containing previous version if available
     */
    public Optional<String> getPreviousVersion(String currentVersion) {
        return getVersion(currentVersion)
            .map(FrameworkVersion::getPreviousVersion)
            .filter(prev -> prev != null && !prev.isEmpty());
    }

    /**
     * Gets rollback chain up to specified depth.
     *
     * @param currentVersion Current version
     * @param depth Number of versions to traverse back
     * @return List of versions in rollback chain
     */
    public List<String> getRollbackChain(String currentVersion, int depth) {
        List<String> chain = new ArrayList<>();
        String version = currentVersion;

        for (int i = 0; i < depth && version != null; i++) {
            chain.add(version);
            Optional<String> previous = getPreviousVersion(version);
            version = previous.orElse(null);
        }

        logger.debug("Rollback chain for {} (depth {}): {}", currentVersion, depth, chain);
        return chain;
    }

    /**
     * Extracts changelog between versions.
     *
     * @param fromVersion From version
     * @param toVersion To version
     * @return List of changes
     */
    private List<String> extractChangelog(String fromVersion, String toVersion) {
        // In production, this would:
        // 1. Query git repository for commits between versions
        // 2. Extract security-related changes
        // 3. Generate human-readable changelog
        // 4. Include compliance impact assessment

        List<String> changelog = new ArrayList<>();
        changelog.add(String.format("Updated from version %s to %s", fromVersion, toVersion));
        changelog.add("Security framework enhancements applied");
        return changelog;
    }

    /**
     * Framework version metadata.
     */
    public static class FrameworkVersion {
        private final String version;
        private final String previousVersion;
        private final Instant deployedAt;
        private final String deployedBy;
        private final String description;
        private final List<String> changelog;

        public FrameworkVersion(String version, String previousVersion, Instant deployedAt,
                               String deployedBy, String description, List<String> changelog) {
            this.version = version;
            this.previousVersion = previousVersion;
            this.deployedAt = deployedAt;
            this.deployedBy = deployedBy;
            this.description = description;
            this.changelog = new ArrayList<>(changelog);
        }

        public String getVersion() {
            return version;
        }

        public String getPreviousVersion() {
            return previousVersion;
        }

        public Instant getDeployedAt() {
            return deployedAt;
        }

        public String getDeployedBy() {
            return deployedBy;
        }

        public String getDescription() {
            return description;
        }

        public List<String> getChangelog() {
            return new ArrayList<>(changelog);
        }
    }
}
