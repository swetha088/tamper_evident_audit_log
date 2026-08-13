package com.schwab.interview.audit;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "audit_events", indexes = {
        @Index(name = "idx_audit_actor_sequence", columnList = "actorId,sequence"),
        @Index(name = "idx_audit_resource_sequence", columnList = "resourceType,resourceId,sequence"),
        @Index(name = "idx_audit_recorded_sequence", columnList = "recordedAt,sequence")
})
public class AuditEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long sequence;
    @Column(nullable = false, unique = true, updatable = false) private String eventId;
    @Column(nullable = false, updatable = false) private String eventType;
    @Column(nullable = false, updatable = false) private String actorId;
    @Column(nullable = false, updatable = false) private String resourceType;
    @Column(nullable = false, updatable = false) private String resourceId;
    @Column(updatable = false) private Instant occurredAt;
    @Column(nullable = false, updatable = false) private Instant recordedAt;
    @Lob @Column(nullable = false) private String payloadJson;
    @Lob @Column(nullable = false, updatable = false) private String commitmentsJson;
    @Column(nullable = false, updatable = false, length = 64) private String previousHash;
    @Column(nullable = false, length = 64) private String contentHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private EventStatus status = EventStatus.ACTIVE;
    private Instant archivedAt;
    private String archivePolicy;
    @Lob private String redactionCertificateJson;

    protected AuditEvent() { }
    public AuditEvent(String eventId, String eventType, String actorId, String resourceType, String resourceId, Instant occurredAt, Instant recordedAt, String payloadJson, String commitmentsJson, String previousHash, String contentHash) {
        this.eventId = eventId; this.eventType = eventType; this.actorId = actorId; this.resourceType = resourceType; this.resourceId = resourceId;
        this.occurredAt = occurredAt; this.recordedAt = recordedAt; this.payloadJson = payloadJson; this.commitmentsJson = commitmentsJson;
        this.previousHash = previousHash; this.contentHash = contentHash;
    }
    public Long getSequence() { return sequence; } public String getEventId() { return eventId; } public String getEventType() { return eventType; }
    public String getActorId() { return actorId; } public String getResourceType() { return resourceType; } public String getResourceId() { return resourceId; }
    public Instant getOccurredAt() { return occurredAt; } public Instant getRecordedAt() { return recordedAt; } public String getPayloadJson() { return payloadJson; }
    public String getCommitmentsJson() { return commitmentsJson; } public String getPreviousHash() { return previousHash; } public String getContentHash() { return contentHash; }
    public EventStatus getStatus() { return status; } public Instant getArchivedAt() { return archivedAt; } public String getArchivePolicy() { return archivePolicy; } public String getRedactionCertificateJson() { return redactionCertificateJson; }
    public void replacePayloadForRedaction(String payloadJson, String certificateJson) { this.payloadJson = payloadJson; this.redactionCertificateJson = certificateJson; this.status = EventStatus.REDACTED; }
    void setContentHash(String contentHash) { this.contentHash = contentHash; }
    public void archive(Instant at, String policy) { this.status = EventStatus.ARCHIVED; this.archivedAt = at; this.archivePolicy = policy; }
    public enum EventStatus { ACTIVE, REDACTED, ARCHIVED }
}
