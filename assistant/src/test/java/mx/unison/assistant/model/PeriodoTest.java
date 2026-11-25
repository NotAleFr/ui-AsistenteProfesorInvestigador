package mx.unison.assistant.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class PeriodoTest {

    @Test
    void testPeriodoDeUnaFecha() {
        assertEquals("2026-1", Periodo.of(LocalDate.of(2026, 6, 30)));
        assertEquals("2026-2", Periodo.of(LocalDate.of(2026, 7, 1)));
    }

    @Test
    void testFormatoYAnio() {
        assertTrue(Periodo.isValid("2026-1"));
        assertFalse(Periodo.isValid("2026-3"));
        assertFalse(Periodo.isValid("26-1"));
        assertFalse(Periodo.isValid(null));
        assertEquals(2026, Periodo.yearOf("2026-2"));
        assertEquals(List.of("2026-1", "2026-2"), Periodo.periodsOf(2026));
    }

    @Test
    void testInicioYFinDeCadaPeriodo() {
        assertEquals(LocalDate.of(2026, 1, 1), Periodo.startOf("2026-1"));
        assertEquals(LocalDate.of(2026, 6, 30), Periodo.endOf("2026-1"));
        assertEquals(LocalDate.of(2026, 7, 1), Periodo.startOf("2026-2"));
        assertEquals(LocalDate.of(2026, 12, 31), Periodo.endOf("2026-2"));
    }

    @Test
    void testPeriodosRecientes() {
        int anio = LocalDate.now().getYear();
        assertEquals(List.of(anio, anio - 1, anio - 2), Periodo.recentYears());
        assertEquals(6, Periodo.recentPeriods().size());
        assertEquals(anio + "-1", Periodo.recentPeriods().get(0));
    }
}
