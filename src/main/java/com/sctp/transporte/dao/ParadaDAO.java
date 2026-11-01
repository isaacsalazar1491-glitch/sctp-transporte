package com.sctp.transporte.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import com.sctp.transporte.model.Parada;

public class ParadaDAO {
    private static final String COLS = "id_parada, id_ruta, nombre, orden, minutos_desde_origen";
    private final Connection c;
    public ParadaDAO(Connection c) { this.c = c; }

    private static Parada map(ResultSet rs) throws SQLException {
        return new Parada(rs.getInt("id_parada"), rs.getInt("id_ruta"), rs.getString("nombre"),
                rs.getInt("orden"), rs.getInt("minutos_desde_origen"));
    }

    public List<Parada> listarPorRuta(int idRuta) throws SQLException {
        return Jdbc.query(c, "SELECT " + COLS + " FROM parada WHERE id_ruta = ? ORDER BY orden", ParadaDAO::map, idRuta);
    }

    public Optional<Parada> buscar(int id) throws SQLException {
        return Jdbc.one(c, "SELECT " + COLS + " FROM parada WHERE id_parada = ?", ParadaDAO::map, id);
    }

    public int crear(Parada p) throws SQLException {
        return Jdbc.insert(c, "INSERT INTO parada (id_ruta, nombre, orden, minutos_desde_origen) VALUES (?,?,?,?) RETURNING id_parada",
                p.idRuta(), p.nombre(), p.orden(), p.minutosDesdeOrigen());
    }

    public int actualizar(Parada p) throws SQLException {
        return Jdbc.update(c, "UPDATE parada SET nombre=?, orden=?, minutos_desde_origen=? WHERE id_parada=?",
                p.nombre(), p.orden(), p.minutosDesdeOrigen(), p.id());
    }

    public int eliminar(int id) throws SQLException {
        return Jdbc.update(c, "DELETE FROM parada WHERE id_parada = ?", id);
    }
}
