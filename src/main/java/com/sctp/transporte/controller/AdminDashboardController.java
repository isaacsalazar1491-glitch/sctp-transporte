package com.sctp.transporte.controller;

import com.sctp.transporte.model.IncidenciaDetalle;
import com.sctp.transporte.model.LicenciaPorVencer;
import com.sctp.transporte.model.ResumenUnidades;
import com.sctp.transporte.model.Unidad;
import com.sctp.transporte.model.enums.EstadoIncidencia;
import com.sctp.transporte.model.enums.EstadoUnidad;
import com.sctp.transporte.service.AdministracionService;
import com.sctp.transporte.ui.Fmt;
import com.sctp.transporte.ui.Tareas;
import java.util.List;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Resumen de operación: unidades por estado, licencias por vencer e incidencias abiertas. */
public class AdminDashboardController {
    private static final int MAX_INCIDENCIAS = 5;

    @FXML private Label mensaje, numActivas, numMantenimiento, numBaja;
    @FXML private TableView<Unidad> tablaUnidades;
    @FXML private TableColumn<Unidad, String> colPlaca, colEco, colCapacidad;
    @FXML private TableColumn<Unidad, EstadoUnidad> colEstado;
    @FXML private TableView<LicenciaPorVencer> tablaLicencias;
    @FXML private TableColumn<LicenciaPorVencer, String> colConductor;
    @FXML private TableColumn<LicenciaPorVencer, LicenciaPorVencer> colVence;
    @FXML private VBox incidenciasAbiertas;

    private final AdministracionService servicio = new AdministracionService();

    @FXML
    private void initialize() {
        Fmt.banner(mensaje, null, true);
        colPlaca.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().placa()));
        colEco.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().numeroEconomico()));
        colCapacidad.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().capacidad())));
        colEstado.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().estado()));
        Fmt.columnaChip(colEstado, Fmt::etiqueta, Fmt::claseEstadoUnidad);
        colConductor.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().conductor()));
        colVence.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        Fmt.columnaChip(colVence, l -> Fmt.fecha(l.vence()), l -> Fmt.claseVigencia(l.vence()));
        Fmt.altoAjustado(tablaUnidades, 3);
        Fmt.altoAjustado(tablaLicencias, 2);
        tablaUnidades.setPlaceholder(new Label("Aún no hay unidades registradas."));
        tablaLicencias.setPlaceholder(new Label("Ninguna licencia por vencer."));

        Tareas.ejecutar(() -> new Object[] {
                        servicio.resumenUnidades(),
                        servicio.listarUnidades(false),
                        servicio.licenciasPorVencer(),
                        servicio.listarIncidencias(EstadoIncidencia.ABIERTA)},
                this::pintar, msg -> Fmt.banner(mensaje, msg, true));
    }

    @SuppressWarnings("unchecked")
    private void pintar(Object[] d) {
        ResumenUnidades r = (ResumenUnidades) d[0];
        numActivas.setText(String.valueOf(r.disponibles() + r.enRuta()));
        numMantenimiento.setText(String.valueOf(r.mantenimiento()));
        numBaja.setText(String.valueOf(r.baja()));
        tablaUnidades.getItems().setAll((List<Unidad>) d[1]);
        tablaLicencias.getItems().setAll((List<LicenciaPorVencer>) d[2]);

        List<IncidenciaDetalle> abiertas = (List<IncidenciaDetalle>) d[3];
        incidenciasAbiertas.getChildren().clear();
        if (abiertas.isEmpty()) {
            Label vacio = new Label("No hay incidencias abiertas.");
            vacio.getStyleClass().add("muted");
            incidenciasAbiertas.getChildren().add(vacio);
        }
        for (IncidenciaDetalle i : abiertas.stream().limit(MAX_INCIDENCIAS).toList()) {
            Label desc = new Label(i.incidencia().descripcion() + " · " + Fmt.eco(i.unidadNumeroEconomico()));
            desc.setWrapText(true);
            HBox.setHgrow(desc, Priority.ALWAYS);
            HBox fila = new HBox(12, Fmt.chip(Fmt.etiqueta(i.incidencia().severidad()), Fmt.claseSeveridad(i.incidencia().severidad())), desc);
            fila.getStyleClass().add("aviso-item");
            incidenciasAbiertas.getChildren().add(fila);
        }
    }

    @FXML private void nuevaUnidad() { MainController.navegar("unidades"); }
    @FXML private void verIncidencias() { MainController.navegar("incidencias"); }
}
