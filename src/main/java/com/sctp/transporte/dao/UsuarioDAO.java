package com.sctp.transporte.dao;

import com.sctp.transporte.model.Usuario;
import com.sctp.transporte.model.enums.RolUsuario;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

public class UsuarioDAO {
    private final Connection c;

    public UsuarioDAO(Connection c) { this.c = c; }

    public Optional<Usuario> porUsername(String username) throws SQLException {
        return Jdbc.one(c, "SELECT id_usuario, username, password_hash, rol, activo FROM usuario WHERE username = ?",
                rs -> new Usuario(rs.getInt("id_usuario"), rs.getString("username"), rs.getString("password_hash"),
                        RolUsuario.valueOf(rs.getString("rol")), rs.getBoolean("activo")), username);
    }
}
