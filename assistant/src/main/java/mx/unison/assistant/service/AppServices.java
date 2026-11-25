package mx.unison.assistant.service;

import mx.unison.assistant.data.AlertaDAO;
import mx.unison.assistant.data.CatalogoDAO;
import mx.unison.assistant.data.EvidenciaDAO;
import mx.unison.assistant.data.PlanAcademicoDAO;

// Main crea uno solo para que todas las pantallas compartan las mismas alertas
public class AppServices {

    public final CatalogoService catalogo;
    public final PlanService planes;
    public final EvidenciaService evidencias;
    public final DocumentoService documentos;
    public final AlertaService alertas;
    public final AjustesService ajustes;

    public AppServices() {
        EvidenciaDAO evidenciaDAO = new EvidenciaDAO();
        documentos = new DocumentoService();
        alertas = new AlertaService(evidenciaDAO, new AlertaDAO());

        catalogo = new CatalogoService(new CatalogoDAO());
        planes = new PlanService(new PlanAcademicoDAO(), evidenciaDAO, documentos, alertas);
        evidencias = new EvidenciaService(evidenciaDAO, documentos, alertas);
        ajustes = new AjustesService(documentos);
    }
}
