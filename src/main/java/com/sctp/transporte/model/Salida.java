package com.sctp.transporte.model;

import com.sctp.transporte.model.enums.EstadoUnidad;
import java.time.LocalTime;

/** Corrida activa con unidad operativa, lista para consulta pública (sin datos del conductor). */
public record Salida(int idCorrida, String unidad, int capacidad, EstadoUnidad estado, LocalTime hora, String dias) {}
