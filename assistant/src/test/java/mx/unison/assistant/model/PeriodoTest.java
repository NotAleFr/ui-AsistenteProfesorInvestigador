package mx.unison.assistant.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Predicate;
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
    void testPeriodoAnterior() {
        assertEquals("2026-1", Periodo.previous("2026-2"));
        assertEquals("2025-2", Periodo.previous("2026-1"));
    }

    @Test
    void testVentanasDeRecordatorios() {
        comprobarVentana(Periodo::isPeriodStart, "2026-01-01", "2026-01-20");
        comprobarVentana(Periodo::isPeriodStart, "2026-08-01", "2026-08-20");
        comprobarVentana(Periodo::isMidPeriod, "2026-03-15", "2026-03-31");
        comprobarVentana(Periodo::isMidPeriod, "2026-10-01", "2026-10-15");
        comprobarVentana(Periodo::isPeriodEnd, "2026-06-01", "2026-06-30");
        comprobarVentana(Periodo::isPeriodEnd, "2026-11-25", "2026-12-20");
    }

    @Test
    void testJulioQuedaFueraDeLasVentanas() {
        for (int dia : List.of(1, 15, 31)) {
            LocalDate fecha = LocalDate.of(2026, 7, dia);
            assertFalse(Periodo.isPeriodStart(fecha));
            assertFalse(Periodo.isMidPeriod(fecha));
            assertFalse(Periodo.isPeriodEnd(fecha));
        }
        assertTrue(Periodo.isPeriodStart(LocalDate.of(2026, 8, 1)));
    }

    private void comprobarVentana(Predicate<LocalDate> estaEnVentana, String inicioTexto, String finTexto) {
        LocalDate fechaInicio = LocalDate.parse(inicioTexto);
        LocalDate fechaFin = LocalDate.parse(finTexto);
        assertFalse(estaEnVentana.test(fechaInicio.minusDays(1)));
        assertTrue(estaEnVentana.test(fechaInicio));
        assertTrue(estaEnVentana.test(fechaFin));
        assertFalse(estaEnVentana.test(fechaFin.plusDays(1)));
    }

    @Test
    void testPeriodosRecientes() {
        int anio = LocalDate.now().getYear();
        assertEquals(List.of(anio, anio - 1, anio - 2), Periodo.recentYears());
        assertEquals(6, Periodo.recentPeriods().size());
        assertEquals(anio + "-1", Periodo.recentPeriods().get(0));
    }
}
