package com.sctp.transporte.ui;

import com.sctp.transporte.model.enums.EstadoIncidencia;
import com.sctp.transporte.model.enums.EstadoUnidad;
import com.sctp.transporte.model.enums.SeveridadIncidencia;
import com.sctp.transporte.util.ReglaNegocioException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.function.Function;
import javafx.beans.binding.Bindings;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.StringConverter;

/** Formato de textos y "chips" de color usados en todas las pantallas. */
public final class Fmt {
    public static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private Fmt() {}

    public static String fecha(LocalDate d) { return d == null ? "" : FECHA.format(d); }
    public static String hora(LocalTime t) { return t == null ? "—" : HORA.format(t); }
    public static String eco(String numeroEconomico) { return "Eco. " + numeroEconomico; }

    /** ABIERTA / EN_ATENCION → "ABIERTA" / "EN ATENCIÓN"; EN_RUTA → "EN RUTA". */
    public static String etiqueta(Enum<?> e) {
        return e.name().replace("EN_ATENCION", "EN ATENCIÓN").replace('_', ' ');
    }

    public static LocalTime parseHora(String texto) {
        try {
            return LocalTime.parse(texto.trim(), DateTimeFormatter.ofPattern("H:mm"));
        } catch (DateTimeParseException | NullPointerException e) {
            throw new ReglaNegocioException("La hora debe tener el formato HH:mm (por ejemplo 07:30)");
        }
    }

    /** Convertidor para ComboBox: muestra el texto que devuelve f (null -> vacío). */
    public static <T> StringConverter<T> texto(Function<T, String> f) {
        return new StringConverter<>() {
            @Override public String toString(T o) { return o == null ? "" : f.apply(o); }
            @Override public T fromString(String s) { return null; }
        };
    }

    /** Tabla que crece con sus filas (sin barra interna): la pantalla entera se desplaza. */
    public static void altoAjustado(TableView<?> t, int filasMinimas) {
        t.setFixedCellSize(48);
        t.prefHeightProperty().bind(Bindings.max(filasMinimas, Bindings.size(t.getItems())).multiply(48).add(44));
        t.minHeightProperty().bind(t.prefHeightProperty());
        t.maxHeightProperty().bind(t.prefHeightProperty());
    }

    public static Label chip(String texto, String claseColor) {
        Label l = new Label(texto);
        l.getStyleClass().addAll("chip", claseColor);
        return l;
    }

    public static String claseEstadoUnidad(EstadoUnidad e) {
        return switch (e) {
            case DISPONIBLE -> "chip-ok";
            case EN_RUTA -> "chip-info";
            case MANTENIMIENTO -> "chip-warn";
            case BAJA -> "chip-danger";
        };
    }

    public static String claseSeveridad(SeveridadIncidencia s) {
        return switch (s) {
            case LEVE -> "chip-info";
            case MODERADA -> "chip-warn";
            case GRAVE -> "chip-danger";
        };
    }

    public static String claseEstadoIncidencia(EstadoIncidencia e) {
        return switch (e) {
            case ABIERTA -> "chip-danger";
            case EN_ATENCION -> "chip-warn";
            case CERRADA -> "chip-ok";
        };
    }

    /** Vigencia de licencia: verde vigente, ámbar si vence en 30 días, rojo vencida. */
    public static String claseVigencia(LocalDate vence) {
        long dias = ChronoUnit.DAYS.between(LocalDate.now(), vence);
        return dias < 0 ? "chip-danger" : dias <= 30 ? "chip-warn" : "chip-ok";
    }

    /** Columna cuyo contenido es un chip generado a partir del valor de la fila. */
    public static <S, T> void columnaChip(TableColumn<S, T> col, Function<T, String> texto, Function<T, String> clase) {
        col.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(T v, boolean vacio) {
                super.updateItem(v, vacio);
                setText(null);
                setGraphic(vacio || v == null ? null : chip(texto.apply(v), clase.apply(v)));
            }
        });
    }

    /** Muestra u oculta un banner de mensaje (error u éxito) sin dejar hueco cuando está vacío. */
    public static void banner(Label l, String mensaje, boolean error) {
        l.getStyleClass().removeAll("alert-error", "alert-ok");
        boolean ver = mensaje != null && !mensaje.isBlank();
        l.setText(ver ? mensaje : "");
        l.setVisible(ver);
        l.setManaged(ver);
        if (ver) l.getStyleClass().add(error ? "alert-error" : "alert-ok");
    }
}
