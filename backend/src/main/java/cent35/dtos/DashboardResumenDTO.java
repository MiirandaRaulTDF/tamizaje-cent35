package cent35.dtos;

/** Indicadores agregados para el panel interno; no incluye datos identificatorios. */
public class DashboardResumenDTO {
    private final long totalTamizajes;
    private final long totalConResultado;
    private final long semaforoVerde;
    private final long semaforoAmarillo;
    private final long semaforoNaranja;
    private final long semaforoRojo;
    private final long derivaciones;
    private final long derivacionesInmediatas;
    private final Double edadPromedio;
    private final Double imcPromedio;
    private final Double sistolicaPromedio;
    private final Double diastolicaPromedio;

    public DashboardResumenDTO(long totalTamizajes, long totalConResultado,
                               long semaforoVerde, long semaforoAmarillo, long semaforoNaranja,
                               long semaforoRojo, long derivaciones, long derivacionesInmediatas,
                               Double edadPromedio, Double imcPromedio,
                               Double sistolicaPromedio, Double diastolicaPromedio) {
        this.totalTamizajes = totalTamizajes;
        this.totalConResultado = totalConResultado;
        this.semaforoVerde = semaforoVerde;
        this.semaforoAmarillo = semaforoAmarillo;
        this.semaforoNaranja = semaforoNaranja;
        this.semaforoRojo = semaforoRojo;
        this.derivaciones = derivaciones;
        this.derivacionesInmediatas = derivacionesInmediatas;
        this.edadPromedio = edadPromedio;
        this.imcPromedio = imcPromedio;
        this.sistolicaPromedio = sistolicaPromedio;
        this.diastolicaPromedio = diastolicaPromedio;
    }

    public long getTotalTamizajes() { return totalTamizajes; }
    public long getTotalConResultado() { return totalConResultado; }
    public long getSemaforoVerde() { return semaforoVerde; }
    public long getSemaforoAmarillo() { return semaforoAmarillo; }
    public long getSemaforoNaranja() { return semaforoNaranja; }
    public long getSemaforoRojo() { return semaforoRojo; }
    public long getDerivaciones() { return derivaciones; }
    public long getDerivacionesInmediatas() { return derivacionesInmediatas; }
    public Double getEdadPromedio() { return edadPromedio; }
    public Double getImcPromedio() { return imcPromedio; }
    public Double getSistolicaPromedio() { return sistolicaPromedio; }
    public Double getDiastolicaPromedio() { return diastolicaPromedio; }
}
