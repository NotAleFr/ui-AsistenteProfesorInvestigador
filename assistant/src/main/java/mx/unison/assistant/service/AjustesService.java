package mx.unison.assistant.service;

import java.io.IOException;
import java.sql.SQLException;
import mx.unison.assistant.data.DatabaseConnection;

// Opciones de la pantalla de Ajustes (RF-22, RF-23)
public class AjustesService {

    private final DocumentoService documentoService;

    public AjustesService(DocumentoService documentoService) {
        this.documentoService = documentoService;
    }

    // Deja el catálogo como viene en el Reglamento PEDPD; las evidencias se conservan (RF-22)
    public void restoreCatalog() throws SQLException {
        DatabaseConnection.restoreDefaultCatalog();
    }

    // Borra planes, evidencias, respaldos y adjuntos, y restaura el catálogo (RF-23).
    // Si falla el borrado de respaldos, se conservan los adjuntos hasta reintentar.
    public void deleteAll() throws SQLException, IOException {
        DatabaseConnection.resetFullDatabase();
        DatabaseConnection.clearBackups();
        documentoService.deleteAllFiles();
    }
}
