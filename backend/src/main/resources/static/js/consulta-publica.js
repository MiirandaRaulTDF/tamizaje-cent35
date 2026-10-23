const token = new URLSearchParams(window.location.search).get("token");
const loading = document.getElementById("public-loading");
const error = document.getElementById("public-error");
const content = document.getElementById("public-content");
document.getElementById("public-print")?.addEventListener("click", () => window.print());

function guidanceFor(signal, urgent) {
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
    description: "El promedio de los indicadores muestra señales que conviene revisar.",
    recommendation: "Consultá con un profesional para revisar las mediciones y definir el seguimiento adecuado.",
    whyTitle: "¿Cómo se obtiene este color?",
    why: "El color global promedia los controles disponibles. Las alertas rojas individuales y los criterios de atención inmediata se muestran por separado.",
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

function controlRecommendation(kind, signal, result) {
  if (kind === "imc") {
    const imc = Number.parseFloat(String(result.valorImc || "").replace(",", "."));
    if (signal === "VERDE") return "Tu IMC está dentro del rango de referencia usado por el sistema. Este indicador debe interpretarse junto con otros aspectos de tu salud.";
    if (signal === "AMARILLO" && Number.isFinite(imc) && imc < 18.5) return "Tu IMC está por debajo del rango de referencia. Una consulta profesional puede ayudar a evaluar tu situación nutricional y determinar si necesitás seguimiento.";
    if (signal === "AMARILLO") return "Tu IMC está por encima del rango de referencia. Podés consultar para evaluar tu salud y recibir orientación sobre hábitos adecuados para vos.";
    if (signal === "ROJO") return "Tu IMC alcanzó un nivel que merece una evaluación más detallada. Un profesional puede interpretarlo junto con tu cintura, antecedentes y otros indicadores.";
  }
  if (kind === "waist") {
    if (signal === "VERDE") return "Tu perímetro de cintura está por debajo del umbral de alerta usado. Mantené tus hábitos saludables y continuá con tus controles preventivos.";
    if (signal === "AMARILLO") return "Tu perímetro de cintura se encuentra en un nivel que conviene revisar. Podés consultar con un profesional para recibir orientación preventiva.";
    if (signal === "ROJO") return "Tu perímetro de cintura alcanzó un criterio de alerta. Te recomendamos consultar en un centro de salud para evaluar este resultado junto con los demás factores de riesgo.";
  }
  if (kind === "pressure") {
    if (result.derivacionInmediata) return "La medición alcanzó un criterio de urgencia. Avisá inmediatamente al responsable del puesto para recibir indicaciones de atención.";
    if (signal === "VERDE") return "Esta medición está por debajo de los umbrales de alerta usados. Una medición aislada no descarta problemas de presión; mantené tus controles habituales.";
    if (signal === "AMARILLO") return "La medición alcanzó un valor que conviene controlar. Te recomendamos consultar para revisar el resultado y determinar si es necesario repetirla.";
    if (signal === "ROJO") return "La medición alcanzó un criterio de alerta. Es importante que un profesional evalúe el resultado y determine el seguimiento necesario.";
  }
  return "";
}

function findriscRecommendation(category) {
  const messages = {
    BAJO: "Según el puntaje obtenido, el riesgo estimado se encuentra en la categoría baja de esta evaluación. Mantené hábitos saludables y continuá con tus controles preventivos.",
    LIGERAMENTE_ELEVADO: "El puntaje indica un riesgo ligeramente elevado según esta evaluación. Revisá tus hábitos de actividad física y alimentación, y mantené controles periódicos.",
    MODERADO: "El puntaje indica un riesgo moderado según esta evaluación. Podés solicitar un control de glucemia en un centro de salud y conversar con un profesional sobre tus factores de riesgo.",
    ALTO: "El puntaje indica un riesgo alto según esta evaluación. Solicitá una evaluación profesional y consultá si corresponde realizar un control de glucemia.",
    MUY_ALTO: "El puntaje se encuentra en la categoría muy alta de esta evaluación. Te recomendamos gestionar una consulta para evaluar tus factores de riesgo y definir si necesitás estudios adicionales.",
  };
  const key = String(category || "").toLocaleUpperCase("es").replaceAll(" ", "_");
  return messages[key] || "";
}
function labelFor(value) {
  if (!value) return "No disponible";
  return String(value)
    .toLocaleLowerCase("es")
    .replaceAll("_", " ")
    .replace(/(^|\s)\S/g, (letter) => letter.toLocaleUpperCase("es"));
}

function setDetail(elementId, primary, signal) {
  const element = document.getElementById(elementId);
  const primaryLabel = labelFor(primary);
  element.replaceChildren(document.createTextNode(primaryLabel));
  if (signal && labelFor(signal).toLocaleLowerCase("es") !== primaryLabel.toLocaleLowerCase("es")) {
    const badge = document.createElement("span");
    badge.className = "public-detail-signal";
    badge.dataset.signal = signal;
    badge.textContent = labelFor(signal);
    element.append(" ", badge);
  }
}

function setMeasurement(elementId, value, signal) {
  const element = document.getElementById(elementId);
  element.replaceChildren(document.createTextNode(value || "Sin dato"));
  if (!signal) return;
  const badge = document.createElement("span");
  badge.className = "public-detail-signal";
  badge.dataset.signal = signal;
  badge.textContent = labelFor(signal);
  element.append(" ", badge);
}

if (!token || !/^[a-f0-9]{32}$/i.test(token)) {
  loading.hidden = true;
  error.hidden = false;
} else {
  try {
    const response = await fetch(`/api/consulta-publica/${encodeURIComponent(token)}`, {
      cache: "no-store",
      headers: { Accept: "application/json" },
    });
    if (!response.ok) throw new Error("consulta no disponible");
    const result = await response.json();
    document.getElementById("public-record").textContent = `#${result.idRegistro}`;
    const status = document.getElementById("public-status");
    const signal = result.derivacionInmediata ? "ROJO" : result.semaforoGlobal;
    const guidance = guidanceFor(result.semaforoGlobal, result.derivacionInmediata);
    status.textContent = guidance.title;
    status.dataset.signal = signal;
    if (Number.isFinite(Number(result.promedioSemaforo))) {
      const score = result.derivacionInmediata ? 3 : Number(result.promedioSemaforo);
      const stops = [[0, 145, 76, 91], [.75, 60, 88, 86], [1, 32, 88, 85], [2, 10, 76, 82], [3, 0, 72, 75]];
      const bounded = Math.min(3, Math.max(0, score));
      const upper = stops.findIndex(([at]) => at >= bounded);
      const [fromAt, fromHue, fromSat, fromLight] = stops[Math.max(0, upper - 1)];
      const [toAt, toHue, toSat, toLight] = stops[Math.max(0, upper)];
      const amount = toAt === fromAt ? 0 : (bounded - fromAt) / (toAt - fromAt);
      const hue = Math.round(fromHue + (toHue - fromHue) * amount);
      const saturation = Math.round(fromSat + (toSat - fromSat) * amount);
      const lightness = Math.round(fromLight + (toLight - fromLight) * amount);
      status.style.backgroundColor = `hsl(${hue} ${saturation}% ${lightness}%)`;
      status.style.color = `hsl(${hue} ${Math.min(75, saturation)}% 27%)`;
    }
    document.getElementById("public-status-description").textContent = guidance.description;
    document.getElementById("public-recommendation").textContent = guidance.recommendation;
    document.getElementById("public-why-title").textContent = guidance.whyTitle;
    document.getElementById("public-why").textContent = guidance.why;
    document.querySelectorAll(".traffic-light-lamp").forEach((lamp) => {
      const active = lamp.dataset.signal === signal;
      lamp.classList.toggle("active", active);
      lamp.setAttribute("aria-pressed", String(active));
    });
    document.getElementById("public-traffic-light")?.setAttribute(
      "aria-label",
      `Semáforo global: ${labelFor(signal)}`,
    );
    setDetail("public-findrisc", result.categoriaFindrisc);
    setMeasurement("public-imc", result.valorImc ? `${result.valorImc} kg/m²` : null, result.semaforoImc);
    setMeasurement("public-pressure", result.valorPresion, result.semaforoPresion);
    setMeasurement("public-waist", result.valorCintura, result.semaforoCintura);
    const scoreNote = document.getElementById("public-findrisc-note");
    const scoreRecommendation = findriscRecommendation(result.categoriaFindrisc);
    if (scoreRecommendation) {
      scoreNote.textContent = `${scoreRecommendation} Este puntaje adaptado no mide la glucemia ni confirma diabetes; el control, si corresponde, se realiza en un centro de salud.`;
      scoreNote.hidden = false;
    }
    [
      ["public-imc-note", controlRecommendation("imc", result.semaforoImc, result)],
      ["public-waist-note", controlRecommendation("waist", result.semaforoCintura, result)],
      ["public-pressure-note", controlRecommendation("pressure", result.semaforoPresion, result)],
    ].forEach(([id, message]) => {
      const note = document.getElementById(id);
      if (message) {
        note.textContent = message;
        note.hidden = false;
      }
    });
    const cvText = String(result.riesgoCardiovascular || "").replace(/^MODO_SIMPLIFICADO_FACTORES_(\d+)$/, (_, total) => `${total} ${total === "1" ? "factor identificado" : "factores identificados"} · evaluación simplificada`);
    setDetail("public-cv", cvText);
    loading.hidden = true;
    content.hidden = false;
  } catch {
    loading.hidden = true;
    error.hidden = false;
  }
}
