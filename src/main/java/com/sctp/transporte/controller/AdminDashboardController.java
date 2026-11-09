package com.sctp.transporte.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public class AdminDashboardController {

    @FXML private StackPane panelCentral;

    @FXML
    public void irConductores() { cargarVista("/fxml/conductores.fxml"); }

    @FXML
    public void irUnidades() { cargarVista("/fxml/unidades.fxml"); }

    @FXML
    public void irRutas() { cargarVista("/fxml/rutas.fxml"); }

    @FXML
    public void irCorridas() { cargarVista("/fxml/corridas.fxml"); }

    @FXML
    public void irIncidencias() { cargarVista("/fxml/incidencias.fxml"); }

    private void cargarVista(String rutaFxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(rutaFxml));
            Parent vista = loader.load();
            panelCentral.getChildren().setAll(vista);
        } catch (IOException e) {
            System.err.println("Error al cargar la vista " + rutaFxml + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}