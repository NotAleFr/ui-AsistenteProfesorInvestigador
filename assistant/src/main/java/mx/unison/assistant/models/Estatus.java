package mx.unison.assistant.models;

/**
 * Catálogo de estados de cumplimiento de una evidencia institucional (tabla {@code Estatus}).
 * Valores esperados según RF-01: "Pendiente", "En proceso" y "Entregado".
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
}