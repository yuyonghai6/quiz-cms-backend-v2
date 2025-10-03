package com.quizfun.questionbank.infrastructure.persistence.documents;

import com.quizfun.questionbank.application.security.SecurityEvent;
import com.quizfun.questionbank.application.security.SecurityEventType;
import com.quizfun.questionbank.application.security.SeverityLevel;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

/**
 * MongoDB document for persisting security audit events.
 *
 * This document follows the existing repository patterns from US-005 and provides
 * comprehensive audit trails for compliance requirements (GDPR, FERPA).
 *
 * Collection: security_events
 *
 * Indexes:
 * - eventType: For filtering by security event type
 * - userId: For user-specific security queries
 * - timestamp: For temporal queries and data retention
 * - severity: For severity-based alerting and reporting
 * - requestId: For correlating with application logs
 *
 * @see SecurityEvent
 * @see com.quizfun.questionbank.application.security.SecurityAuditLogger
 */
@Document("security_events")
public class SecurityEventDocument {

    @Id
    private ObjectId id;

    @Indexed
    private SecurityEventType eventType;

    @Indexed
    private Long userId;

    private String sessionId;

    @Indexed
    private Instant timestamp;

    @Indexed
    private SeverityLevel severity;

    /**
     * Additional event details stored as flexible key-value pairs.
     * Examples:
     * - attemptedUserId: The user ID attempted to be accessed
     * - tokenUserId: The user ID from JWT token
     * - endpoint: The API endpoint accessed
     * - ipAddress: Client IP address (duplicated here for details)
     */
    private Map<String, Object> details;

    @Indexed
    private String requestId;

    private String clientIp;

    private String userAgent;

    /**
     * SHA-256 checksum for tamper detection and audit trail integrity.
     * Calculated from: eventType + userId + timestamp + requestId
     */
    private String checksumHash;

    /**
     * Date when PII (Personally Identifiable Information) should be anonymized.
     * Supports GDPR and FERPA compliance requirements.
     */
    @Indexed
    private Instant anonymizationDate;

    /**
     * Date when the record should be deleted per retention policy.
     * Typically 7 years for compliance purposes.
     */
    @Indexed
    private Instant retentionExpiryDate;

    // Default constructor for MongoDB
    public SecurityEventDocument() {
    }

    // Getters and Setters
    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public SecurityEventType getEventType() {
        return eventType;
    }

    public void setEventType(SecurityEventType eventType) {
        this.eventType = eventType;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public SeverityLevel getSeverity() {
        return severity;
    }

    public void setSeverity(SeverityLevel severity) {
        this.severity = severity;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getChecksumHash() {
        return checksumHash;
    }

    public void setChecksumHash(String checksumHash) {
        this.checksumHash = checksumHash;
    }

    public Instant getAnonymizationDate() {
        return anonymizationDate;
    }

    public void setAnonymizationDate(Instant anonymizationDate) {
        this.anonymizationDate = anonymizationDate;
    }

    public Instant getRetentionExpiryDate() {
        return retentionExpiryDate;
    }

    public void setRetentionExpiryDate(Instant retentionExpiryDate) {
        this.retentionExpiryDate = retentionExpiryDate;
    }

    /**
     * Converts a SecurityEvent domain model to MongoDB document.
     *
     * @param event The security event to convert
     * @return SecurityEventDocument ready for MongoDB persistence
     */
    public static SecurityEventDocument fromSecurityEvent(SecurityEvent event) {
        SecurityEventDocument document = new SecurityEventDocument();
        document.setEventType(event.getType());
        document.setUserId(event.getUserId());
        document.setSessionId(event.getSessionId());
        document.setTimestamp(event.getTimestamp());
        document.setSeverity(event.getSeverity());
        document.setDetails(event.getDetails());
        document.setRequestId(event.getRequestId());
        document.setClientIp(event.getClientIp());
        document.setUserAgent(event.getUserAgent());

        // Set compliance dates
        // Anonymize PII after 90 days
        document.setAnonymizationDate(event.getTimestamp().plusSeconds(90 * 24 * 60 * 60));
        // Retain for 7 years for compliance
        document.setRetentionExpiryDate(event.getTimestamp().plusSeconds(7 * 365 * 24 * 60 * 60));

        return document;
    }

    @Override
    public String toString() {
        return "SecurityEventDocument{" +
                "id=" + id +
                ", eventType=" + eventType +
                ", userId=" + userId +
                ", sessionId='" + sessionId + '\'' +
                ", timestamp=" + timestamp +
                ", severity=" + severity +
                ", requestId='" + requestId + '\'' +
                ", clientIp='" + clientIp + '\'' +
                '}';
    }
}
