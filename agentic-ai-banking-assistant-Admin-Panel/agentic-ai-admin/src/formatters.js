export function safeParse(value) {
  try {
    return value ? JSON.parse(value) : null;
  } catch {
    return null;
  }
}

export function escapeHtml(value) {
  return String(value ?? "").replace(/[&<>"']/g, (char) => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    '"': "&quot;",
    "'": "&#39;"
  })[char]);
}

export function formatNumber(value) {
  if (value === null || value === undefined || value === "") return "—";
  return new Intl.NumberFormat("en-US", { maximumFractionDigits: 2 }).format(Number(value));
}

export function formatMoney(value, currency = "LKR") {
  if (value === null || value === undefined || value === "") return "—";
  const amount = Number(value);
  if (!Number.isFinite(amount)) return `${escapeHtml(value)} ${escapeHtml(currency)}`;
  return `${escapeHtml(currency)} ${new Intl.NumberFormat("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(amount)}`;
}

export function formatDate(value) {
  if (!value) return "—";
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? escapeHtml(value)
    : date.toLocaleString(undefined, { dateStyle: "medium", timeStyle: "short" });
}

export function humanize(value) {
  return String(value ?? "unknown").replaceAll("_", " ").toLowerCase();
}

export function statusTag(value) {
  const normalized = String(value || "unknown").toLowerCase().replaceAll("_", "-");
  return `<span class="status status-${escapeHtml(normalized)}">${escapeHtml(humanize(value))}</span>`;
}
