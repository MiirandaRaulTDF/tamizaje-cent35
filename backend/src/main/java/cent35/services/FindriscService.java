package cent35.services;

import cent35.models.Antropometria;
import cent35.models.Cuestionario;
import cent35.models.Paciente;
import cent35.models.Tamizaje;
import cent35.models.DetalleAntecedenteFamiliar;
import cent35.models.enums.AntecedentesFamiliares;
import cent35.models.enums.ParentescoFamiliar;
import cent35.models.enums.Sexo;
import cent35.models.enums.TipoAntecedente;
import org.springframework.stereotype.Service;

/** Calcula FINDRISC con los datos disponibles en la ficha actual. */
@Service
public class FindriscService {

    /**
     * Variante adaptada a la jornada: suma cintura y antecedentes familiares de diabetes.
     * No pregunta por glucemia elevada previa, por eso no se presenta como FINDRISC formal.
     */
    public Integer calcularPuntaje(Tamizaje tamizaje) {
        if (tamizaje == null) return null;
        Paciente paciente = tamizaje.getPaciente();
        Antropometria antropometria = tamizaje.getAntropometria();
        Cuestionario cuestionario = tamizaje.getCuestionario();
        if (paciente == null || antropometria == null || cuestionario == null
                || paciente.getFecha_nacimiento() == null || !antropometria.validar()
                || !cuestionario.validar()) return null;

        int puntos = 0;
        int edad = paciente.calcularEdad();
        if (edad >= 65) puntos += 4;
        else if (edad >= 55) puntos += 3;
        else if (edad >= 45) puntos += 2;

        float imc = antropometria.calcularIMC();
        if (imc >= 30f) puntos += 3;
        else if (imc >= 25f) puntos += 1;

        float cintura = antropometria.getCinturacm();
        if (paciente.getSexo() == Sexo.M) {
            if (cintura > 102f) puntos += 4;
            else if (cintura >= 94f) puntos += 3;
        } else if (paciente.getSexo() == Sexo.F) {
            if (cintura > 88f) puntos += 4;
            else if (cintura >= 80f) puntos += 3;
        }

        if (Boolean.FALSE.equals(cuestionario.getActividadFisica())) puntos += 2;
        if (Boolean.FALSE.equals(cuestionario.getFrutasVerduras())) puntos += 1;
        if (Boolean.TRUE.equals(cuestionario.getMedicacion())) puntos += 2;

        if (cuestionario.getAntecedentesFamiliares() != null && !cuestionario.getAntecedentesFamiliares().isEmpty()) {
            boolean diabetesPrimerGrado = cuestionario.getAntecedentesFamiliares().stream()
                    .filter(item -> item != null && item.getTipo() == TipoAntecedente.DIABETES)
                    .anyMatch(item -> esPrimerGrado(item.getParentesco()));
            boolean diabetesSegundoGrado = cuestionario.getAntecedentesFamiliares().stream()
                    .filter(item -> item != null && item.getTipo() == TipoAntecedente.DIABETES)
                    .anyMatch(item -> esSegundoGrado(item.getParentesco()));
            if (diabetesPrimerGrado) puntos += 5;
            else if (diabetesSegundoGrado) puntos += 3;
        } else if (cuestionario.getTiposAntecedentes() != null
                && cuestionario.getTiposAntecedentes().contains(TipoAntecedente.DIABETES)) {
            AntecedentesFamiliares antecedentes = cuestionario.getAntecedenteFamiliar();
            if (antecedentes == AntecedentesFamiliares.PRIMER_GRADO) puntos += 5;
            else if (antecedentes == AntecedentesFamiliares.SEGUNDO_GRADO) puntos += 3;
        }
        return puntos;
    }

    private boolean esPrimerGrado(ParentescoFamiliar parentesco) {
        return parentesco == ParentescoFamiliar.PADRE_MADRE
                || parentesco == ParentescoFamiliar.HERMANO_HERMANA
                || parentesco == ParentescoFamiliar.HIJO_HIJA;
    }

    private boolean esSegundoGrado(ParentescoFamiliar parentesco) {
        return parentesco == ParentescoFamiliar.ABUELO_ABUELA
                || parentesco == ParentescoFamiliar.TIO_TIA
                || parentesco == ParentescoFamiliar.PRIMO_PRIMA;
    }

    public String clasificarPuntaje(Integer puntaje) {
        if (puntaje == null) return "DATOS_INSUFICIENTES";
        if (puntaje < 7) return "BAJO";
        if (puntaje <= 11) return "LIGERAMENTE_ELEVADO";
        if (puntaje <= 14) return "MODERADO";
        if (puntaje <= 20) return "ALTO";
        return "MUY_ALTO";
    }
}
