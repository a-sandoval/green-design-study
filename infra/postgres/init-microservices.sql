-- Schema aislado para clientes-service
CREATE SCHEMA IF NOT EXISTS svc_clientes;
SET search_path TO svc_clientes;

CREATE TABLE svc_clientes.clientes (
    id          BIGSERIAL PRIMARY KEY,
    nombre      VARCHAR(200) NOT NULL,
    email       VARCHAR(200) NOT NULL UNIQUE,
    direccion   VARCHAR(500) NOT NULL
);

-- Schema aislado para productos-service
CREATE SCHEMA IF NOT EXISTS svc_productos;
SET search_path TO svc_productos;

CREATE TABLE svc_productos.productos (
    id          BIGSERIAL PRIMARY KEY,
    nombre      VARCHAR(200) NOT NULL,
    descripcion TEXT,
    precio      NUMERIC(12,2) NOT NULL CHECK (precio > 0),
    stock       INTEGER NOT NULL DEFAULT 0 CHECK (stock >= 0)
);

-- Schema aislado para pedidos-service (sin FK cruzadas)
CREATE SCHEMA IF NOT EXISTS svc_pedidos;
SET search_path TO svc_pedidos;

CREATE TABLE svc_pedidos.pedidos (
    id          BIGSERIAL PRIMARY KEY,
    cliente_id  BIGINT NOT NULL,  -- sin FK: el servicio de clientes es la fuente de verdad
    fecha       TIMESTAMP NOT NULL DEFAULT NOW(),
    estado      VARCHAR(50) NOT NULL DEFAULT 'CREADO',
    total       NUMERIC(12,2) NOT NULL
);

CREATE TABLE svc_pedidos.items_pedido (
    id              BIGSERIAL PRIMARY KEY,
    pedido_id       BIGINT NOT NULL REFERENCES svc_pedidos.pedidos(id),
    producto_id     BIGINT NOT NULL,  -- sin FK: el servicio de productos es la fuente de verdad
    cantidad        INTEGER NOT NULL CHECK (cantidad > 0),
    precio_unitario NUMERIC(12,2) NOT NULL
);

CREATE INDEX idx_pedidos_cliente_id ON svc_pedidos.pedidos(cliente_id);
CREATE INDEX idx_items_pedido_pedido_id ON svc_pedidos.items_pedido(pedido_id);
CREATE INDEX idx_items_pedido_producto_id ON svc_pedidos.items_pedido(producto_id);
