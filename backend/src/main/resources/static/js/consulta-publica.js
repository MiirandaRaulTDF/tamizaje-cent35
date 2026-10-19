const token = new URLSearchParams(window.location.search).get("token");
const loading = document.getElementById("public-loading");
const error = document.getElementById("public-error");
const content = document.getElementById("public-content");
document.getElementById("public-print")?.addEventListener("click", () => window.print());

function titleFor(signal) {
  return ({
    VERDE: "Sin alerta prioritaria",
    AMARILLO: "Requiere seguimiento",
    ROJO: "Requiere atención del equipo de salud",
    SIN_CLASIFICAR: "Resultado no disponible",
  })[signal] || "Resultado de tu control";
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
    status.textContent = titleFor(result.semaforoGlobal);
    status.dataset.signal = result.semaforoGlobal;
    const signal = result.derivacionInmediata ? "ROJO" : result.semaforoGlobal;
    document.querySelectorAll(".traffic-light-lamp").forEach((lamp) => {
      const active = lamp.dataset.signal === signal;
      lamp.classList.toggle("active", active);
      lamp.setAttribute("aria-pressed", String(active));
    });
    document.getElementById("public-traffic-light")?.setAttribute(
      "aria-label",
      `Semáforo global: ${labelFor(signal)}`,
    );
    if (result.derivacionInmediata) {
      status.textContent = "Derivación inmediata indicada";
      status.dataset.signal = "ROJO";
    }
    setDetail("public-findrisc", result.categoriaFindrisc);
    setMeasurement("public-imc", result.valorImc ? `${result.valorImc} kg/m²` : null, result.semaforoImc);
    setMeasurement("public-pressure", result.valorPresion, result.semaforoPresion);
    setMeasurement("public-waist", result.valorCintura, result.semaforoCintura);
    const cvText = String(result.riesgoCardiovascular || "").replace(/^MODO_SIMPLIFICADO_FACTORES_(\d+)$/, (_, total) => `${total} ${total === "1" ? "factor identificado" : "factores identificados"} · evaluación simplificada`);
    setDetail("public-cv", cvText);
    document.getElementById("public-recommendation").textContent = result.recomendacion;
    loading.hidden = true;
    content.hidden = false;
  } catch {
    loading.hidden = true;
    error.hidden = false;
  }
}
