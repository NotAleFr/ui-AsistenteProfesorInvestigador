package mx.unison.assistant.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import mx.unison.assistant.data.EvidenciaDAO;
import mx.unison.assistant.data.PlanAcademicoDAO;
import mx.unison.assistant.model.Documento;
import mx.unison.assistant.model.Evidencia;
import mx.unison.assistant.model.PlanAcademico;

/**
 * Servicio de lógica de negocio para la gestión de Planes Académicos de Trabajo (RF-01, RF-02, RF-09).
 * Reemplaza los métodos de planes de {@code SampleData}.
 */
public class PlanService {

    private final PlanAcademicoDAO planDAO;
    private final EvidenciaDAO evidenciaDAO;
    private final DocumentoService documentoService;

    public PlanService() {
        this(new PlanAcademicoDAO(), new EvidenciaDAO(), new DocumentoService());
    }

    public PlanService(PlanAcademicoDAO planDAO, EvidenciaDAO evidenciaDAO, DocumentoService documentoService) {
        this.planDAO = planDAO;
        this.evidenciaDAO = evidenciaDAO;
        this.documentoService = documentoService;
    }

    /**
     * Lista todos los planes académicos registrados en formato {@code { nombre, periodo }}
     * compatible con la vista de Plan Académico.
     *
     * @return lista de arreglos con nombre y periodo de cada plan.
     * @throws SQLException si ocurre un error SQL.
     */
    public List<String[]> listPlans() throws SQLException {
        List<PlanAcademico> plans = planDAO.findAll();
        List<String[]> rows = new ArrayList<>();
        for (PlanAcademico plan : plans) {
            rows.add(new String[] { plan.getNombrePlan(), plan.getPeriodoPlan() });
        }
        return rows;
    }

    /**
     * Obtiene todas las entidades {@link PlanAcademico} almacenadas.
     *
     * @return lista completa de planes académicos.
     * @throws SQLException si ocurre un error SQL.
     */
    public List<PlanAcademico> getAllPlans() throws SQLException {
        return planDAO.findAll();
    }

    /**
     * Obtiene los registros asociados a un plan académico por nombre en el formato
     * {@code { título, categoría, tipo de actividad, periodo, descripción }}.
     *
     * @param planName nombre del plan académico seleccionado.
     * @return lista de filas descriptivas del plan.
     * @throws SQLException si ocurre un error SQL.
     */
    public List<String[]> getRecords(String planName) throws SQLException {
        Optional<PlanAcademico> plan = planDAO.findByNombre(planName);
        if (plan.isEmpty()) {
            return List.of();
        }
        List<Evidencia> evidencias = evidenciaDAO.findByPlan(plan.get().getIdPlanAcademico());
        List<String[]> records = new ArrayList<>();
        for (Evidencia ev : evidencias) {
            records.add(new String[] {
                    ev.getTitulo(),
                    ev.getNombreCategoria(),
                    ev.getNombreTipoEvidencia(),
                    ev.getPeriodo(),
                    ev.getDescripcion() != null ? ev.getDescripcion() : ""
            });
        }
        return records;
    }

    /**
     * Obtiene las entidades {@link Evidencia} vinculadas a un plan académico específico.
     *
     * @param idPlanAcademico identificador del plan.
     * @return lista de evidencias del plan.
     * @throws SQLException si ocurre un error SQL.
     */
    public List<Evidencia> getEvidenciasByPlan(int idPlanAcademico) throws SQLException {
        return evidenciaDAO.findByPlan(idPlanAcademico);
    }

    /**
     * Crea y valida un nuevo Plan Académico de Trabajo.
     *
     * @param nombrePlan  nombre descriptivo del plan (ej. "Plan 2026-1").
     * @param periodoPlan periodo escolar en formato {@code YYYY-1} o {@code YYYY-2}.
     * @return entidad {@link PlanAcademico} creada con su ID asignado.
     * @throws SQLException si ocurre un error SQL.
     */
    public PlanAcademico createPlan(String nombrePlan, String periodoPlan) throws SQLException {
        if (nombrePlan == null || nombrePlan.isBlank()) {
            throw new IllegalArgumentException("El nombre del plan académico es obligatorio.");
        }
        if (periodoPlan == null || !periodoPlan.matches("\\d{4}-[12]")) {
            throw new IllegalArgumentException("El periodo debe tener el formato YYYY-1 o YYYY-2.");
        }
        int anio = Integer.parseInt(periodoPlan.substring(0, 4));
        PlanAcademico plan = new PlanAcademico(0, nombrePlan.trim(), anio, periodoPlan.trim(), false);
        planDAO.insert(plan);
        return plan;
    }

    /**
     * Elimina un plan académico junto con todas sus evidencias y sus archivos adjuntos (RF-09).
     *
     * @param idPlanAcademico identificador del plan a borrar.
     * @throws SQLException si ocurre un error SQL.
     */
    public void deletePlan(int idPlanAcademico) throws SQLException {
        // Se guardan los documentos antes de borrar, porque después ya no están en la BD
        List<Documento> documentos = new ArrayList<>();
        for (Evidencia ev : evidenciaDAO.findByPlan(idPlanAcademico)) {
            documentos.addAll(ev.getDocumentos());
        }

        planDAO.delete(idPlanAcademico);

        // Los archivos se borran al final: si la BD falla, no se pierde ningún archivo
        for (Documento doc : documentos) {
            documentoService.deleteFile(doc);
        }
    }
}