package cent35.dtos;

import cent35.models.Enfermero;
import cent35.models.enums.RolEnfermero;
import cent35.models.enums.Turnos;

public class EnfermeroDTO {

    private int id;
    private String nombre;
    private String apellido;
    private RolEnfermero rol;
    private Turnos turno;

    public EnfermeroDTO(Enfermero enfermero) {
        this.id = enfermero.getId();
        this.nombre = enfermero.getNombre();
        this.apellido = enfermero.getApellido();
        this.rol = enfermero.getRol();
        this.turno = enfermero.getTurno();
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public RolEnfermero getRol() {
        return rol;
    }

    public Turnos getTurno() {
        return turno;
    }
}