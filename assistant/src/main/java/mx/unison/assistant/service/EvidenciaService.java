package mx.unison.assistant.service;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import mx.unison.assistant.data.EvidenciaDAO;
import mx.unison.assistant.model.Documento;
import mx.unison.assistant.model.Evidencia;

/**
 * Servicio central de lógica de negocio para la gestión, búsqueda, filtrado y cálculo
 * de indicadores de Evidencias Institucionales (RF-04 a RF-06, RF-10 a RF-15).
 * Reemplaza los métodos de seguimiento de {@code SampleData}.
 */
public class EvidenciaService {

    /**
     * Estructura de resumen superior para las vistas tabulares (RF-12).
     */
    public record ResumenIndicadores(
            int total,
            int entregadas,
            int pendientes,
            int enProceso,
            double porcentajeAvance
    ) {}

    private final EvidenciaDAO evidenciaDAO;
    private final DocumentoService documentoService;
    private final AlertaService alertaService;

    public EvidenciaService() {
        this.evidenciaDAO = new EvidenciaDAO();
        this.documentoService = new DocumentoService();
        this.alertaService = new AlertaService(evidenciaDAO, new mx.unison.assistant.data.AlertaDAO());
    }

    public EvidenciaService(EvidenciaDAO evidenciaDAO, DocumentoService documentoService, AlertaService alertaService) {
        this.evidenciaDAO = evidenciaDAO;
        this.documentoService = documentoService;
        this.alertaService = alertaService;
    }

    /**
     * Obtiene todas las evidencias registradas para la vista de seguimiento (RF-10).
     *
     * @return lista completa de {@link Evidencia}.
     * @throws SQLException si ocurre un error SQL.
     */
    public List<Evidencia> findForTracking() throws SQLException {
        return evidenciaDAO.findAll();
    }

    /**
     * Obtiene las evidencias filtradas por periodo semestral (ej. "2026-1").
     *
     * @param periodo semestre a consultar; si es {@code null} o "Todos", devuelve todas.
     * @return lista de evidencias del periodo.
     * @throws SQLException si ocurre un error SQL.
     */
    public List<Evidencia> findByPeriodo(String periodo) throws SQLException {
        if (periodo == null || periodo.isBlank() || "Todos".equalsIgnoreCase(periodo)) {
            return evidenciaDAO.findAll();
        }
        return evidenciaDAO.findByPeriodo(periodo);
    }

    /**
     * Obtiene las evidencias de un año calendario determinado (ambos semestres).
     *
     * @param anio año académico.
     * @return lista de evidencias del año indicado.
     * @throws SQLException si ocurre un error SQL.
     */
    public List<Evidencia> findByAnio(int anio) throws SQLException {
        return evidenciaDAO.findByAnio(anio);
    }

    /**
     * Busca una evidencia específica por su identificador.
     *
     * @param idEvidencia ID de la evidencia.
     * @return {@link Optional} con la evidencia encontrada.
     * @throws SQLException si ocurre un error SQL.
     */
    public Optional<Evidencia> findById(int idEvidencia) throws SQLException {
        return evidenciaDAO.findById(idEvidencia);
    }

    /**
     * Valida y registra una nueva evidencia institucional, adjuntando archivos iniciales si existen,
     * y actualiza la bandeja de notificaciones (RF-04, RF-15, RF-19).
     *
     * @param evidencia     entidad {@link Evidencia} a guardar.
     * @param filesToAttach lista opcional de archivos seleccionados por el usuario.
     * @return evidencia persistida con su ID y documentos asociados.
     * @throws SQLException si ocurre un error SQL.
     * @throws IOException  si falla la copia o validación de algún archivo adjunto.
     */
    public Evidencia registerEvidencia(Evidencia evidencia, List<File> filesToAttach)
            throws SQLException, IOException {
        validateEvidencia(evidencia);
        validateFiles(filesToAttach);

        // El año siempre sale del periodo (YYYY-1 o YYYY-2) para que nunca queden distintos
        evidencia.setAnio(yearFromPeriod(evidencia.getPeriodo()));

        int newId = evidenciaDAO.insert(evidencia);
        evidencia.setIdEvidencia(newId);

        // Recuperar nombres resueltos por JOIN (como NombreCategoria para la subcarpeta de archivos)
        Evidencia saved = evidenciaDAO.findById(newId).orElse(evidencia);

        if (filesToAttach != null) {
            try {
                for (File file : filesToAttach) {
                    Documento doc = documentoService.attachDocument(
                            file, newId, saved.getNombreCategoria());
                    saved.getDocumentos().add(doc);
                }
            } catch (IOException | SQLException e) {
                // Si un adjunto falla, se deshace el registro completo para no dejar evidencias a medias.
                // Primero la BD (sus Documento se borran por ON DELETE CASCADE) y luego los archivos.
                try {
                    evidenciaDAO.delete(newId);
                } catch (SQLException cleanup) {
                    // El registro sigue en la BD: se conservan sus archivos para que no queden rotos
                    e.addSuppressed(cleanup);
                    throw e;
                }
                for (Documento doc : saved.getDocumentos()) {
                    documentoService.deleteFile(doc);
                }
                throw e;
            }
        }

        alertaService.checkAlerts();
        return saved;
    }

    /**
     * Actualiza los datos de una evidencia existente, adjunta nuevos archivos si se proporcionan
     * y refresca las alertas del sistema (RF-05, RF-19).
     *
     * @param evidencia evidencia modificada.
     * @param newFiles  archivos adicionales opcionales.
     * @throws SQLException si ocurre un error SQL.
     * @throws IOException  si falla el procesamiento de archivos adjuntos.
     */
    public void updateEvidencia(Evidencia evidencia, List<File> newFiles)
            throws SQLException, IOException {
        if (evidencia.getIdEvidencia() <= 0) {
            throw new IllegalArgumentException("La evidencia a editar no tiene un ID válido.");
        }
        validateEvidencia(evidencia);
        validateFiles(newFiles);

        evidencia.setAnio(yearFromPeriod(evidencia.getPeriodo()));

        evidenciaDAO.update(evidencia);

        if (newFiles != null && !newFiles.isEmpty()) {
            Evidencia current = evidenciaDAO.findById(evidencia.getIdEvidencia()).orElse(evidencia);
            List<Documento> added = new ArrayList<>();
            try {
                for (File file : newFiles) {
                    added.add(documentoService.attachDocument(
                            file, current.getIdEvidencia(), current.getNombreCategoria()));
                }
            } catch (IOException | SQLException e) {
                // Se quitan solo los adjuntos nuevos de esta edición; los cambios de datos ya se guardaron
                for (Documento doc : added) {
                    try {
                        documentoService.removeDocument(doc);
                    } catch (SQLException cleanup) {
                        e.addSuppressed(cleanup);
                    }
                }
                throw e;
            }
        }

        alertaService.checkAlerts();
    }

    /**
     * Elimina una evidencia junto con sus documentos físicos y registros asociados (RF-06).
     *
     * @param idEvidencia identificador de la evidencia a eliminar.
     * @throws SQLException si ocurre un error SQL.
     */
    public void deleteEvidencia(int idEvidencia) throws SQLException {
        Optional<Evidencia> existing = evidenciaDAO.findById(idEvidencia);
        if (existing.isPresent()) {
            for (Documento doc : existing.get().getDocumentos()) {
                documentoService.removeDocument(doc);
            }
        }
        evidenciaDAO.delete(idEvidencia);
        alertaService.checkAlerts();
    }

    /**
     * Filtra una lista de evidencias aplicando los criterios combinados de búsqueda y tabla (RF-11, RF-13).
     *
     * @param source            lista base de evidencias.
     * @param query             texto libre para buscar en Título, Tipo de actividad o Descripción (RF-13).
     * @param periodo           filtro de periodo ("Todos" o null para no filtrar).
     * @param categoria         filtro de categoría ("Todas" o null para no filtrar).
     * @param tipoActividad     filtro de tipo de actividad ("Todos" o null para no filtrar).
     * @param estatus           filtro de estatus ("Todos" o null para no filtrar).
     * @param docFilter         "Con documentos adjuntos", "Sin documentos adjuntos" o null/"Todas".
     * @return lista filtrada que cumple todos los criterios activos.
     */
    public List<Evidencia> filterEvidencias(
            List<Evidencia> source,
            String query,
            String periodo,
            String categoria,
            String tipoActividad,
            String estatus,
            String docFilter
    ) {
        String normalizedQuery = (query != null) ? query.trim().toLowerCase() : "";

        return source.stream()
                .filter(ev -> {
                    if (!normalizedQuery.isEmpty()) {
                        String titulo = ev.getTitulo() != null ? ev.getTitulo().toLowerCase() : "";
                        String tipo = ev.getNombreTipoEvidencia() != null ? ev.getNombreTipoEvidencia().toLowerCase() : "";
                        String desc = ev.getDescripcion() != null ? ev.getDescripcion().toLowerCase() : "";
                        if (!titulo.contains(normalizedQuery)
                                && !tipo.contains(normalizedQuery)
                                && !desc.contains(normalizedQuery)) {
                            return false;
                        }
                    }
                    if (isFilterActive(periodo) && !periodo.equalsIgnoreCase(ev.getPeriodo())) {
                        return false;
                    }
                    if (isFilterActive(categoria) && !categoria.equalsIgnoreCase(ev.getNombreCategoria())) {
                        return false;
                    }
                    if (isFilterActive(tipoActividad) && !tipoActividad.equalsIgnoreCase(ev.getNombreTipoEvidencia())) {
                        return false;
                    }
                    if (isFilterActive(estatus) && !estatus.equalsIgnoreCase(ev.getNombreEstatus())) {
                        return false;
                    }
                    if (isFilterActive(docFilter)) {
                        int count = ev.getDocumentos() != null ? ev.getDocumentos().size() : 0;
                        if (docFilter.toLowerCase().startsWith("con") && count == 0) {
                            return false;
                        }
                        if (docFilter.toLowerCase().startsWith("sin") && count > 0) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    /**
     * Calcula los indicadores superiores de resumen para las evidencias listadas (RF-12).
     *
     * @param evidencias lista visible en la vista actual.
     * @return {@link ResumenIndicadores} con conteos y porcentaje de avance.
     */
    public ResumenIndicadores calculateSummary(List<Evidencia> evidencias) {
        int total = evidencias.size();
        int entregadas = 0;
        int pendientes = 0;
        int enProceso = 0;

        for (Evidencia ev : evidencias) {
            String est = ev.getNombreEstatus() != null ? ev.getNombreEstatus() : "";
            if ("Entregado".equalsIgnoreCase(est)) {
                entregadas++;
            } else if ("En proceso".equalsIgnoreCase(est)) {
                enProceso++;
            } else {
                pendientes++;
            }
        }

        double avance = (total == 0) ? 0.0 : (entregadas * 100.0) / total;
        return new ResumenIndicadores(total, entregadas, pendientes, enProceso, avance);
    }

    private void validateEvidencia(Evidencia ev) {
        if (ev == null) {
            throw new IllegalArgumentException("La evidencia no puede ser nula.");
        }
        if (ev.getTitulo() == null || ev.getTitulo().isBlank()) {
            throw new IllegalArgumentException("El título de la evidencia es obligatorio.");
        }
        if (ev.getPeriodo() == null || !ev.getPeriodo().matches("\\d{4}-[12]")) {
            throw new IllegalArgumentException("El periodo escolar es obligatorio y debe tener formato YYYY-1 o YYYY-2.");
        }
        if (ev.getIdOpcion() <= 0) {
            throw new IllegalArgumentException("Debe seleccionar una opción de evaluación válida.");
        }
        if (ev.getFechaInicio() != null && ev.getFechaFinalizacion() != null
                && !ev.getFechaInicio().isBlank() && !ev.getFechaFinalizacion().isBlank()
                && ev.getFechaFinalizacion().compareTo(ev.getFechaInicio()) < 0) {
            throw new IllegalArgumentException("La fecha de finalización no puede ser anterior a la fecha de inicio.");
        }
    }

    private void validateFiles(List<File> files) throws IOException {
        if (files == null) {
            return;
        }
        for (File file : files) {
            documentoService.validate(file);
        }
    }

    private int yearFromPeriod(String periodo) {
        return Integer.parseInt(periodo.substring(0, 4));
    }

    private boolean isFilterActive(String value) {
        return value != null
                && !value.isBlank()
                && !"Todos".equalsIgnoreCase(value)
                && !"Todas".equalsIgnoreCase(value)
                && !"Todo".equalsIgnoreCase(value);
    }
}