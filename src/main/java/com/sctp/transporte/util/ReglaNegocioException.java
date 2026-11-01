package com.sctp.transporte.util;

/** Error esperado (validación o regla de negocio) cuyo mensaje se puede mostrar al usuario. */
public class ReglaNegocioException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public ReglaNegocioException(String mensaje) { super(mensaje); }
}
