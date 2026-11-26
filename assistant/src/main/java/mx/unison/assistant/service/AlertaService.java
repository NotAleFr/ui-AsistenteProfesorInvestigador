package mx.unison.assistant.service;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import mx.unison.assistant.data.AlertaDAO;
import mx.unison.assistant.data.EvidenciaDAO;
import mx.unison.assistant.model.Alerta;
import mx.unison.assistant.model.Documento;
import mx.unison.assistant.model.Evidencia;
import mx.unison.assistant.model.Periodo;

/**
 * Servicio de evaluación de reglas de negocio para la Bandeja de Notificaciones
 * y Seguimiento de Pendientes (RF-19, RF-20).
 * Reemplaza {@code SampleData.checkAlerts()}.
 */
public class AlertaService {

    private final EvidenciaDAO evidenciaDAO;
    private final AlertaDAO alertaDAO;

    public AlertaService(EvidenciaDAO evidenciaDAO, AlertaDAO alertaDAO) {
        this.evidenciaDAO = evidenciaDAO;
        this.alertaDAO = alertaDAO;
    }

    public List<String> getStartupReminders() throws SQLException {
        return getStartupReminders(LocalDate.now());
    }

    public List<String> getStartupReminders(LocalDate today) throws SQLException {
        List<String> reminders = new ArrayList<>();
        String previousPeriod = Periodo.isPeriodStart(today) ? Periodo.previous(Periodo.of(today)) : null;
        LocalDate tomorrow = today.plusDays(1);
        int previousPeriodPendingCount = 0;
        int dueWithin24HoursCount = 0;
        for (Evidencia evidencia : evidenciaDAO.findActive()) {
            if (!"Pendiente".equalsIgnoreCase(evidencia.getNombreEstatus())) {
                continue;
            }
            if (previousPeriod != null && previousPeriod.equals(evidencia.getPeriodo())) {
                previousPeriodPendingCount++;
            }
            if (isDueWithin24Hours(evidencia.getFechaFinalizacion(), today, tomorrow)) {
                dueWithin24HoursCount++;
            }
        }
        if (previousPeriodPendingCount > 0) {
            reminders.add("Hay " + previousPeriodPendingCount + " evidencia(s) pendiente(s) del periodo anterior ("
                    + previousPeriod + ").");
        }
        if (Periodo.isMidPeriod(today)) {
            reminders.add("A mitad del periodo, sube las evidencias de tus actividades recientes.");
        }
        if (Periodo.isPeriodEnd(today)) {
            reminders.add("Entrega las evidencias de las actividades que finalizan en este periodo.");
        }
        if (dueWithin24HoursCount > 0) {
            reminders.add("Hay " + dueWithin24HoursCount + " evidencia(s) pendiente(s) con fecha límite hoy o mañana.");
        }
        return reminders;
    }

    private boolean isDueWithin24Hours(String endDateText, LocalDate today, LocalDate tomorrow) {
        if (endDateText == null || endDateText.isBlank()) {
            return false;
        }
        try {
            LocalDate deadline = LocalDate.parse(endDateText);
            return !deadline.isBefore(today) && !deadline.isAfter(tomorrow);
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * Inspecciona todas las evidencias en la base de datos bajo las 4 reglas del RF-19,
     * persiste las alertas activas en {@code Alertas} y devuelve los mensajes generados.
     *
     * @return lista de mensajes de alerta activos.
     * @throws SQLException si ocurre un error SQL.
     */
    public List<String> checkAlerts() throws SQLException {
        List<Evidencia> evidencias = evidenciaDAO.findAll();
        List<String> alerts = new ArrayList<>();
        String currentPeriod = Periodo.current();

        int sinDocumentosCount = 0;
        int entregadosSinDocCount = 0;
        int enProcesoCount = 0;
        int pendientesPeriodoCount = 0;
        int formatoNoPrioritarioCount = 0;

        for (Evidencia ev : evidencias) {
            List<Documento> docs = ev.getDocumentos();
            boolean hasDocs = docs != null && !docs.isEmpty();
            String estatus = ev.getNombreEstatus() != null ? ev.getNombreEstatus() : "";

            // Regla 1 (RF-19.1): Evidencias sin documentos probatorios adjuntos
            if (!hasDocs) {
                sinDocumentosCount++;
            }

            // Regla 2 (RF-19.2): Evidencias del periodo actual aún en Pendiente o En proceso
            if (currentPeriod.equals(ev.getPeriodo())) {
                if ("Pendiente".equalsIgnoreCase(estatus)) {
                    pendientesPeriodoCount++;
                } else if ("En proceso".equalsIgnoreCase(estatus)) {
                    enProcesoCount++;
                }
            }

            // Regla 3 (RF-19.3): Inconsistencia de cierre (Entregado sin archivo adjunto)
            if ("Entregado".equalsIgnoreCase(estatus) && !hasDocs) {
                entregadosSinDocCount++;
            }

            // Regla 4 (RF-19.4): Advertencia preventiva PEDPD (ningún archivo .pdf o .jpg)
            if (hasDocs && !hasPdfOrJpg(docs)) {
                formatoNoPrioritarioCount++;
            }
        }

        if (sinDocumentosCount > 0) {
            alerts.add("Cuenta con " + sinDocumentosCount + " registro(s) sin documento adjunto probatorio.");
        }
        if (formatoNoPrioritarioCount > 0) {
            alerts.add("Hay " + formatoNoPrioritarioCount + " evidencia(s) sin archivo prioritario (.pdf o .jpg) requerido por el PEDPD.");
        }
        if (entregadosSinDocCount > 0) {
            alerts.add("Hay " + entregadosSinDocCount + " evidencia(s) con estatus \"Entregado\" sin documentos probatorios adjuntos.");
        }
        if (pendientesPeriodoCount > 0) {
            alerts.add("Hay " + pendientesPeriodoCount + " evidencia(s) del periodo actual (" + currentPeriod + ") en estatus \"Pendiente\".");
        }
        if (enProcesoCount > 0) {
            alerts.add("Hay " + enProcesoCount + " evidencia(s) del periodo actual (" + currentPeriod + ") en estatus \"En proceso\".");
        }

        alertaDAO.replaceAll(alerts);
        return alerts;
    }

    /**
     * Obtiene el detalle completo de alertas activas por evidencia individual (RF-20),
     * indicando tipo de faltante, tipo de evidencia, periodo y extracto de descripción.
     *
     * @return lista detallada de alertas individuales.
     * @throws SQLException si ocurre un error SQL.
     */
    public List<String> getDetailedAlerts() throws SQLException {
        List<Evidencia> evidencias = evidenciaDAO.findAll();
        List<String> detailed = new ArrayList<>();
        String currentPeriod = Periodo.current();

        for (Evidencia ev : evidencias) {
            List<Documento> docs = ev.getDocumentos();
            boolean hasDocs = docs != null && !docs.isEmpty();
            String estatus = ev.getNombreEstatus() != null ? ev.getNombreEstatus() : "";
            String nota = (ev.getDescripcion() != null && !ev.getDescripcion().isBlank())
                    ? " — Nota: " + ev.getDescripcion()
                    : "";

            if ("Entregado".equalsIgnoreCase(estatus) && !hasDocs) {
                detailed.add("[Inconsistencia: Entregado sin documento] " + ev.getTitulo()
                        + " (" + ev.getNombreTipoEvidencia() + " | " + ev.getPeriodo() + ")" + nota);
            } else if (!hasDocs) {
                detailed.add("[Sin documento adjunto] " + ev.getTitulo()
                        + " (" + ev.getNombreTipoEvidencia() + " | " + ev.getPeriodo() + ")" + nota);
            }

            // Regla 2 del RF-19: solo las del periodo actual
            if (currentPeriod.equals(ev.getPeriodo()) && !"Entregado".equalsIgnoreCase(estatus)) {
                detailed.add("[Estatus: " + estatus + "] " + ev.getTitulo()
                        + " (" + ev.getNombreTipoEvidencia() + " | " + ev.getPeriodo() + ")" + nota);
            }

            if (hasDocs && !hasPdfOrJpg(docs)) {
                detailed.add("[Falta formato PDF/JPG] " + ev.getTitulo()
                        + " (" + ev.getNombreTipoEvidencia() + " | " + ev.getPeriodo() + ")" + nota);
            }
        }
        return detailed;
    }

    /**
     * Recupera los registros almacenados actualmente en la tabla {@code Alertas}.
     *
     * @return lista de entidades {@link Alerta}.
     * @throws SQLException si ocurre un error SQL.
     */
    public List<Alerta> getStoredAlerts() throws SQLException {
        return alertaDAO.findAll();
    }

    private boolean hasPdfOrJpg(List<Documento> docs) {
        for (Documento doc : docs) {
            String name = doc.getNombreOriginal() != null ? doc.getNombreOriginal().toLowerCase() : "";
            if (name.endsWith(".pdf") || name.endsWith(".jpg") || name.endsWith(".jpeg")) {
                return true;
            }
        }
        return false;
    }
}
