package com.sctp.transporte.ui;

import com.sctp.transporte.config.MapsConfig;
import com.sctp.transporte.model.Parada;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

/**
 * Mapa de OpenStreetMap (Leaflet dentro de un WebView, gratuito y sin clave) que muestra ÚNICAMENTE las paradas de una ruta,
 * es decir, los puntos donde el camión sube o baja pasajeros. En modo editable permite hacer clic para colocar una
 * parada nueva y arrastrar marcadores para moverlos.
 *
 * <p>La página se sirve desde un servidor HTTP local (solo loopback) para que los mapas reciban un origen http://localhost.
 */
public class MapaParadas extends StackPane {
    private static HttpServer servidor;

    private final WebView web = new WebView();
    private final WebEngine motor = web.getEngine();
    private final Label mensaje = new Label();
    private final VBox panelAviso = new VBox(8);
    private final Puente puente = new Puente(); // referencia fuerte: si se recolecta, JS pierde el puente
    private final BooleanProperty disponible = new SimpleBooleanProperty(false);

    private boolean listo;
    private List<Parada> paradas = List.of();
    private boolean ajustarAlCargar = true;
    private Integer resaltada;
    private boolean editable;
    private Double tempLat, tempLng;

    private BiConsumer<Double, Double> alClic = (a, b) -> {};
    private BiConsumer<Integer, double[]> alMover = (id, ll) -> {};
    private IntConsumer alSeleccionar = id -> {};
    private Consumer<String> alAviso = m -> {};

    public MapaParadas() {
        getStyleClass().add("mapa");
        setMinHeight(260);

        Label titulo = new Label("Mapa no disponible");
        titulo.getStyleClass().add("mapa-aviso-titulo");
        mensaje.setWrapText(true);
        mensaje.getStyleClass().add("mapa-aviso-texto");
        panelAviso.getChildren().addAll(titulo, mensaje);
        panelAviso.setAlignment(Pos.CENTER);
        panelAviso.setMaxWidth(440);
        panelAviso.setMaxHeight(Region.USE_PREF_SIZE);
        panelAviso.getStyleClass().add("mapa-aviso");

        web.setContextMenuEnabled(false);
        motor.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36");
        getChildren().addAll(web, panelAviso);

        panelAviso.setVisible(false);
        panelAviso.setManaged(false);
        motor.getLoadWorker().stateProperty().addListener((o, a, estado) -> {
            if (estado == javafx.concurrent.Worker.State.SUCCEEDED) {
                ((JSObject) motor.executeScript("window")).setMember("java", puente);
                motor.executeScript("window.javaListo()");
            }
        });
        try {
            motor.load("http://localhost:" + iniciarServidor() + "/mapa.html");
        } catch (RuntimeException e) {
            mostrarAviso("No se pudo iniciar el mapa: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ API pública
    /** true cuando el mapa cargó correctamente; si es false, la pantalla debe permitir capturar coordenadas a mano. */
    public BooleanProperty disponibleProperty() { return disponible; }

    public void setParadas(List<Parada> nuevas, boolean ajustar) {
        paradas = List.copyOf(nuevas);
        ajustarAlCargar = ajustar;
        if (listo) enviarParadas();
    }

    public void setResaltada(Integer idParada) {
        resaltada = idParada;
        if (listo) js("window.sctp.resaltar(" + (idParada == null ? "null" : idParada) + ")");
    }

    public void setEditable(boolean v) {
        editable = v;
        if (listo) js("window.sctp.editable(" + v + ")");
    }

    /** Marca la ubicación elegida para una parada nueva (null, null = quitar el marcador). */
    public void setTemporal(Double lat, Double lng) {
        tempLat = lat;
        tempLng = lng;
        if (listo) enviarTemporal();
    }

    public void buscar(String texto) {
        if (listo && texto != null && !texto.isBlank()) js("window.sctp.buscar(" + literal(texto.trim()) + ")");
    }

    public void setAlClic(BiConsumer<Double, Double> f) { alClic = f; }
    public void setAlMover(BiConsumer<Integer, double[]> f) { alMover = f; }
    public void setAlSeleccionar(IntConsumer f) { alSeleccionar = f; }
    public void setAlAviso(Consumer<String> f) { alAviso = f; }

    // ------------------------------------------------------------------ interno
    private void mostrarAviso(String texto) {
        disponible.set(false);
        listo = false;
        mensaje.setText(texto);
        web.setVisible(false);
        panelAviso.setVisible(true);
        panelAviso.setManaged(true);
    }

    private void enviarParadas() {
        StringBuilder sb = new StringBuilder("[");
        for (Parada p : paradas) {
            if (sb.length() > 1) sb.append(',');
            sb.append(String.format(Locale.ROOT, "{\"id\":%d,\"orden\":%d,\"nombre\":%s,\"lat\":%s,\"lng\":%s}",
                    p.id(), p.orden(), jsonTexto(p.nombre()),
                    p.latitud() == null ? "null" : p.latitud().toString(),
                    p.longitud() == null ? "null" : p.longitud().toString()));
        }
        sb.append(']');
        js("window.sctp.paradas(" + literal(sb.toString()) + "," + ajustarAlCargar + ")");
    }

    private void enviarTemporal() {
        js(tempLat == null || tempLng == null ? "window.sctp.temporal(null,null)"
                : String.format(Locale.ROOT, "window.sctp.temporal(%s,%s)", tempLat, tempLng));
    }

    private void js(String codigo) {
        try {
            motor.executeScript(codigo);
        } catch (RuntimeException e) {
            System.err.println("[mapa] " + e.getMessage());
        }
    }

    /** Texto como literal de cadena JavaScript (entre comillas, con escapes). */
    private static String literal(String s) {
        StringBuilder sb = new StringBuilder("'");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '\'' -> sb.append("\\'");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '<' -> sb.append("\\x3C");
                case ' ' -> sb.append("\\u2028");
                case ' ' -> sb.append("\\u2029");
                default -> sb.append(c);
            }
        }
        return sb.append('\'').toString();
    }

    /** Texto como cadena JSON (entre comillas dobles). */
    private static String jsonTexto(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c < 0x20 ? String.format("\\u%04x", (int) c) : String.valueOf(c));
            }
        }
        return sb.append('"').toString();
    }

    private static synchronized int iniciarServidor() {
        if (servidor != null) return servidor.getAddress().getPort();
        try {
            String plantilla;
            try (InputStream in = MapaParadas.class.getResourceAsStream("/map/mapa.html")) {
                if (in == null) throw new IllegalStateException("No se encontró map/mapa.html");
                plantilla = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            byte[] html = plantilla
                    .replace("__LAT__", String.format(Locale.ROOT, "%s", MapsConfig.centroLat()))
                    .replace("__LNG__", String.format(Locale.ROOT, "%s", MapsConfig.centroLng()))
                    .replace("__ZOOM__", String.valueOf(MapsConfig.zoom()))
                    .getBytes(StandardCharsets.UTF_8);
            HttpServer s = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
            s.createContext("/mapa.html", ex -> {
                ex.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                ex.sendResponseHeaders(200, html.length);
                try (var out = ex.getResponseBody()) { out.write(html); }
            });
            s.setExecutor(Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "sctp-mapa-http");
                t.setDaemon(true);
                return t;
            }));
            s.start();
            servidor = s;
            return s.getAddress().getPort();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Métodos que JavaScript invoca (deben ser públicos). */
    public class Puente {
        public void mapaListo() {
            listo = true;
            disponible.set(true);
            enviarParadas();
            js("window.sctp.editable(" + editable + ")");
            if (resaltada != null) js("window.sctp.resaltar(" + resaltada + ")");
            enviarTemporal();
        }

        public void clicMapa(double lat, double lng) {
            // 6 decimales (≈ 11 cm) coinciden con NUMERIC(9,6) de la base de datos.
            alClic.accept(Math.round(lat * 1e6) / 1e6, Math.round(lng * 1e6) / 1e6);
        }

        public void moverParada(int id, double lat, double lng) {
            alMover.accept(id, new double[] {Math.round(lat * 1e6) / 1e6, Math.round(lng * 1e6) / 1e6});
        }

        public void seleccionar(int id) { alSeleccionar.accept(id); }

        public void avisoBusqueda(String texto) { alAviso.accept(texto); }

        public void errorMapa(String texto) { Platform.runLater(() -> mostrarAviso(texto)); }
    }
}
