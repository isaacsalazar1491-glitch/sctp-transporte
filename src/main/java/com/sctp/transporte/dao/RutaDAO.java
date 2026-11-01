package com.sctp.transporte.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import com.sctp.transporte.model.enums.EstadoRuta;
import com.sctp.transporte.model.Ruta;

public class RutaDAO {
    private static final String COLS = "id_ruta, clave, nombre, origen, destino, estado";
    private final Connection c;
    public RutaDAO(Connection c) { this.c = c; }

    private static Ruta map(ResultSet rs) throws SQLException {
        return new Ruta(rs.getInt("id_ruta"), rs.getString("clave"), rs.getString("nombre"),
                rs.getString("origen"), rs.getString("destino"), EstadoRuta.valueOf(rs.getString("estado")));
    }

    public List<Ruta> listar(boolean soloActivas) throws SQLException {
        return Jdbc.query(c, "SELECT " + COLS + " FROM ruta" + (soloActivas ? " WHERE estado = 'ACTIVA'" : "") + " ORDER BY clave", RutaDAO::map);
    }

    public Optional<Ruta> buscar(int id) throws SQLException {
        return Jdbc.one(c, "SELECT " + COLS + " FROM ruta WHERE id_ruta = ?", RutaDAO::map, id);
    }

    public int crear(Ruta r) throws SQLException {
        return Jdbc.insert(c, "INSERT INTO ruta (clave, nombre, origen, destino, estado) VALUES (?,?,?,?,?) RETURNING id_ruta",
                r.clave(), r.nombre(), r.origen(), r.destino(), r.estado());
    }

    public int actualizar(Ruta r) throws SQLException {
        return Jdbc.update(c, "UPDATE ruta SET clave=?, nombre=?, origen=?, destino=?, estado=? WHERE id_ruta=?",
                r.clave(), r.nombre(), r.origen(), r.destino(), r.estado(), r.id());
    }

    public int cambiarEstado(int id, EstadoRuta estado) throws SQLException {
        return Jdbc.update(c, "UPDATE ruta SET estado=? WHERE id_ruta=?", estado, id);
    }
}
