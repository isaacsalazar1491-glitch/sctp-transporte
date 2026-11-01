package com.sctp.transporte.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import com.sctp.transporte.model.Corrida;
import com.sctp.transporte.model.CorridaDetalle;

public class CorridaDAO {
    private static final String COLS = "c.id_corrida, c.id_ruta, c.id_unidad, c.id_conductor, c.id_parada_actual, c.hora_salida, c.dias_operacion, c.activa";
    private final Connection c;
    public CorridaDAO(Connection c) { this.c = c; }

    private static Corrida map(ResultSet rs) throws SQLException {
        return new Corrida(rs.getInt("id_corrida"), rs.getInt("id_ruta"), rs.getInt("id_unidad"), rs.getInt("id_conductor"),
                Jdbc.nullableInt(rs, "id_parada_actual"), rs.getObject("hora_salida", LocalTime.class),
                rs.getString("dias_operacion"), rs.getBoolean("activa"));
    }

    private static CorridaDetalle mapDetalle(ResultSet rs) throws SQLException {
        return new CorridaDetalle(map(rs), rs.getString("ruta_clave"), rs.getString("numero_economico"), rs.getString("conductor_nombre"));
    }

    /** RF-13: todas las corridas, o solo las de un conductor si idConductor no es null. */
    public List<CorridaDetalle> listarDetalle(Integer idConductor) throws SQLException {
        String sql = "SELECT " + COLS + ", r.clave AS ruta_clave, u.numero_economico, d.nombre AS conductor_nombre "
                + "FROM corrida c JOIN ruta r ON r.id_ruta = c.id_ruta JOIN unidad u ON u.id_unidad = c.id_unidad "
                + "JOIN conductor d ON d.id_conductor = c.id_conductor "
                + (idConductor != null ? "WHERE c.id_conductor = ? " : "")
                + "ORDER BY r.clave, c.hora_salida";
        return idConductor != null ? Jdbc.query(c, sql, CorridaDAO::mapDetalle, idConductor)
                                   : Jdbc.query(c, sql, CorridaDAO::mapDetalle);
    }

    public Optional<Corrida> buscar(int id) throws SQLException {
        return Jdbc.one(c, "SELECT " + COLS + " FROM corrida c WHERE c.id_corrida = ?", CorridaDAO::map, id);
    }

    public int crear(Corrida k) throws SQLException {
        return Jdbc.insert(c, "INSERT INTO corrida (id_ruta, id_unidad, id_conductor, id_parada_actual, hora_salida, dias_operacion, activa) "
                + "VALUES (?,?,?,?,?,?,?) RETURNING id_corrida",
                k.idRuta(), k.idUnidad(), k.idConductor(), k.idParadaActual(), k.horaSalida(), k.diasOperacion(), k.activa());
    }

    public int actualizar(Corrida k) throws SQLException {
        return Jdbc.update(c, "UPDATE corrida SET id_ruta=?, id_unidad=?, id_conductor=?, id_parada_actual=?, hora_salida=?, dias_operacion=?, activa=? WHERE id_corrida=?",
                k.idRuta(), k.idUnidad(), k.idConductor(), k.idParadaActual(), k.horaSalida(), k.diasOperacion(), k.activa(), k.id());
    }

    public int cambiarActiva(int id, boolean activa) throws SQLException {
        return Jdbc.update(c, "UPDATE corrida SET activa=? WHERE id_corrida=?", activa, id);
    }
}
