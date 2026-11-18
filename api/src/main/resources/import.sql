-- Catalogos
INSERT INTO sector_economico (codigo, nombre, activo, creado_en) VALUES ('COMERCIO', 'Comercio al por mayor y menor', true, now());

INSERT INTO categoria_riesgo (codigo, descripcion, tasa_descuento_anual, tasa_mora_anual, plazo_maximo_dias, activo, creado_en) VALUES ('A', 'Riesgo bajo', 0.1800, 0.2400, 120, true, now());

INSERT INTO rol (nombre, descripcion, activo, creado_en) VALUES ('ADMIN', 'Jefatura del area de factoraje', true, now());
INSERT INTO rol (nombre, descripcion, activo, creado_en) VALUES ('ASESOR', 'Ejecutivo de banca empresas', true, now());
INSERT INTO rol (nombre, descripcion, activo, creado_en) VALUES ('CLIENTE', 'Una empresa del sistema', true, now());

-- Usuarios (contraseñas BCrypt)
INSERT INTO usuario (username, password_hash, correo, activo, creado_en) VALUES ('asesor1', '$2a$10$M7q938GI5HkmxArkYrmS9Ofg3TDSQmIXEgSRv0s5EvwCALhZVSJnu', 'asesor1@banco.com', true, now());
INSERT INTO usuario (username, password_hash, correo, activo, creado_en) VALUES ('panaderia', '$2a$10$ZXN6TaiJLaP8hrXKeW4EK.heaQ1R6Lso5BMp0ZXKqGCKwaG3oi652', 'contacto@panaderia.com', true, now());
INSERT INTO usuario (username, password_hash, correo, activo, creado_en) VALUES ('admin1', '$2a$10$RMGGE3fML.rnw.FnaZXJ9udviLkaMq/QLY4mTwrFk.OSZCXUeygDS', 'admin1@banco.com', true, now());
-- Credenciales demo: admin1/Admin2026, asesor1/Asesor2026, panaderia/Cliente2026

INSERT INTO usuario_rol (usuario_id, rol_id, asignado_en) VALUES (1, 2, now());  -- asesor1   -> ASESOR
INSERT INTO usuario_rol (usuario_id, rol_id, asignado_en) VALUES (2, 3, now());  -- panaderia -> CLIENTE
INSERT INTO usuario_rol (usuario_id, rol_id, asignado_en) VALUES (3, 1, now());  -- admin1    -> ADMIN

-- Ejecutivo
INSERT INTO ejecutivo (codigo_empleado, nombres, apellidos, usuario_id, activo, creado_en) VALUES ('EMP-001', 'Carla', 'Mejia', 1, true, now());

-- Empresas: id 1 cedente (con cuenta y usuario), id 2 pagadora (sin cuenta ni usuario)
INSERT INTO empresa (nit, nrc, nombre_legal, nombre_comercial, correo, telefono, direccion, cuenta_bancaria, sector_economico_id, usuario_id, activo, creado_en) VALUES ('0614-250190-101-5', '123456-7', 'Panaderia La Espiga S.A. de C.V.', 'La Espiga', 'contacto@panaderia.com', '2250-1010', 'San Salvador', '001234567890', 1, 2, true, now());
INSERT INTO empresa (nit, nrc, nombre_legal, nombre_comercial, correo, telefono, direccion, cuenta_bancaria, sector_economico_id, usuario_id, activo, creado_en) VALUES ('0614-100180-102-3', '765432-1', 'Supermercados del Valle S.A. de C.V.', 'SuperValle', 'pagos@supervalle.com', '2260-2020', 'San Salvador', NULL, 1, NULL, true, now());

-- Cupo del pagador (id 2), monto 50,000, vence en 180 dias
INSERT INTO cupo_pagador (numero_cupo, empresa_pagadora_id, categoria_riesgo_id, aprobado_por_usuario_id, monto_aprobado, monto_disponible, estado, fecha_aprobacion, fecha_vencimiento, creado_en) VALUES ('CUP-2026-001', 2, 1, 1, 50000.00, 50000.00, 'ACTIVO', current_date, current_date + 180, now());

-- Facturas CONFIRMADAS de la cedente (1) a la pagadora (2). Descuentos esperados: 150.00, 45.00, 90.00
INSERT INTO factura (codigo_generacion, sello_recepcion, monto_total, fecha_emision, fecha_vencimiento, empresa_cedente_id, empresa_pagadora_id, estado, fecha_confirmacion, creado_en) VALUES ('DTE-0001-0001-0001-000000000000001', 'SELLO-0001', 5000.00, current_date, current_date + 60, 1, 2, 'CONFIRMADA', now(), now());
INSERT INTO factura (codigo_generacion, sello_recepcion, monto_total, fecha_emision, fecha_vencimiento, empresa_cedente_id, empresa_pagadora_id, estado, fecha_confirmacion, creado_en) VALUES ('DTE-0001-0001-0001-000000000000002', 'SELLO-0002', 3000.00, current_date, current_date + 30, 1, 2, 'CONFIRMADA', now(), now());
INSERT INTO factura (codigo_generacion, sello_recepcion, monto_total, fecha_emision, fecha_vencimiento, empresa_cedente_id, empresa_pagadora_id, estado, fecha_confirmacion, creado_en) VALUES ('DTE-0001-0001-0001-000000000000003', 'SELLO-0003', 2000.00, current_date, current_date + 90, 1, 2, 'CONFIRMADA', now(), now());
-- F4: REGISTRADA, para la prueba de R1
INSERT INTO factura (codigo_generacion, sello_recepcion, monto_total, fecha_emision, fecha_vencimiento, empresa_cedente_id, empresa_pagadora_id, estado, fecha_confirmacion, creado_en) VALUES ('DTE-0001-0001-0001-000000000000004', 'SELLO-0004', 1500.00, current_date, current_date + 60, 1, 2, 'REGISTRADA', NULL, now());
-- F5 y F6: para el caso bueno. Descuentos esperados: 90.00 y 10.00
INSERT INTO factura (codigo_generacion, sello_recepcion, monto_total, fecha_emision, fecha_vencimiento, empresa_cedente_id, empresa_pagadora_id, estado, fecha_confirmacion, creado_en) VALUES ('DTE-0001-0001-0001-000000000000005', 'SELLO-0005', 4000.00, current_date, current_date + 45, 1, 2, 'CONFIRMADA', now(), now());
INSERT INTO factura (codigo_generacion, sello_recepcion, monto_total, fecha_emision, fecha_vencimiento, empresa_cedente_id, empresa_pagadora_id, estado, fecha_confirmacion, creado_en) VALUES ('DTE-0001-0001-0001-000000000000006', 'SELLO-0006', 1000.00, current_date, current_date + 20, 1, 2, 'CONFIRMADA', now(), now());

-- NUEVOS REGISTROS PARA LA PRUEBA DE SEGURIDAD (FACTURA AJENA) ---
INSERT INTO empresa (nit, nrc, nombre_legal, nombre_comercial, correo, telefono, direccion, cuenta_bancaria, sector_economico_id, usuario_id, activo, creado_en) VALUES ('0614-300985-103-1', '987654-3', 'Empresa Externa S.A. de C.V.', 'Externa', 'contacto@externa.com', '2270-3030', 'San Salvador', '009876543210', 1, NULL, true, now());
INSERT INTO factura (codigo_generacion, sello_recepcion, monto_total, fecha_emision, fecha_vencimiento, empresa_cedente_id, empresa_pagadora_id, estado, fecha_confirmacion, creado_en) VALUES ('DTE-0001-0001-0001-000000000000020', 'SELLO-0020', 2500.00, current_date, current_date + 30, 3, 2, 'CONFIRMADA', now(), now());