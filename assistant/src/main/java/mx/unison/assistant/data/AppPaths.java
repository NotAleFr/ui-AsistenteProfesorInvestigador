package mx.unison.assistant.data;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class AppPaths {

    public static final String DATA_DIR_PROPERTY = "asistente.dataDir";
    public static final String ATTACHMENTS_DIRECTORY = "archivos_adjuntos";

    private AppPaths() {
    }

    public static Path getDataDir() {
        String configuredDir = System.getProperty(DATA_DIR_PROPERTY);
        if (configuredDir != null && !configuredDir.isBlank()) {
            return Paths.get(configuredDir);
        }
        return Paths.get(".");
    }

    public static Path getDatabasePath() {
        return getDataDir().resolve("base_datos.db");
    }

    public static Path getAttachmentsDir() {
        return getDataDir().resolve(ATTACHMENTS_DIRECTORY);
    }
}
