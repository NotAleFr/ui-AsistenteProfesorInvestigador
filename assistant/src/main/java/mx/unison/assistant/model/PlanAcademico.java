package mx.unison.assistant.model;

/**
 * Representa un Plan de Trabajo académico creado por el profesor-investigador
 * para organizar sus actividades por año y periodo semestral (tabla {@code PlanAcademico}).
 */
public class PlanAcademico {

    private int idPlanAcademico;
    private String nombrePlan;
    private int anioPlan;
    private String periodoPlan; // Formato YYYY-1 o YYYY-2

    public PlanAcademico() {
    }

    public PlanAcademico(int idPlanAcademico, String nombrePlan, int anioPlan, String periodoPlan) {
        this.idPlanAcademico = idPlanAcademico;
        this.nombrePlan = nombrePlan;
        this.anioPlan = anioPlan;
        this.periodoPlan = periodoPlan;
    }

    public int getIdPlanAcademico() {
        return idPlanAcademico;
    }

    public void setIdPlanAcademico(int idPlanAcademico) {
        this.idPlanAcademico = idPlanAcademico;
    }

    public String getNombrePlan() {
        return nombrePlan;
    }

    public void setNombrePlan(String nombrePlan) {
        this.nombrePlan = nombrePlan;
    }

    public int getAnioPlan() {
        return anioPlan;
    }

    public void setAnioPlan(int anioPlan) {
        this.anioPlan = anioPlan;
    }

    public String getPeriodoPlan() {
        return periodoPlan;
    }

    public void setPeriodoPlan(String periodoPlan) {
        this.periodoPlan = periodoPlan;
    }

    @Override
    public String toString() {
        return nombrePlan;
    }
}