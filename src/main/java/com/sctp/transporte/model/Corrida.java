package com.sctp.transporte.model;

import java.time.LocalTime;
import com.sctp.transporte.util.ReglaNegocioException;
import com.sctp.transporte.util.Validar;

/** Días de operación: L M X J V S D separados por coma (X = miércoles). */
public record Corrida(Integer id, int idRuta, int idUnidad, int idConductor, Integer idParadaActual,
                      LocalTime horaSalida, String diasOperacion, boolean activa) {
    public Corrida {
        Validar.positivo(idRuta, "ruta");
        Validar.positivo(idUnidad, "unidad");
        Validar.positivo(idConductor, "conductor");
        Validar.noNulo(horaSalida, "hora de salida");
        diasOperacion = Validar.texto(diasOperacion, "días de operación", 50);
        if (!diasOperacion.matches("[LMXJVSD](,[LMXJVSD])*"))
            throw new ReglaNegocioException("Días de operación no válidos (use L, M, X, J, V, S, D)");
    }
}
