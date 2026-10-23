import {
  createApp,
  ref,
  reactive,
  computed,
  onMounted,
  onUnmounted,
  nextTick,
  watch,
} from "../vendor/vue/vue.esm-browser.prod.js";
import {
  request,
  optionalRequest,
  label,
  age,
  sexLabel,
  dateFormat,
  badgeClass,
  validatePatient,
  validatePressure,
  validateAnthropometry,
  normalizeQuestionnaire,
  buildDirectRegistrationPayload,
  validateDirectRegistrationStep,
} from "./api.js?v=20261019-family-history";

function freshDirectForm() {
  const suffix =
    globalThis.crypto?.randomUUID?.().slice(0, 8) ||
    Math.random().toString(36).slice(2, 10);
  return {
    operadorMode: "existente",
    pacienteMode: "nuevo",
    operadorId: "",
    operador: { nombre: "", apellido: "", turno: "" },
    paciente: { fechaNacimiento: "", sexo: "" },
    idRegistro: `TAM-${new Date().toISOString().slice(0, 10).replaceAll("-", "")}-${suffix.toUpperCase()}`,
    consent: false,
    supervisor: "",
    antropometria: { pesoKg: "", tallacm: "", cinturacm: "" },
    tomaPresion: { sistolicaMmHg: "", diastolicaMmHg: "" },
    cuestionario: {
      actividadFisica: "",
      frutasVerduras: "",
      medicacion: "",
      tabaquismo: "",
      antecedenteFamiliar: "",
      tiposAntecedentes: [],
      antecedentesFamiliares: [],
    },
  };
}


function globalGuidanceFor(signal, urgent) {
  if (urgent) return {
    title: "Rojo urgente · Atención inmediata",
    description: "Se detectó un criterio de urgencia según las reglas de esta jornada.",
    recommendation: "El resultado requiere atención inmediata. Avisá al profesional o docente responsable del puesto y seguí sus indicaciones para acceder a la atención necesaria.",
    whyTitle: "¿Por qué actuar ahora?",
    why: "Algunas alteraciones importantes pueden necesitar evaluación rápida. La prioridad y la conducta deben ser definidas por Enfermería. Si hay síntomas graves, se debe solicitar asistencia de emergencia.",
  };
  if (signal === "ROJO") return {
    title: "Rojo · Se recomienda una evaluación profesional",
    description: "Al menos un control alcanzó un criterio de alerta.",
    recommendation: "Uno o más resultados alcanzaron un nivel de alerta establecido para esta jornada. Te recomendamos acudir a un centro de salud para que un profesional evalúe las mediciones y determine los pasos a seguir.",
    whyTitle: "¿Por qué consultar?",
    why: "Una medición alterada puede requerir confirmación, seguimiento o estudios complementarios. El tamizaje identifica señales que merecen atención, pero no confirma un diagnóstico.",
  };
  if (signal === "AMARILLO") return {
    title: "Amarillo · Conviene realizar un control",
    description: "Uno o más resultados requieren seguimiento.",
    recommendation: "Uno o más controles presentaron valores que conviene revisar. Esto no significa que tengas una enfermedad; es una oportunidad para conocer mejor tu estado de salud. Te recomendamos solicitar una consulta en un centro de salud.",
    whyTitle: "¿Por qué consultar?",
    why: "Algunos factores de riesgo pueden no producir síntomas. Un profesional puede interpretar tus resultados junto con tus antecedentes y ayudarte a decidir qué seguimiento necesitás.",
  };
  if (signal === "NARANJA") return {
    title: "Naranja · Conviene una evaluación profesional",
    description: "El promedio de los indicadores muestra varias señales que conviene revisar.",
    recommendation: "El resultado combina distintos niveles de alerta. Consultá con un profesional para revisar cada medición y definir el seguimiento adecuado.",
    whyTitle: "¿Cómo se obtiene este color?",
    why: "El color global promedia los controles disponibles. Las alertas rojas individuales y los criterios de atención inmediata se siguen mostrando por separado.",
  };
  if (signal === "VERDE") return {
    title: "Verde · Resultado favorable",
    description: "No se detectaron alertas en los controles realizados.",
    recommendation: "Tus resultados se encuentran dentro de los rangos considerados favorables para los controles realizados. Seguí cuidando tu salud mediante una alimentación equilibrada, actividad física regular, evitando el tabaco y realizando controles preventivos.",
    whyTitle: "¿Necesitás ir a un hospital o clínica?",
    why: "Este resultado no indica una derivación obligatoria por los controles realizados. Sin embargo, no descarta todas las enfermedades ni reemplaza una evaluación médica. Si tenés síntomas, antecedentes relevantes o dudas sobre tu salud, consultá con un profesional.",
  };
  return {
    title: "Resultado no disponible",
    description: "No se pudo clasificar el resultado global.",
    recommendation: "Compartí este resultado con el equipo de salud para recibir orientación.",
    whyTitle: "Información",
    why: "El tamizaje es orientativo y no reemplaza una consulta profesional.",
  };
}

createApp({
  setup() {
    const page = ref("registro"),
      query = ref(""),
      loading = ref(true),
      loadError = ref(""),
      notice = ref("");
    const savedTheme = localStorage.getItem("tamizaje-theme");
    const theme = ref(
      ["natural", "ocean", "lavender", "gold", "dark"].includes(savedTheme)
        ? savedTheme
        : "natural",
    );
    watch(
      theme,
      (value) => {
        localStorage.setItem("tamizaje-theme", value);
        document.getElementById("app")?.setAttribute("data-theme", value);
        const themeColors = {
          natural: "#3d593f",
          ocean: "#315f73",
          lavender: "#67577d",
          gold: "#805c3d",
          dark: "#171d1a",
        };
        document
          .querySelector('meta[name="theme-color"]')
          ?.setAttribute("content", themeColors[value]);
      },
      { immediate: true },
    );
    const busy = ref(false),
      formError = ref(""),
      modalKind = ref("");
    const editingEntity = ref(null);
    const patients = ref([]),
      staff = ref([]),
      records = ref([]),
      record = ref(null);
    const detailLoading = ref(false),
      detailError = ref(""),
      tab = ref("antropometria");
    const anthropometry = ref(null),
      pressure = ref([]),
      savedQuestionnaire = ref(null),
      result = ref(null);
    const editingPressure = ref(null),
      resultOutdated = ref(false);
    const directForm = reactive(freshDirectForm());
    const directStep = ref(0);
    const hasFamilyHistory = ref(false);
    const directMaxStep = ref(0);
    const directSteps = [
      { title: "Atención", description: "Elegí al enfermero, cargá los datos del paciente y registrá el consentimiento." },
      { title: "Controles", description: "Registrá antropometría y presión arterial." },
      { title: "Hábitos", description: "Completá las respuestas y antecedentes junto al paciente." },
      { title: "Revisar y guardar", description: "Revisá los datos antes de guardar la atención completa." },
    ];
    const directStepDescription = computed(
      () => directSteps[directStep.value].description,
    );
    const directProgress = computed(
      () => (directStep.value + 1) / directSteps.length,
    );
    const dashboard = ref(null);
    const dashboardRefreshing = ref(false);
    const dashboardRefreshError = ref("");
    const dashboardUpdatedAt = ref(null);
    const dashboardSignalTotal = computed(() =>
      Number(dashboard.value?.semaforoVerde || 0) +
      Number(dashboard.value?.semaforoAmarillo || 0) +
      Number(dashboard.value?.semaforoNaranja || 0) +
      Number(dashboard.value?.semaforoRojo || 0)
    );
    const dashboardSignalChart = computed(() => {
      const total = dashboardSignalTotal.value;
      if (!total) return "conic-gradient(#e7ece8 0deg 360deg)";
      const green = Number(dashboard.value?.semaforoVerde || 0) / total * 360;
      const yellow = green + Number(dashboard.value?.semaforoAmarillo || 0) / total * 360;
      const orange = yellow + Number(dashboard.value?.semaforoNaranja || 0) / total * 360;
      return `conic-gradient(#278455 0deg ${green}deg, #e7ad28 ${green}deg ${yellow}deg, #e97824 ${yellow}deg ${orange}deg, #c9363e ${orange}deg 360deg)`;
    });
    const signalStyle = (score) => {
      const value = Number(score);
      if (!Number.isFinite(value)) return {};
      const stops = [[0, 145, 76, 91], [.75, 60, 88, 86], [1, 32, 88, 85], [2, 10, 76, 82], [3, 0, 72, 75]];
      const bounded = Math.min(3, Math.max(0, value));
      const upper = stops.findIndex(([at]) => at >= bounded);
      const [fromAt, fromHue, fromSat, fromLight] = stops[Math.max(0, upper - 1)];
      const [toAt, toHue, toSat, toLight] = stops[Math.max(0, upper)];
      const amount = toAt === fromAt ? 0 : (bounded - fromAt) / (toAt - fromAt);
      const hue = Math.round(fromHue + (toHue - fromHue) * amount);
      const saturation = Math.round(fromSat + (toSat - fromSat) * amount);
      const lightness = Math.round(fromLight + (toLight - fromLight) * amount);
      return { backgroundColor: `hsl(${hue} ${saturation}% ${lightness}%)`, color: `hsl(${hue} ${Math.min(75, saturation)}% 27%)`, borderColor: `hsl(${hue} ${Math.min(65, saturation)}% ${Math.max(45, lightness - 17)}%)` };
    };
    const dashboardBarWidth = (value, maximum) => {
      const numeric = Number(value);
      if (!Number.isFinite(numeric) || numeric <= 0) return "0%";
      return `${Math.min(100, numeric / maximum * 100)}%`;
    };
    const directError = ref("");
    const directSaved = ref(null);
    const directGlobalSignal = computed(() => {
      const result = directSaved.value?.resultado;
      if (result?.derivacionInmediata) return "ROJO";
      return result?.semaforoGlobal || "SIN_CLASIFICAR";
    });
    const directGlobalSignalLabel = computed(() =>
      directGlobalSignal.value === "SIN_CLASIFICAR"
        ? "Pendiente"
        : label(directGlobalSignal.value),
    );
    const directResultGuidance = computed(() =>
      globalGuidanceFor(
        directGlobalSignal.value,
        Boolean(directSaved.value?.resultado?.derivacionInmediata),
      ),
    );

    const directConsultUrl = computed(() => {
      const token = directSaved.value?.tokenConsulta;
      return token
        ? `${window.location.origin}/resultado.html?token=${encodeURIComponent(token)}`
        : "";
    });
    const directQrImageUrl = computed(() => {
      const token = directSaved.value?.tokenConsulta;
      if (!token) return "";
      const baseUrl = `${window.location.origin}/resultado.html`;
      return `/api/consulta-publica/${encodeURIComponent(token)}/qr?baseUrl=${encodeURIComponent(baseUrl)}`;
    });
    const qrNeedsNetworkAddress = computed(() =>
      ["localhost", "127.0.0.1", "::1"].includes(window.location.hostname),
    );
    const directMode = computed(() => page.value === "registro");
    const patientForm = reactive({ fecha_nacimiento: "", sexo: "" });
    const staffForm = reactive({
      nombre: "",
      apellido: "",
      rol: "",
      turno: "",
    });
    const screeningForm = reactive({
      id_registro: "",
      paciente: "",
      operador: "",
      supervisor: "",
      consent: false,
    });
    const anthroForm = reactive({ pesoKg: "", tallacm: "", cinturacm: "" });
    const pressureForm = reactive({ sistolicaMmHg: "", diastolicaMmHg: "" });
    const questionForm = reactive({
      actividadFisica: "",
      frutasVerduras: "",
      medicacion: "",
      tabaquismo: "",
      antecedenteFamiliar: "",
      tiposAntecedentes: [],
    });
    const sections = [
      { key: "registro", title: "Registro por pasos", icon: "＋" },
      { key: "inicio", title: "Resumen", icon: "◈" },
      { key: "tamizajes", title: "Historial de tamizajes", icon: "▤" },
      { key: "pacientes", title: "Pacientes", icon: "♧" },
      { key: "equipo", title: "Equipo de salud", icon: "✚" },
    ];
    const sexes = [
      { value: "F", label: "Femenino" },
      { value: "M", label: "Masculino" },
      { value: "O", label: "Otro" },
    ];
    const options = (values) =>
      values.map((value) => ({ value, label: label(value) }));
    const roles = options(["ENFERMERO", "SUPERVISOR"]),
      shifts = options(["MAÑANA", "TARDE", "VESPERTINO"]);
    const tobacco = [
      { value: "NUNCA", label: "Nunca fumó" },
      { value: "EX", label: "Exfumador" },
      { value: "ACTUAL", label: "Fumador actual" },
    ];
    const formatDashboardMetric = (value) => Number.isFinite(value) ? Number(value).toFixed(1) : "—";
    const families = [
      { value: "NO", label: "Sin antecedentes" },
      { value: "SEGUNDO_GRADO", label: "Abuelos, tíos o primos" },
      { value: "PRIMER_GRADO", label: "Padres, hermanos o hijos" },
    ];
    const antecedentTypes = options([
      "DIABETES",
      "HIPERTENSION",
      "ENFERMEDAD_CARDIOVASCULAR",
      "OTRO",
    ]);
    const familyRelationships = options(["PADRE_MADRE", "HERMANO_HERMANA", "HIJO_HIJA", "ABUELO_ABUELA", "TIO_TIA", "PRIMO_PRIMA", "OTRO"]);
    function addFamilyHistory() { directForm.cuestionario.antecedentesFamiliares.push({ tipo: "", parentesco: "" }); }
    function removeFamilyHistory(index) { directForm.cuestionario.antecedentesFamiliares.splice(index, 1); }
    function setFamilyHistoryPresent() {
      if (directForm.cuestionario.antecedenteFamiliar === "NO") directForm.cuestionario.antecedentesFamiliares = [];
      directForm.cuestionario.antecedenteFamiliar = "SEGUNDO_GRADO";
      hasFamilyHistory.value = true;
      if (!directForm.cuestionario.antecedentesFamiliares.length) addFamilyHistory();
    }
    function clearFamilyHistory() {
      directForm.cuestionario.antecedenteFamiliar = "NO";
      hasFamilyHistory.value = false;
      directForm.cuestionario.antecedentesFamiliares = [];
      directForm.cuestionario.tiposAntecedentes = [];
    }
    const operators = computed(() =>
      staff.value.filter((person) => person.rol === "ENFERMERO"),
    );
    const supervisors = computed(() =>
      staff.value.filter((person) => person.rol === "SUPERVISOR"),
    );

    const directPatientAge = computed(() => age(directForm.paciente.fechaNacimiento));
    const directBmi = computed(() => {
      const weight = Number(directForm.antropometria.pesoKg);
      const heightCm = Number(directForm.antropometria.tallacm);
      if (!Number.isFinite(weight) || !Number.isFinite(heightCm) || weight <= 0 || heightCm <= 0)
        return null;
      const heightM = heightCm / 100;
      return weight / (heightM * heightM);
    });
    const directReview = computed(() => {
      const operador =
        directForm.operadorMode === "nuevo"
          ? directForm.operador
          : operators.value.find(
              (person) => person.id === Number(directForm.operadorId),
            );
      const paciente = {
        fecha_nacimiento: directForm.paciente.fechaNacimiento,
        sexo: directForm.paciente.sexo,
      };
      const answers = directForm.cuestionario;
      const booleanLabel = (value) =>
        value === true || value === "true"
          ? "Sí"
          : value === false || value === "false"
            ? "No"
            : "Sin responder";
      const familyTypes =
        answers.antecedenteFamiliar !== "NO" && answers.antecedentesFamiliares.length
          ? ` (${answers.antecedentesFamiliares.map((item) => `${label(item.tipo)} en ${label(item.parentesco)}`).join("; ")})`
          : "";
      return {
        operador: operador
          ? `${operador.nombre.trim()} ${operador.apellido.trim()} · ${label(operador.turno)}`
          : "Sin seleccionar",
        paciente: paciente
          ? `Paciente nuevo · ${dateFormat(paciente.fecha_nacimiento)} · ${directPatientAge.value ?? "—"} años · ${sexLabel(paciente.sexo)}`
          : "Sin seleccionar",
        registro: `Número automático al guardar · Consentimiento ${directForm.consent ? "aceptado" : "pendiente"}`,
        supervisor: directForm.supervisor.trim() || "Sin supervisor",
        antropometria: `${directForm.antropometria.pesoKg} kg · ${directForm.antropometria.tallacm} cm de talla · ${directForm.antropometria.cinturacm} cm de cintura`,
        presion: `${directForm.tomaPresion.sistolicaMmHg} / ${directForm.tomaPresion.diastolicaMmHg} mmHg`,
        cuestionario: `Actividad física: ${booleanLabel(answers.actividadFisica)}. Frutas y verduras: ${booleanLabel(answers.frutasVerduras)}. Medicación: ${booleanLabel(answers.medicacion)}. Tabaquismo: ${label(answers.tabaquismo)}. Familiares: ${label(answers.antecedenteFamiliar)}${familyTypes}.`,
      };
    });
    const canCreateScreening = computed(
      () => patients.value.length > 0 && operators.value.length > 0,
    );
    const canCalculateResult = computed(() =>
      Boolean(
        record.value?.consent &&
        !detailLoading.value &&
        !detailError.value &&
        anthropometry.value &&
        savedQuestionnaire.value &&
        pressure.value.length,
      ),
    );
    const pageTitle = computed(() =>
      record.value
        ? "Ficha de tamizaje"
        : {
            registro: "Registrar atención",
            inicio: "Una mirada a la salud de tu comunidad",
            pacientes: "Pacientes",
            equipo: "Equipo de salud",
            tamizajes: "Tamizajes",
          }[page.value],
    );
    const pageDescription = computed(() =>
      record.value
        ? `Registro ${record.value.id_registro}`
        : page.value === "registro"
          ? "Completá la atención paso a paso y finalizá el registro."
          : "Registrá, evaluá y acompañá cada paso de la atención.",
    );
    const modalTitle = computed(
      () =>
        ({
          paciente: editingEntity.value ? "Editar paciente" : "Registrar paciente",
          enfermero: editingEntity.value ? "Editar integrante" : "Agregar profesional",
          "direct-enfermero": "Agregar enfermero",
          tamizaje: editingEntity.value ? "Editar tamizaje" : "Iniciar tamizaje",
        })[modalKind.value] || "Registrar",
    );
    const matches = (value) =>
      JSON.stringify(value)
        .toLocaleLowerCase("es")
        .includes(query.value.trim().toLocaleLowerCase("es"));
    const filteredPatients = computed(() => patients.value.filter(matches));
    const filteredStaff = computed(() => staff.value.filter(matches));
    const filteredRecords = computed(() => records.value.filter(matches));
    const recentRecords = computed(() =>
      [...filteredRecords.value].sort((a, b) => b.id - a.id).slice(0, 5),
    );
    let registrationModal, launcher;
    let reloadVersion = 0;
    let dashboardInterval = null;
    let dashboardRequestId = 0;
    let dashboardRefreshPromise = null;

    function resetDirectRegistration() {
      const previousOperator = directSaved.value?.operador?.id;
      Object.assign(directForm, freshDirectForm());
      hasFamilyHistory.value = false;
      if (
        previousOperator &&
        operators.value.some((person) => person.id === previousOperator)
      ) {
        directForm.operadorMode = "existente";
        directForm.operadorId = String(previousOperator);
      }
      directSaved.value = null;
      directError.value = "";
      directStep.value = 0;
      directMaxStep.value = 0;
    }
    function startDirectRegistration() {
      if (busy.value) return;
      resetDirectRegistration();
      notice.value = "";
      navigate("registro");
      focusDirectStep();
    }
    async function focusDirectStep(error = false) {
      await nextTick();
      const target = document.getElementById(
        error ? "direct-error" : "direct-step-heading",
      );
      target?.focus({ preventScroll: true });
      document
        .getElementById("direct-wizard")
        ?.scrollIntoView({ behavior: "smooth", block: "start" });
    }
    function validateDirectStepsThrough(last) {
      for (let index = 0; index <= last; index++) {
        try {
          validateDirectRegistrationStep(directForm, index);
        } catch (error) {
          directStep.value = index;
          directError.value = error.message;
          focusDirectStep(true);
          return false;
        }
      }
      return true;
    }
    function goToDirectStep(index) {
      if (
        busy.value ||
        directSaved.value ||
        !Number.isInteger(index) ||
        index < 0 ||
        index > directMaxStep.value
      )
        return;
      directError.value = "";
      if (index > directStep.value && !validateDirectStepsThrough(index - 1))
        return;
      directStep.value = index;
      focusDirectStep();
    }
    function previousDirectStep() {
      goToDirectStep(directStep.value - 1);
    }
    async function submitDirectStep() {
      if (busy.value || loading.value || loadError.value || directSaved.value)
        return;
      directError.value = "";
      if (directStep.value === directSteps.length - 1) {
        await saveDirectRegistration();
        return;
      }
      try {
        validateDirectRegistrationStep(directForm, directStep.value);
        const form = document.getElementById("direct-registration-form");
        if (form && !form.reportValidity()) return;
        directStep.value += 1;
        directMaxStep.value = Math.max(directMaxStep.value, directStep.value);
        focusDirectStep();
      } catch (error) {
        directError.value = error.message;
        focusDirectStep(true);
      }
    }
    async function saveDirectRegistration() {
      if (
        busy.value ||
        directSaved.value ||
        directStep.value !== directSteps.length - 1
      )
        return;
      if (!validateDirectStepsThrough(directSteps.length - 2)) return;
      busy.value = true;
      directError.value = "";
      try {
        const body = buildDirectRegistrationPayload(directForm);
        const saved = await request("/tamizajes/completo", {
          method: "POST",
          body,
        });
        directSaved.value = saved;
        notice.value =
          "Atención guardada. Enfermero, paciente y controles quedaron vinculados correctamente.";
        setRoute("registro");
        await reload();
        await nextTick();
        document
          .getElementById("direct-result")
          ?.scrollIntoView({ behavior: "smooth", block: "start" });
        document
          .getElementById("direct-result")
          ?.focus({ preventScroll: true });
      } catch (error) {
        directError.value = error.message;
        focusDirectStep(true);
      } finally {
        busy.value = false;
      }
    }
    function printDirectResult() {
      if (directSaved.value?.tokenConsulta) window.print();
    }
    function setRoute(hash) {
      window.history.replaceState(
        null,
        "",
        `${location.pathname}${location.search}#${hash}`,
      );
    }
    function navigate(key) {
      if (!sections.some((section) => section.key === key)) return;
      page.value = key;
      record.value = null;
      query.value = "";
      formError.value = "";
      detailError.value = "";
      setRoute(key);
    }
    async function reload() {
      const version = ++reloadVersion;
      loading.value = true;
      loadError.value = "";
      try {
        const [newPatients, newStaff, newRecords, newDashboard] = await Promise.all([
          request("/pacientes"),
          request("/enfermeros"),
          request("/tamizajes"),
          request("/dashboard/resumen"),
        ]);
        if (version !== reloadVersion) return;
        patients.value = newPatients;
        staff.value = newStaff;
        records.value = newRecords;
        dashboard.value = newDashboard;
      } catch (error) {
        if (version === reloadVersion) loadError.value = error.message;
      } finally {
        if (version === reloadVersion) loading.value = false;
      }
    }
    async function refreshDashboard() {
      if (page.value !== "inicio" || dashboardRefreshPromise || document.visibilityState === "hidden") return;
      const requestId = ++dashboardRequestId;
      dashboardRefreshing.value = true;
      dashboardRefreshError.value = "";
      dashboardRefreshPromise = Promise.all([request("/pacientes"), request("/enfermeros"), request("/tamizajes"), request("/dashboard/resumen")]);
      try {
        const [newPatients, newStaff, newRecords, newDashboard] = await dashboardRefreshPromise;
        if (requestId !== dashboardRequestId || page.value !== "inicio") return;
        patients.value = newPatients;
        staff.value = newStaff;
        records.value = newRecords;
        dashboard.value = newDashboard;
        dashboardUpdatedAt.value = new Date();
      } catch (error) {
        if (requestId === dashboardRequestId && page.value === "inicio") dashboardRefreshError.value = error.message;
      } finally {
        if (requestId === dashboardRequestId) {
          dashboardRefreshPromise = null;
          dashboardRefreshing.value = false;
        }
      }
    }
    function onDashboardVisibilityChange() {
      if (document.visibilityState === "visible" && page.value === "inicio") refreshDashboard();
    }
    function stopDashboardRefresh() {
      if (dashboardInterval !== null) window.clearInterval(dashboardInterval);
      dashboardInterval = null;
      dashboardRequestId += 1;
      dashboardRefreshPromise = null;
      dashboardRefreshing.value = false;
      document.removeEventListener("visibilitychange", onDashboardVisibilityChange);
    }
    function startDashboardRefresh() {
      stopDashboardRefresh();
      dashboardRefreshError.value = "";
      refreshDashboard();
      dashboardInterval = window.setInterval(refreshDashboard, 60_000);
      document.addEventListener("visibilitychange", onDashboardVisibilityChange);
    }
    async function openModal(kind, entity = null) {
      if (busy.value) return;
      if (kind === "tamizaje" && !entity) {
        startDirectRegistration();
        return;
      }
      launcher = document.activeElement;
      formError.value = "";
      modalKind.value = kind;
      editingEntity.value = entity;
      Object.assign(patientForm, { fecha_nacimiento: "", sexo: "" });
      Object.assign(staffForm, {
        nombre: "",
        apellido: "",
        rol: "",
        turno: "",
      });
      if (kind === "direct-enfermero") staffForm.rol = "ENFERMERO";
      Object.assign(screeningForm, {
        id_registro: "",
        paciente: "",
        operador: "",
        supervisor: "",
        consent: false,
      });
      if (entity && kind === "paciente") Object.assign(patientForm, { fecha_nacimiento: entity.fecha_nacimiento, sexo: entity.sexo });
      if (entity && kind === "enfermero") Object.assign(staffForm, { nombre: entity.nombre, apellido: entity.apellido, rol: entity.rol, turno: entity.turno });
      if (entity && kind === "tamizaje") Object.assign(screeningForm, { id_registro: entity.id_registro, consent: entity.consent });
      await nextTick();
      registrationModal.show();
    }
    function closeModal() {
      if (!busy.value) registrationModal.hide();
    }
    async function saveModal() {
      if (busy.value) return;
      busy.value = true;
      formError.value = "";
      try {
        const kind = modalKind.value;
        if (kind === "paciente") {
          await request(editingEntity.value ? `/pacientes/${editingEntity.value.id_paciente}` : "/pacientes", {
            method: editingEntity.value ? "PUT" : "POST",
            params: validatePatient(patientForm),
          });
        } else if (kind === "enfermero" || kind === "direct-enfermero") {
          const values = {
            ...staffForm,
            nombre: staffForm.nombre.trim(),
            apellido: staffForm.apellido.trim(),
          };
          if (kind === "direct-enfermero") values.rol = "ENFERMERO";
          if (
            !values.nombre ||
            !values.apellido ||
            !roles.some((x) => x.value === values.rol) ||
            !shifts.some((x) => x.value === values.turno)
          )
            throw new Error("Completá todos los datos del profesional.");
          const savedStaff = await request(editingEntity.value ? `/enfermeros/${editingEntity.value.id}` : "/enfermeros", { method: editingEntity.value ? "PUT" : "POST", params: values });
          if (kind === "direct-enfermero") {
            await reload();
            const savedId = savedStaff?.id ?? savedStaff?.id_enfermero ?? operators.value.find((person) =>
              person.nombre.trim() === values.nombre &&
              person.apellido.trim() === values.apellido &&
              person.turno === values.turno,
            )?.id;
            if (savedId == null)
              throw new Error("El enfermero se guardó, pero no pude seleccionarlo. Actualizá la lista y elegilo manualmente.");
            directForm.operadorMode = "existente";
            directForm.operadorId = String(savedId);
          }
        } else if (kind === "tamizaje") {
          if (editingEntity.value) {
            await request(`/tamizajes/${editingEntity.value.id}`, { method: "PUT", body: { id_registro: screeningForm.id_registro.trim(), consent: screeningForm.consent } });
            registrationModal.hide();
            notice.value = "Tamizaje actualizado.";
            await reload();
            return;
          }
          if (!screeningForm.consent)
            throw new Error("El paciente debe aceptar el consentimiento.");
          const code = screeningForm.id_registro.trim();
          if (!code) throw new Error("Ingresá un identificador de registro.");
          if (
            !patients.value.some(
              (p) => p.id_paciente === Number(screeningForm.paciente),
            )
          )
            throw new Error("Seleccioná un paciente registrado.");
          if (
            !operators.value.some(
              (p) => p.id === Number(screeningForm.operador),
            )
          )
            throw new Error("Seleccioná un operador con rol enfermero.");
          if (
            screeningForm.supervisor &&
            !supervisors.value.some(
              (p) => p.id === Number(screeningForm.supervisor),
            )
          )
            throw new Error("Seleccioná un supervisor válido.");
          await request("/tamizajes", {
            method: "POST",
            body: {
              id_registro: code,
              consent: true,
              paciente: { id_paciente: Number(screeningForm.paciente) },
              operador: { id: Number(screeningForm.operador) },
              supervisor: screeningForm.supervisor
                ? { id: Number(screeningForm.supervisor) }
                : null,
            },
          });
          const updated = await request("/tamizajes");
          records.value = updated;
          const created = updated.find((row) => row.id_registro === code);
          if (!created)
            throw new Error(
              "La ficha fue guardada. Actualizá la lista para abrirla.",
            );
          await openRecord(created);
        } else return;
        registrationModal.hide();
        notice.value =
          kind === "tamizaje"
            ? "Ficha creada. Completá los controles."
            : kind === "direct-enfermero"
              ? "Enfermero agregado y seleccionado para esta atención."
            : "Registro guardado correctamente.";
        await reload();
      } catch (error) {
        formError.value = error.message;
      } finally {
        busy.value = false;
      }
    }
    async function deleteEntity(kind, entity) {
      if (busy.value) return;
      const id = kind === "paciente" ? entity.id_paciente : entity.id;
      const endpoint = kind === "paciente" ? `/pacientes/${id}` : kind === "enfermero" ? `/enfermeros/${id}` : `/tamizajes/${id}`;
      const labelKind = kind === "paciente" ? "paciente" : kind === "enfermero" ? "integrante del equipo" : "tamizaje";
      const linkedWarning = kind === "tamizaje" ? "También se eliminarán sus controles y resultado asociados." : "También se eliminarán sus tamizajes vinculados, controles y resultados.";
      if (!window.confirm(`¿Eliminar este ${labelKind}? ${linkedWarning} Esta acción no se puede deshacer.`)) return;
      busy.value = true;
      formError.value = "";
      try {
        await request(endpoint, { method: "DELETE" });
        if (kind === "tamizaje" && record.value?.id === id) backToRecords();
        notice.value = `Se eliminó el ${labelKind} y sus datos asociados.`;
        await reload();
      } catch (error) {
        window.alert(error.message);
      } finally {
        busy.value = false;
      }
    }
    async function loadRecord() {
      if (!record.value) return;
      const id = record.value.id;
      detailLoading.value = true;
      detailError.value = "";
      try {
        const base = `/tamizajes/${id}`;
        const [anthro, takes, questions, report] = await Promise.all([
          optionalRequest(`${base}/antropometria`),
          request(`${base}/tomas-presion`),
          optionalRequest(`${base}/cuestionario`),
          optionalRequest(`${base}/resultado`),
        ]);
        if (record.value?.id !== id) return;
        anthropometry.value = anthro;
        pressure.value = takes;
        savedQuestionnaire.value = questions;
        result.value = report;
      } catch (error) {
        if (record.value?.id === id) detailError.value = error.message;
      } finally {
        if (record.value?.id === id) detailLoading.value = false;
      }
    }
    async function openRecord(row) {
      page.value = "tamizajes";
      record.value = row;
      query.value = "";
      tab.value = "antropometria";
      anthropometry.value = null;
      pressure.value = [];
      savedQuestionnaire.value = null;
      result.value = null;
      resultOutdated.value = false;
      detailError.value = "";
      formError.value = "";
      cancelPressureEdit();
      Object.assign(anthroForm, { pesoKg: "", tallacm: "", cinturacm: "" });
      Object.assign(questionForm, {
        actividadFisica: "",
        frutasVerduras: "",
        medicacion: "",
        tabaquismo: "",
        antecedenteFamiliar: "",
        tiposAntecedentes: [],
      });
      setRoute(`tamizajes/${row.id}`);
      await loadRecord();
    }
    function backToRecords() {
      navigate("tamizajes");
    }
    function requireConsent() {
      if (!record.value?.consent)
        throw new Error("El paciente no aceptó el consentimiento.");
    }
    async function mutateControl(operation, message) {
      if (busy.value) return;
      busy.value = true;
      formError.value = "";
      try {
        if (detailLoading.value || detailError.value)
          throw new Error(
            "Actualizá los controles antes de registrar cambios.",
          );
        requireConsent();
        await operation();
        if (directSaved.value?.tamizaje.id === record.value.id)
          resetDirectRegistration();
        resultOutdated.value = Boolean(result.value);
        await loadRecord();
        notice.value = message;
      } catch (error) {
        formError.value = error.message;
      } finally {
        busy.value = false;
      }
    }
    async function saveAnthropometry() {
      if (anthropometry.value) return;
      await mutateControl(
        () =>
          request(`/tamizajes/${record.value.id}/antropometria`, {
            method: "POST",
            body: validateAnthropometry(anthroForm),
          }),
        "Antropometría guardada correctamente.",
      );
    }
    function editPressure(take) {
      if (busy.value) return;
      editingPressure.value = take;
      Object.assign(pressureForm, {
        sistolicaMmHg: take.sistolicaMmHg,
        diastolicaMmHg: take.diastolicaMmHg,
      });
      formError.value = "";
    }
    function cancelPressureEdit() {
      editingPressure.value = null;
      Object.assign(pressureForm, { sistolicaMmHg: "", diastolicaMmHg: "" });
    }
    async function savePressure() {
      await mutateControl(async () => {
        const values = validatePressure(pressureForm);
        await request(
          editingPressure.value
            ? `/tomas-presion/${editingPressure.value.idControl}`
            : `/tamizajes/${record.value.id}/tomas-presion`,
          { method: editingPressure.value ? "PUT" : "POST", body: values },
        );
        cancelPressureEdit();
      }, "Toma de presión guardada correctamente.");
    }
    async function deletePressure(take) {
      if (
        busy.value ||
        !window.confirm(`¿Eliminar la toma ${take.numeroToma}?`)
      )
        return;
      await mutateControl(async () => {
        await request(`/tomas-presion/${take.idControl}`, { method: "DELETE" });
        if (editingPressure.value?.idControl === take.idControl)
          cancelPressureEdit();
      }, "Toma de presión eliminada. Recalculá el resultado si corresponde.");
    }
    async function saveQuestionnaire() {
      if (savedQuestionnaire.value) return;
      await mutateControl(
        () =>
          request(`/tamizajes/${record.value.id}/cuestionario`, {
            method: "POST",
            body: normalizeQuestionnaire(questionForm),
          }),
        "Cuestionario guardado correctamente.",
      );
    }
    async function calculateResult() {
      if (busy.value) return;
      busy.value = true;
      formError.value = "";
      try {
        requireConsent();
        if (!canCalculateResult.value)
          throw new Error(
            "Completá antropometría, presión y cuestionario antes de calcular.",
          );
        result.value = await request(
          `/tamizajes/${record.value.id}/resultado`,
          { method: "POST" },
        );
        if (directSaved.value?.tamizaje.id === record.value.id)
          resetDirectRegistration();
        resultOutdated.value = false;
      } catch (error) {
        formError.value = error.message;
      } finally {
        busy.value = false;
      }
    }
    function printResult() {
      if (resultOutdated.value) {
        formError.value =
          "Recalculá el resultado antes de imprimir el informe actualizado.";
        return;
      }
      if (result.value) window.print();
    }
    watch(page, (nextPage) => {
      if (nextPage === "inicio") startDashboardRefresh();
      else stopDashboardRefresh();
    });
    watch(tab, () => {
      formError.value = "";
    });
    watch(
      () => questionForm.antecedenteFamiliar,
      (value) => {
        if (value === "NO") questionForm.tiposAntecedentes = [];
      },
    );
    watch(
      () => directForm.cuestionario.antecedenteFamiliar,
      (value) => {
        if (value === "NO") {
          directForm.cuestionario.tiposAntecedentes = [];
          directForm.cuestionario.antecedentesFamiliares = [];
        }
      },
    );
    onMounted(async () => {
      const element = document.getElementById("registration-modal");
      registrationModal = new window.bootstrap.Modal(element);
      element.addEventListener("shown.bs.modal", () =>
        element
          .querySelector("input:not(:disabled),select:not(:disabled)")
          ?.focus(),
      );
      element.addEventListener("hidden.bs.modal", () => {
        modalKind.value = "";
        editingEntity.value = null;
        formError.value = "";
        if (launcher?.isConnected) launcher.focus();
      });
      await reload();
      const hash = location.hash.slice(1);
      const match = /^tamizajes\/(\d+)$/.exec(hash);
      if (match) {
        try {
          await openRecord(await request(`/tamizajes/${match[1]}`));
        } catch (error) {
          navigate("tamizajes");
          loadError.value = error.message;
        }
      } else if (sections.some((section) => section.key === hash))
        navigate(hash);
    });
    onUnmounted(stopDashboardRefresh);
    return {
      theme,
      page,
      directForm,
      directStep,
      hasFamilyHistory,
      directMaxStep,
      directSteps,
      directStepDescription,
      directProgress,
      directReview,
      directError,
      directSaved,
      directGlobalSignal,
      directGlobalSignalLabel,
      signalStyle,
      directResultGuidance,
      directConsultUrl,
      directQrImageUrl,
      qrNeedsNetworkAddress,
      directPatientAge,
      directBmi,
      directMode,
      startDirectRegistration,
      submitDirectStep,
      previousDirectStep,
      goToDirectStep,
      saveDirectRegistration,
      printDirectResult,
      query,
      loading,
      loadError,
      notice,
      busy,
      formError,
      modalKind,
      editingEntity,
      record,
      detailLoading,
      detailError,
      tab,
      patients,
      staff,
      records,
      dashboard,
      dashboardRefreshing,
      dashboardRefreshError,
      dashboardUpdatedAt,
      refreshDashboard,
      dashboardSignalTotal,
      dashboardSignalChart,
      dashboardBarWidth,
      formatDashboardMetric,
      anthropometry,
      pressure,
      savedQuestionnaire,
      result,
      resultOutdated,
      editingPressure,
      patientForm,
      staffForm,
      screeningForm,
      anthroForm,
      pressureForm,
      questionForm,
      sections,
      sexes,
      roles,
      shifts,
      tobacco,
      families,
      antecedentTypes,
      familyRelationships,
      addFamilyHistory,
      removeFamilyHistory,
      setFamilyHistoryPresent,
      clearFamilyHistory,
      operators,
      supervisors,
      canCreateScreening,
      canCalculateResult,
      pageTitle,
      pageDescription,
      modalTitle,
      filteredPatients,
      filteredStaff,
      filteredRecords,
      recentRecords,
      navigate,
      reload,
      openModal,
      deleteEntity,
      closeModal,
      saveModal,
      openRecord,
      backToRecords,
      loadRecord,
      saveAnthropometry,
      editPressure,
      cancelPressureEdit,
      savePressure,
      deletePressure,
      saveQuestionnaire,
      calculateResult,
      printResult,
      label,
      age,
      sexLabel,
      dateFormat,
      badgeClass,
    };
  },
}).mount("#app");

