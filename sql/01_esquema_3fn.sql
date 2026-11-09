-- SCTP · Esquema PostgreSQL (3FN)
-- Uso: psql -U postgres -d sctp -f 01_esquema_3fn.sql   (luego 02, 03 y 04, en ese orden)
DROP VIEW IF EXISTS v_unidades_por_estado, v_licencias_por_vencer, v_llegadas_estimadas, v_incidencias_publicas;
DROP TABLE IF EXISTS incidencia, corrida, parada, conductor, unidad, ruta, usuario CASCADE;
DROP FUNCTION IF EXISTS fn_validar_corrida(), fn_incidencia_grave_a_mantenimiento() CASCADE;
DROP PROCEDURE IF EXISTS sp_cambiar_estado_incidencia(INT, VARCHAR);

CREATE TABLE usuario (
    id_usuario    SERIAL PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    rol           VARCHAR(20)  NOT NULL DEFAULT 'ADMINISTRADOR' CHECK (rol IN ('ADMINISTRADOR')),
    activo        BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE ruta (
    id_ruta SERIAL PRIMARY KEY,
    clave   VARCHAR(20)  NOT NULL UNIQUE,
    nombre  VARCHAR(100) NOT NULL,
    origen  VARCHAR(100) NOT NULL,
    destino VARCHAR(100) NOT NULL,
    estado  VARCHAR(15)  NOT NULL DEFAULT 'ACTIVA' CHECK (estado IN ('ACTIVA','INACTIVA'))
);

CREATE TABLE parada (
    id_parada            SERIAL PRIMARY KEY,
    id_ruta              INT          NOT NULL REFERENCES ruta(id_ruta),
    nombre               VARCHAR(100) NOT NULL,
    orden                INT          NOT NULL CHECK (orden > 0),
    minutos_desde_origen INT          NOT NULL CHECK (minutos_desde_origen >= 0),
    -- Ubicación (Google Maps); NULL = parada aún sin colocar en el mapa
    latitud              NUMERIC(9,6) NULL CHECK (latitud  BETWEEN -90  AND 90),
    longitud             NUMERIC(9,6) NULL CHECK (longitud BETWEEN -180 AND 180),
    UNIQUE (id_ruta, orden)
);

CREATE TABLE unidad (
    id_unidad        SERIAL PRIMARY KEY,
    placa            VARCHAR(15) NOT NULL UNIQUE,
    numero_economico VARCHAR(20) NOT NULL,
    capacidad        INT         NOT NULL CHECK (capacidad > 0),
    estado           VARCHAR(20) NOT NULL DEFAULT 'DISPONIBLE'
                     CHECK (estado IN ('DISPONIBLE','EN_RUTA','MANTENIMIENTO','BAJA'))
);

CREATE TABLE conductor (
    id_conductor      SERIAL PRIMARY KEY,
    nombre            VARCHAR(150) NOT NULL,
    numero_licencia   VARCHAR(30)  NOT NULL UNIQUE,
    vigencia_licencia DATE         NOT NULL,
    telefono          VARCHAR(15)  NOT NULL,
    activo            BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE corrida (
    id_corrida       SERIAL PRIMARY KEY,
    id_ruta          INT NOT NULL REFERENCES ruta(id_ruta),
    id_unidad        INT NOT NULL REFERENCES unidad(id_unidad),
    id_conductor     INT NOT NULL REFERENCES conductor(id_conductor),
    id_parada_actual INT NULL REFERENCES parada(id_parada) ON DELETE SET NULL,
    hora_salida      TIME        NOT NULL,
    -- L=lunes M=martes X=miércoles J=jueves V=viernes S=sábado D=domingo
    dias_operacion   VARCHAR(50) NOT NULL CHECK (dias_operacion ~ '^[LMXJVSD](,[LMXJVSD])*$'),
    activa           BOOLEAN     NOT NULL DEFAULT TRUE
);

CREATE TABLE incidencia (
    id_incidencia  SERIAL PRIMARY KEY,
    id_ruta        INT          NOT NULL REFERENCES ruta(id_ruta),
    id_unidad      INT          NOT NULL REFERENCES unidad(id_unidad),
    id_conductor   INT          NULL REFERENCES conductor(id_conductor),
    descripcion    VARCHAR(300) NOT NULL,
    severidad      VARCHAR(10)  NOT NULL CHECK (severidad IN ('LEVE','MODERADA','GRAVE')),
    estado         VARCHAR(15)  NOT NULL DEFAULT 'ABIERTA' CHECK (estado IN ('ABIERTA','EN_ATENCION','CERRADA')),
    fecha_registro TIMESTAMP    NOT NULL DEFAULT now(),
    fecha_cierre   TIMESTAMP    NULL,
    CHECK ((estado = 'CERRADA') = (fecha_cierre IS NOT NULL))
);
CREATE INDEX idx_incidencia_ruta ON incidencia(id_ruta);
CREATE INDEX idx_incidencia_unidad ON incidencia(id_unidad);
