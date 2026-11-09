package com.sctp.transporte.controller;

import com.sctp.transporte.model.Conductor;
import com.sctp.transporte.model.Corrida;
import com.sctp.transporte.model.CorridaDetalle;
import com.sctp.transporte.model.Parada;
import com.sctp.transporte.model.Ruta;
import com.sctp.transporte.model.Unidad;
import com.sctp.transporte.service.AdministracionService;
import com.sctp.transporte.ui.Fmt;
import com.sctp.transporte.ui.Tareas;
import com.sctp.transporte.util.Dias;
import com.sctp.transporte.util.ReglaNegocioException;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;

/** Asignación de corridas (ruta + unidad + conductor + horario). Las reglas las valida el trigger de la BD. */
public class CorridasController {
    /** Opción "sin parada" del selector de parada actual. */
    private static final Parada SIN_PARADA = new Parada(null, 0, "— Sin parada —", 1, 0);

    @FXML private ComboBox<Ruta> comboRuta;
    @FXML private ComboBox<Unidad> comboUnidad;
    @FXML private ComboBox<Conductor> comboConductor;
    @FXML private ComboBox<Parada> comboParada;
    @FXML private TextField hora;
    @FXML private FlowPane dias;
    @FXML private Button asignar;
    @FXML private Label mensaje;
    @FXML private TableView<CorridaDetalle> tabla;
    @FXML private TableColumn<CorridaDetalle, String> colRuta, colUnidad, colConductor, colSalida, colDias;
    @FXML private TableColumn<CorridaDetalle, CorridaDetalle> colAcciones;

    private final AdministracionService servicio = new AdministracionService();

    @FXML
    private void initialize() {
        Fmt.banner(mensaje, null, true);
        comboRuta.setConverter(Fmt.texto(r -> r.clave() + " · " + r.nombre()));
        comboUnidad.setConverter(Fmt.texto(u -> Fmt.eco(u.numeroEconomico()) + " · " + Fmt.etiqueta(u.estado())));
        comboConductor.setConverter(Fmt.texto(Conductor::nombre));
        comboParada.setConverter(Fmt.texto(Parada::nombre));

        colRuta.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().rutaClave()));
        colUnidad.setCellValueFactory(c -> new SimpleStringProperty(Fmt.eco(c.getValue().unidadNumeroEconomico())));
        colConductor.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().conductorNombre()));
        colSalida.setCellValueFactory(c -> new SimpleStringProperty(Fmt.hora(c.getValue().corrida().horaSalida())));
        colDias.setCellValueFactory(c -> new SimpleStringProperty(Dias.etiqueta(c.getValue().corrida().diasOperacion())));
        colAcciones.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        colAcciones.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(CorridaDetalle d, boolean vacio) {
                super.updateItem(d, vacio);
                if (vacio || d == null) { setGraphic(null); return; }
                Button b = new Button("Dar de baja");
                b.getStyleClass().add("link");
                b.setOnAction(e -> darDeBaja(d.corrida().id()));
                setGraphic(b);
            }
        });
        Fmt.altoAjustado(tabla, 3);
        tabla.setPlaceholder(new Label("No hay corridas activas."));

        comboRuta.getSelectionModel().selectedItemProperty().addListener((o, a, r) -> cargarParadas(r));

        Tareas.ejecutar(() -> new Object[] {servicio.listarRutas(true), servicio.listarUnidades(false),
                        servicio.listarConductores(true), servicio.listarCorridas(null)},
                this::pintar, msg -> Fmt.banner(mensaje, msg, true));
    }

    @SuppressWarnings("unchecked")
    private void pintar(Object[] d) {
        comboRuta.getItems().setAll((List<Ruta>) d[0]);
        comboUnidad.getItems().setAll((List<Unidad>) d[1]);
        comboConductor.getItems().setAll((List<Conductor>) d[2]);
        pintarCorridas((List<CorridaDetalle>) d[3]);
    }

    private void pintarCorridas(List<CorridaDetalle> todas) {
        tabla.getItems().setAll(todas.stream().filter(c -> c.corrida().activa()).toList());
    }

    private void cargarParadas(Ruta r) {
        comboParada.getItems().clear();
        if (r == null) return;
        Tareas.ejecutar(() -> servicio.listarParadas(r.id()), (List<Parada> ps) -> {
            comboParada.getItems().add(SIN_PARADA);
            comboParada.getItems().addAll(ps);
            comboParada.getSelectionModel().selectFirst();
        }, msg -> Fmt.banner(mensaje, msg, true));
    }

    private void recargarTabla() {
        Tareas.ejecutar(() -> servicio.listarCorridas(null), this::pintarCorridas, msg -> Fmt.banner(mensaje, msg, true));
    }

    @FXML
    private void asignar() {
        Corrida k;
        try {
            if (comboRuta.getValue() == null) throw new ReglaNegocioException("Elige una ruta");
            if (comboUnidad.getValue() == null) throw new ReglaNegocioException("Elige una unidad");
            if (comboConductor.getValue() == null) throw new ReglaNegocioException("Elige un conductor");
            String seleccion = dias.getChildren().stream().map(n -> (ToggleButton) n).filter(ToggleButton::isSelected)
                    .map(b -> (String) b.getUserData()).collect(Collectors.joining(","));
            if (seleccion.isEmpty()) throw new ReglaNegocioException("Elige al menos un día de operación");
            Parada p = comboParada.getValue();
            Integer idParada = p == null || p == SIN_PARADA ? null : p.id();
            LocalTime salida = Fmt.parseHora(hora.getText());
            k = new Corrida(null, comboRuta.getValue().id(), comboUnidad.getValue().id(), comboConductor.getValue().id(),
                    idParada, salida, seleccion, true);
        } catch (ReglaNegocioException e) {
            Fmt.banner(mensaje, "Asignación rechazada\n" + e.getMessage(), true);
            return;
        }
        Corrida nueva = k;
        asignar.setDisable(true);
        Fmt.banner(mensaje, null, true);
        Tareas.ejecutar(() -> servicio.crearCorrida(nueva), id -> {
            asignar.setDisable(false);
            Fmt.banner(mensaje, "Corrida asignada correctamente.", false);
            recargarTabla();
        }, msg -> {
            asignar.setDisable(false);
            Fmt.banner(mensaje, "Asignación rechazada\n" + msg, true);
        });
    }

    private void darDeBaja(int id) {
        Tareas.accion(() -> servicio.darDeBajaCorrida(id),
                () -> { Fmt.banner(mensaje, "Corrida dada de baja.", false); recargarTabla(); },
                msg -> Fmt.banner(mensaje, msg, true));
    }
}
