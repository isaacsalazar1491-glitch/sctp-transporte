package com.sctp.transporte.util;

import com.sctp.transporte.config.ConexionBD;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

public class MigracionBD
{
    // aplica columnas latitud y longitud a parada si faltan en el servidor
    public static void main(String[] args)
    {
        try (Connection con = ConexionBD.obtenerConexion();
             Statement st = con.createStatement())
        {
            DatabaseMetaData meta = con.getMetaData();
            System.out.println("--- Columnas actuales de 'parada' en servidor ---");
            try (ResultSet rs = meta.getColumns(null, null, "parada", null))
            {
                while (rs.next())
                {
                    System.out.println("Columna: " + rs.getString("COLUMN_NAME") + " (" + rs.getString("TYPE_NAME") + ")");
                }
            }

            System.out.println("\nEjecutando migracion de latitud y longitud...");
            st.execute("ALTER TABLE parada ADD COLUMN IF NOT EXISTS latitud NUMERIC(9,6) NULL CHECK (latitud BETWEEN -90 AND 90)");
            st.execute("ALTER TABLE parada ADD COLUMN IF NOT EXISTS longitud NUMERIC(9,6) NULL CHECK (longitud BETWEEN -180 AND 180)");

            st.execute("UPDATE parada SET latitud = 19.1789, longitud = -99.4663 WHERE nombre = 'Terminal Tianguistenco' AND latitud IS NULL");
            st.execute("UPDATE parada SET latitud = 19.2257, longitud = -99.4846 WHERE nombre = 'Santiago Tilapa' AND latitud IS NULL");
            st.execute("UPDATE parada SET latitud = 19.2847, longitud = -99.5122 WHERE nombre = 'Lerma' AND latitud IS NULL");
            st.execute("UPDATE parada SET latitud = 19.2900, longitud = -99.6560 WHERE nombre = 'Terminal Toluca' AND latitud IS NULL");
            st.execute("UPDATE parada SET latitud = 19.1830, longitud = -99.4700 WHERE nombre = 'Mercado' AND latitud IS NULL");

            System.out.println("\nMigracion completada exitosamente en el servidor!");

            System.out.println("\n--- Columnas despues de migracion ---");
            try (ResultSet rs = meta.getColumns(null, null, "parada", null))
            {
                while (rs.next())
                {
                    System.out.println("Columna: " + rs.getString("COLUMN_NAME") + " (" + rs.getString("TYPE_NAME") + ")");
                }
            }
        }
        catch (Exception e)
        {
            System.err.println("Error durante la migracion: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
