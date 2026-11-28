package mx.unison.assistant.ui.views;

import static org.junit.jupiter.api.Assertions.*;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.GraphicsEnvironment;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import mx.unison.assistant.data.DatabaseConnection;
import mx.unison.assistant.model.Evidencia;
import mx.unison.assistant.model.Periodo;
import mx.unison.assistant.model.PlanAcademico;
import mx.unison.assistant.service.AppServices;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SeguimientoFiltersAndTabsTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void tearDown() {
        DatabaseConnection.setDbFilePath(null);
    }

    @Test
    void testPestanasDeSemestreOrdenadasMasRecientePrimero() {
        assertEquals(
                List.of("2026-2", "2026-1", "Todos"),
                SeguimientoView.buildSemesterTabKeys("2026"));
        assertEquals("Semestre 2026-2", SeguimientoView.formatSemesterTabLabel("2026-2"));
        assertEquals("Todos", SeguimientoView.formatSemesterTabLabel("Todos"));
    }

    @Test
    void testSeleccionaAnioInicialConDatosHistoricosSinSaltarAFuturo() {
        Evidencia ev2025 = evidenciaEnPeriodo("2025-2");
        Evidencia ev2027 = evidenciaEnPeriodo("2027-1");

        assertEquals("2025", SeguimientoView.resolveSelectedYear(
                null, List.of(ev2025), Set.of(2026, 2025, 2024), 2026));
        assertEquals("2026", SeguimientoView.resolveSelectedYear(
                null, List.of(ev2027), Set.of(2027, 2026, 2025), 2026));
        assertEquals("2025", SeguimientoView.resolveSelectedYear(
                "2025", List.of(ev2025, evidenciaEnPeriodo("2026-1")), Set.of(2026, 2025), 2026));
    }

    @Test
    void testSeleccionaSemestreMasRecienteConDatosPorDefecto() {
        Evidencia evPrimero = evidenciaEnPeriodo("2026-1");
        Evidencia evSegundo = evidenciaEnPeriodo("2026-2");

        assertEquals("2026-2",
                SeguimientoView.resolveSelectedSemesterKey("2026", null, List.of(evPrimero, evSegundo), "2026-1"));
        assertEquals("2026-1",
                SeguimientoView.resolveSelectedSemesterKey("2026", null, List.of(evPrimero), "2026-2"));
        assertEquals("2026-1",
                SeguimientoView.resolveSelectedSemesterKey("2026", null, List.of(), "2026-1"));
        assertEquals("2025-2",
                SeguimientoView.resolveSelectedSemesterKey("2025", null, List.of(), "2026-1"));
    }

    @Test
    void testPreservaSeleccionAlRecargarYActualizaAlCambiarAnio() {
        Evidencia evSegundo = evidenciaEnPeriodo("2026-2");

        assertEquals("Todos",
                SeguimientoView.resolveSelectedSemesterKey("2026", "Todos", List.of(evSegundo), "2026-2"));
        assertEquals("2026-1",
                SeguimientoView.resolveSelectedSemesterKey("2026", "2026-1", List.of(evSegundo), "2026-2"));
        assertEquals("2026-2",
                SeguimientoView.resolveSelectedSemesterKey("2026", "2025-1", List.of(evSegundo), "2026-2"));
    }

    @Test
    void testParseoDePuntajeMaximoEnFiltros() {
        assertNull(FiltrosPanel.parseMaxScore(""));
        assertNull(FiltrosPanel.parseMaxScore("   "));
        assertEquals(50.0, FiltrosPanel.parseMaxScore("50"), 0.001);
        assertEquals(12.5, FiltrosPanel.parseMaxScore(" 12.5 "), 0.001);
        assertEquals(0.0, FiltrosPanel.parseMaxScore("0"), 0.001);

        assertThrows(IllegalArgumentException.class, () -> FiltrosPanel.parseMaxScore("abc"));
        assertThrows(IllegalArgumentException.class, () -> FiltrosPanel.parseMaxScore("-1"));
        assertThrows(IllegalArgumentException.class, () -> FiltrosPanel.parseMaxScore("NaN"));
        assertThrows(IllegalArgumentException.class, () -> FiltrosPanel.parseMaxScore("Infinity"));
    }

    @Test
    void testControlesDeSeguimientoSincronizanAnioPestanasYPeriodo() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());

        DatabaseConnection.setDbFilePath(tempDir.resolve("test_ui_seguimiento.db").toString());
        AppServices services = new AppServices();
        int currentYear = Periodo.yearOf(Periodo.current());
        int previousYear = currentYear - 1;
        String currentSem1 = currentYear + "-1";
        String currentSem2 = currentYear + "-2";
        String previousSem2 = previousYear + "-2";

        Evidencia evCurrentSem1 = crearEvidenciaActiva(services, "Evidencia S1", currentYear, currentSem1);
        crearEvidenciaActiva(services, "Evidencia S2", currentYear, currentSem2);
        crearEvidenciaActiva(services, "Evidencia Anterior", previousYear, previousSem2);

        SwingUtilities.invokeAndWait(() -> {
            FlatLaf.registerCustomDefaultsSource("theme");
            FlatLightLaf.setup();
            SeguimientoView view = new SeguimientoView(services);

            // Al abrir, selecciona el año actual y su semestre más reciente con datos (YYYY-2)
            assertEquals(String.valueOf(currentYear), view.yearCombo.getSelectedItem());
            assertEquals(currentSem2, view.selectedSemesterKey);
            assertEquals(currentSem2, view.filtrosPanel.getAppliedFilter().period());
            assertEquals(1, view.visibleRows.size());
            assertEquals("Evidencia S2", view.visibleRows.get(0).getTitulo());

            // Aplicar un periodo desde FiltrosPanel sincroniza la pestaña y las filas visibles
            view.filtrosPanel.periodCombo.setSelectedItem(currentSem1);
            view.filtrosPanel.onApply();
            assertEquals(currentSem1, view.selectedSemesterKey);
            assertEquals(1, view.visibleRows.size());
            assertEquals("Evidencia S1", view.visibleRows.get(0).getTitulo());

            // Cambiar el año en la barra superior actualiza periodos de FiltrosPanel y filas
            view.yearCombo.setSelectedItem(String.valueOf(previousYear));
            assertEquals(previousSem2, view.selectedSemesterKey);
            assertEquals(previousSem2, view.filtrosPanel.getAppliedFilter().period());
            assertEquals(1, view.visibleRows.size());
            assertEquals("Evidencia Anterior", view.visibleRows.get(0).getTitulo());

            // Seleccionar "Todos" en las pestañas sincroniza FiltrosPanel a "Todos"
            JToggleButton todosTab = (JToggleButton) view.tabsPanel.getComponent(2);
            todosTab.doClick();
            assertEquals(FiltrosPanel.ALL, view.selectedSemesterKey);
            assertEquals(FiltrosPanel.ALL, view.filtrosPanel.getAppliedFilter().period());
        });

        // Editar una evidencia a un año nuevo y recargar agrega ese año al combo
        int movedYear = currentYear - 3;
        evCurrentSem1.setPeriodo(movedYear + "-1");
        services.evidencias.updateTracking(evCurrentSem1, null, null);

        SwingUtilities.invokeAndWait(() -> {
            FlatLaf.registerCustomDefaultsSource("theme");
            FlatLightLaf.setup();
            SeguimientoView view = new SeguimientoView(services);
            view.reload();
            boolean foundMovedYear = false;
            for (int i = 0; i < view.yearCombo.getItemCount(); i++) {
                if (String.valueOf(movedYear).equals(view.yearCombo.getItemAt(i))) {
                    foundMovedYear = true;
                    break;
                }
            }
            assertTrue(foundMovedYear);
        });
    }

    private Evidencia crearEvidenciaActiva(AppServices services, String titulo, int anio, String periodo)
            throws Exception {
        PlanAcademico plan = services.planes.createPlan("Plan " + titulo, anio, periodo);
        Evidencia ev = new Evidencia();
        ev.setTitulo(titulo);
        ev.setPeriodo(periodo);
        ev.setIdTipoEvidencia(1);
        ev.setIdPlanAcademico(plan.getIdPlanAcademico());
        Evidencia guardada = services.planes.addRecord(ev);
        services.planes.activatePlan(plan.getIdPlanAcademico());
        return guardada;
    }

    private Evidencia evidenciaEnPeriodo(String periodo) {
        Evidencia evidencia = new Evidencia();
        evidencia.setPeriodo(periodo);
        evidencia.setAnio(Periodo.yearOf(periodo));
        return evidencia;
    }
}
