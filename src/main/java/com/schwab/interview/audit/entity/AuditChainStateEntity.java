package com.schwab.interview.audit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_chain_state")
public class AuditChainStateEntity {
  public static final long SINGLETON_ID = 1L;

  @Id private Long id;

  @Column(name = "head_sequence", nullable = false)
  private Long headSequence;

  @Column(name = "head_event_id", nullable = false, length = 36)
  private String headEventId;

  @Column(name = "head_hash", nullable = false, length = 64)
  private String headHash;

  @Column(name = "head_signature", length = 64)
  private String headSignature;

  @Column(name = "signature_key_id", length = 128)
  private String signatureKeyId;

  protected AuditChainStateEntity() {}

  public AuditChainStateEntity(AuditEventEntity event, String signature, String keyId) {
    this.id = SINGLETON_ID;
    advance(event, signature, keyId);
  }

  public Long getHeadSequence() {
    return headSequence;
  }

  public String getHeadEventId() {
    return headEventId;
  }

  public String getHeadHash() {
    return headHash;
  }

  public String getHeadSignature() {
    return headSignature;
  }

  public String getSignatureKeyId() {
    return signatureKeyId;
  }

  public void advance(AuditEventEntity event, String signature, String keyId) {
    this.headSequence = event.getSequence();
    this.headEventId = event.getEventId();
    this.headHash = event.getContentHash();
    this.headSignature = signature;
    this.signatureKeyId = keyId;
  }
}
