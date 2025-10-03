package com.quizfun.questionbank.application.security;

import java.time.Instant;
import java.util.Map;

/**
 * Domain model representing a security event for audit logging.
 *
 * This class encapsulates all information required for comprehensive security
 * audit trails and compliance requirements (GDPR, FERPA).
 *
 * Immutable by design to ensure audit trail integrity.
 *
 * @see SecurityEventType
 * @see SeverityLevel
 * @see SecurityAuditLogger
 */
public class SecurityEvent {

    private final SecurityEventType type;
    private final Long userId;
    private final String sessionId;
    private final Instant timestamp;
    private final SeverityLevel severity;
    private final Map<String, Object> details;
    private final String requestId;
    private final String clientIp;
    private final String userAgent;

    private SecurityEvent(Builder builder) {
        this.type = builder.type;
        this.userId = builder.userId;
        this.sessionId = builder.sessionId;
        this.timestamp = builder.timestamp != null ? builder.timestamp : Instant.now();
        this.severity = builder.severity;
        this.details = builder.details != null ? Map.copyOf(builder.details) : Map.of();
        this.requestId = builder.requestId;
        this.clientIp = builder.clientIp;
        this.userAgent = builder.userAgent;
    }

    // Getters
    public SecurityEventType getType() {
        return type;
    }

    public Long getUserId() {
        return userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public SeverityLevel getSeverity() {
        return severity;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getClientIp() {
        return clientIp;
    }

    public String getUserAgent() {
        return userAgent;
    }

    /**
     * Creates a new Builder for constructing SecurityEvent instances.
     *
     * @return A new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder pattern for constructing immutable SecurityEvent instances.
     */
    public static class Builder {
        private SecurityEventType type;
        private Long userId;
        private String sessionId;
        private Instant timestamp;
        private SeverityLevel severity;
        private Map<String, Object> details;
        private String requestId;
        private String clientIp;
        private String userAgent;

        private Builder() {
        }

        public Builder type(SecurityEventType type) {
            this.type = type;
            return this;
        }

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder severity(SeverityLevel severity) {
            this.severity = severity;
            return this;
        }

        public Builder details(Map<String, Object> details) {
            this.details = details;
            return this;
        }

        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public Builder clientIp(String clientIp) {
            this.clientIp = clientIp;
            return this;
        }

        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        /**
         * Builds the SecurityEvent with validation.
         *
         * @return A new immutable SecurityEvent instance
         * @throws IllegalArgumentException if required fields are missing
         */
        public SecurityEvent build() {
            if (type == null) {
                throw new IllegalArgumentException("SecurityEventType is required");
            }
            if (severity == null) {
                throw new IllegalArgumentException("SeverityLevel is required");
            }
            return new SecurityEvent(this);
        }
    }

    @Override
    public String toString() {
        return "SecurityEvent{" +
                "type=" + type +
                ", userId=" + userId +
                ", sessionId='" + sessionId + '\'' +
                ", timestamp=" + timestamp +
                ", severity=" + severity +
                ", requestId='" + requestId + '\'' +
                ", clientIp='" + clientIp + '\'' +
                '}';
    }
}
