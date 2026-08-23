CREATE SCHEMA IF NOT EXISTS tienda;

CREATE TABLE tienda.clientes (
    id          BIGSERIAL PRIMARY KEY,
    nombre      VARCHAR(200) NOT NULL,
    email       VARCHAR(200) NOT NULL UNIQUE,
    direccion   VARCHAR(500) NOT NULL
);

CREATE TABLE tienda.productos (
    id          BIGSERIAL PRIMARY KEY,
    nombre      VARCHAR(200) NOT NULL,
    descripcion TEXT,
    precio      NUMERIC(12,2) NOT NULL CHECK (precio > 0),
    stock       INTEGER NOT NULL DEFAULT 0 CHECK (stock >= 0)
);

CREATE TABLE tienda.pedidos (
    id          BIGSERIAL PRIMARY KEY,
    cliente_id  BIGINT NOT NULL REFERENCES tienda.clientes(id),
    fecha       TIMESTAMP NOT NULL DEFAULT NOW(),
    estado      VARCHAR(50) NOT NULL DEFAULT 'CREADO',
    total       NUMERIC(12,2) NOT NULL
);

CREATE TABLE tienda.items_pedido (
    id              BIGSERIAL PRIMARY KEY,
    pedido_id       BIGINT NOT NULL REFERENCES tienda.pedidos(id),
    producto_id     BIGINT NOT NULL REFERENCES tienda.productos(id),
    cantidad        INTEGER NOT NULL CHECK (cantidad > 0),
    precio_unitario NUMERIC(12,2) NOT NULL
);

CREATE INDEX idx_pedidos_cliente_id ON tienda.pedidos(cliente_id);
CREATE INDEX idx_items_pedido_pedido_id ON tienda.items_pedido(pedido_id);
CREATE INDEX idx_items_pedido_producto_id ON tienda.items_pedido(producto_id);
