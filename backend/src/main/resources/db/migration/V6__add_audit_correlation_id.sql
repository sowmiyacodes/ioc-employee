ALTER TABLE audit_events ADD COLUMN correlation_id VARCHAR(36);

CREATE INDEX idx_audit_correlation_id ON audit_events(correlation_id);
