package mx.unison.assistant.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import mx.unison.assistant.model.Documento;

/**
 * Objeto de Acceso a Datos (DAO) para la tabla {@code Documento} (RF-03, RF-04, RF-05).
 * Administra los metadatos y huellas SHA-256 de los archivos probatorios vinculados a evidencias.
 */
public class DocumentoDAO {

    /**
     * Obtiene todos los documentos adjuntos pertenecientes a una evidencia.
     *
     * @param idEvidencia identificador de la evidencia padre.
     * @return lista de {@link Documento} ordenados por fecha de subida.
     * @throws SQLException si ocurre un error SQL.
     */
    public List<Documento> findByEvidencia(int idEvidencia) throws SQLException {
        String sql = "SELECT IdDocumento, RutaAlmacenamiento, NombreOriginal, TamanoBytes, "
                   + "SHA256, FechaSubida, IdEvidencia "
                   + "FROM Documento WHERE IdEvidencia = ? ORDER BY IdDocumento ASC";
        List<Documento> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idEvidencia);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    /**
     * Registra un nuevo documento probatorio asociado a una evidencia (RF-03).
     *
     * @param doc objeto {@link Documento} con ruta, tamaño y huella SHA-256 calculada.
     * @return identificador autogenerado del documento.
     * @throws SQLException si ocurre un error SQL.
     */
    public int insert(Documento doc) throws SQLException {
        String sql = "INSERT INTO Documento (RutaAlmacenamiento, NombreOriginal, TamanoBytes, SHA256, IdEvidencia) "
                   + "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, doc.getRutaAlmacenamiento());
            ps.setString(2, doc.getNombreOriginal());
            ps.setLong(3, doc.getTamanoBytes());
            ps.setString(4, doc.getSha256());
            ps.setInt(5, doc.getIdEvidencia());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    doc.setIdDocumento(id);
                    return id;
                }
            }
        }
        return -1;
    }

    /**
     * Elimina un documento adjunto específico por su identificador (RF-04).
     *
     * @param idDocumento identificador del documento a eliminar.
     * @throws SQLException si ocurre un error SQL.
     */
    public void delete(int idDocumento) throws SQLException {
        String sql = "DELETE FROM Documento WHERE IdDocumento = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idDocumento);
            ps.executeUpdate();
        }
    }

    private Documento mapRow(ResultSet rs) throws SQLException {
        Documento doc = new Documento();
        doc.setIdDocumento(rs.getInt("IdDocumento"));
        doc.setRutaAlmacenamiento(rs.getString("RutaAlmacenamiento"));
        doc.setNombreOriginal(rs.getString("NombreOriginal"));
        doc.setTamanoBytes(rs.getLong("TamanoBytes"));
        doc.setSha256(rs.getString("SHA256"));
        doc.setFechaSubida(rs.getString("FechaSubida"));
        doc.setIdEvidencia(rs.getInt("IdEvidencia"));
        return doc;
    }
}