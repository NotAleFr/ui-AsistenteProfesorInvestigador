package mx.unison.assistant.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import mx.unison.assistant.model.Acreditacion;
import mx.unison.assistant.model.Categoria;
import mx.unison.assistant.model.Estatus;
import mx.unison.assistant.model.Opcion;
import mx.unison.assistant.model.TipoEvidencia;

/**
 * Objeto de Acceso a Datos (DAO) para los catálogos normativos del Reglamento PEDPD:
 * {@code Categoria}, {@code TipoEvidencia}, {@code Opcion}, {@code Estatus} y {@code Acreditacion}.
 * Soporta consulta jerárquica y personalización de rubros.
 */
public class CatalogoDAO {

    /**
     * Obtiene todas las categorías ordenadas por su identificador.
     *
     * @return lista de categorías registradas.
     * @throws SQLException si ocurre un error de lectura en SQLite.
     */
    public List<Categoria> findAllCategorias() throws SQLException {
        String sql = "SELECT IdCategoria, ClaveCategoria, NombreCategoria, DefinicionCategoria "
                   + "FROM Categoria ORDER BY IdCategoria ASC";
        List<Categoria> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Categoria(
                        rs.getInt("IdCategoria"),
                        rs.getString("ClaveCategoria"),
                        rs.getString("NombreCategoria"),
                        rs.getString("DefinicionCategoria")
                ));
            }
        }
        return list;
    }

    /**
     * Busca una categoría por su nombre exacto (sin distinguir mayúsculas/minúsculas).
     *
     * @param nombreCategoria nombre de la categoría.
     * @return {@link Optional} con la categoría encontrada.
     * @throws SQLException si ocurre un error SQL.
     */
    public Optional<Categoria> findCategoriaByNombre(String nombreCategoria) throws SQLException {
        String sql = "SELECT IdCategoria, ClaveCategoria, NombreCategoria, DefinicionCategoria "
                   + "FROM Categoria WHERE LOWER(NombreCategoria) = LOWER(?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombreCategoria);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Categoria(
                            rs.getInt("IdCategoria"),
                            rs.getString("ClaveCategoria"),
                            rs.getString("NombreCategoria"),
                            rs.getString("DefinicionCategoria")
                    ));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Lista todos los tipos de evidencia pertenecientes a una categoría.
     *
     * @param idCategoria identificador de la categoría padre.
     * @return lista de tipos de actividad asociados.
     * @throws SQLException si ocurre un error SQL.
     */
    public List<TipoEvidencia> findTiposByCategoria(int idCategoria) throws SQLException {
        String sql = "SELECT IdTipoEvidencia, ClaveTipoEvidencia, NombreTipoEvidencia, "
                   + "DefinicionTipoEvidencia, ContenidoAcreditacion, PuntajeMaximo, IdCategoria "
                   + "FROM TipoEvidencia WHERE IdCategoria = ? ORDER BY IdTipoEvidencia ASC";
        List<TipoEvidencia> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCategoria);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapTipoEvidencia(rs));
                }
            }
        }
        return list;
    }

    /**
     * Obtiene el catálogo completo de tipos de evidencia sin filtro de categoría.
     *
     * @return lista de todos los tipos de evidencia.
     * @throws SQLException si ocurre un error SQL.
     */
    public List<TipoEvidencia> findAllTipos() throws SQLException {
        String sql = "SELECT IdTipoEvidencia, ClaveTipoEvidencia, NombreTipoEvidencia, "
                   + "DefinicionTipoEvidencia, ContenidoAcreditacion, PuntajeMaximo, IdCategoria "
                   + "FROM TipoEvidencia ORDER BY IdTipoEvidencia ASC";
        List<TipoEvidencia> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapTipoEvidencia(rs));
            }
        }
        return list;
    }

    /**
     * Busca un tipo de evidencia por su nombre exacto o por etiqueta combinada "Clave Nombre".
     *
     * @param nombreTipo nombre o etiqueta del tipo de actividad.
     * @return {@link Optional} con el tipo de evidencia encontrado.
     * @throws SQLException si ocurre un error SQL.
     */
    public Optional<TipoEvidencia> findTipoByNombre(String nombreTipo) throws SQLException {
        String sql = "SELECT IdTipoEvidencia, ClaveTipoEvidencia, NombreTipoEvidencia, "
                   + "DefinicionTipoEvidencia, ContenidoAcreditacion, PuntajeMaximo, IdCategoria "
                   + "FROM TipoEvidencia WHERE LOWER(NombreTipoEvidencia) = LOWER(?) "
                   + "OR LOWER(ClaveTipoEvidencia || ' ' || NombreTipoEvidencia) = LOWER(?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombreTipo);
            ps.setString(2, nombreTipo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapTipoEvidencia(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Inserta un nuevo tipo de evidencia personalizado en el catálogo.
     *
     * @param tipo objeto {@link TipoEvidencia} con sus atributos.
     * @return el identificador autogenerado.
     * @throws SQLException si ocurre un error SQL.
     */
    public int insertTipoEvidencia(TipoEvidencia tipo) throws SQLException {
        String sql = "INSERT INTO TipoEvidencia (ClaveTipoEvidencia, NombreTipoEvidencia, "
                   + "DefinicionTipoEvidencia, ContenidoAcreditacion, PuntajeMaximo, IdCategoria) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, tipo.getClaveTipoEvidencia());
            ps.setString(2, tipo.getNombreTipoEvidencia());
            ps.setString(3, tipo.getDefinicionTipoEvidencia());
            ps.setString(4, tipo.getContenidoAcreditacion());
            ps.setDouble(5, tipo.getPuntajeMaximo());
            ps.setInt(6, tipo.getIdCategoria());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    tipo.setIdTipoEvidencia(id);
                    return id;
                }
            }
        }
        return -1;
    }

    /**
     * Actualiza los campos descriptivos y puntaje máximo de un tipo de evidencia existente.
     *
     * @param tipo tipo de evidencia modificado.
     * @throws SQLException si ocurre un error SQL.
     */
    public void updateTipoEvidencia(TipoEvidencia tipo) throws SQLException {
        String sql = "UPDATE TipoEvidencia SET ClaveTipoEvidencia = ?, NombreTipoEvidencia = ?, "
                   + "DefinicionTipoEvidencia = ?, ContenidoAcreditacion = ?, PuntajeMaximo = ?, "
                   + "IdCategoria = ? WHERE IdTipoEvidencia = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tipo.getClaveTipoEvidencia());
            ps.setString(2, tipo.getNombreTipoEvidencia());
            ps.setString(3, tipo.getDefinicionTipoEvidencia());
            ps.setString(4, tipo.getContenidoAcreditacion());
            ps.setDouble(5, tipo.getPuntajeMaximo());
            ps.setInt(6, tipo.getIdCategoria());
            ps.setInt(7, tipo.getIdTipoEvidencia());
            ps.executeUpdate();
        }
    }

    /**
     * Lista todas las opciones de acreditación de un tipo de evidencia ordenadas por {@code OrdenOpcion}.
     *
     * @param idTipoEvidencia identificador del tipo de actividad.
     * @return lista de opciones con su puntaje.
     * @throws SQLException si ocurre un error SQL.
     */
    public List<Opcion> findOpcionesByTipo(int idTipoEvidencia) throws SQLException {
        String sql = "SELECT IdOpcion, NombreOpcion, PuntajeOpcion, OrdenOpcion, IdTipoEvidencia "
                   + "FROM Opcion WHERE IdTipoEvidencia = ? ORDER BY OrdenOpcion ASC";
        List<Opcion> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idTipoEvidencia);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapOpcion(rs));
                }
            }
        }
        return list;
    }

    /**
     * Inserta una nueva opción dentro de un tipo de evidencia.
     *
     * @param opcion objeto {@link Opcion} a registrar.
     * @return identificador autogenerado.
     * @throws SQLException si ocurre un error SQL.
     */
    public int insertOpcion(Opcion opcion) throws SQLException {
        String sql = "INSERT INTO Opcion (NombreOpcion, PuntajeOpcion, OrdenOpcion, IdTipoEvidencia) "
                   + "VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, opcion.getNombreOpcion());
            ps.setDouble(2, opcion.getPuntajeOpcion());
            ps.setInt(3, opcion.getOrdenOpcion());
            ps.setInt(4, opcion.getIdTipoEvidencia());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    opcion.setIdOpcion(id);
                    return id;
                }
            }
        }
        return -1;
    }

    /**
     * Actualiza el nombre, puntaje y orden de una opción existente.
     *
     * @param opcion opción modificada.
     * @throws SQLException si ocurre un error SQL.
     */
    public void updateOpcion(Opcion opcion) throws SQLException {
        String sql = "UPDATE Opcion SET NombreOpcion = ?, PuntajeOpcion = ?, OrdenOpcion = ? "
                   + "WHERE IdOpcion = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, opcion.getNombreOpcion());
            ps.setDouble(2, opcion.getPuntajeOpcion());
            ps.setInt(3, opcion.getOrdenOpcion());
            ps.setInt(4, opcion.getIdOpcion());
            ps.executeUpdate();
        }
    }

    /**
     * Obtiene todos los estatus de cumplimiento configurados en la BD.
     *
     * @return lista de {@link Estatus}.
     * @throws SQLException si ocurre un error SQL.
     */
    public List<Estatus> findAllEstatus() throws SQLException {
        String sql = "SELECT IdEstatus, NombreEstatus FROM Estatus ORDER BY IdEstatus ASC";
        List<Estatus> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Estatus(rs.getInt("IdEstatus"), rs.getString("NombreEstatus")));
            }
        }
        return list;
    }

    /**
     * Obtiene todos los estados de acreditación configurados en la BD.
     *
     * @return lista de {@link Acreditacion}.
     * @throws SQLException si ocurre un error SQL.
     */
    public List<Acreditacion> findAllAcreditaciones() throws SQLException {
        String sql = "SELECT IdAcreditacion, NombreAcreditacion FROM Acreditacion ORDER BY IdAcreditacion ASC";
        List<Acreditacion> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Acreditacion(rs.getInt("IdAcreditacion"), rs.getString("NombreAcreditacion")));
            }
        }
        return list;
    }

    private TipoEvidencia mapTipoEvidencia(ResultSet rs) throws SQLException {
        TipoEvidencia t = new TipoEvidencia();
        t.setIdTipoEvidencia(rs.getInt("IdTipoEvidencia"));
        t.setClaveTipoEvidencia(rs.getString("ClaveTipoEvidencia"));
        t.setNombreTipoEvidencia(rs.getString("NombreTipoEvidencia"));
        t.setDefinicionTipoEvidencia(rs.getString("DefinicionTipoEvidencia"));
        t.setContenidoAcreditacion(rs.getString("ContenidoAcreditacion"));
        t.setPuntajeMaximo(rs.getDouble("PuntajeMaximo"));
        t.setIdCategoria(rs.getInt("IdCategoria"));
        return t;
    }

    private Opcion mapOpcion(ResultSet rs) throws SQLException {
        Opcion o = new Opcion();
        o.setIdOpcion(rs.getInt("IdOpcion"));
        o.setNombreOpcion(rs.getString("NombreOpcion"));
        o.setPuntajeOpcion(rs.getDouble("PuntajeOpcion"));
        o.setOrdenOpcion(rs.getInt("OrdenOpcion"));
        o.setIdTipoEvidencia(rs.getInt("IdTipoEvidencia"));
        return o;
    }
}