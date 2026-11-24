package mx.unison.assistant.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * Administra la conexión JDBC local hacia la base de datos SQLite3 (IS-1.1, IS-1.2).
 * Inicializa automáticamente el esquema ({@code db_schema.sql}) y los catálogos normativos
 * del Reglamento PEDPD ({@code db_data.sql}) si la base de datos aún no existe o está vacía.
 */
public class DatabaseConnection {

    private static final String DEFAULT_DB_PATH = "base_datos.db";
    private static String dbFilePath = DEFAULT_DB_PATH;
    private static boolean initialized = false;

    private DatabaseConnection() {
    }

    /**
     * Permite configurar una ruta personalizada para el archivo SQLite (útil en pruebas unitarias).
     *
     * @param path ruta del archivo {@code .db}.
     */
    public static synchronized void setDbFilePath(String path) {
        dbFilePath = path;
        initialized = false;
    }

    /**
     * Obtiene la ruta actual del archivo SQLite.
     *
     * @return ruta del archivo de base de datos.
     */
    public static String getDbFilePath() {
        return dbFilePath;
    }

    /**
     * Abre una nueva conexión JDBC hacia SQLite3 con llaves foráneas habilitadas.
     * Si es la primera ejecución, ejecuta los scripts de esquema y datos iniciales.
     *
     * @return conexión activa lista para ejecutar consultas.
     * @throws SQLException si ocurre un error al conectar o inicializar la base de datos.
     */
    public static synchronized Connection getConnection() throws SQLException {
        if (!initialized) {
            initializeDatabaseIfNeeded();
            initialized = true;
        }
        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbFilePath);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }

    /**
     * Verifica si la tabla {@code Categoria} existe y tiene registros; de lo contrario,
     * ejecuta {@code db/db_schema.sql} y {@code db/db_data.sql} desde los recursos del proyecto.
     */
    private static void initializeDatabaseIfNeeded() throws SQLException {
        File dbFile = new File(dbFilePath);
        boolean needsSchema = !dbFile.exists() || dbFile.length() == 0;

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbFilePath)) {
            if (!needsSchema) {
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(
                             "SELECT name FROM sqlite_master WHERE type='table' AND name='Categoria'")) {
                    needsSchema = !rs.next();
                }
            }

            if (needsSchema) {
                executeSqlScript(conn, "/db/db_schema.sql");
                executeSqlScript(conn, "/db/db_data.sql");
                seedDefaultPlanIfNeeded(conn);
            }
        }
    }

    /**
     * Restaura únicamente las tablas normativas ({@code Categoria}, {@code TipoEvidencia},
     * {@code Opcion}, {@code Estatus}, {@code Acreditacion}) a sus valores de fábrica (RF-22).
     *
     * @throws SQLException si falla la restauración por restricciones de integridad o SQL.
     */
    public static synchronized void restoreDefaultCatalog() throws SQLException {
        try (Connection conn = getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = OFF;");
                stmt.execute("DELETE FROM Opcion;");
                stmt.execute("DELETE FROM TipoEvidencia;");
                stmt.execute("DELETE FROM Categoria;");
                stmt.execute("DELETE FROM Estatus;");
                stmt.execute("DELETE FROM Acreditacion;");
            }
            executeSqlScript(conn, "/db/db_data.sql");
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON;");
            }
        }
    }

    /**
     * Elimina toda la información registrada y reconstruye la base de datos limpia (RF-23).
     *
     * @throws SQLException si falla la reconstrucción del esquema.
     */
    public static synchronized void resetFullDatabase() throws SQLException {
        File dbFile = new File(dbFilePath);
        if (dbFile.exists() && !dbFile.delete()) {
            // Si el archivo está bloqueado en Windows, limpiamos las tablas manualmente
            try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = OFF;");
                stmt.execute("DELETE FROM Alertas;");
                stmt.execute("DELETE FROM Documento;");
                stmt.execute("DELETE FROM Evidencia;");
                stmt.execute("DELETE FROM PlanAcademico;");
                stmt.execute("DELETE FROM Opcion;");
                stmt.execute("DELETE FROM TipoEvidencia;");
                stmt.execute("DELETE FROM Categoria;");
                stmt.execute("DELETE FROM Estatus;");
                stmt.execute("DELETE FROM Acreditacion;");
                stmt.execute("PRAGMA foreign_keys = ON;");
            }
            try (Connection conn = getConnection()) {
                executeSqlScript(conn, "/db/db_data.sql");
                seedDefaultPlanIfNeeded(conn);
            }
            return;
        }
        initialized = false;
        getConnection().close();
    }

    /**
     * Crea un plan académico base por defecto si no existe ninguno, garantizando que
     * cualquier evidencia directa tenga una llave foránea válida en {@code PlanAcademico}.
     */
    private static void seedDefaultPlanIfNeeded(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM PlanAcademico")) {
            if (rs.next() && rs.getInt(1) == 0) {
                int currentYear = java.time.Year.now().getValue();
                stmt.executeUpdate("INSERT INTO PlanAcademico (IdPlanAcademico, NombrePlan, AnioPlan, PeriodoPlan) "
                        + "VALUES (1, 'Plan General " + currentYear + "-1', " + currentYear + ", '" + currentYear + "-1')");
            }
        }
    }

    /**
     * Lee y ejecuta un archivo {@code .sql} ubicado en el classpath (por ejemplo {@code /db/db_schema.sql}).
     * Soporta bloques {@code BEGIN ... END;} de triggers sin partirlos prematuramente por punto y coma.
     */
    private static void executeSqlScript(Connection conn, String resourcePath) throws SQLException {
        InputStream in = DatabaseConnection.class.getResourceAsStream(resourcePath);
        if (in == null) {
            throw new SQLException("No se encontró el script SQL en recursos: " + resourcePath);
        }

        String rawSql;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            rawSql = reader.lines()
                    .filter(line -> !line.trim().startsWith("--"))
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            throw new SQLException("Error leyendo script SQL: " + resourcePath, e);
        }

        try (Statement stmt = conn.createStatement()) {
            StringBuilder currentStatement = new StringBuilder();
            boolean insideTrigger = false;

            for (String line : rawSql.split("\n")) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                String upper = trimmed.toUpperCase();
                if (upper.startsWith("CREATE TRIGGER")) {
                    insideTrigger = true;
                }

                currentStatement.append(line).append("\n");

                if (insideTrigger) {
                    if (upper.equals("END;")) {
                        stmt.execute(currentStatement.toString().trim());
                        currentStatement.setLength(0);
                        insideTrigger = false;
                    }
                } else if (trimmed.endsWith(";")) {
                    String sql = currentStatement.toString().trim();
                    if (!sql.equalsIgnoreCase("BEGIN TRANSACTION;") && !sql.equalsIgnoreCase("COMMIT;")) {
                        stmt.execute(sql);
                    }
                    currentStatement.setLength(0);
                }
            }
        }
    }
}