package com.sctp.transporte.model;

/** Corrida con los nombres de ruta, unidad y conductor, lista para mostrarse. */
public record CorridaDetalle(Corrida corrida, String rutaClave, String unidadNumeroEconomico, String conductorNombre) {}
