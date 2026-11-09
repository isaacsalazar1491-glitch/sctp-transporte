package com.sctp.transporte.controller;

import com.sctp.transporte.model.Conductor;
import com.sctp.transporte.model.Incidencia;
import com.sctp.transporte.model.IncidenciaDetalle;
import com.sctp.transporte.model.Ruta;
import com.sctp.transporte.model.Unidad;
import com.sctp.transporte.model.enums.EstadoIncidencia;
import com.sctp.transporte.model.enums.SeveridadIncidencia;
import com.sctp.transporte.service.AdministracionService;
import com.sctp.transporte.ui.Fmt;
import com.sctp.transporte.ui.Tareas;
import com.sctp.transporte.util.ReglaNegocioException;
import java.util.List;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;

/** Registro de incidencias y avance de estado (ABIERTA → EN_ATENCION → CERRADA, sin saltos). */
public class IncidenciasController {
    private static final int MAX_DESCRIPCION = 300;
    /** Opción "sin conductor" del selector opcional. */
    private static final Conductor SIN_CONDUCTOR = new Conductor(null, "— Sin conductor —", "-", java.time.LocalDate.now(), "-", true);

    @FXML private HBox filtros, pasoActual;
    @FXML private TableView<IncidenciaDetalle> tabla;
    @FXML private TableColumn<IncidenciaDetalle, String> colRuta, colUnidad, colDescripcion;
    @FXML private TableColumn<IncidenciaDetalle, SeveridadIncidencia> colSeveridad;
    @FXML private TableColumn<IncidenciaDetalle, EstadoIncidencia> colEstado;
    @FXML private Label textoPaso, mensaje, contador;
    @FXML private Button btnAtencion, btnCerrar, registrar;
    @FXML private ComboBox<Ruta> comboRuta;
    @FXML private ComboBox<Unidad> comboUnidad;
    @FXML private ComboBox<SeveridadIncidencia> comboSeveridad;
    @FXML private ComboBox<Conductor> comboConductor;
    @FXML private TextArea descripcion;

    private final AdministracionService servicio = new AdministracionService();
    private final ToggleGroup grupoFiltro = new ToggleGroup();
    private EstadoIncidencia filtro; // null = todas

    @FXML
    private void initialize() {
        Fmt.banner(mensaje, null, true);
        for (var n : filtros.getChildren()) {
            ToggleButton b = (ToggleButton) n;
            b.setToggleGroup(grupoFiltro);
        }
        // Un filtro siempre queda activo: si se vuelve a pulsar el mismo, se mantiene.
        grupoFiltro.selectedToggleProperty().addListener((o, a, nuevo) -> {
            if (nuevo == null) { a.setSelected(true); return; }
            String v = (String) nuevo.getUserData();
            filtro = "TODAS".equals(v) ? null : EstadoIncidencia.valueOf(v);
            recargar();
        });

        colRuta.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().rutaClave()));
        colUnidad.setCellValueFactory(c -> new SimpleStringProperty(Fmt.eco(c.getValue().unidadNumeroEconomico())));
        colDescripcion.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().incidencia().descripcion()));
        colSeveridad.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().incidencia().severidad()));
        Fmt.columnaChip(colSeveridad, Fmt::etiqueta, Fmt::claseSeveridad);
        colEstado.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().incidencia().estado()));
        Fmt.columnaChip(colEstado, Fmt::etiqueta, Fmt::claseEstadoIncidencia);
        Fmt.altoAjustado(tabla, 3);
        tabla.setPlaceholder(new Label("No hay incidencias con este filtro."));
        tabla.getSelectionModel().selectedItemProperty().addListener((o, a, i) -> actualizarPaso(i));

        comboRuta.setConverter(Fmt.texto(r -> r.clave() + " · " + r.nombre()));
        comboUnidad.setConverter(Fmt.texto(u -> Fmt.eco(u.numeroEconomico())));
        comboConductor.setConverter(Fmt.texto(Conductor::nombre));
        comboSeveridad.getItems().setAll(SeveridadIncidencia.values());
        comboSeveridad.setConverter(Fmt.texto(Fmt::etiqueta));
        comboSeveridad.setValue(SeveridadIncidencia.MODERADA);

        // Límite de 300 caracteres con contador (igual que la columna de la BD).
        descripcion.setTextFormatter(new javafx.scene.control.TextFormatter<String>(ch ->
                ch.getControlNewText().length() <= MAX_DESCRIPCION ? ch : null));
        descripcion.textProperty().addListener((o, a, t) -> contador.setText(t.length() + " / " + MAX_DESCRIPCION));

        Tareas.ejecutar(() -> new Object[] {servicio.listarRutas(false), servicio.listarUnidades(false), servicio.listarConductores(true)},
                d -> {
                    @SuppressWarnings("unchecked") List<Ruta> rutas = (List<Ruta>) d[0];
                    @SuppressWarnings("unchecked") List<Unidad> unidades = (List<Unidad>) d[1];
                    @SuppressWarnings("unchecked") List<Conductor> conductores = (List<Conductor>) d[2];
                    comboRuta.getItems().setAll(rutas);
                    comboUnidad.getItems().setAll(unidades);
                    comboConductor.getItems().add(SIN_CONDUCTOR);
                    comboConductor.getItems().addAll(conductores);
                    comboConductor.getSelectionModel().selectFirst();
                }, msg -> Fmt.banner(mensaje, msg, true));
        recargar();
    }

    private void recargar() {
        Integer seleccionada = tabla.getSelectionModel().getSelectedItem() == null ? null
                : tabla.getSelectionModel().getSelectedItem().incidencia().id();
        Tareas.ejecutar(() -> servicio.listarIncidencias(filtro), (List<IncidenciaDetalle> lista) -> {
            tabla.getItems().setAll(lista);
            if (seleccionada != null) lista.stream().filter(i -> seleccionada.equals(i.incidencia().id())).findFirst()
                    .ifPresent(i -> tabla.getSelectionModel().select(i));
            else actualizarPaso(null);
        }, msg -> Fmt.banner(mensaje, msg, true));
    }

    private void actualizarPaso(IncidenciaDetalle i) {
        pasoActual.getChildren().clear();
        if (i == null) {
            textoPaso.setText("Selecciona una incidencia para cambiar su estado.");
            pasoActual.getChildren().add(textoPaso);
            btnAtencion.setDisable(true);
            btnCerrar.setDisable(true);
            return;
        }
        EstadoIncidencia e = i.incidencia().estado();
        Label actual = Fmt.chip(Fmt.etiqueta(e), Fmt.claseEstadoIncidencia(e));
        Label intro = new Label("Incidencia seleccionada:");
        intro.getStyleClass().add("muted");
        pasoActual.getChildren().addAll(intro, actual);
        if (e == EstadoIncidencia.CERRADA) {
            Label fin = new Label("· ya está cerrada");
            fin.getStyleClass().add("muted");
            pasoActual.getChildren().add(fin);
        } else {
            EstadoIncidencia sig = e == EstadoIncidencia.ABIERTA ? EstadoIncidencia.EN_ATENCION : EstadoIncidencia.CERRADA;
            Label flecha = new Label("→ siguiente paso");
            flecha.getStyleClass().add("muted");
            pasoActual.getChildren().addAll(flecha, Fmt.chip(Fmt.etiqueta(sig), Fmt.claseEstadoIncidencia(sig)));
        }
        btnAtencion.setDisable(e != EstadoIncidencia.ABIERTA);
        btnCerrar.setDisable(e != EstadoIncidencia.EN_ATENCION);
    }

    @FXML private void pasarAAtencion() { cambiarEstado(EstadoIncidencia.EN_ATENCION); }
    @FXML private void cerrar() { cambiarEstado(EstadoIncidencia.CERRADA); }

    private void cambiarEstado(EstadoIncidencia nuevo) {
        IncidenciaDetalle i = tabla.getSelectionModel().getSelectedItem();
        if (i == null) return;
        int id = i.incidencia().id();
        btnAtencion.setDisable(true);
        btnCerrar.setDisable(true);
        Tareas.accion(() -> servicio.cambiarEstadoIncidencia(id, nuevo), () -> {
            Fmt.banner(mensaje, "Incidencia en estado " + Fmt.etiqueta(nuevo) + ".", false);
            tabla.getSelectionModel().clearSelection();
            Tareas.ejecutar(() -> servicio.listarIncidencias(filtro), (List<IncidenciaDetalle> lista) -> {
                tabla.getItems().setAll(lista);
                lista.stream().filter(x -> x.incidencia().id() == id).findFirst().ifPresent(x -> tabla.getSelectionModel().select(x));
            }, msg -> Fmt.banner(mensaje, msg, true));
        }, msg -> {
            Fmt.banner(mensaje, msg, true);
            actualizarPaso(i);
        });
    }

    @FXML
    private void registrar() {
        Incidencia nueva;
        try {
            if (comboRuta.getValue() == null) throw new ReglaNegocioException("Elige una ruta");
            if (comboUnidad.getValue() == null) throw new ReglaNegocioException("Elige una unidad");
            Conductor c = comboConductor.getValue();
            Integer idConductor = c == null || c == SIN_CONDUCTOR ? null : c.id();
            nueva = Incidencia.nueva(comboRuta.getValue().id(), comboUnidad.getValue().id(), idConductor,
                    descripcion.getText(), comboSeveridad.getValue());
        } catch (ReglaNegocioException e) {
            Fmt.banner(mensaje, e.getMessage(), true);
            return;
        }
        Incidencia i = nueva;
        registrar.setDisable(true);
        Tareas.ejecutar(() -> servicio.registrarIncidencia(i), id -> {
            registrar.setDisable(false);
            descripcion.clear();
            Fmt.banner(mensaje, i.severidad() == SeveridadIncidencia.GRAVE
                    ? "Incidencia registrada. La unidad pasó a mantenimiento." : "Incidencia registrada.", false);
            recargar();
        }, msg -> { registrar.setDisable(false); Fmt.banner(mensaje, msg, true); });
    }
}
