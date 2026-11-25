package mx.unison.assistant.model;

/**
 * Representa una categoría normativa del Reglamento PEDPD (Anexo 1) y la categoría "Otros" (RF-04).
 * Corresponde a la tabla {@code Categoria} en SQLite3.
 */
public class Categoria {

    private int idCategoria;
    private String claveCategoria;
    private String nombreCategoria;
    private String definicionCategoria;

    public Categoria() {
    }

    public Categoria(int idCategoria, String claveCategoria, String nombreCategoria, String definicionCategoria) {
        this.idCategoria = idCategoria;
        this.claveCategoria = claveCategoria;
        this.nombreCategoria = nombreCategoria;
        this.definicionCategoria = definicionCategoria;
    }

    public int getIdCategoria() {
        return idCategoria;
    }

    public void setIdCategoria(int idCategoria) {
        this.idCategoria = idCategoria;
    }

    public String getClaveCategoria() {
        return claveCategoria;
    }

    public void setClaveCategoria(String claveCategoria) {
        this.claveCategoria = claveCategoria;
    }

    public String getNombreCategoria() {
        return nombreCategoria;
    }

    public void setNombreCategoria(String nombreCategoria) {
        this.nombreCategoria = nombreCategoria;
    }

    public String getDefinicionCategoria() {
        return definicionCategoria;
    }

    public void setDefinicionCategoria(String definicionCategoria) {
        this.definicionCategoria = definicionCategoria;
    }

    @Override
    public String toString() {
        return nombreCategoria;
    }
}