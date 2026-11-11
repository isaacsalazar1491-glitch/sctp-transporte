package com.sctp.transporte.util;

import com.sctp.transporte.config.ConexionBD;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

public class MigracionBD
{
    // aplica columnas latitud y longitud a parada y asegura esquema de incidencias
    public static void main(String[] args)
    {
        verificarYMigrar();
    }

    public static void verificarYMigrar()
    {
        try (Connection con = ConexionBD.obtenerConexion();
             Statement st = con.createStatement())
        {
            DatabaseMetaData meta = con.getMetaData();

            // 1. verificar y agregar columnas geograficas a parada
            st.execute("ALTER TABLE parada ADD COLUMN IF NOT EXISTS latitud NUMERIC(9,6) NULL CHECK (latitud BETWEEN -90 AND 90)");
            st.execute("ALTER TABLE parada ADD COLUMN IF NOT EXISTS longitud NUMERIC(9,6) NULL CHECK (longitud BETWEEN -180 AND 180)");

            st.execute("UPDATE parada SET latitud = 19.1789, longitud = -99.4663 WHERE nombre = 'Terminal Tianguistenco' AND latitud IS NULL");
            st.execute("UPDATE parada SET latitud = 19.2257, longitud = -99.4846 WHERE nombre = 'Santiago Tilapa' AND latitud IS NULL");
            st.execute("UPDATE parada SET latitud = 19.2847, longitud = -99.5122 WHERE nombre = 'Lerma' AND latitud IS NULL");
            st.execute("UPDATE parada SET latitud = 19.2900, longitud = -99.6560 WHERE nombre = 'Terminal Toluca' AND latitud IS NULL");
            st.execute("UPDATE parada SET latitud = 19.1830, longitud = -99.4700 WHERE nombre = 'Mercado' AND latitud IS NULL");

            // 2. verificar y crear tabla de incidencias si no existe
            boolean existeIncidencia = false;
            try (ResultSet rs = meta.getTables(null, "public", "incidencia", new String[]{"TABLE"}))
            {
                if (rs.next())
                {
                    existeIncidencia = true;
                }
            }

            if (!existeIncidencia)
            {
                st.execute("CREATE TABLE IF NOT EXISTS incidencia ("
                        + "    id_incidencia  SERIAL PRIMARY KEY,"
                        + "    id_ruta        INT          NOT NULL REFERENCES ruta(id_ruta),"
                        + "    id_unidad      INT          NOT NULL REFERENCES unidad(id_unidad),"
                        + "    id_conductor   INT          NULL REFERENCES conductor(id_conductor),"
                        + "    descripcion    VARCHAR(300) NOT NULL,"
                        + "    severidad      VARCHAR(10)  NOT NULL CHECK (severidad IN ('LEVE','MODERADA','GRAVE')),"
                        + "    estado         VARCHAR(15)  NOT NULL DEFAULT 'ABIERTA' CHECK (estado IN ('ABIERTA','EN_ATENCION','CERRADA')),"
                        + "    fecha_registro TIMESTAMP    NOT NULL DEFAULT now(),"
                        + "    fecha_cierre   TIMESTAMP    NULL,"
                        + "    CHECK ((estado = 'CERRADA') = (fecha_cierre IS NOT NULL))"
                        + ")");

                st.execute("CREATE INDEX IF NOT EXISTS idx_incidencia_ruta ON incidencia(id_ruta)");
                st.execute("CREATE INDEX IF NOT EXISTS idx_incidencia_unidad ON incidencia(id_unidad)");
            }

            // 3. vista publica de incidencias
            st.execute("CREATE OR REPLACE VIEW v_incidencias_publicas AS "
                    + "SELECT i.id_incidencia, r.id_ruta, r.clave AS ruta_clave, u.numero_economico, "
                    + "       i.severidad, i.estado, i.descripcion, i.fecha_registro "
                    + "FROM incidencia i "
                    + "JOIN ruta   r ON r.id_ruta   = i.id_ruta "
                    + "JOIN unidad u ON u.id_unidad = i.id_unidad "
                    + "WHERE r.estado = 'ACTIVA' AND i.estado <> 'CERRADA'");

            // 4. procedimiento para transicion de estados
            st.execute("CREATE OR REPLACE PROCEDURE sp_cambiar_estado_incidencia(p_id INT, p_estado VARCHAR) "
                    + "LANGUAGE plpgsql AS $$ "
                    + "DECLARE "
                    + "    v_actual VARCHAR; "
                    + "BEGIN "
                    + "    SELECT estado INTO v_actual FROM incidencia WHERE id_incidencia = p_id FOR UPDATE; "
                    + "    IF NOT FOUND THEN "
                    + "        RAISE EXCEPTION 'Incidencia no encontrada'; "
                    + "    END IF; "
                    + "    IF NOT ((v_actual = 'ABIERTA'     AND p_estado = 'EN_ATENCION') "
                    + "         OR (v_actual = 'EN_ATENCION' AND p_estado = 'CERRADA')) THEN "
                    + "        RAISE EXCEPTION 'Transicion no permitida: % -> %', v_actual, p_estado; "
                    + "    END IF; "
                    + "    UPDATE incidencia "
                    + "       SET estado = p_estado, "
                    + "           fecha_cierre = CASE WHEN p_estado = 'CERRADA' THEN now() ELSE NULL END "
                    + "     WHERE id_incidencia = p_id; "
                    + "END; "
                    + "$$;");

            // 5. trigger para incidencias graves a mantenimiento
            st.execute("CREATE OR REPLACE FUNCTION fn_incidencia_grave_a_mantenimiento() RETURNS trigger AS $$ "
                    + "BEGIN "
                    + "    UPDATE unidad SET estado = 'MANTENIMIENTO' "
                    + "     WHERE id_unidad = NEW.id_unidad AND estado IN ('DISPONIBLE','EN_RUTA'); "
                    + "    RETURN NEW; "
                    + "END; "
                    + "$$ LANGUAGE plpgsql;");

            st.execute("DROP TRIGGER IF EXISTS trg_incidencia_grave ON incidencia;");
            st.execute("CREATE TRIGGER trg_incidencia_grave "
                    + "AFTER INSERT OR UPDATE OF severidad, estado ON incidencia "
                    + "FOR EACH ROW "
                    + "WHEN (NEW.severidad = 'GRAVE' AND NEW.estado <> 'CERRADA') "
                    + "EXECUTE FUNCTION fn_incidencia_grave_a_mantenimiento();");

            // 6. datos iniciales si no hay ninguna incidencia
            try (ResultSet rs = st.executeQuery("SELECT count(*) FROM incidencia"))
            {
                if (rs.next() && rs.getInt(1) == 0)
                {
                    st.execute("INSERT INTO incidencia (id_ruta, id_unidad, id_conductor, descripcion, severidad, estado, fecha_registro, fecha_cierre) "
                            + "SELECT (SELECT id_ruta FROM ruta ORDER BY id_ruta LIMIT 1), "
                            + "       (SELECT id_unidad FROM unidad ORDER BY id_unidad LIMIT 1), "
                            + "       (SELECT id_conductor FROM conductor ORDER BY id_conductor LIMIT 1), "
                            + "       'Retraso por congestion vehicular en zona Lerma', 'LEVE', 'ABIERTA', now() - INTERVAL '2 hours', NULL "
                            + "WHERE EXISTS (SELECT 1 FROM ruta) AND EXISTS (SELECT 1 FROM unidad)");

                    st.execute("INSERT INTO incidencia (id_ruta, id_unidad, id_conductor, descripcion, severidad, estado, fecha_registro, fecha_cierre) "
                            + "SELECT (SELECT id_ruta FROM ruta ORDER BY id_ruta LIMIT 1), "
                            + "       (SELECT id_unidad FROM unidad ORDER BY id_unidad OFFSET 1 LIMIT 1), "
                            + "       (SELECT id_conductor FROM conductor ORDER BY id_conductor OFFSET 1 LIMIT 1), "
                            + "       'Falla mecanica preventiva en sistema de frenos', 'GRAVE', 'EN_ATENCION', now() - INTERVAL '5 hours', NULL "
                            + "WHERE (SELECT count(*) FROM unidad) >= 2");

                    st.execute("INSERT INTO incidencia (id_ruta, id_unidad, id_conductor, descripcion, severidad, estado, fecha_registro, fecha_cierre) "
                            + "SELECT (SELECT id_ruta FROM ruta ORDER BY id_ruta DESC LIMIT 1), "
                            + "       (SELECT id_unidad FROM unidad ORDER BY id_unidad DESC LIMIT 1), "
                            + "       (SELECT id_conductor FROM conductor ORDER BY id_conductor DESC LIMIT 1), "
                            + "       'Desvio temporal por obras en tramo Ocoyoacac', 'MODERADA', 'ABIERTA', now() - INTERVAL '30 minutes', NULL "
                            + "WHERE EXISTS (SELECT 1 FROM ruta) AND EXISTS (SELECT 1 FROM unidad)");
                }
            }
        }
        catch (Exception e)
        {
            System.err.println("Aviso durante la verificacion/migracion: " + e.getMessage());
        }
    }
}
