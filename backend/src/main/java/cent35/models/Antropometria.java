package cent35.models;

import javax.persistence.*;

@Entity
public class Antropometria extends Control {

    private float pesoKg;
    private float tallacm;
    private float cinturacm;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="tamizaje_id", nullable = false, unique = true)
    private Tamizaje tamizaje;

    public Antropometria() {}

    public Antropometria(float pesoKg, float tallacm, float cinturacm) {
        this.pesoKg = pesoKg;
        this.tallacm = tallacm;
        this.cinturacm = cinturacm;
    }

    public float getPesoKg() { return pesoKg; }
    public void setPesoKg(float pesoKg) { this.pesoKg = pesoKg; }

    public float getTallacm() { return tallacm; }
    public void setTallacm(float tallacm) { this.tallacm = tallacm; }

    public float getCinturacm() { return cinturacm; }
    public void setCinturacm(float cinturacm) { this.cinturacm = cinturacm; }

    public Tamizaje getTamizaje() { return tamizaje; }
    public void setTamizaje(Tamizaje tamizaje) { this.tamizaje = tamizaje; }

    @Override
    public Boolean validar() {
        return pesoKg >= 30 && pesoKg <= 300
                && tallacm >= 120 && tallacm <= 220;
    }

    public float calcularIMC() {
        if (tallacm <= 0) return 0;
        double tallaMetros = tallacm / 100.0;
        return (float) (this.pesoKg / (tallaMetros * tallaMetros));
    }
}
