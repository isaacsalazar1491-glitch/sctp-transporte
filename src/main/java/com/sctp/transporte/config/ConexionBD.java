package com.sctp.transporte.config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class ConexionBD
{

    private static final Properties props = new Properties();
    private static final String URL;
    private static final String USER;
    private static final String PASSWORD;
    private static final String DATABASE;

    static
    {
        try (InputStream input = ConexionBD.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (input == null)
            {
                throw new RuntimeException("No se encontró el archivo db.properties en src/main/resources");
            }
            props.load(input);
            String host = props.getProperty("db.host");
            String port = props.getProperty("db.port");
            DATABASE = props.getProperty("db.name");
            USER = props.getProperty("db.user");
            PASSWORD = props.getProperty("db.password");
            URL = "jdbc:postgresql://" + host + ":" + port + "/" + DATABASE;

            Class.forName("org.postgresql.Driver");
        } catch (IOException | ClassNotFoundException e)
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
                System.out.println("conexion lista " + DATABASE + "!");
            }
        } catch (SQLException e)
        {
            System.err.println("Falló la conexión a PostgreSQL: " + e.getMessage());
            e.printStackTrace();
        }
    }
}