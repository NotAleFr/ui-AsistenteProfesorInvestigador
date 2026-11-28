package mx.unison.assistant.model;

import java.util.List;

/**
 * Recordatorio estructurado que se muestra al iniciar la aplicación (RF-21).
 */
public record RecordatorioItem(
        String etiqueta,
        String titulo,
        String detalle,
        boolean urgente,
        List<Evidencia> evidencias
) {}
