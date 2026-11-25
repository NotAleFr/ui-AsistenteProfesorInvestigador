package mx.unison.assistant.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import mx.unison.assistant.model.Evidencia;

/**
 * Objeto de Acceso a Datos (DAO) para la tabla {@code Evidencia} (RF-02, RF-04, RF-05, RF-06, RF-10, RF-18).
 * Resuelve mediante JOIN los catálogos asociados ({@code Categoria}, {@code TipoEvidencia},
 * {@code Opcion}, {@code Estatus} y {@code Acreditacion}) y adjunta sus documentos.
 */
public class EvidenciaDAO {

    private static final String BASE_SELECT =
            "SELECT e.IdEvidencia, e.Titulo, e.Descripcion, e.Anio, e.Periodo, "
          + "e.FechaInicio, e.FechaFinalizacion, e.FechaModificacion, "
          + "e.IdTipoEvidencia, e.IdOpcion, e.IdEstatus, e.IdAcreditacion, e.IdPlanAcademico, "
          + "c.IdCategoria, c.NombreCategoria, c.ClaveCategoria, "
          + "t.NombreTipoEvidencia, "
          + "o.NombreOpcion, o.PuntajeOpcion, "
          + "est.NombreEstatus, "
          + "acr.NombreAcreditacion "
          + "FROM Evidencia e "
          + "INNER JOIN TipoEvidencia t ON e.IdTipoEvidencia = t.IdTipoEvidencia "
          + "LEFT JOIN Opcion o ON e.IdOpcion = o.IdOpcion "
          + "INNER JOIN Categoria c ON t.IdCategoria = c.IdCategoria "
          + "INNER JOIN Estatus est ON e.IdEstatus = est.IdEstatus "
          + "INNER JOIN Acreditacion acr ON e.IdAcreditacion = acr.IdAcreditacion ";

    private final DocumentoDAO documentoDAO = new DocumentoDAO();

    /**
     * Obtiene todas las evidencias registradas junto con sus catálogos y documentos adjuntos.
     *
     * @return lista completa de {@link Evidencia}.
     * @throws SQLException si ocurre un error SQL.
     */
    public List<Evidencia> findAll() throws SQLException {
        String sql = BASE_SELECT + "ORDER BY e.Anio DESC, e.Periodo DESC, e.IdEvidencia DESC";
        List<Evidencia> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Evidencia ev = mapRow(rs);
                ev.setDocumentos(documentoDAO.findByEvidencia(ev.getIdEvidencia()));
                list.add(ev);
            }
        }
        return list;
    }

    public List<Evidencia> findActive() throws SQLException {
        String sql = BASE_SELECT
                + "INNER JOIN PlanAcademico p ON e.IdPlanAcademico = p.IdPlanAcademico "
                + "WHERE p.Activo = 1 ORDER BY e.Anio DESC, e.Periodo DESC, e.IdEvidencia DESC";
        List<Evidencia> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Evidencia ev = mapRow(rs);
                ev.setDocumentos(documentoDAO.findByEvidencia(ev.getIdEvidencia()));
                list.add(ev);
            }
        }
        return list;
    }

    /**
     * Obtiene todas las evidencias pertenecientes a un plan académico específico (RF-02).
     *
     * @param idPlanAcademico identificador del plan académico.
     * @return lista de evidencias del plan ordenadas por categoría.
     * @throws SQLException si ocurre un error SQL.
     */
    public List<Evidencia> findByPlan(int idPlanAcademico) throws SQLException {
        String sql = BASE_SELECT + "WHERE e.IdPlanAcademico = ? ORDER BY c.IdCategoria ASC, e.IdEvidencia ASC";
        List<Evidencia> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idPlanAcademico);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Evidencia ev = mapRow(rs);
                    ev.setDocumentos(documentoDAO.findByEvidencia(ev.getIdEvidencia()));
                    list.add(ev);
                }
            }
        }
        return list;
    }

    /**
     * Obtiene todas las evidencias de un periodo escolar (ej. "2026-1").
     *
     * @param periodo identificador de semestre en formato {@code YYYY-1} o {@code YYYY-2}.
     * @return lista de evidencias del periodo.
     * @throws SQLException si ocurre un error SQL.
     */
    public List<Evidencia> findByPeriodo(String periodo) throws SQLException {
        String sql = BASE_SELECT + "WHERE e.Periodo = ? ORDER BY c.IdCategoria ASC, e.IdEvidencia DESC";
        List<Evidencia> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, periodo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Evidencia ev = mapRow(rs);
                    ev.setDocumentos(documentoDAO.findByEvidencia(ev.getIdEvidencia()));
                    list.add(ev);
                }
            }
        }
        return list;
    }

    /**
     * Obtiene las evidencias de un año calendario determinado (ambos semestres, RF-18).
     *
     * @param anio año académico (ej. 2026).
     * @return lista de evidencias del año indicado.
     * @throws SQLException si ocurre un error SQL.
     */
    public List<Evidencia> findByAnio(int anio) throws SQLException {
        String sql = BASE_SELECT + "WHERE e.Anio = ? ORDER BY c.IdCategoria ASC, e.Periodo ASC, e.IdEvidencia DESC";
        List<Evidencia> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, anio);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Evidencia ev = mapRow(rs);
                    ev.setDocumentos(documentoDAO.findByEvidencia(ev.getIdEvidencia()));
                    list.add(ev);
                }
            }
        }
        return list;
    }

    /**
     * Busca una evidencia específica por su identificador único.
     *
     * @param idEvidencia ID de la evidencia.
     * @return {@link Optional} con la evidencia y sus documentos adjuntos.
     * @throws SQLException si ocurre un error SQL.
     */
    public Optional<Evidencia> findById(int idEvidencia) throws SQLException {
        String sql = BASE_SELECT + "WHERE e.IdEvidencia = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idEvidencia);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Evidencia ev = mapRow(rs);
                    ev.setDocumentos(documentoDAO.findByEvidencia(ev.getIdEvidencia()));
                    return Optional.of(ev);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Inserta un nuevo registro de evidencia institucional en SQLite (RF-04).
     *
     * @param ev objeto {@link Evidencia} a persistir.
     * @return identificador autogenerado de la evidencia.
     * @throws SQLException si ocurre un error SQL o restricción CHECK/FK.
     */
    public int insert(Evidencia ev) throws SQLException {
        String sql = "INSERT INTO Evidencia (Titulo, Descripcion, Anio, Periodo, FechaInicio, "
                   + "FechaFinalizacion, IdTipoEvidencia, IdOpcion, IdEstatus, IdAcreditacion, IdPlanAcademico) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, ev.getTitulo());
            ps.setString(2, ev.getDescripcion());
            ps.setInt(3, ev.getAnio());
            ps.setString(4, ev.getPeriodo());
            ps.setString(5, ev.getFechaInicio());
            ps.setString(6, ev.getFechaFinalizacion());
            ps.setInt(7, ev.getIdTipoEvidencia());
            setOpcion(ps, 8, ev.getIdOpcion());
            ps.setInt(9, ev.getIdEstatus() > 0 ? ev.getIdEstatus() : 1);
            ps.setInt(10, ev.getIdAcreditacion() > 0 ? ev.getIdAcreditacion() : 1);
            ps.setInt(11, ev.getIdPlanAcademico());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    ev.setIdEvidencia(id);
                    return id;
                }
            }
        }
        return -1;
    }

    public void updatePlanFields(Evidencia ev) throws SQLException {
        String sql = "UPDATE Evidencia SET Titulo = ?, Descripcion = ?, Anio = ?, Periodo = ?, "
                   + "IdTipoEvidencia = ?, IdOpcion = ? WHERE IdEvidencia = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ev.getTitulo());
            ps.setString(2, ev.getDescripcion());
            ps.setInt(3, ev.getAnio());
            ps.setString(4, ev.getPeriodo());
            ps.setInt(5, ev.getIdTipoEvidencia());
            setOpcion(ps, 6, ev.getIdOpcion());
            ps.setInt(7, ev.getIdEvidencia());
            ps.executeUpdate();
        }
    }

    /**
     * Guarda el seguimiento de una evidencia (Tabla 3). No cambia el plan al que pertenece.
     * El trigger de SQLite actualiza automáticamente {@code FechaModificacion}.
     *
     * @param ev evidencia con los cambios aplicados.
     * @throws SQLException si ocurre un error SQL.
     */
    public void update(Evidencia ev) throws SQLException {
        String sql = "UPDATE Evidencia SET Titulo = ?, Descripcion = ?, Anio = ?, Periodo = ?, "
                   + "FechaInicio = ?, FechaFinalizacion = ?, IdTipoEvidencia = ?, IdOpcion = ?, IdEstatus = ?, "
                   + "IdAcreditacion = ? WHERE IdEvidencia = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ev.getTitulo());
            ps.setString(2, ev.getDescripcion());
            ps.setInt(3, ev.getAnio());
            ps.setString(4, ev.getPeriodo());
            ps.setString(5, ev.getFechaInicio());
            ps.setString(6, ev.getFechaFinalizacion());
            ps.setInt(7, ev.getIdTipoEvidencia());
            setOpcion(ps, 8, ev.getIdOpcion());
            ps.setInt(9, ev.getIdEstatus());
            ps.setInt(10, ev.getIdAcreditacion());
            ps.setInt(11, ev.getIdEvidencia());
            ps.executeUpdate();
        }
    }

    /**
     * Elimina una evidencia de la base de datos. Por {@code ON DELETE CASCADE},
     * SQLite elimina automáticamente sus filas vinculadas en {@code Documento} (RF-06).
     *
     * @param idEvidencia identificador de la evidencia a borrar.
     * @throws SQLException si ocurre un error SQL.
     */
    public void delete(int idEvidencia) throws SQLException {
        String sql = "DELETE FROM Evidencia WHERE IdEvidencia = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idEvidencia);
            ps.executeUpdate();
        }
    }

    // Sin opción elegida (id 0) se guarda NULL
    private void setOpcion(PreparedStatement ps, int index, int idOpcion) throws SQLException {
        if (idOpcion > 0) {
            ps.setInt(index, idOpcion);
        } else {
            ps.setNull(index, Types.INTEGER);
        }
    }

    private Evidencia mapRow(ResultSet rs) throws SQLException {
        Evidencia ev = new Evidencia();
        ev.setIdEvidencia(rs.getInt("IdEvidencia"));
        ev.setTitulo(rs.getString("Titulo"));
        ev.setDescripcion(rs.getString("Descripcion"));
        ev.setAnio(rs.getInt("Anio"));
        ev.setPeriodo(rs.getString("Periodo"));
        ev.setFechaInicio(rs.getString("FechaInicio"));
        ev.setFechaFinalizacion(rs.getString("FechaFinalizacion"));
        ev.setFechaModificacion(rs.getString("FechaModificacion"));
        ev.setIdTipoEvidencia(rs.getInt("IdTipoEvidencia"));
        ev.setIdOpcion(rs.getInt("IdOpcion")); // NULL se lee como 0
        ev.setIdEstatus(rs.getInt("IdEstatus"));
        ev.setIdAcreditacion(rs.getInt("IdAcreditacion"));
        ev.setIdPlanAcademico(rs.getInt("IdPlanAcademico"));
        ev.setIdCategoria(rs.getInt("IdCategoria"));
        ev.setNombreCategoria(rs.getString("NombreCategoria"));
        ev.setClaveCategoria(rs.getString("ClaveCategoria"));
        ev.setNombreTipoEvidencia(rs.getString("NombreTipoEvidencia"));
        ev.setNombreOpcion(rs.getString("NombreOpcion"));
        ev.setPuntajeOpcion(rs.getDouble("PuntajeOpcion"));
        ev.setNombreEstatus(rs.getString("NombreEstatus"));
        ev.setNombreAcreditacion(rs.getString("NombreAcreditacion"));
        return ev;
    }
}