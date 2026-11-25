package mx.unison.assistant.model;

/**
 * Catálogo de estados de acreditación institucional de una evidencia (tabla {@code Acreditacion}).
 * Valores esperados según la Tabla 3: "Pendiente", "En revisión", "Aceptado" y "Publicado".
 */
public class Acreditacion {

    private int idAcreditacion;
    private String nombreAcreditacion;

    public Acreditacion() {
    }

    public Acreditacion(int idAcreditacion, String nombreAcreditacion) {
        this.idAcreditacion = idAcreditacion;
        this.nombreAcreditacion = nombreAcreditacion;
    }

    public int getIdAcreditacion() {
        return idAcreditacion;
    }

    public void setIdAcreditacion(int idAcreditacion) {
        this.idAcreditacion = idAcreditacion;
    }

    public String getNombreAcreditacion() {
        return nombreAcreditacion;
    }

    public void setNombreAcreditacion(String nombreAcreditacion) {
        this.nombreAcreditacion = nombreAcreditacion;
    }

    @Override
    public String toString() {
        return nombreAcreditacion;
    }

    // Compara por id para que JComboBox.setSelectedItem() encuentre el valor guardado
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Acreditacion)) {
            return false;
        }
        Acreditacion that = (Acreditacion) other;
        return idAcreditacion == that.idAcreditacion;
    }

    @Override
    public int hashCode() {
        return idAcreditacion;
    }
}
