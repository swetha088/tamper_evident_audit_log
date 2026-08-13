package com.schwab.interview.audit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_chain_state")
public class AuditChainStateEntity {
    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id;

    @Column(name = "head_sequence", nullable = false)
    private Long headSequence;

    @Column(name = "head_event_id", nullable = false, length = 36)
    private String headEventId;

    @Column(name = "head_hash", nullable = false, length = 64)
    private String headHash;

    protected AuditChainStateEntity() {
    }

    public AuditChainStateEntity(AuditEventEntity event) {
        this.id = SINGLETON_ID;
        advance(event);
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

    public void advance(AuditEventEntity event) {
        this.headSequence = event.getSequence();
        this.headEventId = event.getEventId();
        this.headHash = event.getContentHash();
    }
}
