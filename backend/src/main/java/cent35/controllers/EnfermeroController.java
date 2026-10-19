package cent35.controllers;

import cent35.dtos.EnfermeroDTO;
import cent35.models.Enfermero;
import cent35.models.enums.RolEnfermero;
import cent35.models.enums.Turnos;
import cent35.repositories.EnfermeroRepository;
import cent35.repositories.TamizajeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api")
public class EnfermeroController {

    @Autowired
    private EnfermeroRepository enfermeroRepository;

    @Autowired
    private TamizajeRepository tamizajeRepository;

    @GetMapping("/enfermeros")
    public List<EnfermeroDTO> getEnfermeros() {
        return enfermeroRepository.findAll().stream().map(EnfermeroDTO::new).collect(Collectors.toList());
    }

    @GetMapping("/enfermeros/{id}")
    public EnfermeroDTO getEnfermero(@PathVariable int id) {
        return enfermeroRepository.findById(id).map(EnfermeroDTO::new).orElse(null);
    }

    @PostMapping("/enfermeros")
    public ResponseEntity<Object> createEnfermero(
            @RequestParam String nombre,
            @RequestParam String apellido,
            @RequestParam RolEnfermero rol,
            @RequestParam Turnos turno) {

        if (nombre == null || nombre.trim().isEmpty() || apellido == null || apellido.trim().isEmpty() || rol == null || turno == null) {
            return new ResponseEntity<>("Faltan datos", HttpStatus.BAD_REQUEST);
        }

        Enfermero enfermero = new Enfermero(nombre, apellido, rol, turno);
        return new ResponseEntity<>(new EnfermeroDTO(enfermeroRepository.save(enfermero)), HttpStatus.CREATED);
    }

    @PutMapping("/enfermeros/{id}")
    public ResponseEntity<Object> actualizarEnfermero(@PathVariable Integer id, @RequestParam String nombre,
            @RequestParam String apellido, @RequestParam RolEnfermero rol, @RequestParam Turnos turno) {
        Enfermero enfermero = enfermeroRepository.findById(id).orElse(null);
        if (enfermero == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Integrante no encontrado");
        if (nombre == null || nombre.trim().isEmpty() || apellido == null || apellido.trim().isEmpty() || rol == null || turno == null)
            return ResponseEntity.badRequest().body("Faltan datos");
        enfermero.setNombre(nombre.trim());
        enfermero.setApellido(apellido.trim());
        enfermero.setRol(rol);
        enfermero.setTurno(turno);
        return ResponseEntity.ok(new EnfermeroDTO(enfermeroRepository.save(enfermero)));
    }

    @DeleteMapping("/enfermeros/{id}")
    @Transactional
    public ResponseEntity<Object> eliminarEnfermero(@PathVariable Integer id) {
        Enfermero enfermero = enfermeroRepository.findById(id).orElse(null);
        if (enfermero == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Integrante no encontrado");
        tamizajeRepository.deleteAll(tamizajeRepository.buscarPorEnfermero(id));
        enfermeroRepository.delete(enfermero);
        return ResponseEntity.noContent().build();
    }
}
