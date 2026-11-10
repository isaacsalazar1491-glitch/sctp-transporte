package com.sctp.movil.db;

import android.os.Handler;
import android.os.Looper;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConexionMovil
{
    // parametros del servidor remoto de postgresql
    private static final String HOST = "100.127.110.88";
    private static final String PORT = "5433";
    private static final String DB = "backend_ruta";
    private static final String USER = "uni";
    private static final String PASS = "P4ssw0rd123106";
    // conexion jdbc para postgresql en android
    private static final String URL = "jdbc:postgresql://" + HOST + ":" + PORT + "/" + DB
            + "?ssl=false&sslmode=disable&gssEncMode=disable";

    private static final ExecutorService executor = Executors.newFixedThreadPool(4);
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface Callback<T>
    {
        void onSuccess(T result);
        void onError(Exception e);
    }

    public static Connection obtenerConexionDirecta() throws SQLException, ClassNotFoundException
    {
        Class.forName("org.postgresql.Driver");
        Properties info = new Properties();
        info.setProperty("user", USER);
        info.setProperty("password", PASS);
        info.setProperty("ssl", "false");
        info.setProperty("sslmode", "disable");
        info.setProperty("gssEncMode", "disable");
        info.setProperty("loginTimeout", "6");
        return DriverManager.getConnection(URL, info);
    }

    // ejecucion asincrona tolerante a fallos para no cerrar la app
    public static <T> void ejecutar(Operacion<T> op, Callback<T> callback)
    {
        executor.execute(() ->
        {
            try
            {
                T res = op.ejecutar();
                mainHandler.post(() -> callback.onSuccess(res));
            }
            catch (Throwable t)
            {
                android.util.Log.e("ConexionMovil", "error en operacion sql", t);
                String detalle = t.getMessage();
                if (t.getCause() != null)
                {
                    detalle += " [Causa: " + t.getCause().getMessage() + "]";
                }
                Exception ex = new Exception(detalle != null ? detalle : t.toString());
                mainHandler.post(() -> callback.onError(ex));
            }
        });
    }

    @FunctionalInterface
    public interface Operacion<T>
    {
        T ejecutar() throws Exception;
    }
}

