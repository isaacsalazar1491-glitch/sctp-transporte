package com.sctp.transporte;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class MainApp {

    private static Stage primaryStage;

    public static void main(String[] args) {
        // Llamar a Application.launch desde una clase que NO extiende Application
        // evita el error "faltan los componentes de JavaFX runtime" en JDK 21
        Application.launch(FxApp.class, args);
    }

    public static class FxApp extends Application {
        @Override
        public void start(Stage stage) {
            primaryStage = stage;
            mostrarVista("/fxml/main.fxml", "SCTP - Sistema de Control de Transporte Público", 1150, 620);
        }
    }

    public static void mostrarVista(String rutaFxml, String titulo, int ancho, int alto) {
        try {
            URL fxmlUrl = MainApp.class.getResource(rutaFxml);
            if (fxmlUrl == null) {
                System.err.println("No se encontró el archivo FXML en: " + rutaFxml);
                return;
            }
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            Scene scene = new Scene(root, ancho, alto);
            primaryStage.setTitle(titulo);
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
            primaryStage.show();
        } catch (IOException e) {
            System.err.println("Error al cargar la vista " + rutaFxml + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}