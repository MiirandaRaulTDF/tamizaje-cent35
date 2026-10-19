package cent35.models;

import cent35.models.enums.RolEnfermero;
import cent35.models.enums.Turnos;
import org.hibernate.annotations.GenericGenerator;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.OneToMany;
import javax.persistence.FetchType;
import javax.persistence.*;

@Entity
public class Enfermero {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private int id;

    private String nombre;
    private String apellido;

    @Enumerated(EnumType.STRING)
    private RolEnfermero rol;

    @Enumerated(EnumType.STRING)
    private Turnos turno;

    @OneToMany(mappedBy="operador", fetch=FetchType.LAZY)
    Set<Tamizaje> tamizajesOperados = new HashSet<>();

    @OneToMany(mappedBy="supervisor", fetch=FetchType.LAZY)
    Set<Tamizaje> tamizajesSupervisados = new HashSet<>();

    public Enfermero() {}

    public Enfermero(String nombre, String apellido, RolEnfermero rol, Turnos turno) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.rol = rol;
        this.turno = turno;
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public RolEnfermero getRol() {
        return rol;
    }

    public void setRol(RolEnfermero rol) {
        this.rol = rol;
    }

    public Turnos getTurno() {
        return turno;
    }

    public void setTurno(Turnos turno) {
        this.turno = turno;
    }
}