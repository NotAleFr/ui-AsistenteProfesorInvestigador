package mx.unison.assistant.service;

import mx.unison.assistant.model.Evidencia;
import mx.unison.assistant.model.Periodo;

public class RegistroValidator {

    public static void validateRecord(Evidencia ev) {
        if (ev == null) {
            throw new IllegalArgumentException("La evidencia no puede ser nula.");
        }
        if (ev.getTitulo() == null || ev.getTitulo().isBlank()) {
            throw new IllegalArgumentException("El título de la evidencia es obligatorio.");
        }
        if (!Periodo.isValid(ev.getPeriodo())) {
            throw new IllegalArgumentException("El periodo escolar es obligatorio y debe tener formato YYYY-1 o YYYY-2.");
        }
        if (ev.getIdTipoEvidencia() <= 0) {
            throw new IllegalArgumentException("Selecciona un tipo de actividad.");
        }
    }
}
