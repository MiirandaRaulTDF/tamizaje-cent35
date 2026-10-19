package cent35.controllers;

import cent35.dtos.TamizajeDTO;
import cent35.models.Tamizaje;
import cent35.repositories.TamizajeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class TamizajeController {

    @Autowired
    private TamizajeRepository tamizajeRepository;

    @GetMapping("/tamizajes")
    public List<TamizajeDTO> getTamizajes() {
        return tamizajeRepository.findAll().stream().map(TamizajeDTO::new).collect(Collectors.toList());
    }

    @PostMapping("/tamizajes")
    public ResponseEntity<Object> crearTamizaje(@RequestBody Tamizaje nuevoTamizaje) {

        if (!nuevoTamizaje.isConsent()) {
            return new ResponseEntity<>("El paciente no acepto el consentimiento", HttpStatus.FORBIDDEN);
        }

        if (nuevoTamizaje.getId_registro() == null || nuevoTamizaje.getId_registro().trim().isEmpty()) {
            return new ResponseEntity<>("Falta el id de registro", HttpStatus.BAD_REQUEST);
        }
        if (tamizajeRepository.existsByIdRegistro(nuevoTamizaje.getId_registro())) {
            return new ResponseEntity<>("El id de registro ya existe", HttpStatus.CONFLICT);
        }

        nuevoTamizaje.setTimestamp(LocalDateTime.now());

        tamizajeRepository.save(nuevoTamizaje);

        return new ResponseEntity<>("Ficha de tamizaje inicializada con exito", HttpStatus.CREATED);
    }

    @GetMapping("/tamizajes/{id}")
    public ResponseEntity<Object> getTamizaje(@PathVariable Long id) {
        Tamizaje tamizaje = tamizajeRepository.findById(id).orElse(null);
        if (tamizaje == null) return new ResponseEntity<>("Tamizaje no encontrado", HttpStatus.NOT_FOUND);
        return ResponseEntity.ok(new TamizajeDTO(tamizaje));
    }

    @PutMapping("/tamizajes/{id}")
    public ResponseEntity<Object> actualizarTamizaje(@PathVariable Long id, @RequestBody Tamizaje cambios) {
        Tamizaje tamizaje = tamizajeRepository.findById(id).orElse(null);
        if (tamizaje == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Tamizaje no encontrado");
        if (cambios.getId_registro() != null && !cambios.getId_registro().trim().isEmpty()
                && !cambios.getId_registro().equals(tamizaje.getId_registro())
                && tamizajeRepository.existsByIdRegistro(cambios.getId_registro().trim()))
            return ResponseEntity.status(HttpStatus.CONFLICT).body("El identificador de registro ya existe");
        if (cambios.getId_registro() != null && !cambios.getId_registro().trim().isEmpty())
            tamizaje.setId_registro(cambios.getId_registro().trim());
        tamizaje.setConsent(cambios.isConsent());
        return ResponseEntity.ok(new TamizajeDTO(tamizajeRepository.save(tamizaje)));
    }

    @DeleteMapping("/tamizajes/{id}")
    @Transactional
    public ResponseEntity<Object> eliminarTamizaje(@PathVariable Long id) {
        Tamizaje tamizaje = tamizajeRepository.findById(id).orElse(null);
        if (tamizaje == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Tamizaje no encontrado");
        // El cascade de Tamizaje elimina también los controles y el resultado asociados.
        tamizajeRepository.delete(tamizaje);
        return ResponseEntity.noContent().build();
    }
}
