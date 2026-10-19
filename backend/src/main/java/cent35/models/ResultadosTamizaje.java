package cent35.models;

import cent35.models.enums.SemaforoGlobal;
import org.hibernate.annotations.GenericGenerator;
import javax.persistence.*;
import java.util.Locale;

@Entity
public class ResultadosTamizaje {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private long id;

    private int puntaje_findrisc;
    private String categoria_findrisc;
    private String riesgo_cv;
    private String semaforo_riesgo_cv;
    private String semaforo_imc;
    private String semaforo_cintura;
    private String semaforo_presion;
    private boolean derivacionInmediata;

    @Enumerated(EnumType.STRING)
    private SemaforoGlobal semaforo_global;

    private boolean deriva;
    private String observaciones;

    /** Token aleatorio que habilita la consulta pública desde el QR, sin exponer el ID interno. */
    @Column(name = "token_consulta", unique = true, length = 64)
    private String tokenConsulta;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tamizaje_id", nullable = false, unique = true)
    private Tamizaje tamizaje;

    public ResultadosTamizaje() {}

    public ResultadosTamizaje(int puntaje_findrisc, String categoria_findrisc, String riesgo_cv, SemaforoGlobal semaforo_global, boolean deriva, String observaciones) {
        this.puntaje_findrisc = puntaje_findrisc;
        this.categoria_findrisc = categoria_findrisc;
        this.riesgo_cv = riesgo_cv;
        this.semaforo_global = semaforo_global;
        this.deriva = deriva;
        this.observaciones = observaciones;
    }

    public long getId() { return id; }

    public int getPuntaje_findrisc() { return puntaje_findrisc; }
    public void setPuntaje_findrisc(int puntaje_findrisc) { this.puntaje_findrisc = puntaje_findrisc; }

    public String getCategoria_findrisc() { return categoria_findrisc; }
    public void setCategoria_findrisc(String categoria_findrisc) { this.categoria_findrisc = categoria_findrisc; }

    public String getRiesgo_cv() { return riesgo_cv; }
    public void setRiesgo_cv(String riesgo_cv) { this.riesgo_cv = riesgo_cv; }

    public String getSemaforo_riesgo_cv() { return semaforo_riesgo_cv; }
    public void setSemaforo_riesgo_cv(String semaforo_riesgo_cv) { this.semaforo_riesgo_cv = semaforo_riesgo_cv; }

    public SemaforoGlobal getSemaforo_global() { return semaforo_global; }
    public void setSemaforo_global(SemaforoGlobal semaforo_global) { this.semaforo_global = semaforo_global; }

    public boolean isDeriva() { return deriva; }
    public void setDeriva(boolean deriva) { this.deriva = deriva; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    public String getTokenConsulta() { return tokenConsulta; }
    public void setTokenConsulta(String tokenConsulta) { this.tokenConsulta = tokenConsulta; }

    public String getSemaforo_imc() { return semaforo_imc; }
    public void setSemaforo_imc(String semaforo_imc) { this.semaforo_imc = semaforo_imc; }
    public String getSemaforo_cintura() { return semaforo_cintura; }
    public void setSemaforo_cintura(String semaforo_cintura) { this.semaforo_cintura = semaforo_cintura; }
    public String getSemaforo_presion() { return semaforo_presion; }
    public void setSemaforo_presion(String semaforo_presion) { this.semaforo_presion = semaforo_presion; }
    public boolean isDerivacionInmediata() { return derivacionInmediata; }
    public void setDerivacionInmediata(boolean derivacionInmediata) { this.derivacionInmediata = derivacionInmediata; }

    public Tamizaje getTamizaje() { return tamizaje; }
    public void setTamizaje(Tamizaje tamizaje) { this.tamizaje = tamizaje; }

    public void marcarDerivacion() {
        this.deriva = true;
    }

    public void marcarDerivacionInmediata() {
        this.derivacionInmediata = true;
        this.deriva = true;
    }

    public void agregarObservacion(String observacion) {
        if (observacion == null || observacion.trim().isEmpty()) return;
        if (observaciones == null || observaciones.trim().isEmpty()) observaciones = observacion.trim();
        else observaciones += "\n" + observacion.trim();
    }

    public SemaforoGlobal calcularSemaforoGlobal() {
        boolean rojo = esRojo(semaforo_imc) || esRojo(semaforo_presion)
                || esRojo(categoria_findrisc) || esRojo(semaforo_riesgo_cv);
        boolean amarillo = esAmarillo(semaforo_imc) || esAmarillo(semaforo_presion)
                || esAmarillo(categoria_findrisc) || esAmarillo(semaforo_riesgo_cv);
        semaforo_global = rojo ? SemaforoGlobal.ROJO
                : amarillo ? SemaforoGlobal.AMARILLO : SemaforoGlobal.VERDE;
        deriva = semaforo_global == SemaforoGlobal.ROJO || derivacionInmediata;
        return semaforo_global;
    }

    private boolean esRojo(String valor) {
        String normalizado = normalizar(valor);
        return "ROJO".equals(normalizado) || "ALTO".equals(normalizado)
                || "MUY ALTO".equals(normalizado) || "MUYALTO".equals(normalizado);
    }

    private boolean esAmarillo(String valor) {
        String normalizado = normalizar(valor);
        return "AMARILLO".equals(normalizado) || "MODERADO".equals(normalizado);
    }

    private String normalizar(String valor) {
        return valor == null ? "" : valor.trim().toUpperCase(Locale.ROOT).replace('_', ' ');
    }
}
