package com.sctp.transporte.model;

/** Resumen público por ruta: incidencias no cerradas. */
public record ResumenIncidenciasRuta(int idRuta, String rutaClave, String rutaNombre, long abiertas, long enAtencion, long graves) {}
