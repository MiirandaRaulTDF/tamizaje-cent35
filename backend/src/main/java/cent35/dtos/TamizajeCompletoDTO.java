package cent35.dtos;

import cent35.models.Tamizaje;
import cent35.models.TomaPresion;

/** Respuesta sin entidades JPA ni referencias circulares. */
public class TamizajeCompletoDTO {
    private final TamizajeDTO tamizaje;
    private final PacienteDTO paciente;
    private final EnfermeroDTO operador;
    private final AntropometriaDTO antropometria;
    private final TomaPresionDTO tomaPresion;
    private final CuestionarioDTO cuestionario;
    private final ResultadosTamizajeDTO resultado;
    private final String tokenConsulta;

    public TamizajeCompletoDTO(Tamizaje tamizaje, TomaPresion tomaPresion) {
        this.tamizaje = new TamizajeDTO(tamizaje);
        this.paciente = new PacienteDTO(tamizaje.getPaciente());
        this.operador = new EnfermeroDTO(tamizaje.getOperador());
        this.antropometria = new AntropometriaDTO(tamizaje.getAntropometria());
        this.tomaPresion = new TomaPresionDTO(tomaPresion);
        this.cuestionario = new CuestionarioDTO(tamizaje.getCuestionario());
        this.resultado = new ResultadosTamizajeDTO(tamizaje.getResultados());
        this.tokenConsulta = tamizaje.getResultados().getTokenConsulta();
    }

    public TamizajeDTO getTamizaje() { return tamizaje; }
    public PacienteDTO getPaciente() { return paciente; }
    public EnfermeroDTO getOperador() { return operador; }
    public AntropometriaDTO getAntropometria() { return antropometria; }
    public TomaPresionDTO getTomaPresion() { return tomaPresion; }
    public CuestionarioDTO getCuestionario() { return cuestionario; }
    public ResultadosTamizajeDTO getResultado() { return resultado; }
    public String getTokenConsulta() { return tokenConsulta; }
}
