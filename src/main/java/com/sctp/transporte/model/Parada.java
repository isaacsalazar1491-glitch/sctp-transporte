package com.sctp.transporte.model;

import com.sctp.transporte.util.ReglaNegocioException;
import com.sctp.transporte.util.Validar;

/** latitud/longitud son opcionales: una parada sin ubicación aún no se ha colocado en el mapa. */
public record Parada(Integer id, int idRuta, String nombre, int orden, int minutosDesdeOrigen,
                     Double latitud, Double longitud) {
    public Parada {
        nombre = Validar.texto(nombre, "nombre", 100);
        Validar.positivo(orden, "orden");
        if (minutosDesdeOrigen < 0) throw new ReglaNegocioException("Los minutos desde el origen no pueden ser negativos");
        if ((latitud == null) != (longitud == null))
            throw new ReglaNegocioException("Latitud y longitud deben indicarse juntas");
        if (latitud != null && (latitud < -90 || latitud > 90))
            throw new ReglaNegocioException("La latitud debe estar entre -90 y 90");
        if (longitud != null && (longitud < -180 || longitud > 180))
            throw new ReglaNegocioException("La longitud debe estar entre -180 y 180");
    }

    /** Constructor sin ubicación. */
    public Parada(Integer id, int idRuta, String nombre, int orden, int minutosDesdeOrigen) {
        this(id, idRuta, nombre, orden, minutosDesdeOrigen, null, null);
    }

    public boolean tieneUbicacion() { return latitud != null && longitud != null; }
}
