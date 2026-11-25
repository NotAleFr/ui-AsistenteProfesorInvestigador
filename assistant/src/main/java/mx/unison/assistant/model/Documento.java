package mx.unison.assistant.model;

/**
 * Representa un archivo probatorio adjunto a una {@link Evidencia} (tabla {@code Documento}, RF-03).
 * Almacena la ruta física relativa, metadatos del archivo y su huella de integridad SHA-256.
 */
public class Documento {

    private int idDocumento;
    private String rutaAlmacenamiento;
    private String nombreOriginal;
    private long tamanoBytes;
    private String sha256; // Cadena hexadecimal de 64 caracteres
    private String fechaSubida;
    private int idEvidencia;

    public Documento() {
    }

    public int getIdDocumento() {
        return idDocumento;
    }

    public void setIdDocumento(int idDocumento) {
        this.idDocumento = idDocumento;
    }

    public String getRutaAlmacenamiento() {
        return rutaAlmacenamiento;
    }

    public void setRutaAlmacenamiento(String rutaAlmacenamiento) {
        this.rutaAlmacenamiento = rutaAlmacenamiento;
    }

    public String getNombreOriginal() {
        return nombreOriginal;
    }

    public void setNombreOriginal(String nombreOriginal) {
        this.nombreOriginal = nombreOriginal;
    }

    public long getTamanoBytes() {
        return tamanoBytes;
    }

    public void setTamanoBytes(long tamanoBytes) {
        this.tamanoBytes = tamanoBytes;
    }

    public String getSha256() {
        return sha256;
    }

    public void setSha256(String sha256) {
        this.sha256 = sha256;
    }

    public String getFechaSubida() {
        return fechaSubida;
    }

    public void setFechaSubida(String fechaSubida) {
        this.fechaSubida = fechaSubida;
    }

    public int getIdEvidencia() {
        return idEvidencia;
    }

    public void setIdEvidencia(int idEvidencia) {
        this.idEvidencia = idEvidencia;
    }

    @Override
    public String toString() {
        return nombreOriginal;
    }
}