package com.sctp.transporte.model;

import com.sctp.transporte.model.enums.*;

import com.sctp.transporte.util.Validar;

public record Unidad(Integer id, String placa, String numeroEconomico, int capacidad, EstadoUnidad estado) {
    public Unidad {
        placa = Validar.texto(placa, "placa", 15).toUpperCase();
        numeroEconomico = Validar.texto(numeroEconomico, "número económico", 20);
        Validar.positivo(capacidad, "capacidad");
        Validar.noNulo(estado, "estado");
    }

    /** Operativa = puede asignarse a corridas y mostrarse como disponible (no está en MANTENIMIENTO ni BAJA). */
    public boolean estaOperativa() { return estado == EstadoUnidad.DISPONIBLE || estado == EstadoUnidad.EN_RUTA; }
}
