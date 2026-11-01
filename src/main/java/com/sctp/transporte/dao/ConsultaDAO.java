package com.sctp.transporte.dao;

import com.sctp.transporte.model.IncidenciaPublica;
import com.sctp.transporte.model.LicenciaPorVencer;
import com.sctp.transporte.model.Llegada;
import com.sctp.transporte.model.ResumenIncidenciasRuta;
import com.sctp.transporte.model.ResumenUnidades;
import com.sctp.transporte.model.Salida;
import com.sctp.transporte.model.enums.EstadoIncidencia;
import com.sctp.transporte.model.enums.SeveridadIncidencia;
import com.sctp.transporte.model.enums.EstadoUnidad;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Consultas de solo lectura para la zona pública y los reportes. Nunca exponen datos del conductor. */
public class ConsultaDAO {
    private final Connection c;

    public ConsultaDAO(Connection c) { this.c = c; }

    /** Corridas activas de una ruta activa cuya unidad está operativa. */
    public List<Salida> salidasActivas(int idRuta) throws SQLException {
        return Jdbc.query(c,
                "SELECT c.id_corrida, u.numero_economico, u.capacidad, u.estado, c.hora_salida, c.dias_operacion "
                + "FROM corrida c JOIN unidad u ON u.id_unidad = c.id_unidad JOIN ruta r ON r.id_ruta = c.id_ruta "
                + "WHERE c.id_ruta = ? AND c.activa AND u.estado IN ('DISPONIBLE','EN_RUTA') AND r.estado = 'ACTIVA' "
                + "ORDER BY c.hora_salida, u.numero_economico",
                rs -> new Salida(rs.getInt("id_corrida"), rs.getString("numero_economico"), rs.getInt("capacidad"),
                        EstadoUnidad.valueOf(rs.getString("estado")),
                        rs.getObject("hora_salida", LocalTime.class), rs.getString("dias_operacion")),
                idRuta);
    }

    /** Llegadas estimadas a una parada (vista v_llegadas_estimadas); {@code enMinutos} lo calcula el servicio. */
    public List<Llegada> llegadasAParada(int idParada) throws SQLException {
        return Jdbc.query(c,
                "SELECT id_corrida, numero_economico, capacidad, estado_unidad, hora_salida, dias_operacion, minuto_llegada "
                + "FROM v_llegadas_estimadas WHERE id_parada = ? AND minuto_llegada < 1440 ORDER BY minuto_llegada, numero_economico",
                rs -> {
                    Salida s = new Salida(rs.getInt("id_corrida"), rs.getString("numero_economico"), rs.getInt("capacidad"),
                            EstadoUnidad.valueOf(rs.getString("estado_unidad")), rs.getObject("hora_salida", LocalTime.class), rs.getString("dias_operacion"));
                    int m = rs.getInt("minuto_llegada"); // < 1440: no se muestran llegadas que pasan de medianoche
                    return new Llegada(s, LocalTime.of(m / 60, m % 60), -1); // enMinutos lo calcula el servicio
                }, idParada);
    }

    /** Ids de rutas que tienen una parada cuyo nombre contiene el texto (% y _ se tratan como texto). */
    public Set<Integer> rutasConParada(String texto) throws SQLException {
        String patron = "%" + texto.toLowerCase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        return new HashSet<>(Jdbc.query(c, "SELECT DISTINCT id_ruta FROM parada WHERE lower(nombre) LIKE ?",
                rs -> rs.getInt(1), patron));
    }

    /** Unidades por estado (vista v_unidades_por_estado). */
    public ResumenUnidades resumenUnidades() throws SQLException {
        long[] t = new long[4];
        Jdbc.query(c, "SELECT estado, total FROM v_unidades_por_estado", rs -> {
            switch (EstadoUnidad.valueOf(rs.getString(1))) {
                case DISPONIBLE -> t[0] = rs.getLong(2);
                case EN_RUTA -> t[1] = rs.getLong(2);
                case MANTENIMIENTO -> t[2] = rs.getLong(2);
                case BAJA -> t[3] = rs.getLong(2);
            }
            return null;
        });
        return new ResumenUnidades(t[0], t[1], t[2], t[3]);
    }

    /** Licencias de conductores activos que vencen en 30 días o ya vencieron (vista v_licencias_por_vencer). */
    public List<LicenciaPorVencer> licenciasPorVencer() throws SQLException {
        return Jdbc.query(c, "SELECT nombre, vigencia_licencia, dias_restantes FROM v_licencias_por_vencer",
                rs -> new LicenciaPorVencer(rs.getString("nombre"), rs.getObject("vigencia_licencia", LocalDate.class),
                        rs.getInt("dias_restantes")));
    }

    /** Incidencias no cerradas visibles al público (vista v_incidencias_publicas); idRuta null = todas. */
    public List<IncidenciaPublica> incidenciasPublicas(Integer idRuta) throws SQLException {
        String sql = "SELECT id_incidencia, id_ruta, ruta_clave, numero_economico, severidad, estado, descripcion, fecha_registro "
                + "FROM v_incidencias_publicas " + (idRuta != null ? "WHERE id_ruta = ? " : "")
                + "ORDER BY fecha_registro DESC";
        Jdbc.RowMapper<IncidenciaPublica> m = rs -> new IncidenciaPublica(rs.getInt("id_incidencia"), rs.getInt("id_ruta"),
                rs.getString("ruta_clave"), rs.getString("numero_economico"),
                SeveridadIncidencia.valueOf(rs.getString("severidad")), EstadoIncidencia.valueOf(rs.getString("estado")),
                rs.getString("descripcion"), rs.getObject("fecha_registro", LocalDateTime.class));
        return idRuta != null ? Jdbc.query(c, sql, m, idRuta) : Jdbc.query(c, sql, m);
    }

    /** Resumen por ruta activa (incluye rutas sin incidencias, con ceros). */
    public List<ResumenIncidenciasRuta> resumenIncidenciasPorRuta() throws SQLException {
        return Jdbc.query(c,
                "SELECT r.id_ruta, r.clave, r.nombre, "
                + "COUNT(*) FILTER (WHERE i.estado = 'ABIERTA') AS abiertas, "
                + "COUNT(*) FILTER (WHERE i.estado = 'EN_ATENCION') AS en_atencion, "
                + "COUNT(*) FILTER (WHERE i.severidad = 'GRAVE') AS graves "
                + "FROM ruta r LEFT JOIN v_incidencias_publicas i ON i.id_ruta = r.id_ruta "
                + "WHERE r.estado = 'ACTIVA' GROUP BY r.id_ruta, r.clave, r.nombre ORDER BY r.clave",
                rs -> new ResumenIncidenciasRuta(rs.getInt("id_ruta"), rs.getString("clave"), rs.getString("nombre"),
                        rs.getLong("abiertas"), rs.getLong("en_atencion"), rs.getLong("graves")));
    }
}
