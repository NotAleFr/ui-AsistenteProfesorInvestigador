package mx.unison.assistant.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import mx.unison.assistant.models.PlanAcademico;

/**
 * Objeto de Acceso a Datos (DAO) para la tabla {@code PlanAcademico}.
 * Permite listar, crear, actualizar y eliminar planes de trabajo semestrales (RF-06).
 */
public class PlanAcademicoDAO {

    /**
     * Obtiene todos los planes académicos registrados ordenados del más reciente al más antiguo.
     *
     * @return lista de {@link PlanAcademico}.
     * @throws SQLException si ocurre un error SQL.
     */
    public List<PlanAcademico> findAll() throws SQLException {
        String sql = "SELECT IdPlanAcademico, NombrePlan, AnioPlan, PeriodoPlan "
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
        String sql = "SELECT IdPlanAcademico, NombrePlan, AnioPlan, PeriodoPlan "
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
     * Busca un plan académico por su nombre exacto.
     *
     * @param nombrePlan nombre del plan.
     * @return {@link Optional} con el plan encontrado.
     * @throws SQLException si ocurre un error SQL.
     */
    public Optional<PlanAcademico> findByNombre(String nombrePlan) throws SQLException {
        String sql = "SELECT IdPlanAcademico, NombrePlan, AnioPlan, PeriodoPlan "
                   + "FROM PlanAcademico WHERE NombrePlan = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombrePlan);
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

    /**
     * Elimina un plan académico por su ID (siempre que no tenga evidencias restringidas).
     *
     * @param idPlanAcademico identificador del plan a borrar.
     * @throws SQLException si ocurre un error SQL o restricción de llave foránea.
     */
    public void delete(int idPlanAcademico) throws SQLException {
        String sql = "DELETE FROM PlanAcademico WHERE IdPlanAcademico = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idPlanAcademico);
            ps.executeUpdate();
        }
    }

    private PlanAcademico mapRow(ResultSet rs) throws SQLException {
        return new PlanAcademico(
                rs.getInt("IdPlanAcademico"),
                rs.getString("NombrePlan"),
                rs.getInt("AnioPlan"),
                rs.getString("PeriodoPlan")
        );
    }
}