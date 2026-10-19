package cent35.models;

import javax.persistence.*;

@Entity
@Table(name = "toma_presion")
public class TomaPresion extends Control {

    @Column(nullable = false)
    private Integer numeroToma;

    @Column(nullable = false)
    private Integer sistolicaMmHg;

    @Column(nullable = false)
    private Integer diastolicaMmHg;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tamizaje_id", nullable = false)
    private Tamizaje tamizaje;

    public TomaPresion() {}

    @Override
    public Boolean validar() {
        return validarValores();
    }

    public boolean validarValores() {
        return sistolicaMmHg != null && diastolicaMmHg != null
                && sistolicaMmHg >= 70 && sistolicaMmHg <= 260
                && diastolicaMmHg >= 40 && diastolicaMmHg <= 150
                && sistolicaMmHg > diastolicaMmHg;
    }
    public Integer getNumeroToma() {
        return numeroToma;
    }

    public void setNumeroToma(Integer numeroToma) {
        this.numeroToma = numeroToma;
    }

    public Integer getSistolicaMmHg() {
        return sistolicaMmHg;
    }

    public void setSistolicaMmHg(Integer sistolicaMmHg) {
        this.sistolicaMmHg = sistolicaMmHg;
    }

    public Integer getDiastolicaMmHg() {
        return diastolicaMmHg;
    }

    public void setDiastolicaMmHg(Integer diastolicaMmHg) {
        this.diastolicaMmHg = diastolicaMmHg;
    }

    public Tamizaje getTamizaje() {
        return tamizaje;
    }

    public void setTamizaje(Tamizaje tamizaje) {
        this.tamizaje = tamizaje;
    }
}