package mx.unison.assistant.service;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import mx.unison.assistant.data.AlertaDAO;
import mx.unison.assistant.data.DatabaseConnection;
import mx.unison.assistant.model.Documento;
import mx.unison.assistant.model.Evidencia;
import mx.unison.assistant.model.PlanAcademico;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Pruebas unitarias y de integración para validar la inicialización de SQLite3,
 * catálogos normativos PEDPD, CRUD de planes y evidencias, adjuntos con SHA-256
 * y el motor de alertas (RF-01 a RF-23).
 */
class BackendServicesTest {

    @TempDir
    Path tempDir;

    private CatalogoService catalogoService;
    private PlanService planService;
    private DocumentoService documentoService;
    private EvidenciaService evidenciaService;
    private AlertaService alertaService;

    @BeforeEach
    void setUp() {
        Path testDb = tempDir.resolve("test_asistente.db");
        Path testAttachments = tempDir.resolve("archivos_adjuntos");

        DatabaseConnection.setDbFilePath(testDb.toString());

        documentoService = new DocumentoService(
                new mx.unison.assistant.data.DocumentoDAO(),
                testAttachments.toString()
        );
        catalogoService = new CatalogoService(new mx.unison.assistant.data.CatalogoDAO(), documentoService);
        planService = new PlanService(
                new mx.unison.assistant.data.PlanAcademicoDAO(),
                new mx.unison.assistant.data.EvidenciaDAO(),
                documentoService
        );
        alertaService = new AlertaService();
        evidenciaService = new EvidenciaService(
                new mx.unison.assistant.data.EvidenciaDAO(),
                documentoService,
                alertaService
        );
    }

    @Test
    void testCatalogosInicializadosDesdeSql() throws Exception {
        String[] categorias = catalogoService.getCategories();
        assertEquals(5, categorias.length);
        assertEquals("Productividad Docente", categorias[0]);

        String[] tiposDocencia = catalogoService.getTypes("Productividad Docente");
        assertTrue(tiposDocencia.length > 0);

        String[] opciones = catalogoService.getOptions("Diseño de Planes de Estudio en sus diversos grados y modalidades.");
        assertEquals(3, opciones.length);
        assertEquals("Coordinador de la comisión responsable (Sólo un coordinador por Plan de Estudios) (80 pts)", opciones[0]);

        String[] estatus = catalogoService.getStatuses();
        assertArrayEquals(new String[] { "Pendiente", "En proceso", "Entregado" }, estatus);
    }

    @Test
    void testRegistroEdicionAdjuntoSha256YAlertas() throws Exception {
        // 1. Crear plan académico
        PlanAcademico plan = planService.createPlan("Plan 2026-2", "2026-2");
        assertTrue(plan.getIdPlanAcademico() > 0);

        // 2. Crear archivo temporal válido (.pdf)
        Path samplePdf = tempDir.resolve("constancia_oficial.pdf");
        Files.writeString(samplePdf, "Contenido simulado de constancia oficial UNISON");
        File pdfFile = samplePdf.toFile();

        // 3. Registrar evidencia con documento adjunto (RF-04 + RF-15)
        Evidencia ev = new Evidencia();
        ev.setTitulo("Diseño de Plan de Ingeniería de Software");
        ev.setDescripcion("Participación como coordinador de comisión");
        ev.setPeriodo("2026-2");
        ev.setFechaInicio("2026-01-15");
        ev.setFechaFinalizacion("2026-06-30");
        ev.setIdOpcion(1); // Coordinador (80 pts)
        ev.setIdEstatus(1); // Pendiente
        ev.setIdAcreditacion(1); // Pendiente
        ev.setIdPlanAcademico(plan.getIdPlanAcademico());

        Evidencia guardada = evidenciaService.registerEvidencia(ev, List.of(pdfFile));
        assertTrue(guardada.getIdEvidencia() > 0);
        assertEquals(1, guardada.getDocumentos().size());

        Documento doc = guardada.getDocumentos().get(0);
        assertEquals("constancia_oficial.pdf", doc.getNombreOriginal());
        assertEquals(64, doc.getSha256().length());
        assertTrue(doc.getRutaAlmacenamiento().contains("productividad_docente"));

        // 4. Verificar cálculo de resumen (RF-12) y alertas (RF-19)
        EvidenciaService.ResumenIndicadores resumen = evidenciaService.calculateSummary(
                evidenciaService.findForTracking()
        );
        assertEquals(1, resumen.total());
        assertEquals(0, resumen.entregadas());
        assertEquals(1, resumen.pendientes());

        List<String> alertas = alertaService.checkAlerts();
        assertFalse(alertas.isEmpty());

        // 5. Actualizar estatus a Entregado y verificar avance 100%
        guardada.setIdEstatus(3); // Entregado
        evidenciaService.updateEvidencia(guardada, List.of());

        EvidenciaService.ResumenIndicadores resumenFinal = evidenciaService.calculateSummary(
                evidenciaService.findForTracking()
        );
        assertEquals(1, resumenFinal.entregadas());
        assertEquals(100.0, resumenFinal.porcentajeAvance(), 0.01);

        // 6. Eliminar evidencia (RF-06)
        evidenciaService.deleteEvidencia(guardada.getIdEvidencia());
        assertTrue(evidenciaService.findForTracking().isEmpty());
    }

    @Test
    void testRechazaExtensionNoPermitida() throws Exception {
        Path exeFile = tempDir.resolve("virus.exe");
        Files.writeString(exeFile, "binario no permitido");

        Evidencia ev = new Evidencia();
        ev.setTitulo("Actividad con archivo inválido");
        ev.setPeriodo("2026-2");
        ev.setIdOpcion(1);

        assertThrows(Exception.class, () ->
                evidenciaService.registerEvidencia(ev, List.of(exeFile.toFile()))
        );
    }

    @Test
    void testEliminarPlanConEvidenciasYAdjuntos() throws Exception {
        PlanAcademico plan = planService.createPlan("Plan a borrar", "2026-2");
        Evidencia guardada = evidenciaService.registerEvidencia(
                nuevaEvidencia(plan.getIdPlanAcademico()), List.of(crearPdf("acta.pdf")));
        Path archivo = Path.of(guardada.getDocumentos().get(0).getRutaAlmacenamiento());
        assertTrue(Files.exists(archivo));

        planService.deletePlan(plan.getIdPlanAcademico()); // RF-09

        assertTrue(planService.getEvidenciasByPlan(plan.getIdPlanAcademico()).isEmpty());
        assertTrue(evidenciaService.findById(guardada.getIdEvidencia()).isEmpty());
        assertFalse(Files.exists(archivo));
    }

    @Test
    void testAdjuntoInvalidoNoDejaEvidenciaNiArchivos() throws Exception {
        PlanAcademico plan = planService.createPlan("Plan adjuntos", "2026-2");
        Path exe = tempDir.resolve("programa.exe");
        Files.writeString(exe, "no permitido");

        assertThrows(Exception.class, () -> evidenciaService.registerEvidencia(
                nuevaEvidencia(plan.getIdPlanAcademico()), List.of(crearPdf("bueno.pdf"), exe.toFile())));

        assertTrue(evidenciaService.findForTracking().isEmpty());
        assertFalse(Files.exists(tempDir.resolve("archivos_adjuntos")));
    }

    @Test
    void testCambiarPeriodoActualizaAnio() throws Exception {
        PlanAcademico plan = planService.createPlan("Plan anio", "2026-2");
        Evidencia guardada = evidenciaService.registerEvidencia(nuevaEvidencia(plan.getIdPlanAcademico()), List.of());
        assertEquals(2026, guardada.getAnio());

        guardada.setPeriodo("2027-1");
        evidenciaService.updateEvidencia(guardada, List.of());

        assertEquals(2027, evidenciaService.findById(guardada.getIdEvidencia()).get().getAnio());
    }

    @Test
    void testRestaurarCatalogoConservaEvidencias() throws Exception {
        PlanAcademico plan = planService.createPlan("Plan catalogo", "2026-2");
        evidenciaService.registerEvidencia(nuevaEvidencia(plan.getIdPlanAcademico()), List.of());

        catalogoService.restoreDefaultCatalog(); // RF-22

        assertEquals(5, catalogoService.getCategories().length);
        assertEquals(1, evidenciaService.findForTracking().size());
    }

    @Test
    void testRestablecerTodoBorraDatosYArchivos() throws Exception {
        PlanAcademico plan = planService.createPlan("Plan reset", "2026-2");
        evidenciaService.registerEvidencia(nuevaEvidencia(plan.getIdPlanAcademico()), List.of(crearPdf("oficio.pdf")));
        assertTrue(Files.exists(tempDir.resolve("archivos_adjuntos")));

        catalogoService.resetAllSystemData(); // RF-23

        assertTrue(evidenciaService.findForTracking().isEmpty());
        assertFalse(Files.exists(tempDir.resolve("archivos_adjuntos")));
        assertEquals(5, catalogoService.getCategories().length);
    }

    @Test
    void testCategoriaOtrosTieneTipoEvidencia() throws Exception {
        // RF-04: la categoría adicional "Otros" debe poder usarse
        assertEquals(1, catalogoService.getTypes("Otras evidencias").length);
    }

    @Test
    void testAlertasAnterioresSeConservanSiFallaElReemplazo() throws Exception {
        AlertaDAO alertaDAO = new AlertaDAO();
        alertaDAO.replaceAll(List.of("Alerta 1", "Alerta 2"));

        // ContenidoAlerta es NOT NULL: el segundo INSERT falla y se deshace todo el reemplazo
        assertThrows(SQLException.class, () -> alertaDAO.replaceAll(Arrays.asList("Alerta nueva", null)));

        assertEquals(2, alertaDAO.findAll().size());
    }

    private Evidencia nuevaEvidencia(int idPlan) {
        Evidencia ev = new Evidencia();
        ev.setTitulo("Evidencia de prueba");
        ev.setPeriodo("2026-2");
        ev.setIdOpcion(1);
        ev.setIdPlanAcademico(idPlan);
        return ev;
    }

    private File crearPdf(String nombre) throws Exception {
        Path pdf = tempDir.resolve(nombre);
        Files.writeString(pdf, "contenido de prueba");
        return pdf.toFile();
    }
}
