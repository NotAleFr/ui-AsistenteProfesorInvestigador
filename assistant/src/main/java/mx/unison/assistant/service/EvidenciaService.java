package mx.unison.assistant.service;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import mx.unison.assistant.data.EvidenciaDAO;
import mx.unison.assistant.model.Documento;
import mx.unison.assistant.model.Evidencia;
import mx.unison.assistant.model.Periodo;

// Los filtros y la búsqueda los hace la pantalla sobre la lista ya cargada (RF-11, RF-13)
public class EvidenciaService {

    /** Indicadores de la parte superior de Seguimiento (RF-12). */
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

    public EvidenciaService(EvidenciaDAO evidenciaDAO, DocumentoService documentoService, AlertaService alertaService) {
        this.evidenciaDAO = evidenciaDAO;
        this.documentoService = documentoService;
        this.alertaService = alertaService;
    }

    public List<Evidencia> findForTracking() throws SQLException {
        return evidenciaDAO.findActive();
    }

    public Optional<Evidencia> findById(int idEvidencia) throws SQLException {
        return evidenciaDAO.findById(idEvidencia);
    }

    public ResumenIndicadores countByStatus(List<Evidencia> evidencias) {
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

    // newFiles y removed pueden ser null
    public void updateTracking(Evidencia evidencia, List<File> newFiles, List<Documento> removed)
            throws SQLException, IOException {
        if (evidencia == null || evidencia.getIdEvidencia() <= 0) {
            throw new IllegalArgumentException("La evidencia a editar no tiene un ID válido.");
        }
        RegistroValidator.validateRecord(evidencia);
        if (evidencia.getIdEstatus() <= 0 || evidencia.getIdAcreditacion() <= 0) {
            throw new IllegalArgumentException("Selecciona el estatus y la acreditación.");
        }
        validateDates(evidencia);
        validateFiles(newFiles);
        if (evidenciaDAO.findById(evidencia.getIdEvidencia()).isEmpty()) {
            throw new IllegalArgumentException("El registro ya no existe.");
        }

        evidencia.setAnio(Periodo.yearOf(evidencia.getPeriodo()));
        List<Documento> stagedDocuments = stageNewFiles(evidencia, newFiles);
        try {
            evidenciaDAO.saveTrackingChanges(evidencia, stagedDocuments, removed);
        } catch (SQLException | RuntimeException e) {
            deleteFilesQuietly(stagedDocuments);
            throw e;
        }

        // Los archivos anteriores se borran en disco solo después de confirmar la transacción en BD
        deleteFilesQuietly(removed);
        refreshAlertsAfterCommit();
    }

    private List<Documento> stageNewFiles(Evidencia evidencia, List<File> newFiles) throws IOException, SQLException {
        if (newFiles == null || newFiles.isEmpty()) {
            return List.of();
        }
        String categoryName = evidenciaDAO.findCategoryNameByType(evidencia.getIdTipoEvidencia())
                .orElseThrow(() -> new IllegalArgumentException("El tipo de actividad seleccionado no existe."));
        List<Documento> stagedDocuments = new ArrayList<>();
        try {
            for (File file : newFiles) {
                stagedDocuments.add(documentoService.stageFile(file, evidencia.getIdEvidencia(), categoryName));
            }
        } catch (IOException e) {
            deleteFilesQuietly(stagedDocuments);
            throw e;
        }
        return stagedDocuments;
    }

    private void deleteFilesQuietly(List<Documento> documents) {
        if (documents == null) {
            return;
        }
        for (Documento doc : documents) {
            documentoService.deleteFile(doc);
        }
    }

    private void refreshAlertsAfterCommit() {
        try {
            alertaService.checkAlerts();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void validateDates(Evidencia ev) {
        String inicio = ev.getFechaInicio();
        String fin = ev.getFechaFinalizacion();
        if (inicio == null || fin == null || inicio.isBlank() || fin.isBlank()) {
            return;
        }
        // Con el formato "2026-09-05", comparar el texto da el mismo orden que comparar las fechas
        if (fin.compareTo(inicio) < 0) {
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
}
