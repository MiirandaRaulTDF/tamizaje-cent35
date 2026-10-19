package cent35.controllers;

import cent35.dtos.AntropometriaDTO;
import cent35.models.Antropometria;
import cent35.models.Tamizaje;
import cent35.repositories.AntropometriaRepository;
import cent35.repositories.TamizajeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AntropometriaController {
    private final AntropometriaRepository antropometriaRepository;
    private final TamizajeRepository tamizajeRepository;

    public AntropometriaController(AntropometriaRepository antropometriaRepository,
                                   TamizajeRepository tamizajeRepository) {
        this.antropometriaRepository = antropometriaRepository;
        this.tamizajeRepository = tamizajeRepository;
    }

    @PostMapping("/tamizajes/{idTamizaje}/antropometria")
    public ResponseEntity<Object> crear(@PathVariable Long idTamizaje,
                                        @RequestBody Antropometria antropometria) {
        Tamizaje tamizaje = tamizajeRepository.findById(idTamizaje).orElse(null);
        if (tamizaje == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Tamizaje no encontrado");
        if (!tamizaje.isConsent()) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("El paciente no acepto el consentimiento");
        if (!antropometria.validar()) return ResponseEntity.badRequest().body("Peso o talla invalidos");
        antropometria.setIdControl(null);
        antropometria.setTamizaje(tamizaje);
        Antropometria guardada = antropometriaRepository.save(antropometria);
        return ResponseEntity.status(HttpStatus.CREATED).body(new AntropometriaDTO(guardada));
    }

    @GetMapping("/tamizajes/{idTamizaje}/antropometria")
    public ResponseEntity<Object> obtener(@PathVariable Long idTamizaje) {
        Tamizaje tamizaje = tamizajeRepository.findById(idTamizaje).orElse(null);
        if (tamizaje == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Tamizaje no encontrado");
        if (tamizaje.getAntropometria() == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Antropometria no encontrada");
        return ResponseEntity.ok(new AntropometriaDTO(tamizaje.getAntropometria()));
    }
}
