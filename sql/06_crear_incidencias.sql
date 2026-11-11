-- ==============================================================================
-- SCTP · Creación y Verificación de Módulo de Incidencias
-- ==============================================================================

-- 1. Tabla principal de incidencias
CREATE TABLE IF NOT EXISTS incidencia (
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

-- Índices para optimizar búsquedas por ruta y unidad
CREATE INDEX IF NOT EXISTS idx_incidencia_ruta ON incidencia(id_ruta);
CREATE INDEX IF NOT EXISTS idx_incidencia_unidad ON incidencia(id_unidad);

-- 2. Vista pública de incidencias para usuarios y app móvil
CREATE OR REPLACE VIEW v_incidencias_publicas AS
SELECT i.id_incidencia, r.id_ruta, r.clave AS ruta_clave, u.numero_economico,
       i.severidad, i.estado, i.descripcion, i.fecha_registro
FROM incidencia i
JOIN ruta   r ON r.id_ruta   = i.id_ruta
JOIN unidad u ON u.id_unidad = i.id_unidad
WHERE r.estado = 'ACTIVA' AND i.estado <> 'CERRADA';

-- 3. Procedimiento para transición de estados
CREATE OR REPLACE PROCEDURE sp_cambiar_estado_incidencia(p_id INT, p_estado VARCHAR)
LANGUAGE plpgsql AS $$
DECLARE
    v_actual VARCHAR;
BEGIN
    SELECT estado INTO v_actual FROM incidencia WHERE id_incidencia = p_id FOR UPDATE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Incidencia no encontrada';
    END IF;
    IF NOT ((v_actual = 'ABIERTA'     AND p_estado = 'EN_ATENCION')
         OR (v_actual = 'EN_ATENCION' AND p_estado = 'CERRADA')) THEN
        RAISE EXCEPTION 'Transición no permitida: % → %', v_actual, p_estado;
    END IF;
    UPDATE incidencia
       SET estado = p_estado,
           fecha_cierre = CASE WHEN p_estado = 'CERRADA' THEN now() ELSE NULL END
     WHERE id_incidencia = p_id;
END;
$$;

-- 4. Trigger: incidencia grave pasa unidad a MANTENIMIENTO
CREATE OR REPLACE FUNCTION fn_incidencia_grave_a_mantenimiento() RETURNS trigger AS $$
BEGIN
    UPDATE unidad SET estado = 'MANTENIMIENTO'
     WHERE id_unidad = NEW.id_unidad AND estado IN ('DISPONIBLE','EN_RUTA');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_incidencia_grave ON incidencia;
CREATE TRIGGER trg_incidencia_grave
AFTER INSERT OR UPDATE OF severidad, estado ON incidencia
FOR EACH ROW
WHEN (NEW.severidad = 'GRAVE' AND NEW.estado <> 'CERRADA')
EXECUTE FUNCTION fn_incidencia_grave_a_mantenimiento();

-- 5. Datos iniciales de prueba (solo si la tabla está vacía)
INSERT INTO incidencia (id_ruta, id_unidad, id_conductor, descripcion, severidad, estado, fecha_registro, fecha_cierre)
SELECT (SELECT id_ruta FROM ruta ORDER BY id_ruta LIMIT 1),
       (SELECT id_unidad FROM unidad ORDER BY id_unidad LIMIT 1),
       (SELECT id_conductor FROM conductor ORDER BY id_conductor LIMIT 1),
       'Retraso por congestión vehicular en zona Lerma', 'LEVE', 'ABIERTA', now() - INTERVAL '2 hours', NULL
WHERE EXISTS (SELECT 1 FROM ruta) AND EXISTS (SELECT 1 FROM unidad)
  AND NOT EXISTS (SELECT 1 FROM incidencia);

INSERT INTO incidencia (id_ruta, id_unidad, id_conductor, descripcion, severidad, estado, fecha_registro, fecha_cierre)
SELECT (SELECT id_ruta FROM ruta ORDER BY id_ruta LIMIT 1),
       (SELECT id_unidad FROM unidad ORDER BY id_unidad OFFSET 1 LIMIT 1),
       (SELECT id_conductor FROM conductor ORDER BY id_conductor OFFSET 1 LIMIT 1),
       'Falla mecánica preventiva en sistema de frenos', 'GRAVE', 'EN_ATENCION', now() - INTERVAL '5 hours', NULL
WHERE (SELECT count(*) FROM unidad) >= 2
  AND (SELECT count(*) FROM incidencia) = 1;

INSERT INTO incidencia (id_ruta, id_unidad, id_conductor, descripcion, severidad, estado, fecha_registro, fecha_cierre)
SELECT (SELECT id_ruta FROM ruta ORDER BY id_ruta DESC LIMIT 1),
       (SELECT id_unidad FROM unidad ORDER BY id_unidad DESC LIMIT 1),
       (SELECT id_conductor FROM conductor ORDER BY id_conductor DESC LIMIT 1),
       'Desvío temporal por obras en tramo Ocoyoacac', 'MODERADA', 'ABIERTA', now() - INTERVAL '30 minutes', NULL
WHERE EXISTS (SELECT 1 FROM ruta) AND EXISTS (SELECT 1 FROM unidad)
  AND (SELECT count(*) FROM incidencia) = 2;
