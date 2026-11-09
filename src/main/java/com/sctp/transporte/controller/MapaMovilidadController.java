package com.sctp.transporte.controller;

import com.sun.net.httpserver.HttpServer;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class MapaMovilidadController {

    @FXML private WebView webViewMapa;
    @FXML private TextField txtBuscadorRuta;

    private WebEngine webEngine;
    private static HttpServer servidorMapa;
    private static int puertoMapa = -1;

    @FXML
    public void initialize() {
        if (webViewMapa != null) {
            webViewMapa.setContextMenuEnabled(false);

            webEngine = webViewMapa.getEngine();
            webEngine.setJavaScriptEnabled(true);
            webEngine.setUserAgent("SCTP-Transporte-App/1.0 (JavaFX; EstadoDeMexico)");

            webEngine.getLoadWorker().stateProperty().addListener((obs, anterior, estado) -> {
                if (estado == Worker.State.SUCCEEDED) {
                    refrescarMapa();
                }
            });

            webViewMapa.widthProperty().addListener((obs, oldW, newW) -> refrescarMapa());
            webViewMapa.heightProperty().addListener((obs, oldH, newH) -> refrescarMapa());

            // Cargar mapa desde localhost para que OpenStreetMap y CARTO acepten las peticiones sin error 403
            iniciarServidorLocalMapa();
            if (puertoMapa > 0) {
                webEngine.load("http://127.0.0.1:" + puertoMapa + "/mapa.html");
            }
        }
    }

    private synchronized void iniciarServidorLocalMapa() {
        if (servidorMapa != null) return;
        try {
            servidorMapa = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            servidorMapa.createContext("/mapa.html", exchange -> {
                try (InputStream is = getClass().getResourceAsStream("/html/mapa.html")) {
                    byte[] bytes = is != null ? is.readAllBytes() : "Error: No se encontró /html/mapa.html".getBytes();
                    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                }
            });
            servidorMapa.setExecutor(null);
            servidorMapa.start();
            puertoMapa = servidorMapa.getAddress().getPort();
        } catch (Exception e) {
            System.err.println("Error al iniciar servidor local del mapa: " + e.getMessage());
        }
    }

    private void refrescarMapa() {
        if (webEngine == null) return;
        try {
            webEngine.executeScript(
                    "setTimeout(function(){ if (typeof map !== 'undefined' && map) { map.invalidateSize(true); } }, 250);");
        } catch (Exception ignored) {
        }
    }

    @FXML
    public void handleBuscar() {
        if (txtBuscadorRuta != null && webEngine != null) {
            String busqueda = txtBuscadorRuta.getText();
            if (busqueda != null && !busqueda.trim().isEmpty()) {
                System.out.println("Buscando ruta: " + busqueda.trim());
            }
        }
    }
}