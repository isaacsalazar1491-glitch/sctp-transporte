package com.sctp.transporte.model;

import java.time.LocalTime;

/** Ruta con número de unidades activas y su próxima salida de hoy (null si ya no hay). */
public record RutaResumen(Ruta ruta, int unidades, LocalTime proximaSalida) {}
