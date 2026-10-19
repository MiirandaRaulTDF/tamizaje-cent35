package cent35.services;

import cent35.dtos.DashboardResumenDTO;
import cent35.models.Antropometria;
import cent35.models.Paciente;
import cent35.models.ResultadosTamizaje;
import cent35.models.Tamizaje;
import cent35.models.TomaPresion;
import cent35.models.enums.SemaforoGlobal;
import cent35.repositories.TamizajeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DashboardResumenService {
    private final TamizajeRepository tamizajes;

    public DashboardResumenService(TamizajeRepository tamizajes) {
        this.tamizajes = tamizajes;
    }

    @Transactional(readOnly = true)
    public DashboardResumenDTO obtenerResumen() {
        List<Tamizaje> registros = tamizajes.findAll();
        long totalConResultado = 0;
        long verdes = 0;
        long amarillos = 0;
        long rojos = 0;
        long urgentes = 0;
        double sumaEdad = 0;
        long cantidadEdad = 0;
        double sumaImc = 0;
        long cantidadImc = 0;
        double sumaSistolica = 0;
        double sumaDiastolica = 0;
        long cantidadPresion = 0;

        for (Tamizaje tamizaje : registros) {
            Paciente paciente = tamizaje.getPaciente();
            if (paciente != null && paciente.getFecha_nacimiento() != null) {
                int edad = paciente.calcularEdad();
                if (edad >= 0) {
                    sumaEdad += edad;
                    cantidadEdad++;
                }
            }

            Antropometria antropometria = tamizaje.getAntropometria();
            if (antropometria != null && Boolean.TRUE.equals(antropometria.validar())) {
                sumaImc += antropometria.calcularIMC();
                cantidadImc++;
            }

            TomaPresion toma = tamizaje.obtenerUltimaTomaPresionValida();
            if (toma != null) {
                sumaSistolica += toma.getSistolicaMmHg();
                sumaDiastolica += toma.getDiastolicaMmHg();
                cantidadPresion++;
            }

            ResultadosTamizaje resultado = tamizaje.getResultados();
            if (resultado == null) continue;
            totalConResultado++;
            SemaforoGlobal semaforo = resultado.getSemaforo_global();
            if (semaforo == SemaforoGlobal.VERDE) verdes++;
            else if (semaforo == SemaforoGlobal.AMARILLO) amarillos++;
            else if (semaforo == SemaforoGlobal.ROJO) rojos++;
            if (resultado.isDerivacionInmediata()) urgentes++;
        }

        return new DashboardResumenDTO(registros.size(), totalConResultado, verdes, amarillos, rojos,
                urgentes, promedio(sumaEdad, cantidadEdad), promedio(sumaImc, cantidadImc),
                promedio(sumaSistolica, cantidadPresion), promedio(sumaDiastolica, cantidadPresion));
    }

    private Double promedio(double suma, long cantidad) {
        return cantidad == 0 ? null : Math.round((suma / cantidad) * 10.0) / 10.0;
    }
}