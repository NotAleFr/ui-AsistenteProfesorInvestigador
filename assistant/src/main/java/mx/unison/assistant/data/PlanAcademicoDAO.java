package mx.unison.assistant.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import mx.unison.assistant.model.PlanAcademico;

/**
 * Objeto de Acceso a Datos (DAO) para la tabla {@code PlanAcademico}.
 * Permite listar, crear, actualizar y eliminar planes de trabajo semestrales (RF-01, RF-02, RF-09).
 */
public class PlanAcademicoDAO {

    /**
     * Obtiene todos los planes académicos registrados ordenados del más reciente al más antiguo.
     *
     * @return lista de {@link PlanAcademico}.
     * @throws SQLException si ocurre un error SQL.
     */
    public List<PlanAcademico> findAll() throws SQLException {
        String sql = "SELECT IdPlanAcademico, NombrePlan, AnioPlan, PeriodoPlan, Activo "
                   + "FROM PlanAcademico ORDER BY AnioPlan DESC, PeriodoPlan DESC, IdPlanAcademico ASC";
        List<PlanAcademico> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    /**
     * Busca un plan académico por su identificador único.
     *
     * @param idPlanAcademico ID del plan.
     * @return {@link Optional} con el plan encontrado.
     * @throws SQLException si ocurre un error SQL.
     */
    public Optional<PlanAcademico> findById(int idPlanAcademico) throws SQLException {
        String sql = "SELECT IdPlanAcademico, NombrePlan, AnioPlan, PeriodoPlan, Activo "
                   + "FROM PlanAcademico WHERE IdPlanAcademico = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idPlanAcademico);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Inserta un nuevo plan académico en la base de datos.
     *
     * @param plan objeto {@link PlanAcademico} a guardar.
     * @return identificador autogenerado del nuevo plan.
     * @throws SQLException si ocurre un error SQL.
     */
    public int insert(PlanAcademico plan) throws SQLException {
        String sql = "INSERT INTO PlanAcademico (NombrePlan, AnioPlan, PeriodoPlan) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, plan.getNombrePlan());
            ps.setInt(2, plan.getAnioPlan());
            ps.setString(3, plan.getPeriodoPlan());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    plan.setIdPlanAcademico(id);
                    return id;
                }
            }
        }
        return -1;
    }

    /**
     * Actualiza los datos de un plan académico existente.
     *
     * @param plan plan con los datos actualizados.
     * @throws SQLException si ocurre un error SQL.
     */
    public void update(PlanAcademico plan) throws SQLException {
        String sql = "UPDATE PlanAcademico SET NombrePlan = ?, AnioPlan = ?, PeriodoPlan = ? "
                   + "WHERE IdPlanAcademico = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, plan.getNombrePlan());
            ps.setInt(2, plan.getAnioPlan());
            ps.setString(3, plan.getPeriodoPlan());
            ps.setInt(4, plan.getIdPlanAcademico());
            ps.executeUpdate();
        }
    }

    /** Da de alta el plan: sus evidencias pasan a Seguimiento (RF-08). */
    public void activate(int idPlanAcademico) throws SQLException {
        String sql = "UPDATE PlanAcademico SET Activo = 1 WHERE IdPlanAcademico = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idPlanAcademico);
            ps.executeUpdate();
        }
    }

    /**
     * Elimina un plan académico y todas sus evidencias en una sola transacción (RF-09).
     *
     * @param idPlanAcademico identificador del plan a borrar.
     * @throws SQLException si ocurre un error SQL; en ese caso no se borra nada.
     */
    public void delete(int idPlanAcademico) throws SQLException {
        // Evidencia.IdPlanAcademico es ON DELETE RESTRICT: primero se borran las evidencias
        // (sus Documento se borran solos por ON DELETE CASCADE) y luego el plan, en una sola transacción.
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement deleteEvidencias = conn.prepareStatement(
                         "DELETE FROM Evidencia WHERE IdPlanAcademico = ?");
                 PreparedStatement deletePlan = conn.prepareStatement(
                         "DELETE FROM PlanAcademico WHERE IdPlanAcademico = ?")) {
                deleteEvidencias.setInt(1, idPlanAcademico);
                deleteEvidencias.executeUpdate();
                deletePlan.setInt(1, idPlanAcademico);
                deletePlan.executeUpdate();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * Consigue el plan academico activo
     * @return {@link Optional} con el plan encontrado.
     * @throws SQLException
     */

    public Optional<PlanAcademico> getPlanActivo_Optional() throws SQLException {
        String sql = "SELECT IdPlanAcademico, NombrePlan, AnioPlan, PeriodoPlan, Activo "
                   + "FROM PlanAcademico WHERE Activo = 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    private PlanAcademico mapRow(ResultSet rs) throws SQLException {
        return new PlanAcademico(
                rs.getInt("IdPlanAcademico"),
                rs.getString("NombrePlan"),
                rs.getInt("AnioPlan"),
                rs.getString("PeriodoPlan"),
                rs.getInt("Activo") == 1
        );
    }
}