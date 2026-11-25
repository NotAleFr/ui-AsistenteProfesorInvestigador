package mx.unison.assistant.model;

/**
 * Catálogo de estados de cumplimiento de una evidencia institucional (tabla {@code Estatus}).
 * Valores esperados según la Tabla 3: "Pendiente", "En proceso" y "Entregado".
 */
public class Estatus {

    private int idEstatus;
    private String nombreEstatus;

    public Estatus() {
    }

    public Estatus(int idEstatus, String nombreEstatus) {
        this.idEstatus = idEstatus;
        this.nombreEstatus = nombreEstatus;
    }

    public int getIdEstatus() {
        return idEstatus;
    }

    public void setIdEstatus(int idEstatus) {
        this.idEstatus = idEstatus;
    }

    public String getNombreEstatus() {
        return nombreEstatus;
    }

    public void setNombreEstatus(String nombreEstatus) {
        this.nombreEstatus = nombreEstatus;
    }

    @Override
    public String toString() {
        return nombreEstatus;
    }

    // Compara por id para que JComboBox.setSelectedItem() encuentre el valor guardado
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Estatus)) {
            return false;
        }
        Estatus that = (Estatus) other;
        return idEstatus == that.idEstatus;
    }

    @Override
    public int hashCode() {
        return idEstatus;
    }
}
