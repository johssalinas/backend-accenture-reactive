CREATE TABLE IF NOT EXISTS idempotency_key (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id VARCHAR(120) NOT NULL,
    idempotency_key VARCHAR(120) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL,
    response_payload TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_idempotency_key_client_key
    ON idempotency_key (client_id, idempotency_key);
