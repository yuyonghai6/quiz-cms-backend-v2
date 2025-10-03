package com.quizfun.questionbank.application.security;

import java.time.Instant;

/**
 * Domain model representing session context information for security validation.
 *
 * This immutable class captures session metadata required for detecting
 * session hijacking attempts and other session-based security threats.
 *
 * Enhanced for US-024: Session Hijacking Detection System
 *
 * @see SessionSecurityValidator
 */
public class SessionContext {

    private final String sessionId;
    private final Long userId;
    private final String clientIp;
    private final String userAgent;
    private final Instant sessionCreatedAt;
    private final Instant lastAccessedAt;

    private SessionContext(Builder builder) {
        this.sessionId = builder.sessionId;
        this.userId = builder.userId;
        this.clientIp = builder.clientIp;
        this.userAgent = builder.userAgent;
        this.sessionCreatedAt = builder.sessionCreatedAt != null ? builder.sessionCreatedAt : Instant.now();
        this.lastAccessedAt = builder.lastAccessedAt != null ? builder.lastAccessedAt : Instant.now();
    }

    public String getSessionId() {
        return sessionId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getClientIp() {
        return clientIp;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public Instant getSessionCreatedAt() {
        return sessionCreatedAt;
    }

    public Instant getLastAccessedAt() {
        return lastAccessedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder pattern for constructing immutable SessionContext instances.
     */
    public static class Builder {
        private String sessionId;
        private Long userId;
        private String clientIp;
        private String userAgent;
        private Instant sessionCreatedAt;
        private Instant lastAccessedAt;

        private Builder() {
        }

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder userId(Long userId) {
            this.userId = userId;
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

        public Builder sessionCreatedAt(Instant sessionCreatedAt) {
            this.sessionCreatedAt = sessionCreatedAt;
            return this;
        }

        public Builder lastAccessedAt(Instant lastAccessedAt) {
            this.lastAccessedAt = lastAccessedAt;
            return this;
        }

        /**
         * Builds the SessionContext with validation.
         *
         * @return A new immutable SessionContext instance
         * @throws IllegalArgumentException if required fields are missing
         */
        public SessionContext build() {
            if (sessionId == null || sessionId.trim().isEmpty()) {
                throw new IllegalArgumentException("Session ID is required");
            }
            if (userId == null) {
                throw new IllegalArgumentException("User ID is required");
            }
            return new SessionContext(this);
        }
    }

    @Override
    public String toString() {
        return "SessionContext{" +
                "sessionId='" + sessionId + '\'' +
                ", userId=" + userId +
                ", clientIp='" + clientIp + '\'' +
                ", sessionCreatedAt=" + sessionCreatedAt +
                ", lastAccessedAt=" + lastAccessedAt +
                '}';
    }
}
