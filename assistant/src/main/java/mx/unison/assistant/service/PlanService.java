package mx.unison.assistant.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import mx.unison.assistant.data.EvidenciaDAO;
import mx.unison.assistant.data.PlanAcademicoDAO;
import mx.unison.assistant.model.Documento;
import mx.unison.assistant.model.Evidencia;
import mx.unison.assistant.model.Periodo;
import mx.unison.assistant.model.PlanAcademico;

// No hay savePlan(): cada registro se guarda al agregarlo, así que un plan nuevo ya es un borrador (RF-07)
public class PlanService {

    public static final int MIN_YEAR = 1900;
    public static final int MAX_YEAR = 2200;

    private final PlanAcademicoDAO planDAO;
    private final EvidenciaDAO evidenciaDAO;
    private final DocumentoService documentoService;
    private final AlertaService alertaService;

    public PlanService(PlanAcademicoDAO planDAO, EvidenciaDAO evidenciaDAO,
                       DocumentoService documentoService, AlertaService alertaService) {
        this.planDAO = planDAO;
        this.evidenciaDAO = evidenciaDAO;
        this.documentoService = documentoService;
        this.alertaService = alertaService;
    }

    public List<PlanAcademico> listPlans() throws SQLException {
        return planDAO.findAll();
    }

    public PlanAcademico createPlan(String nombre, int anio, String periodo) throws SQLException {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del plan académico es obligatorio.");
        }
        if (anio < MIN_YEAR || anio > MAX_YEAR) {
            throw new IllegalArgumentException("El año del plan debe estar entre " + MIN_YEAR + " y " + MAX_YEAR + ".");
        }
        if (!Periodo.isValid(periodo)) {
            throw new IllegalArgumentException("El periodo debe tener el formato YYYY-1 o YYYY-2.");
        }
        if (Periodo.yearOf(periodo) != anio) {
            throw new IllegalArgumentException("El periodo debe pertenecer al año del plan.");
        }
        PlanAcademico plan = new PlanAcademico(0, nombre.trim(), anio, periodo, false);
        planDAO.insert(plan);
        return plan;
    }

    public List<Evidencia> getRecords(int idPlanAcademico) throws SQLException {
        return evidenciaDAO.findByPlan(idPlanAcademico);
    }

    public Evidencia addRecord(Evidencia record) throws SQLException {
        RegistroValidator.validateRecord(record);
        if (record.getIdPlanAcademico() <= 0) {
            throw new IllegalArgumentException("El registro debe pertenecer a un plan académico.");
        }
        record.setAnio(Periodo.yearOf(record.getPeriodo()));
        int id = evidenciaDAO.insert(record);
        alertaService.checkAlerts();
        // Se lee de la BD para regresarlo con su estatus "Pendiente" y el nombre de su categoría
        return evidenciaDAO.findById(id).get();
    }

    public void updateRecord(Evidencia record) throws SQLException {
        if (record == null || record.getIdEvidencia() <= 0) {
            throw new IllegalArgumentException("El registro a editar no tiene un ID válido.");
        }
        RegistroValidator.validateRecord(record);

        Optional<Evidencia> guardado = evidenciaDAO.findById(record.getIdEvidencia());
        if (guardado.isEmpty()) {
            throw new IllegalArgumentException("El registro ya no existe.");
        }
        Evidencia anterior = guardado.get();

        // Si cambió el tipo de actividad, la opción que se había elegido ya no aplica
        if (anterior.getIdTipoEvidencia() == record.getIdTipoEvidencia()) {
            record.setIdOpcion(anterior.getIdOpcion());
        } else {
            record.setIdOpcion(0);
        }

        record.setAnio(Periodo.yearOf(record.getPeriodo()));
        evidenciaDAO.updatePlanFields(record);
        alertaService.checkAlerts();
    }

    public void deleteRecord(int idEvidencia) throws SQLException {
        Optional<Evidencia> guardado = evidenciaDAO.findById(idEvidencia);
        evidenciaDAO.delete(idEvidencia);

        // Los archivos se borran al final: si la BD falla, no se pierde ningún archivo
        if (guardado.isPresent()) {
            for (Documento doc : guardado.get().getDocumentos()) {
                documentoService.deleteFile(doc);
            }
        }
        alertaService.checkAlerts();
    }

    public void activatePlan(int idPlanAcademico) throws SQLException {
        planDAO.activate(idPlanAcademico);
        refreshAlertsAfterCommit();
    }

    private void refreshAlertsAfterCommit() {
        try {
            alertaService.checkAlerts();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deletePlan(int idPlanAcademico) throws SQLException {
        List<Documento> documentos = new ArrayList<>();
        for (Evidencia ev : evidenciaDAO.findByPlan(idPlanAcademico)) {
            documentos.addAll(ev.getDocumentos());
        }

        planDAO.delete(idPlanAcademico);

        // Los archivos se borran al final: si la BD falla, no se pierde ningún archivo
        for (Documento doc : documentos) {
            documentoService.deleteFile(doc);
        }
        alertaService.checkAlerts();
    }
}
