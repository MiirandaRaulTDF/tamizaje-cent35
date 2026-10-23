package cent35.services;

import cent35.models.ResultadosTamizaje;
import cent35.models.Tamizaje;
import org.springframework.stereotype.Service;
import java.util.UUID;

/** Comparte el cálculo clínico entre el alta completa y el recálculo de fichas existentes. */
@Service
public class ResultadoTamizajeService {
    private final FindriscService findriscService;

    public ResultadoTamizajeService(FindriscService findriscService) {
        this.findriscService = findriscService;
    }

    public ResultadosTamizaje calcular(Tamizaje tamizaje, ResultadosTamizaje resultado) {
        Integer puntaje = findriscService.calcularPuntaje(tamizaje);
        if (puntaje == null) return null;

        resultado.setTamizaje(tamizaje);
        if (resultado.getTokenConsulta() == null || resultado.getTokenConsulta().trim().isEmpty()) {
            resultado.setTokenConsulta(UUID.randomUUID().toString().replace("-", ""));
        }
        resultado.setPuntaje_findrisc(puntaje);
        resultado.setCategoria_findrisc(findriscService.clasificarPuntaje(puntaje));
        resultado.setSemaforo_imc(tamizaje.clasificarIMC());
        resultado.setSemaforo_cintura(tamizaje.clasificarCintura());
        resultado.setSemaforo_presion(tamizaje.clasificarPresion());
        resultado.setRiesgo_cv(tamizaje.calcularRiesgoCardiovascularSimplificado());
        resultado.setSemaforo_riesgo_cv(tamizaje.clasificarRiesgoCardiovascularSimplificado());
        resultado.setDerivacionInmediata(tamizaje.requiereDerivacionInmediata());
        resultado.setSemaforo_global(tamizaje.calcularSemaforoGlobal(puntaje));
        resultado.setDeriva(tamizaje.requiereDerivacion(puntaje) || resultado.isDerivacionInmediata());
        resultado.setObservaciones(resultado.isDerivacionInmediata()
                ? "Derivacion inmediata y aviso al docente responsable."
                : resultado.isDeriva() ? "Recomendar consulta con un centro de salud."
                : "Mantener habitos saludables.");
        tamizaje.setResultados(resultado);
        return resultado;
    }
}
