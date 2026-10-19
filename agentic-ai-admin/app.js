import { createApiClient, apiErrorText } from "./src/api.js";
import { DEFAULT_API_BASE, MODULES, PAGES, STORAGE_KEYS } from "./src/config.js";
import { escapeHtml, formatDate, formatMoney, formatNumber, humanize, safeParse, statusTag } from "./src/formatters.js";

const root = document.getElementById("app");
const savedApiBase = localStorage.getItem(STORAGE_KEYS.apiBase);
const state = {
  apiBase: (savedApiBase && !/^https?:\/\/(?:localhost|127\.0\.0\.1):(?:5173|8081)\/api\/v1\/?$/i.test(savedApiBase) ? savedApiBase : DEFAULT_API_BASE).replace(/\/+$/, ""),
  token: sessionStorage.getItem(STORAGE_KEYS.token),
  admin: safeParse(sessionStorage.getItem(STORAGE_KEYS.admin)) || null,
  page: "dashboard",
  sidebarOpen: false,
  dashboardDays: 30,
  tablePages: {},
  tableFilters: {},
  tableRows: {}
};
const pages = PAGES;
const modules = MODULES;
const api = createApiClient({
  getApiBase: () => state.apiBase,
  getToken: () => state.token
});

  function hasPermission(permission) {
    const permissions = state.admin?.permissions;
    return Array.isArray(permissions) && (permissions.includes("*") || permissions.includes(permission));
  }

  function hasExportPermission(page) {
    const permission = {
      users: "users:export",
      transactions: "transactions:export",
      bills: "bill-payments:export",
      security: "security:export",
      ai: "ai:export",
      audit: "audit:export"
    }[page];
    return hasPermission("reports:export") || Boolean(permission && hasPermission(permission));
  }

  function moduleHeading(key) {
    const actions = [];
    if (key === "users" && hasPermission("users:create")) actions.push(`<button class="btn btn-primary" data-workflow="create-user" type="button">Register user</button>`);
    if (key === "administrators" && state.admin?.role === "SUPER_ADMIN") actions.push(`<button class="btn btn-primary" data-workflow="create-admin" type="button">Register Admin</button>`);
    if (hasExportPermission(key)) actions.push(`<button class="btn btn-secondary" data-export="${key}" type="button">Export CSV</button>`);
    return heading(key, actions.join(""));
  }

  function persistApiBase(value) {
    state.apiBase = value.trim().replace(/\/+$/, "");
    localStorage.setItem(STORAGE_KEYS.apiBase, state.apiBase);
  }

  function hasAccess(page) {
    if (state.admin?.mustChangeCredentials) return false;
    if (page === "settings") return true;
    const permissions = state.admin?.permissions;
    if (!Array.isArray(permissions)) return false;
    if (!permissions.includes("*") && !permissions.includes(pages[page]?.permission)) return false;
    const capabilities = state.admin?.capabilities;
    return !Array.isArray(capabilities) || capabilities.includes(page);
  }

  function loginMarkup(error = "") {
    return `<main class="login-page">
      <section class="login-aside" aria-label="Bank operations">
        <div class="brand"><span class="brand-mark">A</span><span>Agentic AI Assistant <span style="font-weight:500;color:#a9bdd6">/ Admin</span></span></div>
        <div class="login-aside-copy"><p class="eyebrow">Banking operations console</p><h1>Clarity for every critical decision.</h1><p>A secure workspace for customer operations, financial audits, risk investigations and AI oversight.</p></div>
        <div class="secure-note"><span aria-hidden="true">●</span> Protected administrator access · Activity is auditable</div>
      </section>
      <section class="login-main">
        <form class="login-card" id="login-form">
          <h2>Welcome back</h2><p>Sign in with your provisioned administrator account.</p>
          ${error ? `<div class="notice" role="alert">${escapeHtml(error)}</div>` : ""}
          <div class="field"><label for="login-email">Work email</label><input id="login-email" name="email" type="email" autocomplete="username" required placeholder="name@bank.com"></div>
          <div class="field"><label for="login-password">Password</label><input id="login-password" name="password" type="password" autocomplete="current-password" required placeholder="Enter your password"></div>
          <button class="btn btn-primary btn-full" type="submit">Sign in securely</button>
        </form>
      </section>
    </main>`;
  }

  function firstLoginCredentialsMarkup(error = "") {
    return `<main class="login-page"><section class="login-main"><form class="login-card" id="first-login-form">
      <h2>Change your initial credentials</h2>
      <p>Your Super Admin provided temporary sign-in credentials. Set your own username and password before continuing.</p>
      ${error ? `<div class="notice" role="alert">${escapeHtml(error)}</div>` : ""}
      <div class="field"><label for="first-login-email">New username (email)</label><input id="first-login-email" name="email" type="email" required maxlength="254" autocomplete="username"></div>
      <div class="field"><label for="first-login-password">New password</label><input id="first-login-password" name="password" type="password" required minlength="14" maxlength="72" autocomplete="new-password"><small>At least 14 characters; maximum 72 UTF-8 bytes.</small></div>
      <div class="field"><label for="first-login-confirm">Confirm new password</label><input id="first-login-confirm" name="confirmPassword" type="password" required minlength="14" maxlength="72" autocomplete="new-password"></div>
      <button class="btn btn-primary btn-full" type="submit">Save credentials and continue</button>
      <button class="btn btn-secondary btn-full" id="first-login-logout" type="button" style="margin-top:10px">Sign out</button>
    </form></section></main>`;
  }

  function shellMarkup() {
    const sections = ["OVERVIEW", "OPERATIONS", "RISK & CONTROLS", "ADMINISTRATION"];
    const nav = sections.map((section) => {
      const links = Object.entries(pages).filter(([key, item]) => item.section === section && hasAccess(key));
      if (!links.length) return "";
      return `<div class="nav-label">${section}</div><nav class="nav-list" aria-label="${section}">${links.map(([key, item]) =>
        `<button class="nav-link ${state.page === key ? "active" : ""}" data-page="${key}" type="button"><span class="nav-glyph">${item.glyph}</span>${item.label}</button>`
      ).join("")}</nav>`;
    }).join("");
    const displayName = state.admin?.fullName || state.admin?.name || "Administrator";
    const role = state.admin?.role || "Authorized access";
    const initial = displayName.trim().slice(0, 1).toUpperCase() || "A";
    return `<div class="shell">
      <aside class="sidebar ${state.sidebarOpen ? "open" : ""}" id="sidebar">
        <div class="brand"><span class="brand-mark">A</span><span>Agentic AI Assistant <span style="font-weight:500;color:#a9bdd6">/ Admin</span></span></div>
        ${nav}
        <div class="sidebar-bottom"><div class="admin-mini"><span class="avatar">${escapeHtml(initial)}</span><div><strong>${escapeHtml(displayName)}</strong><span>${escapeHtml(role)}</span></div></div></div>
      </aside>
      <div class="workspace">
        <header class="topbar">
          <button class="icon-button menu-toggle" id="menu-toggle" type="button" aria-label="Toggle navigation">☰</button>
          <div class="breadcrumb">Agentic AI Assistant <strong>/ ${escapeHtml(pages[state.page].label)}</strong></div>
          <div class="top-actions"><span class="env-chip"><span class="env-dot"></span>Admin API</span><button class="icon-button" id="logout" type="button" title="Sign out" aria-label="Sign out">↪</button></div>
        </header>
        <main class="main-content" id="page-content"><div class="loading">Loading secure workspace</div></main>
      </div>
      <dialog class="record-dialog" id="record-dialog" aria-labelledby="record-dialog-title">
        <div class="record-dialog-head"><div><p class="eyebrow">Read-only record</p><h2 id="record-dialog-title">Record details</h2></div><button class="icon-button" data-close-dialog type="button" aria-label="Close details">×</button></div>
        <div class="record-dialog-body" id="record-dialog-body"></div>
      </dialog>
      <dialog class="record-dialog workflow-dialog" id="workflow-dialog" aria-labelledby="workflow-title">
        <div class="record-dialog-head"><div><p class="eyebrow">Authorized operation</p><h2 id="workflow-title">Administrative action</h2></div><button class="icon-button" data-close-workflow type="button" aria-label="Close form">×</button></div>
        <div class="record-dialog-body" id="workflow-body"></div>
      </dialog>
    </div>`;
  }

  function heading(page = state.page, actions = "") {
    const meta = pages[page];
    return `<div class="page-heading"><div><h1>${escapeHtml(meta.label)}</h1><p>${escapeHtml(meta.description)}</p></div>${actions ? `<div class="heading-actions">${actions}</div>` : ""}</div>`;
  }

  function errorPanel(error, retryAction = "") {
    return `<div class="error-banner" role="alert"><div><strong>Live data unavailable</strong>${escapeHtml(apiErrorText(error))}</div>${retryAction ? `<button class="retry" data-retry="${retryAction}" type="button">Retry</button>` : ""}</div>`;
  }

  function loadingPanel() {
    return `<div class="loading">Loading live data</div>`;
  }

  function emptyPanel(title, detail) {
    return `<div class="empty-state"><span class="empty-icon">—</span><h3>${escapeHtml(title)}</h3><p>${escapeHtml(detail)}</p></div>`;
  }

  function card(label, value, sub, glyph) {
    return `<article class="stat-card"><div class="stat-top"><span class="stat-label">${escapeHtml(label)}</span><span class="stat-icon">${escapeHtml(glyph)}</span></div><div class="stat-value">${value}</div><div class="stat-sub">${escapeHtml(sub)}</div></article>`;
  }

  function renderLineChart(points) {
    const values = Array.isArray(points) ? points.filter((point) => Number.isFinite(Number(point.value))) : [];
    if (!values.length) return emptyPanel("No trend data available", "The admin API has not returned transaction trends for this period.");
    const width = 640, height = 190, left = 30, right = 12, top = 12, bottom = 28;
    const max = Math.max(...values.map((item) => Number(item.value)), 1);
    const plotHeight = height - top - bottom, plotWidth = width - left - right;
    const coords = values.map((item, index) => ({
      x: left + (values.length === 1 ? plotWidth / 2 : index * plotWidth / (values.length - 1)),
      y: top + plotHeight - Number(item.value) / max * plotHeight
    }));
    const path = coords.map((point, index) => `${index ? "L" : "M"}${point.x.toFixed(1)},${point.y.toFixed(1)}`).join(" ");
    const area = `${path} L${coords.at(-1).x},${height - bottom} L${coords[0].x},${height - bottom} Z`;
    const labels = values.map((point, index) => index % Math.max(1, Math.ceil(values.length / 6)) === 0 || index === values.length - 1
      ? `<text class="chart-label" x="${coords[index].x}" y="${height - 6}" text-anchor="middle">${escapeHtml(point.label || "")}</text>` : "").join("");
    const grids = [0, 1, 2, 3].map((index) => {
      const y = top + index * plotHeight / 3;
      return `<line class="chart-gridline" x1="${left}" x2="${width - right}" y1="${y}" y2="${y}"/>`;
    }).join("");
    return `<div class="chart-wrap"><svg viewBox="0 0 ${width} ${height}" role="img" aria-label="Transaction volume over time"><defs><linearGradient id="chartFill" x1="0" x2="0" y1="0" y2="1"><stop offset="0%" stop-color="#4b8de4" stop-opacity=".23"/><stop offset="100%" stop-color="#4b8de4" stop-opacity="0"/></linearGradient></defs>${grids}<path class="chart-area" d="${area}"/><path class="chart-line" d="${path}"/>${labels}</svg></div>`;
  }

  function renderStatusBars(statuses) {
    const entries = Object.entries(statuses || {}).filter(([, value]) => value !== null && value !== undefined);
    if (!entries.length) return emptyPanel("No payment breakdown available", "Status totals will appear when the admin API supplies payment analytics.");
    const max = Math.max(...entries.map(([, value]) => Number(value) || 0), 1);
    return `<div class="bar-list">${entries.map(([name, value]) => `<div class="bar-item"><div class="bar-item-head"><span>${escapeHtml(humanize(name))}</span><strong>${formatNumber(value)}</strong></div><div class="bar-track"><div class="bar-fill" style="width:${Math.min(100, (Number(value) || 0) / max * 100)}%"></div></div></div>`).join("")}</div>`;
  }

  function renderRecent(items) {
    if (!Array.isArray(items) || !items.length) return `<div class="empty-activity">No recent registrations were returned.</div>`;
    return `<div class="activity-list">${items.slice(0, 5).map((item) => `<div class="activity-row"><span class="activity-dot"></span><div class="activity-copy"><strong>${escapeHtml(item.fullName || item.name || item.customerId || "Customer")}</strong> registered${item.status ? ` · ${statusTag(item.status)}` : ""}</div><span class="activity-time">${formatDate(item.registeredAt || item.createdAt)}</span></div>`).join("")}</div>`;
  }

  async function loadDashboard(days = state.dashboardDays) {
    state.dashboardDays = Number(days) || 30;
    const host = document.getElementById("page-content");
    if (!host) return;
    host.innerHTML = `${heading("dashboard", `<select class="date-select" id="dashboard-days" aria-label="Dashboard date range"><option value="7" ${state.dashboardDays === 7 ? "selected" : ""}>Last 7 days</option><option value="30" ${state.dashboardDays === 30 ? "selected" : ""}>Last 30 days</option><option value="90" ${state.dashboardDays === 90 ? "selected" : ""}>Last 90 days</option></select><button class="btn btn-secondary" data-retry="dashboard" type="button">Refresh</button>`)}${loadingPanel()}`;
    try {
      const query = new URLSearchParams({ days: String(state.dashboardDays) });
      const response = await api(`/admin/dashboard?${query}`);
      const data = await response.json();
      if (state.page !== "dashboard") return;
      const users = data.users || {};
      const transactions = data.transactions || {};
      const bills = data.billPayments || {};
      const alerts = data.securityAlerts || {};
      const ai = data.aiActivity || {};
      host.innerHTML = `${heading("dashboard", `<select class="date-select" id="dashboard-days" aria-label="Dashboard date range"><option value="7" ${state.dashboardDays === 7 ? "selected" : ""}>Last 7 days</option><option value="30" ${state.dashboardDays === 30 ? "selected" : ""}>Last 30 days</option><option value="90" ${state.dashboardDays === 90 ? "selected" : ""}>Last 90 days</option></select><button class="btn btn-secondary" data-retry="dashboard" type="button">Refresh</button>`)}
        <div class="stat-grid">
          ${card("Registered users", formatNumber(users.total), `${formatNumber(users.active)} active · ${formatNumber(users.inactive)} inactive · ${formatNumber(users.suspended)} suspended · ${formatNumber(users.blocked)} blocked`, "US")}
          ${card("Transaction volume", formatMoney(transactions.volume, transactions.currency || "LKR"), `${formatNumber(transactions.total)} transactions in period`, "TX")}
          ${card("Bill payment volume", formatMoney(bills.volume, bills.currency || "LKR"), `${formatNumber(bills.total)} payments in period`, "BP")}
          ${card("Open security alerts", formatNumber(alerts.open), `${formatNumber(alerts.critical)} critical · ${formatNumber(alerts.total)} total · ${formatNumber(ai.total)} AI interactions`, "SC")}
        </div>
        <div class="section-title"><h2>Activity overview</h2><span>Numbers reflect the selected period and returned API data</span></div>
        <div class="dashboard-grid">
          <section class="panel"><div class="panel-heading"><div><h3>Transaction volume trend</h3><p>Authoritative transaction totals by period</p></div></div>${renderLineChart(data.transactionTrend)}</section>
          <section class="panel"><div class="panel-heading"><div><h3>Transaction status</h3><p>Outcome distribution for the selected period</p></div></div>${renderStatusBars(transactions.byStatus)}</section>
        </div>
        <div class="lower-grid">
          <section class="panel"><div class="panel-heading"><div><h3>Recent registrations</h3><p>Latest customer accounts</p></div><button class="btn btn-secondary" data-page="users" type="button">View users</button></div>${renderRecent(data.recentRegistrations)}</section>
          <section class="panel"><div class="panel-heading"><div><h3>Operations snapshot</h3><p>Successful, pending and exception counts</p></div></div>${renderStatusBars({ "Successful transactions": transactions.successful, "Pending transactions": transactions.pending, "Failed transactions": transactions.failed, "Reversed transactions": transactions.reversed, "Failed bill payments": bills.failed, "Pending bill payments": bills.pending })}</section>
        </div>`;
    } catch (error) {
      if (state.page === "dashboard") host.innerHTML = `${heading("dashboard", `<button class="btn btn-secondary" data-retry="dashboard" type="button">Retry</button>`)}${errorPanel(error, "dashboard")}${emptyPanel("Dashboard data is not available", "No sample values are displayed. Connect an authorized admin API to populate these metrics.")}`;
    }
  }

  function cellValue(item, key) {
    const value = item?.[key];
    if (value === null || value === undefined || value === "") return "—";
    if (key.toLowerCase().includes("status") || key === "outcome" || key === "severity") return statusTag(value);
    if (key.toLowerCase().includes("date") || key.endsWith("At")) return formatDate(value);
    if (key === "amount") return formatMoney(value, item.currency || "LKR");
    if (Array.isArray(value)) return escapeHtml(value.join(", ") || "—");
    if (typeof value === "object") return escapeHtml(JSON.stringify(value));
    return escapeHtml(value);
  }

  function safeDetailEntries(value, prefix = "", depth = 0) {
    if (!value || typeof value !== "object" || depth > 2) return [];
    const sensitive = /password|secret|token|credential|authorization|cookie|otp|pin|cvv|security.?code|private.?key|access.?key|jwt/i;
    const entries = [];
    for (const [key, field] of Object.entries(value)) {
      if (sensitive.test(key) || field === null || field === undefined || typeof field === "function") continue;
      const label = prefix ? `${prefix} · ${humanize(key)}` : humanize(key);
      if (typeof field === "object" && !Array.isArray(field)) {
        entries.push(...safeDetailEntries(field, label, depth + 1));
      } else {
        let display = Array.isArray(field) ? field.join(", ") : String(field);
        if (!/masked/i.test(key) && /mobile|phone|telephone/i.test(key)) {
          const digits = display.replace(/\D/g, "");
          display = digits.length > 4 ? `${"•".repeat(Math.min(8, digits.length - 4))}${digits.slice(-4)}` : "••••";
        } else if (!/masked/i.test(key) && /email/i.test(key)) {
          const [name, domain] = display.split("@");
          display = domain ? `${name.slice(0, 1)}•••@${domain}` : "••••";
        } else if (!/masked/i.test(key) && /account.?(no|number)|card.?(no|number)|iban/i.test(key)) {
          const compact = display.replace(/\s/g, "");
          display = compact.length > 4 ? `${"•".repeat(Math.min(12, compact.length - 4))}${compact.slice(-4)}` : "••••";
        }
        entries.push([label, display]);
      }
    }
    return entries;
  }

  function detailMarkup(record) {
    const entries = safeDetailEntries(record);
    if (!entries.length) return emptyPanel("No safe detail fields", "The record did not include displayable fields.");
    return `<dl class="detail-grid">${entries.map(([label, value]) => `<div class="detail-item"><dt>${escapeHtml(label)}</dt><dd>${escapeHtml(value)}</dd></div>`).join("")}</dl><p class="detail-disclaimer">Sensitive credentials are omitted. Access controls and data redaction must also be enforced by the backend.</p>`;
  }

  async function openRecordDetail(key, index) {
    const module = modules[key];
    const record = state.tableRows[key]?.[index];
    const dialog = document.getElementById("record-dialog");
    const body = document.getElementById("record-dialog-body");
    if (!record || !dialog || !body) return;
    const id = record.id || record.transactionId || record.paymentId || record.customerId || record.alertId || record.interactionId || record.eventId || record.administratorId;
    const title = record.transactionRef || record.paymentRef || record.fullName || record.customerName || record.alertId || record.interactionId || record.eventId || record.administratorName || record.id || "Record details";
    document.getElementById("record-dialog-title").textContent = String(title);
    dialog.showModal();
    body.innerHTML = loadingPanel();
    if (!id) {
      body.innerHTML = `<div class="notice info">The list record does not contain a supported record identifier, so detail lookup is unavailable.</div>${detailMarkup(record)}`;
      return;
    }
    try {
      const response = await api(`${module.endpoint}/${encodeURIComponent(id)}`);
      const details = await response.json();
      body.innerHTML = detailMarkup(details);
    } catch (error) {
      body.innerHTML = `${errorPanel(error)}${detailMarkup(record)}`;
    }
  }

  function openWorkflow(action, index = null) {
    const dialog = document.getElementById("workflow-dialog");
    const title = document.getElementById("workflow-title");
    const body = document.getElementById("workflow-body");
    if (!dialog || !title || !body) return;
    let content = "";
    if (action === "create-admin" && state.admin?.role === "SUPER_ADMIN") {
      title.textContent = "Register an Admin";
      content = `<form id="workflow-form" data-action="create-admin">
        <div class="notice info">The new account will have the ADMIN role and must change its initial credentials after first login. The email address is also its username.</div>
        <div class="field"><label for="admin-full-name">Full name</label><input id="admin-full-name" name="fullName" required maxlength="160" autocomplete="name"></div>
        <div class="field"><label for="admin-email">Username (email address)</label><input id="admin-email" name="email" type="email" required maxlength="254" autocomplete="username"></div>
        <div class="field"><label for="admin-initial-password">Initial password</label><input id="admin-initial-password" name="password" type="password" required minlength="14" maxlength="72" autocomplete="new-password"><small>Use at least 14 characters. The password is stored only as a secure hash.</small></div>
        <button class="btn btn-primary" type="submit">Create Admin account</button></form>`;
    } else if (action === "create-user" && hasPermission("users:create")) {
      title.textContent = "Register a customer";
      content = `<form id="workflow-form" data-action="create-user"><div class="notice info">The backend must validate identifiers, enforce uniqueness, provision access securely and audit this registration. Do not include a password here.</div>
        <div class="field"><label for="new-cif">Banking customer identifier (CIF)</label><input id="new-cif" name="cif" required maxlength="80" autocomplete="off"></div>
        <div class="field"><label for="new-name">Full name</label><input id="new-name" name="fullName" required maxlength="160" autocomplete="name"></div>
        <div class="field"><label for="new-mobile">Mobile number</label><input id="new-mobile" name="mobile" type="tel" required maxlength="40" autocomplete="tel"></div>
        <div class="field"><label for="new-email">Email address (if supported)</label><input id="new-email" name="email" type="email" maxlength="254" autocomplete="email"></div>
        <div class="field"><label for="new-language">Preferred language</label><select id="new-language" name="preferredLanguage"><option value="EN">English</option><option value="SI">Sinhala</option><option value="TA">Tamil</option></select></div>
        <button class="btn btn-primary" type="submit">Submit registration</button></form>`;
    } else if (action === "user-status" && hasPermission("users:status:update")) {
      const customer = state.tableRows.users?.[index];
      if (!customer) return;
      const id = customer.customerId || customer.id;
      if (!id) return toast("This user record does not include an identifier for a status update.");
      title.textContent = "Change account status";
      content = `<form id="workflow-form" data-action="user-status" data-record-id="${escapeHtml(id)}"><div class="notice info">Status changes require server-side policy checks, authorization and an immutable audit event. Current status: ${escapeHtml(humanize(customer.status))}.</div>
        <div class="field"><label for="new-status">New status</label><select id="new-status" name="status" required><option value="ACTIVE">Active</option><option value="BLOCKED">Blocked</option></select></div>
        <div class="field"><label for="status-reason">Reason</label><input id="status-reason" name="reason" required maxlength="500"></div>
        <button class="btn btn-primary" type="submit">Submit status change</button></form>`;
    } else if (action === "security-case" && hasPermission("security:investigate")) {
      const alert = state.tableRows.security?.[index];
      if (!alert) return;
      const id = alert.alertId || alert.id;
      if (!id) return toast("This alert does not include an identifier for case review.");
      title.textContent = "Review security case";
      content = `<form id="workflow-form" data-action="security-case" data-record-id="${escapeHtml(id)}"><div class="notice info">Signals are investigative leads, not proof of fraud. The backend must record investigator identity and preserve case history.</div>
        <div class="field"><label for="case-status">Investigation status</label><select id="case-status" name="status" required><option value="OPEN">Open</option><option value="INVESTIGATING">Investigating</option><option value="RESOLVED">Resolved</option><option value="FALSE_POSITIVE">False positive</option></select></div>
        <div class="field"><label for="case-notes">Investigation notes</label><input id="case-notes" name="notes" required maxlength="2000"></div>
        <div class="field"><label for="case-assignee">Assignee (optional)</label><input id="case-assignee" name="assignedTo" maxlength="120"></div>
        <button class="btn btn-primary" type="submit">Save investigation</button></form>`;
    } else {
      return toast("Your administrator permissions do not allow this action.");
    }
    body.innerHTML = content;
    dialog.showModal();
    body.querySelector("form")?.addEventListener("submit", submitWorkflow);
  }

  async function submitWorkflow(event) {
    event.preventDefault();
    const form = event.currentTarget;
    const data = Object.fromEntries(new FormData(form).entries());
    const action = form.dataset.action;
    const button = form.querySelector('button[type="submit"]');
    button.disabled = true;
    button.textContent = "Submitting…";
    try {
      if (action === "create-admin") {
        await api("/admin/administrators", { method: "POST", body: JSON.stringify(data) });
        toast("Admin account created. The account is marked to change its initial credentials on first login.");
      } else if (action === "create-user") {
        await api("/admin/users", { method: "POST", body: JSON.stringify(data) });
        state.tablePages.users = 0;
        toast("User registration submitted.");
      } else if (action === "user-status") {
        await api(`/admin/users/${encodeURIComponent(form.dataset.recordId)}/status`, { method: "PATCH", body: JSON.stringify(data) });
        toast("Account status updated.");
      } else if (action === "security-case") {
        await api(`/admin/security/alerts/${encodeURIComponent(form.dataset.recordId)}/investigation`, { method: "PATCH", body: JSON.stringify(data) });
        toast("Security case updated.");
      }
      form.reset();
      document.getElementById("workflow-dialog")?.close();
      await loadModule(action === "security-case" ? "security" : action === "create-admin" ? "administrators" : "users");
    } catch (error) {
      button.disabled = false;
      button.textContent = "Retry";
      const existing = form.querySelector("[role=alert]");
      if (existing) existing.remove();
      const message = document.createElement("div");
      message.className = "notice";
      message.setAttribute("role", "alert");
      message.textContent = apiErrorText(error);
      form.prepend(message);
    }
  }

  function tableMarkup(module, result) {
    const rows = Array.isArray(result?.items) ? result.items : [];
    state.tableRows[state.page] = rows;
    const page = Number(result?.page ?? 0);
    const size = Number(result?.size ?? 20);
    const total = Number(result?.totalElements ?? rows.length);
    const lastPage = Math.max(0, Number(result?.totalPages ?? Math.ceil(total / Math.max(size, 1))) - 1);
    if (!rows.length) return emptyPanel("No records found", "The admin API returned no records for the current filters.");
    return `<div class="table-wrap"><table><thead><tr>${module.columns.map(([, label]) => `<th>${escapeHtml(label)}</th>`).join("")}<th>Details</th>${keyActionsHeader(module)}</tr></thead><tbody>${rows.map((item, index) => `<tr>${module.columns.map(([key]) => `<td>${cellValue(item, key)}</td>`).join("")}<td><button class="btn btn-secondary" data-record-detail="${index}" type="button">View</button></td>${keyActionsCell(module, item, index)}</tr>`).join("")}</tbody></table></div><div class="pagination"><span>Showing ${total ? page * size + 1 : 0}–${Math.min((page + 1) * size, total)} of ${formatNumber(total)}</span><div class="pagination-controls"><button class="btn btn-secondary" data-table-page="${Math.max(0, page - 1)}" ${page <= 0 ? "disabled" : ""} type="button">Previous</button><button class="btn btn-secondary" data-table-page="${Math.min(lastPage, page + 1)}" ${page >= lastPage ? "disabled" : ""} type="button">Next</button></div></div>`;
  }

  function keyActionsHeader(module) {
    if (module.endpoint === "/admin/users" && hasPermission("users:status:update")) return "<th>Account action</th>";
    if (module.endpoint === "/admin/security/alerts" && hasPermission("security:investigate")) return "<th>Case action</th>";
    return "";
  }

  function keyActionsCell(module, item, index) {
    if (module.endpoint === "/admin/users" && hasPermission("users:status:update")) {
      return `<td><button class="btn btn-secondary" data-workflow="user-status" data-record-index="${index}" type="button">Change status</button></td>`;
    }
    if (module.endpoint === "/admin/security/alerts" && hasPermission("security:investigate")) {
      return `<td><button class="btn btn-secondary" data-workflow="security-case" data-record-index="${index}" type="button">Review case</button></td>`;
    }
    return "";
  }

  function moduleToolbar(module, values) {
    const search = `<input type="search" id="table-search" value="${escapeHtml(values.q || "")}" placeholder="Search ${escapeHtml(module.title.toLowerCase())}…" aria-label="Search ${escapeHtml(module.title)}">`;
    const status = `<select id="table-status" aria-label="Filter by status"><option value="">All statuses</option>${(values.statuses || []).map((status) => `<option value="${escapeHtml(status)}" ${values.status === status ? "selected" : ""}>${escapeHtml(humanize(status))}</option>`).join("")}</select>`;
    const specific = module.filters.filter((filter) => filter !== "status").map((filter) => `<select data-filter="${escapeHtml(filter)}" aria-label="Filter by ${escapeHtml(humanize(filter))}"><option value="">All ${escapeHtml(humanize(filter))}</option>${(values[`${filter}Options`] || []).map((option) => `<option value="${escapeHtml(option)}" ${values[filter] === option ? "selected" : ""}>${escapeHtml(humanize(option))}</option>`).join("")}</select>`).join("");
    const dateFilter = `<input type="date" id="date-from" value="${escapeHtml(values.from || "")}" aria-label="Start date"><input type="date" id="date-to" value="${escapeHtml(values.to || "")}" aria-label="End date">`;
    return `<div class="toolbar">${search}${status}${specific}${dateFilter}<button class="btn btn-secondary" data-retry="module" type="button">Apply filters</button></div>`;
  }

  async function loadModule(key, overrides = {}) {
    const module = modules[key];
    const host = document.getElementById("page-content");
    if (!module || !host) return;
    const values = { ...(state.tableFilters[key] || {}), ...overrides };
    state.tableFilters[key] = values;
    const page = state.tablePages[key] || 0;
    host.innerHTML = `${moduleHeading(key)}<section class="table-card">${moduleToolbar(module, values)}<div id="table-results">${loadingPanel()}</div></section>`;
    try {
      const params = new URLSearchParams({ page: String(page), size: "20", sort: "createdAt,desc" });
      if (values.q) params.set("q", values.q);
      if (values.status) params.set("status", values.status);
      if (values.from) params.set("from", values.from);
      if (values.to) params.set("to", values.to);
      for (const filter of module.filters) if (filter !== "status" && values[filter]) params.set(filter, values[filter]);
      const response = await api(`${module.endpoint}?${params}`);
      const result = await response.json();
      if (state.page !== key) return;
      values.statuses = values.statuses || result.filters?.statuses || [];
      for (const filter of module.filters) if (filter !== "status") values[`${filter}Options`] = values[`${filter}Options`] || result.filters?.[filter] || [];
      host.innerHTML = `${moduleHeading(key)}<section class="table-card">${moduleToolbar(module, values)}<div id="table-results">${tableMarkup(module, result)}</div></section>`;
    } catch (error) {
      if (state.page !== key) return;
      host.innerHTML = `${moduleHeading(key)}${errorPanel(error, "module")}<section class="table-card">${moduleToolbar(module, values)}${emptyPanel("Records are not available", "This page only displays data returned by the authorized admin API.")}</section>`;
    }
  }

  async function exportModule(key) {
    const module = modules[key];
    if (!module) return;
    if (!hasExportPermission(key)) return toast("Your administrator permissions do not allow this export.");
    const filter = state.tableFilters[key] || {};
    const params = new URLSearchParams({ format: "csv" });
    if (filter.q) params.set("q", filter.q);
    if (filter.status) params.set("status", filter.status);
    if (filter.from) params.set("from", filter.from);
    if (filter.to) params.set("to", filter.to);
    try {
      const response = await api(`${module.endpoint}/export?${params}`);
      const blob = await response.blob();
      const link = document.createElement("a");
      link.href = URL.createObjectURL(blob);
      link.download = `${key}-audit-${new Date().toISOString().slice(0, 10)}.csv`;
      link.click();
      URL.revokeObjectURL(link.href);
      toast("Report export downloaded.");
    } catch (error) {
      toast(`Export unavailable: ${apiErrorText(error)}`);
    }
  }

  async function loadReports() {
    const host = document.getElementById("page-content");
    const mayExport = hasPermission("reports:export");
    host.innerHTML = `${heading("reports")}<section class="panel config-panel"><h2>Export authorized reports</h2><p>Choose a report type and date range. CSV exports are generated by the backend and logged in the append-only administrator audit trail.</p>${mayExport ? "" : `<div class="notice info">Your role can view reports but does not have the reports:export permission.</div>`}<form id="report-form"><div class="field"><label for="report-type">Report</label><select id="report-type" required><option value="transactions">Transaction summary</option><option value="bill-payments">Bill payment summary</option><option value="users">User and account status</option><option value="security-alerts">Security activity</option><option value="admin-audit">Administrative activity</option><option value="ai-activity">AI agent execution summary</option></select></div><div class="field"><label for="report-from">From</label><input id="report-from" type="date" required></div><div class="field"><label for="report-to">To</label><input id="report-to" type="date" required></div><div class="field"><label for="report-format">Format</label><select id="report-format"><option value="csv">CSV</option></select></div><button class="btn btn-primary" type="submit" ${mayExport ? "" : "disabled"}>Generate report</button></form><div id="report-result" style="margin-top:14px"></div></section>`;
  }

  async function generateReport(form) {
    if (!hasPermission("reports:export")) return toast("Your administrator permissions do not allow report exports.");
    const from = form.querySelector("#report-from").value;
    const to = form.querySelector("#report-to").value;
    if (from && to && from > to) {
      toast("The start date must be before the end date.");
      return;
    }
    const params = new URLSearchParams({
      type: form.querySelector("#report-type").value,
      from,
      to,
      format: form.querySelector("#report-format").value
    });
    const result = document.getElementById("report-result");
    result.innerHTML = loadingPanel();
    try {
      const response = await api(`/admin/reports/export?${params}`);
      const blob = await response.blob();
      const link = document.createElement("a");
      link.href = URL.createObjectURL(blob);
      link.download = `bank-report-${params.get("type")}-${from}-to-${to}.${params.get("format")}`;
      link.click();
      URL.revokeObjectURL(link.href);
      result.innerHTML = `<div class="notice info">Report generated by the admin API. The server is responsible for permission checks and export audit logging.</div>`;
    } catch (error) {
      result.innerHTML = errorPanel(error);
    }
  }

  async function loadSettings() {
    const host = document.getElementById("page-content");
    const canChangeCredentials = state.admin?.role === "ADMIN";
    host.innerHTML = `${heading("settings")}
      ${canChangeCredentials ? `<section class="panel config-panel"><h2>Administrator credentials</h2><p>Signed in as <strong>${escapeHtml(state.admin?.email || "")}</strong>. Password changes require your current password.</p><form id="credentials-form">
        <div class="field"><label for="credentials-email">New username (email, optional)</label><input id="credentials-email" name="email" type="email" maxlength="254" autocomplete="username" value="${escapeHtml(state.admin?.email || "")}"></div>
        <div class="field"><label for="credentials-current-password">Current password (required to change password)</label><input id="credentials-current-password" name="currentPassword" type="password" autocomplete="current-password"></div>
        <div class="field"><label for="credentials-new-password">New password (optional)</label><input id="credentials-new-password" name="password" type="password" minlength="14" maxlength="72" autocomplete="new-password"><small>At least 14 characters; maximum 72 UTF-8 bytes.</small></div>
        <div class="field"><label for="credentials-confirm-password">Confirm new password</label><input id="credentials-confirm-password" name="confirmPassword" type="password" minlength="14" maxlength="72" autocomplete="new-password"></div>
        <button class="btn btn-primary" type="submit">Update credentials</button></form><div id="credentials-result"></div></section>` : ""}
      <section class="panel config-panel"><h2>Spring Boot core API connection</h2><p>The administrator API is served by the existing Spring Boot core API. Its default URL is <code>http://localhost:8081/api/v1</code>. Banking records come from the core database; administrator audit events and security case notes use the separate <code>admin_console</code> schema.</p><form id="settings-form"><div class="field"><label for="settings-api-base">Core API base URL</label><input id="settings-api-base" type="url" value="${escapeHtml(state.apiBase)}" required></div><button class="btn btn-primary" type="submit">Save connection</button><button class="btn btn-secondary" id="settings-test" type="button" style="margin-left:7px">Test connection</button></form><div id="settings-result"></div><div class="section-title" style="margin-top:28px"><h2>Data coverage</h2></div><div class="notice info">Users and transactions come from the banking database. Bill payments are the ledger entries marked BILL_PAYMENT. Security signals are policy DENY/STEP_UP audit events, not fraud conclusions. AI activity is based on the existing redacted assistant audit log. The banking schema does not contain a separate payment processor, risk-alert, or AI-trace store.</div></section>`;
  }

  async function submitFirstLoginCredentials(event) {
    event.preventDefault();
    const form = event.currentTarget;
    const data = Object.fromEntries(new FormData(form).entries());
    const button = form.querySelector('button[type="submit"]');
    button.disabled = true;
    button.textContent = "Saving credentials…";
    try {
      const response = await api("/admin/auth/first-login-credentials", { method: "POST", body: JSON.stringify(data) });
      const result = await response.json();
      state.token = result.accessToken;
      state.admin = result.administrator;
      sessionStorage.setItem(STORAGE_KEYS.token, state.token);
      sessionStorage.setItem(STORAGE_KEYS.admin, JSON.stringify(state.admin));
      state.page = "dashboard";
      render();
    } catch (error) {
      root.innerHTML = firstLoginCredentialsMarkup(apiErrorText(error));
      attachEvents();
    }
  }

  async function submitCredentialUpdate(event) {
    event.preventDefault();
    const form = event.currentTarget;
    const data = Object.fromEntries(new FormData(form).entries());
    const button = form.querySelector('button[type="submit"]');
    button.disabled = true;
    button.textContent = "Updating…";
    try {
      const response = await api("/admin/auth/credentials", { method: "PATCH", body: JSON.stringify(data) });
      const result = await response.json();
      state.token = result.accessToken;
      state.admin = result.administrator;
      sessionStorage.setItem(STORAGE_KEYS.token, state.token);
      sessionStorage.setItem(STORAGE_KEYS.admin, JSON.stringify(state.admin));
      toast("Administrator credentials updated. Other sessions were signed out.");
      await loadSettings();
    } catch (error) {
      const host = document.getElementById("credentials-result");
      if (host) host.innerHTML = `<div class="notice" role="alert">${escapeHtml(apiErrorText(error))}</div>`;
      button.disabled = false;
      button.textContent = "Retry";
    }
  }

  async function testConnection() {
    const host = document.getElementById("settings-result");
    host.innerHTML = loadingPanel();
    try {
      const response = await api("/admin/health");
      const result = await response.json();
      host.innerHTML = `<div class="notice info">Admin API responded${result?.status ? `: ${escapeHtml(result.status)}` : "."}</div>`;
    } catch (error) {
      host.innerHTML = `<div style="margin-top:14px">${errorPanel(error)}</div>`;
    }
  }

  async function renderPage() {
    if (state.page === "dashboard") return loadDashboard();
    if (state.page === "settings") return loadSettings();
    if (state.page === "reports") return loadReports();
    if (modules[state.page]) return loadModule(state.page);
    const host = document.getElementById("page-content");
    host.innerHTML = `${heading()}${emptyPanel("Page is not available", "This console does not have a configured view for this section.")}`;
  }

  function toast(message) {
    document.querySelector(".toast")?.remove();
    const element = document.createElement("div");
    element.className = "toast";
    element.setAttribute("role", "status");
    element.textContent = message;
    document.body.append(element);
    window.setTimeout(() => element.remove(), 4500);
  }

  async function signIn(form) {
    const data = new FormData(form);
    const button = form.querySelector('button[type="submit"]');
    button.disabled = true;
    button.textContent = "Signing in…";
    try {
      const response = await api("/admin/auth/login", {
        method: "POST",
        body: JSON.stringify({ email: data.get("email"), password: data.get("password") })
      });
      const result = await response.json();
      if (!result.accessToken) throw new Error("The admin API did not return an administrator access token.");
      state.token = result.accessToken;
      state.admin = result.administrator || result.admin || null;
      if (!state.admin && Array.isArray(result.permissions)) {
        state.admin = { fullName: result.fullName, role: result.role, permissions: result.permissions, capabilities: result.capabilities };
      }
      sessionStorage.setItem(STORAGE_KEYS.token, state.token);
      sessionStorage.setItem(STORAGE_KEYS.admin, JSON.stringify(state.admin));
      state.page = hasAccess("dashboard") ? "dashboard" : "settings";
      render();
    } catch (error) {
      root.innerHTML = loginMarkup(apiErrorText(error));
      attachEvents();
    }
  }

  async function signOut() {
    try {
      if (state.token) await api("/admin/auth/logout", { method: "POST" });
    } catch (error) {
      if (error.status !== 404 && error.status !== 401) toast(`Sign-out was local; server notification failed: ${apiErrorText(error)}`);
    } finally {
      sessionStorage.removeItem(STORAGE_KEYS.token);
      sessionStorage.removeItem(STORAGE_KEYS.admin);
      state.token = null;
      state.admin = null;
      state.page = "dashboard";
      render();
    }
  }

  function navigate(page) {
    if (!pages[page]) return;
    if (!hasAccess(page)) return toast("Your administrator permissions do not include this section.");
    state.page = page;
    state.sidebarOpen = false;
    render();
  }

  function attachEvents() {
    document.getElementById("login-form")?.addEventListener("submit", (event) => {
      event.preventDefault();
      signIn(event.currentTarget);
    });
    document.getElementById("first-login-form")?.addEventListener("submit", submitFirstLoginCredentials);
    document.getElementById("first-login-logout")?.addEventListener("click", signOut);
    document.getElementById("logout")?.addEventListener("click", signOut);
    document.getElementById("menu-toggle")?.addEventListener("click", () => {
      state.sidebarOpen = !state.sidebarOpen;
      document.getElementById("sidebar")?.classList.toggle("open", state.sidebarOpen);
    });
    document.querySelectorAll("[data-page]").forEach((button) => button.addEventListener("click", () => navigate(button.dataset.page)));
    document.querySelector("[data-close-dialog]")?.addEventListener("click", () => document.getElementById("record-dialog")?.close());
    document.querySelector("[data-close-workflow]")?.addEventListener("click", () => document.getElementById("workflow-dialog")?.close());
    const host = document.getElementById("page-content");
    if (!host || host.dataset.eventsAttached) return;
    host.dataset.eventsAttached = "true";
    host.addEventListener("click", (event) => {
      const target = event.target instanceof Element ? event.target : null;
      if (!target) return;
      const pageButton = target.closest("[data-page]");
      if (pageButton) return navigate(pageButton.dataset.page);
      const retryButton = target.closest("[data-retry]");
      if (retryButton) {
        if (retryButton.dataset.retry === "dashboard") loadDashboard();
        else if (retryButton.dataset.retry === "module") collectModuleFilters();
        return;
      }
      const exportButton = target.closest("[data-export]");
      if (exportButton) return exportModule(exportButton.dataset.export);
      const workflowButton = target.closest("[data-workflow]");
      if (workflowButton) return openWorkflow(workflowButton.dataset.workflow, Number(workflowButton.dataset.recordIndex));
      const detailButton = target.closest("[data-record-detail]");
      if (detailButton) return openRecordDetail(state.page, Number(detailButton.dataset.recordDetail));
      const paginationButton = target.closest("[data-table-page]");
      if (paginationButton) {
        const nextPage = Number(paginationButton.dataset.tablePage);
        if (nextPage === (state.tablePages[state.page] || 0)) return;
        state.tablePages[state.page] = nextPage;
        loadModule(state.page);
        return;
      }
      if (target.closest("#settings-test")) testConnection();
    });
    host.addEventListener("keydown", (event) => {
      if (event.target instanceof Element && event.target.id === "table-search" && event.key === "Enter") {
        event.preventDefault();
        collectModuleFilters();
      }
    });
    host.addEventListener("change", (event) => {
      if (!(event.target instanceof Element)) return;
      if (event.target.id === "dashboard-days") return loadDashboard(event.target.value);
      if (event.target.id === "table-status" || event.target.matches("[data-filter]")) return collectModuleFilters();
      if (event.target.id === "date-from" || event.target.id === "date-to") {
        const from = document.getElementById("date-from")?.value;
        const to = document.getElementById("date-to")?.value;
        if (from && to && from > to) {
          toast("The start date must be before the end date.");
          return;
        }
        collectModuleFilters();
      }
    });
    host.addEventListener("submit", (event) => {
      if (!(event.target instanceof HTMLFormElement)) return;
      if (event.target.id === "credentials-form") {
        submitCredentialUpdate(event);
      } else if (event.target.id === "report-form") {
        event.preventDefault();
        generateReport(event.target);
      } else if (event.target.id === "settings-form") {
        event.preventDefault();
        persistApiBase(document.getElementById("settings-api-base").value);
        document.getElementById("settings-result").innerHTML = `<div class="notice info">Admin API URL saved for this browser.</div>`;
      }
    });
  }

  function collectModuleFilters() {
    const module = modules[state.page];
    if (!module) return;
    const filters = { ...(state.tableFilters[state.page] || {}) };
    filters.q = document.getElementById("table-search")?.value.trim() || "";
    filters.status = document.getElementById("table-status")?.value || "";
    filters.from = document.getElementById("date-from")?.value || "";
    filters.to = document.getElementById("date-to")?.value || "";
    document.querySelectorAll("[data-filter]").forEach((select) => { filters[select.dataset.filter] = select.value; });
    if (filters.from && filters.to && filters.from > filters.to) return toast("The start date must be before the end date.");
    state.tableFilters[state.page] = filters;
    state.tablePages[state.page] = 0;
    loadModule(state.page, filters);
  }

  function render() {
    if (!state.token) {
      root.innerHTML = loginMarkup();
      attachEvents();
      return;
    }
    if (state.admin?.mustChangeCredentials) {
      root.innerHTML = firstLoginCredentialsMarkup();
      attachEvents();
      return;
    }
    if (!hasAccess(state.page)) state.page = hasAccess("dashboard") ? "dashboard" : "settings";
    root.innerHTML = shellMarkup();
    attachEvents();
    renderPage();
  }

render();
