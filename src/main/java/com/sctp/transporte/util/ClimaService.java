package com.sctp.transporte.util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Consulta clima, calidad del aire y UV en Open-Meteo (gratis, sin API key).
 */
public class ClimaService {

    /** temperatura (NaN si no hay dato), código WMO, si es de día, índice UV (-1 si no hay), AQI (-1 si no hay). */
    public record Clima(double temperatura, int codigo, boolean esDia, double uv, int aqi) {}

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    public static CompletableFuture<Clima> obtener(double lat, double lon) {
        String urlClima = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon
                + "&current=temperature_2m,weather_code,is_day&timezone=auto";
        String urlAire = "https://air-quality-api.open-meteo.com/v1/air-quality?latitude=" + lat + "&longitude=" + lon
                + "&current=us_aqi,uv_index&timezone=auto";

        CompletableFuture<String> clima = pedir(urlClima);
        // Si falla el aire, seguimos mostrando el clima
        CompletableFuture<String> aire = pedir(urlAire).exceptionally(e -> "");

        return clima.thenCombine(aire, (c, a) -> new Clima(
                numero(c, "temperature_2m", Double.NaN),
                (int) numero(c, "weather_code", 3),
                numero(c, "is_day", 1) == 1,
                numero(a, "uv_index", -1),
                (int) Math.round(numero(a, "us_aqi", -1))
        ));
    }

    private static CompletableFuture<String> pedir(String url) {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        return HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenApply(r -> {
                    if (r.statusCode() != 200) {
                        throw new RuntimeException("HTTP " + r.statusCode());
                    }
                    return r.body();
                });
    }

    /** Lee un número del JSON sin necesitar librerías externas. */
    private static double numero(String json, String clave, double porDefecto) {
        if (json == null || json.isEmpty()) return porDefecto;
        Matcher m = Pattern.compile("\"" + clave + "\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)").matcher(json);
        return m.find() ? Double.parseDouble(m.group(1)) : porDefecto;
    }
}