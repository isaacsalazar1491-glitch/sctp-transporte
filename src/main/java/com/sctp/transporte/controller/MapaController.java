package com.sctp.transporte.controller;

import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

public class MapaController {

    @FXML private WebView webViewMapa;

    @FXML
    public void initialize() {
        WebEngine webEngine = webViewMapa.getEngine();
        webEngine.setJavaScriptEnabled(true);

        // Cargar el archivo HTML del mapa desde los recursos
        String urlMapa = getClass().getResource("/html/mapa.html").toExternalForm();
        webEngine.load(urlMapa);
    }

    // Método opcional para mover el mapa o poner marcadores de paradas desde Java
    public void centrarUbicacion(double lat, double lng, String nombre) {
        WebEngine engine = webViewMapa.getEngine();
        engine.executeScript("map.setView([" + lat + ", " + lng + "], 15);");
        engine.executeScript("agregarMarcador(" + lat + ", lng, '" + nombre + "');");
    }
}