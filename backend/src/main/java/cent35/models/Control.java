package cent35.models;

import javax.persistence.*;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Control {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idControl;

    public Boolean validar() {
        return true;
    }

    public Long getIdControl() { return idControl; }
    public void setIdControl(Long idControl) { this.idControl = idControl; }
}