package cent35.services;

import cent35.dtos.TamizajeCompletoDTO;
import cent35.dtos.TamizajeCompletoRequest;
import cent35.models.Antropometria;
import cent35.models.Cuestionario;
import cent35.models.Enfermero;
import cent35.models.Paciente;
import cent35.models.ResultadosTamizaje;
import cent35.models.Tamizaje;
import cent35.models.TomaPresion;
import cent35.models.enums.RolEnfermero;
import cent35.repositories.EnfermeroRepository;
import cent35.repositories.PacienteRepository;
import cent35.repositories.TamizajeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Service
public class TamizajeCompletoService {
    private final TamizajeRepository tamizajes;
    private final PacienteRepository pacientes;
    private final EnfermeroRepository enfermeros;
    private final ResultadoTamizajeService resultados;

    public TamizajeCompletoService(TamizajeRepository tamizajes, PacienteRepository pacientes,
                                  EnfermeroRepository enfermeros, ResultadoTamizajeService resultados) {
        this.tamizajes = tamizajes;
        this.pacientes = pacientes;
        this.enfermeros = enfermeros;
        this.resultados = resultados;
    }

    /** Ninguna persona, ficha ni control queda guardado si falla cualquier parte del alta. */
    @Transactional
    public TamizajeCompletoDTO registrar(TamizajeCompletoRequest datos) {
        if (datos == null) throw error(HttpStatus.BAD_REQUEST, "Faltan los datos del tamizaje");
        if (!Boolean.TRUE.equals(datos.consent)) {
            throw error(HttpStatus.FORBIDDEN, "El paciente no acepto el consentimiento");
        }
        String idRegistro = textoRequerido(datos.idRegistro, "id de registro");
        if (datos.supervisorNombre != null && datos.supervisorNombre.trim().length() > 120) {
            throw error(HttpStatus.BAD_REQUEST, "El nombre del supervisor no puede superar 120 caracteres");
        }
        if (tamizajes.existsByIdRegistro(idRegistro)) {
            throw error(HttpStatus.CONFLICT, "El id de registro ya existe");
        }

        Enfermero operador = resolverOperador(datos);
        Paciente paciente = resolverPaciente(datos);
        Enfermero supervisor = resolverSupervisor(datos.supervisorId);
        Antropometria antropometria = crearAntropometria(datos.antropometria);
        TomaPresion toma = crearTomaPresion(datos.tomaPresion);
        Cuestionario cuestionario = crearCuestionario(datos.cuestionario);

        Tamizaje tamizaje = new Tamizaje(true, idRegistro);
        tamizaje.setOperador(operador);
        tamizaje.setPaciente(paciente);
        tamizaje.setSupervisor(supervisor);
        tamizaje.setSupervisorNombre(datos.supervisorNombre);
        tamizaje.setAntropometria(antropometria);
        tamizaje.setCuestionario(cuestionario);
        tamizaje.addTomaPresion(toma);
        if (resultados.calcular(tamizaje, new ResultadosTamizaje()) == null) {
            throw error(HttpStatus.BAD_REQUEST, "No se pudo calcular FINDRISC");
        }

        // Validación y cálculo completos antes de la primera escritura.
        if (datos.operadorId == null) enfermeros.save(operador);
        if (datos.pacienteId == null) pacientes.save(paciente);
        tamizajes.saveAndFlush(tamizaje);
        return new TamizajeCompletoDTO(tamizaje, toma);
    }

    private Enfermero resolverOperador(TamizajeCompletoRequest datos) {
        if ((datos.operadorId == null) == (datos.operador == null)) {
            throw error(HttpStatus.BAD_REQUEST, "Seleccione un enfermero existente o complete uno nuevo");
        }
        if (datos.operadorId != null) {
            Enfermero operador = enfermeros.findById(datos.operadorId)
                    .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Enfermero no encontrado"));
            if (operador.getRol() != RolEnfermero.ENFERMERO) {
                throw error(HttpStatus.BAD_REQUEST, "El operador debe tener rol ENFERMERO");
            }
            return operador;
        }
        TamizajeCompletoRequest.Operador operador = datos.operador;
        if (operador.turno == null) throw error(HttpStatus.BAD_REQUEST, "Falta el turno del enfermero");
        return new Enfermero(textoRequerido(operador.nombre, "nombre del enfermero"),
                textoRequerido(operador.apellido, "apellido del enfermero"),
                RolEnfermero.ENFERMERO, operador.turno);
    }

    private Paciente resolverPaciente(TamizajeCompletoRequest datos) {
        if ((datos.pacienteId == null) == (datos.paciente == null)) {
            throw error(HttpStatus.BAD_REQUEST, "Seleccione un paciente existente o complete uno nuevo");
        }
        Paciente paciente = datos.pacienteId != null
                ? pacientes.findById(datos.pacienteId)
                    .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Paciente no encontrado"))
                : new Paciente(datos.paciente.fechaNacimiento, datos.paciente.sexo, LocalDateTime.now());
        if (!paciente.validar()) {
            throw error(HttpStatus.BAD_REQUEST, "El paciente debe tener sexo y fecha de nacimiento validos (14 a 120 años)");
        }
        return paciente;
    }

    private Enfermero resolverSupervisor(Integer id) {
        if (id == null) return null;
        Enfermero supervisor = enfermeros.findById(id)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Supervisor no encontrado"));
        if (supervisor.getRol() != RolEnfermero.SUPERVISOR) {
            throw error(HttpStatus.BAD_REQUEST, "El supervisor debe tener rol SUPERVISOR");
        }
        return supervisor;
    }

    private Antropometria crearAntropometria(TamizajeCompletoRequest.Antropometria datos) {
        if (datos == null || datos.pesoKg == null || datos.tallacm == null) {
            throw error(HttpStatus.BAD_REQUEST, "Faltan el peso o la talla");
        }
        // Cero representa una medición opcional que no fue tomada, igual que el modelo existente.
        if (datos.cinturacm != null && (!Float.isFinite(datos.cinturacm)
                || datos.cinturacm < 0 || datos.cinturacm > 300)) {
            throw error(HttpStatus.BAD_REQUEST, "Valor de cintura inválido");
        }
        Antropometria antropometria = new Antropometria(datos.pesoKg, datos.tallacm,
                datos.cinturacm == null ? 0 : datos.cinturacm);
        if (!antropometria.validar()) throw error(HttpStatus.BAD_REQUEST, "Peso o talla invalidos");
        return antropometria;
    }

    private TomaPresion crearTomaPresion(TamizajeCompletoRequest.TomaPresion datos) {
        if (datos == null) throw error(HttpStatus.BAD_REQUEST, "Falta la toma de presión");
        TomaPresion toma = new TomaPresion();
        toma.setNumeroToma(1);
        toma.setSistolicaMmHg(datos.sistolicaMmHg);
        toma.setDiastolicaMmHg(datos.diastolicaMmHg);
        if (!toma.validar()) throw error(HttpStatus.BAD_REQUEST, "Valores de presión inválidos");
        return toma;
    }

    private Cuestionario crearCuestionario(TamizajeCompletoRequest.Cuestionario datos) {
        if (datos == null || datos.tiposAntecedentes == null
                || datos.tiposAntecedentes.stream().anyMatch(tipo -> tipo == null)) {
            throw error(HttpStatus.BAD_REQUEST, "Cuestionario incompleto o inválido");
        }
        Cuestionario cuestionario = new Cuestionario(datos.actividadFisica, datos.frutasVerduras,
                datos.medicacion, datos.tabaquismo, datos.antecedenteFamiliar,
                new ArrayList<>(datos.tiposAntecedentes));
        cuestionario.setAntecedentesFamiliares(new ArrayList<>(datos.antecedentesFamiliares));
        if (!cuestionario.validar()) throw error(HttpStatus.BAD_REQUEST, "Cuestionario incompleto o inválido");
        return cuestionario;
    }

    private String textoRequerido(String texto, String campo) {
        if (texto == null || texto.trim().isEmpty() || texto.trim().length() > 255) {
            throw error(HttpStatus.BAD_REQUEST, "Falta o es inválido el " + campo);
        }
        return texto.trim();
    }

    private ResponseStatusException error(HttpStatus estado, String mensaje) {
        return new ResponseStatusException(estado, mensaje);
    }
}
