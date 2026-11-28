package mx.unison.assistant.service;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import mx.unison.assistant.data.AlertaDAO;
import mx.unison.assistant.data.CatalogoDAO;
import mx.unison.assistant.data.DatabaseConnection;
import mx.unison.assistant.data.DocumentoDAO;
import mx.unison.assistant.data.EvidenciaDAO;
import mx.unison.assistant.data.PlanAcademicoDAO;
import mx.unison.assistant.model.AlertaItem;
import mx.unison.assistant.model.Categoria;
import mx.unison.assistant.model.Documento;
import mx.unison.assistant.model.Estatus;
import mx.unison.assistant.model.Evidencia;
import mx.unison.assistant.model.Opcion;
import mx.unison.assistant.model.Periodo;
import mx.unison.assistant.model.PlanAcademico;
import mx.unison.assistant.model.RecordatorioItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.SQLiteErrorCode;
import org.sqlite.SQLiteException;

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
    private DocumentoService documentoService;

    @BeforeEach
    void setUp() {
        DatabaseConnection.setDbFilePath(tempDir.resolve("test_asistente.db").toString());

        documentoService = new DocumentoService(
                new DocumentoDAO(), tempDir.resolve("archivos_adjuntos").toString());
        EvidenciaDAO evidenciaDAO = new EvidenciaDAO();
        alertaService = new AlertaService(evidenciaDAO, new AlertaDAO());
        catalogoService = new CatalogoService(new CatalogoDAO());
        ajustesService = new AjustesService(documentoService);
        planService = new PlanService(new PlanAcademicoDAO(), evidenciaDAO, documentoService, alertaService);
        evidenciaService = new EvidenciaService(evidenciaDAO, documentoService, alertaService);
    }

    @AfterEach
    void tearDown() {
        DatabaseConnection.setDbFilePath(null);
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
        assertThrows(IllegalArgumentException.class, () -> planService.createPlan("Plan", 1899, "1899-1"));
        assertThrows(IllegalArgumentException.class, () -> planService.createPlan("Plan", 2201, "2201-1"));

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
        assertTrue(doc.getRutaAlmacenamiento().startsWith("archivos_adjuntos/"));
        assertFalse(Path.of(doc.getRutaAlmacenamiento()).isAbsolute());

        // RF-12
        EvidenciaService.ResumenIndicadores resumen = evidenciaService.countByStatus(evidenciaService.findForTracking());
        assertEquals(1, resumen.entregadas());
        assertEquals(100.0, resumen.porcentajeAvance(), 0.01);

        // RF-06: se borran el registro y su archivo
        Path archivo = documentoService.resolveFile(doc);
        assertTrue(Files.exists(archivo));
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
        Path archivo = documentoService.resolveFile(doc);
        assertTrue(Files.exists(archivo));

        evidenciaService.updateTracking(registro, null, List.of(doc));

        assertTrue(evidenciaService.findById(registro.getIdEvidencia()).get().getDocumentos().isEmpty());
        assertFalse(Files.exists(archivo));
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
    void testFalloAlAdjuntarRevierteCambiosDeSeguimientoYAdjuntosParciales() throws Exception {
        Evidencia registro = registroEnSeguimiento();
        evidenciaService.updateTracking(registro, List.of(crearPdf("anterior.pdf")), null);
        Evidencia estadoInicial = evidenciaService.findById(registro.getIdEvidencia()).get();
        Documento documentoAnterior = estadoInicial.getDocumentos().get(0);
        Path archivoAnterior = documentoService.resolveFile(documentoAnterior);
        Path carpetaCategoria = archivoAnterior.getParent();

        DocumentoService documentoServiceConFallo = new DocumentoService(
                new DocumentoDAO(), tempDir.resolve("archivos_adjuntos").toString()) {
            private int intentos = 0;

            @Override
            public Documento stageFile(File sourceFile, int idEvidencia, String categoryName)
                    throws java.io.IOException {
                intentos++;
                if (intentos == 2) {
                    throw new java.io.IOException("Error simulado al copiar segundo adjunto");
                }
                return super.stageFile(sourceFile, idEvidencia, categoryName);
            }
        };
        EvidenciaService servicioConFallo = new EvidenciaService(
                new EvidenciaDAO(), documentoServiceConFallo, alertaService);

        Evidencia editada = evidenciaService.findById(registro.getIdEvidencia()).get();
        editada.setTitulo("Título modificado que debe revertirse");
        editada.setIdEstatus(ESTATUS_ENTREGADO);
        editada.setIdOpcion(OPCION_COORDINADOR);

        assertThrows(java.io.IOException.class, () -> servicioConFallo.updateTracking(
                editada,
                List.of(crearPdf("primero.pdf"), crearPdf("segundo.pdf")),
                List.of(documentoAnterior)));

        Evidencia trasFallo = evidenciaService.findById(registro.getIdEvidencia()).get();
        assertEquals(estadoInicial.getTitulo(), trasFallo.getTitulo());
        assertEquals(estadoInicial.getIdEstatus(), trasFallo.getIdEstatus());
        assertEquals(estadoInicial.getIdOpcion(), trasFallo.getIdOpcion());
        assertEquals(estadoInicial.getFechaModificacion(), trasFallo.getFechaModificacion());
        assertEquals(1, trasFallo.getDocumentos().size());
        assertEquals("anterior.pdf", trasFallo.getDocumentos().get(0).getNombreOriginal());
        assertTrue(Files.exists(archivoAnterior));
        try (var archivosEnDisco = Files.list(carpetaCategoria)) {
            assertEquals(1, archivosEnDisco.count());
        }
    }

    @Test
    void testFalloSqlEnTransaccionDeSeguimientoRevierteTodoYPermiteReintentarSinDuplicar() throws Exception {
        Evidencia registro = registroEnSeguimiento();
        evidenciaService.updateTracking(registro, List.of(crearPdf("anterior.pdf")), null);
        Evidencia estadoInicial = evidenciaService.findById(registro.getIdEvidencia()).get();
        Documento documentoAnterior = estadoInicial.getDocumentos().get(0);
        Path archivoAnterior = documentoService.resolveFile(documentoAnterior);
        Path carpetaCategoria = archivoAnterior.getParent();
        File archivoNuevo = crearPdf("nuevo.pdf");

        Evidencia conOpcionInvalida = evidenciaService.findById(registro.getIdEvidencia()).get();
        conOpcionInvalida.setTitulo("Cambio rechazado por SQL");
        conOpcionInvalida.setIdOpcion(OPCION_DE_OTRO_TIPO);

        assertThrows(SQLException.class, () -> evidenciaService.updateTracking(
                conOpcionInvalida,
                List.of(archivoNuevo),
                List.of(documentoAnterior)));

        Evidencia trasFallo = evidenciaService.findById(registro.getIdEvidencia()).get();
        assertEquals(estadoInicial.getTitulo(), trasFallo.getTitulo());
        assertEquals(estadoInicial.getFechaModificacion(), trasFallo.getFechaModificacion());
        assertEquals(1, trasFallo.getDocumentos().size());
        assertTrue(Files.exists(archivoAnterior));
        try (var archivosEnDisco = Files.list(carpetaCategoria)) {
            assertEquals(1, archivosEnDisco.count());
        }

        // Reintentar con la opción válida deja exactamente el archivo nuevo sin duplicados
        conOpcionInvalida.setIdOpcion(OPCION_COORDINADOR);
        evidenciaService.updateTracking(conOpcionInvalida, List.of(archivoNuevo), List.of(documentoAnterior));

        Evidencia trasReintento = evidenciaService.findById(registro.getIdEvidencia()).get();
        assertEquals("Cambio rechazado por SQL", trasReintento.getTitulo());
        assertEquals(1, trasReintento.getDocumentos().size());
        assertEquals("nuevo.pdf", trasReintento.getDocumentos().get(0).getNombreOriginal());
        assertFalse(Files.exists(archivoAnterior));
        try (var archivosEnDisco = Files.list(carpetaCategoria)) {
            assertEquals(1, archivosEnDisco.count());
        }
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
        Path archivo = documentoService.resolveFile(evidenciaService.findById(registro.getIdEvidencia()).get()
                .getDocumentos().get(0));
        assertTrue(Files.exists(archivo));

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
    void testAlertasIndividualesIncluyenEvidenciaYEstadoDelPlan() throws Exception {
        PlanAcademico planBorrador = planService.createPlan("Borrador", 2026, "2026-2");
        Evidencia evidenciaBorrador = planService.addRecord(nuevoRegistro(planBorrador));

        Evidencia evidenciaEntregadaSinArchivo = registroEnSeguimiento();
        evidenciaEntregadaSinArchivo.setIdEstatus(ESTATUS_ENTREGADO);
        evidenciaService.updateTracking(evidenciaEntregadaSinArchivo, null, null);

        Evidencia evidenciaSoloDocx = registroEnSeguimiento();
        evidenciaSoloDocx.setIdEstatus(ESTATUS_ENTREGADO);
        Path archivoDocx = tempDir.resolve("constancia.docx");
        Files.writeString(archivoDocx, "documento word");
        evidenciaService.updateTracking(evidenciaSoloDocx, List.of(archivoDocx.toFile()), null);

        Evidencia evidenciaCompleta = registroEnSeguimiento();
        evidenciaCompleta.setIdEstatus(ESTATUS_ENTREGADO);
        evidenciaService.updateTracking(evidenciaCompleta, List.of(crearPdf("completa.pdf")), null);

        List<AlertaItem> items = alertaService.getAlertItems("2026-2");

        assertTrue(items.stream().anyMatch(item ->
                item.idEvidencia() == evidenciaBorrador.getIdEvidencia()
                        && item.idPlanAcademico() == planBorrador.getIdPlanAcademico()
                        && !item.planActivo()
                        && item.regla() == AlertaItem.Regla.SIN_DOCUMENTOS
                        && item.clave().equals(evidenciaBorrador.getIdEvidencia() + ":SIN_DOCUMENTOS")
                        && item.mensaje().contains("Sin documento")));
        assertTrue(items.stream().anyMatch(item ->
                item.idEvidencia() == evidenciaBorrador.getIdEvidencia()
                        && item.regla() == AlertaItem.Regla.PENDIENTE_EN_PROCESO
                        && item.mensaje().contains("Pendiente")));
        assertTrue(items.stream().anyMatch(item ->
                item.idEvidencia() == evidenciaEntregadaSinArchivo.getIdEvidencia()
                        && item.planActivo()
                        && item.regla() == AlertaItem.Regla.ENTREGADO_SIN_DOC
                        && item.clave().equals(evidenciaEntregadaSinArchivo.getIdEvidencia() + ":ENTREGADO_SIN_DOC")
                        && item.mensaje().contains("Entregado")));
        assertTrue(items.stream().anyMatch(item ->
                item.idEvidencia() == evidenciaSoloDocx.getIdEvidencia()
                        && item.planActivo()
                        && item.regla() == AlertaItem.Regla.FORMATO_NO_PEDPD
                        && item.mensaje().contains(".pdf o .jpg")));
        assertTrue(items.stream().noneMatch(item ->
                item.idEvidencia() == evidenciaCompleta.getIdEvidencia()));

        // checkAlerts() y refreshAlertItems() derivan de getAlertItems() y sincronizan la tabla Alertas
        List<String> resumenAlertas = alertaService.checkAlerts("2026-2");
        assertTrue(resumenAlertas.stream().anyMatch(msg -> msg.contains("1 registro(s) sin documento")));
        assertTrue(resumenAlertas.stream().anyMatch(msg -> msg.contains("1 evidencia(s) sin archivo prioritario")));
        assertTrue(resumenAlertas.stream().anyMatch(msg -> msg.contains("1 evidencia(s) con estatus \"Entregado\" sin documentos")));
        assertTrue(resumenAlertas.stream().anyMatch(msg -> msg.contains("1 evidencia(s) del periodo actual (2026-2)")));

        new AlertaDAO().replaceAll(List.of());
        List<AlertaItem> refreshedItems = alertaService.refreshAlertItems("2026-2");
        assertEquals(items.size(), refreshedItems.size());
        assertEquals(resumenAlertas.size(), alertaService.getStoredAlerts().size());
    }

    @Test
    void testSeguimientoValidaFormatoDeFechas() throws Exception {
        Evidencia registro = registroEnSeguimiento();

        registro.setFechaInicio("2026-11-31");
        registro.setFechaFinalizacion(null);
        assertThrows(IllegalArgumentException.class, () -> evidenciaService.updateTracking(registro, null, null));

        registro.setFechaInicio("2026-09-01");
        registro.setFechaFinalizacion("15/09/2026");
        assertThrows(IllegalArgumentException.class, () -> evidenciaService.updateTracking(registro, null, null));

        registro.setFechaInicio("2026-09-10");
        registro.setFechaFinalizacion("2026-09-01");
        assertThrows(IllegalArgumentException.class, () -> evidenciaService.updateTracking(registro, null, null));

        registro.setFechaInicio(" 2026-09-01 ");
        registro.setFechaFinalizacion(" 2026-09-10 ");
        assertDoesNotThrow(() -> evidenciaService.updateTracking(registro, null, null));
        Evidencia guardada = evidenciaService.findById(registro.getIdEvidencia()).orElseThrow();
        assertEquals("2026-09-01", guardada.getFechaInicio());
        assertEquals("2026-09-10", guardada.getFechaFinalizacion());
    }

    @Test
    void testMarcarAlertaComoLeidaOcultaEnSesionSinBorrarResumen() throws Exception {
        PlanAcademico plan = planService.createPlan("Plan alertas leidas", 2026, "2026-2");
        planService.addRecord(nuevoRegistro(plan));

        List<AlertaItem> todas = alertaService.refreshAlertItems("2026-2");
        assertEquals(2, todas.size());

        String clavePrimera = todas.get(0).clave();
        alertaService.markAlertAsRead(clavePrimera);

        List<AlertaItem> noLeidas = alertaService.refreshAlertItems("2026-2");
        assertEquals(1, noLeidas.size());
        assertNotEquals(clavePrimera, noLeidas.get(0).clave());
        assertEquals(2, alertaService.getAlertItems("2026-2").size());
        assertEquals(2, alertaService.getStoredAlerts().size());

        // Una nueva instancia simula un reinicio de la app: las pendientes vuelven a mostrarse
        AlertaService nuevaSesion = new AlertaService(new EvidenciaDAO(), new AlertaDAO());
        assertEquals(2, nuevaSesion.refreshAlertItems("2026-2").size());
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
        DatabaseConnection.createBackup();
        assertTrue(Files.exists(tempDir.resolve("archivos_adjuntos")));

        ajustesService.deleteAll(); // RF-23

        assertTrue(planService.listPlans().isEmpty()); // no se crea ningún plan por defecto
        assertTrue(evidenciaService.findForTracking().isEmpty());
        assertFalse(Files.exists(tempDir.resolve("archivos_adjuntos")));
        assertTrue(DatabaseConnection.listBackups().isEmpty());
        assertEquals(5, catalogoService.getCategories().size());
    }

    @Test
    void testRespaldoIncluyeCambiosConfirmadosEnWalYConservaTresCopias() throws Exception {
        Path primeraCopia;
        try (Connection conexionAbierta = DatabaseConnection.getConnection()) {
            registroEnSeguimiento();
            registroEnSeguimiento();
            assertEquals(2, contarEvidencias(conexionAbierta));
            assertTrue(Files.size(Path.of(DatabaseConnection.getDbFilePath() + "-wal")) > 0);

            primeraCopia = DatabaseConnection.createBackup();
            assertEquals(tempDir.resolve("backups"), primeraCopia.getParent());
            try (Connection conexionRespaldo = DriverManager.getConnection("jdbc:sqlite:" + primeraCopia)) {
                assertEquals(2, contarEvidencias(conexionRespaldo));
            }
        }
        Path segundaCopia = DatabaseConnection.createBackup();
        Path terceraCopia = DatabaseConnection.createBackup();
        Path cuartaCopia = DatabaseConnection.createBackup();

        assertEquals(List.of(cuartaCopia, terceraCopia, segundaCopia), DatabaseConnection.listBackups());
        assertFalse(Files.exists(primeraCopia));
    }

    @Test
    void testRestaurarIgnoraRespaldoDanadoYResguardaArchivosAnteriores() throws Exception {
        registroEnSeguimiento();
        Path respaldoValido = DatabaseConnection.createBackup();
        Path respaldoDanado = respaldoValido.resolveSibling("base_datos-99991231_235959_999.bak");
        Files.writeString(respaldoDanado, "Esto no es una base SQLite");
        registroEnSeguimiento();
        assertEquals(2, evidenciaService.findForTracking().size());

        Path archivoWal = Path.of(DatabaseConnection.getDbFilePath() + "-wal");
        Path archivoShm = Path.of(DatabaseConnection.getDbFilePath() + "-shm");
        Files.writeString(archivoWal, "WAL anterior");
        Files.writeString(archivoShm, "SHM anterior");

        assertEquals(respaldoDanado, DatabaseConnection.listBackups().getFirst());
        assertTrue(DatabaseConnection.restoreLatestBackup());
        assertFalse(Files.exists(archivoWal));
        assertFalse(Files.exists(archivoShm));
        assertEquals(DatabaseConnection.HealthStatus.OK, DatabaseConnection.checkHealth().status());
        assertEquals(1, evidenciaService.findForTracking().size());
        try (var archivosEnTemp = Files.list(tempDir)) {
            assertTrue(archivosEnTemp.anyMatch(path ->
                    path.getFileName().toString().startsWith("test_asistente-corrupt-")
                            && path.getFileName().toString().endsWith(".db")));
        }
    }

    @Test
    void testBaseSqliteSinTablasYSinRespaldosSeInicializaLimpia() throws Exception {
        Path rutaBd = Path.of(DatabaseConnection.getDbFilePath());
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + rutaBd);
             Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");
        }
        assertTrue(Files.size(rutaBd) > 0);

        assertEquals(DatabaseConnection.HealthStatus.OK, DatabaseConnection.checkHealth().status());
        assertEquals(5, catalogoService.getCategories().size());
    }

    @Test
    void testBaseQueNoEsSqliteSeClasificaComoDanada() throws Exception {
        Files.writeString(Path.of(DatabaseConnection.getDbFilePath()), "Esto no es una base SQLite");

        DatabaseConnection.HealthResult resultadoSalud = DatabaseConnection.checkHealth();

        assertEquals(DatabaseConnection.HealthStatus.CORRUPT, resultadoSalud.status());
        SQLiteException errorSqlite = assertInstanceOf(SQLiteException.class, resultadoSalud.cause());
        assertEquals(SQLiteErrorCode.SQLITE_NOTADB, errorSqlite.getResultCode());
    }

    @Test
    void testBaseCreaCarpetasParaUnaRutaPersonalizada() {
        Path rutaBaseDatos = tempDir.resolve("datos/subcarpeta/base.db");
        DatabaseConnection.setDbFilePath(rutaBaseDatos.toString());

        assertEquals(DatabaseConnection.HealthStatus.OK, DatabaseConnection.checkHealth().status());
        assertTrue(Files.isRegularFile(rutaBaseDatos));
    }

    @Test
    void testResuelveAdjuntosConRutasRelativasYAbsolutas() {
        Path rutaEsperada = tempDir.resolve("archivos_adjuntos/productividad_docente/acta.pdf");
        Documento adjunto = new Documento();
        for (String rutaGuardada : List.of("archivos_adjuntos/productividad_docente/acta.pdf",
                "productividad_docente/acta.pdf", rutaEsperada.toString())) {
            adjunto.setRutaAlmacenamiento(rutaGuardada);
            assertEquals(rutaEsperada, documentoService.resolveFile(adjunto));
        }
    }

    @Test
    void testRecordatorioDeInicioCuentaPendientesDelPeriodoAnteriorDesdeAgosto() throws Exception {
        agregarEvidencia("2026-1", true, 1, null);
        agregarEvidencia("2026-1", true, 1, null);
        agregarEvidencia("2026-1", true, ESTATUS_ENTREGADO, null);
        agregarEvidencia("2025-2", true, 1, null);

        assertTrue(alertaService.getStartupReminderItems(LocalDate.of(2026, 7, 15)).isEmpty());

        List<RecordatorioItem> items = alertaService.getStartupReminderItems(LocalDate.of(2026, 8, 1));
        assertEquals(1, items.size());
        assertEquals("Inicio de periodo", items.get(0).etiqueta());
        assertEquals(2, items.get(0).evidencias().size());
        assertTrue(items.get(0).detalle().contains("2 evidencia(s) pendiente(s) del periodo 2026-1"));
    }

    @Test
    void testRecordatorioDeMitadDePeriodo() throws Exception {
        List<RecordatorioItem> items = alertaService.getStartupReminderItems(LocalDate.of(2026, 10, 1));
        assertEquals(1, items.size());
        assertEquals("Mitad de periodo", items.get(0).etiqueta());
        assertTrue(items.get(0).detalle().contains("A mitad del periodo 2026-2"));
    }

    @Test
    void testRecordatorioDeCierreDePeriodo() throws Exception {
        List<RecordatorioItem> items = alertaService.getStartupReminderItems(LocalDate.of(2026, 11, 25));
        assertEquals(1, items.size());
        assertEquals("Cierre de periodo", items.get(0).etiqueta());
        assertTrue(items.get(0).detalle().contains("31/12/2026"));
    }

    @Test
    void testRecordatorioDeVencimientoCuentaSoloPendientesDeHoyYManana() throws Exception {
        agregarEvidencia("2026-1", true, 1, "2026-04-15");
        agregarEvidencia("2026-1", true, 1, "2026-04-16");
        agregarEvidencia("2026-1", true, 1, "2026-04-14");
        agregarEvidencia("2026-1", true, 1, "2026-04-17");
        agregarEvidencia("2026-1", true, ESTATUS_ENTREGADO, "2026-04-15");
        AlertaDAO alertaDAO = new AlertaDAO();
        alertaDAO.replaceAll(List.of("Alerta guardada"));

        List<RecordatorioItem> items = alertaService.getStartupReminderItems(LocalDate.of(2026, 4, 15));
        assertEquals(1, items.size());
        assertEquals("Vence pronto", items.get(0).etiqueta());
        assertTrue(items.get(0).urgente());
        assertEquals(2, items.get(0).evidencias().size());
        assertEquals("Alerta guardada", alertaDAO.findAll().getFirst().getContenidoAlerta());
    }

    @Test
    void testRecordatoriosEstructuradosIncluyenEvidenciasClickeables() throws Exception {
        agregarEvidencia("2026-2", true, 1, "2026-11-26");
        agregarEvidencia("2026-2", true, ESTATUS_ENTREGADO, "2026-11-26");

        List<RecordatorioItem> items = alertaService.getStartupReminderItems(LocalDate.of(2026, 11, 26));

        assertEquals(2, items.size());
        assertEquals("Vence pronto", items.get(0).etiqueta());
        assertTrue(items.get(0).urgente());
        assertEquals(1, items.get(0).evidencias().size());
        assertEquals("Cierre de periodo", items.get(1).etiqueta());
        assertEquals(1, items.get(1).evidencias().size());
        assertTrue(items.get(1).detalle().contains("31/12/2026"));
    }

    @Test
    void testRecordatoriosIgnoranBorradoresYFechasFaltantes() throws Exception {
        agregarEvidencia("2026-1", false, 1, "2026-08-01");
        agregarEvidencia("2026-2", true, 1, null);
        agregarEvidencia("2026-2", true, 1, " ");

        assertTrue(alertaService.getStartupReminderItems(LocalDate.of(2026, 8, 1)).isEmpty());
        assertTrue(alertaService.getStartupReminderItems(LocalDate.of(2026, 7, 15)).isEmpty());
    }

    @Test
    void testBaseDeCeroBytesConRespaldoOfreceRecuperarSinBorrarRespaldos() throws Exception {
        registroEnSeguimiento();
        DatabaseConnection.createBackup();
        Path archivoBase = vaciarArchivoDeBase();

        DatabaseConnection.HealthResult resultadoSalud = DatabaseConnection.checkHealth();

        assertEquals(DatabaseConnection.HealthStatus.CORRUPT, resultadoSalud.status());
        assertEquals(0, Files.size(archivoBase));
        assertEquals(1, DatabaseConnection.listBackups().size());

        assertTrue(DatabaseConnection.restoreLatestBackup());
        assertEquals(DatabaseConnection.HealthStatus.OK, DatabaseConnection.checkHealth().status());
        assertEquals(1, evidenciaService.findForTracking().size());
    }

    @Test
    void testBaseDeCeroBytesSinRespaldoSeCreaDeNuevo() throws Exception {
        vaciarArchivoDeBase();

        assertEquals(DatabaseConnection.HealthStatus.OK, DatabaseConnection.checkHealth().status());
        assertEquals(5, catalogoService.getCategories().size());
    }

    @Test
    void testRestaurarOmiteRespaldosIncompletosOConLlavesRotas() throws Exception {
        registroEnSeguimiento();
        Path respaldoValido = DatabaseConnection.createBackup();

        Path respaldoSinAlertas = respaldoValido.resolveSibling("base_datos-99991231_235959_998.bak");
        Files.copy(respaldoValido, respaldoSinAlertas);
        ejecutarEnArchivo(respaldoSinAlertas, "DROP TABLE Alertas");

        Path respaldoConLlavesRotas = respaldoValido.resolveSibling("base_datos-99991231_235959_999.bak");
        Files.copy(respaldoValido, respaldoConLlavesRotas);
        ejecutarEnArchivo(respaldoConLlavesRotas, "UPDATE Evidencia SET IdPlanAcademico = 9999");

        registroEnSeguimiento();
        assertEquals(2, evidenciaService.findForTracking().size());
        assertEquals(respaldoConLlavesRotas, DatabaseConnection.listBackups().getFirst());

        assertTrue(DatabaseConnection.restoreLatestBackup());

        assertEquals(DatabaseConnection.HealthStatus.OK, DatabaseConnection.checkHealth().status());
        assertEquals(1, evidenciaService.findForTracking().size());
        assertEquals(1, planService.listPlans().size());
    }

    @Test
    void testBaseSinTablaRequeridaSeClasificaComoDanada() throws Exception {
        assertEquals(DatabaseConnection.HealthStatus.OK, DatabaseConnection.checkHealth().status());
        ejecutarEnArchivo(Path.of(DatabaseConnection.getDbFilePath()), "DROP TABLE Alertas");

        assertEquals(DatabaseConnection.HealthStatus.CORRUPT, DatabaseConnection.checkHealth().status());
    }

    @Test
    void testBaseConLlavesRotasSeClasificaComoDanada() throws Exception {
        registroEnSeguimiento();
        ejecutarEnArchivo(Path.of(DatabaseConnection.getDbFilePath()), "UPDATE Evidencia SET IdPlanAcademico = 9999");

        assertEquals(DatabaseConnection.HealthStatus.CORRUPT, DatabaseConnection.checkHealth().status());
    }

    @Test
    void testRestablecerTodoConAdjuntoAbiertoLimpiaLaBaseYAvisa() throws Exception {
        // Windows no deja borrar un archivo abierto; en otros sistemas el borrado sí se puede
        Assumptions.assumeTrue(System.getProperty("os.name").toLowerCase().contains("win"));
        Evidencia registro = registroEnSeguimiento();
        evidenciaService.updateTracking(registro, List.of(crearPdf("abierto.pdf")), null);
        Documento adjunto = evidenciaService.findById(registro.getIdEvidencia()).orElseThrow().getDocumentos().get(0);
        DatabaseConnection.createBackup();

        try (FileInputStream archivoAbierto = new FileInputStream(documentoService.resolveFile(adjunto).toFile())) {
            assertThrows(IOException.class, () -> ajustesService.deleteAll());
        }

        assertTrue(planService.listPlans().isEmpty());
        assertTrue(evidenciaService.findForTracking().isEmpty());
        assertTrue(DatabaseConnection.listBackups().isEmpty());
    }

    @Test
    void testCambiosGuardadosNoFallanSiNoSePuedenActualizarLasAlertas() throws Exception {
        PlanAcademico plan = planService.createPlan("Plan sin alertas", 2026, "2026-2");
        ejecutarEnArchivo(Path.of(DatabaseConnection.getDbFilePath()), "DROP TABLE Alertas");

        Evidencia registro = planService.addRecord(nuevoRegistro(plan));
        assertEquals(1, planService.getRecords(plan.getIdPlanAcademico()).size());

        registro.setTitulo("Título editado");
        assertDoesNotThrow(() -> planService.updateRecord(registro));
        assertEquals("Título editado", evidenciaService.findById(registro.getIdEvidencia()).orElseThrow().getTitulo());

        assertEquals(0, planService.deleteRecord(registro.getIdEvidencia()));
        assertTrue(planService.getRecords(plan.getIdPlanAcademico()).isEmpty());

        assertEquals(0, planService.deletePlan(plan.getIdPlanAcademico()));
        assertTrue(planService.listPlans().isEmpty());
    }

    @Test
    void testRecordatorioDeVencimientoUsaCierreDelPeriodoSinFechaFinal() throws Exception {
        agregarEvidencia("2026-2", true, 1, null);
        agregarEvidencia("2026-2", true, ESTATUS_ENTREGADO, null);
        agregarEvidencia("2026-1", true, 1, " ");

        List<RecordatorioItem> items = alertaService.getStartupReminderItems(LocalDate.of(2026, 12, 30));

        assertEquals(1, items.size());
        assertEquals("Vence pronto", items.get(0).etiqueta());
        assertEquals(1, items.get(0).evidencias().size());
        assertEquals("2026-2", items.get(0).evidencias().get(0).getPeriodo());
    }

    @Test
    void testAdjuntoQueNoSePuedeBorrarSeReportaAlEliminarRegistroYPlan() throws Exception {
        Evidencia registro = registroEnSeguimiento();
        evidenciaService.updateTracking(registro, List.of(crearPdf("bloqueado.pdf")), null);
        Path archivo = bloquearAdjunto(registro);

        assertEquals(1, planService.deleteRecord(registro.getIdEvidencia()));
        assertTrue(evidenciaService.findById(registro.getIdEvidencia()).isEmpty());
        assertTrue(Files.exists(archivo));

        Evidencia otroRegistro = registroEnSeguimiento();
        evidenciaService.updateTracking(otroRegistro, List.of(crearPdf("bloqueado2.pdf")), null);
        bloquearAdjunto(otroRegistro);

        assertEquals(1, planService.deletePlan(otroRegistro.getIdPlanAcademico()));
        assertTrue(planService.getRecords(otroRegistro.getIdPlanAcademico()).isEmpty());
    }

    // Cambia el adjunto por una carpeta con contenido para provocar fallo de borrado sin depender del SO
    private Path bloquearAdjunto(Evidencia registro) throws Exception {
        Documento adjunto = evidenciaService.findById(registro.getIdEvidencia()).orElseThrow().getDocumentos().get(0);
        Path archivo = documentoService.resolveFile(adjunto);
        Files.delete(archivo);
        Files.createDirectories(archivo);
        Files.writeString(archivo.resolve("contenido.txt"), "no se puede borrar");
        return archivo;
    }

    private Path vaciarArchivoDeBase() throws Exception {
        Path archivoBase = Path.of(DatabaseConnection.getDbFilePath());
        DatabaseConnection.setDbFilePath(archivoBase.toString());
        Files.deleteIfExists(Path.of(archivoBase + "-wal"));
        Files.deleteIfExists(Path.of(archivoBase + "-shm"));
        Files.write(archivoBase, new byte[0]);
        return archivoBase;
    }

    private void ejecutarEnArchivo(Path archivoBase, String sql) throws SQLException {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + archivoBase);
             Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = OFF");
            stmt.execute(sql);
        }
    }

    private void agregarEvidencia(String periodo, boolean activo, int estatus, String fechaFinalizacion)
            throws SQLException {
        PlanAcademico plan = planService.createPlan("Plan " + periodo,
                Periodo.yearOf(periodo), periodo);
        Evidencia evidencia = nuevoRegistro(plan);
        evidencia.setAnio(plan.getAnioPlan());
        evidencia.setIdEstatus(estatus);
        evidencia.setFechaFinalizacion(fechaFinalizacion);
        new EvidenciaDAO().insert(evidencia);
        if (activo) {
            planService.activatePlan(plan.getIdPlanAcademico());
        }
    }

    private int contarEvidencias(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet resultado = stmt.executeQuery("SELECT COUNT(*) FROM Evidencia")) {
            assertTrue(resultado.next());
            return resultado.getInt(1);
        }
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
