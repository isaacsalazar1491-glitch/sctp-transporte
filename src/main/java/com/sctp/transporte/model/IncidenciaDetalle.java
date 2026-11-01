package com.sctp.transporte.model;

/** Incidencia con nombres para la pantalla de administración (incluye conductor; NO es para el público). */
public record IncidenciaDetalle(Incidencia incidencia, String rutaClave, String unidadNumeroEconomico, String conductorNombre) {}
