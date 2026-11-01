package com.sctp.transporte.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import com.sctp.transporte.model.Conductor;

public class ConductorDAO {
    private static final String COLS = "id_conductor, nombre, numero_licencia, vigencia_licencia, telefono, activo";
    private final Connection c;
    public ConductorDAO(Connection c) { this.c = c; }

    private static Conductor map(ResultSet rs) throws SQLException {
        return new Conductor(rs.getInt("id_conductor"), rs.getString("nombre"), rs.getString("numero_licencia"),
                rs.getObject("vigencia_licencia", LocalDate.class), rs.getString("telefono"), rs.getBoolean("activo"));
    }

    public List<Conductor> listar(boolean soloActivos) throws SQLException {
        return Jdbc.query(c, "SELECT " + COLS + " FROM conductor" + (soloActivos ? " WHERE activo" : "") + " ORDER BY nombre", ConductorDAO::map);
    }

    public Optional<Conductor> buscar(int id) throws SQLException {
        return Jdbc.one(c, "SELECT " + COLS + " FROM conductor WHERE id_conductor = ?", ConductorDAO::map, id);
    }

    public int crear(Conductor d) throws SQLException {
        return Jdbc.insert(c, "INSERT INTO conductor (nombre, numero_licencia, vigencia_licencia, telefono, activo) VALUES (?,?,?,?,?) RETURNING id_conductor",
                d.nombre(), d.numeroLicencia(), d.vigenciaLicencia(), d.telefono(), d.activo());
    }

    public int actualizar(Conductor d) throws SQLException {
        return Jdbc.update(c, "UPDATE conductor SET nombre=?, numero_licencia=?, vigencia_licencia=?, telefono=?, activo=? WHERE id_conductor=?",
                d.nombre(), d.numeroLicencia(), d.vigenciaLicencia(), d.telefono(), d.activo(), d.id());
    }

    public int cambiarActivo(int id, boolean activo) throws SQLException {
        return Jdbc.update(c, "UPDATE conductor SET activo=? WHERE id_conductor=?", activo, id);
    }
}
