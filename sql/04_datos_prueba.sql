-- Datos de prueba. Administrador: admin / admin123 (cámbiala antes de la demo)
INSERT INTO usuario (username, password_hash) VALUES ('admin', '$2a$10$kGQ7Kbr0NUyoNxmTGYib4uSqk0qIRh2/EiCAVuH4zD3Vb6M8Qizni');

INSERT INTO ruta (clave, nombre, origen, destino, estado) VALUES
 ('R-01','Tianguistenco – Toluca','Terminal Tianguistenco','Terminal Toluca','ACTIVA'),
 ('R-02','Tianguistenco – Santiago Tilapa','Terminal Tianguistenco','Santiago Tilapa','ACTIVA'),
 ('R-03','Ruta de prueba inactiva','Origen','Destino','INACTIVA');

INSERT INTO parada (id_ruta, nombre, orden, minutos_desde_origen) VALUES
 (1,'Terminal Tianguistenco',1,0),(1,'Santiago Tilapa',2,12),(1,'Lerma',3,28),(1,'Terminal Toluca',4,45),
 (2,'Terminal Tianguistenco',1,0),(2,'Mercado',2,8),(2,'Santiago Tilapa',3,15);

-- numero_economico se muestra como "Eco. 14"
INSERT INTO unidad (placa, numero_economico, capacidad, estado) VALUES
 ('ABC-123','14',18,'DISPONIBLE'),('DEF-456','07',18,'EN_RUTA'),('GHI-789','22',24,'DISPONIBLE'),
 ('JKL-012','31',30,'MANTENIMIENTO'),('MNO-345','40',30,'BAJA');

INSERT INTO conductor (nombre, numero_licencia, vigencia_licencia, telefono, activo) VALUES
 ('Conductor Uno','LIC-0001', CURRENT_DATE + 365,'7141000001',TRUE),
 ('Conductor Dos','LIC-0002', CURRENT_DATE + 20, '7141000002',TRUE),
 ('Conductor Tres','LIC-0003',CURRENT_DATE - 10, '7141000003',TRUE),
 ('Conductor Cuatro','LIC-0004',CURRENT_DATE + 200,'7141000004',TRUE);

INSERT INTO corrida (id_ruta, id_unidad, id_conductor, hora_salida, dias_operacion) VALUES
 (1,1,1,'06:00','L,M,X,J,V,S'),(1,1,1,'07:00','L,M,X,J,V,S'),(1,1,1,'08:45','L,M,X,J,V,S'),
 (1,2,2,'06:30','L,M,X,J,V,S'),(1,2,2,'07:30','L,M,X,J,V,S'),(1,2,2,'09:30','L,M,X,J,V,S'),
 (1,3,4,'08:15','L,M,X,J,V,S,D'),
 (2,1,1,'10:00','L,M,X,J,V,S'),(2,2,2,'11:00','L,M,X,J,V,S');

-- Incidencias (la grave cerrada no dispara el trigger: la unidad 31 ya está en MANTENIMIENTO)
INSERT INTO incidencia (id_ruta, id_unidad, id_conductor, descripcion, severidad, estado, fecha_registro, fecha_cierre) VALUES
 (1,1,1,'Retraso por tráfico en Lerma','LEVE','ABIERTA', now() - INTERVAL '2 hours', NULL),
 (1,2,2,'Puerta trasera con falla intermitente','MODERADA','EN_ATENCION', now() - INTERVAL '1 day', NULL),
 (1,4,3,'Falla de frenos','GRAVE','CERRADA', now() - INTERVAL '3 days', now() - INTERVAL '1 day');
