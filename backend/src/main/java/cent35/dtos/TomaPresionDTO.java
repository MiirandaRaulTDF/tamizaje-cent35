package cent35.dtos;

import cent35.models.TomaPresion;

public class TomaPresionDTO {
    private Long idControl;
    private Integer numeroToma;
    private Integer sistolicaMmHg;
    private Integer diastolicaMmHg;

    public TomaPresionDTO(TomaPresion toma) {
        this.idControl = toma.getIdControl();
        this.numeroToma = toma.getNumeroToma();
        this.sistolicaMmHg = toma.getSistolicaMmHg();
        this.diastolicaMmHg = toma.getDiastolicaMmHg();
    }

    public Long getIdControl() {
        return idControl;
    }

    public Integer getNumeroToma() {
        return numeroToma;
    }

    public Integer getSistolicaMmHg() {
        return sistolicaMmHg;
    }

    public Integer getDiastolicaMmHg() {
        return diastolicaMmHg;
    }
}