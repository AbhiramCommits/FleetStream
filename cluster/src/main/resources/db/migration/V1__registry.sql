CREATE TABLE IF NOT EXISTS device (
    device_id VARCHAR(255) PRIMARY KEY,
    site_id VARCHAR(255) NOT NULL,
    model VARCHAR(255) NOT NULL,
    rated_kw DOUBLE PRECISION NOT NULL,
    registered_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS command_audit (
    command_id VARCHAR(255) PRIMARY KEY,
    device_id VARCHAR(255) NOT NULL,
    kind VARCHAR(50) NOT NULL,
    payload JSONB NOT NULL,
    issued_at TIMESTAMP NOT NULL,
    acked_at TIMESTAMP,
    attempts INT NOT NULL,
    status VARCHAR(50) NOT NULL CHECK (status IN ('pending','acked','failed'))
);
