package com.sctp.transporte.model;

import com.sctp.transporte.model.enums.EstadoIncidencia;
import com.sctp.transporte.model.enums.SeveridadIncidencia;
import java.time.LocalDateTime;

/** Incidencia visible al público: sin datos del conductor. */
public record IncidenciaPublica(int id, int idRuta, String rutaClave, String unidad, SeveridadIncidencia severidad,
                                EstadoIncidencia estado, String descripcion, LocalDateTime fecha) {}
