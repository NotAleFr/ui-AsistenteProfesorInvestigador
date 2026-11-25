package mx.unison.assistant.services;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import mx.unison.assistant.data.CatalogoDAO;
import mx.unison.assistant.data.DatabaseConnection;
import mx.unison.assistant.models.Acreditacion;
import mx.unison.assistant.models.Categoria;
import mx.unison.assistant.models.Estatus;
import mx.unison.assistant.models.Opcion;
import mx.unison.assistant.models.TipoEvidencia;

/**
 * Servicio de lógica de negocio para la consulta y administración de los catálogos
 * normativos del Reglamento PEDPD (RF-20, RF-21, RF-22, RF-23).
 * Reemplaza los métodos estáticos de catálogo de {@code SampleData}.
 */
public class CatalogoService {

    private final CatalogoDAO catalogoDAO;

    public CatalogoService() {
        this.catalogoDAO = new CatalogoDAO();
    }

    public CatalogoService(CatalogoDAO catalogoDAO) {
        this.catalogoDAO = catalogoDAO;
    }

    /**
     * Obtiene los nombres de todas las categorías normativas registradas.
     *
     * @return arreglo de nombres de categoría en el orden del Reglamento PEDPD.
     * @throws SQLException si ocurre un error al consultar la base de datos.
     */
    public String[] getCategories() throws SQLException {
        List<Categoria> categorias = catalogoDAO.findAllCategorias();
        return categorias.stream()
                .map(Categoria::getNombreCategoria)
                .toArray(String[]::new);
    }

    /**
     * Obtiene la lista completa de objetos {@link Categoria}.
     *
     * @return lista de categorías.
     * @throws SQLException si ocurre un error SQL.
     */
    public List<Categoria> getAllCategorias() throws SQLException {
        return catalogoDAO.findAllCategorias();
    }

    /**
     * Obtiene los nombres de los tipos de evidencia pertenecientes a una categoría.
     *
     * @param categoryName nombre de la categoría seleccionada.
     * @return arreglo con los nombres de tipos de actividad correspondientes.
     * @throws SQLException si ocurre un error SQL.
     */
    public String[] getTypes(String categoryName) throws SQLException {
        Optional<Categoria> cat = catalogoDAO.findCategoriaByNombre(categoryName);
        if (cat.isEmpty()) {
            return new String[0];
        }
        return catalogoDAO.findTiposByCategoria(cat.get().getIdCategoria()).stream()
                .map(TipoEvidencia::getNombreTipoEvidencia)
                .toArray(String[]::new);
    }

    /**
     * Obtiene todos los nombres de tipos de evidencia sin filtro de categoría (RF-13).
     *
     * @return arreglo con todos los tipos de evidencia.
     * @throws SQLException si ocurre un error SQL.
     */
    public String[] getTypes() throws SQLException {
        return catalogoDAO.findAllTipos().stream()
                .map(TipoEvidencia::getNombreTipoEvidencia)
                .toArray(String[]::new);
    }

    /**
     * Obtiene las etiquetas formateadas {@code "Opción (N pts)"} para un tipo de evidencia.
     *
     * @param typeName nombre del tipo de actividad.
     * @return arreglo con las opciones formateadas y su puntaje.
     * @throws SQLException si ocurre un error SQL.
     */
    public String[] getOptions(String typeName) throws SQLException {
        Optional<TipoEvidencia> tipo = catalogoDAO.findTipoByNombre(typeName);
        if (tipo.isEmpty()) {
            return new String[0];
        }
        return catalogoDAO.findOpcionesByTipo(tipo.get().getIdTipoEvidencia()).stream()
                .map(Opcion::getFormattedLabel)
                .toArray(String[]::new);
    }

    /**
     * Obtiene la lista de entidades {@link Opcion} para un tipo de evidencia.
     *
     * @param typeName nombre del tipo de actividad.
     * @return lista de opciones del tipo indicado.
     * @throws SQLException si ocurre un error SQL.
     */
    public List<Opcion> getOpcionEntities(String typeName) throws SQLException {
        Optional<TipoEvidencia> tipo = catalogoDAO.findTipoByNombre(typeName);
        if (tipo.isEmpty()) {
            return List.of();
        }
        return catalogoDAO.findOpcionesByTipo(tipo.get().getIdTipoEvidencia());
    }

    /**
     * Obtiene la definición normativa de un tipo de evidencia.
     *
     * @param typeName nombre del tipo de evidencia.
     * @return definición descriptiva o cadena vacía si no existe.
     * @throws SQLException si ocurre un error SQL.
     */
    public String getDefinition(String typeName) throws SQLException {
        return catalogoDAO.findTipoByNombre(typeName)
                .map(t -> t.getDefinicionTipoEvidencia() != null ? t.getDefinicionTipoEvidencia() : "")
                .orElse("");
    }

    /**
     * Obtiene los criterios y documentos requeridos para acreditar un tipo de evidencia.
     *
     * @param typeName nombre del tipo de evidencia.
     * @return contenido de acreditación o cadena vacía si no existe.
     * @throws SQLException si ocurre un error SQL.
     */
    public String getAccreditation(String typeName) throws SQLException {
        return catalogoDAO.findTipoByNombre(typeName)
                .map(t -> t.getContenidoAcreditacion() != null ? t.getContenidoAcreditacion() : "")
                .orElse("");
    }

    /**
     * Obtiene los nombres de los estados de cumplimiento disponibles (RF-01).
     *
     * @return arreglo con los nombres de estatus.
     * @throws SQLException si ocurre un error SQL.
     */
    public String[] getStatuses() throws SQLException {
        return catalogoDAO.findAllEstatus().stream()
                .map(Estatus::getNombreEstatus)
                .toArray(String[]::new);
    }

    /**
     * Obtiene los nombres de los estados de acreditación disponibles (RF-01).
     *
     * @return arreglo con los estados de acreditación.
     * @throws SQLException si ocurre un error SQL.
     */
    public String[] getAccreditations() throws SQLException {
        return catalogoDAO.findAllAcreditaciones().stream()
                .map(Acreditacion::getNombreAcreditacion)
                .toArray(String[]::new);
    }

    /**
     * Registra un nuevo tipo de evidencia personalizado previa validación (RF-21).
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
     * Actualiza un tipo de evidencia existente en el catálogo (RF-21).
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
     * Agrega una nueva opción de evaluación a un tipo de evidencia (RF-21).
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
     * Actualiza una opción existente del catálogo (RF-21).
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

    /**
     * Restaura las tablas del catálogo normativo a los valores originales del Reglamento PEDPD (RF-22).
     *
     * @throws SQLException si ocurre un error durante la restauración.
     */
    public void restoreDefaultCatalog() throws SQLException {
        DatabaseConnection.restoreDefaultCatalog();
    }

    /**
     * Elimina toda la información del sistema y restablece los catálogos de fábrica (RF-23).
     *
     * @throws SQLException si ocurre un error al limpiar la base de datos.
     */
    public void resetAllSystemData() throws SQLException {
        DatabaseConnection.resetFullDatabase();
    }
}