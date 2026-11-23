package mx.unison.assistant.ui.common;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;

// TEMPORAL. Datos de ejemplo para que la UI funcione mientras no existen model, data y service.
// Es el único lugar de la UI con datos de ejemplo.
// Cada método se llama igual que el método del service que lo va a reemplazar, según el diagrama de clases del SDD.
// Para conectar una pantalla, se cambia SampleData.x() por el método del service que indica su comentario.
// Cuando ninguna clase use SampleData, se borra este archivo.
// El catálogo de esta clase es de ejemplo. No es el catálogo del Reglamento PEDPD.
public class SampleData {

    // Catálogo. Lo reemplaza CatalogoService

    // Categorías del Apéndice B del SRS y la categoría "Otros" de RF-04
    static final String[] CATEGORIES = {
        "Productividad docente",
        "Investigación",
        "Tutoría",
        "Cuerpos colegiados",
        "Otros"
    };

    // { categoría, tipo de actividad, definición, acreditación }
    static final String[][] TYPES = {
        {
            "Productividad docente",
            "1.1.1.1 Diseño de Planes de Estudio",
            "Elaboración de un nuevo plan de estudios para ofertar como propuesta formativa. "
                + "Los componentes de un plan de estudios se elaboran de acuerdo con los criterios técnicos establecidos conforme a la normatividad institucional vigente.",
            "1) La Secretaría General Administrativa acredita mediante el acta de aprobación emitida por el Colegio Universitario.\n"
                + "2) El Colegio Departamental acreditará la participación en actividades específicas mediante constancia avalada."
        },
        {
            "Productividad docente",
            "1.1.1.14 Impartición de cursos/diplomados",
            "",
            "Presentar la constancia oficial emitida por la instancia institucional correspondiente."
        },
        {
            "Investigación",
            "1.2.4.1 Artículos en revistas de catálogo",
            "",
            "Presentar el artículo publicado o la carta de aceptación de la revista."
        },
        {
            "Investigación",
            "1.2.6.1 Responsable o colaborador de proyecto",
            "",
            "Presentar la constancia de registro del proyecto."
        },
        {
            "Tutoría",
            "1.3.5.1 Dirección de tesis",
            "",
            "1) La Dirección de Servicios Escolares acredita la actividad a través del registro en el SIIA del acta de examen profesional o de grado (artículo 28 del Reglamento de PEDPD).\n"
                + "2) Cuando con una misma opción de titulación se titulan 2 o más estudiantes, sólo cuenta como una actividad."
        },
        {
            "Cuerpos colegiados",
            "Comisiones internas de acreditación",
            "",
            "Presentar el nombramiento o constancia de la comisión."
        },
        {
            "Otros",
            "3.1 Antigüedad y Permanencia",
            "",
            ""
        }
    };

    // { tipo de actividad, opción, puntaje }
    static final String[][] OPTIONS = {
        { "1.1.1.1 Diseño de Planes de Estudio", "Coordinador de la comisión", "50" },
        { "1.1.1.1 Diseño de Planes de Estudio", "Participante / Colaborador", "30" },
        { "1.2.4.1 Artículos en revistas de catálogo", "Revista internacional", "30" },
        { "1.2.4.1 Artículos en revistas de catálogo", "Revista nacional", "20" },
        { "1.3.5.1 Dirección de tesis", "Titular de tesis de maestría", "30" },
        { "1.3.5.1 Dirección de tesis", "Titular de tesis de licenciatura", "20" }
    };

    // Se reemplaza por CatalogoService.getCategories()
    public static String[] getCategories() {
        return CATEGORIES;
    }

    // Se reemplaza por CatalogoService.getTypes(categoría)
    public static String[] getTypes(String category) {
        List<String> result = new ArrayList<>();
        for (String[] type : TYPES) {
            if (type[0].equals(category)) {
                result.add(type[1]);
            }
        }
        return result.toArray(new String[0]);
    }

    // Se reemplaza por un CatalogoService.getTypes() sin filtro. Lo usa el filtro "Tipo de evidencia" de RF-11
    public static String[] getAllTypes() {
        String[] result = new String[TYPES.length];
        for (int i = 0; i < TYPES.length; i++) {
            result[i] = TYPES[i][1];
        }
        return result;
    }

    // Se reemplaza por CatalogoService.getOptions(tipo). Regresa el texto del combo, por ejemplo "Opción (N pts)"
    public static String[] getOptions(String type) {
        List<String> result = new ArrayList<>();
        for (String[] option : OPTIONS) {
            if (option[0].equals(type)) {
                result.add(option[1] + " (" + option[2] + " pts)");
            }
        }
        return result.toArray(new String[0]);
    }

    // Se reemplaza por EvidenceType.getDefinition()
    public static String getDefinition(String type) {
        String[] row = findType(type);
        if (row == null) {
            return "";
        }
        return row[2];
    }

    // Se reemplaza por EvidenceType.getAccreditationContent()
    public static String getAccreditation(String type) {
        String[] row = findType(type);
        if (row == null) {
            return "";
        }
        return row[3];
    }

    private static String[] findType(String type) {
        for (String[] row : TYPES) {
            if (row[1].equals(type)) {
                return row;
            }
        }
        return null;
    }

    // Valores fijos del SRS (RF-13). Los reemplazan los enums Status y Accreditation del model

    // Se reemplaza por Status.values()
    public static String[] getStatuses() {
        return new String[] { "Pendiente", "En proceso", "Entregado" };
    }

    // Se reemplaza por Accreditation.values()
    public static String[] getAccreditations() {
        return new String[] { "En revisión", "Aceptado", "Publicado" };
    }

    // Años y periodos. Los reemplaza la clase Period del model

    // Año actual y los dos anteriores, del más reciente al más viejo
    public static String[] getYears() {
        int current = Year.now().getValue();
        return new String[] {
            String.valueOf(current),
            String.valueOf(current - 1),
            String.valueOf(current - 2)
        };
    }

    // Periodos del año con el formato YYYY-1 y YYYY-2 de RF-01
    public static String[] getPeriods(String year) {
        return new String[] { year + "-1", year + "-2" };
    }

    // Todos los periodos de getYears(), para los combos de "Periodo"
    public static String[] getAllPeriods() {
        List<String> result = new ArrayList<>();
        for (String year : getYears()) {
            for (String period : getPeriods(year)) {
                result.add(period);
            }
        }
        return result.toArray(new String[0]);
    }

    // Planes académicos. Los reemplaza PlanService

    // Se reemplaza por PlanService.listPlans(). Cada fila es { nombre, periodo }
    public static List<String[]> listPlans() {
        List<String[]> plans = new ArrayList<>();
        plans.add(new String[] { "Plan #1", "2026-1" });
        plans.add(new String[] { "Plan #2 (Borrador)", "2026-2" });
        return plans;
    }

    // Se reemplaza por PlanService.getRecords(plan). Cada fila es { título, categoría, tipo de actividad, periodo, descripción }
    public static List<String[]> getRecords(String planName) {
        List<String[]> records = new ArrayList<>();
        if (planName.equals("Plan #1")) {
            records.add(new String[] { "MIEMBRO DE LA COMISIÓN RESPONSABLE", "Productividad docente", "1.1.1.1 Diseño de Planes de Estudio", "2026-1", "Diseño y actualización de plan de estudios" });
            records.add(new String[] { "TALLER ACADÉMICO", "Productividad docente", "1.1.1.14 Impartición de cursos/diplomados", "2026-1", "Taller de mejora de procesos para licenciatura" });
            records.add(new String[] { "Participación en Artículo", "Investigación", "1.2.4.1 Artículos en revistas de catálogo", "2026-1", "Artículo de proyectos de mejora en la industria de alimentos" });
            records.add(new String[] { "Dirección de Tesis de Maestría", "Tutoría", "1.3.5.1 Dirección de tesis", "2026-1", "Seguimiento de tesis de maestría en ingeniería industrial" });
            records.add(new String[] { "MEJORA DE PROCESOS ACADÉMICOS", "Cuerpos colegiados", "Comisiones internas de acreditación", "2026-1", "Comisión para acreditación de licenciatura" });
        } else if (planName.equals("Plan #2 (Borrador)")) {
            records.add(new String[] { "Financiamiento de proyecto", "Investigación", "1.2.6.1 Responsable o colaborador de proyecto", "2026-2", "Solicitud de financiamiento interno" });
        }
        return records;
    }

    // Seguimiento de evidencias. Lo reemplaza EvidenciaService.findForTracking()

    // Se reemplaza por la clase Evidence del model
    public static class TrackingRow {
        public String title;
        public String category;
        public String type;
        public String option;
        public String period;
        public String startDate;
        public String dueDate;
        public int score;
        public String status;
        public String accreditation;
        public List<String> docs;

        TrackingRow(String title, String category, String type, String option, String period,
                    String dueDate, int score, String status, List<String> docs) {
            this.title = title;
            this.category = category;
            this.type = type;
            this.option = option;
            this.period = period;
            this.startDate = "15/01/" + period.substring(0, 4);
            this.dueDate = dueDate;
            this.score = score;
            this.status = status;
            this.accreditation = "En revisión";
            this.docs = new ArrayList<>(docs);
        }
    }

    public static List<TrackingRow> findForTracking() {
        List<TrackingRow> rows = new ArrayList<>();
        rows.add(new TrackingRow("MIEMBRO DE LA COMISIÓN RESPONSABLE", "Productividad docente", "1.1.1.1 Diseño de Planes de Estudio", "Coordinador de la comisión (50 pts)", "2026-1", "25/10/2026", 50, "Entregado", List.of("Acta_Comision.pdf")));
        rows.add(new TrackingRow("Financiamiento de proyecto", "Investigación", "1.2.6.1 Responsable o colaborador de proyecto", "", "2026-1", "15/06/2026", 50, "En proceso", List.of()));
        rows.add(new TrackingRow("Constancia de Perfil PRODEP", "Otros", "3.1 Antigüedad y Permanencia", "", "2026-1", "22/10/2026", 80, "Entregado", List.of("PRODEP_1.pdf", "PRODEP_2.pdf", "PRODEP_3.jpg")));
        rows.add(new TrackingRow("TALLER ACADÉMICO", "Productividad docente", "1.1.1.14 Impartición de cursos/diplomados", "", "2026-2", "15/11/2026", 40, "Pendiente", List.of("Constancia.pdf", "Lista.pdf", "Programa.pdf", "Foto1.jpg", "Foto2.jpg")));
        rows.add(new TrackingRow("Participación en Artículo", "Investigación", "1.2.4.1 Artículos en revistas de catálogo", "Revista internacional (30 pts)", "2026-2", "15/11/2026", 30, "En proceso", List.of("Articulo.pdf", "Carta_Aceptacion.pdf")));
        rows.add(new TrackingRow("Dirección de Tesis de Maestría", "Tutoría", "1.3.5.1 Dirección de tesis", "Titular de tesis de maestría (30 pts)", "2026-2", "15/11/2026", 30, "Pendiente", List.of()));
        return rows;
    }

    // Reporte anual. Lo reemplaza ReporteService.getAnnualReport(año)

    // Cada fila es { categoría, título, tipo de actividad, periodo, puntaje, documentos }
    public static List<Object[]> getAnnualReport(int year) {
        List<Object[]> rows = new ArrayList<>();
        if (year == 2026) {
            rows.add(new Object[] { "Productividad docente", "MIEMBRO DE LA COMISIÓN RESPONSABLE", "1.1.1.1 Diseño de Planes de Estudio", "2026-1", 50, "1 archivo (.pdf)" });
            rows.add(new Object[] { "Otros", "Constancia de Perfil PRODEP", "3.1 Antigüedad y Permanencia", "2026-1", 80, "3 archivos (.pdf, .jpg)" });
        } else {
            rows.add(new Object[] { "Investigación", "Publicación en revista arbitrada", "1.2.4.1 Artículos en revistas de catálogo", year + "-2", 30, "2 archivos (.pdf)" });
        }
        return rows;
    }

    // Alertas. Las reemplaza AlertaService.checkAlerts()

    public static List<String> checkAlerts() {
        List<String> alerts = new ArrayList<>();
        alerts.add("Cuenta con 3 registros sin documento adj. aprobatorio");
        alerts.add("Registro con evidencia en formato no válido");
        alerts.add("Hay 3 evidencias con estatus de \"Entregado\" sin documentos aprobatorios adjuntos");
        alerts.add("Hay 1 evidencia con estatus de \"En proceso\"");
        return alerts;
    }
}
