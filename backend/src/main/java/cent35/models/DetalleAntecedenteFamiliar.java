package cent35.models;

import cent35.models.enums.ParentescoFamiliar;
import cent35.models.enums.TipoAntecedente;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

@Embeddable
public class DetalleAntecedenteFamiliar {
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_antecedente", nullable = false, length = 40)
    private TipoAntecedente tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "parentesco", nullable = false, length = 30)
    private ParentescoFamiliar parentesco;

    public DetalleAntecedenteFamiliar() { }

    public DetalleAntecedenteFamiliar(TipoAntecedente tipo, ParentescoFamiliar parentesco) {
        this.tipo = tipo;
        this.parentesco = parentesco;
    }

    public TipoAntecedente getTipo() { return tipo; }
    public void setTipo(TipoAntecedente tipo) { this.tipo = tipo; }
    public ParentescoFamiliar getParentesco() { return parentesco; }
    public void setParentesco(ParentescoFamiliar parentesco) { this.parentesco = parentesco; }
}
