package com.sctp.transporte.controller;

import com.sctp.transporte.config.Db;
import com.sctp.transporte.dao.RutaDAO;
import com.sctp.transporte.model.Ruta;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

public class RutasController {

    @FXML private TextField txtBuscar;
    @FXML private TableView<Ruta> tablaRutas;
    @FXML private TableColumn<Ruta, Integer> colId;
    @FXML private TableColumn<Ruta, String> colClave;
    @FXML private TableColumn<Ruta, String> colNombre;
    @FXML private TableColumn<Ruta, String> colOrigen;
    @FXML private TableColumn<Ruta, String> colDestino;
    @FXML private TableColumn<Ruta, Object> colEstado;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colClave.setCellValueFactory(new PropertyValueFactory<>("clave"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colOrigen.setCellValueFactory(new PropertyValueFactory<>("origen"));
        colDestino.setCellValueFactory(new PropertyValueFactory<>("destino"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

        cargarRutas();
    }

    private void cargarRutas() {
        try {
            List<Ruta> lista = Db.read(c -> new RutaDAO(c).listar(false));
            tablaRutas.setItems(FXCollections.observableArrayList(lista));
        } catch (Exception e) {
            System.err.println("Error al cargar rutas: " + e.getMessage());
        }
    }

    @FXML
    public void handleNuevaRuta() {
        System.out.println("Nueva ruta");
    }
}
