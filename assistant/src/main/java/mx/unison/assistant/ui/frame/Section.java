package mx.unison.assistant.ui.frame;

public enum Section {
    ACADEMIC_PLAN("Plan acad."),
    TRACKING("Seguimiento"),
    REPORTS("Reportes"),
    ALERTS("Alertas"),
    SETTINGS("Ajustes");

    String label; // texto que se ve en el tile del menú

    Section(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
