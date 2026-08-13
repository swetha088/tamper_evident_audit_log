CREATE TABLE audit_events (
    sequence BIGINT NOT NULL AUTO_INCREMENT,
    event_id VARCHAR(36) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    actor_id VARCHAR(255) NOT NULL,
    resource_type VARCHAR(255) NOT NULL,
    resource_id VARCHAR(255) NOT NULL,
    occurred_at TIMESTAMP NULL,
    recorded_at TIMESTAMP NOT NULL,
    payload_json CLOB NOT NULL,
    commitments_json CLOB NOT NULL,
    previous_hash VARCHAR(64) NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    archived_at TIMESTAMP NULL,
    archive_policy VARCHAR(255) NULL,
    redaction_certificate_json CLOB NULL,
    CONSTRAINT pk_audit_events PRIMARY KEY (sequence),
    CONSTRAINT uk_audit_events_event_id UNIQUE (event_id)
);

CREATE INDEX idx_audit_actor_sequence ON audit_events (actor_id, sequence);
CREATE INDEX idx_audit_resource_sequence ON audit_events (resource_type, resource_id, sequence);
CREATE INDEX idx_audit_recorded_sequence ON audit_events (recorded_at, sequence);

CREATE TABLE audit_chain_state (
    id BIGINT NOT NULL,
    head_sequence BIGINT NOT NULL,
    head_event_id VARCHAR(36) NOT NULL,
    head_hash VARCHAR(64) NOT NULL,
    CONSTRAINT pk_audit_chain_state PRIMARY KEY (id)
);
