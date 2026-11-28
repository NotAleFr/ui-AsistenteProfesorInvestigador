package mx.unison.assistant.ui.views;

import static org.junit.jupiter.api.Assertions.*;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.GraphicsEnvironment;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import javax.swing.SwingUtilities;
import mx.unison.assistant.data.DatabaseConnection;
import mx.unison.assistant.model.Evidencia;
import mx.unison.assistant.model.Periodo;
import mx.unison.assistant.model.PlanAcademico;
import mx.unison.assistant.service.AppServices;
import mx.unison.assistant.ui.common.StatusBadge;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PlanAcademicoViewTest {

    private static final int TIPO_DISENO_PLANES = 1;
    private static final int ESTATUS_ENTREGADO = 3;

    @TempDir
    Path tempDir;

    private AppServices services;
    private int currentYear;

    @BeforeEach
    void setUp() {
        DatabaseConnection.setDbFilePath(tempDir.resolve("test_ui_plan.db").toString());
        services = new AppServices();
        currentYear = Periodo.yearOf(Periodo.current());
    }

    @AfterEach
    void tearDown() {
        DatabaseConnection.setDbFilePath(null);
    }

    @Test
    void testRecargarMuestraCambiosHechosEnSeguimiento() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        PlanAcademico plan = services.planes.createPlan("Plan recarga", currentYear, currentYear + "-2");
        Evidencia registro = agregarRegistro(plan);
        services.planes.activatePlan(plan.getIdPlanAcademico());

        PlanAcademicoView[] vista = new PlanAcademicoView[1];
        SwingUtilities.invokeAndWait(() -> {
            prepararTema();
            vista[0] = new PlanAcademicoView(services);
            vista[0].selectPlanById(plan.getIdPlanAcademico());
        });

        // Seguimiento cambia el periodo y el estatus del registro
        Evidencia seguido = services.evidencias.findById(registro.getIdEvidencia()).orElseThrow();
        seguido.setPeriodo(currentYear + "-1");
        seguido.setIdEstatus(ESTATUS_ENTREGADO);
        services.evidencias.updateTracking(seguido, null, null);

        SwingUtilities.invokeAndWait(() -> {
            PlanAcademicoView view = vista[0];
            assertEquals(currentYear + "-2", registrosMostrados(view).get(0).getPeriodo());

            // Al editar se usa el registro recién leído, no la copia de la tabla
            Evidencia recienLeido = view.loadFreshRecord(registro.getIdEvidencia());
            assertEquals(currentYear + "-1", recienLeido.getPeriodo());
            assertEquals(ESTATUS_ENTREGADO, recienLeido.getIdEstatus());

            view.reload();
            assertEquals(currentYear + "-1", registrosMostrados(view).get(0).getPeriodo());
        });
    }

    @Test
    void testGuardarBorradorSoloSeHabilitaEnPlanesBorrador() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        PlanAcademico borrador = services.planes.createPlan("Plan borrador", currentYear, currentYear + "-2");
        PlanAcademico activo = services.planes.createPlan("Plan activo", currentYear, currentYear + "-1");
        services.planes.activatePlan(activo.getIdPlanAcademico());

        SwingUtilities.invokeAndWait(() -> {
            prepararTema();
            PlanAcademicoView view = new PlanAcademicoView(services);

            view.selectPlanById(borrador.getIdPlanAcademico());
            assertTrue(view.saveDraftButton.isEnabled());

            view.selectPlanById(activo.getIdPlanAcademico());
            assertFalse(view.saveDraftButton.isEnabled());
            assertFalse(view.activatePlanButton.isEnabled());

            // El espacio del estado alcanza para el badge más ancho
            int anchoActivo = StatusBadge.forPlanState(true).getPreferredSize().width;
            assertTrue(view.planStatusSlot.getPreferredSize().width >= anchoActivo);
        });
    }

    @Test
    void testRecargarActualizaSeccionesSiCambiaElCatalogo() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        PlanAcademicoView[] vista = new PlanAcademicoView[1];
        SwingUtilities.invokeAndWait(() -> {
            prepararTema();
            vista[0] = new PlanAcademicoView(services);
            assertEquals(5, vista[0].sections.size());
        });

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DatabaseConnection.getDbFilePath());
             Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO Categoria (ClaveCategoria, NombreCategoria) VALUES ('X.1', 'Categoría extra')");
        }
        SwingUtilities.invokeAndWait(() -> {
            vista[0].reload();
            assertEquals(6, vista[0].sections.size());
        });

        services.ajustes.restoreCatalog(); // RF-22
        SwingUtilities.invokeAndWait(() -> {
            vista[0].reload();
            assertEquals(5, vista[0].sections.size());
            assertEquals(5, vista[0].recordsByCategory.size());
        });
    }

    private List<Evidencia> registrosMostrados(PlanAcademicoView view) {
        for (List<Evidencia> registros : view.recordsByCategory.values()) {
            if (!registros.isEmpty()) {
                return registros;
            }
        }
        return List.of();
    }

    private Evidencia agregarRegistro(PlanAcademico plan) throws Exception {
        Evidencia ev = new Evidencia();
        ev.setTitulo("Registro del plan");
        ev.setPeriodo(plan.getPeriodoPlan());
        ev.setIdTipoEvidencia(TIPO_DISENO_PLANES);
        ev.setIdPlanAcademico(plan.getIdPlanAcademico());
        return services.planes.addRecord(ev);
    }

    private void prepararTema() {
        FlatLaf.registerCustomDefaultsSource("theme");
        FlatLightLaf.setup();
    }
}
