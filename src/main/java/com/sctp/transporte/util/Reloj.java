package com.sctp.transporte.util;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Hora "actual" del sistema (zona de México). Para demostraciones o pruebas se puede congelar con
 * -Dsctp.ahora=2026-10-26T06:55 (lunes 26 de octubre de 2026, 06:55).
 */
public final class Reloj {
    private static final ZoneId ZONA = ZoneId.of("America/Mexico_City");
    private static final String FIJA = System.getProperty("sctp.ahora");

    private Reloj() {}

    public static LocalDateTime ahora() {
        return FIJA != null && !FIJA.isBlank() ? LocalDateTime.parse(FIJA) : LocalDateTime.now(Clock.system(ZONA));
    }
}
