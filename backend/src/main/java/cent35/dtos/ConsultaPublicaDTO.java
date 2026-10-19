package cent35.dtos;

import cent35.models.ResultadosTamizaje;

/** Datos mínimos y no identificatorios que se muestran al abrir un QR de consulta. */
public class ConsultaPublicaDTO {
    private final String idRegistro;
    private final String semaforoGlobal;
    private final boolean derivacionInmediata;
    private final boolean deriva;
    private final String recomendacion;
    private final String categoriaFindrisc;
    private final String semaforoImc;
    private final String semaforoPresion;
    private final String riesgoCardiovascular;
    private final String semaforoRiesgoCardiovascular;
    private final String valorImc;
    private final String valorPresion;
    private final String valorCintura;
    private final String semaforoCintura;
    private final String rangoCintura;

    public ConsultaPublicaDTO(ResultadosTamizaje resultado) {
        this.idRegistro = String.valueOf(resultado.getTamizaje().getId());
        this.semaforoGlobal = resultado.getSemaforo_global() == null
                ? "SIN_CLASIFICAR" : resultado.getSemaforo_global().name();
        this.derivacionInmediata = resultado.isDerivacionInmediata();
        this.deriva = resultado.isDeriva();
        this.categoriaFindrisc = resultado.getCategoria_findrisc();
        this.semaforoImc = resultado.getSemaforo_imc();
        this.semaforoPresion = resultado.getSemaforo_presion();
        this.riesgoCardiovascular = resultado.getRiesgo_cv();
        this.semaforoRiesgoCardiovascular = resultado.getSemaforo_riesgo_cv();
        cent35.models.Tamizaje tamizaje = resultado.getTamizaje();
        cent35.models.Antropometria antropometria = tamizaje.getAntropometria();
        this.valorImc = antropometria == null || !antropometria.validar() ? null
                : String.format(java.util.Locale.ROOT, "%.1f", antropometria.calcularIMC());
        this.valorCintura = antropometria == null || antropometria.getCinturacm() <= 0 ? null
                : String.format(java.util.Locale.ROOT, "%.0f cm", antropometria.getCinturacm());
        this.semaforoCintura = tamizaje.clasificarCintura();
        this.rangoCintura = tamizaje.getPaciente() == null || tamizaje.getPaciente().getSexo() == null ? "" : "M".equals(tamizaje.getPaciente().getSexo().name())
                ? "Varón: amarillo desde 94 cm; rojo desde 102 cm"
                : "F".equals(tamizaje.getPaciente().getSexo().name())
                    ? "Mujer: amarillo desde 80 cm; rojo desde 88 cm" : "Rango no disponible para el sexo registrado";
        cent35.models.TomaPresion ultima = tamizaje.obtenerUltimaTomaPresionValida();
        this.valorPresion = ultima == null ? null : ultima.getSistolicaMmHg() + "/" + ultima.getDiastolicaMmHg() + " mmHg";
        if (derivacionInmediata) {
            this.recomendacion = "Alerta prioritaria: avise ahora al equipo de Enfermería, que le indicará la derivación adecuada.";
        } else if (deriva) {
            this.recomendacion = "Se recomienda consultar con el centro de salud para recibir orientación.";
        } else {
            this.recomendacion = "Comparta este resultado con su equipo de salud y continúe con los controles preventivos.";
        }
    }

    public String getIdRegistro() { return idRegistro; }
    public String getSemaforoGlobal() { return semaforoGlobal; }
    public boolean isDerivacionInmediata() { return derivacionInmediata; }
    public boolean isDeriva() { return deriva; }
    public String getRecomendacion() { return recomendacion; }
    public String getCategoriaFindrisc() { return categoriaFindrisc; }
    public String getSemaforoImc() { return semaforoImc; }
    public String getSemaforoPresion() { return semaforoPresion; }
    public String getRiesgoCardiovascular() { return riesgoCardiovascular; }
    public String getSemaforoRiesgoCardiovascular() { return semaforoRiesgoCardiovascular; }
    public String getValorImc() { return valorImc; }
    public String getValorPresion() { return valorPresion; }
    public String getValorCintura() { return valorCintura; }
    public String getSemaforoCintura() { return semaforoCintura; }
    public String getRangoCintura() { return rangoCintura; }
}
