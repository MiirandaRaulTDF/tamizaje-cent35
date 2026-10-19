import { readFile } from "node:fs/promises";
import test from "node:test";
import assert from "node:assert/strict";

const source = await readFile(
  new URL("../../main/resources/static/js/api.js", import.meta.url),
  "utf8",
);
const { buildDirectRegistrationPayload, validateDirectRegistrationStep } =
  await import(
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
    operador: { nombre: "  Ana ", apellido: " Pérez  ", turno: "MAÑANA" },
    pacienteMode: "nuevo",
    pacienteId: "",
    paciente: { fechaNacimiento: birthdayYearsAgo(40), sexo: "F" },
    idRegistro: "  REG-WIZARD ",
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

test("each wizard step accepts a partial draft without requiring any later section", () => {
  const draft = form();
  const partials = [
    { operadorMode: draft.operadorMode, operador: draft.operador },
    { pacienteMode: draft.pacienteMode, paciente: draft.paciente },
    { idRegistro: draft.idRegistro, consent: true },
    { antropometria: draft.antropometria },
    { tomaPresion: draft.tomaPresion },
    { cuestionario: draft.cuestionario },
  ];
  const expected = [
    {
      operadorId: null,
      operador: { nombre: "Ana", apellido: "Pérez", turno: "MAÑANA" },
    },
    { pacienteId: null, paciente: draft.paciente },
    { idRegistro: "REG-WIZARD", consent: true, supervisorId: null },
    { pesoKg: 72.5, tallacm: 170, cinturacm: 88.5 },
    { sistolicaMmHg: 120, diastolicaMmHg: 80 },
    {
      actividadFisica: true,
      frutasVerduras: false,
      medicacion: false,
      tabaquismo: "NUNCA",
      antecedenteFamiliar: "PRIMER_GRADO",
      tiposAntecedentes: ["DIABETES", "HIPERTENSION"],
    },
  ];
  partials.forEach((partial, index) => {
    assert.deepEqual(
      validateDirectRegistrationStep(partial, index),
      expected[index],
    );
    assert.deepEqual(
      validateDirectRegistrationStep(draft, index),
      expected[index],
    );
  });
});

test("existing people validate only their selected ID and ignore inactive creation fields", () => {
  assert.deepEqual(
    validateDirectRegistrationStep(
      {
        operadorMode: "existente",
        operadorId: " 2147483647 ",
        operador: { nombre: "", apellido: "", turno: "NOCHE" },
      },
      0,
    ),
    { operadorId: 2147483647, operador: null },
  );
  assert.deepEqual(
    validateDirectRegistrationStep(
      {
        pacienteMode: "existente",
        pacienteId: "001",
        paciente: { fechaNacimiento: "2025-02-29", sexo: "" },
      },
      1,
    ),
    { pacienteId: 1, paciente: null },
  );
  for (const id of ["", null, 0, -1, 1.5, "1e2", 2147483648, true]) {
    assert.throws(
      () =>
        validateDirectRegistrationStep(
          { operadorMode: "existente", operadorId: id },
          0,
        ),
      /enfermero registrado válido/,
    );
    assert.throws(
      () =>
        validateDirectRegistrationStep(
          { pacienteMode: "existente", pacienteId: id },
          1,
        ),
      /paciente registrado válido/,
    );
  }
});

test("nurse step trims names, enforces both name limits and requires a supported shift", () => {
  for (const turno of ["MAÑANA", "TARDE", "VESPERTINO"]) {
    const draft = {
      operadorMode: "nuevo",
      operadorId: -1,
      operador: { nombre: ` ${"a".repeat(80)} `, apellido: " Pérez ", turno },
    };
    assert.equal(
      validateDirectRegistrationStep(draft, 0).operador.nombre.length,
      80,
    );
  }
  for (const key of ["nombre", "apellido"]) {
    for (const value of [" ", "x".repeat(81), null]) {
      const draft = form();
      draft.operador[key] = value;
      assert.throws(
        () => validateDirectRegistrationStep(draft, 0),
        /80 caracteres/,
      );
    }
  }
  const draft = form();
  draft.operador.turno = "NOCHE";
  assert.throws(() => validateDirectRegistrationStep(draft, 0), /turno/);
  draft.operadorMode = "";
  assert.throws(
    () => validateDirectRegistrationStep(draft, 0),
    /Elegí cómo registrar al enfermero/,
  );
});

test("patient step checks calendar dates, sex and the 14 to 120 age limits", () => {
  for (const years of [14, 120]) {
    const paciente = { fechaNacimiento: birthdayYearsAgo(years), sexo: "O" };
    assert.deepEqual(
      validateDirectRegistrationStep(
        { pacienteMode: "nuevo", paciente, pacienteId: -1 },
        1,
      ),
      { pacienteId: null, paciente },
    );
  }
  for (const fechaNacimiento of [
    "",
    "2025-02-29",
    birthdayYearsAgo(14, 1),
    birthdayYearsAgo(121),
  ]) {
    assert.throws(
      () =>
        validateDirectRegistrationStep(
          { pacienteMode: "nuevo", paciente: { fechaNacimiento, sexo: "M" } },
          1,
        ),
      /edad/,
    );
  }
  const draft = form();
  draft.paciente.sexo = "";
  assert.throws(() => validateDirectRegistrationStep(draft, 1), /sexo/);
  draft.pacienteMode = "";
  assert.throws(
    () => validateDirectRegistrationStep(draft, 1),
    /Elegí cómo registrar al paciente/,
  );
});

test("registration step requires explicit consent, a trimmed code and a valid optional supervisor", () => {
  const draft = { idRegistro: ` ${"r".repeat(255)} `, consent: true };
  for (const supervisorId of [undefined, null, "", " "]) {
    const validated = validateDirectRegistrationStep(
      { ...draft, supervisorId },
      2,
    );
    assert.equal(validated.idRegistro.length, 255);
    assert.equal(validated.supervisorId, null);
  }
  assert.equal(
    validateDirectRegistrationStep({ ...draft, supervisorId: "2147483647" }, 2)
      .supervisorId,
    2147483647,
  );
  for (const consent of [false, "true", 1, null, undefined])
    assert.throws(
      () => validateDirectRegistrationStep({ ...draft, consent }, 2),
      /consentimiento/,
    );
  for (const idRegistro of [" ", "r".repeat(256), 123])
    assert.throws(
      () => validateDirectRegistrationStep({ ...draft, idRegistro }, 2),
      /identificador/,
    );
  for (const supervisorId of [0, -1, "1.5", 2147483648, true])
    assert.throws(
      () => validateDirectRegistrationStep({ ...draft, supervisorId }, 2),
      /supervisor registrado válido/,
    );
});

test("wizard measurements require a positive waist while allowing valid measurement boundaries", () => {
  for (const cinturacm of [
    undefined,
    null,
    "",
    " ",
    0,
    "0",
    -1,
    301,
    Infinity,
    false,
  ]) {
    assert.throws(
      () =>
        validateDirectRegistrationStep(
          { antropometria: { pesoKg: 70, tallacm: 170, cinturacm } },
          3,
        ),
      /cintura/,
    );
  }
  assert.deepEqual(
    validateDirectRegistrationStep(
      { antropometria: { pesoKg: "30", tallacm: "120", cinturacm: "0.5" } },
      3,
    ),
    { pesoKg: 30, tallacm: 120, cinturacm: 0.5 },
  );
  assert.deepEqual(
    validateDirectRegistrationStep(
      { antropometria: { pesoKg: 300, tallacm: 220, cinturacm: 300 } },
      3,
    ),
    { pesoKg: 300, tallacm: 220, cinturacm: 300 },
  );
  for (const override of [{ pesoKg: "" }, { pesoKg: 29 }, { tallacm: 221 }])
    assert.throws(() =>
      validateDirectRegistrationStep(
        {
          antropometria: {
            pesoKg: 70,
            tallacm: 170,
            cinturacm: 80,
            ...override,
          },
        },
        3,
      ),
    );
});

test("pressure step rejects inverted or fractional readings and permits urgent readings", () => {
  for (const [sistolicaMmHg, diastolicaMmHg] of [
    [80, 90],
    [80, 80],
    [120, 39],
    [185, 151],
    [120.5, 80],
    [120, ""],
  ])
    assert.throws(() =>
      validateDirectRegistrationStep(
        { tomaPresion: { sistolicaMmHg, diastolicaMmHg } },
        4,
      ),
    );
  assert.deepEqual(
    validateDirectRegistrationStep(
      { tomaPresion: { sistolicaMmHg: "185", diastolicaMmHg: "110" } },
      4,
    ),
    { sistolicaMmHg: 185, diastolicaMmHg: 110 },
  );
});

test("questionnaire step keeps explicit false answers and requires selected family conditions", () => {
  const cuestionario = {
    actividadFisica: "false",
    frutasVerduras: false,
    medicacion: "false",
    tabaquismo: "EX",
    antecedenteFamiliar: "SEGUNDO_GRADO",
    tiposAntecedentes: ["DIABETES", "HIPERTENSION", "DIABETES"],
  };
  assert.deepEqual(validateDirectRegistrationStep({ cuestionario }, 5), {
    ...cuestionario,
    actividadFisica: false,
    frutasVerduras: false,
    medicacion: false,
    tiposAntecedentes: ["DIABETES", "HIPERTENSION"],
  });
  for (const tiposAntecedentes of [[], undefined, ["UNKNOWN"]])
    assert.throws(
      () =>
        validateDirectRegistrationStep(
          { cuestionario: { ...cuestionario, tiposAntecedentes } },
          5,
        ),
      /al menos/,
    );
  assert.deepEqual(
    validateDirectRegistrationStep(
      { cuestionario: { ...cuestionario, antecedenteFamiliar: "NO" } },
      5,
    ).tiposAntecedentes,
    [],
  );
  assert.throws(
    () =>
      validateDirectRegistrationStep(
        { cuestionario: { ...cuestionario, medicacion: "" } },
        5,
      ),
    /Respondé/,
  );
});

test("final review validates every section and produces the unchanged complete API payload", () => {
  const draft = form();
  assert.deepEqual(
    validateDirectRegistrationStep(draft, 6),
    buildDirectRegistrationPayload(draft),
  );
  const incomplete = [
    ["operador", "nombre", "", /enfermero/],
    ["paciente", "sexo", "", /sexo/],
    [null, "consent", false, /consentimiento/],
    ["antropometria", "cinturacm", "", /cintura/],
    ["tomaPresion", "diastolicaMmHg", 130, /mayor/],
    ["cuestionario", "tiposAntecedentes", [], /al menos/],
  ];
  for (const [section, key, value, message] of incomplete) {
    const invalid = form();
    (section ? invalid[section] : invalid)[key] = value;
    assert.throws(() => validateDirectRegistrationStep(invalid, 6), message);
  }
  draft.antropometria.cinturacm = "";
  assert.equal(
    buildDirectRegistrationPayload(draft).antropometria.cinturacm,
    0,
  );
  assert.throws(() => validateDirectRegistrationStep(draft, 6), /cintura/);
});

test("step normalization leaves the draft unchanged after success, output edits and errors", () => {
  const draft = form();
  const snapshot = structuredClone(draft);
  for (let index = 0; index <= 6; index += 1) {
    const normalized = validateDirectRegistrationStep(draft, index);
    if (normalized.operador) normalized.operador.nombre = "Otro";
    if (normalized.paciente) normalized.paciente.sexo = "M";
    if (normalized.tiposAntecedentes) normalized.tiposAntecedentes.push("OTRO");
    if (normalized.cuestionario)
      normalized.cuestionario.tiposAntecedentes.push("OTRO");
    normalized.pesoKg = 100;
    normalized.sistolicaMmHg = 200;
    assert.deepEqual(draft, snapshot);
  }
  draft.cuestionario.tiposAntecedentes = [];
  const incomplete = structuredClone(draft);
  assert.throws(() => validateDirectRegistrationStep(draft, 6), /al menos/);
  assert.deepEqual(draft, incomplete);
});

test("invalid wizard indices and missing drafts are rejected explicitly", () => {
  for (const index of [-1, 7, 0.5, "0", null, undefined, NaN, Infinity])
    assert.throws(
      () => validateDirectRegistrationStep(form(), index),
      /paso del formulario/,
    );
  for (const draft of [undefined, null, false, ""])
    assert.throws(
      () => validateDirectRegistrationStep(draft, 0),
      /datos del formulario/,
    );
});
