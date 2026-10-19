package cent35.dtos;

import cent35.models.Paciente;
import cent35.models.enums.Sexo;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class PacienteDTO {
    private int id_paciente;
    private LocalDate fecha_nacimiento;
    private Sexo sexo;
    private LocalDateTime fecha_registro;

    public PacienteDTO(Paciente paciente) {
        this.id_paciente = paciente.getId_paciente();
        this.fecha_nacimiento = paciente.getFecha_nacimiento();
        this.sexo = paciente.getSexo();
        this.fecha_registro = paciente.getFecha_registro();
    }

    public int getId_paciente() {
        return id_paciente;
    }

    public LocalDate getFecha_nacimiento() {
        return fecha_nacimiento;
    }

    public Sexo getSexo() {
        return sexo;
    }

    public LocalDateTime getFecha_registro() {
        return fecha_registro;
    }
}