package com.sctp.transporte.config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class ConexionBD
{
    // parametros por defecto al servidor remoto de postgresql
    private static final String DEFAULT_HOST = "100.127.110.88";
    private static final String DEFAULT_PORT = "5433";
    private static final String DEFAULT_NAME = "backend_ruta";
    private static final String DEFAULT_USER = "uni";
    private static final String DEFAULT_PASS = "P4ssw0rd123106";

    private static final Properties props = new Properties();
    private static final String URL;
    private static final String USER;
    private static final String PASSWORD;
    private static final String DATABASE;

    static
    {
        try
        {
            // intentar leer configuracion desde db.properties
            InputStream input = ConexionBD.class.getClassLoader().getResourceAsStream("db.properties");
            if (input != null)
            {
                try (input)
                {
                    props.load(input);
                }
            }

            // usar valores del archivo o respaldo directo al servidor remoto
            String host = props.getProperty("db.host", DEFAULT_HOST);
            String port = props.getProperty("db.port", DEFAULT_PORT);
            DATABASE = props.getProperty("db.name", DEFAULT_NAME);
            USER = props.getProperty("db.user", DEFAULT_USER);
            PASSWORD = props.getProperty("db.password", DEFAULT_PASS);
            URL = "jdbc:postgresql://" + host + ":" + port + "/" + DATABASE;

            Class.forName("org.postgresql.Driver");
        }
        catch (IOException | ClassNotFoundException e)
        {
            throw new ExceptionInInitializerError("Error al inicializar ConexionBD: " + e.getMessage());
        }
    }

    public static Connection obtenerConexion() throws SQLException
    {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void main(String[] args)
    {
        try (Connection con = obtenerConexion())
        {
            if (con != null && !con.isClosed())
            {
                System.out.println("conexion lista " + DATABASE + " en servidor remoto!");
            }
        }
        catch (SQLException e)
        {
            System.err.println("Fallo la conexion a PostgreSQL en el servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
}