package cent35.dtos;

import cent35.models.enums.AntecedentesFamiliares;
import cent35.models.enums.Sexo;
import cent35.models.enums.Tabaquismo;
import cent35.models.enums.TipoAntecedente;
import cent35.models.enums.Turnos;
import cent35.models.DetalleAntecedenteFamiliar;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Datos del formulario único; los identificadores sólo se aceptan para reutilizar personas. */
public class TamizajeCompletoRequest {
    public String idRegistro;
    public Boolean consent;
    public Integer operadorId;
    public Operador operador;
    public Integer pacienteId;
    public Paciente paciente;
    public Integer supervisorId;
    public String supervisorNombre;
    public Antropometria antropometria;
    public TomaPresion tomaPresion;
    public Cuestionario cuestionario;

    public static class Operador {
        public String nombre;
        public String apellido;
        public Turnos turno;
    }

    public static class Paciente {
        public LocalDate fechaNacimiento;
        public Sexo sexo;
    }

    public static class Antropometria {
        public Float pesoKg;
        public Float tallacm;
        public Float cinturacm;
    }

    public static class TomaPresion {
        public Integer sistolicaMmHg;
        public Integer diastolicaMmHg;
    }

    public static class Cuestionario {
        public Boolean actividadFisica;
        public Boolean frutasVerduras;
        public Boolean medicacion;
        public Tabaquismo tabaquismo;
        public AntecedentesFamiliares antecedenteFamiliar;
        public List<TipoAntecedente> tiposAntecedentes = new ArrayList<>();
        public List<DetalleAntecedenteFamiliar> antecedentesFamiliares = new ArrayList<>();
    }
}
