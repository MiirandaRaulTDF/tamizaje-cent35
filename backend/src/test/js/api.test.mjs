import { readFile } from "node:fs/promises";
import test from "node:test";
import assert from "node:assert/strict";

const source = await readFile(
  new URL("../../main/resources/static/js/api.js", import.meta.url),
  "utf8",
);
const api = await import(
  `data:text/javascript;base64,${Buffer.from(source).toString("base64")}`
);

async function withFetch(mock, run) {
  const previous = globalThis.fetch;
  globalThis.fetch = mock;
  try {
    await run();
  } finally {
    globalThis.fetch = previous;
  }
}

test("request encodes query parameters and keeps false in JSON bodies", async () => {
  await withFetch(
    async (url, options) => {
      assert.equal(
        url,
        "/api/pacientes?buscar=A%26B+%C3%B1&activo=false&tipo=uno&tipo=dos",
      );
      assert.equal(options.method, "POST");
      assert.equal(options.headers["Content-Type"], "application/json");
      assert.deepEqual(JSON.parse(options.body), { medicacion: false });
      return new Response('{"id":3}', { status: 201 });
    },
    async () => {
      assert.deepEqual(
        await api.request("/pacientes", {
          method: "POST",
          body: { medicacion: false },
          params: {
            buscar: "A&B ñ",
            activo: false,
            tipo: ["uno", "dos"],
            absent: null,
          },
        }),
        { id: 3 },
      );
    },
  );
});

test("request returns null for an empty DELETE response", async () => {
  await withFetch(
    async () => new Response(null, { status: 204 }),
    async () => {
      assert.equal(
        await api.request("/pacientes/3", { method: "DELETE" }),
        null,
      );
    },
  );
});

test("request preserves JSON and text server error messages and numeric HTTP statuses", async () => {
  await withFetch(
    async () =>
      new Response('{"message":"Cuestionario incompleto"}', { status: 400 }),
    async () => {
      await assert.rejects(
        api.request("/cuestionarios"),
        (error) =>
          error.status === 400 && error.message === "Cuestionario incompleto",
      );
    },
  );
  await withFetch(
    async () => new Response("Ya existe el control", { status: 409 }),
    async () => {
      await assert.rejects(
        api.request("/controles"),
        (error) =>
          error.status === 409 && error.message === "Ya existe el control",
      );
    },
  );
});

test("optionalRequest ignores only 404, while other HTTP failures remain visible", async () => {
  await withFetch(
    async () => new Response("No encontrado", { status: 404 }),
    async () => {
      assert.equal(
        await api.optionalRequest("/tamizajes/2/cuestionario"),
        null,
      );
    },
  );
  await withFetch(
    async () => new Response("Error de base de datos", { status: 500 }),
    async () => {
      await assert.rejects(
        api.optionalRequest("/tamizajes/2/cuestionario"),
        (error) => error.status === 500,
      );
    },
  );
});

test("network and cancellation failures remain distinguishable from a missing control", async () => {
  await withFetch(
    async () => {
      throw new TypeError("fetch failed");
    },
    async () => {
      await assert.rejects(
        api.optionalRequest("/controles"),
        (error) => error.status === undefined && /conectar/.test(error.message),
      );
    },
  );
  const aborted = new DOMException("Aborted", "AbortError");
  await withFetch(
    async () => {
      throw aborted;
    },
    async () => {
      await assert.rejects(
        api.request("/pacientes"),
        (error) => error === aborted,
      );
    },
  );
});

const questionnaire = {
  actividadFisica: "true",
  frutasVerduras: false,
  medicacion: "false",
  tabaquismo: "NUNCA",
  antecedenteFamiliar: "PRIMER_GRADO",
  tiposAntecedentes: ["DIABETES", "HIPERTENSION"],
};

test("questionnaire converts explicit booleans and preserves multiple antecedent types", () => {
  const normalized = api.normalizeQuestionnaire(questionnaire);
  assert.equal(normalized.actividadFisica, true);
  assert.equal(normalized.frutasVerduras, false);
  assert.equal(normalized.medicacion, false);
  assert.deepEqual(normalized.tiposAntecedentes, ["DIABETES", "HIPERTENSION"]);
  assert.equal(questionnaire.medicacion, "false");
});

test("questionnaire clears stale antecedent selections for NO without modifying the form", () => {
  const form = { ...questionnaire, antecedenteFamiliar: "NO" };
  assert.deepEqual(api.normalizeQuestionnaire(form).tiposAntecedentes, []);
  assert.equal(form.tiposAntecedentes.length, 2);
});

test("questionnaire rejects unanswered boolean, tobacco, family and antecedent questions", () => {
  for (const unanswered of ["", null, undefined, 0, 1, "yes"]) {
    assert.throws(
      () =>
        api.normalizeQuestionnaire({
          ...questionnaire,
          medicacion: unanswered,
        }),
      /Respondé/,
    );
  }
  assert.throws(
    () => api.normalizeQuestionnaire({ ...questionnaire, tabaquismo: "" }),
    /tabaquismo/,
  );
  assert.throws(
    () =>
      api.normalizeQuestionnaire({ ...questionnaire, antecedenteFamiliar: "" }),
    /familiares/,
  );
  assert.throws(
    () =>
      api.normalizeQuestionnaire({ ...questionnaire, tiposAntecedentes: [] }),
    /al menos/,
  );
  assert.throws(
    () =>
      api.normalizeQuestionnaire({
        ...questionnaire,
        tiposAntecedentes: ["UNKNOWN"],
      }),
    /al menos/,
  );
});

test("pressure normalizes strings and rejects invalid order, nonintegers, blanks and ranges", () => {
  assert.deepEqual(
    api.validatePressure({
      sistolicaMmHg: "120",
      diastolicaMmHg: "80",
      numeroToma: 2,
    }),
    { sistolicaMmHg: 120, diastolicaMmHg: 80, numeroToma: 2 },
  );
  const invalid = [
    [80, 80],
    [80, 90],
    [120.5, 80],
    [120, 80.5],
    [69, 40],
    [261, 80],
    [120, 39],
    [180, 151],
    ["", 80],
    [120, Infinity],
    [null, 80],
  ];
  for (const [sistolicaMmHg, diastolicaMmHg] of invalid)
    assert.throws(() =>
      api.validatePressure({ sistolicaMmHg, diastolicaMmHg }),
    );
});

test("calendar age handles birthdays and leap days, rejecting invalid dates", () => {
  const today = new Date(2026, 9, 14);
  assert.equal(api.age("2012-10-14", today), 14);
  assert.equal(api.age("2012-10-15", today), 13);
  assert.equal(api.age("2012-10-13", today), 14);
  assert.equal(api.age("2000-02-29", new Date(2025, 1, 28)), 24);
  assert.equal(api.age("2000-02-29", new Date(2025, 2, 1)), 25);
  assert.equal(api.age("2025-02-29", today), null);
  assert.equal(api.age("2026-13-01", today), null);
  assert.equal(api.age("", today), null);
});

function birthdayYearsAgo(years, dayOffset = 0) {
  const today = new Date();
  const date = new Date(
    today.getFullYear() - years,
    today.getMonth(),
    today.getDate() + dayOffset,
  );
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}

test("patient validation accepts 14 and 120 year limits and rejects an upcoming 14th birthday", () => {
  for (const years of [14, 120]) {
    const form = { fecha_nacimiento: birthdayYearsAgo(years), sexo: "F" };
    assert.deepEqual(api.validatePatient(form), form);
  }
  assert.throws(
    () =>
      api.validatePatient({
        fecha_nacimiento: birthdayYearsAgo(14, 1),
        sexo: "M",
      }),
    /edad/,
  );
  assert.throws(
    () =>
      api.validatePatient({
        fecha_nacimiento: birthdayYearsAgo(121),
        sexo: "O",
      }),
    /edad/,
  );
  assert.throws(
    () => api.validatePatient({ fecha_nacimiento: "invalid", sexo: "M" }),
    /edad/,
  );
  assert.throws(
    () =>
      api.validatePatient({ fecha_nacimiento: birthdayYearsAgo(40), sexo: "" }),
    /sexo/,
  );
});

test("anthropometry normalizes numeric strings and optional waist while checking all ranges", () => {
  assert.deepEqual(
    api.validateAnthropometry({
      pesoKg: "72.5",
      tallacm: "170",
      cinturacm: "88.5",
    }),
    { pesoKg: 72.5, tallacm: 170, cinturacm: 88.5 },
  );
  assert.equal(
    api.validateAnthropometry({ pesoKg: 30, tallacm: 120, cinturacm: "" })
      .cinturacm,
    0,
  );
  assert.equal(
    api.validateAnthropometry({ pesoKg: 300, tallacm: 220 }).cinturacm,
    0,
  );
  for (const override of [
    { pesoKg: 29.9 },
    { pesoKg: 301 },
    { pesoKg: NaN },
    { tallacm: 119 },
    { tallacm: 221 },
    { tallacm: "" },
    { cinturacm: -1 },
    { cinturacm: 0 },
    { cinturacm: 301 },
    { cinturacm: Infinity },
  ]) {
    assert.throws(() =>
      api.validateAnthropometry({ pesoKg: 70, tallacm: 170, ...override }),
    );
  }
});

test("display helpers handle booleans, enums, missing values and Bootstrap risk colors", () => {
  assert.equal(api.label(false), "No");
  assert.equal(
    api.label("ENFERMEDAD_CARDIOVASCULAR"),
    "Enfermedad cardiovascular",
  );
  assert.equal(api.label(null), "Sin registrar");
  assert.equal(api.sexLabel("F"), "Femenino");
  assert.equal(api.badgeClass("ROJO"), "text-bg-danger");
  assert.equal(api.badgeClass(undefined), "text-bg-secondary");
  assert.equal(api.dateFormat("2026-10-14"), "14/10/2026");
});
