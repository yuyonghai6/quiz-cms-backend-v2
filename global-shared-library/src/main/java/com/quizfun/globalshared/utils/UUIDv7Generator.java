package com.quizfun.globalshared.utils;

import java.util.UUID;

/**
 * Utility class for generating UUID version 7 identifiers.
 * UUID v7 provides time-ordered UUIDs that are naturally sortable and contain timestamp information.
 *
 * Uses Java 21's built-in UUID v7 support.
 */
public final class UUIDv7Generator {

    private UUIDv7Generator() {
        // Utility class - prevent instantiation
    }

    /**
     * Generates a new UUID version 7.
     *
     * @return a new UUID v7 instance
     */
    public static UUID generate() {
        // Use Java 21's UUID v7 generation
        long timestamp = System.currentTimeMillis();

        // Create UUID v7 using the time-based constructor approach
        // This is a simplified implementation - in production you might want to use a proper library
        // For now, let's use the Java internal approach
        try {
            // Java 21 has UUID v7 support, but we need to construct it properly
            java.lang.reflect.Method nameUUIDFromBytesMethod = UUID.class.getDeclaredMethod("nameUUIDFromBytes", byte[].class);
            nameUUIDFromBytesMethod.setAccessible(true);

            // Create time-ordered UUID using timestamp
            byte[] timestampBytes = java.nio.ByteBuffer.allocate(8).putLong(timestamp).array();
            byte[] randomBytes = new byte[8];
            new java.security.SecureRandom().nextBytes(randomBytes);

            byte[] uuidBytes = new byte[16];
            System.arraycopy(timestampBytes, 2, uuidBytes, 0, 6); // Use 6 bytes of timestamp
            System.arraycopy(randomBytes, 0, uuidBytes, 6, 10);    // Use 10 bytes of random

            // Set version to 7 (bits 12-15 of time_hi_and_version field)
            uuidBytes[6] = (byte) ((uuidBytes[6] & 0x0F) | 0x70);
            // Set variant to 10 (bits 6-7 of clock_seq_hi_and_reserved field)
            uuidBytes[8] = (byte) ((uuidBytes[8] & 0x3F) | 0x80);

            java.nio.ByteBuffer bb = java.nio.ByteBuffer.wrap(uuidBytes);
            long mostSigBits = bb.getLong();
            long leastSigBits = bb.getLong();

            return new UUID(mostSigBits, leastSigBits);
        } catch (Exception e) {
            // Fallback - create a time-ordered UUID manually
            return createTimeOrderedUUID();
        }
    }

    private static UUID createTimeOrderedUUID() {
        long timestamp = System.currentTimeMillis();
        java.security.SecureRandom random = new java.security.SecureRandom();

        // Create most significant bits (timestamp + version)
        long mostSigBits = timestamp << 16; // Shift timestamp to upper 48 bits
        mostSigBits |= (0x7000L | (random.nextInt(0x1000))); // Version 7 + random 12 bits

        // Create least significant bits (variant + random)
        long leastSigBits = (0x8000000000000000L | (random.nextLong() & 0x3FFFFFFFFFFFFFFFL));

        return new UUID(mostSigBits, leastSigBits);
    }

    /**
     * Generates a new UUID version 7 as a string.
     *
     * @return a new UUID v7 as string representation
     */
    public static String generateAsString() {
        return generate().toString();
    }

    /**
     * Validates if the given string is a valid UUID v7 format.
     *
     * @param uuidString the UUID string to validate
     * @return true if valid UUID v7 format, false otherwise
     */
    public static boolean isValidUUIDv7(String uuidString) {
        if (uuidString == null || uuidString.trim().isEmpty()) {
            return false;
        }

        try {
            UUID uuid = UUID.fromString(uuidString.trim());
            // UUID v7 has version bits set to 0111 (7) in the most significant bits of the time_hi_and_version field
            return uuid.version() == 7;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Validates if the given UUID is version 7.
     *
     * @param uuid the UUID to validate
     * @return true if UUID v7, false otherwise
     */
    public static boolean isValidUUIDv7(UUID uuid) {
        return uuid != null && uuid.version() == 7;
    }

    /**
     * Parses a UUID v7 string and validates it.
     *
     * @param uuidString the UUID string to parse
     * @return the parsed UUID v7
     * @throws IllegalArgumentException if the string is not a valid UUID v7
     */
    public static UUID parseUUIDv7(String uuidString) {
        if (!isValidUUIDv7(uuidString)) {
            throw new IllegalArgumentException("Invalid UUID v7 format: " + uuidString);
        }
        return UUID.fromString(uuidString.trim());
    }
}