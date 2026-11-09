package com.sctp.transporte.controller;

import com.sctp.transporte.model.IncidenciaPublica;
import com.sctp.transporte.model.Llegada;
import com.sctp.transporte.model.Parada;
import com.sctp.transporte.model.Ruta;
import com.sctp.transporte.model.UnidadDisponible;
import com.sctp.transporte.model.enums.EstadoUnidad;
import com.sctp.transporte.service.ConsultaPublicaService;
import com.sctp.transporte.ui.Fmt;
import com.sctp.transporte.ui.MapaParadas;
import com.sctp.transporte.ui.Navegador;
import com.sctp.transporte.ui.Tareas;
import com.sctp.transporte.util.Dias;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Consulta pública: rutas, mapa de paradas, próximas llegadas (estimadas), disponibilidad y avisos. Sin sesión. */
public class ConsultaPublicaController {
    private static final int MAX_LLEGADAS = 8;

    @FXML private ScrollPane scroll;
    @FXML private VBox contenido, seccionRutas, seccionHorarios, seccionDisponibilidad, avisos;
    @FXML private ComboBox<Ruta> comboRuta;
    @FXML private ComboBox<Parada> comboParada;
    @FXML private Label mensaje, chipResumen, tituloLlegadas, tituloDisponibilidad;
    @FXML private MapaParadas mapa;
    @FXML private FlowPane listaParadas, tarjetasUnidades;
    @FXML private TableView<Llegada> tablaLlegadas;
    @FXML private TableColumn<Llegada, String> colSalida, colUnidad, colDias;
    @FXML private TableColumn<Llegada, Llegada> colLlegada;

    private final ConsultaPublicaService servicio = new ConsultaPublicaService();
    private boolean cargando;

    @FXML
    private void initialize() {
        Fmt.banner(mensaje, null, true);
        chipResumen.visibleProperty().bind(chipResumen.textProperty().isNotEmpty());
        chipResumen.managedProperty().bind(chipResumen.visibleProperty());
        comboRuta.setConverter(Fmt.texto(r -> r.clave() + " · " + r.nombre()));
        comboParada.setConverter(Fmt.texto(Parada::nombre));

        colSalida.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(Fmt.hora(c.getValue().salida().hora())));
        colUnidad.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(Fmt.eco(c.getValue().salida().unidad())));
        colDias.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(Dias.etiqueta(c.getValue().salida().dias())));
        colLlegada.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue()));
        colLlegada.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Llegada l, boolean vacio) {
                super.updateItem(l, vacio);
                if (vacio || l == null) { setGraphic(null); return; }
                Label hora = new Label(Fmt.hora(l.llegada()));
                hora.getStyleClass().add("llegada-hora");
                Label falta = new Label(l.enMinutos() == 0 ? "ahora" : "en " + l.enMinutos() + " min");
                falta.getStyleClass().add("muted-small");
                HBox box = new HBox(8, hora, falta);
                box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                setGraphic(box);
            }
        });
        tablaLlegadas.setPlaceholder(new Label("No hay más llegadas hoy a esta parada."));

        // Elegir una parada tocando su marcador en el mapa.
        mapa.setAlSeleccionar(id -> comboParada.getItems().stream().filter(p -> p.id() == id).findFirst()
                .ifPresent(p -> comboParada.getSelectionModel().select(p)));

        comboRuta.getSelectionModel().selectedItemProperty().addListener((o, a, r) -> { if (r != null && !cargando) cargarRuta(r); });
        comboParada.getSelectionModel().selectedItemProperty().addListener((o, a, p) -> { if (p != null && !cargando) alElegirParada(p); });

        Tareas.ejecutar(servicio::rutasActivas, rutas -> {
            comboRuta.getItems().setAll(rutas);
            if (rutas.isEmpty()) Fmt.banner(mensaje, "Por ahora no hay rutas activas para consultar.", true);
            else comboRuta.getSelectionModel().selectFirst();
        }, msg -> Fmt.banner(mensaje, msg, true));
    }

    // ------------------------------------------------------------------ carga de datos
    private void cargarRuta(Ruta ruta) {
        Fmt.banner(mensaje, null, true);
        tituloDisponibilidad.setText("Disponibilidad en " + ruta.clave());
        Tareas.ejecutar(() -> new Object[] {servicio.paradas(ruta.id()), servicio.disponibilidad(ruta.id()), servicio.incidencias(ruta.id())},
                datos -> {
                    @SuppressWarnings("unchecked") List<Parada> paradas = (List<Parada>) datos[0];
                    @SuppressWarnings("unchecked") List<UnidadDisponible> unidades = (List<UnidadDisponible>) datos[1];
                    @SuppressWarnings("unchecked") List<IncidenciaPublica> avisosRuta = (List<IncidenciaPublica>) datos[2];
                    cargando = true;
                    comboParada.getItems().setAll(paradas);
                    cargando = false;
                    pintarDisponibilidad(unidades);
                    pintarAvisos(avisosRuta);
                    mapa.setParadas(paradas, true);
                    int total = paradas.stream().mapToInt(Parada::minutosDesdeOrigen).max().orElse(0);
                    chipResumen.setText(paradas.size() + " paradas · " + total + " min");
                    if (paradas.isEmpty()) {
                        pintarParadas(paradas, null);
                        tablaLlegadas.getItems().clear();
                        tituloLlegadas.setText("Próximas llegadas");
                    } else {
                        comboParada.getSelectionModel().selectFirst();
                    }
                },
                msg -> Fmt.banner(mensaje, msg, true));
    }

    private void alElegirParada(Parada p) {
        pintarParadas(comboParada.getItems(), p);
        mapa.setResaltada(p.id());
        tituloLlegadas.setText("Próximas llegadas a " + p.nombre());
        Tareas.ejecutar(() -> servicio.proximasLlegadas(p.id(), MAX_LLEGADAS),
                llegadas -> tablaLlegadas.getItems().setAll(llegadas),
                msg -> Fmt.banner(mensaje, msg, true));
    }

    @FXML
    private void consultar() {
        Ruta r = comboRuta.getValue();
        if (r == null) { Fmt.banner(mensaje, "Elige una ruta para consultar.", true); return; }
        Parada p = comboParada.getValue();
        if (p == null) { Fmt.banner(mensaje, "Elige una parada para ver las próximas llegadas.", true); return; }
        Fmt.banner(mensaje, null, true);
        alElegirParada(p);
        Tareas.ejecutar(() -> new Object[] {servicio.disponibilidad(r.id()), servicio.incidencias(r.id())},
                datos -> {
                    @SuppressWarnings("unchecked") List<UnidadDisponible> unidades = (List<UnidadDisponible>) datos[0];
                    @SuppressWarnings("unchecked") List<IncidenciaPublica> av = (List<IncidenciaPublica>) datos[1];
                    pintarDisponibilidad(unidades);
                    pintarAvisos(av);
                },
                msg -> Fmt.banner(mensaje, msg, true));
    }

    // ------------------------------------------------------------------ pintado
    private void pintarParadas(List<Parada> paradas, Parada elegida) {
        listaParadas.getChildren().clear();
        for (Parada p : paradas) {
            boolean activa = elegida != null && p.id().equals(elegida.id());
            Label num = new Label(String.valueOf(p.orden()));
            StackPane badge = new StackPane(num);
            badge.getStyleClass().add("parada-badge");
            Label nombre = new Label(p.nombre());
            nombre.getStyleClass().add("parada-nombre");
            Label tiempo = new Label(p.minutosDesdeOrigen() + " min" + (activa ? " · tu parada" : ""));
            tiempo.getStyleClass().add("muted-small");
            HBox item = new HBox(10, badge, new VBox(nombre, tiempo));
            item.getStyleClass().add("parada-item");
            if (activa) item.getStyleClass().add("parada-item-activa");
            item.setPrefWidth(190);
            item.setOnMouseClicked(e -> comboParada.getSelectionModel().select(p));
            listaParadas.getChildren().add(item);
        }
    }

    private void pintarDisponibilidad(List<UnidadDisponible> unidades) {
        tarjetasUnidades.getChildren().clear();
        if (unidades.isEmpty()) {
            Label vacio = new Label("No hay unidades operativas en esta ruta.");
            vacio.getStyleClass().add("muted");
            tarjetasUnidades.getChildren().add(vacio);
            return;
        }
        for (UnidadDisponible u : unidades) {
            Label nombre = new Label(Fmt.eco(u.unidad()));
            nombre.getStyleClass().add("unidad-nombre");
            Label cap = new Label("Capacidad " + u.capacidad());
            cap.getStyleClass().add("muted-small");
            Label estado = u.estado() == EstadoUnidad.EN_RUTA ? Fmt.chip("En ruta", "chip-info") : Fmt.chip("Disponible", "chip-ok");
            Label prox = new Label(u.proximaSalida() == null ? "Sin más salidas hoy" : "Próxima salida " + Fmt.hora(u.proximaSalida()));
            prox.getStyleClass().add("muted-small");
            VBox card = new VBox(4, nombre, cap, estado, prox);
            card.getStyleClass().add("unidad-card");
            card.setPrefWidth(130);
            tarjetasUnidades.getChildren().add(card);
        }
    }

    private void pintarAvisos(List<IncidenciaPublica> lista) {
        avisos.getChildren().clear();
        if (lista.isEmpty()) {
            Label vacio = new Label("Sin avisos activos en esta ruta.");
            vacio.getStyleClass().add("muted");
            avisos.getChildren().add(vacio);
            return;
        }
        for (IncidenciaPublica i : lista) {
            Label desc = new Label(i.descripcion() + " · " + Fmt.eco(i.unidad()));
            desc.setWrapText(true);
            HBox.setHgrow(desc, Priority.ALWAYS);
            HBox fila = new HBox(12, Fmt.chip(Fmt.etiqueta(i.severidad()), Fmt.claseSeveridad(i.severidad())), desc);
            fila.getStyleClass().add("aviso-item");
            avisos.getChildren().add(fila);
        }
    }

    // ------------------------------------------------------------------ navegación
    private void irA(Node destino) {
        Node raiz = scroll.getContent();
        double y = destino.localToScene(destino.getBoundsInLocal()).getMinY() - raiz.localToScene(raiz.getBoundsInLocal()).getMinY();
        double max = raiz.getBoundsInLocal().getHeight() - scroll.getViewportBounds().getHeight();
        scroll.setVvalue(max <= 0 ? 0 : Math.max(0, Math.min(1, y / max)));
    }

    @FXML private void irARutas() { irA(seccionRutas); }
    @FXML private void irAHorarios() { irA(seccionHorarios); }
    @FXML private void irADisponibilidad() { irA(seccionDisponibilidad); }
    @FXML private void accesoAdmin() { Navegador.irAAdmin(); }
}
