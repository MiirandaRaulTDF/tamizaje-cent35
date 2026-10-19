package cent35.controllers;

import cent35.dtos.TomaPresionDTO;
import cent35.models.Tamizaje;
import cent35.models.TomaPresion;
import cent35.repositories.TamizajeRepository;
import cent35.repositories.TomaPresionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class TomaPresionController {

    private final TomaPresionRepository tomaPresionRepository;
    private final TamizajeRepository tamizajeRepository;

    public TomaPresionController(TomaPresionRepository tomaPresionRepository,
                                 TamizajeRepository tamizajeRepository) {
        this.tomaPresionRepository = tomaPresionRepository;
        this.tamizajeRepository = tamizajeRepository;
    }

    @GetMapping("/tamizajes/{idTamizaje}/tomas-presion")
    public List<TomaPresionDTO> listarPorTamizaje(@PathVariable Long idTamizaje) {
        return tomaPresionRepository.findByTamizajeIdOrderByNumeroTomaAsc(idTamizaje)
                .stream()
                .map(TomaPresionDTO::new)
                .collect(Collectors.toList());
    }

    @GetMapping("/tomas-presion/{id}")
    public ResponseEntity<Object> obtener(@PathVariable Long id) {
        TomaPresion toma = tomaPresionRepository.findById(id).orElse(null);
        if (toma == null) {
            return new ResponseEntity<>("Toma de presión no encontrada", HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(new TomaPresionDTO(toma), HttpStatus.OK);
    }

    @PostMapping("/tamizajes/{idTamizaje}/tomas-presion")
    public ResponseEntity<Object> crear(@PathVariable Long idTamizaje,
                                        @RequestBody TomaPresion toma) {
        Tamizaje tamizaje = tamizajeRepository.findById(idTamizaje).orElse(null);
        if (tamizaje == null) {
            return new ResponseEntity<>("Tamizaje no encontrado", HttpStatus.NOT_FOUND);
        }
        if (!toma.validar()) {
            return new ResponseEntity<>("Valores de presión inválidos", HttpStatus.BAD_REQUEST);
        }

        toma.setIdControl(null);
        toma.setTamizaje(tamizaje);

        toma.setNumeroToma((int) tomaPresionRepository.countByTamizajeId(idTamizaje) + 1);

        TomaPresion guardada = tomaPresionRepository.save(toma);
        return new ResponseEntity<>(new TomaPresionDTO(guardada), HttpStatus.CREATED);
    }

    @PutMapping("/tomas-presion/{id}")
    public ResponseEntity<Object> actualizar(@PathVariable Long id,
                                             @RequestBody TomaPresion datos) {
        TomaPresion existente = tomaPresionRepository.findById(id).orElse(null);
        if (existente == null) {
            return new ResponseEntity<>("Toma de presión no encontrada", HttpStatus.NOT_FOUND);
        }
        existente.setSistolicaMmHg(datos.getSistolicaMmHg());
        existente.setDiastolicaMmHg(datos.getDiastolicaMmHg());
        if (!existente.validar()) {
            return new ResponseEntity<>("Valores de presión inválidos", HttpStatus.BAD_REQUEST);
        }
        TomaPresion guardada = tomaPresionRepository.save(existente);
        return new ResponseEntity<>(new TomaPresionDTO(guardada), HttpStatus.OK);
    }

    @DeleteMapping("/tomas-presion/{id}")
    public ResponseEntity<Object> eliminar(@PathVariable Long id) {
        if (!tomaPresionRepository.existsById(id)) {
            return new ResponseEntity<>("Toma de presión no encontrada", HttpStatus.NOT_FOUND);
        }
        tomaPresionRepository.deleteById(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}