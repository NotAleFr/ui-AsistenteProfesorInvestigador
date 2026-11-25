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
        evidenciaDAO.update(evidencia);

        if (newFiles != null) {
            // Se lee de nuevo por si cambió el tipo de actividad, porque la categoría decide la carpeta del archivo
            Evidencia guardada = evidenciaDAO.findById(evidencia.getIdEvidencia()).get();
            List<Documento> added = new ArrayList<>();
            try {
                for (File file : newFiles) {
                    added.add(documentoService.attachDocument(
                            file, guardada.getIdEvidencia(), guardada.getNombreCategoria()));
                }
            } catch (IOException | SQLException e) {
                for (Documento doc : added) {
                    documentoService.removeDocument(doc);
                }
                throw e;
            }
        }

        // Los anteriores se quitan hasta que los nuevos ya están guardados
        if (removed != null) {
            for (Documento doc : removed) {
                documentoService.removeDocument(doc);
            }
        }

        alertaService.checkAlerts();
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
