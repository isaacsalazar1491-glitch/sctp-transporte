package com.sctp.transporte.service;

import com.sctp.transporte.config.Db;
import com.sctp.transporte.dao.ConsultaDAO;
import com.sctp.transporte.dao.ParadaDAO;
import com.sctp.transporte.dao.RutaDAO;
import com.sctp.transporte.model.IncidenciaPublica;
import com.sctp.transporte.model.Llegada;
import com.sctp.transporte.model.Parada;
import com.sctp.transporte.model.ResumenIncidenciasRuta;
import com.sctp.transporte.model.Ruta;
import com.sctp.transporte.model.RutaResumen;
import com.sctp.transporte.model.Salida;
import com.sctp.transporte.model.UnidadDisponible;
import com.sctp.transporte.model.enums.EstadoRuta;
import com.sctp.transporte.util.Dias;
import com.sctp.transporte.util.ReglaNegocioException;
import com.sctp.transporte.util.Reloj;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Consulta pública (solo lectura, sin sesión). La llegada es SIEMPRE una estimación:
 * hora de salida + minutos desde el origen; no hay datos en tiempo real. Nunca se expone información del conductor.
 */
public class ConsultaPublicaService {
    private static int minutoDelDia(LocalTime t) { return t.getHour() * 60 + t.getMinute(); }

    public List<Ruta> rutasActivas() { return Db.read(c -> new RutaDAO(c).listar(true)); }

    /** Solo las rutas activas son públicas. */
    public Ruta rutaActiva(int id) {
        Ruta r = Db.read(c -> new RutaDAO(c).buscar(id)).orElseThrow(() -> new ReglaNegocioException("Ruta no encontrada"));
        if (r.estado() != EstadoRuta.ACTIVA) throw new ReglaNegocioException("Ruta no encontrada");
        return r;
    }

    public List<Parada> paradas(int idRuta) { return Db.read(c -> new ParadaDAO(c).listarPorRuta(idRuta)); }

    public Parada parada(int id) {
        return Db.read(c -> new ParadaDAO(c).buscar(id)).orElseThrow(() -> new ReglaNegocioException("Parada no encontrada"));
    }

    /** Horarios: todas las salidas (de unidades operativas) de una ruta, ordenadas por hora. */
    public List<Salida> horarios(int idRuta) { return Db.read(c -> new ConsultaDAO(c).salidasActivas(idRuta)); }

    /** Rutas activas que coinciden con el texto (clave, nombre, origen, destino o parada), con su próxima salida. */
    public List<RutaResumen> buscarRutas(String q) {
        LocalDateTime ahora = Reloj.ahora();
        return Db.read(c -> {
            ConsultaDAO dao = new ConsultaDAO(c);
            String t = q == null ? "" : q.trim().toLowerCase();
            Set<Integer> porParada = t.isEmpty() ? Set.of() : dao.rutasConParada(t);
            List<RutaResumen> out = new ArrayList<>();
            for (Ruta r : new RutaDAO(c).listar(true)) {
                boolean coincide = t.isEmpty() || porParada.contains(r.id())
                        || (r.clave() + " " + r.nombre() + " " + r.origen() + " " + r.destino()).toLowerCase().contains(t);
                if (!coincide) continue;
                List<Salida> ss = dao.salidasActivas(r.id());
                int unidades = (int) ss.stream().map(Salida::unidad).distinct().count();
                out.add(new RutaResumen(r, unidades, proximaSalida(ss, ahora)));
            }
            return out;
        });
    }

    private static LocalTime proximaSalida(List<Salida> ss, LocalDateTime ahora) {
        char hoy = Dias.letra(ahora.getDayOfWeek());
        int min = minutoDelDia(ahora.toLocalTime());
        return ss.stream().filter(s -> Dias.opera(s.dias(), hoy) && minutoDelDia(s.hora()) >= min)
                .map(Salida::hora).min(Comparator.naturalOrder()).orElse(null);
    }

    /** Próximas llegadas de hoy a una parada (estimadas), de la más cercana a la más lejana. */
    public List<Llegada> proximasLlegadas(int idParada, int limite) {
        LocalDateTime ahora = Reloj.ahora();
        char hoy = Dias.letra(ahora.getDayOfWeek());
        int min = minutoDelDia(ahora.toLocalTime());
        List<Llegada> out = new ArrayList<>();
        for (Llegada l : Db.read(c -> new ConsultaDAO(c).llegadasAParada(idParada))) {
            int llegada = minutoDelDia(l.llegada());
            if (!Dias.opera(l.salida().dias(), hoy) || llegada < min) continue;
            out.add(new Llegada(l.salida(), l.llegada(), llegada - min));
        }
        out.sort(Comparator.comparingInt(Llegada::enMinutos));
        return out.size() > limite ? out.subList(0, limite) : out;
    }

    /** Unidades operativas de la ruta con su próxima salida de hoy (null si ya no hay). */
    public List<UnidadDisponible> disponibilidad(int idRuta) {
        LocalDateTime ahora = Reloj.ahora();
        Map<String, UnidadDisponible> porUnidad = new LinkedHashMap<>();
        List<Salida> ss = horarios(idRuta);
        for (Salida s : ss) {
            LocalTime prox = proximaSalida(ss.stream().filter(x -> x.unidad().equals(s.unidad())).toList(), ahora);
            porUnidad.putIfAbsent(s.unidad(), new UnidadDisponible(s.unidad(), s.capacidad(), s.estado(), prox));
        }
        return new ArrayList<>(porUnidad.values());
    }

    /** Incidencias no cerradas (sin conductor); idRuta null = todas. */
    public List<IncidenciaPublica> incidencias(Integer idRuta) { return Db.read(c -> new ConsultaDAO(c).incidenciasPublicas(idRuta)); }

    /** Resumen de incidencias por ruta activa. */
    public List<ResumenIncidenciasRuta> resumenIncidencias() { return Db.read(c -> new ConsultaDAO(c).resumenIncidenciasPorRuta()); }
}
