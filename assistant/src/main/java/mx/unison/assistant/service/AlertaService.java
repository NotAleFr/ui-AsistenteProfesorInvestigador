package mx.unison.assistant.service;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import mx.unison.assistant.data.AlertaDAO;
import mx.unison.assistant.data.EvidenciaDAO;
import mx.unison.assistant.model.Alerta;
import mx.unison.assistant.model.AlertaItem;
import mx.unison.assistant.model.AlertaItem.Regla;
import mx.unison.assistant.model.Documento;
import mx.unison.assistant.model.Evidencia;
import mx.unison.assistant.model.Fechas;
import mx.unison.assistant.model.Periodo;
import mx.unison.assistant.model.RecordatorioItem;

/**
 * Servicio de evaluación de reglas de negocio para la Bandeja de Notificaciones
 * y Seguimiento de Pendientes (RF-19, RF-20, RF-21).
 */
public class AlertaService {

    private final EvidenciaDAO evidenciaDAO;
    private final AlertaDAO alertaDAO;
    private final Set<String> readAlertKeys = ConcurrentHashMap.newKeySet();

    public AlertaService(EvidenciaDAO evidenciaDAO, AlertaDAO alertaDAO) {
        this.evidenciaDAO = evidenciaDAO;
        this.alertaDAO = alertaDAO;
    }

    public void markAlertAsRead(String alertKey) {
        if (alertKey != null && !alertKey.isBlank()) {
            readAlertKeys.add(alertKey);
        }
    }

    public List<RecordatorioItem> getStartupReminderItems() throws SQLException {
        return getStartupReminderItems(LocalDate.now());
    }

    public List<RecordatorioItem> getStartupReminderItems(LocalDate today) throws SQLException {
        List<RecordatorioItem> items = new ArrayList<>();
        String currentPeriod = Periodo.of(today);
        String previousPeriod = Periodo.isPeriodStart(today) ? Periodo.previous(currentPeriod) : null;
        LocalDate tomorrow = today.plusDays(1);

        List<Evidencia> previousPeriodPending = new ArrayList<>();
        List<Evidencia> currentPeriodOpen = new ArrayList<>();
        List<Evidencia> dueWithin24Hours = new ArrayList<>();

        for (Evidencia evidencia : evidenciaDAO.findActive()) {
            String estatus = evidencia.getNombreEstatus() != null ? evidencia.getNombreEstatus() : "";
            boolean isPending = "Pendiente".equalsIgnoreCase(estatus);
            boolean isOpen = !"Entregado".equalsIgnoreCase(estatus);

            if (isPending && previousPeriod != null && previousPeriod.equals(evidencia.getPeriodo())) {
                previousPeriodPending.add(evidencia);
            }
            if (isOpen && currentPeriod.equals(evidencia.getPeriodo())) {
                currentPeriodOpen.add(evidencia);
            }
            if (isPending && isDueWithin24Hours(evidencia.getFechaFinalizacion(), today, tomorrow)) {
                dueWithin24Hours.add(evidencia);
            }
        }

        if (!dueWithin24Hours.isEmpty()) {
            items.add(new RecordatorioItem(
                    "Vence pronto",
                    "Evidencias con fecha límite hoy o mañana",
                    "Tienes " + dueWithin24Hours.size() + " evidencia(s) pendiente(s) por vencer en las próximas 24 horas.",
                    true,
                    dueWithin24Hours
            ));
        }
        if (!previousPeriodPending.isEmpty()) {
            items.add(new RecordatorioItem(
                    "Inicio de periodo",
                    "Pendientes del periodo anterior (" + previousPeriod + ")",
                    "Tienes " + previousPeriodPending.size() + " evidencia(s) pendiente(s) del periodo " + previousPeriod + ".",
                    false,
                    previousPeriodPending
            ));
        }
        if (Periodo.isMidPeriod(today)) {
            String detail = currentPeriodOpen.isEmpty()
                    ? "A mitad del periodo " + currentPeriod + ", sube las evidencias de tus actividades recientes."
                    : "Tienes " + currentPeriodOpen.size() + " actividad(es) abiertas en " + currentPeriod
                            + ". Sube las evidencias de tus actividades recientes.";
            items.add(new RecordatorioItem(
                    "Mitad de periodo",
                    "Avance del periodo " + currentPeriod,
                    detail,
                    false,
                    currentPeriodOpen
            ));
        }
        if (Periodo.isPeriodEnd(today)) {
            String periodEndDate = Fechas.toScreen(Periodo.endOf(currentPeriod));
            String detail = currentPeriodOpen.isEmpty()
                    ? "El periodo " + currentPeriod + " cierra el " + periodEndDate
                            + ". Verifica que todas tus evidencias estén entregadas."
                    : "El periodo " + currentPeriod + " cierra el " + periodEndDate + " y tienes "
                            + currentPeriodOpen.size() + " evidencia(s) sin entregar.";
            items.add(new RecordatorioItem(
                    "Cierre de periodo",
                    "Cierre del periodo " + currentPeriod,
                    detail,
                    false,
                    currentPeriodOpen
            ));
        }
        return items;
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
     * persiste los resúmenes activos en {@code Alertas} y devuelve los mensajes generados.
     */
    public List<String> checkAlerts() throws SQLException {
        return checkAlerts(Periodo.current());
    }

    public List<String> checkAlerts(String currentPeriod) throws SQLException {
        List<AlertaItem> alertItems = getAlertItems(currentPeriod);
        List<String> summaries = summarizeAlerts(alertItems, currentPeriod);
        alertaDAO.replaceAll(summaries);
        return summaries;
    }

    /**
     * Evalúa las alertas individuales (RF-19), sincroniza en el mismo paso los resúmenes
     * persistidos en la tabla {@code Alertas} y devuelve solo las no leídas de la sesión (RF-20).
     */
    public List<AlertaItem> refreshAlertItems() throws SQLException {
        return refreshAlertItems(Periodo.current());
    }

    public List<AlertaItem> refreshAlertItems(String currentPeriod) throws SQLException {
        List<AlertaItem> alertItems = getAlertItems(currentPeriod);
        alertaDAO.replaceAll(summarizeAlerts(alertItems, currentPeriod));
        return filterUnreadAlerts(alertItems);
    }

    private List<AlertaItem> filterUnreadAlerts(List<AlertaItem> alertItems) {
        List<AlertaItem> unread = new ArrayList<>();
        for (AlertaItem item : alertItems) {
            if (!readAlertKeys.contains(item.clave())) {
                unread.add(item);
            }
        }
        return unread;
    }

    private List<String> summarizeAlerts(List<AlertaItem> alertItems, String currentPeriod) {
        int sinDocumentosCount = 0;
        int formatoNoPrioritarioCount = 0;
        int entregadosSinDocCount = 0;
        int pendientesEnProcesoCount = 0;

        for (AlertaItem item : alertItems) {
            switch (item.regla()) {
                case SIN_DOCUMENTOS -> sinDocumentosCount++;
                case FORMATO_NO_PEDPD -> formatoNoPrioritarioCount++;
                case ENTREGADO_SIN_DOC -> entregadosSinDocCount++;
                case PENDIENTE_EN_PROCESO -> pendientesEnProcesoCount++;
            }
        }

        List<String> alerts = new ArrayList<>();
        if (sinDocumentosCount > 0) {
            alerts.add("Cuenta con " + sinDocumentosCount + " registro(s) sin documento adjunto probatorio.");
        }
        if (formatoNoPrioritarioCount > 0) {
            alerts.add("Hay " + formatoNoPrioritarioCount
                    + " evidencia(s) sin archivo prioritario (.pdf o .jpg) requerido por el PEDPD.");
        }
        if (entregadosSinDocCount > 0) {
            alerts.add("Hay " + entregadosSinDocCount
                    + " evidencia(s) con estatus \"Entregado\" sin documentos probatorios adjuntos.");
        }
        if (pendientesEnProcesoCount > 0) {
            alerts.add("Hay " + pendientesEnProcesoCount + " evidencia(s) del periodo actual ("
                    + currentPeriod + ") aún en estatus \"Pendiente\" o \"En proceso\".");
        }
        return alerts;
    }

    /**
     * Fuente única de evaluación de las 4 reglas del RF-19 por evidencia individual.
     */
    public List<AlertaItem> getAlertItems() throws SQLException {
        return getAlertItems(Periodo.current());
    }

    public List<AlertaItem> getAlertItems(String currentPeriod) throws SQLException {
        List<Evidencia> evidencias = evidenciaDAO.findAll();
        List<AlertaItem> items = new ArrayList<>();

        for (Evidencia evidencia : evidencias) {
            List<Documento> docs = evidencia.getDocumentos();
            boolean hasDocs = docs != null && !docs.isEmpty();
            String estatus = evidencia.getNombreEstatus() != null ? evidencia.getNombreEstatus() : "";
            String titulo = evidencia.getTitulo() != null ? evidencia.getTitulo() : "Evidencia sin título";
            String periodo = evidencia.getPeriodo() != null ? evidencia.getPeriodo() : "";

            // Regla 3 (RF-19.3) y Regla 1 (RF-19.1): sin documentos probatorios (sin doble conteo)
            if ("Entregado".equalsIgnoreCase(estatus) && !hasDocs) {
                items.add(new AlertaItem(
                        evidencia.getIdEvidencia(),
                        evidencia.getIdPlanAcademico(),
                        evidencia.isPlanActivo(),
                        Regla.ENTREGADO_SIN_DOC,
                        titulo,
                        "Marcada como \"Entregado\" sin documentos adjuntos (" + periodo + ")."
                ));
            } else if (!hasDocs) {
                items.add(new AlertaItem(
                        evidencia.getIdEvidencia(),
                        evidencia.getIdPlanAcademico(),
                        evidencia.isPlanActivo(),
                        Regla.SIN_DOCUMENTOS,
                        titulo,
                        "Sin documento probatorio adjunto (" + periodo + ")."
                ));
            }

            // Regla 2 (RF-19.2): evidencias del periodo actual aún en Pendiente o En proceso
            boolean isPendingOrInProgress = "Pendiente".equalsIgnoreCase(estatus)
                    || "En proceso".equalsIgnoreCase(estatus);
            if (currentPeriod.equals(periodo) && isPendingOrInProgress) {
                items.add(new AlertaItem(
                        evidencia.getIdEvidencia(),
                        evidencia.getIdPlanAcademico(),
                        evidencia.isPlanActivo(),
                        Regla.PENDIENTE_EN_PROCESO,
                        titulo,
                        "Aún en estatus \"" + estatus + "\" en el periodo actual (" + periodo + ")."
                ));
            }

            // Regla 4 (RF-19.4): sin archivo prioritario (.pdf o .jpg) requerido por el PEDPD
            if (hasDocs && !hasPdfOrJpg(docs)) {
                items.add(new AlertaItem(
                        evidencia.getIdEvidencia(),
                        evidencia.getIdPlanAcademico(),
                        evidencia.isPlanActivo(),
                        Regla.FORMATO_NO_PEDPD,
                        titulo,
                        "Sin archivo prioritario (.pdf o .jpg) requerido por el PEDPD (" + periodo + ")."
                ));
            }
        }
        return items;
    }

    /**
     * Recupera los registros almacenados actualmente en la tabla {@code Alertas}.
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
