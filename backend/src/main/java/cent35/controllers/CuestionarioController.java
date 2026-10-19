package cent35.controllers;

import cent35.dtos.CuestionarioDTO;
import cent35.models.Cuestionario;
import cent35.models.Tamizaje;
import cent35.repositories.CuestionarioRepository;
import cent35.repositories.TamizajeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class CuestionarioController {

    @Autowired
    private CuestionarioRepository cuestionarioRepository;

    @Autowired
    private TamizajeRepository tamizajeRepository;

    @GetMapping("/cuestionarios")
    public List<CuestionarioDTO> getCuestionarios() {
        return cuestionarioRepository.findAll()
                .stream()
                .map(CuestionarioDTO::new)
                .collect(Collectors.toList());
    }

    @GetMapping("/cuestionarios/{id}")
    public ResponseEntity<Object> getCuestionario(@PathVariable Long id) {
        Cuestionario cuestionario = cuestionarioRepository.findById(id).orElse(null);
        if (cuestionario == null) {
            return new ResponseEntity<>("Cuestionario no encontrado", HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(new CuestionarioDTO(cuestionario), HttpStatus.OK);
    }

    @GetMapping("/tamizajes/{idTamizaje}/cuestionario")
    public ResponseEntity<Object> obtenerPorTamizaje(@PathVariable Long idTamizaje) {
        Tamizaje tamizaje = tamizajeRepository.findById(idTamizaje).orElse(null);
        if (tamizaje == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Tamizaje no encontrado");
        if (tamizaje.getCuestionario() == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Cuestionario no encontrado");
        return ResponseEntity.ok(new CuestionarioDTO(tamizaje.getCuestionario()));
    }

    @PostMapping("/tamizajes/{idTamizaje}/cuestionario")
    public ResponseEntity<Object> crearCuestionario(@PathVariable Long idTamizaje, @RequestBody Cuestionario cuestionario) {

        Tamizaje tamizaje = tamizajeRepository.findById(idTamizaje).orElse(null);
        if (tamizaje == null) {
            return new ResponseEntity<>("Tamizaje no encontrado", HttpStatus.NOT_FOUND);
        }

        if (!cuestionario.validar()) {
            return new ResponseEntity<>("Cuestionario incompleto o inválido", HttpStatus.BAD_REQUEST);
        }

        cuestionario.setTamizaje(tamizaje);
        Cuestionario guardado = cuestionarioRepository.save(cuestionario);

        return new ResponseEntity<>(new CuestionarioDTO(guardado), HttpStatus.CREATED);
    }
}