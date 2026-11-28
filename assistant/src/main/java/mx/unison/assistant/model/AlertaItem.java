package mx.unison.assistant.model;

/**
 * Alerta individual vinculada a una evidencia para la bandeja de notificaciones (RF-19, RF-20).
 */
public record AlertaItem(
        int idEvidencia,
        int idPlanAcademico,
        boolean planActivo,
        Regla regla,
        String titulo,
        String mensaje
) {
    public enum Regla {
        ENTREGADO_SIN_DOC,
        SIN_DOCUMENTOS,
        FORMATO_NO_PEDPD,
        PENDIENTE_EN_PROCESO
    }

    public String clave() {
        return idEvidencia + ":" + regla.name();
    }
}
