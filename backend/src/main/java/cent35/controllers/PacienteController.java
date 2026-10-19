package cent35.controllers;

import cent35.dtos.PacienteDTO;
import cent35.models.Paciente;
import cent35.models.enums.Sexo;
import cent35.repositories.PacienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api")
public class PacienteController {

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private cent35.repositories.TamizajeRepository tamizajeRepository;

    @GetMapping("/pacientes")
    public List<PacienteDTO> getPacientes() {
        return pacienteRepository.findAll().stream().map(PacienteDTO::new).collect(Collectors.toList());
    }

    @PostMapping("/pacientes")
    public ResponseEntity<Object> registrarPaciente(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fecha_nacimiento,
            @RequestParam Sexo sexo) {

        if (fecha_nacimiento == null || sexo == null) {
            return new ResponseEntity<>("Faltan datos", HttpStatus.BAD_REQUEST);
        }

        Paciente nuevoPaciente = new Paciente(fecha_nacimiento, sexo, LocalDateTime.now());

        Paciente guardado = pacienteRepository.save(nuevoPaciente);
        return new ResponseEntity<>(new PacienteDTO(guardado), HttpStatus.CREATED);
    }

    @PutMapping("/pacientes/{id}")
    public ResponseEntity<Object> actualizarPaciente(@PathVariable Integer id,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fecha_nacimiento,
            @RequestParam Sexo sexo) {
        Paciente paciente = pacienteRepository.findById(id).orElse(null);
        if (paciente == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Paciente no encontrado");
        if (fecha_nacimiento == null || sexo == null) return ResponseEntity.badRequest().body("Faltan datos");
        paciente.setFecha_nacimiento(fecha_nacimiento);
        paciente.setSexo(sexo);
        if (!paciente.validar()) return ResponseEntity.badRequest().body("Datos de paciente inválidos");
        return ResponseEntity.ok(new PacienteDTO(pacienteRepository.save(paciente)));
    }

    @DeleteMapping("/pacientes/{id}")
    @Transactional
    public ResponseEntity<Object> eliminarPaciente(@PathVariable Integer id) {
        Paciente paciente = pacienteRepository.findById(id).orElse(null);
        if (paciente == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Paciente no encontrado");
        tamizajeRepository.deleteAll(tamizajeRepository.buscarPorPaciente(id));
        pacienteRepository.delete(paciente);
        return ResponseEntity.noContent().build();
    }
}
