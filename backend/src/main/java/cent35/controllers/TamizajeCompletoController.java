package cent35.controllers;

import cent35.dtos.TamizajeCompletoRequest;
import cent35.repositories.TamizajeRepository;
import cent35.services.TamizajeCompletoService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/tamizajes")
public class TamizajeCompletoController {
    private final TamizajeCompletoService servicio;
    private final TamizajeRepository tamizajes;

    public TamizajeCompletoController(TamizajeCompletoService servicio, TamizajeRepository tamizajes) {
        this.servicio = servicio;
        this.tamizajes = tamizajes;
    }

    @PostMapping("/completo")
    public ResponseEntity<Object> registrar(@RequestBody TamizajeCompletoRequest datos) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(servicio.registrar(datos));
        } catch (ResponseStatusException error) {
            return ResponseEntity.status(error.getStatus()).body(error.getReason());
        } catch (DataIntegrityViolationException error) {
            // El servicio transaccional ya revirtió todas sus escrituras antes de resolver una carrera.
            if (datos.idRegistro != null && tamizajes.existsByIdRegistro(datos.idRegistro.trim())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("El id de registro ya existe");
            }
            throw error;
        }
    }
}
