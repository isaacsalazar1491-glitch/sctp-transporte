package com.sctp.transporte.controller;

import com.sctp.transporte.util.ClimaService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class MainController {

    @FXML private StackPane contenedorCentral;
    @FXML private VBox vistaDashboardInicio;
    @FXML private Label lblEstadoSesion;
    @FXML private Label lblUsuarioMenu;
    @FXML private Button btnInicio;
    @FXML private Button btnVerRutas;
    @FXML private Button btnPanelAdmin;
    @FXML private Button btnAccesoUsuario;
    @FXML private Button btnCerrarSesion;

    // Coordenadas y ubicación inicial mientras detecta la ubicación exacta
    private String ubicacionActual = "San Mateo Atenco";
    private double latActual = 19.2638;
    private double lonActual = -99.5372;

    @FXML private Label lblFecha, lblUbicacion, lblTemp, lblClimaDesc;
    @FXML private Label lblAireNivel, lblAireDetalle, lblUvValor, lblUvNivel;
    @FXML private StackPane contIconoClima, iconoAire;
    @FXML private Region barraAire, barraUV;

    private Timeline relojClima;

    private record Nivel(String texto, String color) {}

    @FXML
    public void initialize() {
        iniciarClimaYFecha();
        detectarUbicacionExactaEnTiempoReal();
    }

    @FXML
    public void mostrarInicio() {
        if (contenedorCentral != null && vistaDashboardInicio != null) {
            contenedorCentral.getChildren().setAll(vistaDashboardInicio);
        }
        if (lblEstadoSesion != null && (btnCerrarSesion == null || !btnCerrarSesion.isVisible())) {
            lblEstadoSesion.setText("Modo: Consulta Pública (Sin sesión)");
        }
    }

    @FXML
    public void mostrarConsultaPublica() {
        cargarVistaCentro("/fxml/mapa_movilidad.fxml", "Modo: Movilidad y Rutas");
    }

    @FXML
    public void mostrarModuloSeguridad() {
        cargarVistaCentro("/fxml/seguridad.fxml", "Modo: Seguridad y Emergencias");
    }

    @FXML
    public void mostrarModuloIncidencias() {
        cargarVistaCentro("/fxml/incidencias.fxml", "Modo: Incidencias Activas");
    }

    @FXML
    public void mostrarPanelAdmin() {
        cargarVistaCentro("/fxml/admin_dashboard.fxml", "Modo: Administrador");
    }

    private void cargarVistaCentro(String rutaFxml, String estado) {
        try {
            URL archivoUrl = getClass().getResource(rutaFxml);
            if (archivoUrl == null) {
                System.err.println("No se encontró el archivo FXML en la ruta: " + rutaFxml);
                return;
            }
            FXMLLoader loader = new FXMLLoader(archivoUrl);
            Parent vista = loader.load();
            if (contenedorCentral != null) {
                contenedorCentral.getChildren().setAll(vista);
            }
            if (lblEstadoSesion != null) {
                lblEstadoSesion.setText(estado);
            }
        } catch (Exception e) {
            System.err.println("Error al cargar la vista " + rutaFxml + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void abrirDialogoLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();

            LoginController loginController = loader.getController();
            loginController.setMainController(this);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("SCTP - Iniciar Sesión");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void actualizarSesionAdmin(String usuario) {
        if (lblEstadoSesion != null) {
            lblEstadoSesion.setText("Sesión activa: " + usuario);
        }
        if (lblUsuarioMenu != null) {
            lblUsuarioMenu.setText("Admin: " + usuario);
        }
        if (btnAccesoUsuario != null) {
            btnAccesoUsuario.setVisible(false);
            btnAccesoUsuario.setManaged(false);
        }
        if (btnCerrarSesion != null) {
            btnCerrarSesion.setVisible(true);
            btnCerrarSesion.setManaged(true);
        }
        if (btnPanelAdmin != null) {
            btnPanelAdmin.setVisible(true);
            btnPanelAdmin.setManaged(true);
        }
        mostrarPanelAdmin();
    }

    @FXML
    public void cerrarSesion() {
        if (lblEstadoSesion != null) {
            lblEstadoSesion.setText("Modo: Consulta Pública (Sin sesión)");
        }
        if (lblUsuarioMenu != null) {
            lblUsuarioMenu.setText("¿Eres administrador?");
        }
        if (btnAccesoUsuario != null) {
            btnAccesoUsuario.setVisible(true);
            btnAccesoUsuario.setManaged(true);
        }
        if (btnCerrarSesion != null) {
            btnCerrarSesion.setVisible(false);
            btnCerrarSesion.setManaged(false);
        }
        if (btnPanelAdmin != null) {
            btnPanelAdmin.setVisible(false);
            btnPanelAdmin.setManaged(false);
        }
        mostrarInicio();
    }

    // =====================================================
    //  GEOLOCALIZACIÓN EXACTA EN TIEMPO REAL Y CLIMA
    // =====================================================

    private void iniciarClimaYFecha() {
        if (lblFecha != null) {
            String f = LocalDate.now().format(
                    DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-MX")));
            lblFecha.setText(Character.toUpperCase(f.charAt(0)) + f.substring(1));
        }

        if (lblUbicacion != null) {
            lblUbicacion.setText("📍 " + ubicacionActual);
        }

        if (iconoAire != null) {
            iconoAire.getChildren().setAll(crearPatronPuntosAire("#0b2e2b"));
        }

        cargarClima(latActual, lonActual);

        relojClima = new Timeline(new KeyFrame(javafx.util.Duration.minutes(15), e -> cargarClima(latActual, lonActual)));
        relojClima.setCycleCount(Timeline.INDEFINITE);
        relojClima.play();
    }

    /**
     * Obtiene las coordenadas exactas desde el sensor de ubicación de Windows (Wi-Fi / GPS)
     * y consulta el municipio o localidad exacta en tiempo real con OpenStreetMap.
     */
    private void detectarUbicacionExactaEnTiempoReal() {
        new Thread(() -> {
            try {
                // 1. Obtener latitud y longitud reales desde el servicio de ubicación de Windows via PowerShell
                String comandoPs = "Add-Type -AssemblyName System.Device; " +
                        "$w = New-Object System.Device.Location.GeoCoordinateWatcher; " +
                        "$w.Start(); " +
                        "$c = 0; while (($w.Status -ne 'Ready') -and ($c -lt 30)) { Start-Sleep -Milliseconds 100; $c++ }; " +
                        "if ($w.Position.Location.IsUnknown -eq $false) { " +
                        "$lat = $w.Position.Location.Latitude.ToString([System.Globalization.CultureInfo]::InvariantCulture); " +
                        "$lon = $w.Position.Location.Longitude.ToString([System.Globalization.CultureInfo]::InvariantCulture); " +
                        "Write-Output \"$lat,$lon\" }";

                ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-NoProfile", "-Command", comandoPs);
                Process p = pb.start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String salida = reader.readLine();

                double latDetectada = Double.NaN;
                double lonDetectada = Double.NaN;

                if (salida != null && salida.contains(",")) {
                    String[] partes = salida.trim().split(",");
                    latDetectada = Double.parseDouble(partes[0]);
                    lonDetectada = Double.parseDouble(partes[1]);
                } else {
                    // Respaldo por IP si la ubicación de Windows está desactivada
                    URI uriIp = URI.create("http://ip-api.com/json/?fields=status,lat,lon");
                    HttpURLConnection connIp = (HttpURLConnection) uriIp.toURL().openConnection();
                    connIp.setConnectTimeout(3000);
                    connIp.setReadTimeout(3000);
                    if (connIp.getResponseCode() == 200) {
                        BufferedReader rIp = new BufferedReader(new InputStreamReader(connIp.getInputStream()));
                        String jsonIp = rIp.readLine();
                        rIp.close();
                        if (jsonIp != null && jsonIp.contains("\"status\":\"success\"")) {
                            latDetectada = Double.parseDouble(extraerJsonValor(jsonIp, "lat"));
                            lonDetectada = Double.parseDouble(extraerJsonValor(jsonIp, "lon"));
                        }
                    }
                }

                if (!Double.isNaN(latDetectada) && !Double.isNaN(lonDetectada)) {
                    final double latFinal = latDetectada;
                    final double lonFinal = lonDetectada;

                    // 2. Convertir coordenadas en el nombre exacto del municipio/ciudad con Nominatim
                    String urlNom = String.format(Locale.US,
                            "https://nominatim.openstreetmap.org/reverse?format=json&lat=%.6f&lon=%.6f&zoom=14&addressdetails=1",
                            latFinal, lonFinal);

                    HttpURLConnection connNom = (HttpURLConnection) URI.create(urlNom).toURL().openConnection();
                    connNom.setRequestProperty("User-Agent", "SCTP-Transporte-App/1.0");
                    connNom.setConnectTimeout(4000);
                    connNom.setReadTimeout(4000);

                    String nombreLugar = ubicacionActual;
                    if (connNom.getResponseCode() == 200) {
                        BufferedReader rNom = new BufferedReader(new InputStreamReader(connNom.getInputStream(), StandardCharsets.UTF_8));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = rNom.readLine()) != null) sb.append(line);
                        rNom.close();

                        String jsonNom = sb.toString();
                        String town = extraerJsonValor(jsonNom, "town");
                        String municipality = extraerJsonValor(jsonNom, "municipality");
                        String city = extraerJsonValor(jsonNom, "city");
                        String village = extraerJsonValor(jsonNom, "village");
                        String county = extraerJsonValor(jsonNom, "county");
                        String state = extraerJsonValor(jsonNom, "state");

                        if (!town.isEmpty()) nombreLugar = town;
                        else if (!municipality.isEmpty()) nombreLugar = municipality;
                        else if (!city.isEmpty()) nombreLugar = city;
                        else if (!village.isEmpty()) nombreLugar = village;
                        else if (!county.isEmpty()) nombreLugar = county;
                        else if (!state.isEmpty()) nombreLugar = state;
                    }

                    final String lugarFinal = nombreLugar;
                    Platform.runLater(() -> {
                        ubicacionActual = lugarFinal;
                        latActual = latFinal;
                        lonActual = lonFinal;
                        if (lblUbicacion != null) {
                            lblUbicacion.setText("📍 " + ubicacionActual);
                        }
                        cargarClima(latActual, lonActual);
                    });
                }
            } catch (Exception e) {
                System.err.println("No se pudo obtener la geolocalización exacta: " + e.getMessage());
            }
        }).start();
    }

    private String extraerJsonValor(String json, String clave) {
        try {
            String search = "\"" + clave + "\":";
            int start = json.indexOf(search);
            if (start == -1) return "";
            start += search.length();
            while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '"')) {
                if (json.charAt(start) == '"') {
                    start++;
                    int end = json.indexOf("\"", start);
                    return end != -1 ? json.substring(start, end).trim() : "";
                }
                start++;
            }
            int end = json.indexOf(",", start);
            if (end == -1) end = json.indexOf("}", start);
            return end != -1 ? json.substring(start, end).trim().replace("\"", "") : "";
        } catch (Exception e) {
            return "";
        }
    }

    private void cargarClima(double lat, double lon) {
        ClimaService.obtener(lat, lon)
                .thenAccept(c -> Platform.runLater(() -> mostrarClima(c)))
                .exceptionally(ex -> {
                    Platform.runLater(this::mostrarSinConexion);
                    return null;
                });
    }

    private void mostrarClima(ClimaService.Clima c) {
        if (lblTemp == null) return;

        lblTemp.setText(Double.isNaN(c.temperatura()) ? "--°C" : Math.round(c.temperatura()) + "°C");
        if (lblClimaDesc != null) lblClimaDesc.setText(descripcionClima(c.codigo()));
        if (contIconoClima != null) contIconoClima.getChildren().setAll(crearIconoClima(c.codigo(), c.esDia()));

        Nivel aire = nivelAire(c.aqi());
        if (barraAire != null) barraAire.setStyle("-fx-background-color: " + aire.color() + "; -fx-background-radius: 3;");
        if (iconoAire != null) iconoAire.getChildren().setAll(crearPatronPuntosAire("#0b2e2b"));
        if (lblAireNivel != null) {
            lblAireNivel.setText(aire.texto());
            lblAireNivel.setStyle("-fx-text-fill: #0b2e2b; -fx-font-weight: bold; -fx-font-size: 14px;");
        }
        if (lblAireDetalle != null) {
            lblAireDetalle.setText("Calidad de aire");
        }

        Nivel uv = nivelUv(c.uv());
        if (barraUV != null) barraUV.setStyle("-fx-background-color: " + uv.color() + "; -fx-background-radius: 3;");
        if (lblUvValor != null) lblUvValor.setText(c.uv() >= 0 ? String.valueOf(Math.round(c.uv())) : "0");
        if (lblUvNivel != null) {
            lblUvNivel.setText(uv.texto());
            lblUvNivel.setStyle("-fx-text-fill: #0b2e2b; -fx-font-weight: bold; -fx-font-size: 14px;");
        }
    }

    private void mostrarSinConexion() {
        if (lblTemp == null) return;
        lblTemp.setText("--°C");
        if (lblClimaDesc != null) lblClimaDesc.setText("Sin conexión");
        if (contIconoClima != null) contIconoClima.getChildren().setAll(ico("icon-cloud", "#a9b9c1", 34, 22, 0, 0));
        if (barraAire != null) barraAire.setStyle("-fx-background-color: #cbd5d1; -fx-background-radius: 3;");
        if (barraUV != null) barraUV.setStyle("-fx-background-color: #cbd5d1; -fx-background-radius: 3;");
        if (lblAireNivel != null) lblAireNivel.setText("Sin datos");
        if (lblAireDetalle != null) lblAireDetalle.setText("Calidad de aire");
        if (lblUvValor != null) lblUvValor.setText("-");
        if (lblUvNivel != null) lblUvNivel.setText("Sin datos");
    }

    private Node crearPatronPuntosAire(String colorHex) {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(3);
        grid.setVgap(3);
        grid.setTranslateY(2);

        int[][] mascara = {
                {0, 1, 1, 1, 0},
                {1, 1, 1, 1, 1},
                {1, 1, 1, 1, 1},
                {1, 1, 1, 1, 1},
                {0, 1, 1, 1, 0}
        };

        for (int f = 0; f < 5; f++) {
            for (int col = 0; col < 5; col++) {
                if (mascara[f][col] == 1) {
                    Circle punto = new Circle((f == 2 && col == 2) ? 1.6 : 1.2, Color.web(colorHex));
                    punto.setOpacity((f == 0 || f == 4 || col == 0 || col == 4) ? 0.55 : 0.90);
                    grid.add(punto, col, f);
                }
            }
        }
        return grid;
    }

    private Nivel nivelAire(int aqi) {
        if (aqi < 0)    return new Nivel("Sin datos", "#94a3b8");
        if (aqi <= 50)  return new Nivel("Buena", "#16a34a");
        if (aqi <= 100) return new Nivel("Regular", "#f59e0b");
        if (aqi <= 150) return new Nivel("Mala", "#ff7a00");
        if (aqi <= 200) return new Nivel("Muy mala", "#dc2626");
        return new Nivel("Extrema", "#7e22ce");
    }

    private Nivel nivelUv(double uv) {
        if (uv < 0)  return new Nivel("Sin datos", "#94a3b8");
        if (uv < 3)  return new Nivel("Bajo", "#16a34a");
        if (uv < 6)  return new Nivel("Moderado", "#eab308");
        if (uv < 8)  return new Nivel("Alto", "#ff7a00");
        if (uv < 11) return new Nivel("Muy alto", "#dc2626");
        return new Nivel("Extremo", "#7e22ce");
    }

    private String descripcionClima(int c) {
        return switch (c) {
            case 0 -> "Despejado";
            case 1 -> "Mayormente despejado";
            case 2 -> "Parcialmente nublado";
            case 3 -> "Nublado";
            case 45, 48 -> "Neblina";
            case 51, 53, 55, 56, 57 -> "Llovizna";
            case 61, 63, 65, 66, 67 -> "Lluvia";
            case 71, 73, 75, 77, 85, 86 -> "Nieve";
            case 80, 81, 82 -> "Chubascos";
            case 95, 96, 99 -> "Tormenta";
            default -> "Clima";
        };
    }

    private StackPane crearIconoClima(int cod, boolean dia) {
        StackPane p = new StackPane();
        p.setMinSize(40, 40);
        p.setPrefSize(40, 40);
        p.setMaxSize(40, 40);

        boolean lluvia = (cod >= 51 && cod <= 67) || (cod >= 80 && cod <= 82);
        boolean nieve = (cod >= 71 && cod <= 77) || cod == 85 || cod == 86;

        if (cod == 0 || cod == 1) {
            p.getChildren().add(dia
                    ? ico("icon-sun", "#f59e0b", 28, 28, 0, 0)
                    : ico("icon-moon", "#6d5cae", 25, 25, 0, 0));
        } else if (cod == 2) {
            p.getChildren().addAll(
                    dia ? ico("icon-sun", "#f59e0b", 20, 20, -7, -7)
                            : ico("icon-moon", "#6d5cae", 18, 18, -7, -7),
                    ico("icon-cloud", "#e2e8f0", 28, 18, 3, 3));
        } else if (cod == 45 || cod == 48) {
            p.getChildren().add(ico("icon-cloud", "#cbd5e1", 30, 20, 0, 0));
        } else if (lluvia) {
            p.getChildren().addAll(
                    dia ? ico("icon-sun", "#f59e0b", 16, 16, -8, -9)
                            : ico("icon-moon", "#6d5cae", 16, 16, -8, -9),
                    ico("icon-cloud", "#e2e8f0", 28, 18, 2, -3),
                    ico("icon-drop", "#4f46e5", 6, 9, -6, 10),
                    ico("icon-drop", "#4f46e5", 6, 9, 1, 11),
                    ico("icon-drop", "#4f46e5", 6, 9, 8, 10));
        } else if (nieve) {
            p.getChildren().add(crearCopo());
        } else if (cod >= 95) {
            p.getChildren().addAll(
                    ico("icon-cloud", "#64748b", 28, 18, 0, -4),
                    ico("icon-bolt", "#f59e0b", 10, 14, 2, 9));
        } else {
            p.getChildren().add(ico("icon-cloud", "#cbd5e1", 30, 20, 0, 0));
        }
        return p;
    }

    private Node crearCopo() {
        StackPane s = new StackPane();
        for (double ang : new double[]{0, 60, 120}) {
            Region barra = new Region();
            barra.setStyle("-fx-background-color: #60a5fa; -fx-background-radius: 2;"
                    + "-fx-min-width: 2.5; -fx-pref-width: 2.5; -fx-max-width: 2.5;"
                    + "-fx-min-height: 22; -fx-pref-height: 22; -fx-max-height: 22;");
            barra.setRotate(ang);
            s.getChildren().add(barra);
        }
        return s;
    }

    private Region ico(String forma, String color, double w, double h, double dx, double dy) {
        Region r = new Region();
        r.getStyleClass().addAll("icon", forma);
        r.setStyle("-fx-background-color: " + color + ";"
                + "-fx-min-width: " + w + "; -fx-pref-width: " + w + "; -fx-max-width: " + w + ";"
                + "-fx-min-height: " + h + "; -fx-pref-height: " + h + "; -fx-max-height: " + h + ";");
        r.setTranslateX(dx);
        r.setTranslateY(dy);
        return r;
    }
}