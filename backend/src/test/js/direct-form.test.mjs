import { readFile } from "node:fs/promises";
import test from "node:test";
import assert from "node:assert/strict";

const source = await readFile(
  new URL("../../main/resources/static/js/api.js", import.meta.url),
  "utf8",
);
const { buildDirectRegistrationPayload } = await import(
  `data:text/javascript;base64,${Buffer.from(source).toString("base64")}`
);

function birthdayYearsAgo(years, dayOffset = 0) {
  const today = new Date();
  const date = new Date(
    today.getFullYear() - years,
    today.getMonth(),
    today.getDate() + dayOffset,
  );
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}

function form() {
  return {
    operadorMode: "nuevo",
    operadorId: "",
    operador: { nombre: "  Ana  ", apellido: " Pérez ", turno: "MAÑANA" },
    pacienteMode: "nuevo",
    pacienteId: "",
    paciente: { fechaNacimiento: birthdayYearsAgo(40), sexo: "F" },
    idRegistro: "  REG-100  ",
    consent: true,
    supervisorId: "",
    antropometria: { pesoKg: "72.5", tallacm: "170", cinturacm: "88.5" },
    tomaPresion: { sistolicaMmHg: "120", diastolicaMmHg: "80" },
    cuestionario: {
      actividadFisica: "true",
      frutasVerduras: false,
      medicacion: "false",
      tabaquismo: "NUNCA",
      antecedenteFamiliar: "PRIMER_GRADO",
      tiposAntecedentes: ["DIABETES", "HIPERTENSION"],
    },
  };
}

test("direct registration builds one exact DTO for a new nurse, patient and complete control", () => {
  const draft = form();
  draft.operador.rol = "SUPERVISOR";
  draft.paciente.fecha_nacimiento = "2000-01-01";
  draft.antropometria.id = 999;
  draft.tomaPresion.numeroToma = 9;
  draft.cuestionario.tamizajeId = 999;
  draft.extra = "not sent";
  assert.deepEqual(buildDirectRegistrationPayload(draft), {
    idRegistro: "REG-100",
    consent: true,
    operadorId: null,
    operador: { nombre: "Ana", apellido: "Pérez", turno: "MAÑANA" },
    pacienteId: null,
    paciente: { fechaNacimiento: draft.paciente.fechaNacimiento, sexo: "F" },
    supervisorId: null,
    antropometria: { pesoKg: 72.5, tallacm: 170, cinturacm: 88.5 },
    tomaPresion: { sistolicaMmHg: 120, diastolicaMmHg: 80 },
    cuestionario: {
      actividadFisica: true,
      frutasVerduras: false,
      medicacion: false,
      tabaquismo: "NUNCA",
      antecedenteFamiliar: "PRIMER_GRADO",
      tiposAntecedentes: ["DIABETES", "HIPERTENSION"],
    },
  });
});

test("existing mode sends numeric IDs and excludes invalid inactive creation drafts", () => {
  const draft = form();
  Object.assign(draft, {
    operadorMode: "existente",
    operadorId: " 25 ",
    operador: { nombre: "", apellido: "", turno: "" },
    pacienteMode: "existente",
    pacienteId: "007",
    paciente: { fechaNacimiento: "not a date", sexo: "" },
    supervisorId: "3",
  });
  const payload = buildDirectRegistrationPayload(draft);
  assert.equal(payload.operadorId, 25);
  assert.equal(payload.operador, null);
  assert.equal(payload.pacienteId, 7);
  assert.equal(payload.paciente, null);
  assert.equal(payload.supervisorId, 3);
});

test("mixed modes create only the requested person and ignore stale IDs for new records", () => {
  const existingNurse = form();
  Object.assign(existingNurse, {
    operadorMode: "existente",
    operadorId: 18,
    pacienteId: 19,
  });
  const first = buildDirectRegistrationPayload(existingNurse);
  assert.equal(first.operadorId, 18);
  assert.equal(first.operador, null);
  assert.equal(first.pacienteId, null);
  assert.deepEqual(first.paciente, {
    fechaNacimiento: existingNurse.paciente.fechaNacimiento,
    sexo: "F",
  });

  const existingPatient = form();
  Object.assign(existingPatient, {
    operadorId: 18,
    pacienteMode: "existente",
    pacienteId: 19,
  });
  const second = buildDirectRegistrationPayload(existingPatient);
  assert.equal(second.operadorId, null);
  assert.deepEqual(second.operador, {
    nombre: "Ana",
    apellido: "Pérez",
    turno: "MAÑANA",
  });
  assert.equal(second.pacienteId, 19);
  assert.equal(second.paciente, null);
});

test("registration rejects missing modes, blank identifiers and any consent other than true", () => {
  for (const key of ["operadorMode", "pacienteMode"]) {
    for (const value of ["", undefined, null, true, "nuevo,existente"]) {
      assert.throws(
        () => buildDirectRegistrationPayload({ ...form(), [key]: value }),
        /Elegí cómo registrar/,
      );
    }
  }
  for (const value of [false, "true", "false", 1, "", undefined, null]) {
    assert.throws(
      () => buildDirectRegistrationPayload({ ...form(), consent: value }),
      /consentimiento/,
    );
  }
  for (const value of ["", "   ", "x".repeat(256), undefined, null, 100]) {
    assert.throws(
      () => buildDirectRegistrationPayload({ ...form(), idRegistro: value }),
      /identificador de registro/,
    );
  }
  assert.equal(
    buildDirectRegistrationPayload({ ...form(), idRegistro: "x".repeat(255) })
      .idRegistro.length,
    255,
  );
});

test("selected nurse, patient and supervisor IDs must be positive 32-bit integers", () => {
  const invalidIds = [
    -1,
    0,
    1.5,
    2147483648,
    Infinity,
    NaN,
    true,
    {},
    "-1",
    "1.5",
    "1e2",
    "0x10",
  ];
  for (const key of ["operadorId", "pacienteId", "supervisorId"]) {
    for (const value of invalidIds) {
      const draft = form();
      draft.operadorMode = "existente";
      draft.operadorId = 1;
      draft.pacienteMode = "existente";
      draft.pacienteId = 2;
      draft[key] = value;
      assert.throws(
        () => buildDirectRegistrationPayload(draft),
        /registrado válido/,
      );
    }
  }
  for (const key of ["operadorId", "pacienteId"]) {
    for (const value of ["", " ", undefined, null]) {
      const draft = form();
      draft.operadorMode = "existente";
      draft.operadorId = 1;
      draft.pacienteMode = "existente";
      draft.pacienteId = 2;
      draft[key] = value;
      assert.throws(
        () => buildDirectRegistrationPayload(draft),
        /registrado válido/,
      );
    }
  }
  const bounds = form();
  Object.assign(bounds, {
    operadorMode: "existente",
    operadorId: 1,
    pacienteMode: "existente",
    pacienteId: "2147483647",
    supervisorId: 2147483647,
  });
  const payload = buildDirectRegistrationPayload(bounds);
  assert.equal(payload.operadorId, 1);
  assert.equal(payload.pacienteId, 2147483647);
  assert.equal(payload.supervisorId, 2147483647);
  for (const supervisorId of ["", "  ", null, undefined])
    assert.equal(
      buildDirectRegistrationPayload({ ...form(), supervisorId }).supervisorId,
      null,
    );
});

test("new nurses require trimmed names of at most 80 characters and a supported shift", () => {
  for (const key of ["nombre", "apellido"]) {
    for (const value of ["", "  ", "x".repeat(81), undefined, null, 100]) {
      const draft = form();
      draft.operador[key] = value;
      assert.throws(
        () => buildDirectRegistrationPayload(draft),
        /80 caracteres/,
      );
    }
  }
  for (const turno of ["", undefined, "NOCHE", "mañana"])
    assert.throws(
      () =>
        buildDirectRegistrationPayload({
          ...form(),
          operador: { nombre: "Ana", apellido: "Pérez", turno },
        }),
      /turno/,
    );
  for (const turno of ["MAÑANA", "TARDE", "VESPERTINO"]) {
    const draft = form();
    draft.operador = {
      nombre: "x".repeat(80),
      apellido: "y".repeat(80),
      turno,
    };
    assert.equal(buildDirectRegistrationPayload(draft).operador.turno, turno);
  }
});

test("new patients validate calendar dates, sex and the 14 to 120 age range", () => {
  for (const years of [14, 120]) {
    const draft = form();
    draft.paciente = { fechaNacimiento: birthdayYearsAgo(years), sexo: "O" };
    assert.deepEqual(
      buildDirectRegistrationPayload(draft).paciente,
      draft.paciente,
    );
  }
  for (const fechaNacimiento of [
    "",
    "2025-02-29",
    birthdayYearsAgo(14, 1),
    birthdayYearsAgo(121),
  ]) {
    const draft = form();
    draft.paciente.fechaNacimiento = fechaNacimiento;
    assert.throws(() => buildDirectRegistrationPayload(draft), /edad/);
  }
  const draft = form();
  draft.paciente.sexo = "";
  assert.throws(() => buildDirectRegistrationPayload(draft), /sexo/);
});

test("direct form validates measurements, pressure and all answers before producing a payload", () => {
  const invalid = [
    ["antropometria", "pesoKg", "", /peso/],
    ["antropometria", "pesoKg", 29, /peso/],
    ["antropometria", "tallacm", 221, /talla/],
    ["antropometria", "cinturacm", -1, /cintura/],
    ["tomaPresion", "sistolicaMmHg", 80, /mayor/],
    ["tomaPresion", "diastolicaMmHg", 39, /diastólica/],
    ["tomaPresion", "sistolicaMmHg", 120.5, /entero/],
    ["cuestionario", "actividadFisica", "", /Respondé/],
    ["cuestionario", "frutasVerduras", undefined, /Respondé/],
    ["cuestionario", "medicacion", null, /Respondé/],
    ["cuestionario", "tabaquismo", "", /tabaquismo/],
    ["cuestionario", "antecedenteFamiliar", "", /familiares/],
    ["cuestionario", "tiposAntecedentes", [], /al menos/],
    ["cuestionario", "tiposAntecedentes", ["UNKNOWN"], /al menos/],
  ];
  for (const [section, key, value, message] of invalid) {
    const draft = form();
    draft[section][key] = value;
    assert.throws(() => buildDirectRegistrationPayload(draft), message);
  }
  const optionalWaist = form();
  optionalWaist.antropometria.cinturacm = "";
  assert.equal(
    buildDirectRegistrationPayload(optionalWaist).antropometria.cinturacm,
    0,
  );
});

test("questionnaire preserves explicit false answers and multiple family conditions", () => {
  const draft = form();
  draft.cuestionario.actividadFisica = "false";
  draft.cuestionario.frutasVerduras = "false";
  draft.cuestionario.medicacion = false;
  draft.cuestionario.antecedenteFamiliar = "SEGUNDO_GRADO";
  draft.cuestionario.tiposAntecedentes = [
    "DIABETES",
    "HIPERTENSION",
    "DIABETES",
    "ENFERMEDAD_CARDIOVASCULAR",
  ];
  const payload = buildDirectRegistrationPayload(draft);
  assert.equal(payload.cuestionario.actividadFisica, false);
  assert.equal(payload.cuestionario.frutasVerduras, false);
  assert.equal(payload.cuestionario.medicacion, false);
  assert.equal(payload.cuestionario.antecedenteFamiliar, "SEGUNDO_GRADO");
  assert.deepEqual(payload.cuestionario.tiposAntecedentes, [
    "DIABETES",
    "HIPERTENSION",
    "ENFERMEDAD_CARDIOVASCULAR",
  ]);
  draft.cuestionario.antecedenteFamiliar = "NO";
  assert.deepEqual(
    buildDirectRegistrationPayload(draft).cuestionario.tiposAntecedentes,
    [],
  );
  assert.equal(draft.cuestionario.tiposAntecedentes.length, 4);
});

test("building a payload preserves the entire draft and shares no mutable nested objects", () => {
  const draft = form();
  const snapshot = structuredClone(draft);
  const payload = buildDirectRegistrationPayload(draft);
  assert.deepEqual(draft, snapshot);
  payload.operador.nombre = "Otro";
  payload.paciente.sexo = "M";
  payload.antropometria.pesoKg = 90;
  payload.tomaPresion.sistolicaMmHg = 150;
  payload.cuestionario.tiposAntecedentes.push("OTRO");
  assert.deepEqual(draft, snapshot);
});
