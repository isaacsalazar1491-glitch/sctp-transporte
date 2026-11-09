package com.sctp.transporte.controller;

import com.sctp.transporte.config.Db;
import com.sctp.transporte.dao.UnidadDAO;
import com.sctp.transporte.model.Unidad;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

public class UnidadesController {

    @FXML private TableView<Unidad> tablaUnidades;
    @FXML private TableColumn<Unidad, Integer> colId;
    @FXML private TableColumn<Unidad, String> colClave;
    @FXML private TableColumn<Unidad, Integer> colCapacidad;
    @FXML private TableColumn<Unidad, Object> colEstado;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colClave.setCellValueFactory(new PropertyValueFactory<>("clave"));
        colCapacidad.setCellValueFactory(new PropertyValueFactory<>("capacidad"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

        cargarUnidades();
    }

    private void cargarUnidades() {
        try {
            List<Unidad> lista = Db.read(c -> new UnidadDAO(c).listar(true));
            tablaUnidades.setItems(FXCollections.observableArrayList(lista));
        } catch (Exception e) {
            System.err.println("Error al cargar unidades: " + e.getMessage());
        }
    }
}