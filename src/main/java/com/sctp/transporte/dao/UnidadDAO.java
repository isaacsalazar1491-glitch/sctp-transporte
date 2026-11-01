package com.sctp.transporte.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import com.sctp.transporte.model.enums.EstadoUnidad;
import com.sctp.transporte.model.Unidad;

public class UnidadDAO {
    private static final String COLS = "id_unidad, placa, numero_economico, capacidad, estado";
    private final Connection c;
    public UnidadDAO(Connection c) { this.c = c; }

    private static Unidad map(ResultSet rs) throws SQLException {
        return new Unidad(rs.getInt("id_unidad"), rs.getString("placa"), rs.getString("numero_economico"),
                rs.getInt("capacidad"), EstadoUnidad.valueOf(rs.getString("estado")));
    }

    public List<Unidad> listar(boolean soloOperativas) throws SQLException {
        return Jdbc.query(c, "SELECT " + COLS + " FROM unidad" + (soloOperativas ? " WHERE estado IN ('DISPONIBLE','EN_RUTA')" : "") + " ORDER BY numero_economico", UnidadDAO::map);
    }

    public Optional<Unidad> buscar(int id) throws SQLException {
        return Jdbc.one(c, "SELECT " + COLS + " FROM unidad WHERE id_unidad = ?", UnidadDAO::map, id);
    }

    public int crear(Unidad u) throws SQLException {
        return Jdbc.insert(c, "INSERT INTO unidad (placa, numero_economico, capacidad, estado) VALUES (?,?,?,?) RETURNING id_unidad",
                u.placa(), u.numeroEconomico(), u.capacidad(), u.estado());
    }

    public int actualizar(Unidad u) throws SQLException {
        return Jdbc.update(c, "UPDATE unidad SET placa=?, numero_economico=?, capacidad=?, estado=? WHERE id_unidad=?",
                u.placa(), u.numeroEconomico(), u.capacidad(), u.estado(), u.id());
    }

    public int cambiarEstado(int id, EstadoUnidad estado) throws SQLException {
        return Jdbc.update(c, "UPDATE unidad SET estado=? WHERE id_unidad=?", estado, id);
    }
}
