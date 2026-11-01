package com.sctp.transporte.service;

import com.sctp.transporte.config.Db;
import com.sctp.transporte.dao.UsuarioDAO;
import com.sctp.transporte.model.Usuario;
import com.sctp.transporte.util.ReglaNegocioException;
import java.util.Optional;
import org.mindrot.jbcrypt.BCrypt;

/** Inicio de sesión del administrador. */
public class AuthService {
    // Hash de relleno para que el tiempo de respuesta no revele si el usuario existe.
    private static final String HASH_RELLENO = BCrypt.hashpw("relleno", BCrypt.gensalt());

    /** Devuelve el usuario autenticado o lanza ReglaNegocioException con un mensaje genérico. */
    public Usuario autenticar(String username, String password) {
        if (username == null || password == null || username.isBlank() || password.isEmpty())
            throw new ReglaNegocioException("Usuario o contraseña incorrectos");
        Optional<Usuario> u = Db.read(c -> new UsuarioDAO(c).porUsername(username.trim()));
        boolean ok = BCrypt.checkpw(password, u.map(Usuario::passwordHash).orElse(HASH_RELLENO));
        if (u.isEmpty() || !ok || !u.get().activo()) throw new ReglaNegocioException("Usuario o contraseña incorrectos");
        return u.get();
    }
}
