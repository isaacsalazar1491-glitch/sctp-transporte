package com.sctp.transporte;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.sctp.transporte.model.*;
import com.sctp.transporte.model.enums.*;
import com.sctp.transporte.service.AdministracionService;
import com.sctp.transporte.service.ConsultaPublicaService;
import com.sctp.transporte.util.Dias;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Consulta pública con la hora congelada en lunes 2026-10-26 07:06 (argLine en el pom). */
class ConsultaPublicaServiceIT extends BaseIT {
    private final ConsultaPublicaService svc = new ConsultaPublicaService();
    private final AdministracionService admin = new AdministracionService();

    @BeforeEach
    void horaCongelada() {
        assumeTrue("2026-10-26T07:06".equals(System.getProperty("sctp.ahora")), "Falta -Dsctp.ahora=2026-10-26T07:06");
    }

    @Test
    void proximasLlegadasSonEstimacionPorHorario() {
        List<Llegada> l = svc.proximasLlegadas(2, 3); // parada 2 = Santiago Tilapa (12 min)
        assertEquals(3, l.size());
        assertEquals(LocalTime.of(7, 12), l.get(0).llegada());
        assertEquals(6, l.get(0).enMinutos());
        assertEquals("14", l.get(0).salida().unidad());
        assertEquals(LocalTime.of(7, 42), l.get(1).llegada());
        assertEquals(LocalTime.of(8, 27), l.get(2).llegada());
        assertEquals(81, l.get(2).enMinutos());
    }

    @Test
    void unidadEnMantenimientoNoApareceNiEnLlegadasNiEnDisponibilidad() {
        admin.cambiarEstadoUnidad(1, EstadoUnidad.MANTENIMIENTO); // Eco. 14
        assertTrue(svc.disponibilidad(1).stream().noneMatch(u -> u.unidad().equals("14")));
        assertTrue(svc.proximasLlegadas(2, 10).stream().noneMatch(x -> x.salida().unidad().equals("14")));
        assertTrue(svc.horarios(1).stream().noneMatch(x -> x.unidad().equals("14")));
    }

    @Test
    void incidenciaGraveRetiraLaUnidadDeLaConsultaPublicaYApareceEnElResumen() {
        admin.registrarIncidencia(Incidencia.nueva(1, 3, 4, "Falla de transmisión", SeveridadIncidencia.GRAVE)); // Eco. 22
        assertTrue(svc.disponibilidad(1).stream().noneMatch(u -> u.unidad().equals("22")));
        assertTrue(svc.incidencias(1).stream().anyMatch(i -> i.unidad().equals("22") && i.severidad() == SeveridadIncidencia.GRAVE));
        ResumenIncidenciasRuta r = svc.resumenIncidencias().stream().filter(x -> x.rutaClave().equals("R-01")).findFirst().orElseThrow();
        assertEquals(1, r.graves());
    }

    @Test
    void disponibilidadConProximaSalidaYEstado() {
        List<UnidadDisponible> d = svc.disponibilidad(1);
        assertEquals(3, d.size());
        UnidadDisponible e14 = d.stream().filter(u -> u.unidad().equals("14")).findFirst().orElseThrow();
        assertEquals(LocalTime.of(8, 45), e14.proximaSalida());
        assertEquals(EstadoUnidad.DISPONIBLE, e14.estado());
        assertEquals(EstadoUnidad.EN_RUTA, d.stream().filter(u -> u.unidad().equals("07")).findFirst().orElseThrow().estado());
    }

    @Test
    void rutasYBuscador() {
        assertEquals(2, svc.buscarRutas("").size()); // la inactiva no es pública
        assertEquals(List.of("R-01"), svc.buscarRutas("lerma").stream().map(r -> r.ruta().clave()).toList());
        assertEquals(3, svc.buscarRutas("").get(0).unidades());
        assertEquals(LocalTime.of(7, 30), svc.buscarRutas("").get(0).proximaSalida());
        assertTrue(svc.buscarRutas("no existe").isEmpty());
        assertTrue(svc.buscarRutas("100%").isEmpty()); // % se trata como texto, no como comodín
        rechaza("no encontrada", () -> svc.rutaActiva(3));
    }

    @Test
    void incidenciasPublicasSinConductorNiCerradas() {
        List<IncidenciaPublica> l = svc.incidencias(null);
        assertEquals(2, l.size()); // la grave cerrada no se muestra
        assertTrue(l.stream().noneMatch(i -> i.estado() == EstadoIncidencia.CERRADA));
        // el tipo IncidenciaPublica no tiene campos de conductor: ni nombre, ni licencia, ni teléfono
        assertTrue(java.util.Arrays.stream(IncidenciaPublica.class.getRecordComponents()).noneMatch(c -> c.getName().toLowerCase().contains("conductor")));
        assertTrue(java.util.Arrays.stream(Salida.class.getRecordComponents()).noneMatch(c -> c.getName().toLowerCase().contains("conductor")));
    }

    @Test
    void etiquetasDeDias() {
        assertEquals("Lun–Sáb", Dias.etiqueta("L,M,X,J,V,S"));
        assertEquals("Lun, Mié, Vie", Dias.etiqueta("L,X,V"));
        assertEquals("Sáb, Dom", Dias.etiqueta("S,D"));
    }
}
