ALTER TABLE audit_chain_state ADD COLUMN head_signature VARCHAR(64) NULL;
ALTER TABLE audit_chain_state ADD COLUMN signature_key_id VARCHAR(128) NULL;
