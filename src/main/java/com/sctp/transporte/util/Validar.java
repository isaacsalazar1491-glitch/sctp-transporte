package com.sctp.transporte.util;

public final class Validar {
    private Validar() {}

    public static String texto(String v, String campo, int max) {
        if (v == null || v.isBlank()) throw new ReglaNegocioException("El campo " + campo + " es obligatorio");
        v = v.trim();
        if (v.length() > max) throw new ReglaNegocioException("El campo " + campo + " admite máximo " + max + " caracteres");
        return v;
    }

    public static int positivo(int v, String campo) {
        if (v <= 0) throw new ReglaNegocioException("El campo " + campo + " debe ser mayor que 0");
        return v;
    }

    public static <T> T noNulo(T v, String campo) {
        if (v == null) throw new ReglaNegocioException("El campo " + campo + " es obligatorio");
        return v;
    }
}
