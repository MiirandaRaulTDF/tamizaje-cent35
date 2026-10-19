package cent35.controllers;

import cent35.dtos.ResultadosTamizajeDTO;
import cent35.models.ResultadosTamizaje;
import cent35.models.Tamizaje;
import cent35.repositories.ResultadosTamizajeRepository;
import cent35.repositories.TamizajeRepository;
import cent35.services.ResultadoTamizajeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tamizajes")
public class ResultadoTamizajeController {
    private final TamizajeRepository tamizajeRepository;
    private final ResultadosTamizajeRepository resultadosRepository;
    private final ResultadoTamizajeService resultadoService;

    public ResultadoTamizajeController(TamizajeRepository tamizajeRepository,
                                       ResultadosTamizajeRepository resultadosRepository,
                                       ResultadoTamizajeService resultadoService) {
        this.tamizajeRepository = tamizajeRepository;
        this.resultadosRepository = resultadosRepository;
        this.resultadoService = resultadoService;
    }

    @PostMapping("/{idTamizaje}/resultado")
    public ResponseEntity<Object> calcularResultado(@PathVariable Long idTamizaje) {
        Tamizaje tamizaje = tamizajeRepository.findById(idTamizaje).orElse(null);
        if (tamizaje == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Tamizaje no encontrado");
        if (!tamizaje.isConsent()) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("El paciente no acepto el consentimiento");
        if (tamizaje.getPaciente() == null || !tamizaje.getPaciente().validar()
                || tamizaje.getAntropometria() == null || !tamizaje.getAntropometria().validar()
                || tamizaje.getCuestionario() == null || !tamizaje.getCuestionario().validar()
                || tamizaje.obtenerUltimaTomaPresionValida() == null) {
            return ResponseEntity.badRequest().body("El tamizaje no tiene todos los controles validos");
        }

        ResultadosTamizaje resultado = resultadosRepository.findByTamizajeId(idTamizaje)
                .orElseGet(ResultadosTamizaje::new);
        if (resultadoService.calcular(tamizaje, resultado) == null) {
            return ResponseEntity.badRequest().body("No se pudo calcular FINDRISC");
        }
        ResultadosTamizaje guardado = resultadosRepository.save(resultado);
        return ResponseEntity.ok(new ResultadosTamizajeDTO(guardado));
    }

    @GetMapping("/{idTamizaje}/resultado")
    public ResponseEntity<Object> obtenerResultado(@PathVariable Long idTamizaje) {
        return resultadosRepository.findByTamizajeId(idTamizaje)
                .<ResponseEntity<Object>>map(resultado -> ResponseEntity.ok(new ResultadosTamizajeDTO(resultado)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Resultado no encontrado"));
    }
}
