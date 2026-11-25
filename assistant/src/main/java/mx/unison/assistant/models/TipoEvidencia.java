package mx.unison.assistant.models;

/**
 * Representa un tipo de actividad académica definida en el catálogo del Reglamento PEDPD.
 * Pertenece a una {@link Categoria} y corresponde a la tabla {@code TipoEvidencia}.
 */
public class TipoEvidencia {

    private int idTipoEvidencia;
    private String claveTipoEvidencia;
    private String nombreTipoEvidencia;
    private String definicionTipoEvidencia;
    private String contenidoAcreditacion;
    private double puntajeMaximo;
    private int idCategoria;

    public TipoEvidencia() {
    }

    public int getIdTipoEvidencia() {
        return idTipoEvidencia;
    }

    public void setIdTipoEvidencia(int idTipoEvidencia) {
        this.idTipoEvidencia = idTipoEvidencia;
    }

    public String getClaveTipoEvidencia() {
        return claveTipoEvidencia;
    }

    public void setClaveTipoEvidencia(String claveTipoEvidencia) {
        this.claveTipoEvidencia = claveTipoEvidencia;
    }

    public String getNombreTipoEvidencia() {
        return nombreTipoEvidencia;
    }

    public void setNombreTipoEvidencia(String nombreTipoEvidencia) {
        this.nombreTipoEvidencia = nombreTipoEvidencia;
    }

    public String getDefinicionTipoEvidencia() {
        return definicionTipoEvidencia;
    }

    public void setDefinicionTipoEvidencia(String definicionTipoEvidencia) {
        this.definicionTipoEvidencia = definicionTipoEvidencia;
    }

    public String getContenidoAcreditacion() {
        return contenidoAcreditacion;
    }

    public void setContenidoAcreditacion(String contenidoAcreditacion) {
        this.contenidoAcreditacion = contenidoAcreditacion;
    }

    public double getPuntajeMaximo() {
        return puntajeMaximo;
    }

    public void setPuntajeMaximo(double puntajeMaximo) {
        this.puntajeMaximo = puntajeMaximo;
    }

    public int getIdCategoria() {
        return idCategoria;
    }

    public void setIdCategoria(int idCategoria) {
        this.idCategoria = idCategoria;
    }

    @Override
    public String toString() {
        return nombreTipoEvidencia;
    }
}