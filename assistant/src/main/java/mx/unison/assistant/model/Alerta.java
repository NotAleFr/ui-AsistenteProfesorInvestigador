package mx.unison.assistant.model;

/**
 * Representa un aviso o recordatorio generado por el motor de reglas (RF-19, RF-20)
 * y almacenado en la tabla {@code Alertas}.
 */
public class Alerta {

    private int idAlerta;
    private String contenidoAlerta;
    private String fechaHoraCreacion;

    public Alerta() {
    }

    public Alerta(int idAlerta, String contenidoAlerta, String fechaHoraCreacion) {
        this.idAlerta = idAlerta;
        this.contenidoAlerta = contenidoAlerta;
        this.fechaHoraCreacion = fechaHoraCreacion;
    }

    public int getIdAlerta() {
        return idAlerta;
    }

    public void setIdAlerta(int idAlerta) {
        this.idAlerta = idAlerta;
    }

    public String getContenidoAlerta() {
        return contenidoAlerta;
    }

    public void setContenidoAlerta(String contenidoAlerta) {
        this.contenidoAlerta = contenidoAlerta;
    }

    public String getFechaHoraCreacion() {
        return fechaHoraCreacion;
    }

    public void setFechaHoraCreacion(String fechaHoraCreacion) {
        this.fechaHoraCreacion = fechaHoraCreacion;
    }

    @Override
    public String toString() {
        return contenidoAlerta;
    }
}