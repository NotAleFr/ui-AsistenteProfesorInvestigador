package mx.unison.assistant.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Unidad central del sistema: representa una evidencia institucional registrada
 * por el profesor-investigador (tabla {@code Evidencia}, RF-04).
 * Incluye campos auxiliares resueltos mediante JOIN para evitar consultas repetitivas en la UI.
 */
public class Evidencia {

    // Columnas persistidas directamente en la tabla Evidencia
    private int idEvidencia;
    private String titulo;
    private String descripcion;
    private int anio;
    private String periodo;           // Formato YYYY-1 o YYYY-2
    private String fechaInicio;       // Formato ISO 8601: YYYY-MM-DD
    private String fechaFinalizacion; // Formato ISO 8601: YYYY-MM-DD
    private String fechaModificacion;
    private int idTipoEvidencia;
    private int idOpcion;             // 0 = todavía sin opción (columna NULL)
    private int idEstatus;
    private int idAcreditacion;
    private int idPlanAcademico;

    // Atributos descriptivos obtenidos por JOIN con los catálogos normativos
    private int idCategoria;
    private String nombreCategoria;
    private String claveCategoria;
    private String nombreTipoEvidencia;
    private String nombreOpcion;
    private double puntajeOpcion;
    private String nombreEstatus;
    private String nombreAcreditacion;
    private boolean planActivo;
    private List<Documento> documentos = new ArrayList<>();

    public Evidencia() {
    }

    public int getIdEvidencia() {
        return idEvidencia;
    }

    public void setIdEvidencia(int idEvidencia) {
        this.idEvidencia = idEvidencia;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public int getAnio() {
        return anio;
    }

    public void setAnio(int anio) {
        this.anio = anio;
    }

    public String getPeriodo() {
        return periodo;
    }

    public void setPeriodo(String periodo) {
        this.periodo = periodo;
    }

    public String getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(String fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public String getFechaFinalizacion() {
        return fechaFinalizacion;
    }

    public void setFechaFinalizacion(String fechaFinalizacion) {
        this.fechaFinalizacion = fechaFinalizacion;
    }

    public String getFechaModificacion() {
        return fechaModificacion;
    }

    public void setFechaModificacion(String fechaModificacion) {
        this.fechaModificacion = fechaModificacion;
    }

    public int getIdTipoEvidencia() {
        return idTipoEvidencia;
    }

    public void setIdTipoEvidencia(int idTipoEvidencia) {
        this.idTipoEvidencia = idTipoEvidencia;
    }

    public int getIdOpcion() {
        return idOpcion;
    }

    public void setIdOpcion(int idOpcion) {
        this.idOpcion = idOpcion;
    }

    public int getIdEstatus() {
        return idEstatus;
    }

    public void setIdEstatus(int idEstatus) {
        this.idEstatus = idEstatus;
    }

    public int getIdAcreditacion() {
        return idAcreditacion;
    }

    public void setIdAcreditacion(int idAcreditacion) {
        this.idAcreditacion = idAcreditacion;
    }

    public int getIdPlanAcademico() {
        return idPlanAcademico;
    }

    public void setIdPlanAcademico(int idPlanAcademico) {
        this.idPlanAcademico = idPlanAcademico;
    }

    public int getIdCategoria() {
        return idCategoria;
    }

    public void setIdCategoria(int idCategoria) {
        this.idCategoria = idCategoria;
    }

    public String getNombreCategoria() {
        return nombreCategoria;
    }

    public void setNombreCategoria(String nombreCategoria) {
        this.nombreCategoria = nombreCategoria;
    }

    public String getClaveCategoria() {
        return claveCategoria;
    }

    public void setClaveCategoria(String claveCategoria) {
        this.claveCategoria = claveCategoria;
    }

    public String getNombreTipoEvidencia() {
        return nombreTipoEvidencia;
    }

    public void setNombreTipoEvidencia(String nombreTipoEvidencia) {
        this.nombreTipoEvidencia = nombreTipoEvidencia;
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

    public String getNombreEstatus() {
        return nombreEstatus;
    }

    public void setNombreEstatus(String nombreEstatus) {
        this.nombreEstatus = nombreEstatus;
    }

    public String getNombreAcreditacion() {
        return nombreAcreditacion;
    }

    public void setNombreAcreditacion(String nombreAcreditacion) {
        this.nombreAcreditacion = nombreAcreditacion;
    }

    public boolean isPlanActivo() {
        return planActivo;
    }

    public void setPlanActivo(boolean planActivo) {
        this.planActivo = planActivo;
    }

    public List<Documento> getDocumentos() {
        return documentos;
    }

    public void setDocumentos(List<Documento> documentos) {
        this.documentos = documentos;
    }
}