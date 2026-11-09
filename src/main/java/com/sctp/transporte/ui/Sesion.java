package com.sctp.transporte.ui;

import com.sctp.transporte.model.Usuario;

/** Usuario autenticado de la aplicación (un solo administrador a la vez). */
public final class Sesion {
    private static Usuario usuario;

    private Sesion() {}

    public static void iniciar(Usuario u) { usuario = u; }
    public static void cerrar() { usuario = null; }
    public static boolean activa() { return usuario != null; }
    public static Usuario usuario() { return usuario; }
}
