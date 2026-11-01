package com.sctp.transporte.model;

import java.time.LocalDate;
import com.sctp.transporte.util.Validar;

public record Conductor(Integer id, String nombre, String numeroLicencia, LocalDate vigenciaLicencia,
                        String telefono, boolean activo) {
    public Conductor {
        nombre = Validar.texto(nombre, "nombre", 150);
        numeroLicencia = Validar.texto(numeroLicencia, "número de licencia", 30);
        Validar.noNulo(vigenciaLicencia, "vigencia de la licencia");
        telefono = Validar.texto(telefono, "teléfono", 15);
    }

    public boolean licenciaVigente() { return !vigenciaLicencia.isBefore(LocalDate.now()); }
}
