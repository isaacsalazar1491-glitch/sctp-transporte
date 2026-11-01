package com.sctp.transporte;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.sctp.transporte.config.Db;
import com.sctp.transporte.util.ReglaNegocioException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Base de las pruebas de integración: recrea el esquema y los datos en cada prueba.
 * SEGURIDAD: solo se ejecuta si la base de datos se llama "*_test" (nunca borra tu BD de desarrollo).
 * Credenciales de la BD de pruebas: src/test/resources/db.properties (ver db.properties.example).
 */
abstract class BaseIT {
    @BeforeEach
    void prepararBd() throws Exception {
        try (Connection c = Db.get()) {
            assumeTrue(c.getCatalog().endsWith("_test"), "Se omite: la BD debe llamarse *_test (actual: " + c.getCatalog() + ")");
            try (Statement st = c.createStatement()) {
                for (String f : new String[]{"01_esquema_3fn", "02_trigger_validacion", "03_vistas_y_procedimientos", "04_datos_prueba"})
                    st.execute(Files.readString(Path.of("sql/" + f + ".sql")));
            }
        } catch (SQLException | ExceptionInInitializerError | NoClassDefFoundError e) {
            assumeTrue(false, "PostgreSQL / db.properties de pruebas no disponible: " + e);
        }
    }

    static void rechaza(String fragmento, Runnable r) {
        ReglaNegocioException e = assertThrows(ReglaNegocioException.class, r::run);
        assertTrue(e.getMessage().contains(fragmento), "Mensaje: " + e.getMessage());
    }
}
