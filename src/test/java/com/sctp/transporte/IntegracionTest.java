package com.sctp.transporte;

import com.sctp.transporte.model.*;
import com.sctp.transporte.model.enums.EstadoIncidencia;
import com.sctp.transporte.model.enums.SeveridadIncidencia;
import com.sctp.transporte.util.Dias;
import com.sctp.transporte.util.ReglaNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas de verificación de unión del proyecto (Backend SCTP)")
class IntegracionTest {

    @Test
    @DisplayName("Verificar que los scripts SQL integrados existen y tienen contenido")
    void verificarArchivosSqlIntegrados() {
        String[] scripts = {
            "01_esquema_3fn.sql",
            "02_trigger_validacion.sql",
            "03_vistas_y_procedimientos.sql",
            "04_datos_prueba.sql"
        };
        for (String script : scripts) {
            Path p = Path.of("sql", script);
            assertTrue(Files.exists(p), "El script " + script + " debe existir");
            assertDoesNotThrow(() -> {
                String contenido = Files.readString(p);
                assertTrue(contenido.length() > 500, "El script " + script + " no debe estar vacío");
            });
        }
    }

    @Test
    @DisplayName("Verificar funcionamiento de Corrida y CorridaDetalle (Record)")
    void verificarCorridaYCorridaDetalle() {
        Corrida corrida = new Corrida(
            1, 10, 20, 30, null,
            LocalTime.of(8, 30),
            "L,M,X,J,V",
            true
        );

        assertEquals(1, corrida.id());
        assertEquals(10, corrida.idRuta());
        assertEquals(20, corrida.idUnidad());
        assertEquals(30, corrida.idConductor());
        assertEquals(LocalTime.of(8, 30), corrida.horaSalida());
        assertEquals("L,M,X,J,V", corrida.diasOperacion());
        assertTrue(corrida.activa());

        CorridaDetalle detalle = new CorridaDetalle(corrida, "RUTA-01", "ECO-101", "Carlos Pérez");
        assertEquals("RUTA-01", detalle.rutaClave());
        assertEquals("ECO-101", detalle.unidadNumeroEconomico());
        assertEquals("Carlos Pérez", detalle.conductorNombre());
        assertEquals(corrida, detalle.corrida());
    }

    @Test
    @DisplayName("Verificar validaciones de negocio en Corrida")
    void verificarValidacionDiasCorridaInvalida() {
        assertThrows(ReglaNegocioException.class, () ->
            new Corrida(1, 1, 1, 1, null, LocalTime.of(7, 0), "INVALIDO", true)
        );
        assertThrows(ReglaNegocioException.class, () ->
            new Corrida(1, -5, 1, 1, null, LocalTime.of(7, 0), "L,M", true)
        );
    }

    @Test
    @DisplayName("Verificar Incidencia e IncidenciaDetalle")
    void verificarIncidenciaEIncidenciaDetalle() {
        Incidencia nueva = Incidencia.nueva(1, 2, 3, "Falla en frenos", SeveridadIncidencia.GRAVE);
        assertEquals(EstadoIncidencia.ABIERTA, nueva.estado());
        assertEquals(SeveridadIncidencia.GRAVE, nueva.severidad());
        assertEquals("Falla en frenos", nueva.descripcion());

        IncidenciaDetalle detalle = new IncidenciaDetalle(nueva, "RUTA-02", "ECO-202", "Juan López");
        assertEquals("RUTA-02", detalle.rutaClave());
        assertEquals("ECO-202", detalle.unidadNumeroEconomico());
        assertEquals("Juan López", detalle.conductorNombre());
    }

    @Test
    @DisplayName("Verificar utilidad de conversión de Días")
    void verificarUtilidadDias() {
        assertEquals('L', Dias.letra(DayOfWeek.MONDAY));
        assertEquals('X', Dias.letra(DayOfWeek.WEDNESDAY));
        assertEquals('D', Dias.letra(DayOfWeek.SUNDAY));

        assertTrue(Dias.opera("L,X,V", 'L'));
        assertTrue(Dias.opera("L,X,V", 'X'));
        assertFalse(Dias.opera("L,X,V", 'M'));

        assertTrue(Dias.operaLunesASabado("D,L"));
        assertFalse(Dias.operaLunesASabado("D"));

        assertEquals("Lun, Mié, Vie", Dias.etiqueta("L,X,V"));
        assertEquals("Lun–Sáb", Dias.etiqueta("L,M,X,J,V,S"));
    }
}
