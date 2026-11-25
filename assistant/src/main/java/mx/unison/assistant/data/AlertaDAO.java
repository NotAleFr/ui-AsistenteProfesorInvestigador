package mx.unison.assistant.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import mx.unison.assistant.model.Alerta;

/**
 * Objeto de Acceso a Datos (DAO) para la tabla {@code Alertas} (RF-19, RF-20).
 * Permite persistir y consultar las notificaciones generadas por el motor de reglas.
 */
public class AlertaDAO {

    /**
     * Obtiene todas las alertas activas ordenadas de la más reciente a la más antigua.
     *
     * @return lista de {@link Alerta}.
     * @throws SQLException si ocurre un error SQL.
     */
    public List<Alerta> findAll() throws SQLException {
        String sql = "SELECT IdAlerta, ContenidoAlerta, FechaHoraCreacion "
                   + "FROM Alertas ORDER BY IdAlerta DESC";
        List<Alerta> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Alerta(
                        rs.getInt("IdAlerta"),
                        rs.getString("ContenidoAlerta"),
                        rs.getString("FechaHoraCreacion")
                ));
            }
        }
        return list;
    }

    /**
     * Reemplaza el conjunto de alertas activas con una nueva evaluación actualizada (RF-19).
     * Si algo falla, se conservan las alertas anteriores.
     *
     * @param contenidos lista de mensajes de alerta generados.
     * @throws SQLException si ocurre un error SQL.
     */
    public void replaceAll(List<String> contenidos) throws SQLException {
        String insertSql = "INSERT INTO Alertas (ContenidoAlerta) VALUES (?)";
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (Statement clearStmt = conn.createStatement();
                 PreparedStatement ps = conn.prepareStatement(insertSql)) {
                clearStmt.executeUpdate("DELETE FROM Alertas");
                for (String contenido : contenidos) {
                    ps.setString(1, contenido);
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }
}