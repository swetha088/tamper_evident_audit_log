package com.schwab.interview.audit.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
    name = "audit_events",
    indexes = {
      @Index(name = "idx_audit_actor_sequence", columnList = "actor_id,sequence"),
      @Index(
          name = "idx_audit_resource_sequence",
          columnList = "resource_type,resource_id,sequence"),
      @Index(name = "idx_audit_recorded_sequence", columnList = "recorded_at,sequence")
    })
public class AuditEventEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long sequence;

  @Column(name = "event_id", nullable = false, unique = true, updatable = false)
  private String eventId;

  @Column(name = "event_type", nullable = false, updatable = false)
  private String eventType;

  @Column(name = "actor_id", nullable = false, updatable = false)
  private String actorId;

  @Column(name = "resource_type", nullable = false, updatable = false)
  private String resourceType;

  @Column(name = "resource_id", nullable = false, updatable = false)
  private String resourceId;

  @Column(name = "occurred_at", updatable = false)
  private Instant occurredAt;

  @Column(name = "recorded_at", nullable = false, updatable = false)
  private Instant recordedAt;

  @Lob
  @Column(name = "payload_json", nullable = false)
  private String payloadJson;

  @Lob
  @Column(name = "commitments_json", nullable = false, updatable = false)
  private String commitmentsJson;

  @Column(name = "previous_hash", nullable = false, updatable = false, length = 64)
  private String previousHash;

  @Column(name = "content_hash", nullable = false, length = 64)
  private String contentHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Status status = Status.ACTIVE;

  @Column(name = "archived_at")
  private Instant archivedAt;

  @Column(name = "archive_policy")
  private String archivePolicy;

  @Lob
  @Column(name = "redaction_certificate_json")
  private String redactionCertificateJson;

  @Column(name = "lifecycle_signature", length = 64)
  private String lifecycleSignature;

  @Column(name = "lifecycle_signature_key_id", length = 128)
  private String lifecycleSignatureKeyId;

  protected AuditEventEntity() {}

  public AuditEventEntity(
      String eventId,
      String eventType,
      String actorId,
      String resourceType,
      String resourceId,
      Instant occurredAt,
      Instant recordedAt,
      String payloadJson,
      String commitmentsJson,
      String previousHash) {
    this.eventId = eventId;
    this.eventType = eventType;
    this.actorId = actorId;
    this.resourceType = resourceType;
    this.resourceId = resourceId;
    this.occurredAt = occurredAt;
    this.recordedAt = recordedAt;
    this.payloadJson = payloadJson;
    this.commitmentsJson = commitmentsJson;
    this.previousHash = previousHash;
    this.contentHash = "PENDING";
  }

  public Long getSequence() {
    return sequence;
  }

  public String getEventId() {
    return eventId;
  }

  public String getEventType() {
    return eventType;
  }

  public String getActorId() {
    return actorId;
  }

  public String getResourceType() {
    return resourceType;
  }

  public String getResourceId() {
    return resourceId;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public Instant getRecordedAt() {
    return recordedAt;
  }

  public String getPayloadJson() {
    return payloadJson;
  }

  public String getCommitmentsJson() {
    return commitmentsJson;
  }

  public String getPreviousHash() {
    return previousHash;
  }

  public String getContentHash() {
    return contentHash;
  }

  public Status getStatus() {
    return status;
  }

  public Instant getArchivedAt() {
    return archivedAt;
  }

  public String getArchivePolicy() {
    return archivePolicy;
  }

  public String getRedactionCertificateJson() {
    return redactionCertificateJson;
  }

  public String getLifecycleSignature() {
    return lifecycleSignature;
  }

  public String getLifecycleSignatureKeyId() {
    return lifecycleSignatureKeyId;
  }

  public void setContentHash(String value) {
    contentHash = value;
  }

  public void signLifecycle(String signature, String keyId) {
    lifecycleSignature = signature;
    lifecycleSignatureKeyId = keyId;
  }

  public void redact(String payload, String certificate) {
    payloadJson = payload;
    redactionCertificateJson = certificate;
    status = Status.REDACTED;
  }

  public void archive(Instant timestamp, String policy) {
    status = Status.ARCHIVED;
    archivedAt = timestamp;
    archivePolicy = policy;
  }

  public enum Status {
    ACTIVE,
    REDACTED,
    ARCHIVED
  }
}
