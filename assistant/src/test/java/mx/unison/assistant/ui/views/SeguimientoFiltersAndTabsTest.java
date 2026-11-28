package mx.unison.assistant.ui.views;

import static org.junit.jupiter.api.Assertions.*;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.Set;
import javax.swing.JComboBox;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
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

    @Test
    void testBusquedaIgnoraAcentosYMayusculas() {
        Evidencia evidencia = evidenciaEnPeriodo("2026-2");
        evidencia.setTitulo("Investigación Educativa");

        assertEquals("investigacion educativa", SeguimientoView.normalizeForSearch("Investigación Educativa"));
        assertTrue(SeguimientoView.matchesFilters(evidencia,
                SeguimientoView.normalizeForSearch("INVESTIGACION"), null, FiltrosPanel.FilterCriteria.defaults()));
        assertTrue(SeguimientoView.matchesFilters(evidencia,
                SeguimientoView.normalizeForSearch("educatíva"), null, FiltrosPanel.FilterCriteria.defaults()));
        assertFalse(SeguimientoView.matchesFilters(evidencia,
                SeguimientoView.normalizeForSearch("tutoria"), null, FiltrosPanel.FilterCriteria.defaults()));
    }

    @Test
    void testPuntajeSinOpcionMuestraRaya() {
        Evidencia sinOpcion = evidenciaEnPeriodo("2026-2");
        assertEquals("—", SeguimientoView.formatScore(sinOpcion));

        Evidencia conOpcion = evidenciaEnPeriodo("2026-2");
        conOpcion.setIdOpcion(1);
        conOpcion.setPuntajeOpcion(80.0);
        assertEquals("80.0", SeguimientoView.formatScore(conOpcion));
    }

    @Test
    void testVistaQueAbrioVaciaMuestraElAnioDeLaPrimeraEvidencia() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        DatabaseConnection.setDbFilePath(tempDir.resolve("test_ui_vacia.db").toString());
        AppServices services = new AppServices();
        int previousYear = Periodo.yearOf(Periodo.current()) - 1;

        SeguimientoView[] vista = new SeguimientoView[1];
        SwingUtilities.invokeAndWait(() -> {
            prepararTema();
            vista[0] = new SeguimientoView(services);
            assertTrue(vista[0].allRows.isEmpty());
        });

        crearEvidenciaActiva(services, "Evidencia de otro año", previousYear, previousYear + "-1");

        SwingUtilities.invokeAndWait(() -> {
            vista[0].reload();
            assertEquals(String.valueOf(previousYear), vista[0].yearCombo.getSelectedItem());
            assertEquals(previousYear + "-1", vista[0].selectedSemesterKey);
            assertEquals(1, vista[0].visibleRows.size());
        });
    }

    @Test
    void testAnioElegidoPorElUsuarioSeConservaAlRecargar() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        DatabaseConnection.setDbFilePath(tempDir.resolve("test_ui_manual.db").toString());
        AppServices services = new AppServices();
        int currentYear = Periodo.yearOf(Periodo.current());
        String chosenYear = String.valueOf(currentYear - 2);

        SeguimientoView[] vista = new SeguimientoView[1];
        SwingUtilities.invokeAndWait(() -> {
            prepararTema();
            vista[0] = new SeguimientoView(services);
            vista[0].yearCombo.setSelectedItem(chosenYear);
        });

        crearEvidenciaActiva(services, "Evidencia del año anterior", currentYear - 1, (currentYear - 1) + "-2");

        SwingUtilities.invokeAndWait(() -> {
            vista[0].reload();
            assertEquals(chosenYear, vista[0].yearCombo.getSelectedItem());
            assertTrue(vista[0].visibleRows.isEmpty());
        });
    }

    @Test
    void testFiltrosRecarganCatalogoConservandoSeleccionesValidas() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        DatabaseConnection.setDbFilePath(tempDir.resolve("test_ui_catalogo.db").toString());
        AppServices services = new AppServices();
        String tipoOriginal = services.catalogo.getAllTypes().get(0).getNombreTipoEvidencia();
        int idTipoOriginal = services.catalogo.getAllTypes().get(0).getIdTipoEvidencia();

        FiltrosPanel[] panel = new FiltrosPanel[1];
        SwingUtilities.invokeAndWait(() -> {
            prepararTema();
            panel[0] = new FiltrosPanel(services, null, null);
            panel[0].typeCombo.setSelectedItem(tipoOriginal);
            panel[0].statusCombo.setSelectedItem("Entregado");
            panel[0].onApply();
        });

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DatabaseConnection.getDbFilePath());
             Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO Estatus (NombreEstatus) VALUES ('Revisado')");
            stmt.execute("UPDATE TipoEvidencia SET NombreTipoEvidencia = 'Tipo renombrado' "
                    + "WHERE IdTipoEvidencia = " + idTipoOriginal);
        }

        SwingUtilities.invokeAndWait(() -> {
            panel[0].reloadCatalog();
            assertTrue(contieneElemento(panel[0].statusCombo, "Revisado"));
            assertTrue(contieneElemento(panel[0].typeCombo, "Tipo renombrado"));
            assertEquals("Entregado", panel[0].statusCombo.getSelectedItem());
            assertEquals("Entregado", panel[0].getAppliedFilter().status());
            // El tipo elegido ya no existe con ese nombre: vuelve a "Todos"
            assertEquals(FiltrosPanel.ALL, panel[0].typeCombo.getSelectedItem());
            assertEquals(FiltrosPanel.ALL, panel[0].getAppliedFilter().type());
        });
    }

    @Test
    void testControlesNoSeEnciman1024ConFiltrosAbiertos() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        DatabaseConnection.setDbFilePath(tempDir.resolve("test_ui_layout.db").toString());
        AppServices services = new AppServices();
        int currentYear = Periodo.yearOf(Periodo.current());
        crearEvidenciaActiva(services, "Evidencia visible", currentYear, currentYear + "-2");

        SwingUtilities.invokeAndWait(() -> {
            prepararTema();
            SeguimientoView view = new SeguimientoView(services);
            int sidebarWidth = UIManager.getInt("Sidebar.width");
            view.setSize(1024 - sidebarWidth, 640);
            view.toggleFilters();
            acomodarTodo(view);

            Rectangle tabs = SwingUtilities.convertRectangle(view.tabsPanel.getParent(), view.tabsPanel.getBounds(), view);
            Rectangle stats = SwingUtilities.convertRectangle(view.statsBar.getParent(), view.statsBar.getBounds(), view);
            assertFalse(tabs.intersects(stats));

            // El combo de año queda dentro de su fila
            Container toolbar = view.yearCombo.getParent();
            Rectangle year = view.yearCombo.getBounds();
            assertTrue(year.width > 0 && year.x + year.width <= toolbar.getWidth());
            assertTrue(year.y + year.height <= toolbar.getHeight());

            // La tabla se desplaza en horizontal en lugar de aplastar las columnas
            assertFalse(view.table.getScrollableTracksViewportWidth());
            assertTrue(view.table.getColumnModel().getColumn(8).getWidth() >= 130);
        });
    }

    private void acomodarTodo(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container) {
                acomodarTodo((Container) child);
            }
        }
    }

    private boolean contieneElemento(JComboBox<String> combo, String valor) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (valor.equals(combo.getItemAt(i))) {
                return true;
            }
        }
        return false;
    }

    private void prepararTema() {
        FlatLaf.registerCustomDefaultsSource("theme");
        FlatLightLaf.setup();
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
