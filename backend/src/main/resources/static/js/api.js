/** Same-origin API client and form validation shared by the Vue application. */
export async function request(
  path,
  { method = "GET", body, params, signal } = {},
) {
  const query = new URLSearchParams();
  if (params) {
    for (const [key, value] of Object.entries(params)) {
      if (value === undefined || value === null) continue;
      if (Array.isArray(value))
        value.forEach((item) => query.append(key, String(item)));
      else query.set(key, String(value));
    }
  }
  const suffix = query.toString();
  const url = `/api${path.startsWith("/") ? path : `/${path}`}${suffix ? `${path.includes("?") ? "&" : "?"}${suffix}` : ""}`;
  const options = { method, signal, headers: { Accept: "application/json" } };
  if (body !== undefined) {
    options.headers["Content-Type"] = "application/json";
    options.body = JSON.stringify(body);
  }
  let response;
  try {
    response = await fetch(url, options);
  } catch (error) {
    if (error.name === "AbortError") throw error;
    throw new Error(
      "No se pudo conectar con el servidor. Revisá la conexión e intentá nuevamente.",
      { cause: error },
    );
  }
  const raw = await response.text();
  let data = null;
  if (raw.trim()) {
    try {
      data = JSON.parse(raw);
    } catch {
      data = raw;
    }
  }
  if (!response.ok) {
    const serverMessage =
      typeof data === "string"
        ? data
        : data?.message || data?.detail || data?.error;
    const error = new Error(
      serverMessage || `La solicitud falló (HTTP ${response.status}).`,
    );
    error.status = response.status;
    throw error;
  }
  return data;
}

export async function optionalRequest(path) {
  try {
    return await request(path);
  } catch (error) {
    if (error.status === 404) return null;
    throw error;
  }
}

function calendarDate(value) {
  if (typeof value !== "string" || !/^\d{4}-\d{2}-\d{2}$/.test(value))
    return null;
  const [year, month, day] = value.split("-").map(Number);
  const parsed = new Date(0);
  parsed.setFullYear(year, month - 1, day);
  parsed.setHours(0, 0, 0, 0);
  return parsed.getFullYear() === year &&
    parsed.getMonth() === month - 1 &&
    parsed.getDate() === day
    ? parsed
    : null;
}

export function age(date, today = new Date()) {
  const birthday = calendarDate(date);
  const current = today instanceof Date ? today : calendarDate(today);
  if (!birthday || !current || !Number.isFinite(current.getTime())) return null;
  let years = current.getFullYear() - birthday.getFullYear();
  if (
    current.getMonth() < birthday.getMonth() ||
    (current.getMonth() === birthday.getMonth() &&
      current.getDate() < birthday.getDate())
  )
    years -= 1;
  return years;
}

const labels = {
  NO: "Sin antecedentes",
  PRIMER_GRADO: "Padres, hermanos o hijos",
  SEGUNDO_GRADO: "Abuelos, tíos o primos",
  PADRE_MADRE: "Padre o madre",
  HERMANO_HERMANA: "Hermano/a",
  HIJO_HIJA: "Hijo/a",
  ABUELO_ABUELA: "Abuelo/a",
  TIO_TIA: "Tío/a",
  PRIMO_PRIMA: "Primo/a",
  NUNCA: "Nunca fumó",
  EX: "Exfumador",
  ACTUAL: "Fuma actualmente",
  DIABETES: "Diabetes",
  HIPERTENSION: "Hipertensión",
  ENFERMEDAD_CARDIOVASCULAR: "Enfermedad cardiovascular",
  OTRO: "Otro",
  VERDE: "Verde",
  AMARILLO: "Amarillo",
  ROJO: "Rojo",
  MAÑANA: "Mañana",
  TARDE: "Tarde",
  VESPERTINO: "Vespertino",
  ENFERMERO: "Enfermero",
  SUPERVISOR: "Supervisor",
  BAJO: "Bajo",
  LIGERAMENTE_ELEVADO: "Ligeramente elevado",
  MODERADO: "Moderado",
  ALTO: "Alto",
  MUY_ALTO: "Muy alto",
};

export function label(value) {
  if (value === true || value === "true") return "Sí";
  if (value === false || value === "false") return "No";
  if (value === null || value === undefined || value === "")
    return "Sin registrar";
  return (
    labels[value] ||
    String(value)
      .replaceAll("_", " ")
      .toLowerCase()
      .replace(/^./, (first) => first.toUpperCase())
  );
}

export function sexLabel(value) {
  return { M: "Masculino", F: "Femenino", O: "Otro" }[value] || "Sin registrar";
}

export function dateFormat(value) {
  if (!value) return "Sin registrar";
  const parsed = calendarDate(value) || new Date(value);
  if (!Number.isFinite(parsed.getTime())) return "Fecha inválida";
  return new Intl.DateTimeFormat("es-AR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  }).format(parsed);
}

export function badgeClass(value) {
  return (
    {
      VERDE: "text-bg-success",
      AMARILLO: "text-bg-warning",
      ROJO: "text-bg-danger",
    }[value] || "text-bg-secondary"
  );
}

export function normalizeQuestionnaire(values) {
  const normalized = { ...values };
  for (const field of ["actividadFisica", "frutasVerduras", "medicacion"]) {
    const answer = values[field];
    if (answer === true || answer === "true") normalized[field] = true;
    else if (answer === false || answer === "false") normalized[field] = false;
    else throw new Error("Respondé todas las preguntas del cuestionario.");
  }
  if (!["NUNCA", "EX", "ACTUAL"].includes(values.tabaquismo))
    throw new Error("Seleccioná una respuesta sobre tabaquismo.");
  if (
    !["NO", "PRIMER_GRADO", "SEGUNDO_GRADO"].includes(
      values.antecedenteFamiliar,
    )
  )
    throw new Error("Seleccioná los antecedentes familiares.");
  if (values.antecedenteFamiliar === "NO") normalized.tiposAntecedentes = [];
  else {
    const types = values.tiposAntecedentes;
    if (
      !Array.isArray(types) ||
      !types.length ||
      types.some(
        (type) =>
          ![
            "DIABETES",
            "HIPERTENSION",
            "ENFERMEDAD_CARDIOVASCULAR",
            "OTRO",
          ].includes(type),
      )
    ) {
      throw new Error("Seleccioná al menos un tipo de antecedente familiar.");
    }
    normalized.tiposAntecedentes = [...new Set(types)];
  }
  return normalized;
}

function numeric(value, message) {
  if (
    (typeof value !== "number" && typeof value !== "string") ||
    (typeof value === "string" && !value.trim())
  )
    throw new Error(message);
  const number = Number(value);
  if (!Number.isFinite(number)) throw new Error(message);
  return number;
}

export function validatePressure(values) {
  const sistolicaMmHg = numeric(
    values.sistolicaMmHg,
    "Ingresá una presión sistólica válida.",
  );
  const diastolicaMmHg = numeric(
    values.diastolicaMmHg,
    "Ingresá una presión diastólica válida.",
  );
  if (
    !Number.isInteger(sistolicaMmHg) ||
    sistolicaMmHg < 70 ||
    sistolicaMmHg > 260
  )
    throw new Error(
      "La presión sistólica debe ser un número entero entre 70 y 260 mmHg.",
    );
  if (
    !Number.isInteger(diastolicaMmHg) ||
    diastolicaMmHg < 40 ||
    diastolicaMmHg > 150
  )
    throw new Error(
      "La presión diastólica debe ser un número entero entre 40 y 150 mmHg.",
    );
  if (sistolicaMmHg <= diastolicaMmHg)
    throw new Error("La presión sistólica debe ser mayor que la diastólica.");
  return { ...values, sistolicaMmHg, diastolicaMmHg };
}

export function validateAnthropometry(values) {
  const pesoKg = numeric(values.pesoKg, "Ingresá un peso válido.");
  const tallacm = numeric(values.tallacm, "Ingresá una talla válida.");
  if (pesoKg < 30 || pesoKg > 300)
    throw new Error("El peso debe estar entre 30 y 300 kg.");
  if (tallacm < 120 || tallacm > 220)
    throw new Error("La talla debe estar entre 120 y 220 cm.");
  const waist = values.cinturacm;
  const cinturacm =
    waist === undefined ||
    waist === null ||
    (typeof waist === "string" && !waist.trim())
      ? 0
      : numeric(waist, "Ingresá una medida de cintura válida.");
  if (cinturacm !== 0 && (cinturacm <= 0 || cinturacm > 300))
    throw new Error("La cintura debe ser mayor que 0 y como máximo 300 cm.");
  if (
    waist !== undefined &&
    waist !== null &&
    String(waist).trim() &&
    cinturacm === 0
  )
    throw new Error("La cintura debe ser mayor que 0 y como máximo 300 cm.");
  return { ...values, pesoKg, tallacm, cinturacm };
}

export function validatePatient(values) {
  const years = age(values.fecha_nacimiento);
  if (years === null || years < 14 || years > 120)
    throw new Error(
      "La fecha de nacimiento debe corresponder a una edad entre 14 y 120 años.",
    );
  if (!["M", "F", "O"].includes(values.sexo))
    throw new Error("Seleccioná el sexo del paciente.");
  return {
    ...values,
    fecha_nacimiento: values.fecha_nacimiento,
    sexo: values.sexo,
  };
}

function registrationId(value, message, optional = false) {
  if (
    optional &&
    (value === undefined ||
      value === null ||
      (typeof value === "string" && !value.trim()))
  )
    return null;
  if (
    (typeof value !== "number" && typeof value !== "string") ||
    (typeof value === "string" && !/^\d+$/.test(value.trim()))
  )
    throw new Error(message);
  const id = Number(value);
  if (!Number.isInteger(id) || id < 1 || id > 2147483647)
    throw new Error(message);
  return id;
}

function registrationText(value, limit, message) {
  if (typeof value !== "string") throw new Error(message);
  const text = value.trim();
  if (!text || text.length > limit) throw new Error(message);
  return text;
}

function registrationForm(values) {
  if (!values || typeof values !== "object")
    throw new Error("Completá los datos del formulario.");
}

function registrationContext(values) {
  const idRegistro = registrationText(
    values.idRegistro,
    255,
    "Ingresá un identificador de registro de hasta 255 caracteres.",
  );
  if (values.consent !== true)
    throw new Error("Se requiere el consentimiento del paciente.");
  const supervisorNombre = values.supervisor?.trim()
    ? registrationText(
        values.supervisor,
        120,
        "El nombre del supervisor no puede superar 120 caracteres.",
      )
    : null;
  return { idRegistro, consent: true, supervisorNombre };
}

function registrationNurse(values) {
  if (!["nuevo", "existente"].includes(values.operadorMode))
    throw new Error("Elegí cómo registrar al enfermero.");
  let operadorId = null;
  let operador = null;
  if (values.operadorMode === "existente") {
    operadorId = registrationId(
      values.operadorId,
      "Seleccioná un enfermero registrado válido.",
    );
  } else {
    const form = values.operador || {};
    const nombre = registrationText(
      form.nombre,
      80,
      "Ingresá el nombre del enfermero, de hasta 80 caracteres.",
    );
    const apellido = registrationText(
      form.apellido,
      80,
      "Ingresá el apellido del enfermero, de hasta 80 caracteres.",
    );
    if (!["MAÑANA", "TARDE", "VESPERTINO"].includes(form.turno))
      throw new Error("Seleccioná el turno del enfermero.");
    operador = { nombre, apellido, turno: form.turno };
  }
  return { operadorId, operador };
}

function registrationPatient(values) {
  const form = values.paciente || {};
  const validated = validatePatient({
    fecha_nacimiento: form.fechaNacimiento,
    sexo: form.sexo,
  });
  return {
    pacienteId: null,
    paciente: {
      fechaNacimiento: validated.fecha_nacimiento,
      sexo: validated.sexo,
    },
  };
}

function registrationAnthropometry(values, requiredWaist = false) {
  const anthropometry = validateAnthropometry(values.antropometria || {});
  if (requiredWaist && anthropometry.cinturacm <= 0)
    throw new Error(
      "Ingresá una medida de cintura mayor que 0 y de hasta 300 cm.",
    );
  return {
    pesoKg: anthropometry.pesoKg,
    tallacm: anthropometry.tallacm,
    cinturacm: anthropometry.cinturacm,
  };
}

function registrationPressure(values) {
  const pressure = validatePressure(values.tomaPresion || {});
  return {
    sistolicaMmHg: pressure.sistolicaMmHg,
    diastolicaMmHg: pressure.diastolicaMmHg,
  };
}

function registrationQuestionnaire(values) {
  const input = values.cuestionario || {};
  const rows = (input.antecedentesFamiliares || []).filter((row) => row.tipo || row.parentesco);
  if (input.antecedenteFamiliar !== "NO" && (!rows.length || rows.some((row) => !row.tipo || !row.parentesco)))
    throw new Error("Para cada antecedente, seleccioná la condición y el familiar correspondiente.");
  const types = [...new Set(rows.map((row) => row.tipo))];
  const questionnaire = normalizeQuestionnaire({
    ...input,
    tiposAntecedentes: input.antecedenteFamiliar === "NO" ? [] : types,
  });
  return {
    actividadFisica: questionnaire.actividadFisica,
    frutasVerduras: questionnaire.frutasVerduras,
    medicacion: questionnaire.medicacion,
    tabaquismo: questionnaire.tabaquismo,
    antecedenteFamiliar: questionnaire.antecedenteFamiliar,
    tiposAntecedentes: questionnaire.tiposAntecedentes,
    antecedentesFamiliares: input.antecedenteFamiliar === "NO" ? [] : rows,
  };
}

/** Validate the four grouped wizard steps, or the complete draft at step 4. */
export function validateDirectRegistrationStep(values, index) {
  registrationForm(values);
  const sections = [
    registrationNurse,
    registrationPatient,
    registrationContext,
    (draft) => registrationAnthropometry(draft, true),
    registrationPressure,
    registrationQuestionnaire,
  ];
  const groups = [[0, 1, 2], [3, 4], [5]];
  if (!Number.isInteger(index) || index < 0 || index > groups.length)
    throw new Error("El paso del formulario no es válido.");
  if (index < groups.length) {
    for (const section of groups[index]) sections[section](values);
    return true;
  }
  // Validate in screen order so the first incomplete step remains actionable.
  sections.forEach((validate) => validate(values));
  return buildDirectRegistrationPayload(values);
}

/** Validate the complete direct form before sending one registration request. */
export function buildDirectRegistrationPayload(values) {
  registrationForm(values);
  return {
    ...registrationContext(values),
    ...registrationNurse(values),
    ...registrationPatient(values),
    antropometria: registrationAnthropometry(values),
    tomaPresion: registrationPressure(values),
    cuestionario: registrationQuestionnaire(values),
  };
}
