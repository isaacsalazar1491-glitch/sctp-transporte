package com.sctp.transporte.controller;

import com.sctp.transporte.ui.Navegador;
import java.util.LinkedHashMap;
import java.util.Map;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

/** Estructura de la administración: cambia la pantalla del centro según la navegación del encabezado. */
public class MainController {
    private static MainController actual;

    @FXML private StackPane contenido;
    @FXML private Button navInicio, navRutas, navUnidades, navConductores, navCorridas, navIncidencias;

    private final Map<String, Button> botones = new LinkedHashMap<>();

    /** Cambia de pantalla desde otro controlador (p. ej. los accesos directos del resumen). */
    public static void navegar(String vista) {
        if (actual != null) actual.mostrar(vista);
    }

    @FXML
    private void initialize() {
        actual = this;
        botones.put("admin_dashboard", navInicio);
        botones.put("rutas", navRutas);
        botones.put("unidades", navUnidades);
        botones.put("conductores", navConductores);
        botones.put("corridas", navCorridas);
        botones.put("incidencias", navIncidencias);
        mostrar("admin_dashboard");
    }

    private void mostrar(String vista) {
        Parent p = Navegador.cargar(vista);
        contenido.getChildren().setAll(p);
        botones.forEach((v, b) -> {
            b.getStyleClass().remove("nav-link-active");
            if (v.equals(vista)) b.getStyleClass().add("nav-link-active");
        });
    }

    @FXML private void irInicio() { mostrar("admin_dashboard"); }
    @FXML private void irRutas() { mostrar("rutas"); }
    @FXML private void irUnidades() { mostrar("unidades"); }
    @FXML private void irConductores() { mostrar("conductores"); }
    @FXML private void irCorridas() { mostrar("corridas"); }
    @FXML private void irIncidencias() { mostrar("incidencias"); }

    @FXML
    private void cerrarSesion() {
        actual = null;
        Navegador.cerrarSesion();
    }
}
