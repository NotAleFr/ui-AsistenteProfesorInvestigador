package mx.unison.assistant.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class FechasTest {

    @Test
    void testDeBaseDeDatosAPantalla() {
        assertEquals("05/09/2026", Fechas.toScreen("2026-09-05"));
        assertEquals("31/12/2026", Fechas.toScreen(LocalDate.of(2026, 12, 31)));
        assertEquals("", Fechas.toScreen((String) null));
        assertEquals("", Fechas.toScreen(""));
        assertEquals("2026-11-31", Fechas.toScreen("2026-11-31"));
    }

    @Test
    void testDePantallaABaseDeDatos() {
        assertEquals("2026-09-05", Fechas.toIso("05/09/2026"));
        assertEquals("2026-09-05", Fechas.toIso(" 05/09/2026 "));
        assertNull(Fechas.toIso(""));
        assertNull(Fechas.toIso(null));
    }

    @Test
    void testFechaInvalidaDaMensajeParaElUsuario() {
        assertThrows(IllegalArgumentException.class, () -> Fechas.toIso("31/02/2026"));
        assertThrows(IllegalArgumentException.class, () -> Fechas.toIso("2026-09-05"));
        assertThrows(IllegalArgumentException.class, () -> Fechas.toIso("5/9/26"));
    }
}
