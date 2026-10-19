package cent35.models;

import cent35.models.enums.Sexo;
import org.hibernate.annotations.GenericGenerator;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Enumerated;
import javax.persistence.EnumType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.OneToMany;
import javax.persistence.FetchType;

@Entity
public class Paciente {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private int id_paciente;

    private LocalDate fecha_nacimiento;

    @Enumerated(EnumType.STRING)
    private Sexo sexo;

    private LocalDateTime fecha_registro;

    @OneToMany(mappedBy="paciente", fetch=FetchType.EAGER)
    Set<Tamizaje> tamizajes = new HashSet<>();

    public Paciente() {
    }

    public Paciente(LocalDate fecha_nacimiento, Sexo sexo, LocalDateTime fecha_registro) {
        this.fecha_nacimiento = fecha_nacimiento;
        this.sexo = sexo;
        this.fecha_registro = fecha_registro;
    }

    public int getId_paciente() {
        return id_paciente;
    }

    public LocalDate getFecha_nacimiento() {
        return fecha_nacimiento;
    }

    public void setFecha_nacimiento(LocalDate fecha_nacimiento) {
        this.fecha_nacimiento = fecha_nacimiento;
    }

    public Sexo getSexo() {
        return sexo;
    }

    public void setSexo(Sexo sexo) {
        this.sexo = sexo;
    }

    public LocalDateTime getFecha_registro() {
        return fecha_registro;
    }

    public void setFecha_registro(LocalDateTime fecha_registro) {
        this.fecha_registro = fecha_registro;
    }

    public int calcularEdad() {
        if (fecha_nacimiento == null) return 0;
        return Period.between(fecha_nacimiento, LocalDate.now()).getYears();
    }

    public boolean validar() {
        int edad = calcularEdad();
        return fecha_nacimiento != null && sexo != null && edad >= 14 && edad <= 120;
    }
}
