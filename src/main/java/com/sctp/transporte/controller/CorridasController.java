package com.sctp.transporte.controller;

import com.sctp.transporte.config.Db;
import com.sctp.transporte.dao.ConsultaDAO;
import com.sctp.transporte.model.Salida;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

public class CorridasController {

    @FXML private TableView<Salida> tablaCorridas;
    @FXML private TableColumn<Salida, Object> colId;
    @FXML private TableColumn<Salida, Object> colRuta;
    @FXML private TableColumn<Salida, String> colUnidad;
    @FXML private TableColumn<Salida, Object> colHorario;
    @FXML private TableColumn<Salida, Object> colEstado;

    @FXML
    public void initialize() {
        // Enlazar columnas con las propiedades del modelo Salida
        colUnidad.setCellValueFactory(new PropertyValueFactory<>("unidad"));
        colHorario.setCellValueFactory(new PropertyValueFactory<>("hora"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("dias"));

        cargarCorridas();
    }

    private void cargarCorridas() {
        try {
            // Carga las salidas activas generales desde ConsultaDAO
            List<Salida> lista = Db.read(c -> new ConsultaDAO(c).salidasActivas(1));
            tablaCorridas.setItems(FXCollections.observableArrayList(lista));
        } catch (Exception e) {
            System.err.println("Error al cargar corridas: " + e.getMessage());
        }
    }

    @FXML
    public void handleNuevaCorrida() {
        System.out.println("Programar nueva corrida");
    }
}
