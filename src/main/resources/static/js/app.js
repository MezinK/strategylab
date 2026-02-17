"use strict";

const BASE_COLORS = ["#58a6ff","#f0883e","#f778ba","#7ee787","#d2a8ff"];

function getColor(i) {
  if (i < BASE_COLORS.length) return BASE_COLORS[i];
  // generate distinct hues via golden angle offset, varying saturation/lightness per cycle
  const offset = i - BASE_COLORS.length;
  const hue = (offset * 137.508) % 360;
  const saturation = 65 + (offset % 3) * 10;
  const lightness = 60 + (offset % 4) * 5;
  return `hsl(${Math.round(hue)}, ${saturation}%, ${lightness}%)`;
}
let strategies = [];
let slotCounter = 0;
let lastResults = null;
let lastInputs = null;

// ── Fetch available strategies on load ──
async function loadStrategies() {
  try {
    const res = await fetch("/api/strategies");
    strategies = await res.json();
  } catch {
    strategies = [
      { id: "BUY_AND_HOLD", displayName: "Buy & Hold", parameters: [] },
      { id: "DCA", displayName: "DCA", parameters: [
        { name: "contributionAmount", description: "Dollar amount per period", type: "number", defaultValue: "500" },
        { name: "frequencyDays", description: "Days between contributions", type: "integer", defaultValue: "21" }
      ]},
      { id: "MA_CROSSOVER", displayName: "MA Crossover", parameters: [
        { name: "shortWindow", description: "Short SMA window", type: "integer", defaultValue: "20" },
        { name: "longWindow", description: "Long SMA window", type: "integer", defaultValue: "50" }
      ]}
    ];
  }
  addSlot();
}

// ── Slot management ──
function addSlot() {
  const idx = slotCounter++;
  const color = getColor(idx);
  const defaultStart = "2020-01-01";
  const defaultEnd = "2024-12-31";

  const div = document.createElement("div");
  div.className = "slot";
  div.dataset.idx = idx;
  div.innerHTML = `
    <div class="slot-color" style="background:${color}"></div>
    <div class="slot-fields">
      <div class="field">
        <label>Symbol</label>
        <input type="text" class="w-sm" value="SPY" data-field="symbol" placeholder="e.g. AAPL" spellcheck="false">
      </div>
      <div class="field">
        <label>Strategy</label>
        <select data-field="strategyId">
          ${strategies.map(s => `<option value="${s.id}">${s.displayName}</option>`).join("")}
        </select>
      </div>
      <div class="field">
        <label>Start</label>
        <input type="date" value="${defaultStart}" data-field="startDate">
      </div>
      <div class="field">
        <label>End</label>
        <input type="date" value="${defaultEnd}" data-field="endDate">
      </div>
      <div class="field">
        <label>Capital</label>
        <input type="text" class="w-sm" value="10000" data-field="initialCapital">
      </div>
    </div>
    <div class="strategy-params" data-params></div>
    ${idx > 0 ? `<button class="slot-remove" type="button" title="Remove">&times;</button>` : ""}
  `;
  document.getElementById("slots").appendChild(div);

  const sel = div.querySelector("[data-field=strategyId]");
  sel.addEventListener("change", () => renderParams(div, sel.value));
  renderParams(div, sel.value);

  const rm = div.querySelector(".slot-remove");
  if (rm) rm.addEventListener("click", () => { div.remove(); recolorSlots(); });
}

function renderParams(slot, strategyId) {
  const container = slot.querySelector("[data-params]");
  const strat = strategies.find(s => s.id === strategyId);
  if (!strat || !strat.parameters.length) { container.innerHTML = ""; return; }
  container.innerHTML = `<span class="strategy-params-label">Strategy Params</span>` + strat.parameters.map(p => `
    <div class="field">
      <label>${p.name}</label>
      <input type="text" class="w-sm" value="${p.defaultValue || ""}" data-param="${p.name}" placeholder="${p.description || ""}">
    </div>
  `).join("");
}

function recolorSlots() {
  document.querySelectorAll(".slot").forEach((s, i) => {
    s.querySelector(".slot-color").style.background = getColor(i);
  });
}

function collectSlots() {
  const slots = document.querySelectorAll(".slot");
  return Array.from(slots).map(s => {
    const get = f => s.querySelector(`[data-field="${f}"]`).value.trim();
    const params = {};
    s.querySelectorAll("[data-param]").forEach(el => {
      if (el.value.trim()) params[el.dataset.param] = el.value.trim();
    });
    return {
      symbol: get("symbol").toUpperCase(),
      startDate: get("startDate"),
      endDate: get("endDate"),
      initialCapital: parseFloat(get("initialCapital")),
      strategyId: get("strategyId"),
      strategyParams: params
    };
  });
}

// ── Run backtest ──
async function runBacktest() {
  const btn = document.getElementById("run-btn");
  const status = document.getElementById("run-status");
  const backtests = collectSlots();

  btn.disabled = true;
  status.className = "run-status";
  status.innerHTML = `<span class="spinner"></span> Running...`;

  try {
    const res = await fetch("/api/backtest", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ backtests })
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({ error: res.statusText }));
      throw new Error(err.error || `HTTP ${res.status}`);
    }

    const data = await res.json();
    status.textContent = `Completed \u2014 ${data.results.length} result(s)`;
    renderResults(data.results, backtests);
  } catch (e) {
    status.className = "run-status error";
    status.textContent = e.message;
  } finally {
    btn.disabled = false;
  }
}

// ── Render all results ──
function renderResults(results, inputs) {
  lastResults = results;
  lastInputs = inputs;
  const section = document.getElementById("results");
  section.classList.add("visible");
  renderChart(results, inputs);
  renderMetrics(results, inputs);
  renderTrades(results, inputs);
}

// ── Canvas equity chart ──
function renderChart(results, inputs) {
  const canvas = document.getElementById("equity-chart");
  const ctx = canvas.getContext("2d");
  const dpr = window.devicePixelRatio || 1;
  const rect = canvas.parentElement.getBoundingClientRect();
  const w = rect.width - 40;
  const h = 380;
  canvas.width = w * dpr;
  canvas.height = h * dpr;
  canvas.style.width = w + "px";
  canvas.style.height = h + "px";
  ctx.scale(dpr, dpr);

  // build series
  const series = results.map((r, i) => ({
    label: `${inputs[i].symbol} \u2014 ${strategyLabel(r.strategyId)}`,
    color: getColor(i),
    points: r.equityCurve.map(p => ({ date: p.date, value: parseFloat(p.portfolioValue) }))
  }));

  // legend
  const legend = document.getElementById("chart-legend");
  legend.innerHTML = series.map(s =>
    `<div class="legend-item"><span class="legend-swatch" style="background:${s.color}"></span>${s.label}</div>`
  ).join("");

  // compute bounds
  let allVals = series.flatMap(s => s.points.map(p => p.value));
  let minVal = Math.min(...allVals) * 0.95;
  let maxVal = Math.max(...allVals) * 1.05;
  if (minVal === maxVal) { minVal -= 100; maxVal += 100; }

  const pad = { top: 12, right: 16, bottom: 36, left: 70 };
  const cw = w - pad.left - pad.right;
  const ch = h - pad.top - pad.bottom;

  const maxLen = Math.max(...series.map(s => s.points.length));
  const xScale = i => pad.left + (i / (maxLen - 1)) * cw;
  const yScale = v => pad.top + ch - ((v - minVal) / (maxVal - minVal)) * ch;

  // clear
  ctx.clearRect(0, 0, w, h);

  // grid
  ctx.strokeStyle = "#21262d";
  ctx.lineWidth = 1;
  const gridLines = 5;
  for (let i = 0; i <= gridLines; i++) {
    const y = pad.top + (i / gridLines) * ch;
    ctx.beginPath(); ctx.moveTo(pad.left, y); ctx.lineTo(w - pad.right, y); ctx.stroke();
    const val = maxVal - (i / gridLines) * (maxVal - minVal);
    ctx.fillStyle = "#484f58";
    ctx.font = "11px 'JetBrains Mono'";
    ctx.textAlign = "right";
    ctx.fillText(formatCurrency(val), pad.left - 10, y + 4);
  }

  // x-axis labels
  if (series.length > 0 && series[0].points.length > 0) {
    const pts = series[0].points;
    const labelCount = Math.min(6, pts.length);
    ctx.fillStyle = "#484f58";
    ctx.textAlign = "center";
    ctx.font = "10px 'JetBrains Mono'";
    for (let i = 0; i < labelCount; i++) {
      const idx = Math.round((i / (labelCount - 1)) * (pts.length - 1));
      const x = xScale(idx);
      ctx.fillText(pts[idx].date, x, h - 8);
    }
  }

  // draw lines
  series.forEach(s => {
    ctx.beginPath();
    ctx.strokeStyle = s.color;
    ctx.lineWidth = 1.8;
    ctx.lineJoin = "round";
    s.points.forEach((p, i) => {
      const x = xScale(i);
      const y = yScale(p.value);
      if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
    });
    ctx.stroke();

    // subtle fill
    ctx.globalAlpha = 0.04;
    ctx.lineTo(xScale(s.points.length - 1), pad.top + ch);
    ctx.lineTo(xScale(0), pad.top + ch);
    ctx.closePath();
    ctx.fillStyle = s.color;
    ctx.fill();
    ctx.globalAlpha = 1;
  });

  // tooltip on hover
  const tooltip = document.getElementById("chart-tooltip");
  canvas.onmousemove = e => {
    const br = canvas.getBoundingClientRect();
    const mx = e.clientX - br.left;
    const my = e.clientY - br.top;
    if (mx < pad.left || mx > w - pad.right) { tooltip.classList.remove("visible"); return; }
    const idx = Math.round(((mx - pad.left) / cw) * (maxLen - 1));
    if (idx < 0 || idx >= maxLen) { tooltip.classList.remove("visible"); return; }

    let html = "";
    series.forEach(s => {
      if (idx < s.points.length) {
        const p = s.points[idx];
        if (!html) html += `<div style="color:var(--text-muted);margin-bottom:6px">${p.date}</div>`;
        html += `<div style="display:flex;justify-content:space-between;gap:16px;color:${s.color}"><span style="opacity:.8">${s.label}</span><span>${formatCurrency(p.value)}</span></div>`;
      }
    });
    tooltip.innerHTML = html;
    tooltip.classList.add("visible");
    tooltip.style.left = (mx + 24) + "px";
    tooltip.style.top = (my - 10) + "px";
    if (mx > w * 0.65) tooltip.style.left = (mx - tooltip.offsetWidth - 16) + "px";
  };
  canvas.onmouseleave = () => tooltip.classList.remove("visible");
}

// ── Metrics cards ──
function renderMetrics(results, inputs) {
  const grid = document.getElementById("metrics-grid");
   grid.innerHTML = results.map((r, i) => {
    const m = r.metrics;
    const color = getColor(i);
    const label = `${inputs[i].symbol} \u2014 ${strategyLabel(r.strategyId)}`;
    return `<div class="metric-card">
      <h3><span class="dot" style="background:${color}"></span>${label}</h3>
      <div class="metric-rows">
        ${metricRow("Final Value", formatCurrency(m.finalValue), parseFloat(m.finalValue) >= parseFloat(m.totalContributions))}
        ${metricRow("Total Contributed", formatCurrency(m.totalContributions))}
        ${metricRow("Return", formatPct((parseFloat(m.finalValue) / parseFloat(m.totalContributions) - 1) * 100), parseFloat(m.finalValue) >= parseFloat(m.totalContributions))}
        ${metricRow("CAGR", formatPct(parseFloat(m.cagr) * 100), parseFloat(m.cagr) >= 0)}
        ${metricRow("Max Drawdown", formatPct(parseFloat(m.maxDrawdown) * 100), false)}
        ${metricRow("Volatility", formatPct(parseFloat(m.annualizedVolatility) * 100))}
        ${metricRow("Sharpe Ratio", parseFloat(m.sharpeRatio).toFixed(2), parseFloat(m.sharpeRatio) >= 0)}
        ${metricRow("Trades", m.numberOfTrades)}
      </div>
    </div>`;
  }).join("");
}

function metricRow(label, value, positive) {
  let cls = "metric-value";
  if (positive === true) cls += " positive";
  else if (positive === false && label !== "Volatility" && label !== "Trades" && label !== "Total Contributed") cls += " negative";
  return `<div class="metric-row"><span class="metric-label">${label}</span><span class="${cls}">${value}</span></div>`;
}

// ── Trades table ──
function renderTrades(results, inputs) {
  const section = document.getElementById("trades-section");
  section.innerHTML = results.map((r, i) => {
    const color = getColor(i);
    const label = `${inputs[i].symbol} \u2014 ${strategyLabel(r.strategyId)}`;
    if (!r.trades.length) return `
      <div class="trades-strategy collapsed">
        <div class="trades-strategy-header"><span class="dot" style="background:${color}"></span>${label}<span class="chevron">&#9660;</span></div>
        <div class="trades-body" style="padding:16px 18px;color:var(--text-muted);font-size:.82rem">No trades recorded.</div>
      </div>`;
    return `<div class="trades-strategy collapsed">
      <div class="trades-strategy-header"><span class="dot" style="background:${color}"></span>${label} &mdash; ${r.trades.length} trade(s)<span class="chevron">&#9660;</span></div>
      <div class="trades-body" style="overflow-x:auto">
      <table>
        <thead><tr><th>Date</th><th>Action</th><th>Qty</th><th>Price</th><th>Reason</th></tr></thead>
        <tbody>${r.trades.map(t => `<tr>
          <td>${t.date}</td>
          <td class="action-${t.action.toLowerCase()}">${t.action}</td>
          <td>${parseFloat(t.quantity).toFixed(4)}</td>
          <td>${formatCurrency(t.price)}</td>
          <td class="reason" title="${esc(t.reason)}">${esc(t.reason)}</td>
        </tr>`).join("")}</tbody>
      </table>
      </div>
    </div>`;
  }).join("");

  // wire up collapse toggles
  section.querySelectorAll(".trades-strategy-header").forEach(header => {
    header.addEventListener("click", () => {
      header.parentElement.classList.toggle("collapsed");
    });
  });
}

// ── Helpers ──
function strategyLabel(id) {
  const s = strategies.find(s => s.id === id);
  return s ? s.displayName : id;
}

function formatCurrency(v) {
  const n = typeof v === "number" ? v : parseFloat(v);
  return "$" + n.toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function formatPct(v) {
  return (v >= 0 ? "+" : "") + v.toFixed(2) + "%";
}

function esc(s) {
  const d = document.createElement("div"); d.textContent = s; return d.innerHTML;
}

// ── Event wiring ──
document.getElementById("add-slot").addEventListener("click", addSlot);
document.getElementById("run-btn").addEventListener("click", runBacktest);
document.getElementById("tabs").addEventListener("click", e => {
  const tab = e.target.closest(".tab");
  if (!tab) return;
  document.querySelectorAll(".tab").forEach(t => t.classList.remove("active"));
  document.querySelectorAll(".tab-content").forEach(t => t.classList.remove("active"));
  tab.classList.add("active");
  document.getElementById("tab-" + tab.dataset.tab).classList.add("active");
  if (tab.dataset.tab === "chart" && lastResults) {
    renderChart(lastResults, lastInputs);
  }
});

// resize chart
window.addEventListener("resize", () => {
  if (lastResults && document.getElementById("tab-chart").classList.contains("active")) {
    renderChart(lastResults, lastInputs);
  }
});

// boot
loadStrategies();
