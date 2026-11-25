package mx.unison.assistant.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
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
    // mvn exec:java pone esta propiedad (ver pom.xml) para que la BD nueva traiga db_demo.sql
    public static final String DEMO_PROPERTY = "asistente.demo";
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
        } catch (SQLException e) {
            conn.close();
            throw e;
        }
        return conn;
    }

    /**
     * Verifica si la tabla {@code Categoria} existe; de lo contrario,
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
                // Antes de la transacción, porque SQLite ignora este PRAGMA dentro de una
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("PRAGMA foreign_keys = ON;");
                }
                // En una transacción: si algo falla, no queda una BD a medias que ya no se vuelva a crear
                conn.setAutoCommit(false);
                try {
                    executeSqlScript(conn, "/db/db_schema.sql");
                    executeSqlScript(conn, "/db/db_data.sql");
                    if ("true".equals(System.getProperty(DEMO_PROPERTY))) {
                        executeSqlScript(conn, "/db/db_demo.sql");
                    }
                    conn.commit();
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                }
            }
        }
    }

    /**
     * Restaura únicamente las tablas normativas ({@code Categoria}, {@code TipoEvidencia},
     * {@code Opcion}, {@code Estatus}, {@code Acreditacion}) a sus valores de fábrica (RF-22).
     * Si alguna evidencia quedara apuntando a un catálogo que ya no existe, no se cambia nada.
     *
     * @throws SQLException si falla la restauración; en ese caso la BD queda como estaba.
     */
    public static synchronized void restoreDefaultCatalog() throws SQLException {
        try (Connection conn = getConnection()) {
            runWithoutForeignKeys(conn, () -> {
                deleteAllRows(conn, "Opcion", "TipoEvidencia", "Categoria", "Estatus", "Acreditacion");
                executeSqlScript(conn, "/db/db_data.sql");
            });
        }
    }

    /**
     * Elimina toda la información registrada y deja la base de datos como recién instalada (RF-23).
     * Los archivos adjuntos los borra {@code DocumentoService}, no esta clase.
     *
     * @throws SQLException si falla la limpieza; en ese caso la BD queda como estaba.
     */
    public static synchronized void resetFullDatabase() throws SQLException {
        try (Connection conn = getConnection()) {
            runWithoutForeignKeys(conn, () -> {
                deleteAllRows(conn, "Alertas", "Documento", "Evidencia", "PlanAcademico",
                        "Opcion", "TipoEvidencia", "Categoria", "Estatus", "Acreditacion");
                executeSqlScript(conn, "/db/db_data.sql");
            });
        }
    }

    /** Pasos de SQL que se ejecutan dentro de {@link #runWithoutForeignKeys}. */
    private interface SqlSteps {
        void run() throws SQLException;
    }

    /**
     * Ejecuta los pasos en una transacción con las llaves foráneas apagadas, revisa que
     * no quede ninguna llave rota y solo entonces confirma. Si algo falla, deshace todo.
     * (SQLite ignora {@code PRAGMA foreign_keys} dentro de una transacción, por eso se apaga antes.)
     */
    private static void runWithoutForeignKeys(Connection conn, SqlSteps steps) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = OFF;");
        }
        conn.setAutoCommit(false);
        try {
            steps.run();
            try (Statement stmt = conn.createStatement();
                 ResultSet broken = stmt.executeQuery("PRAGMA foreign_key_check;")) {
                if (broken.next()) {
                    throw new SQLException("La operación dejaría registros apuntando a datos que ya no existen "
                            + "(tabla " + broken.getString("table") + "). No se hizo ningún cambio.");
                }
            }
            conn.commit();
        } catch (SQLException | RuntimeException e) {
            // También RuntimeException: si no se deshace aquí, setAutoCommit(true) confirmaría el borrado
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON;");
            }
        }
    }

    private static void deleteAllRows(Connection conn, String... tables) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            for (String table : tables) {
                stmt.execute("DELETE FROM " + table + ";");
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
        } catch (IOException | UncheckedIOException e) {
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