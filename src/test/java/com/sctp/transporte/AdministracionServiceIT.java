package com.sctp.transporte;

import static org.junit.jupiter.api.Assertions.*;

import com.sctp.transporte.model.*;
import com.sctp.transporte.model.enums.*;
import com.sctp.transporte.service.AdministracionService;
import com.sctp.transporte.service.AuthService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class AdministracionServiceIT extends BaseIT {
    private final AdministracionService s = new AdministracionService();

    private int total() { return s.listarCorridas(null).size(); }

    @Test
    void loginBcrypt() {
        AuthService a = new AuthService();
        assertEquals("admin", a.autenticar("admin", "admin123").username());
        rechaza("incorrectos", () -> a.autenticar("admin", "mala"));
        rechaza("incorrectos", () -> a.autenticar("nadie", "admin123"));
        rechaza("incorrectos", () -> a.autenticar("", ""));
    }

    @Test
    void crudRutaYBajaLogica() {
        int id = s.crearRuta(new Ruta(null, "R-10", "Prueba", "A", "B", EstadoRuta.ACTIVA));
        s.actualizarRuta(new Ruta(id, "R-10", "Renombrada", "A", "B", EstadoRuta.ACTIVA));
        assertEquals("Renombrada", s.obtenerRuta(id).nombre());
        s.darDeBajaRuta(id);
        assertEquals(EstadoRuta.INACTIVA, s.obtenerRuta(id).estado());
        assertEquals(1, s.listarRutas(false).stream().filter(r -> r.id() == id).count());
        rechaza("clave", () -> s.crearRuta(new Ruta(null, "R-10", "Otra", "A", "B", EstadoRuta.ACTIVA)));
        rechaza("obligatorio", () -> new Ruta(null, " ", "x", "a", "b", EstadoRuta.ACTIVA));
        rechaza("no encontrada", () -> s.obtenerRuta(9999));
    }

    @Test
    void unicosYChecks() {
        rechaza("placa", () -> s.crearUnidad(new Unidad(null, "abc-123", "99", 10, EstadoUnidad.DISPONIBLE)));
        rechaza("licencia", () -> s.crearConductor(new Conductor(null, "X", "LIC-0001", LocalDate.now().plusDays(5), "7140000000", true)));
        rechaza("orden", () -> s.crearParada(new Parada(null, 1, "Repetida", 1, 5)));
        rechaza("operación no válidos", () -> new Corrida(null, 1, 1, 1, null, LocalTime.of(7, 0), "L,Z", true));
    }

    @Test
    void triggerCorridaRechazaUnidadNoDisponibleLicenciaVencidaConductorInactivoYParadaAjena() {
        int antes = total();
        rechaza("no está disponible", () -> s.crearCorrida(new Corrida(null, 1, 4, 1, null, LocalTime.of(10, 0), "L,M", true))); // Eco. 31 mantenimiento
        rechaza("no está disponible", () -> s.crearCorrida(new Corrida(null, 1, 5, 1, null, LocalTime.of(10, 0), "L,M", true))); // Eco. 40 baja
        rechaza("venció", () -> s.crearCorrida(new Corrida(null, 1, 1, 3, null, LocalTime.of(10, 0), "L,M", true)));
        s.darDeBajaConductor(1);
        rechaza("dado de baja", () -> s.crearCorrida(new Corrida(null, 1, 1, 1, null, LocalTime.of(11, 0), "L", true)));
        rechaza("no pertenece", () -> s.crearCorrida(new Corrida(null, 1, 1, 2, 5, LocalTime.of(11, 0), "L", true)));
        assertEquals(antes, total(), "no debe insertarse nada");
    }

    @Test
    void corridaValidaYUnidadEnMantenimiento() {
        int antes = total();
        int id = s.crearCorrida(new Corrida(null, 1, 2, 1, 2, LocalTime.of(12, 30), "V,L,M", true)); // Eco. 07 EN_RUTA es operativa
        assertEquals(antes + 1, total());
        s.cambiarEstadoUnidad(2, EstadoUnidad.MANTENIMIENTO);
        assertFalse(s.obtenerUnidad(2).estaOperativa());
        Corrida c = s.obtenerCorrida(id);
        rechaza("no está disponible", () -> s.actualizarCorrida(new Corrida(id, c.idRuta(), c.idUnidad(), c.idConductor(), c.idParadaActual(), LocalTime.of(13, 0), c.diasOperacion(), true)));
        s.darDeBajaCorrida(id);
        s.actualizarCorrida(new Corrida(id, c.idRuta(), c.idUnidad(), c.idConductor(), c.idParadaActual(), LocalTime.of(13, 0), c.diasOperacion(), false));
        assertEquals(LocalTime.of(13, 0), s.obtenerCorrida(id).horaSalida());
    }

    @Test
    void corridasPorConductor() {
        assertEquals(4, s.listarCorridas(1).size());
        assertEquals(4, s.listarCorridas(2).size());
        assertEquals(1, s.listarCorridas(4).size());
    }

    @Test
    void incidenciaGraveSacaLaUnidadDeServicio() {
        assertTrue(s.obtenerUnidad(3).estaOperativa());
        int id = s.registrarIncidencia(Incidencia.nueva(1, 3, 4, "Falla de motor", SeveridadIncidencia.GRAVE));
        assertEquals(EstadoUnidad.MANTENIMIENTO, s.obtenerUnidad(3).estado());
        assertEquals(EstadoIncidencia.ABIERTA, s.obtenerIncidencia(id).estado());
        rechaza("no está disponible", () -> s.crearCorrida(new Corrida(null, 1, 3, 4, null, LocalTime.of(14, 0), "L", true)));
    }

    @Test
    void incidenciaLeveONoCambiaLaUnidadYBajaSeRespeta() {
        s.registrarIncidencia(Incidencia.nueva(1, 3, null, "Espejo roto", SeveridadIncidencia.LEVE));
        assertEquals(EstadoUnidad.DISPONIBLE, s.obtenerUnidad(3).estado());
        s.registrarIncidencia(Incidencia.nueva(1, 5, null, "Falla grave en unidad de baja", SeveridadIncidencia.GRAVE));
        assertEquals(EstadoUnidad.BAJA, s.obtenerUnidad(5).estado(), "una unidad en BAJA no pasa a MANTENIMIENTO");
    }

    @Test
    void seguimientoDeIncidenciaPorProcedimiento() {
        int id = s.registrarIncidencia(Incidencia.nueva(1, 3, null, "Llanta baja", SeveridadIncidencia.MODERADA));
        rechaza("Transición no permitida", () -> s.cambiarEstadoIncidencia(id, EstadoIncidencia.CERRADA)); // no se puede saltar
        s.cambiarEstadoIncidencia(id, EstadoIncidencia.EN_ATENCION);
        assertNull(s.obtenerIncidencia(id).fechaCierre());
        s.cambiarEstadoIncidencia(id, EstadoIncidencia.CERRADA);
        assertNotNull(s.obtenerIncidencia(id).fechaCierre());
        rechaza("Transición no permitida", () -> s.cambiarEstadoIncidencia(id, EstadoIncidencia.ABIERTA)); // no retrocede
        rechaza("no encontrada", () -> s.cambiarEstadoIncidencia(9999, EstadoIncidencia.EN_ATENCION));
        assertEquals(1, s.listarIncidencias(EstadoIncidencia.ABIERTA).size()); // la sembrada LEVE
        assertEquals(4, s.listarIncidencias(null).size()); // 3 sembradas + la nueva
    }

    @Test
    void noSeEliminaUnidadConIncidenciasSoloBaja() {
        s.darDeBajaUnidad(1);
        assertEquals(EstadoUnidad.BAJA, s.obtenerUnidad(1).estado());
        assertEquals(1, s.listarUnidades(false).stream().filter(u -> u.id() == 1).count());
        assertFalse(s.listarUnidades(true).stream().anyMatch(u -> u.id() == 1));
    }

    @Test
    void reportesDeAdministracion() {
        assertEquals(new ResumenUnidades(2, 1, 1, 1), s.resumenUnidades());
        List<LicenciaPorVencer> l = s.licenciasPorVencer();
        assertEquals(2, l.size()); // vencida (Tres) + por vencer (Dos)
        assertTrue(l.get(0).diasRestantes() < 0);
    }
}
