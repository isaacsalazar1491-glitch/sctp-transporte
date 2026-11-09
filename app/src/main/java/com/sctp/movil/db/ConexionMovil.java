package com.sctp.movil.db;

import android.os.Handler;
import android.os.Looper;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
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
    private static final String URL = "jdbc:postgresql://" + HOST + ":" + PORT + "/" + DB;

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
        return DriverManager.getConnection(URL, USER, PASS);
    }

    // ejecucion asincrona para no bloquear el hilo principal de android
    public static <T> void ejecutar(Operacion<T> op, Callback<T> callback)
    {
        executor.execute(() ->
        {
            try
            {
                T res = op.ejecutar();
                mainHandler.post(() -> callback.onSuccess(res));
            }
            catch (Exception e)
            {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    @FunctionalInterface
    public interface Operacion<T>
    {
        T ejecutar() throws Exception;
    }
}
