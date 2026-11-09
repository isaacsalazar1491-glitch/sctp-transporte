package com.sctp.transporte.controller;

import com.sctp.transporte.model.Conductor;
import com.sctp.transporte.service.AdministracionService;
import com.sctp.transporte.ui.Fmt;
import com.sctp.transporte.ui.Tareas;
import com.sctp.transporte.util.ReglaNegocioException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

/** Alta, edición y baja lógica de conductores; la vigencia de la licencia se resalta por color. */
public class ConductoresController {
    @FXML private TableView<Conductor> tabla;
    @FXML private TableColumn<Conductor, String> colNombre, colLicencia, colEstado;
    @FXML private TableColumn<Conductor, Conductor> colVigencia, colAcciones;
    @FXML private Label tituloForm, mensaje;
    @FXML private TextField nombre, licencia, telefono;
    @FXML private DatePicker vigencia;
    @FXML private Button guardar, baja;

    private final AdministracionService servicio = new AdministracionService();
    private Conductor editando; // null = conductor nuevo

    @FXML
    private void initialize() {
        colNombre.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().nombre()));
        colLicencia.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().numeroLicencia()));
        colEstado.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().activo() ? "Activo" : "Baja"));
        colVigencia.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        Fmt.columnaChip(colVigencia, c -> Fmt.fecha(c.vigenciaLicencia()), c -> Fmt.claseVigencia(c.vigenciaLicencia()));
        colAcciones.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        colAcciones.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Conductor d, boolean vacio) {
                super.updateItem(d, vacio);
                if (vacio || d == null) { setGraphic(null); return; }
                Button b = new Button("Editar");
                b.getStyleClass().add("link");
                b.setOnAction(e -> editar(d));
                setGraphic(b);
            }
        });
        Fmt.altoAjustado(tabla, 4);
        tabla.setPlaceholder(new Label("Aún no hay conductores registrados."));
        vigencia.setConverter(new StringConverter<>() {
            @Override public String toString(LocalDate d) { return Fmt.fecha(d); }
            @Override public LocalDate fromString(String s) {
                if (s == null || s.isBlank()) return null;
                try { return LocalDate.parse(s.trim(), Fmt.FECHA); }
                catch (DateTimeParseException e) { return null; }
            }
        });
        tabla.getSelectionModel().selectedItemProperty().addListener((o, a, d) -> { if (d != null) editar(d); });
        nuevo();
        recargar(null, null);
    }

    private void recargar(Integer seleccionar, String exito) {
        Tareas.ejecutar(() -> servicio.listarConductores(false), (List<Conductor> lista) -> {
            tabla.getItems().setAll(lista);
            if (seleccionar != null) lista.stream().filter(d -> seleccionar.equals(d.id())).findFirst().ifPresent(this::editar);
            if (exito != null) Fmt.banner(mensaje, exito, false);
        }, msg -> Fmt.banner(mensaje, msg, true));
    }

    @FXML
    private void nuevo() {
        editando = null;
        tabla.getSelectionModel().clearSelection();
        tituloForm.setText("Conductor nuevo");
        nombre.clear();
        licencia.clear();
        telefono.clear();
        vigencia.setValue(null);
        baja.setDisable(true);
        Fmt.banner(mensaje, null, true);
    }

    private void editar(Conductor d) {
        editando = d;
        tituloForm.setText("Editar conductor");
        nombre.setText(d.nombre());
        licencia.setText(d.numeroLicencia());
        telefono.setText(d.telefono());
        vigencia.setValue(d.vigenciaLicencia());
        baja.setDisable(!d.activo());
        Fmt.banner(mensaje, null, true);
    }

    @FXML
    private void guardar() {
        Conductor c;
        try {
            LocalDate vig = vigencia.getConverter().fromString(vigencia.getEditor().getText());
            if (vig == null) vig = vigencia.getValue();
            if (vig == null && !vigencia.getEditor().getText().isBlank())
                throw new ReglaNegocioException("La vigencia debe tener el formato DD/MM/AAAA");
            c = new Conductor(editando == null ? null : editando.id(), nombre.getText(), licencia.getText(), vig,
                    telefono.getText(), editando == null || editando.activo());
        } catch (ReglaNegocioException e) {
            Fmt.banner(mensaje, e.getMessage(), true);
            return;
        }
        Conductor aGuardar = c;
        boolean nuevo = editando == null;
        guardar.setDisable(true);
        Tareas.ejecutar(() -> {
            if (nuevo) return servicio.crearConductor(aGuardar);
            servicio.actualizarConductor(aGuardar);
            return aGuardar.id();
        }, id -> {
            guardar.setDisable(false);
            recargar(id, "Conductor guardado.");
        }, msg -> { guardar.setDisable(false); Fmt.banner(mensaje, msg, true); });
    }

    @FXML
    private void darDeBaja() {
        if (editando == null) return;
        int id = editando.id();
        Tareas.accion(() -> servicio.darDeBajaConductor(id),
                () -> recargar(id, "Conductor dado de baja."),
                msg -> Fmt.banner(mensaje, msg, true));
    }
}
