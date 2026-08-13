ALTER TABLE audit_events ADD COLUMN lifecycle_signature VARCHAR(64) NULL;
ALTER TABLE audit_events ADD COLUMN lifecycle_signature_key_id VARCHAR(128) NULL;
