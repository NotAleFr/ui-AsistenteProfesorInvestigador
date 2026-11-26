package mx.unison.assistant.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteErrorCode;
import org.sqlite.SQLiteException;

/**
 * Administra la conexión JDBC local hacia la base de datos SQLite3 (IS-1.1, IS-1.2).
 * Inicializa automáticamente el esquema ({@code db_schema.sql}) y los catálogos normativos
 * del Reglamento PEDPD ({@code db_data.sql}) si la base de datos aún no existe o está vacía.
 */
public class DatabaseConnection {

    private static final int BUSY_TIMEOUT_MS = 5000;
    private static final int MAX_BACKUPS = 3;
    private static final DateTimeFormatter BACKUP_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");
    // mvn exec:java pone esta propiedad (ver pom.xml) para que la BD nueva traiga db_demo.sql
    public static final String DEMO_PROPERTY = "asistente.demo";
    private static String dbFilePath;
    private static Path initializedPath;

    private DatabaseConnection() {
    }

    /**
     * Permite configurar una ruta personalizada para el archivo SQLite (útil en pruebas unitarias).
     *
     * @param path ruta del archivo {@code .db}.
     */
    public static synchronized void setDbFilePath(String path) {
        dbFilePath = path;
        initializedPath = null;
    }

    /**
     * Obtiene la ruta actual del archivo SQLite.
     *
     * @return ruta del archivo de base de datos.
     */
    public static synchronized String getDbFilePath() {
        return dbFilePath == null ? AppPaths.getDatabasePath().toString() : dbFilePath;
    }

    /**
     * Abre una nueva conexión JDBC hacia SQLite3 con llaves foráneas habilitadas.
     * Si es la primera ejecución, ejecuta los scripts de esquema y datos iniciales.
     *
     * @return conexión activa lista para ejecutar consultas.
     * @throws SQLException si ocurre un error al conectar o inicializar la base de datos.
     */
    public static synchronized Connection getConnection() throws SQLException {
        Path path = Path.of(getDbFilePath());
        Path parent = path.getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                throw new SQLException("No se pudo crear la carpeta de la base de datos.", e);
            }
        }
        if (!path.equals(initializedPath)) {
            initializeDatabaseIfNeeded(path);
            initializedPath = path;
        }
        return openConnection(path);
    }

    private static Connection openConnection(Path path) throws SQLException {
        SQLiteConfig config = new SQLiteConfig();
        config.setJournalMode(SQLiteConfig.JournalMode.WAL);
        config.setBusyTimeout(BUSY_TIMEOUT_MS);
        config.setSynchronous(SQLiteConfig.SynchronousMode.NORMAL);
        config.enforceForeignKeys(true);
        return DriverManager.getConnection("jdbc:sqlite:" + path, config.toProperties());
    }

    /**
     * Verifica si la tabla {@code Categoria} existe; de lo contrario,
     * ejecuta {@code db/db_schema.sql} y {@code db/db_data.sql} desde los recursos del proyecto.
     */
    private static void initializeDatabaseIfNeeded(Path path) throws SQLException {
        try (Connection conn = openConnection(path)) {
            boolean needsSchema;
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT name FROM sqlite_master WHERE type='table' AND name='Categoria'")) {
                needsSchema = !rs.next();
            }

            if (needsSchema) {
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

    public enum HealthStatus {
        OK("La base de datos está en buen estado."),
        LOCKED("La base de datos está ocupada. Cierra otras instancias del asistente y vuelve a intentar."),
        PERMISSION_DENIED("No se pudo abrir la base de datos. Comprueba que la carpeta de datos sea accesible "
                + "y que tengas permisos de lectura y escritura."),
        DISK_FULL("No hay espacio suficiente en el disco. Libera espacio y vuelve a intentar."),
        CORRUPT("La base de datos está dañada. Restaura un respaldo si está disponible."),
        OTHER("No se pudo comprobar la base de datos. Revisa que la carpeta de datos esté disponible "
                + "y vuelve a intentar.");

        private final String message;

        HealthStatus(String message) {
            this.message = message;
        }
    }

    public record HealthResult(HealthStatus status, String message, Throwable cause) {
    }

    public static HealthResult checkHealth() {
        try (Connection conn = getConnection()) {
            HealthStatus status = passesQuickCheck(conn) ? HealthStatus.OK : HealthStatus.CORRUPT;
            return new HealthResult(status, status.message, null);
        } catch (SQLException | RuntimeException e) {
            HealthStatus status = classifyHealthFailure(e);
            return new HealthResult(status, status.message, e);
        }
    }

    private static HealthStatus classifyHealthFailure(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLiteException sqliteException) {
                int primaryCode = sqliteException.getResultCode().code & 0xff;
                return switch (SQLiteErrorCode.getErrorCode(primaryCode)) {
                    case SQLITE_BUSY, SQLITE_LOCKED -> HealthStatus.LOCKED;
                    case SQLITE_PERM, SQLITE_READONLY, SQLITE_CANTOPEN -> HealthStatus.PERMISSION_DENIED;
                    case SQLITE_FULL -> HealthStatus.DISK_FULL;
                    case SQLITE_CORRUPT, SQLITE_NOTADB -> HealthStatus.CORRUPT;
                    default -> HealthStatus.OTHER;
                };
            }
            if (cause instanceof AccessDeniedException || cause instanceof FileAlreadyExistsException
                    || cause instanceof NotDirectoryException || cause instanceof SecurityException) {
                return HealthStatus.PERMISSION_DENIED;
            }
        }
        return HealthStatus.OTHER;
    }

    private static boolean passesQuickCheck(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA quick_check")) {
            return rs.next() && "ok".equals(rs.getString(1)) && !rs.next();
        }
    }

    public static synchronized Path createBackup() throws IOException, SQLException {
        Path backupsDir = getBackupsDir();
        Files.createDirectories(backupsDir);
        Path tempBackupFile = Files.createTempFile(backupsDir, "base_datos-", ".tmp");
        try {
            // VACUUM INTO escribe una copia consistente incluso con WAL activo
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement("VACUUM INTO ?")) {
                stmt.setString(1, tempBackupFile.toAbsolutePath().toString());
                stmt.execute();
            }

            LocalDateTime timestamp = LocalDateTime.now();
            Path backupFile = backupsDir.resolve("base_datos-" + timestamp.format(BACKUP_TIMESTAMP) + ".bak");
            while (Files.exists(backupFile)) {
                timestamp = timestamp.plusNanos(1_000_000);
                backupFile = backupsDir.resolve("base_datos-" + timestamp.format(BACKUP_TIMESTAMP) + ".bak");
            }
            Files.move(tempBackupFile, backupFile);

            pruneOldBackups(backupFile);
            return backupFile;
        } finally {
            Files.deleteIfExists(tempBackupFile);
        }
    }

    private static void pruneOldBackups(Path newestBackup) throws IOException {
        List<Path> backups = listBackups();
        int keptCount = 0;
        for (Path existingBackup : backups) {
            if (existingBackup.equals(newestBackup) || keptCount < MAX_BACKUPS) {
                keptCount++;
            } else {
                Files.deleteIfExists(existingBackup);
            }
        }
    }

    public static synchronized List<Path> listBackups() throws IOException {
        Path backupsDir = getBackupsDir();
        if (Files.notExists(backupsDir)) {
            return List.of();
        }
        List<Path> backups = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(backupsDir, "*.bak")) {
            for (Path candidate : stream) {
                if (Files.isRegularFile(candidate)) {
                    backups.add(candidate);
                }
            }
        }
        backups.sort(Comparator.comparing((Path file) -> file.getFileName().toString()).reversed());
        return backups;
    }

    public static synchronized boolean restoreLatestBackup() throws IOException {
        Path dbFile = Path.of(getDbFilePath()).toAbsolutePath();
        for (Path backupFile : listBackups()) {
            if (!isValidBackup(backupFile)) {
                continue;
            }
            Path parentDir = dbFile.getParent();
            Files.createDirectories(parentDir);
            Path tempRestoreFile = Files.createTempFile(parentDir, "restauracion-", ".tmp");
            try {
                Files.copy(backupFile, tempRestoreFile, StandardCopyOption.REPLACE_EXISTING);
                // Mueve la BD dañada y sus archivos -wal/-shm aparte antes de copiar el respaldo
                quarantineCorruptFiles(dbFile);
                Files.move(tempRestoreFile, dbFile, StandardCopyOption.REPLACE_EXISTING);
                initializedPath = null;
                return true;
            } finally {
                Files.deleteIfExists(tempRestoreFile);
            }
        }
        return false;
    }

    private static void quarantineCorruptFiles(Path dbFile) throws IOException {
        String timestamp = LocalDateTime.now().format(BACKUP_TIMESTAMP);
        String corruptFileName = "base_datos-corrupt-" + timestamp + ".db";
        moveIfExists(dbFile.resolveSibling(dbFile.getFileName() + "-wal"),
                dbFile.resolveSibling(corruptFileName + "-wal"));
        moveIfExists(dbFile.resolveSibling(dbFile.getFileName() + "-shm"),
                dbFile.resolveSibling(corruptFileName + "-shm"));
        moveIfExists(dbFile, dbFile.resolveSibling(corruptFileName));
    }

    private static void moveIfExists(Path source, Path target) throws IOException {
        if (Files.exists(source)) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static boolean isValidBackup(Path backup) {
        SQLiteConfig config = new SQLiteConfig();
        config.setReadOnly(true);
        config.setBusyTimeout(BUSY_TIMEOUT_MS);
        try {
            if (Files.size(backup) == 0) {
                return false;
            }
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + backup, config.toProperties())) {
                if (!passesQuickCheck(conn)) {
                    return false;
                }
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(
                             "SELECT 1 FROM sqlite_master WHERE type='table' AND name='Categoria'")) {
                    return rs.next();
                }
            }
        } catch (IOException | SQLException e) {
            return false;
        }
    }

    private static Path getBackupsDir() {
        Path parent = Path.of(getDbFilePath()).getParent();
        return parent == null ? Path.of("backups") : parent.resolve("backups");
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
