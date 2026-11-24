package mx.unison.assistant.services;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import mx.unison.assistant.data.DatabaseConnection;
import mx.unison.assistant.models.Documento;
import mx.unison.assistant.models.Evidencia;
import mx.unison.assistant.models.PlanAcademico;
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

        catalogoService = new CatalogoService();
        planService = new PlanService();
        documentoService = new DocumentoService(
                new mx.unison.assistant.data.DocumentoDAO(),
                testAttachments.toString()
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

        // 3. Registrar evidencia con documento adjunto (RF-01 + RF-03)
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

        // 4. Verificar cálculo de resumen (RF-11) y alertas (RF-17)
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

        // 6. Eliminar evidencia (RF-05)
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
}