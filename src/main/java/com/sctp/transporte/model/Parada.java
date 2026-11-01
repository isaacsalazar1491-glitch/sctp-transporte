package com.sctp.transporte.model;

import com.sctp.transporte.util.ReglaNegocioException;
import com.sctp.transporte.util.Validar;

public record Parada(Integer id, int idRuta, String nombre, int orden, int minutosDesdeOrigen) {
    public Parada {
        nombre = Validar.texto(nombre, "nombre", 100);
        Validar.positivo(orden, "orden");
        if (minutosDesdeOrigen < 0) throw new ReglaNegocioException("Los minutos desde el origen no pueden ser negativos");
    }
}
