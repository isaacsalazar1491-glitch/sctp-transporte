package com.sctp.transporte.service;

import com.sctp.transporte.config.Db;
import com.sctp.transporte.dao.ConductorDAO;
import com.sctp.transporte.dao.ConsultaDAO;
import com.sctp.transporte.dao.CorridaDAO;
import com.sctp.transporte.dao.IncidenciaDAO;
import com.sctp.transporte.dao.ParadaDAO;
import com.sctp.transporte.dao.RutaDAO;
import com.sctp.transporte.dao.UnidadDAO;
import com.sctp.transporte.model.Conductor;
import com.sctp.transporte.model.Corrida;
import com.sctp.transporte.model.CorridaDetalle;
import com.sctp.transporte.model.Incidencia;
import com.sctp.transporte.model.IncidenciaDetalle;
import com.sctp.transporte.model.LicenciaPorVencer;
import com.sctp.transporte.model.Parada;
import com.sctp.transporte.model.ResumenUnidades;
import com.sctp.transporte.model.Ruta;
import com.sctp.transporte.model.Unidad;
import com.sctp.transporte.model.enums.EstadoIncidencia;
import com.sctp.transporte.model.enums.EstadoRuta;
import com.sctp.transporte.model.enums.EstadoUnidad;
import com.sctp.transporte.util.ReglaNegocioException;
import java.util.List;
import java.util.function.Supplier;

/**
 * Operaciones del administrador: rutas, paradas, unidades, conductores, corridas (asignaciones y horarios),
 * incidencias y reportes. Cada escritura es una transacción; las reglas de integridad las aplican la BD
 * (restricciones y triggers) y sus mensajes se propagan como {@link ReglaNegocioException}.
 * Las bajas son lógicas: nunca se elimina una ruta, unidad o conductor con registros asociados.
 */
public class AdministracionService {

    private static void existe(int filas, String entidad) {
        if (filas == 0) throw new ReglaNegocioException(entidad + " no encontrada");
    }

    private static <T> T o404(java.util.Optional<T> o, Supplier<String> msg) {
        return o.orElseThrow(() -> new ReglaNegocioException(msg.get()));
    }

    // ---------------------------------------------------------------- Rutas
    public List<Ruta> listarRutas(boolean soloActivas) { return Db.read(c -> new RutaDAO(c).listar(soloActivas)); }
    public Ruta obtenerRuta(int id) { return o404(Db.read(c -> new RutaDAO(c).buscar(id)), () -> "Ruta no encontrada"); }
    public int crearRuta(Ruta r) { return Db.tx(c -> new RutaDAO(c).crear(r)); }
    public void actualizarRuta(Ruta r) { Db.tx(c -> { existe(new RutaDAO(c).actualizar(r), "Ruta"); return null; }); }
    public void darDeBajaRuta(int id) {
        Db.tx(c -> { existe(new RutaDAO(c).cambiarEstado(id, EstadoRuta.INACTIVA), "Ruta"); return null; });
    }

    // -------------------------------------------------------------- Paradas
    public List<Parada> listarParadas(int idRuta) { return Db.read(c -> new ParadaDAO(c).listarPorRuta(idRuta)); }
    public Parada obtenerParada(int id) { return o404(Db.read(c -> new ParadaDAO(c).buscar(id)), () -> "Parada no encontrada"); }
    public int crearParada(Parada p) { return Db.tx(c -> new ParadaDAO(c).crear(p)); }
    public void actualizarParada(Parada p) { Db.tx(c -> { existe(new ParadaDAO(c).actualizar(p), "Parada"); return null; }); }
    public void eliminarParada(int id) { Db.tx(c -> { existe(new ParadaDAO(c).eliminar(id), "Parada"); return null; }); }

    // -------------------------------------------------------------- Unidades
    public List<Unidad> listarUnidades(boolean soloOperativas) { return Db.read(c -> new UnidadDAO(c).listar(soloOperativas)); }
    public Unidad obtenerUnidad(int id) { return o404(Db.read(c -> new UnidadDAO(c).buscar(id)), () -> "Unidad no encontrada"); }
    public int crearUnidad(Unidad u) { return Db.tx(c -> new UnidadDAO(c).crear(u)); }
    public void actualizarUnidad(Unidad u) { Db.tx(c -> { existe(new UnidadDAO(c).actualizar(u), "Unidad"); return null; }); }
    public void cambiarEstadoUnidad(int id, EstadoUnidad estado) {
        Db.tx(c -> { existe(new UnidadDAO(c).cambiarEstado(id, estado), "Unidad"); return null; });
    }
    public void darDeBajaUnidad(int id) { cambiarEstadoUnidad(id, EstadoUnidad.BAJA); }

    // ------------------------------------------------------------ Conductores
    public List<Conductor> listarConductores(boolean soloActivos) { return Db.read(c -> new ConductorDAO(c).listar(soloActivos)); }
    public Conductor obtenerConductor(int id) { return o404(Db.read(c -> new ConductorDAO(c).buscar(id)), () -> "Conductor no encontrado"); }
    public int crearConductor(Conductor d) { return Db.tx(c -> new ConductorDAO(c).crear(d)); }
    public void actualizarConductor(Conductor d) { Db.tx(c -> { existe(new ConductorDAO(c).actualizar(d), "Conductor"); return null; }); }
    public void darDeBajaConductor(int id) {
        Db.tx(c -> { existe(new ConductorDAO(c).cambiarActivo(id, false), "Conductor"); return null; });
    }

    // --------------------------------------------- Corridas (asignación y horario)
    /** Todas las corridas, o solo las de un conductor si idConductor no es null. */
    public List<CorridaDetalle> listarCorridas(Integer idConductor) { return Db.read(c -> new CorridaDAO(c).listarDetalle(idConductor)); }
    public Corrida obtenerCorrida(int id) { return o404(Db.read(c -> new CorridaDAO(c).buscar(id)), () -> "Corrida no encontrada"); }
    /** Si el trigger rechaza (unidad no disponible, licencia vencida...), se revierte y se propaga su mensaje. */
    public int crearCorrida(Corrida k) { return Db.tx(c -> new CorridaDAO(c).crear(k)); }
    public void actualizarCorrida(Corrida k) { Db.tx(c -> { existe(new CorridaDAO(c).actualizar(k), "Corrida"); return null; }); }
    public void darDeBajaCorrida(int id) {
        Db.tx(c -> { existe(new CorridaDAO(c).cambiarActiva(id, false), "Corrida"); return null; });
    }

    // ------------------------------------------------------------ Incidencias
    public List<IncidenciaDetalle> listarIncidencias(EstadoIncidencia estado) { return Db.read(c -> new IncidenciaDAO(c).listarDetalle(estado)); }
    public Incidencia obtenerIncidencia(int id) { return o404(Db.read(c -> new IncidenciaDAO(c).buscar(id)), () -> "Incidencia no encontrada"); }
    /** Una incidencia GRAVE pasa la unidad a MANTENIMIENTO (trigger) en la misma transacción. */
    public int registrarIncidencia(Incidencia i) { return Db.tx(c -> new IncidenciaDAO(c).crear(i)); }
    public void actualizarIncidencia(Incidencia i) { Db.tx(c -> { existe(new IncidenciaDAO(c).actualizar(i), "Incidencia"); return null; }); }
    /** ABIERTA → EN_ATENCION → CERRADA (procedimiento almacenado). */
    public void cambiarEstadoIncidencia(int id, EstadoIncidencia nuevo) {
        Db.tx(c -> { new IncidenciaDAO(c).cambiarEstado(id, nuevo); return null; });
    }

    // --------------------------------------------------------------- Reportes
    public ResumenUnidades resumenUnidades() { return Db.read(c -> new ConsultaDAO(c).resumenUnidades()); }
    public List<LicenciaPorVencer> licenciasPorVencer() { return Db.read(c -> new ConsultaDAO(c).licenciasPorVencer()); }
}
