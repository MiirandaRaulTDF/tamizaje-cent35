package cent35.models;

import cent35.models.enums.AntecedentesFamiliares;
import cent35.models.enums.Tabaquismo;
import cent35.models.enums.TipoAntecedente;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Cuestionario extends Control {

    private Boolean actividadFisica;
    private Boolean frutasVerduras;
    private Boolean medicacion;

    @Enumerated(EnumType.STRING)
    private Tabaquismo tabaquismo;

    @Enumerated(EnumType.STRING)
    private AntecedentesFamiliares antecedenteFamiliar;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    private List<TipoAntecedente> tiposAntecedentes = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "cuestionario_antecedente_familiar", joinColumns = @JoinColumn(name = "cuestionario_id_control"))
    private List<DetalleAntecedenteFamiliar> antecedentesFamiliares = new ArrayList<>();

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="tamizaje_id", nullable = false, unique = true)
    private Tamizaje tamizaje;

    public Cuestionario() {
    }

    public Cuestionario(Boolean actividadFisica, Boolean frutasVerduras, Boolean medicacion, Tabaquismo tabaquismo, AntecedentesFamiliares antecedenteFamiliar, List<TipoAntecedente> tiposAntecedentes) {
        this.actividadFisica = actividadFisica;
        this.frutasVerduras = frutasVerduras;
        this.medicacion = medicacion;
        this.tabaquismo = tabaquismo;
        this.antecedenteFamiliar = antecedenteFamiliar;
        this.tiposAntecedentes = tiposAntecedentes;
    }

    @Override
    public Boolean validar() {
        if (actividadFisica == null || frutasVerduras == null || medicacion == null
                || tabaquismo == null || antecedenteFamiliar == null) {
            return false;
        }
        if (antecedenteFamiliar != AntecedentesFamiliares.NO
                && (antecedentesFamiliares == null || antecedentesFamiliares.isEmpty())
                && (tiposAntecedentes == null || tiposAntecedentes.isEmpty())) {
            return false;
        }
        if (antecedentesFamiliares != null && antecedentesFamiliares.stream()
                .anyMatch(item -> item == null || item.getTipo() == null || item.getParentesco() == null)) return false;
        return true;
    }

    public Boolean getActividadFisica() { return actividadFisica; }
    public void setActividadFisica(Boolean actividadFisica) { this.actividadFisica = actividadFisica; }

    public Boolean getFrutasVerduras() { return frutasVerduras; }
    public void setFrutasVerduras(Boolean frutasVerduras) { this.frutasVerduras = frutasVerduras; }

    public Boolean getMedicacion() { return medicacion; }
    public void setMedicacion(Boolean medicacion) { this.medicacion = medicacion; }

    public Tabaquismo getTabaquismo() { return tabaquismo; }
    public void setTabaquismo(Tabaquismo tabaquismo) { this.tabaquismo = tabaquismo; }

    public AntecedentesFamiliares getAntecedenteFamiliar() { return antecedenteFamiliar; }
    public void setAntecedenteFamiliar(AntecedentesFamiliares antecedenteFamiliar) { this.antecedenteFamiliar = antecedenteFamiliar; }

    public List<TipoAntecedente> getTiposAntecedentes() { return tiposAntecedentes; }
    public void setTiposAntecedentes(List<TipoAntecedente> tiposAntecedentes) { this.tiposAntecedentes = tiposAntecedentes; }

    public List<DetalleAntecedenteFamiliar> getAntecedentesFamiliares() { return antecedentesFamiliares; }
    public void setAntecedentesFamiliares(List<DetalleAntecedenteFamiliar> antecedentesFamiliares) { this.antecedentesFamiliares = antecedentesFamiliares; }

    public Tamizaje getTamizaje() { return tamizaje; }
    public void setTamizaje(Tamizaje tamizaje) { this.tamizaje = tamizaje; }
}
