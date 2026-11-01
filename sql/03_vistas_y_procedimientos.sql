-- Vistas públicas / de reportes y procedimiento almacenado

-- Llegadas estimadas: hora de salida + minutos desde el origen. SIEMPRE es una estimación por horario.
-- Solo corridas activas, de rutas activas y con unidad operativa. No expone datos del conductor.
CREATE OR REPLACE VIEW v_llegadas_estimadas AS
SELECT r.id_ruta, r.clave AS ruta_clave, p.id_parada, p.nombre AS parada, p.orden,
       c.id_corrida, u.numero_economico, u.capacidad, u.estado AS estado_unidad,
       c.hora_salida, c.dias_operacion, p.minutos_desde_origen,
       (EXTRACT(HOUR FROM c.hora_salida) * 60 + EXTRACT(MINUTE FROM c.hora_salida)
         + p.minutos_desde_origen)::int AS minuto_llegada,
       (c.hora_salida + p.minutos_desde_origen * INTERVAL '1 minute') AS hora_llegada_estimada
FROM corrida c
JOIN ruta   r ON r.id_ruta   = c.id_ruta
JOIN parada p ON p.id_ruta   = c.id_ruta
JOIN unidad u ON u.id_unidad = c.id_unidad
WHERE c.activa AND r.estado = 'ACTIVA' AND u.estado IN ('DISPONIBLE','EN_RUTA');

-- Resumen público de incidencias (sin conductor).
CREATE OR REPLACE VIEW v_incidencias_publicas AS
SELECT i.id_incidencia, r.id_ruta, r.clave AS ruta_clave, u.numero_economico,
       i.severidad, i.estado, i.descripcion, i.fecha_registro
FROM incidencia i
JOIN ruta   r ON r.id_ruta   = i.id_ruta
JOIN unidad u ON u.id_unidad = i.id_unidad
WHERE r.estado = 'ACTIVA' AND i.estado <> 'CERRADA';

-- Reportes del administrador
CREATE OR REPLACE VIEW v_unidades_por_estado AS
SELECT e.estado, COUNT(u.id_unidad) AS total
FROM (VALUES ('DISPONIBLE'), ('EN_RUTA'), ('MANTENIMIENTO'), ('BAJA')) AS e(estado)
LEFT JOIN unidad u ON u.estado = e.estado
GROUP BY e.estado;

CREATE OR REPLACE VIEW v_licencias_por_vencer AS
SELECT id_conductor, nombre, numero_licencia, vigencia_licencia,
       (vigencia_licencia - CURRENT_DATE) AS dias_restantes
FROM conductor
WHERE activo AND vigencia_licencia <= CURRENT_DATE + 30
ORDER BY vigencia_licencia;

-- Procedimiento almacenado: seguimiento de una incidencia (ABIERTA → EN_ATENCION → CERRADA).
-- No permite retroceder ni saltar estados; al cerrar registra la fecha de cierre.
-- Una unidad enviada a MANTENIMIENTO por incidencia grave NO se reactiva sola: el administrador la libera.
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
