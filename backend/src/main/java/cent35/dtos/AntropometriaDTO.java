package cent35.dtos;

import cent35.models.Antropometria;

public class AntropometriaDTO {
    private Long idControl;
    private float pesoKg;
    private float tallacm;
    private float cinturacm;
    private float imc;

    public AntropometriaDTO(Antropometria antropometria) {
        this.idControl = antropometria.getIdControl();
        this.pesoKg = antropometria.getPesoKg();
        this.tallacm = antropometria.getTallacm();
        this.cinturacm = antropometria.getCinturacm();
        this.imc = antropometria.calcularIMC();
    }

    public Long getIdControl() { return idControl; }
    public float getPesoKg() { return pesoKg; }
    public float getTallacm() { return tallacm; }
    public float getCinturacm() { return cinturacm; }
    public float getImc() { return imc; }
}
