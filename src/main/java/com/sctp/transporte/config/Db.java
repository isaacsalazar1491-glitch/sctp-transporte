package com.sctp.transporte.config;

import com.sctp.transporte.util.ReglaNegocioException;

import java.sql.Connection;
import java.sql.SQLException;
import org.postgresql.util.PSQLException;

/** Transacciones sobre {@link ConexionBD}: lectura, escritura (commit/rollback) y traducción de errores SQL a mensajes de negocio. */
public final class Db {
    private Db() {}

    @FunctionalInterface
    public interface Work<T> { T run(Connection c) throws SQLException; }

    public static Connection get() throws SQLException {
        return ConexionBD.obtenerConexion();
    }

    /** Operación de lectura (autocommit). */
    public static <T> T read(Work<T> w) {
        try (Connection c = get()) {
            return w.run(c);
        } catch (SQLException e) {
            throw traducir(e);
        }
    }

    /** Operación de escritura: todo o nada (commit / rollback). */
    public static <T> T tx(Work<T> w) {
        try (Connection c = get()) {
            c.setAutoCommit(false);
            try {
                T r = w.run(c);
                c.commit();
                return r;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw traducir(e);
        }
    }

    static RuntimeException traducir(SQLException e) {
        String state = e.getSQLState() == null ? "" : e.getSQLState();
        String constraint = (e instanceof PSQLException p && p.getServerErrorMessage() != null)
                ? String.valueOf(p.getServerErrorMessage().getConstraint()) : "";
        switch (state) {
            case "23505":
                if (constraint.contains("clave")) return new ReglaNegocioException("La clave de ruta ya existe");
                if (constraint.contains("placa")) return new ReglaNegocioException("La placa ya existe");
                if (constraint.contains("licencia")) return new ReglaNegocioException("El número de licencia ya existe");
                if (constraint.contains("username")) return new ReglaNegocioException("El usuario ya existe");
                if (constraint.contains("orden")) return new ReglaNegocioException("Ya existe una parada con ese orden en la ruta");
                return new ReglaNegocioException("Ya existe un registro con ese valor");
            case "23503":
                return new ReglaNegocioException("La operación no es posible: el registro está relacionado con otros datos");
            case "23514":
                return new ReglaNegocioException("Algún valor no cumple las restricciones permitidas");
            case "P0001": // RAISE EXCEPTION del trigger
                return new ReglaNegocioException(e instanceof PSQLException p && p.getServerErrorMessage() != null
                        ? p.getServerErrorMessage().getMessage() : e.getMessage());
            default:
                return new IllegalStateException("Error de base de datos (" + state + "): " + e.getMessage(), e);
        }
    }
}
