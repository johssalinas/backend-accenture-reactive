CREATE TABLE IF NOT EXISTS sucursal (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    franquicia_id UUID NOT NULL,
    version BIGINT DEFAULT 0,
    CONSTRAINT fk_sucursal_franquicia
        FOREIGN KEY (franquicia_id)
        REFERENCES franquicia(id)
        ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_sucursal_franquicia_id
    ON sucursal (franquicia_id);
