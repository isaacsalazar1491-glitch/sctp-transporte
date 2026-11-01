package com.sctp.transporte.dao;

import com.sctp.transporte.model.Incidencia;
import com.sctp.transporte.model.IncidenciaDetalle;
import com.sctp.transporte.model.enums.EstadoIncidencia;
import com.sctp.transporte.model.enums.SeveridadIncidencia;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class IncidenciaDAO {
    private static final String COLS = "i.id_incidencia, i.id_ruta, i.id_unidad, i.id_conductor, i.descripcion, "
            + "i.severidad, i.estado, i.fecha_registro, i.fecha_cierre";
    private final Connection c;

    public IncidenciaDAO(Connection c) { this.c = c; }

    private static Incidencia map(ResultSet rs) throws SQLException {
        return new Incidencia(rs.getInt("id_incidencia"), rs.getInt("id_ruta"), rs.getInt("id_unidad"),
                Jdbc.nullableInt(rs, "id_conductor"), rs.getString("descripcion"),
                SeveridadIncidencia.valueOf(rs.getString("severidad")), EstadoIncidencia.valueOf(rs.getString("estado")),
                rs.getObject("fecha_registro", LocalDateTime.class), rs.getObject("fecha_cierre", LocalDateTime.class));
    }

    private static IncidenciaDetalle mapDetalle(ResultSet rs) throws SQLException {
        return new IncidenciaDetalle(map(rs), rs.getString("ruta_clave"), rs.getString("numero_economico"),
                rs.getString("conductor_nombre"));
    }

    /** Todas las incidencias (más recientes primero); filtra por estado si no es null. */
    public List<IncidenciaDetalle> listarDetalle(EstadoIncidencia estado) throws SQLException {
        String sql = "SELECT " + COLS + ", r.clave AS ruta_clave, u.numero_economico, d.nombre AS conductor_nombre "
                + "FROM incidencia i JOIN ruta r ON r.id_ruta = i.id_ruta JOIN unidad u ON u.id_unidad = i.id_unidad "
                + "LEFT JOIN conductor d ON d.id_conductor = i.id_conductor "
                + (estado != null ? "WHERE i.estado = ? " : "")
                + "ORDER BY i.fecha_registro DESC, i.id_incidencia DESC";
        return estado != null ? Jdbc.query(c, sql, IncidenciaDAO::mapDetalle, estado)
                              : Jdbc.query(c, sql, IncidenciaDAO::mapDetalle);
    }

    public Optional<Incidencia> buscar(int id) throws SQLException {
        return Jdbc.one(c, "SELECT " + COLS + " FROM incidencia i WHERE i.id_incidencia = ?", IncidenciaDAO::map, id);
    }

    /** El trigger trg_incidencia_grave pasa la unidad a MANTENIMIENTO si la incidencia es GRAVE. */
    public int crear(Incidencia i) throws SQLException {
        return Jdbc.insert(c, "INSERT INTO incidencia (id_ruta, id_unidad, id_conductor, descripcion, severidad, estado) "
                + "VALUES (?,?,?,?,?,'ABIERTA') RETURNING id_incidencia",
                i.idRuta(), i.idUnidad(), i.idConductor(), i.descripcion(), i.severidad());
    }

    /** Edita los datos de la incidencia, no su estado (el estado solo cambia con el procedimiento almacenado). */
    public int actualizar(Incidencia i) throws SQLException {
        return Jdbc.update(c, "UPDATE incidencia SET id_ruta=?, id_unidad=?, id_conductor=?, descripcion=?, severidad=? WHERE id_incidencia=?",
                i.idRuta(), i.idUnidad(), i.idConductor(), i.descripcion(), i.severidad(), i.id());
    }

    /** Procedimiento almacenado: valida la transición y registra la fecha de cierre. */
    public void cambiarEstado(int id, EstadoIncidencia nuevo) throws SQLException {
        Jdbc.update(c, "CALL sp_cambiar_estado_incidencia(?, ?)", id, nuevo.name());
    }
}
