package com.sctp.transporte.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Ajustes opcionales del mapa (OpenStreetMap + Leaflet, sin clave): centro y zoom iniciales en maps.properties. */
public final class MapsConfig {
    private static final Properties P = new Properties();

    static {
        try (InputStream in = MapsConfig.class.getClassLoader().getResourceAsStream("maps.properties")) {
            if (in != null) P.load(in);
        } catch (IOException ignorado) {
            // sin archivo: el mapa se muestra en modo "no configurado"
        }
    }

    private MapsConfig() {}

    public static double centroLat() { return num("maps.centro.lat", 19.1789); }
    public static double centroLng() { return num("maps.centro.lng", -99.4663); }
    public static int zoom() { return (int) num("maps.zoom", 12); }

    private static double num(String k, double def) {
        try { return Double.parseDouble(P.getProperty(k, "").trim()); } catch (NumberFormatException e) { return def; }
    }
}
