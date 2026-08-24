-- Seed clientes en su schema
SET search_path TO svc_clientes;

INSERT INTO clientes (nombre, email, direccion) VALUES
('Ana García', 'ana.garcia@mail.com', 'Av. Corrientes 1234, CABA'),
('Bruno López', 'bruno.lopez@mail.com', 'Calle Florida 567, CABA'),
('Carla Martínez', 'carla.martinez@mail.com', 'Av. Santa Fe 890, CABA'),
('Diego Rodríguez', 'diego.rodriguez@mail.com', 'Calle Lavalle 321, CABA'),
('Elena Fernández', 'elena.fernandez@mail.com', 'Av. de Mayo 654, CABA'),
('Franco Gómez', 'franco.gomez@mail.com', 'Calle Reconquista 987, CABA'),
('Gabriela Díaz', 'gabriela.diaz@mail.com', 'Av. Belgrano 147, CABA'),
('Hugo Pérez', 'hugo.perez@mail.com', 'Calle Tucumán 258, CABA'),
('Inés Sánchez', 'ines.sanchez@mail.com', 'Av. Callao 369, CABA'),
('Juan Torres', 'juan.torres@mail.com', 'Calle Viamonte 741, CABA'),
('Karina Romero', 'karina.romero@mail.com', 'Av. Rivadavia 852, CABA'),
('Lucas Moreno', 'lucas.moreno@mail.com', 'Calle Suipacha 963, CABA'),
('María Álvarez', 'maria.alvarez@mail.com', 'Av. Pueyrredón 174, CABA'),
('Nicolás Ruiz', 'nicolas.ruiz@mail.com', 'Calle Esmeralda 285, CABA'),
('Olivia Castro', 'olivia.castro@mail.com', 'Av. Las Heras 396, CABA'),
('Pablo Herrera', 'pablo.herrera@mail.com', 'Calle Maipú 417, CABA'),
('Romina Flores', 'romina.flores@mail.com', 'Av. Independencia 528, CABA'),
('Sebastián Acosta', 'sebastian.acosta@mail.com', 'Calle San Martín 639, CABA'),
('Tamara Medina', 'tamara.medina@mail.com', 'Av. Córdoba 750, CABA'),
('Ulises Vargas', 'ulises.vargas@mail.com', 'Calle Paraguay 861, CABA');

-- Seed productos en su schema
SET search_path TO svc_productos;

INSERT INTO productos (nombre, descripcion, precio, stock) VALUES
('Notebook 15"', 'Laptop 15 pulgadas 8GB RAM', 450000.00, 999999),
('Monitor 24"', 'Monitor LED Full HD', 180000.00, 999999),
('Teclado mecánico', 'Teclado mecánico retroiluminado', 45000.00, 999999),
('Mouse inalámbrico', 'Mouse ergonómico 2.4GHz', 15000.00, 999999),
('Auriculares BT', 'Auriculares Bluetooth con ANC', 85000.00, 999999),
('Webcam HD', 'Cámara web 1080p con micrófono', 32000.00, 999999),
('Hub USB-C', 'Hub 7 puertos USB-C', 28000.00, 999999),
('SSD 1TB', 'Disco sólido NVMe 1TB', 95000.00, 999999),
('RAM 16GB', 'Módulo DDR4 3200MHz', 55000.00, 999999),
('Fuente 750W', 'Fuente certificada 80+ Gold', 72000.00, 999999),
('Gabinete ATX', 'Gabinete mid-tower con vidrio', 65000.00, 999999),
('Placa de video', 'GPU 8GB GDDR6', 380000.00, 999999),
('Procesador 8 núcleos', 'CPU 8 cores 16 threads', 290000.00, 999999),
('Motherboard ATX', 'Placa madre chipset B650', 165000.00, 999999),
('Cooler CPU', 'Disipador tower con 2 ventiladores', 42000.00, 999999),
('Cable HDMI 2m', 'Cable HDMI 2.1 4K', 8000.00, 999999),
('Mousepad XL', 'Alfombrilla 90x40cm', 12000.00, 999999),
('Soporte monitor', 'Brazo articulado VESA', 35000.00, 999999),
('UPS 1000VA', 'Sistema de alimentación ininterrumpida', 120000.00, 999999),
('Pendrive 64GB', 'USB 3.0 64GB', 7500.00, 999999),
('Disco externo 2TB', 'HDD portátil USB 3.0', 68000.00, 999999),
('Switch red 8p', 'Switch Gigabit 8 puertos', 25000.00, 999999),
('Cable red Cat6 3m', 'Cable UTP categoría 6', 3500.00, 999999),
('Router WiFi 6', 'Router dual band AX1800', 55000.00, 999999),
('Cargador USB-C 65W', 'Cargador GaN compacto', 22000.00, 999999),
('Adaptador DisplayPort', 'DP a HDMI activo', 9500.00, 999999),
('Limpiador pantallas', 'Kit limpieza LCD/LED', 4500.00, 999999),
('Mochila notebook', 'Mochila acolchada 15.6"', 28000.00, 999999),
('Lámpara escritorio', 'Lámpara LED regulable USB', 18000.00, 999999),
('Organizador cables', 'Kit organización de cables', 6000.00, 999999);
