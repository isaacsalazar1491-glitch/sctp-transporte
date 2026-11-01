-- Triggers: (1) validación de corridas, (2) incidencia grave → unidad en MANTENIMIENTO

-- (1) Unidad operativa, conductor activo con licencia vigente, parada actual de la misma ruta.
CREATE OR REPLACE FUNCTION fn_validar_corrida() RETURNS trigger AS $$
DECLARE
    v_estado   VARCHAR;
    v_activo   BOOLEAN;
    v_vigencia DATE;
    v_ruta     INT;
BEGIN
    IF NEW.activa THEN
        SELECT estado INTO v_estado FROM unidad WHERE id_unidad = NEW.id_unidad;
        IF v_estado NOT IN ('DISPONIBLE','EN_RUTA') THEN
            RAISE EXCEPTION 'La unidad no está disponible (estado: %)', v_estado;
        END IF;

        SELECT activo, vigencia_licencia INTO v_activo, v_vigencia
          FROM conductor WHERE id_conductor = NEW.id_conductor;
        IF NOT v_activo THEN
            RAISE EXCEPTION 'El conductor está dado de baja';
        END IF;
        IF v_vigencia < CURRENT_DATE THEN
            RAISE EXCEPTION 'La licencia del conductor venció el %', v_vigencia;
        END IF;
    END IF;

    IF NEW.id_parada_actual IS NOT NULL THEN
        SELECT id_ruta INTO v_ruta FROM parada WHERE id_parada = NEW.id_parada_actual;
        IF v_ruta <> NEW.id_ruta THEN
            RAISE EXCEPTION 'La parada actual no pertenece a la ruta de la corrida';
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_validar_corrida ON corrida;
CREATE TRIGGER trg_validar_corrida
BEFORE INSERT OR UPDATE OF id_ruta, id_unidad, id_conductor, id_parada_actual, activa ON corrida
FOR EACH ROW EXECUTE FUNCTION fn_validar_corrida();

-- (2) Una incidencia GRAVE no cerrada saca la unidad de servicio (deja de aparecer como disponible).
--     Una unidad dada de baja no cambia de estado.
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
