package com.sctp.transporte.model;

import com.sctp.transporte.model.enums.*;

import com.sctp.transporte.util.Validar;

public record Ruta(Integer id, String clave, String nombre, String origen, String destino, EstadoRuta estado) {
    public Ruta {
        clave = Validar.texto(clave, "clave", 20);
        nombre = Validar.texto(nombre, "nombre", 100);
        origen = Validar.texto(origen, "origen", 100);
        destino = Validar.texto(destino, "destino", 100);
        Validar.noNulo(estado, "estado");
    }
}
