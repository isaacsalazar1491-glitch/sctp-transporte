package com.sctp.transporte.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Utilidades mínimas para JDBC. Todas las consultas usan PreparedStatement. */
final class Jdbc {
    private Jdbc() {}

    @FunctionalInterface
    interface RowMapper<T> { T map(ResultSet rs) throws SQLException; }

    static <T> List<T> query(Connection c, String sql, RowMapper<T> m, Object... p) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, p);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> out = new ArrayList<>();
                while (rs.next()) out.add(m.map(rs));
                return out;
            }
        }
    }

    static <T> Optional<T> one(Connection c, String sql, RowMapper<T> m, Object... p) throws SQLException {
        return query(c, sql, m, p).stream().findFirst();
    }

    static int update(Connection c, String sql, Object... p) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, p);
            return ps.executeUpdate();
        }
    }

    /** INSERT ... RETURNING id: devuelve la llave generada. */
    static int insert(Connection c, String sqlReturning, Object... p) throws SQLException {
        return query(c, sqlReturning, rs -> rs.getInt(1), p).get(0);
    }

    static Integer nullableInt(ResultSet rs, String col) throws SQLException {
        int v = rs.getInt(col);
        return rs.wasNull() ? null : v;
    }

    private static void bind(PreparedStatement ps, Object[] p) throws SQLException {
        for (int i = 0; i < p.length; i++) {
            ps.setObject(i + 1, p[i] instanceof Enum<?> e ? e.name() : p[i]);
        }
    }
}
