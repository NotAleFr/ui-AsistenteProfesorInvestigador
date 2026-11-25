package mx.unison.assistant.service;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import mx.unison.assistant.data.AlertaDAO;
import mx.unison.assistant.data.CatalogoDAO;
import mx.unison.assistant.data.DatabaseConnection;
import mx.unison.assistant.data.DocumentoDAO;
import mx.unison.assistant.data.EvidenciaDAO;
import mx.unison.assistant.data.PlanAcademicoDAO;
import mx.unison.assistant.model.Categoria;
import mx.unison.assistant.model.Documento;
import mx.unison.assistant.model.Estatus;
import mx.unison.assistant.model.Evidencia;
import mx.unison.assistant.model.Opcion;
import mx.unison.assistant.model.PlanAcademico;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Pruebas de integración sobre una base de datos SQLite temporal: catálogos PEDPD,
 * planes y sus registros, seguimiento con adjuntos SHA-256, alertas y ajustes.
 */
class BackendServicesTest {

    private static final int TIPO_DISENO_PLANES = 1;    // opciones 1 a 3
    private static final int OPCION_COORDINADOR = 1;    // 80 pts, del tipo 1
    private static final int OPCION_DE_OTRO_TIPO = 4;   // pertenece al tipo 2
    private static final int ESTATUS_ENTREGADO = 3;

    @TempDir
    Path tempDir;

    private CatalogoService catalogoService;
    private PlanService planService;
    private EvidenciaService evidenciaService;
    private AlertaService alertaService;
    private AjustesService ajustesService;

    @BeforeEach
    void setUp() {
        DatabaseConnection.setDbFilePath(tempDir.resolve("test_asistente.db").toString());

        DocumentoService documentoService = new DocumentoService(
                new DocumentoDAO(), tempDir.resolve("archivos_adjuntos").toString());
        EvidenciaDAO evidenciaDAO = new EvidenciaDAO();
        alertaService = new AlertaService(evidenciaDAO, new AlertaDAO());
        catalogoService = new CatalogoService(new CatalogoDAO());
        ajustesService = new AjustesService(documentoService);
        planService = new PlanService(new PlanAcademicoDAO(), evidenciaDAO, documentoService, alertaService);
        evidenciaService = new EvidenciaService(evidenciaDAO, documentoService, alertaService);
    }

    @Test
    void testCatalogosInicializadosDesdeSql() throws Exception {
        List<Categoria> categorias = catalogoService.getCategories();
        assertEquals(5, categorias.size());
        assertEquals("Productividad Docente", categorias.get(0).toString());

        assertFalse(catalogoService.getTypes(categorias.get(0).getIdCategoria()).isEmpty());

        List<Opcion> opciones = catalogoService.getOptions(TIPO_DISENO_PLANES);
        assertEquals(3, opciones.size());
        assertEquals("Coordinador de la comisión responsable (Sólo un coordinador por Plan de Estudios) (80 pts)",
                opciones.get(0).toString());

        List<String> estatus = new ArrayList<>();
        for (Estatus e : catalogoService.getStatuses()) {
            estatus.add(e.getNombreEstatus());
        }
        assertEquals(List.of("Pendiente", "En proceso", "Entregado"), estatus);
    }

    @Test
    void testCategoriaOtrosTieneTipoEvidencia() throws Exception {
        // RF-04: la categoría adicional "Otros" debe poder usarse
        Categoria otros = catalogoService.getCategories().get(4);
        assertEquals(1, catalogoService.getTypes(otros.getIdCategoria()).size());
    }

    @Test
    void testCatalogoSeComparaPorId() throws Exception {
        assertEquals(catalogoService.getCategories().get(0), catalogoService.getCategories().get(0));
    }

    @Test
    void testCrearPlanValidaAnioYPeriodo() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> planService.createPlan("Plan", 2025, "2026-1"));
        assertThrows(IllegalArgumentException.class, () -> planService.createPlan("Plan", 2026, "2026-3"));
        assertThrows(IllegalArgumentException.class, () -> planService.createPlan(" ", 2026, "2026-1"));

        PlanAcademico plan = planService.createPlan("Plan 2026-1", 2026, "2026-1");
        assertFalse(plan.isActivo());
        assertEquals(List.of(plan), planService.listPlans());
    }

    @Test
    void testFlujoCompletoPlanSeguimientoYEliminar() throws Exception {
        // RF-01 y RF-04: el registro del plan no lleva opción ni archivos
        PlanAcademico plan = planService.createPlan("Plan 2026-2", 2026, "2026-2");
        Evidencia registro = planService.addRecord(nuevoRegistro(plan));
        assertEquals(0, registro.getIdOpcion());
        assertEquals(1, planService.getRecords(plan.getIdPlanAcademico()).size());

        // RF-07: mientras el plan es borrador no aparece en Seguimiento
        assertTrue(evidenciaService.findForTracking().isEmpty());

        // RF-08: al darlo de alta pasa a Seguimiento
        planService.activatePlan(plan.getIdPlanAcademico());
        assertEquals(1, evidenciaService.findForTracking().size());

        // Tabla 3 y RF-15: opción, estatus y un PDF
        registro.setIdOpcion(OPCION_COORDINADOR);
        registro.setIdEstatus(ESTATUS_ENTREGADO);
        registro.setFechaInicio("2026-07-01");
        registro.setFechaFinalizacion("2026-12-31");
        evidenciaService.updateTracking(registro, List.of(crearPdf("constancia_oficial.pdf")), null);

        Evidencia seguida = evidenciaService.findById(registro.getIdEvidencia()).get();
        assertEquals(80.0, seguida.getPuntajeOpcion(), 0.01);
        Documento doc = seguida.getDocumentos().get(0);
        assertEquals("constancia_oficial.pdf", doc.getNombreOriginal());
        assertEquals(64, doc.getSha256().length());
        assertTrue(doc.getRutaAlmacenamiento().contains("productividad_docente"));

        // RF-12
        EvidenciaService.ResumenIndicadores resumen = evidenciaService.countByStatus(evidenciaService.findForTracking());
        assertEquals(1, resumen.entregadas());
        assertEquals(100.0, resumen.porcentajeAvance(), 0.01);

        // RF-06: se borran el registro y su archivo
        Path archivo = Path.of(doc.getRutaAlmacenamiento());
        planService.deleteRecord(registro.getIdEvidencia());
        assertTrue(evidenciaService.findById(registro.getIdEvidencia()).isEmpty());
        assertFalse(Files.exists(archivo));
    }

    @Test
    void testEditarRegistroCambiandoTipoQuitaLaOpcion() throws Exception {
        PlanAcademico plan = planService.createPlan("Plan tipo", 2026, "2026-2");
        Evidencia registro = planService.addRecord(nuevoRegistro(plan));
        registro.setIdOpcion(OPCION_COORDINADOR);
        evidenciaService.updateTracking(registro, null, null);

        registro.setTitulo("Título nuevo");
        planService.updateRecord(registro);
        assertEquals(OPCION_COORDINADOR, evidenciaService.findById(registro.getIdEvidencia()).get().getIdOpcion());

        registro.setIdTipoEvidencia(2);
        planService.updateRecord(registro);
        Evidencia editada = evidenciaService.findById(registro.getIdEvidencia()).get();
        assertEquals(0, editada.getIdOpcion());
        assertEquals("Título nuevo", editada.getTitulo());
    }

    @Test
    void testOpcionDeOtroTipoSeRechaza() throws Exception {
        PlanAcademico plan = planService.createPlan("Plan opcion ajena", 2026, "2026-2");
        Evidencia registro = planService.addRecord(nuevoRegistro(plan));
        registro.setIdOpcion(OPCION_DE_OTRO_TIPO);

        assertThrows(SQLException.class, () -> evidenciaService.updateTracking(registro, null, null));
    }

    @Test
    void testQuitarDocumentoEnSeguimientoBorraRegistroYArchivo() throws Exception {
        Evidencia registro = registroEnSeguimiento();
        evidenciaService.updateTracking(registro, List.of(crearPdf("acta.pdf")), null);
        Documento doc = evidenciaService.findById(registro.getIdEvidencia()).get().getDocumentos().get(0);

        evidenciaService.updateTracking(registro, null, List.of(doc));

        assertTrue(evidenciaService.findById(registro.getIdEvidencia()).get().getDocumentos().isEmpty());
        assertFalse(Files.exists(Path.of(doc.getRutaAlmacenamiento())));
    }

    @Test
    void testAdjuntoInvalidoNoDejaArchivosNiQuitaLosAnteriores() throws Exception {
        Evidencia registro = registroEnSeguimiento();
        evidenciaService.updateTracking(registro, List.of(crearPdf("anterior.pdf")), null);
        Documento anterior = evidenciaService.findById(registro.getIdEvidencia()).get().getDocumentos().get(0);
        Path exe = tempDir.resolve("programa.exe");
        Files.writeString(exe, "no permitido");

        assertThrows(Exception.class, () -> evidenciaService.updateTracking(
                registro, List.of(crearPdf("bueno.pdf"), exe.toFile()), List.of(anterior)));

        List<Documento> docs = evidenciaService.findById(registro.getIdEvidencia()).get().getDocumentos();
        assertEquals(1, docs.size());
        assertEquals("anterior.pdf", docs.get(0).getNombreOriginal());
    }

    @Test
    void testSeguimientoDeRegistroBorradoDaMensaje() throws Exception {
        Evidencia registro = registroEnSeguimiento();
        planService.deleteRecord(registro.getIdEvidencia());

        assertThrows(IllegalArgumentException.class,
                () -> evidenciaService.updateTracking(registro, List.of(crearPdf("acta.pdf")), null));
    }

    @Test
    void testEliminarPlanConEvidenciasYAdjuntos() throws Exception {
        Evidencia registro = registroEnSeguimiento();
        evidenciaService.updateTracking(registro, List.of(crearPdf("acta.pdf")), null);
        Path archivo = Path.of(evidenciaService.findById(registro.getIdEvidencia()).get()
                .getDocumentos().get(0).getRutaAlmacenamiento());

        planService.deletePlan(registro.getIdPlanAcademico()); // RF-09

        assertTrue(planService.getRecords(registro.getIdPlanAcademico()).isEmpty());
        assertTrue(planService.listPlans().isEmpty());
        assertFalse(Files.exists(archivo));
    }

    @Test
    void testCambiarPeriodoActualizaAnio() throws Exception {
        PlanAcademico plan = planService.createPlan("Plan anio", 2026, "2026-2");
        Evidencia registro = planService.addRecord(nuevoRegistro(plan));
        assertEquals(2026, registro.getAnio());

        registro.setPeriodo("2027-1");
        planService.updateRecord(registro);

        assertEquals(2027, evidenciaService.findById(registro.getIdEvidencia()).get().getAnio());
    }

    @Test
    void testAlertasCuentanRegistrosDeBorradores() throws Exception {
        // RF-19: "cualquier registro en la tabla Evidencia", también los de planes borrador
        PlanAcademico borrador = planService.createPlan("Plan borrador", 2026, "2026-2");
        planService.addRecord(nuevoRegistro(borrador));

        List<String> alertas = alertaService.checkAlerts();
        assertTrue(alertas.get(0).contains("1 registro(s) sin documento"));
    }

    @Test
    void testAlertasAnterioresSeConservanSiFallaElReemplazo() throws Exception {
        AlertaDAO alertaDAO = new AlertaDAO();
        alertaDAO.replaceAll(List.of("Alerta 1", "Alerta 2"));

        // ContenidoAlerta es NOT NULL: el segundo INSERT falla y se deshace todo el reemplazo
        assertThrows(SQLException.class, () -> alertaDAO.replaceAll(Arrays.asList("Alerta nueva", null)));

        assertEquals(2, alertaDAO.findAll().size());
    }

    @Test
    void testRestaurarCatalogoConservaEvidencias() throws Exception {
        registroEnSeguimiento();

        ajustesService.restoreCatalog(); // RF-22

        assertEquals(5, catalogoService.getCategories().size());
        assertEquals(1, evidenciaService.findForTracking().size());
    }

    @Test
    void testRestablecerTodoBorraDatosYArchivos() throws Exception {
        Evidencia registro = registroEnSeguimiento();
        evidenciaService.updateTracking(registro, List.of(crearPdf("oficio.pdf")), null);
        assertTrue(Files.exists(tempDir.resolve("archivos_adjuntos")));

        ajustesService.deleteAll(); // RF-23

        assertTrue(planService.listPlans().isEmpty()); // no se crea ningún plan por defecto
        assertTrue(evidenciaService.findForTracking().isEmpty());
        assertFalse(Files.exists(tempDir.resolve("archivos_adjuntos")));
        assertEquals(5, catalogoService.getCategories().size());
    }

    private Evidencia registroEnSeguimiento() throws Exception {
        PlanAcademico plan = planService.createPlan("Plan activo", 2026, "2026-2");
        Evidencia registro = planService.addRecord(nuevoRegistro(plan));
        planService.activatePlan(plan.getIdPlanAcademico());
        return registro;
    }

    private Evidencia nuevoRegistro(PlanAcademico plan) {
        Evidencia ev = new Evidencia();
        ev.setTitulo("Evidencia de prueba");
        ev.setPeriodo(plan.getPeriodoPlan());
        ev.setIdTipoEvidencia(TIPO_DISENO_PLANES);
        ev.setIdPlanAcademico(plan.getIdPlanAcademico());
        return ev;
    }

    private File crearPdf(String nombre) throws Exception {
        Path pdf = tempDir.resolve(nombre);
        Files.writeString(pdf, "contenido de prueba");
        return pdf.toFile();
    }
}
