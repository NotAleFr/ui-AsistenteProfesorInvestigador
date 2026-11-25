package mx.unison.assistant.service;

import java.sql.SQLException;
import java.util.List;
import mx.unison.assistant.data.CatalogoDAO;
import mx.unison.assistant.model.Acreditacion;
import mx.unison.assistant.model.Categoria;
import mx.unison.assistant.model.Estatus;
import mx.unison.assistant.model.Opcion;
import mx.unison.assistant.model.TipoEvidencia;

/**
 * Servicio de lógica de negocio para la consulta y administración de los catálogos
 * normativos del Reglamento PEDPD.
 * Reemplaza los métodos estáticos de catálogo de {@code SampleData}.
 */
public class CatalogoService {

    private final CatalogoDAO catalogoDAO;
    public CatalogoService() {
        this(new CatalogoDAO());
    }

    public CatalogoService(CatalogoDAO catalogoDAO) {
        this.catalogoDAO = catalogoDAO;
    }

    /** Categorías en el orden del Reglamento PEDPD, incluida "Otros" (RF-04). */
    public List<Categoria> getCategories() throws SQLException {
        return catalogoDAO.findAllCategorias();
    }

    /** Tipos de actividad de una categoría (RF-04). */
    public List<TipoEvidencia> getTypes(int idCategoria) throws SQLException {
        return catalogoDAO.findTiposByCategoria(idCategoria);
    }

    /** Todos los tipos de actividad, para el filtro de Seguimiento (RF-11). */
    public List<TipoEvidencia> getAllTypes() throws SQLException {
        return catalogoDAO.findAllTipos();
    }

    /** Opciones de evaluación de un tipo de actividad (Tabla 3). */
    public List<Opcion> getOptions(int idTipoEvidencia) throws SQLException {
        return catalogoDAO.findOpcionesByTipo(idTipoEvidencia);
    }

    /** Estatus de cumplimiento: Pendiente, En proceso, Entregado (Tabla 3). */
    public List<Estatus> getStatuses() throws SQLException {
        return catalogoDAO.findAllEstatus();
    }

    /** Estados de acreditación: Pendiente, En revisión, Aceptado, Publicado (Tabla 3). */
    public List<Acreditacion> getAccreditations() throws SQLException {
        return catalogoDAO.findAllAcreditaciones();
    }

    /**
     * Registra un nuevo tipo de evidencia personalizado previa validación.
     *
     * @param tipo entidad {@link TipoEvidencia} con sus datos obligatorios.
     * @return identificador generado en la base de datos.
     * @throws SQLException si ocurre un error SQL.
     */
    public int addCustomTipoEvidencia(TipoEvidencia tipo) throws SQLException {
        if (tipo.getNombreTipoEvidencia() == null || tipo.getNombreTipoEvidencia().isBlank()) {
            throw new IllegalArgumentException("El nombre del tipo de evidencia es obligatorio.");
        }
        if (tipo.getClaveTipoEvidencia() == null || tipo.getClaveTipoEvidencia().isBlank()) {
            throw new IllegalArgumentException("La clave del tipo de evidencia es obligatoria.");
        }
        if (tipo.getPuntajeMaximo() < 0) {
            throw new IllegalArgumentException("El puntaje máximo no puede ser negativo.");
        }
        return catalogoDAO.insertTipoEvidencia(tipo);
    }

    /**
     * Actualiza un tipo de evidencia existente en el catálogo.
     *
     * @param tipo entidad modificada.
     * @throws SQLException si ocurre un error SQL.
     */
    public void updateTipoEvidencia(TipoEvidencia tipo) throws SQLException {
        if (tipo.getIdTipoEvidencia() <= 0) {
            throw new IllegalArgumentException("Identificador de tipo de evidencia inválido.");
        }
        if (tipo.getNombreTipoEvidencia() == null || tipo.getNombreTipoEvidencia().isBlank()) {
            throw new IllegalArgumentException("El nombre del tipo de evidencia no puede estar vacío.");
        }
        catalogoDAO.updateTipoEvidencia(tipo);
    }

    /**
     * Agrega una nueva opción de evaluación a un tipo de evidencia.
     *
     * @param opcion entidad {@link Opcion} a registrar.
     * @return identificador generado.
     * @throws SQLException si ocurre un error SQL.
     */
    public int addCustomOpcion(Opcion opcion) throws SQLException {
        if (opcion.getNombreOpcion() == null || opcion.getNombreOpcion().isBlank()) {
            throw new IllegalArgumentException("El nombre de la opción es obligatorio.");
        }
        if (opcion.getPuntajeOpcion() < 0) {
            throw new IllegalArgumentException("El puntaje de la opción no puede ser negativo.");
        }
        return catalogoDAO.insertOpcion(opcion);
    }

    /**
     * Actualiza una opción existente del catálogo.
     *
     * @param opcion entidad modificada.
     * @throws SQLException si ocurre un error SQL.
     */
    public void updateOpcion(Opcion opcion) throws SQLException {
        if (opcion.getIdOpcion() <= 0) {
            throw new IllegalArgumentException("Identificador de opción inválido.");
        }
        catalogoDAO.updateOpcion(opcion);
    }
}