CREATE TABLE IF NOT EXISTS producto (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS sucursal_producto (
    sucursal_id UUID NOT NULL,
    producto_id UUID NOT NULL,
    stock INTEGER NOT NULL,
    PRIMARY KEY (sucursal_id, producto_id),
    CONSTRAINT fk_sucursal_producto_sucursal
        FOREIGN KEY (sucursal_id)
        REFERENCES sucursal(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_sucursal_producto_producto
        FOREIGN KEY (producto_id)
        REFERENCES producto(id)
        ON DELETE CASCADE,
    CONSTRAINT chk_sucursal_producto_stock_non_negative
        CHECK (stock >= 0)
);

CREATE INDEX IF NOT EXISTS idx_sucursal_producto_producto_id
    ON sucursal_producto (producto_id);

CREATE INDEX IF NOT EXISTS idx_sucursal_producto_sucursal_stock
    ON sucursal_producto (sucursal_id, stock DESC);
