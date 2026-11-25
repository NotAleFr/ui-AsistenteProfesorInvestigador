package mx.unison.assistant.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import mx.unison.assistant.data.DocumentoDAO;
import mx.unison.assistant.models.Documento;

/**
 * Servicio encargado de gestionar los archivos probatorios adjuntos a evidencias (RF-03, IS-3.3):
 * valida extensiones permitidas, copia el archivo físico a su subcarpeta de categoría,
 * calcula su huella SHA-256 y tamaño en bytes, y registra sus metadatos en SQLite.
 */
public class DocumentoService {

    public static final String ROOT_ATTACHMENTS_DIR = "archivos_adjuntos";

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".pdf", ".jpg", ".jpeg", ".png", ".docx", ".xlsx"
    );

    private final DocumentoDAO documentoDAO;
    private final String baseAttachmentsPath;

    public DocumentoService() {
        this(new DocumentoDAO(), ROOT_ATTACHMENTS_DIR);
    }

    public DocumentoService(DocumentoDAO documentoDAO, String baseAttachmentsPath) {
        this.documentoDAO = documentoDAO;
        this.baseAttachmentsPath = baseAttachmentsPath;
    }

    /**
     * Valida, copia al almacenamiento local y registra un archivo adjunto vinculado a una evidencia (RF-03).
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
        if (sourceFile == null || !sourceFile.exists() || !sourceFile.isFile()) {
            throw new IOException("El archivo seleccionado no existe o no es válido.");
        }

        String originalName = sourceFile.getName();
        String extension = getExtension(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IOException("Extensión no permitida (" + extension + "). "
                    + "Formatos válidos: .pdf, .jpg, .jpeg, .png, .docx, .xlsx");
        }

        String categoryFolder = mapCategoryToFolder(categoryName);
        Path targetDir = Paths.get(baseAttachmentsPath, categoryFolder);
        Files.createDirectories(targetDir);

        // Prefijo temporal con timestamp para evitar colisiones de nombres duplicados
        String storedFileName = System.currentTimeMillis() + "_" + originalName;
        Path targetPath = targetDir.resolve(storedFileName);
        Files.copy(sourceFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        String sha256 = computeSha256(targetPath.toFile());
        long sizeBytes = Files.size(targetPath);

        Documento doc = new Documento();
        doc.setRutaAlmacenamiento(targetPath.toString().replace("\\", "/"));
        doc.setNombreOriginal(originalName);
        doc.setTamanoBytes(sizeBytes);
        doc.setSha256(sha256);
        doc.setIdEvidencia(idEvidencia);

        documentoDAO.insert(doc);
        return doc;
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
     * Elimina el registro en base de datos y el archivo físico en disco de un documento adjunto (RF-04, RF-05).
     *
     * @param doc documento a eliminar.
     * @throws SQLException si falla el borrado en SQLite.
     */
    public void removeDocument(Documento doc) throws SQLException {
        if (doc == null) {
            return;
        }
        documentoDAO.delete(doc.getIdDocumento());
        if (doc.getRutaAlmacenamiento() != null) {
            try {
                Files.deleteIfExists(Paths.get(doc.getRutaAlmacenamiento()));
            } catch (IOException ignored) {
                // Si el archivo físico ya no existía, se mantiene consistente la BD
            }
        }
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
     * Calcula la huella criptográfica SHA-256 en formato hexadecimal de 64 caracteres (RF-03).
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