package com.sctp.transporte.controller;

import com.sctp.transporte.config.Db;
import com.sctp.transporte.dao.ConductorDAO;
import com.sctp.transporte.model.Conductor;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

public class ConductoresController {

    @FXML private TextField txtBuscar;
    @FXML private TableView<Conductor> tablaConductores;
    @FXML private TableColumn<Conductor, Integer> colId;
    @FXML private TableColumn<Conductor, String> colNombre;
    @FXML private TableColumn<Conductor, String> colLicencia;
    @FXML private TableColumn<Conductor, String> colVigencia;
    @FXML private TableColumn<Conductor, String> colTelefono;

    @FXML
    public void initialize() {
        // Enlazar las columnas con los campos del modelo Conductor
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colLicencia.setCellValueFactory(new PropertyValueFactory<>("licencia"));
        colVigencia.setCellValueFactory(new PropertyValueFactory<>("vigenciaLicencia"));
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));

        cargarConductores();
    }

    private void cargarConductores() {
        try {
            List<Conductor> lista = Db.read(c -> new ConductorDAO(c).listar(true));
            tablaConductores.setItems(FXCollections.observableArrayList(lista));
        } catch (Exception e) {
            System.err.println("Error al cargar conductores: " + e.getMessage());
        }
    }

    @FXML
    public void handleNuevo() {
        // Aquí puedes abrir un modal de registro más adelante
        System.out.println("Nuevo conductor");
    }
}
