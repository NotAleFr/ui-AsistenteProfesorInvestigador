package mx.unison.assistant.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import mx.unison.assistant.data.AppPaths;
import mx.unison.assistant.data.DocumentoDAO;
import mx.unison.assistant.model.Documento;

/**
 * Servicio encargado de gestionar los archivos probatorios adjuntos a evidencias (RF-15, IS-3.3):
 * valida extensiones permitidas, copia el archivo físico a su subcarpeta de categoría,
 * calcula su huella SHA-256 y tamaño en bytes, y registra sus metadatos en SQLite.
 */
public class DocumentoService {

    public static final String ROOT_ATTACHMENTS_DIR = AppPaths.ATTACHMENTS_DIRECTORY;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".pdf", ".jpg", ".jpeg", ".png", ".docx", ".xlsx"
    );

    private final DocumentoDAO documentoDAO;
    private final String baseAttachmentsPath;

    public DocumentoService() {
        this(new DocumentoDAO(), AppPaths.getAttachmentsDir().toString());
    }

    public DocumentoService(DocumentoDAO documentoDAO, String baseAttachmentsPath) {
        this.documentoDAO = documentoDAO;
        this.baseAttachmentsPath = baseAttachmentsPath;
    }

    /**
     * Valida, copia al almacenamiento local y registra un archivo adjunto vinculado a una evidencia (RF-15).
     *
     * @param sourceFile    archivo seleccionado por el usuario desde el sistema operativo.
     * @param idEvidencia   identificador de la evidencia destino.
     * @param categoryName  nombre o clave de la categoría para determinar la subcarpeta (IS-3.3).
     * @return entidad {@link Documento} persistida con su ID y huella SHA-256.
     * @throws IOException  si el archivo no existe, tiene extensión inválida o falla la copia en disco.
     * @throws SQLException si falla la inserción en la tabla {@code Documento}.
     */
    public Documento attachDocument(File sourceFile, int idEvidencia, String categoryName)
            throws IOException, SQLException {
        Documento doc = stageFile(sourceFile, idEvidencia, categoryName);
        try {
            documentoDAO.insert(doc);
        } catch (SQLException e) {
            deleteFile(doc);
            throw e;
        }
        return doc;
    }

    /**
     * Valida y copia un archivo a su subcarpeta de categoría calculando SHA-256 y tamaño,
     * sin registrar todavía la fila en SQLite.
     */
    public Documento stageFile(File sourceFile, int idEvidencia, String categoryName) throws IOException {
        validate(sourceFile);
        String originalName = sourceFile.getName();

        String categoryFolder = mapCategoryToFolder(categoryName);
        Path targetDir = Paths.get(baseAttachmentsPath, categoryFolder);
        Files.createDirectories(targetDir);

        // Prefijo con timestamp + 8 caracteres aleatorios: dos archivos con el mismo nombre
        // adjuntados en el mismo milisegundo no chocan.
        // Sin REPLACE_EXISTING: si aun así el nombre existe, la copia falla en vez de pisar otro adjunto.
        String storedFileName = System.currentTimeMillis() + "_"
                + UUID.randomUUID().toString().substring(0, 8) + "_" + originalName;
        Path targetPath = targetDir.resolve(storedFileName);
        Files.copy(sourceFile.toPath(), targetPath);

        Documento doc = new Documento();
        Path attachmentsDir = Paths.get(baseAttachmentsPath).normalize();
        Path storedPath = attachmentsDir.endsWith(ROOT_ATTACHMENTS_DIR)
                ? Path.of(ROOT_ATTACHMENTS_DIR, categoryFolder, storedFileName)
                : Path.of(categoryFolder, storedFileName);
        doc.setRutaAlmacenamiento(storedPath.toString().replace('\\', '/'));
        doc.setNombreOriginal(originalName);
        doc.setIdEvidencia(idEvidencia);

        try {
            doc.setSha256(computeSha256(targetPath.toFile()));
            doc.setTamanoBytes(Files.size(targetPath));
        } catch (IOException e) {
            try {
                Files.deleteIfExists(targetPath);
            } catch (IOException cleanup) {
                e.addSuppressed(cleanup);
            }
            throw e;
        }
        return doc;
    }

    /**
     * Revisa que el archivo exista y que su extensión esté permitida (RF-15).
     *
     * @param sourceFile archivo seleccionado por el usuario.
     * @throws IOException si el archivo no existe o su extensión no está permitida.
     */
    public void validate(File sourceFile) throws IOException {
        if (sourceFile == null || !sourceFile.exists() || !sourceFile.isFile()) {
            throw new IOException("El archivo seleccionado no existe o no es válido.");
        }
        String extension = getExtension(sourceFile.getName());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IOException("Extensión no permitida (" + extension + "). "
                    + "Formatos válidos: .pdf, .jpg, .jpeg, .png, .docx, .xlsx");
        }
    }

    /**
     * Obtiene todos los documentos asociados a una evidencia.
     *
     * @param idEvidencia identificador de la evidencia.
     * @return lista de documentos registrados.
     * @throws SQLException si ocurre un error SQL.
     */
    public List<Documento> getDocumentsByEvidencia(int idEvidencia) throws SQLException {
        return documentoDAO.findByEvidencia(idEvidencia);
    }

    /**
     * Elimina el registro en base de datos y el archivo físico en disco de un documento adjunto (RF-15).
     *
     * @param doc documento a eliminar.
     * @throws SQLException si falla el borrado en SQLite.
     */
    public void removeDocument(Documento doc) throws SQLException {
        if (doc == null) {
            return;
        }
        documentoDAO.delete(doc.getIdDocumento());
        deleteFile(doc);
    }

    /**
     * Borra solo el archivo físico de un documento, sin tocar la base de datos.
     * Se usa cuando la BD ya borró el registro (por ejemplo, al eliminar un plan, RF-09).
     *
     * @param doc documento cuyo archivo se va a borrar.
     */
    public void deleteFile(Documento doc) {
        if (doc == null || doc.getRutaAlmacenamiento() == null) {
            return;
        }
        try {
            Files.deleteIfExists(resolveFile(doc));
        } catch (IOException ignored) {
            // El registro ya no existe en la BD; un archivo que no se pudo borrar solo queda huérfano
        }
    }

    /** Resuelve la ruta en disco de un adjunto registrado en la base de datos. */
    public Path resolveFile(Documento doc) {
        Path storedPath = Paths.get(doc.getRutaAlmacenamiento()).normalize();
        if (storedPath.isAbsolute()) {
            return storedPath;
        }
        Path attachmentsDir = Paths.get(baseAttachmentsPath).toAbsolutePath().normalize();
        Path dataDir = attachmentsDir.getParent();
        if (storedPath.startsWith(ROOT_ATTACHMENTS_DIR) && dataDir != null) {
            return dataDir.resolve(storedPath);
        }
        return attachmentsDir.resolve(storedPath);
    }

    /**
     * Borra la carpeta de adjuntos completa con todas sus subcarpetas (RF-23).
     * Las subcarpetas se vuelven a crear al adjuntar el siguiente archivo.
     *
     * @throws IOException si algún archivo no se pudo borrar.
     */
    public void deleteAllFiles() throws IOException {
        File folder = new File(baseAttachmentsPath);
        if (folder.exists()) {
            deleteFolder(folder);
        }
    }

    private void deleteFolder(File folder) throws IOException {
        File[] children = folder.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory()) {
                    deleteFolder(child);
                } else {
                    Files.delete(child.toPath());
                }
            }
        }
        Files.delete(folder.toPath());
    }

    /**
     * Traduce el nombre o clave de la categoría a los nombres exactos de subcarpeta
     * exigidos en el requerimiento IS-3.3 del SRS.
     *
     * @param category nombre o clave de categoría.
     * @return nombre normalizado de carpeta.
     */
    public String mapCategoryToFolder(String category) {
        if (category == null) {
            return "otros";
        }
        String normalized = category.trim().toLowerCase();
        if (normalized.contains("docente") || normalized.equals("i.1.1")) {
            return "productividad_docente";
        }
        if (normalized.contains("investigaci") || normalized.equals("i.2")) {
            return "investigacion";
        }
        if (normalized.contains("tutor") || normalized.equals("i.3")) {
            return "tutoria";
        }
        if (normalized.contains("colegiado") || normalized.contains("gesti") || normalized.equals("i.4")) {
            return "cuerpos_colegiados";
        }
        return "otros";
    }

    /**
     * Calcula la huella criptográfica SHA-256 en formato hexadecimal de 64 caracteres (RF-15).
     *
     * @param file archivo a procesar.
     * @return cadena hexadecimal en minúsculas de 64 caracteres.
     * @throws IOException si ocurre un error de lectura.
     */
    public String computeSha256(File file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }
            byte[] hashBytes = digest.digest();
            StringBuilder hex = new StringBuilder(64);
            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 no disponible en el entorno JVM.", e);
        }
    }

    private String getExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot < 0) {
            return "";
        }
        return fileName.substring(lastDot).toLowerCase();
    }
}
