package com.sctp.transporte.controller;

import com.sctp.transporte.config.Db;
import com.sctp.transporte.dao.ConsultaDAO;
import com.sctp.transporte.model.IncidenciaPublica;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

public class IncidenciasController {

    @FXML private TableView<IncidenciaPublica> tablaIncidencias;
    @FXML private TableColumn<IncidenciaPublica, Object> colId;
    @FXML private TableColumn<IncidenciaPublica, Object> colRuta;
    @FXML private TableColumn<IncidenciaPublica, String> colDescripcion;
    @FXML private TableColumn<IncidenciaPublica, Object> colFecha;

    @FXML
    public void initialize() {
        colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        // Carga las incidencias públicas actuales registradas en el sistema
        cargarIncidencias();
    }

    private void cargarIncidencias() {
        try {
            List<IncidenciaPublica> lista = Db.read(c -> new ConsultaDAO(c).incidenciasPublicas(null));
            tablaIncidencias.setItems(FXCollections.observableArrayList(lista));
        } catch (Exception e) {
            System.err.println("Error al cargar incidencias: " + e.getMessage());
        }
    }
}