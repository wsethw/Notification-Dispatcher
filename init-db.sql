-- Create notification_audit table
CREATE TABLE IF NOT EXISTS notification_audit (
    id BIGSERIAL PRIMARY KEY,
    notification_id VARCHAR(36) NOT NULL UNIQUE,
    client_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    channel VARCHAR(50) NOT NULL,
    subject VARCHAR(500),
    body VARCHAR(2000),
    idempotency_key VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Create indexes for performance
CREATE INDEX idx_notification_audit_client_id ON notification_audit(client_id);
CREATE INDEX idx_notification_audit_user_id ON notification_audit(user_id);
CREATE INDEX idx_notification_audit_status ON notification_audit(status);
CREATE INDEX idx_notification_audit_created_at ON notification_audit(created_at);

-- Create audit function to update updated_at
CREATE OR REPLACE FUNCTION update_notification_audit_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for automatic timestamp update
DROP TRIGGER IF EXISTS notification_audit_timestamp_trigger ON notification_audit;
CREATE TRIGGER notification_audit_timestamp_trigger
BEFORE UPDATE ON notification_audit
FOR EACH ROW
EXECUTE FUNCTION update_notification_audit_timestamp();

-- Grant permissions
GRANT ALL PRIVILEGES ON TABLE notification_audit TO postgres;
GRANT USAGE, SELECT ON SEQUENCE notification_audit_id_seq TO postgres;

COMMIT;