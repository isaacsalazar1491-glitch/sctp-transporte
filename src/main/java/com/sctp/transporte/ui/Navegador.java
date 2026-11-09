package com.sctp.transporte.ui;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Objects;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/** Carga las vistas FXML y cambia entre las pantallas principales (consulta pública, login, administración). */
public final class Navegador {
    private static Scene scene;

    private Navegador() {}

    public static void iniciar(Stage stage) {
        scene = new Scene(new StackPane(), 1280, 820);
        scene.getStylesheets().add(urlCss());
        scene.setFill(Color.web("#F0F4FC"));
        stage.setScene(scene);
    }

    public static String urlCss() {
        return Objects.requireNonNull(Navegador.class.getResource("/css/styles.css"), "styles.css no encontrado").toExternalForm();
    }

    /** Carga /fxml/{vista}.fxml y devuelve su raíz. */
    public static Parent cargar(String vista) {
        URL url = Navegador.class.getResource("/fxml/" + vista + ".fxml");
        if (url == null) throw new IllegalArgumentException("Vista no encontrada: " + vista);
        try {
            return new FXMLLoader(url).load();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo cargar la vista " + vista, e);
        }
    }

    public static void mostrar(String vista) { scene.setRoot(cargar(vista)); }

    public static void irAConsulta() { mostrar("consulta_publica"); }

    public static void irALogin() { mostrar("login"); }

    /** Entra a la administración; si no hay sesión, manda al login. */
    public static void irAAdmin() {
        if (!Sesion.activa()) { irALogin(); return; }
        mostrar("main");
    }

    public static void cerrarSesion() {
        Sesion.cerrar();
        irAConsulta();
    }
}
