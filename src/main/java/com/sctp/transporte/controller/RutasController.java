package com.sctp.transporte.controller;

import com.sctp.transporte.model.Parada;
import com.sctp.transporte.model.Ruta;
import com.sctp.transporte.model.enums.EstadoRuta;
import com.sctp.transporte.service.AdministracionService;
import com.sctp.transporte.ui.Fmt;
import com.sctp.transporte.ui.MapaParadas;
import com.sctp.transporte.ui.Navegador;
import com.sctp.transporte.ui.Tareas;
import com.sctp.transporte.util.ReglaNegocioException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.fxml.FXML;

/**
 * Rutas y paradas. El administrador coloca las paradas (únicamente los puntos donde el camión sube o baja gente)
 * haciendo clic en el mapa (OpenStreetMap); los marcadores se pueden arrastrar para corregir su ubicación.
 */
public class RutasController {
    private static final String ICONO_EDITAR = "M12 20h9 M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4Z";
    private static final String ICONO_BORRAR = "M3 6h18 M8 6V4h8v2 M6 6l1 14h10l1-14";

    @FXML private Label mensaje, ayuda, modo, tituloForm, notaCoordenadas;
    @FXML private ComboBox<Ruta> comboRuta;
    @FXML private Button btnEditarRuta, btnBajaRuta, guardar;
    @FXML private TextField buscar, nombre, orden, minutos, latitud, longitud;
    @FXML private MapaParadas mapa;
    @FXML private VBox listaParadas;

    private final AdministracionService servicio = new AdministracionService();
    private List<Parada> paradas = List.of();
    private Parada editando; // null = parada nueva

    @FXML
    private void initialize() {
        Fmt.banner(mensaje, null, true);
        comboRuta.setConverter(Fmt.texto(r -> r.clave() + " · " + r.nombre() + (r.estado() == EstadoRuta.INACTIVA ? " (inactiva)" : "")));
        comboRuta.getSelectionModel().selectedItemProperty().addListener((o, a, r) -> alElegirRuta(r));
        btnEditarRuta.setDisable(true);
        btnBajaRuta.setDisable(true);

        mapa.setEditable(true);
        mapa.setAlClic(this::alClicMapa);
        mapa.setAlMover(this::alMoverMarcador);
        mapa.setAlSeleccionar(id -> paradas.stream().filter(p -> p.id() == id).findFirst().ifPresent(this::editar));
        mapa.setAlAviso(texto -> Fmt.banner(mensaje, texto, true));
        // Sin mapa (sin conexión a Internet) las coordenadas se capturan a mano.
        mapa.disponibleProperty().addListener((o, a, disponible) -> alCambiarDisponibilidadMapa(disponible));
        alCambiarDisponibilidadMapa(mapa.disponibleProperty().get());

        recargarRutas(null);
    }

    private void alCambiarDisponibilidadMapa(boolean disponible) {
        latitud.setEditable(!disponible);
        longitud.setEditable(!disponible);
        buscar.setDisable(!disponible);
        modo.setVisible(disponible);
        notaCoordenadas.setText(disponible ? "Latitud y longitud se llenan al hacer clic en el mapa."
                : "El mapa no está disponible: captura latitud y longitud a mano (por ejemplo 19.178900 y -99.466300).");
        ayuda.setText(disponible ? "Haz clic en el mapa para colocar una parada nueva. Arrastra un marcador para moverlo."
                : "Sin conexión a Internet el mapa no carga; captura las coordenadas a mano.");
    }

    // ------------------------------------------------------------------ rutas
    private void recargarRutas(Integer seleccionar) {
        Tareas.ejecutar(() -> servicio.listarRutas(false), (List<Ruta> rutas) -> {
            Ruta previa = comboRuta.getValue();
            Integer id = seleccionar != null ? seleccionar : previa == null ? null : previa.id();
            comboRuta.getItems().setAll(rutas);
            Optional<Ruta> elegida = rutas.stream().filter(r -> id != null && id.equals(r.id())).findFirst();
            if (elegida.isPresent()) comboRuta.getSelectionModel().select(elegida.get());
            else if (!rutas.isEmpty()) comboRuta.getSelectionModel().selectFirst();
        }, msg -> Fmt.banner(mensaje, msg, true));
    }

    private void alElegirRuta(Ruta r) {
        btnEditarRuta.setDisable(r == null);
        btnBajaRuta.setDisable(r == null || r.estado() == EstadoRuta.INACTIVA);
        limpiarFormulario();
        if (r == null) {
            paradas = List.of();
            pintarParadas();
            mapa.setParadas(paradas, true);
            return;
        }
        recargarParadas(true, null);
    }

    @FXML
    private void nuevaRuta() {
        pedirRuta(null).ifPresent(r -> Tareas.ejecutar(() -> servicio.crearRuta(r), id -> {
            Fmt.banner(mensaje, "Ruta creada. Ahora coloca sus paradas en el mapa.", false);
            recargarRutas(id);
        }, msg -> Fmt.banner(mensaje, msg, true)));
    }

    @FXML
    private void editarRuta() {
        Ruta base = comboRuta.getValue();
        if (base == null) return;
        pedirRuta(base).ifPresent(r -> Tareas.accion(() -> servicio.actualizarRuta(r), () -> {
            Fmt.banner(mensaje, "Ruta actualizada.", false);
            recargarRutas(r.id());
        }, msg -> Fmt.banner(mensaje, msg, true)));
    }

    @FXML
    private void darDeBajaRuta() {
        Ruta r = comboRuta.getValue();
        if (r == null) return;
        if (!confirmar("Dar de baja la ruta " + r.clave(), "La ruta dejará de mostrarse en la consulta pública. Sus paradas y corridas se conservan.")) return;
        Tareas.accion(() -> servicio.darDeBajaRuta(r.id()), () -> {
            Fmt.banner(mensaje, "Ruta dada de baja.", false);
            recargarRutas(r.id());
        }, msg -> Fmt.banner(mensaje, msg, true));
    }

    /** Diálogo de alta/edición de ruta; devuelve la ruta validada o vacío si se cancela. */
    private Optional<Ruta> pedirRuta(Ruta base) {
        Dialog<Ruta> dialogo = new Dialog<>();
        dialogo.setTitle(base == null ? "Nueva ruta" : "Editar ruta");
        dialogo.setHeaderText(null);
        dialogo.getDialogPane().getStylesheets().add(Navegador.urlCss());
        ButtonType ok = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);

        TextField clave = new TextField(base == null ? "" : base.clave());
        TextField nom = new TextField(base == null ? "" : base.nombre());
        TextField origen = new TextField(base == null ? "" : base.origen());
        TextField destino = new TextField(base == null ? "" : base.destino());
        clave.setPromptText("R-04");
        nom.setPromptText("Tianguistenco – Toluca");
        origen.setPromptText("Terminal Tianguistenco");
        destino.setPromptText("Terminal Toluca");
        ComboBox<EstadoRuta> estado = new ComboBox<>();
        estado.getItems().setAll(EstadoRuta.values());
        estado.setValue(base == null ? EstadoRuta.ACTIVA : base.estado());
        estado.setConverter(Fmt.texto(Fmt::etiqueta));
        estado.setMaxWidth(Double.MAX_VALUE);
        estado.setDisable(base == null);
        Label error = new Label();
        Fmt.banner(error, null, true);

        GridPane g = new GridPane();
        g.setHgap(12);
        g.setVgap(12);
        g.setPadding(new Insets(12, 8, 4, 8));
        String[] etiquetas = {"Clave", "Nombre", "Origen", "Destino", "Estado"};
        javafx.scene.Node[] campos = {clave, nom, origen, destino, estado};
        for (int i = 0; i < etiquetas.length; i++) {
            Label l = new Label(etiquetas[i]);
            l.getStyleClass().add("field-label");
            g.add(l, 0, i);
            g.add(campos[i], 1, i);
            GridPane.setHgrow(campos[i], Priority.ALWAYS);
        }
        g.add(error, 0, etiquetas.length, 2, 1);
        g.setPrefWidth(460);
        dialogo.getDialogPane().setContent(g);

        // Si algún dato no es válido el diálogo no se cierra: se muestra el motivo.
        Button botonOk = (Button) dialogo.getDialogPane().lookupButton(ok);
        botonOk.addEventFilter(ActionEvent.ACTION, ev -> {
            try {
                new Ruta(base == null ? null : base.id(), clave.getText(), nom.getText(), origen.getText(), destino.getText(), estado.getValue());
                Fmt.banner(error, null, true);
            } catch (ReglaNegocioException e) {
                Fmt.banner(error, e.getMessage(), true);
                ev.consume();
            }
        });
        dialogo.setResultConverter(bt -> bt == ok
                ? new Ruta(base == null ? null : base.id(), clave.getText(), nom.getText(), origen.getText(), destino.getText(), estado.getValue())
                : null);
        return dialogo.showAndWait();
    }


    private boolean confirmar(String titulo, String texto) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, texto, ButtonType.OK, ButtonType.CANCEL);
        a.setTitle("Confirmar");
        a.setHeaderText(titulo);
        a.getDialogPane().getStylesheets().add(Navegador.urlCss());
        return a.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }

    // ------------------------------------------------------------------ paradas
    private void recargarParadas(boolean ajustarMapa, Integer resaltar) {
        Ruta r = comboRuta.getValue();
        if (r == null) return;
        Tareas.ejecutar(() -> servicio.listarParadas(r.id()), (List<Parada> ps) -> {
            if (comboRuta.getValue() == null || !r.id().equals(comboRuta.getValue().id())) return; // cambió la ruta mientras cargaba
            paradas = ps;
            mapa.setParadas(ps, ajustarMapa);
            pintarParadas();
            if (editando == null) sugerirSiguiente();
            if (resaltar != null) mapa.setResaltada(resaltar);
        }, msg -> Fmt.banner(mensaje, msg, true));
    }

    private void pintarParadas() {
        listaParadas.getChildren().clear();
        if (paradas.isEmpty()) {
            Label vacio = new Label("Esta ruta aún no tiene paradas. Haz clic en el mapa para colocar la primera.");
            vacio.getStyleClass().add("muted");
            vacio.setWrapText(true);
            listaParadas.getChildren().add(vacio);
            return;
        }
        for (Parada p : paradas) {
            boolean activa = editando != null && editando.id().equals(p.id());
            Label num = new Label(String.valueOf(p.orden()));
            StackPane badge = new StackPane(num);
            badge.getStyleClass().add("parada-badge");
            Label nombreLbl = new Label(p.nombre());
            nombreLbl.getStyleClass().add("parada-nombre");
            Label detalle = new Label(p.minutosDesdeOrigen() + " min · "
                    + (p.tieneUbicacion() ? String.format(Locale.ROOT, "%.6f, %.6f", p.latitud(), p.longitud()) : "sin ubicar en el mapa"));
            detalle.getStyleClass().add("muted-small");
            VBox textos = new VBox(nombreLbl, detalle);
            HBox.setHgrow(textos, Priority.ALWAYS);
            textos.setMinWidth(0);

            Button editar = botonIcono(ICONO_EDITAR, "#1B64B5", "Editar " + p.nombre());
            editar.setOnAction(e -> editar(p));
            Button borrar = botonIcono(ICONO_BORRAR, "#8A1A12", "Eliminar " + p.nombre());
            borrar.setOnAction(e -> eliminar(p));

            HBox fila = new HBox(12, badge, textos, editar, borrar);
            fila.setAlignment(Pos.CENTER_LEFT);
            fila.getStyleClass().add("parada-item");
            if (activa) fila.getStyleClass().add("parada-item-activa");
            fila.setOnMouseClicked(e -> editar(p));
            listaParadas.getChildren().add(fila);
        }
    }

    private static Button botonIcono(String contenido, String color, String descripcion) {
        SVGPath icono = new SVGPath();
        icono.setContent(contenido);
        icono.setFill(Color.TRANSPARENT);
        icono.setStroke(Color.web(color));
        icono.setStrokeWidth(2);
        icono.setStrokeLineCap(StrokeLineCap.ROUND);
        Button b = new Button();
        b.setGraphic(icono);
        b.getStyleClass().add("btn-icon");
        b.setAccessibleText(descripcion);
        b.setTooltip(new javafx.scene.control.Tooltip(descripcion));
        return b;
    }

    private void editar(Parada p) {
        editando = p;
        tituloForm.setText("Editar parada");
        nombre.setText(p.nombre());
        orden.setText(String.valueOf(p.orden()));
        minutos.setText(String.valueOf(p.minutosDesdeOrigen()));
        latitud.setText(p.latitud() == null ? "" : String.format(Locale.ROOT, "%.6f", p.latitud()));
        longitud.setText(p.longitud() == null ? "" : String.format(Locale.ROOT, "%.6f", p.longitud()));
        mapa.setTemporal(null, null);
        mapa.setResaltada(p.id());
        Fmt.banner(mensaje, null, true);
        pintarParadas();
    }

    private void eliminar(Parada p) {
        if (!confirmar("Eliminar la parada \"" + p.nombre() + "\"", "Se quitará de la ruta. Esta acción no se puede deshacer.")) return;
        Tareas.accion(() -> servicio.eliminarParada(p.id()), () -> {
            Fmt.banner(mensaje, "Parada eliminada.", false);
            if (editando != null && editando.id().equals(p.id())) limpiarFormulario();
            recargarParadas(false, null);
        }, msg -> Fmt.banner(mensaje, msg, true));
    }

    /** Clic en el mapa: fija (o corrige) la ubicación de la parada que se está capturando. */
    private void alClicMapa(Double lat, Double lng) {
        latitud.setText(String.format(Locale.ROOT, "%.6f", lat));
        longitud.setText(String.format(Locale.ROOT, "%.6f", lng));
        mapa.setTemporal(lat, lng);
        Fmt.banner(mensaje, null, true);
        if (editando == null) nombre.requestFocus();
    }

    /** Un marcador existente se arrastró a otro punto: se guarda la nueva ubicación. */
    private void alMoverMarcador(Integer id, double[] ll) {
        paradas.stream().filter(p -> p.id().equals(id)).findFirst().ifPresent(p -> {
            Parada movida;
            try {
                movida = new Parada(p.id(), p.idRuta(), p.nombre(), p.orden(), p.minutosDesdeOrigen(), ll[0], ll[1]);
            } catch (ReglaNegocioException e) {
                Fmt.banner(mensaje, e.getMessage(), true);
                recargarParadas(false, null);
                return;
            }
            Tareas.accion(() -> servicio.actualizarParada(movida), () -> {
                Fmt.banner(mensaje, "Ubicación de \"" + p.nombre() + "\" actualizada.", false);
                if (editando != null && editando.id().equals(id)) editando = movida;
                if (editando != null && editando.id().equals(id)) {
                    latitud.setText(String.format(Locale.ROOT, "%.6f", ll[0]));
                    longitud.setText(String.format(Locale.ROOT, "%.6f", ll[1]));
                }
                recargarParadas(false, editando == null ? null : editando.id());
            }, msg -> {
                Fmt.banner(mensaje, msg, true);
                recargarParadas(false, null); // devuelve el marcador a su lugar
            });
        });
    }

    @FXML
    private void buscarLugar() { mapa.buscar(buscar.getText()); }

    @FXML
    private void guardarParada() {
        Ruta r = comboRuta.getValue();
        if (r == null) { Fmt.banner(mensaje, "Elige o crea una ruta antes de colocar paradas.", true); return; }
        Parada p;
        try {
            int ord = entero(orden.getText(), "El orden debe ser un número entero");
            int min = entero(minutos.getText(), "Los minutos desde el origen deben ser un número entero");
            Double lat = decimal(latitud.getText(), "La latitud debe ser un número (por ejemplo 19.178900)");
            Double lng = decimal(longitud.getText(), "La longitud debe ser un número (por ejemplo -99.466300)");
            p = new Parada(editando == null ? null : editando.id(), r.id(), nombre.getText(), ord, min, lat, lng);
        } catch (ReglaNegocioException e) {
            Fmt.banner(mensaje, e.getMessage(), true);
            return;
        }
        Parada aGuardar = p;
        boolean nueva = editando == null;
        guardar.setDisable(true);
        Tareas.ejecutar(() -> {
            if (nueva) return servicio.crearParada(aGuardar);
            servicio.actualizarParada(aGuardar);
            return aGuardar.id();
        }, id -> {
            guardar.setDisable(false);
            Fmt.banner(mensaje, aGuardar.tieneUbicacion() ? "Parada guardada." : "Parada guardada sin ubicación: colócala en el mapa para que se vea.", false);
            limpiarFormulario();
            recargarParadas(false, null);
        }, msg -> { guardar.setDisable(false); Fmt.banner(mensaje, msg, true); });
    }

    @FXML
    private void cancelar() {
        limpiarFormulario();
        Fmt.banner(mensaje, null, true);
    }

    private void limpiarFormulario() {
        editando = null;
        tituloForm.setText("Parada nueva");
        nombre.clear();
        minutos.clear();
        latitud.clear();
        longitud.clear();
        mapa.setTemporal(null, null);
        mapa.setResaltada(null);
        sugerirSiguiente();
        pintarParadas();
    }

    private void sugerirSiguiente() {
        orden.setText(String.valueOf(paradas.stream().mapToInt(Parada::orden).max().orElse(0) + 1));
    }

    private static int entero(String t, String error) {
        try { return Integer.parseInt(t.trim()); }
        catch (NumberFormatException | NullPointerException e) { throw new ReglaNegocioException(error); }
    }

    /** Vacío = sin ubicación (null). */
    private static Double decimal(String t, String error) {
        if (t == null || t.isBlank()) return null;
        try { return Double.valueOf(t.trim().replace(',', '.')); }
        catch (NumberFormatException e) { throw new ReglaNegocioException(error); }
    }
}
