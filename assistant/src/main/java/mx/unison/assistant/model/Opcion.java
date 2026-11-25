package mx.unison.assistant.model;

/**
 * Representa un criterio de evaluación u opción de participación dentro de un {@link TipoEvidencia},
 * junto con su puntaje asociado según el Anexo 5 del Reglamento PEDPD (tabla {@code Opcion}).
 */
public class Opcion {

    private int idOpcion;
    private String nombreOpcion;
    private double puntajeOpcion;
    private int ordenOpcion;
    private int idTipoEvidencia;

    public Opcion() {
    }

    public int getIdOpcion() {
        return idOpcion;
    }

    public void setIdOpcion(int idOpcion) {
        this.idOpcion = idOpcion;
    }

    public String getNombreOpcion() {
        return nombreOpcion;
    }

    public void setNombreOpcion(String nombreOpcion) {
        this.nombreOpcion = nombreOpcion;
    }

    public double getPuntajeOpcion() {
        return puntajeOpcion;
    }

    public void setPuntajeOpcion(double puntajeOpcion) {
        this.puntajeOpcion = puntajeOpcion;
    }

    public int getOrdenOpcion() {
        return ordenOpcion;
    }

    public void setOrdenOpcion(int ordenOpcion) {
        this.ordenOpcion = ordenOpcion;
    }

    public int getIdTipoEvidencia() {
        return idTipoEvidencia;
    }

    public void setIdTipoEvidencia(int idTipoEvidencia) {
        this.idTipoEvidencia = idTipoEvidencia;
    }

    /**
     * Formatea la opción para desplegarse en combos de la UI con el formato "Nombre (N pts)".
     *
     * @return etiqueta formateada con el puntaje redondeado.
     */
    public String getFormattedLabel() {
        int pts = (int) Math.round(puntajeOpcion);
        return nombreOpcion + " (" + pts + " pts)";
    }

    @Override
    public String toString() {
        return getFormattedLabel();
    }
}