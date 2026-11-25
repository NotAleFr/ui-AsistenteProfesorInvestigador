package mx.unison.assistant.ui.common;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

// La pantalla muestra 05/09/2026 y la BD guarda 2026-09-05
public class Fechas {

    // STRICT rechaza fechas que no existen, como 31/02/2026, en vez de cambiarlas a 28/02/2026.
    // En modo STRICT el año se escribe "uuuu", no "yyyy"
    static final DateTimeFormatter SCREEN =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    public static String toScreen(String iso) {
        if (iso == null || iso.isBlank()) {
            return "";
        }
        return LocalDate.parse(iso).format(SCREEN);
    }

    public static String toScreen(LocalDate date) {
        if (date == null) {
            return "";
        }
        return date.format(SCREEN);
    }

    public static String toIso(String screen) {
        if (screen == null || screen.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(screen.trim(), SCREEN).toString();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("La fecha \"" + screen.trim() + "\" no es válida. Usa el formato dd/mm/aaaa.");
        }
    }
}
