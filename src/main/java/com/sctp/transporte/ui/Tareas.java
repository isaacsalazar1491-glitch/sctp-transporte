package com.sctp.transporte.ui;

import com.sctp.transporte.util.ReglaNegocioException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.application.Platform;

/** Ejecuta el acceso a la base de datos fuera del hilo de JavaFX para que la interfaz no se congele. */
public final class Tareas {
    private static final ExecutorService POOL = Executors.newFixedThreadPool(3, r -> {
        Thread t = new Thread(r, "sctp-bd");
        t.setDaemon(true);
        return t;
    });

    private Tareas() {}

    /** Mensaje apto para mostrar al usuario: las reglas de negocio traen su texto; el resto se resume. */
    public static String mensaje(Throwable e) {
        if (e instanceof ReglaNegocioException) return e.getMessage();
        Throwable causa = e;
        while (causa.getCause() != null) causa = causa.getCause();
        String detalle = causa.getMessage() == null ? causa.getClass().getSimpleName() : causa.getMessage();
        return "Ocurrió un error inesperado: " + detalle;
    }

    public static <T> void ejecutar(Supplier<T> trabajo, Consumer<T> alExito, Consumer<String> alError) {
        POOL.execute(() -> {
            try {
                T r = trabajo.get();
                Platform.runLater(() -> alExito.accept(r));
            } catch (Throwable e) {
                if (!(e instanceof ReglaNegocioException)) e.printStackTrace();
                String m = mensaje(e);
                Platform.runLater(() -> alError.accept(m));
            }
        });
    }

    public static void accion(Runnable trabajo, Runnable alExito, Consumer<String> alError) {
        ejecutar(() -> { trabajo.run(); return Boolean.TRUE; }, ok -> alExito.run(), alError);
    }
}
