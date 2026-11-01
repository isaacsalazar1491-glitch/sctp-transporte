package com.sctp.transporte.model;

import com.sctp.transporte.model.enums.EstadoIncidencia;
import com.sctp.transporte.model.enums.SeveridadIncidencia;
import com.sctp.transporte.util.Validar;
import java.time.LocalDateTime;

/** idConductor es opcional (puede ser una falla de la unidad sin conductor involucrado). */
public record Incidencia(Integer id, int idRuta, int idUnidad, Integer idConductor, String descripcion,
                         SeveridadIncidencia severidad, EstadoIncidencia estado,
                         LocalDateTime fechaRegistro, LocalDateTime fechaCierre) {
    public Incidencia {
        Validar.positivo(idRuta, "ruta");
        Validar.positivo(idUnidad, "unidad");
        descripcion = Validar.texto(descripcion, "descripción", 300);
        Validar.noNulo(severidad, "severidad");
        Validar.noNulo(estado, "estado");
    }

    /** Incidencia nueva: estado ABIERTA, la fecha la pone la base de datos. */
    public static Incidencia nueva(int idRuta, int idUnidad, Integer idConductor, String descripcion, SeveridadIncidencia severidad) {
        return new Incidencia(null, idRuta, idUnidad, idConductor, descripcion, severidad, EstadoIncidencia.ABIERTA, null, null);
    }
}
