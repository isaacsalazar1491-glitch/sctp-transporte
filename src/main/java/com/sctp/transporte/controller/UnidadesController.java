package com.sctp.transporte.controller;

import com.sctp.transporte.model.Unidad;
import com.sctp.transporte.model.enums.EstadoUnidad;
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
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

/** Alta, edición y baja lógica de unidades. */
public class UnidadesController {
    @FXML private TableView<Unidad> tabla;
    @FXML private TableColumn<Unidad, String> colPlaca, colEco, colCapacidad;
    @FXML private TableColumn<Unidad, EstadoUnidad> colEstado;
    @FXML private TableColumn<Unidad, Unidad> colAcciones;
    @FXML private Label tituloForm, mensaje;
    @FXML private TextField placa, eco, capacidad;
    @FXML private ComboBox<EstadoUnidad> estado;
    @FXML private Button guardar, baja;

    private final AdministracionService servicio = new AdministracionService();
    private Unidad editando; // null = unidad nueva

    @FXML
    private void initialize() {
        colPlaca.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().placa()));
        colEco.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().numeroEconomico()));
        colCapacidad.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().capacidad())));
        colEstado.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().estado()));
        Fmt.columnaChip(colEstado, Fmt::etiqueta, Fmt::claseEstadoUnidad);
        colAcciones.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        colAcciones.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Unidad u, boolean vacio) {
                super.updateItem(u, vacio);
                if (vacio || u == null) { setGraphic(null); return; }
                Button b = new Button("Editar");
                b.getStyleClass().add("link");
                b.setOnAction(e -> editar(u));
                setGraphic(b);
            }
        });
        Fmt.altoAjustado(tabla, 4);
        tabla.setPlaceholder(new Label("Aún no hay unidades registradas."));
        estado.getItems().setAll(EstadoUnidad.values());
        estado.setConverter(Fmt.texto(Fmt::etiqueta));
        tabla.getSelectionModel().selectedItemProperty().addListener((o, a, u) -> { if (u != null) editar(u); });
        nueva();
        recargar(null, null);
    }

    private void recargar(Integer seleccionar, String exito) {
        Tareas.ejecutar(() -> servicio.listarUnidades(false), (List<Unidad> lista) -> {
            tabla.getItems().setAll(lista);
            if (seleccionar != null) lista.stream().filter(u -> seleccionar.equals(u.id())).findFirst().ifPresent(this::editar);
            if (exito != null) Fmt.banner(mensaje, exito, false);
        }, msg -> Fmt.banner(mensaje, msg, true));
    }

    @FXML
    private void nueva() {
        editando = null;
        tabla.getSelectionModel().clearSelection();
        tituloForm.setText("Unidad nueva");
        placa.clear();
        eco.clear();
        capacidad.clear();
        estado.setValue(EstadoUnidad.DISPONIBLE);
        baja.setDisable(true);
        Fmt.banner(mensaje, null, true);
    }

    private void editar(Unidad u) {
        editando = u;
        tituloForm.setText("Editar unidad");
        placa.setText(u.placa());
        eco.setText(u.numeroEconomico());
        capacidad.setText(String.valueOf(u.capacidad()));
        estado.setValue(u.estado());
        baja.setDisable(u.estado() == EstadoUnidad.BAJA);
        Fmt.banner(mensaje, null, true);
    }

    @FXML
    private void guardar() {
        Unidad u;
        try {
            int cap;
            try {
                cap = Integer.parseInt(capacidad.getText().trim());
            } catch (NumberFormatException e) {
                throw new ReglaNegocioException("La capacidad debe ser un número entero");
            }
            u = new Unidad(editando == null ? null : editando.id(), placa.getText(), eco.getText(), cap, estado.getValue());
        } catch (ReglaNegocioException e) {
            Fmt.banner(mensaje, e.getMessage(), true);
            return;
        }
        Unidad aGuardar = u;
        boolean nueva = editando == null;
        guardar.setDisable(true);
        Tareas.ejecutar(() -> {
            if (nueva) return servicio.crearUnidad(aGuardar);
            servicio.actualizarUnidad(aGuardar);
            return aGuardar.id();
        }, id -> {
            guardar.setDisable(false);
            recargar(id, "Unidad guardada.");
        }, msg -> { guardar.setDisable(false); Fmt.banner(mensaje, msg, true); });
    }

    @FXML
    private void darDeBaja() {
        if (editando == null) return;
        int id = editando.id();
        Tareas.accion(() -> servicio.darDeBajaUnidad(id),
                () -> { recargar(id, "Unidad dada de baja."); },
                msg -> Fmt.banner(mensaje, msg, true));
    }
}
