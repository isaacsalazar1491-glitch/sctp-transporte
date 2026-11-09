package com.sctp.transporte.controller;

import com.sctp.transporte.model.IncidenciaPublica;
import com.sctp.transporte.model.Llegada;
import com.sctp.transporte.model.Parada;
import com.sctp.transporte.model.Ruta;
import com.sctp.transporte.model.RutaResumen;
import com.sctp.transporte.model.Salida;
import com.sctp.transporte.model.UnidadDisponible;
import com.sctp.transporte.service.ConsultaPublicaService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.util.List;

public class ConsultaPublicaController {

    @FXML private TextField txtBuscar;
    @FXML private ListView<RutaResumen> listaRutas;
    @FXML private Label lblRutaSeleccionada;
    @FXML private ListView<Parada> listaParadas;
    @FXML private ListView<String> listaLlegadas;
    @FXML private ListView<String> listaHorarios;
    @FXML private ListView<String> listaUnidades;
    @FXML private ListView<String> listaIncidencias;

    private final ConsultaPublicaService service = new ConsultaPublicaService();

    @FXML
    public void initialize() {
        configurarCeldas();

        // Al seleccionar una ruta de la lista izquierda, carga sus detalles
        listaRutas.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, nueva) -> {
            if (nueva != null) {
                cargarDetalleRuta(nueva.ruta());
            }
        });

        // Al seleccionar una parada, calcula las próximas llegadas estimadas
        listaParadas.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, parada) -> {
            if (parada != null) {
                cargarProximasLlegadas(parada.id());
            }
        });

        cargarRutas("");
    }

    @FXML
    public void handleBuscar() {
        cargarRutas(txtBuscar.getText());
    }

    @FXML
    public void handleLimpiar() {
        txtBuscar.clear();
        cargarRutas("");
    }

    private void cargarRutas(String filtro) {
        try {
            List<RutaResumen> resultados = service.buscarRutas(filtro);
            listaRutas.setItems(FXCollections.observableArrayList(resultados));
            if (!resultados.isEmpty()) {
                listaRutas.getSelectionModel().selectFirst();
            } else {
                lblRutaSeleccionada.setText("No se encontraron rutas con ese criterio");
                listaParadas.getItems().clear();
                listaLlegadas.getItems().clear();
                listaHorarios.getItems().clear();
                listaUnidades.getItems().clear();
            }
            cargarIncidencias(null);
        } catch (Exception e) {
            lblRutaSeleccionada.setText("Error al consultar rutas: " + e.getMessage());
        }
    }

    private void cargarDetalleRuta(Ruta r) {
        lblRutaSeleccionada.setText("Ruta [" + r.clave() + "] " + r.nombre() + " (" + r.origen() + " ➔ " + r.destino() + ")");

        // 1. Paradas
        List<Parada> paradas = service.paradas(r.id());
        listaParadas.setItems(FXCollections.observableArrayList(paradas));
        listaLlegadas.getItems().clear();
        if (!paradas.isEmpty()) {
            listaParadas.getSelectionModel().selectFirst();
        }

        // 2. Horarios
        List<Salida> salidas = service.horarios(r.id());
        listaHorarios.setItems(FXCollections.observableArrayList(
                salidas.stream().map(s -> "🕒 " + s.hora() + " hrs  |  Unidad: " + s.unidad() + "  |  Días: " + s.dias()).toList()
        ));

        // 3. Unidades disponibles
        List<UnidadDisponible> unidades = service.disponibilidad(r.id());
        listaUnidades.setItems(FXCollections.observableArrayList(
                unidades.stream().map(u -> "🚌 Unidad " + u.unidad() + " (Cap: " + u.capacidad() + ") - Estado: " + u.estado()
                        + " | Próx. salida: " + (u.proximaSalida() != null ? u.proximaSalida() : "Sin salidas hoy")).toList()
        ));

        // 4. Incidencias de esta ruta
        cargarIncidencias(r.id());
    }

    private void cargarProximasLlegadas(int idParada) {
        List<Llegada> llegadas = service.proximasLlegadas(idParada, 10);
        if (llegadas.isEmpty()) {
            listaLlegadas.setItems(FXCollections.observableArrayList("No hay más llegadas programadas por hoy para esta parada."));
        } else {
            listaLlegadas.setItems(FXCollections.observableArrayList(
                    llegadas.stream().map(l -> "⏱️ Llega a las " + l.llegada() + " (en " + l.enMinutos() + " min) - Unidad: " + l.salida().unidad()).toList()
            ));
        }
    }

    private void cargarIncidencias(Integer idRuta) {
        List<IncidenciaPublica> inc = service.incidencias(idRuta);
        if (inc.isEmpty()) {
            listaIncidencias.setItems(FXCollections.observableArrayList("✅ Sin incidencias activas en este momento."));
        } else {
            listaIncidencias.setItems(FXCollections.observableArrayList(
                    inc.stream().map(Record::toString).toList()
            ));
        }
    }

    private void configurarCeldas() {
        listaRutas.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(RutaResumen item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    Ruta r = item.ruta();
                    String prox = item.proximaSalida() != null ? item.proximaSalida().toString() : "Terminado por hoy";
                    setText("[" + r.clave() + "] " + r.nombre() + "\n"
                            + r.origen() + " ➔ " + r.destino() + "\n"
                            + "Unidades: " + item.unidades() + "  |  Próx. salida: " + prox);
                }
            }
        });

        listaParadas.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Parada item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.orden() + ". " + item.nombre());
                }
            }
        });
    }
}