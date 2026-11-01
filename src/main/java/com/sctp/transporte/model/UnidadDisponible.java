package com.sctp.transporte.model;

import com.sctp.transporte.model.enums.EstadoUnidad;
import java.time.LocalTime;

/** Unidad operativa de una ruta con su próxima salida de hoy (null si ya no hay). Sin datos del conductor. */
public record UnidadDisponible(String unidad, int capacidad, EstadoUnidad estado, LocalTime proximaSalida) {}
