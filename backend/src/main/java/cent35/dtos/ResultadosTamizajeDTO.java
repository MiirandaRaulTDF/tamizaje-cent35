package cent35.dtos;

import cent35.models.ResultadosTamizaje;
import cent35.models.enums.SemaforoGlobal;

public class ResultadosTamizajeDTO {
    private long id;
    private int puntajeFindrisc;
    private String categoriaFindrisc;
    private String riesgoCardiovascular;
    private String semaforoRiesgoCardiovascular;
    private String semaforoImc;
    private String semaforoCintura;
    private String semaforoPresion;
    private SemaforoGlobal semaforoGlobal;
    private double promedioSemaforo;
    private boolean deriva;
    private boolean derivacionInmediata;
    private String observaciones;

    public ResultadosTamizajeDTO(ResultadosTamizaje resultado) {
        this.id = resultado.getId();
        this.puntajeFindrisc = resultado.getPuntaje_findrisc();
        this.categoriaFindrisc = resultado.getCategoria_findrisc();
        this.riesgoCardiovascular = resultado.getRiesgo_cv();
        this.semaforoRiesgoCardiovascular = resultado.getSemaforo_riesgo_cv();
        this.semaforoImc = resultado.getSemaforo_imc();
        this.semaforoCintura = resultado.getSemaforo_cintura() != null ? resultado.getSemaforo_cintura()
                : resultado.getTamizaje() == null ? null : resultado.getTamizaje().clasificarCintura();
        this.semaforoPresion = resultado.getSemaforo_presion();
        this.semaforoGlobal = resultado.getTamizaje() == null
                ? resultado.getSemaforo_global()
                : resultado.getTamizaje().calcularSemaforoGlobal(resultado.getPuntaje_findrisc());
        this.promedioSemaforo = resultado.getTamizaje() == null ? 0
                : resultado.getTamizaje().calcularPromedioSemaforo(resultado.getPuntaje_findrisc());
        this.deriva = resultado.isDeriva() || resultado.getTamizaje() != null
                && (resultado.getTamizaje().requiereDerivacion(resultado.getPuntaje_findrisc())
                || resultado.isDerivacionInmediata());
        this.derivacionInmediata = resultado.isDerivacionInmediata();
        this.observaciones = resultado.getObservaciones();
    }

    public long getId() { return id; }
    public int getPuntajeFindrisc() { return puntajeFindrisc; }
    public String getCategoriaFindrisc() { return categoriaFindrisc; }
    public String getRiesgoCardiovascular() { return riesgoCardiovascular; }
    public String getSemaforoRiesgoCardiovascular() { return semaforoRiesgoCardiovascular; }
    public String getSemaforoImc() { return semaforoImc; }
    public String getSemaforoCintura() { return semaforoCintura; }
    public String getSemaforoPresion() { return semaforoPresion; }
    public SemaforoGlobal getSemaforoGlobal() { return semaforoGlobal; }
    public double getPromedioSemaforo() { return promedioSemaforo; }
    public boolean isDeriva() { return deriva; }
    public boolean isDerivacionInmediata() { return derivacionInmediata; }
    public String getObservaciones() { return observaciones; }
}
