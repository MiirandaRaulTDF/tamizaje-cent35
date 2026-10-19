package cent35;

import cent35.models.Enfermero;
import cent35.models.Paciente;
import cent35.models.Tamizaje;
import cent35.models.enums.RolEnfermero;
import cent35.models.enums.Sexo;
import cent35.models.enums.Turnos;
import cent35.repositories.AntropometriaRepository;
import cent35.repositories.CuestionarioRepository;
import cent35.repositories.EnfermeroRepository;
import cent35.repositories.PacienteRepository;
import cent35.repositories.ResultadosTamizajeRepository;
import cent35.repositories.TamizajeRepository;
import cent35.repositories.TomaPresionRepository;
import cent35.services.FindriscService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TamizajeApplication.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:tamizaje-completo-tests;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class TamizajeCompletoIntegrationTests {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private PacienteRepository pacientes;
    @Autowired private EnfermeroRepository enfermeros;
    @SpyBean private TamizajeRepository tamizajes;
    @Autowired private AntropometriaRepository antropometrias;
    @Autowired private TomaPresionRepository presiones;
    @Autowired private CuestionarioRepository cuestionarios;
    @Autowired private ResultadosTamizajeRepository resultados;
    @SpyBean private FindriscService findrisc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactions;

    @BeforeEach
    void limpiarDatos() {
        tamizajes.deleteAll();
        pacientes.deleteAll();
        enfermeros.deleteAll();
    }

    @AfterEach
    void restaurarServicios() {
        reset(tamizajes, findrisc);
    }

    @Test
    void altaNuevaGuardaTodasLasRelacionesYElMismoResultadoQueElRecalculo() throws Exception {
        ObjectNode datos = formularioValido("DIRECTO-001");
        datos.with("operador").put("nombre", " Ana ").put("apellido", " Pérez ");
        datos.with("antropometria").put("cinturacm", 80);
        JsonNode respuesta = registrar(datos);
        long id = respuesta.path("tamizaje").path("id").asLong();
        assertThat(id).isPositive();
        assertThat(respuesta.path("operador").path("nombre").asText()).isEqualTo("Ana");
        assertThat(respuesta.path("operador").path("apellido").asText()).isEqualTo("Pérez");
        assertThat(respuesta.path("operador").path("rol").asText()).isEqualTo("ENFERMERO");
        assertThat(respuesta.path("tomaPresion").path("numeroToma").asInt()).isEqualTo(1);
        assertThat(respuesta.path("antropometria").path("cinturacm").asDouble()).isEqualTo(80);
        assertThat(respuesta.path("resultado").path("puntajeFindrisc").asInt()).isEqualTo(2);
        assertThat(respuesta.path("resultado").path("semaforoGlobal").asText()).isEqualTo("VERDE");
        assertThat(conteos()).containsExactly(1, 1, 1, 1, 1, 1, 1, 3);

        new TransactionTemplate(transactions).executeWithoutResult(transaction -> {
            Tamizaje guardado = tamizajes.findById(id).orElseThrow();
            assertThat(guardado.getPaciente().getId_paciente())
                    .isEqualTo(respuesta.path("paciente").path("id_paciente").asInt());
            assertThat(guardado.getOperador().getId())
                    .isEqualTo(respuesta.path("operador").path("id").asInt());
            assertThat(guardado.getAntropometria().getIdControl())
                    .isEqualTo(respuesta.path("antropometria").path("idControl").asLong());
            assertThat(guardado.getAntropometria().getCinturacm()).isEqualTo(80);
            assertThat(guardado.getCuestionario().getIdControl())
                    .isEqualTo(respuesta.path("cuestionario").path("idControl").asLong());
            assertThat(guardado.getTomaPresiones()).hasSize(1);
            assertThat(guardado.obtenerUltimaTomaPresionValida().getIdControl())
                    .isEqualTo(respuesta.path("tomaPresion").path("idControl").asLong());
            assertThat(guardado.getResultados().getId())
                    .isEqualTo(respuesta.path("resultado").path("id").asLong());
        });

        mvc.perform(get("/api/tamizajes/{id}/antropometria", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cinturacm").value(80));

        String recalculado = mvc.perform(post("/api/tamizajes/{id}/resultado", id))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(json.readTree(recalculado)).isEqualTo(respuesta.path("resultado"));
        assertThat(resultados.count()).isEqualTo(1);
    }

    @Test
    void reutilizaPacienteYEnfermeroSinDuplicarlosYVinculaSupervisorOpcional() throws Exception {
        Paciente paciente = pacientes.save(new Paciente(LocalDate.now().minusYears(50), Sexo.F, LocalDateTime.now()));
        Enfermero enfermero = enfermeros.save(new Enfermero("Ana", "Pérez", RolEnfermero.ENFERMERO, Turnos.MAÑANA));
        Enfermero supervisor = enfermeros.save(new Enfermero("Luis", "García", RolEnfermero.SUPERVISOR, Turnos.TARDE));
        ObjectNode datos = formularioValido("EXISTENTES-001");
        datos.remove("operador");
        datos.remove("paciente");
        datos.put("operadorId", enfermero.getId());
        datos.put("pacienteId", paciente.getId_paciente());
        datos.put("supervisorId", supervisor.getId());

        JsonNode respuesta = registrar(datos);
        assertThat(respuesta.path("paciente").path("id_paciente").asInt()).isEqualTo(paciente.getId_paciente());
        assertThat(respuesta.path("operador").path("id").asInt()).isEqualTo(enfermero.getId());
        assertThat(pacientes.count()).isEqualTo(1);
        assertThat(enfermeros.count()).isEqualTo(2);
        assertThat(tamizajes.findById(respuesta.path("tamizaje").path("id").asLong())
                .orElseThrow().getSupervisor().getId()).isEqualTo(supervisor.getId());
    }

    @Test
    void codigoDuplicadoNoGuardaPersonasNiControlesAdicionales() throws Exception {
        registrar(formularioValido("DUPLICADO"));
        long[] antes = conteos();
        ObjectNode duplicado = formularioValido(" DUPLICADO ");

        mvc.perform(post("/api/tamizajes/completo").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(duplicado)))
                .andExpect(status().isConflict())
                .andExpect(content().string("El id de registro ya existe"));

        assertThat(conteos()).isEqualTo(antes);
    }

    @Test
    void carreraDeCodigoRevierteLasPersonasInsertadasAntesDeLaRestriccionUnica() throws Exception {
        registrar(formularioValido("CARRERA"));
        long[] antes = conteos();
        // Simula una consulta previa obsoleta: la restricción de la base debe ser la última defensa.
        doReturn(false, true).when(tamizajes).existsByIdRegistro("CARRERA");

        mvc.perform(post("/api/tamizajes/completo").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(formularioValido("CARRERA"))))
                .andExpect(status().isConflict());

        assertThat(conteos()).isEqualTo(antes);
    }

    @ParameterizedTest
    @ValueSource(strings = {"peso", "talla", "cinturaNegativa", "cinturaExcesiva", "cinturaNoFinita", "presionOrden", "presionRango", "edad", "sexo", "enfermero",
            "cuestionario", "antecedentes", "antecedenteNulo", "ambosPaciente", "ambosOperador", "codigo", "sinControl"})
    void formularioInvalidoNoDejaRegistrosParciales(String caso) throws Exception {
        ObjectNode datos = formularioValido("INVALIDO");
        switch (caso) {
            case "peso": datos.with("antropometria").put("pesoKg", 10); break;
            case "talla": datos.with("antropometria").remove("tallacm"); break;
            case "cinturaNegativa": datos.with("antropometria").put("cinturacm", -1); break;
            case "cinturaExcesiva": datos.with("antropometria").put("cinturacm", 301); break;
            case "cinturaNoFinita": datos.with("antropometria").put("cinturacm", "NaN"); break;
            case "presionOrden": datos.with("tomaPresion").put("diastolicaMmHg", 125); break;
            case "presionRango": datos.with("tomaPresion").put("sistolicaMmHg", 270); break;
            case "edad": datos.with("paciente").put("fechaNacimiento", LocalDate.now().minusYears(10).toString()); break;
            case "sexo": datos.with("paciente").remove("sexo"); break;
            case "enfermero": datos.with("operador").put("apellido", " "); break;
            case "cuestionario": datos.with("cuestionario").remove("actividadFisica"); break;
            case "antecedentes": datos.with("cuestionario").put("antecedenteFamiliar", "PRIMER_GRADO"); break;
            case "antecedenteNulo": datos.with("cuestionario").withArray("tiposAntecedentes").addNull(); break;
            case "ambosPaciente": datos.put("pacienteId", 1); break;
            case "ambosOperador": datos.put("operadorId", 1); break;
            case "codigo": datos.put("idRegistro", " "); break;
            case "sinControl": datos.remove("antropometria"); break;
            default: throw new IllegalArgumentException(caso);
        }

        mvc.perform(post("/api/tamizajes/completo").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(datos)))
                .andExpect(status().isBadRequest());

        assertThat(conteos()).containsOnly(0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"omitida", "nula", "cero"})
    void cinturaOpcionalSinTomarSeConservaComoCero(String caso) throws Exception {
        ObjectNode datos = formularioValido("CINTURA-OPCIONAL");
        if (caso.equals("omitida")) datos.with("antropometria").remove("cinturacm");
        else if (caso.equals("nula")) datos.with("antropometria").putNull("cinturacm");
        JsonNode respuesta = registrar(datos);
        assertThat(respuesta.path("antropometria").path("cinturacm").asDouble()).isZero();
        mvc.perform(get("/api/tamizajes/{id}/antropometria", respuesta.path("tamizaje").path("id").asLong()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cinturacm").value(0));
    }

    @Test
    void consentimientoEsObligatorioYNoSeGuardaNadaSiFalta() throws Exception {
        ObjectNode datos = formularioValido("SIN-CONSENTIMIENTO");
        datos.remove("consent");
        mvc.perform(post("/api/tamizajes/completo").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(datos)))
                .andExpect(status().isForbidden());
        assertThat(conteos()).containsOnly(0);
    }

    @Test
    void noPermiteQueUnSupervisorSeaElOperadorNiUnEnfermeroElSupervisor() throws Exception {
        Enfermero enfermero = enfermeros.save(new Enfermero("Ana", "Pérez", RolEnfermero.ENFERMERO, Turnos.MAÑANA));
        Enfermero supervisor = enfermeros.save(new Enfermero("Luis", "García", RolEnfermero.SUPERVISOR, Turnos.TARDE));
        long[] antes = conteos();
        ObjectNode operadorIncorrecto = formularioValido("ROL-OPERADOR");
        operadorIncorrecto.remove("operador");
        operadorIncorrecto.put("operadorId", supervisor.getId());
        mvc.perform(post("/api/tamizajes/completo").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(operadorIncorrecto)))
                .andExpect(status().isBadRequest());
        ObjectNode supervisorIncorrecto = formularioValido("ROL-SUPERVISOR");
        supervisorIncorrecto.put("supervisorId", enfermero.getId());
        mvc.perform(post("/api/tamizajes/completo").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(supervisorIncorrecto)))
                .andExpect(status().isBadRequest());
        assertThat(conteos()).isEqualTo(antes);
    }

    @Test
    void personaNoEncontradaNoDejaElAltaAmedias() throws Exception {
        ObjectNode datos = formularioValido("PERSONA-AUSENTE");
        datos.remove("paciente");
        datos.put("pacienteId", Integer.MAX_VALUE);
        mvc.perform(post("/api/tamizajes/completo").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(datos)))
                .andExpect(status().isNotFound());
        assertThat(conteos()).containsOnly(0);
    }

    @Test
    void falloDelCalculoNoGuardaPersonasNiFichaNiControles() throws Exception {
        doReturn(null).when(findrisc).calcularPuntaje(any(Tamizaje.class));
        mvc.perform(post("/api/tamizajes/completo").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(formularioValido("CALCULO-FALLIDO"))))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("No se pudo calcular FINDRISC"));
        assertThat(conteos()).containsOnly(0);
    }

    private ObjectNode formularioValido(String codigo) {
        ObjectNode datos = json.createObjectNode();
        datos.put("idRegistro", codigo).put("consent", true);
        datos.putObject("operador").put("nombre", "Ana").put("apellido", "Pérez").put("turno", "MAÑANA");
        datos.putObject("paciente").put("fechaNacimiento", LocalDate.now().minusYears(50).toString()).put("sexo", "F");
        datos.putObject("antropometria").put("pesoKg", 70).put("tallacm", 170).put("cinturacm", 0);
        datos.putObject("tomaPresion").put("sistolicaMmHg", 120).put("diastolicaMmHg", 80);
        ObjectNode cuestionario = datos.putObject("cuestionario").put("actividadFisica", true)
                .put("frutasVerduras", true).put("medicacion", false).put("tabaquismo", "NUNCA")
                .put("antecedenteFamiliar", "NO");
        cuestionario.putArray("tiposAntecedentes");
        return datos;
    }

    private JsonNode registrar(ObjectNode datos) throws Exception {
        String respuesta = mvc.perform(post("/api/tamizajes/completo").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(datos)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tamizaje.id").isNumber())
                .andExpect(jsonPath("$.resultado.id").isNumber())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        return json.readTree(respuesta);
    }

    private long[] conteos() {
        return new long[]{pacientes.count(), enfermeros.count(), tamizajes.count(), antropometrias.count(),
                presiones.count(), cuestionarios.count(), resultados.count(),
                jdbc.queryForObject("select count(*) from control", Long.class)};
    }
}
