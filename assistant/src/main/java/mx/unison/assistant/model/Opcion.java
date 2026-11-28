package mx.unison.assistant.model;

import java.math.BigDecimal;

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
     * Conserva decimales cuando el puntaje no es entero (por ejemplo, "0.5 pts").
     *
     * @return etiqueta formateada con el puntaje.
     */
    public String getFormattedLabel() {
        return nombreOpcion + " (" + formatScore(puntajeOpcion) + " pts)";
    }

    private static String formatScore(double score) {
        if (score == Math.rint(score)) {
            return String.valueOf((long) score);
        }
        return BigDecimal.valueOf(score).stripTrailingZeros().toPlainString();
    }

    @Override
    public String toString() {
        return getFormattedLabel();
    }

    // Compara por id para que JComboBox.setSelectedItem() encuentre el valor guardado
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Opcion)) {
            return false;
        }
        Opcion that = (Opcion) other;
        return idOpcion == that.idOpcion;
    }

    @Override
    public int hashCode() {
        return idOpcion;
    }
}
