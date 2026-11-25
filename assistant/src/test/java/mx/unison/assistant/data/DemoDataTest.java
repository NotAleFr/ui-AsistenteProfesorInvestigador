package mx.unison.assistant.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.SQLException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DemoDataTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void tearDown() {
        System.clearProperty(DatabaseConnection.DEMO_PROPERTY);
    }

    @Test
    void testSinLaPropiedadLaBdNuevaEstaVacia() throws SQLException {
        DatabaseConnection.setDbFilePath(tempDir.resolve("vacia.db").toString());

        assertTrue(new PlanAcademicoDAO().findAll().isEmpty());
        assertTrue(new EvidenciaDAO().findAll().isEmpty());
    }

    @Test
    void testConLaPropiedadSeCarganLosDatosDeEjemplo() throws SQLException {
        System.setProperty(DatabaseConnection.DEMO_PROPERTY, "true");
        DatabaseConnection.setDbFilePath(tempDir.resolve("demo.db").toString());

        assertEquals(2, new PlanAcademicoDAO().findAll().size());
        assertEquals(7, new EvidenciaDAO().findAll().size());
        // Solo el plan dado de alta aparece en Seguimiento
        assertEquals(5, new EvidenciaDAO().findActive().size());
    }
}
