import { createApiClient, apiErrorText } from "./src/api.js";
import { DEFAULT_API_BASE, MODULES, PAGES, STORAGE_KEYS } from "./src/config.js";
import { escapeHtml, formatDate, formatMoney, formatNumber, humanize, safeParse, statusTag } from "./src/formatters.js";

// ── State ─────────────────────────────────────────────────────────────────────

const root = document.getElementById("app");
let savedApiBase = localStorage.getItem(STORAGE_KEYS.apiBase);
if (savedApiBase && (savedApiBase.includes(":8080") || !savedApiBase.includes(":8081"))) {
  savedApiBase = DEFAULT_API_BASE;
  try { localStorage.setItem(STORAGE_KEYS.apiBase, DEFAULT_API_BASE); } catch {}
}
const state = {
  apiBase: (savedApiBase || DEFAULT_API_BASE).replace(/\/+$/, ""),
  token:   sessionStorage.getItem(STORAGE_KEYS.token),
  admin:   safeParse(sessionStorage.getItem(STORAGE_KEYS.admin)) || null,
  page:    "dashboard",
  sidebarOpen: false,
  tablePages:  {},
  tableFilters: {},
  tableRows:   {}
};

const api = createApiClient({
  getApiBase: () => state.apiBase,
  getToken:   () => state.token
});

// ── Permissions ───────────────────────────────────────────────────────────────

function hasPermission(perm) {
  const p = state.admin?.permissions;
  return Array.isArray(p) && (p.includes("*") || p.includes(perm));
}

function hasAccess(page) {
  if (!state.admin) return false;
  if (page === "settings") return true;
  const p = state.admin?.permissions;
  if (!Array.isArray(p)) return false;
  return p.includes("*") || p.includes(PAGES[page]?.permission);
}

// ── Markup helpers ─────────────────────────────────────────────────────────────

function loginMarkup(error = "") {
  return `<main class="login-page">
    <section class="login-aside" aria-label="Bank operations">
      <div class="brand"><span class="brand-mark">A</span><span>Agentic AI Bank <span style="font-weight:500;color:#a9bdd6">/ Admin</span></span></div>
      <div class="login-aside-copy">
        <p class="eyebrow">Banking operations console</p>
        <h1>Secure admin workspace.</h1>
        <p>Manage customer accounts, review transactions, and monitor AI agent activity.</p>
      </div>
      <div class="secure-note"><span aria-hidden="true">●</span> Protected administrator access · All actions are audited</div>
    </section>
    <section class="login-main">
      <form class="login-card" id="login-form">
        <h2>Welcome back</h2>
        <p>Sign in with your administrator credentials.</p>
        ${error ? `<div class="notice" role="alert">${escapeHtml(error)}</div>` : ""}
        <div class="field"><label for="login-email">Email or Username</label><input id="login-email" name="email" type="text" autocomplete="username" required placeholder="admin@bank.com" value="admin@bank.com"></div>
        <div class="field"><label for="login-password">Password</label><input id="login-password" name="password" type="password" autocomplete="current-password" required placeholder="Admin@1234" value="Admin@1234"></div>
        <button class="btn btn-primary btn-full" type="submit">Sign in securely</button>
        <p style="margin-top:16px;font-size:12px;color:#8899aa;text-align:center">
          Default: <code>admin@bank.com</code> / <code>Admin@1234</code>
        </p>
        <p style="margin-top:8px;font-size:11px;color:#5a6e85;text-align:center">
          API Target: <code>${escapeHtml(state.apiBase)}</code>
        </p>
      </form>
    </section>
  </main>`;
}

function shellMarkup() {
  const sections = ["OVERVIEW", "OPERATIONS", "RISK & CONTROLS", "ADMINISTRATION"];
  const nav = sections.map((section) => {
    const links = Object.entries(PAGES).filter(([key, item]) => item.section === section && hasAccess(key));
    if (!links.length) return "";
    return `<div class="nav-label">${section}</div><nav class="nav-list" aria-label="${section}">${
      links.map(([key, item]) =>
        `<button class="nav-link ${state.page === key ? "active" : ""}" data-page="${key}" type="button"><span class="nav-glyph">${item.glyph}</span>${item.label}</button>`
      ).join("")
    }</nav>`;
  }).join("");

  const displayName = state.admin?.fullName || state.admin?.name || "Administrator";
  const role = state.admin?.role || "Admin";
  const initial = displayName.trim().slice(0, 1).toUpperCase() || "A";

  return `<div class="shell">
    <aside class="sidebar ${state.sidebarOpen ? "open" : ""}" id="sidebar">
      <div class="brand"><span class="brand-mark">A</span><span>Agentic AI Bank <span style="font-weight:500;color:#a9bdd6">/ Admin</span></span></div>
      ${nav}
      <div class="sidebar-bottom"><div class="admin-mini"><span class="avatar">${escapeHtml(initial)}</span><div><strong>${escapeHtml(displayName)}</strong><span>${escapeHtml(role)}</span></div></div></div>
    </aside>
    <div class="workspace">
      <header class="topbar">
        <button class="icon-button menu-toggle" id="menu-toggle" type="button" aria-label="Toggle navigation">☰</button>
        <div class="breadcrumb">Agentic AI Bank <strong>/ ${escapeHtml(PAGES[state.page]?.label || state.page)}</strong></div>
        <div class="top-actions"><span class="env-chip"><span class="env-dot"></span>Live DB</span><span class="env-chip"><span class="env-dot" id="ws-status-dot" style="background:#00e5a3"></span><span id="ws-status-text">Live WS</span></span><button class="icon-button" id="logout" type="button" title="Sign out" aria-label="Sign out">↪</button></div>
      </header>
      <main class="main-content" id="page-content"><div class="loading">Loading secure workspace</div></main>
    </div>
    <dialog class="record-dialog" id="record-dialog" aria-labelledby="record-dialog-title">
      <div class="record-dialog-head"><div><p class="eyebrow">Record details</p><h2 id="record-dialog-title">Details</h2></div><button class="icon-button" data-close-dialog type="button" aria-label="Close">×</button></div>
      <div class="record-dialog-body" id="record-dialog-body"></div>
    </dialog>
    <dialog class="record-dialog workflow-dialog" id="workflow-dialog" aria-labelledby="workflow-title">
      <div class="record-dialog-head"><div><p class="eyebrow">Admin action</p><h2 id="workflow-title">Action</h2></div><button class="icon-button" data-close-workflow type="button" aria-label="Close">×</button></div>
      <div class="record-dialog-body" id="workflow-body"></div>
    </dialog>
  </div>`;
}

function heading(page = state.page, actions = "") {
  const meta = PAGES[page] || { label: page, description: "" };
  return `<div class="page-heading"><div><h1>${escapeHtml(meta.label)}</h1><p>${escapeHtml(meta.description)}</p></div>${actions ? `<div class="heading-actions">${actions}</div>` : ""}</div>`;
}

function errorPanel(error, retryAction = "") {
  return `<div class="error-banner" role="alert"><div><strong>Live data unavailable · </strong>${escapeHtml(apiErrorText(error))}</div>${retryAction ? `<button class="retry" data-retry="${retryAction}" type="button">Retry</button>` : ""}</div>`;
}

function loadingPanel() { return `<div class="loading">Loading live data…</div>`; }

function emptyPanel(title, detail) {
  return `<div class="empty-state"><span class="empty-icon">—</span><h3>${escapeHtml(title)}</h3><p>${escapeHtml(detail)}</p></div>`;
}

function card(label, value, sub, glyph) {
  return `<article class="stat-card"><div class="stat-top"><span class="stat-label">${escapeHtml(label)}</span><span class="stat-icon">${escapeHtml(glyph)}</span></div><div class="stat-value">${value}</div><div class="stat-sub">${escapeHtml(sub)}</div></article>`;
}

// ── Dashboard ──────────────────────────────────────────────────────────────────

async function loadDashboard() {
  const host = document.getElementById("page-content");
  if (!host) return;
  const createBtn = hasPermission("users:read")
    ? `<div style="display:flex;gap:8px">
         <button class="btn btn-secondary" data-workflow="deposit-funds" type="button" style="background:#059669;color:#fff;border-color:#10b981">+ Deposit / Cash In</button>
         <button class="btn btn-secondary" data-workflow="create-card" type="button">+ Issue Card</button>
         <button class="btn btn-primary" data-workflow="create-customer" type="button">+ New Account</button>
       </div>` : "";
  host.innerHTML = `${heading("dashboard", createBtn)}${loadingPanel()}`;
  try {
    const resp = await api("/admin/dashboard");
    const d    = await resp.json();
    if (state.page !== "dashboard") return;

    const recentRows = (d.recentTransactions || []).slice(0, 8).map(t => `
      <tr>
        <td><code>${escapeHtml(t.txn_ref || "—")}</code></td>
        <td>${escapeHtml(t.action_type || "—")}</td>
        <td>${escapeHtml(t.customer_name || "—")}</td>
        <td style="text-align:right">${formatMoney(t.amount, t.currency || "LKR")}</td>
        <td>${statusTag(t.status || "")}</td>
        <td>${formatDate(t.created_at)}</td>
      </tr>`).join("");

    host.innerHTML = `${heading("dashboard", createBtn)}
      <div class="stat-grid">
        ${card("Total Customers",    formatNumber(d.totalCustomers),    "Active bank accounts", "US")}
        ${card("Total Accounts",     formatNumber(d.totalAccounts),     "SAVINGS, CURRENT, FD",  "AC")}
        ${card("Registered on App",  formatNumber(d.registeredUsers),   "Users linked on mobile", "📱")}
        ${card("Total Deposits",     formatMoney(d.totalDeposits, "LKR"), "Across all accounts",  "LK")}
      </div>
      <div class="section-title"><h2>Recent Transactions</h2><span>Latest 8 executed transactions</span></div>
      <section class="panel">
        ${recentRows ? `<table class="data-table">
          <thead><tr><th>Reference</th><th>Type</th><th>Customer</th><th>Amount</th><th>Status</th><th>Date</th></tr></thead>
          <tbody>${recentRows}</tbody>
        </table>` : emptyPanel("No transactions yet", "Transactions will appear once customers start using the app.")}
      </section>`;
  } catch (error) {
    if (state.page === "dashboard") {
      host.innerHTML = `${heading("dashboard", "")}${errorPanel(error, "dashboard")}`;
    }
  }
}

// ── Table module (customers / transactions / audit) ────────────────────────────

function cellValue(item, key) {
  const value = item?.[key];
  if (value === null || value === undefined || value === "") return "—";
  if (key === "status" || key === "outcome") return statusTag(String(value));
  if (key === "is_registered") return value ? `<span class="badge badge-success">Yes</span>` : `<span class="badge">No</span>`;
  if (key.toLowerCase().endsWith("_at") || key.toLowerCase().endsWith("date")) return formatDate(value);
  if (key === "amount") return formatMoney(value, item.currency || "LKR");
  if (Array.isArray(value)) return escapeHtml(value.join(", ") || "—");
  return escapeHtml(String(value));
}

async function loadModule(key) {
  const module = MODULES[key];
  const host = document.getElementById("page-content");
  if (!host || !module) return;

  const page    = state.tablePages[key]  || 0;
  const createBtn = key === "users" && hasPermission("users:read")
    ? `<div style="display:flex;gap:8px">
         <button class="btn btn-secondary" data-workflow="deposit-funds" type="button" style="background:#059669;color:#fff;border-color:#10b981">+ Deposit / Cash In</button>
         <button class="btn btn-secondary" data-workflow="create-card" type="button">+ Issue Card</button>
         <button class="btn btn-primary" data-workflow="create-customer" type="button">+ New Account</button>
       </div>` : "";

  host.innerHTML = `${heading(key, createBtn)}${loadingPanel()}`;
  try {
    const qs = new URLSearchParams({ page: String(page), size: "20" });
    const resp = await api(`${module.endpoint}?${qs}`);
    const data = await resp.json();
    if (state.page !== key) return;

    // The server returns different root keys for different endpoints
    const rows = data.customers || data.transactions || data.auditLogs || data.items || [];
    state.tableRows[key] = rows;

    if (!rows.length) {
      host.innerHTML = `${heading(key, createBtn)}${emptyPanel("No records", "No data found in the database.")}`;
      return;
    }

    const thead = `<tr>${module.columns.map(([, label]) => `<th>${escapeHtml(label)}</th>`).join("")}<th></th></tr>`;
    const tbody = rows.map((item, idx) => `<tr data-row="${idx}">
      ${module.columns.map(([col]) => `<td>${cellValue(item, col)}</td>`).join("")}
      <td style="white-space:nowrap">
        <button class="btn btn-secondary btn-sm" data-detail="${key}" data-idx="${idx}">View</button>
        ${key === "users" ? `
          <button class="btn btn-secondary btn-sm" style="margin-left:6px;background:#059669;color:#fff;border-color:#10b981" data-deposit-idx="${idx}">+ Deposit</button>
          <button class="btn btn-secondary btn-sm" style="margin-left:6px" data-issue-card-idx="${idx}">+ Card</button>
        ` : ""}
      </td>
    </tr>`).join("");

    host.innerHTML = `${heading(key, createBtn)}
      <section class="panel" style="overflow-x:auto">
        <table class="data-table"><thead>${thead}</thead><tbody>${tbody}</tbody></table>
      </section>`;
  } catch (error) {
    if (state.page === key) host.innerHTML = `${heading(key, createBtn)}${errorPanel(error, key)}`;
  }
}

// ── Customer Detail Dialog ─────────────────────────────────────────────────────

async function openCustomerDetail(idx) {
  const record = state.tableRows["users"]?.[idx];
  if (!record) return;
  state.lastViewedCustomerIdx = idx;
  const dialog = document.getElementById("record-dialog");
  const body   = document.getElementById("record-dialog-body");
  document.getElementById("record-dialog-title").textContent = record.full_name || "Customer";
  dialog.showModal();
  body.innerHTML = loadingPanel();
  try {
    const resp = await api(`/admin/customers/${record.id}`);
    const d    = await resp.json();
    const accounts = (d.accounts || []).map(a => `
      <div style="background:#1a2035;border-radius:8px;padding:12px;margin-bottom:8px">
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span><strong>${escapeHtml(a.account_no || "")}</strong> · ${escapeHtml(a.product_type || "")}</span>
          <strong style="color:#4ade80">${formatMoney(a.balance, a.currency || "LKR")}</strong>
        </div>
        <div style="display:flex;justify-content:space-between;align-items:center;margin-top:6px">
          <div style="color:#8899aa;font-size:12px">${escapeHtml(a.nickname || "")} · ${statusTag(a.status)}</div>
          <div style="display:flex;gap:6px">
            <button class="btn btn-secondary btn-sm" style="background:#059669;color:#fff;border-color:#10b981" data-deposit-for-acc="${escapeHtml(a.account_no)}" data-cust-id="${escapeHtml(d.id)}" data-cust-name="${escapeHtml(d.full_name || '')}" type="button">+ Deposit Money</button>
            <button class="btn btn-secondary btn-sm" data-issue-card-for-acc="${escapeHtml(a.account_no)}" data-cust-id="${escapeHtml(d.id)}" data-cust-name="${escapeHtml(d.full_name || '')}" type="button">+ Issue Card</button>
          </div>
        </div>
      </div>`).join("") || "<p style='color:#8899aa'>No accounts</p>";

    const cards = (d.cards || []).map(c => `
      <div style="background:#1a2035;border-radius:8px;padding:12px;margin-bottom:8px;border-left:4px solid ${c.card_type === 'CREDIT' ? '#ec4899' : '#3b82f6'}">
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>
            <strong style="color:${c.card_type === 'CREDIT' ? '#f472b6' : '#60a5fa'}">${escapeHtml(c.network || "VISA")} ${escapeHtml(c.card_type || "")}</strong>
            <code style="margin-left:8px;background:rgba(255,255,255,0.06);padding:2px 6px;border-radius:4px">•••• ${escapeHtml(c.card_last4 || "")}</code>
          </span>
          <span>${statusTag(c.status)}</span>
        </div>
        <div style="color:#8899aa;font-size:12px;margin-top:6px;display:flex;justify-content:space-between;align-items:center">
          <span>${escapeHtml(c.nickname || "")}</span>
          ${c.credit_limit ? `<span>Credit Limit: <strong style="color:#4ade80">${formatMoney(c.credit_limit, "LKR")}</strong></span>` : `<span style="color:#60a5fa">Debit (Linked to Account)</span>`}
        </div>
      </div>`).join("") || `<p style='color:#8899aa;font-size:13px'>No cards issued yet. Click "+ Issue Card" to create one.</p>`;

    const userCred = d.userCredentials
      ? `<div style="background:#1a2035;border-radius:8px;padding:12px;margin-bottom:8px">
          <div><strong>App Username:</strong> ${escapeHtml(d.userCredentials.username || "")}</div>
          <div style="color:#8899aa;font-size:12px">Registered: ${formatDate(d.userCredentials.created_at)} · Last login: ${formatDate(d.userCredentials.last_login_at)}</div>
        </div>`
      : `<p style="color:#8899aa">User has not registered on the mobile app yet.</p>`;

    body.innerHTML = `
      <dl class="detail-grid">
        <div class="detail-item"><dt>CIF</dt><dd>${escapeHtml(d.cif || "")}</dd></div>
        <div class="detail-item"><dt>Full Name</dt><dd>${escapeHtml(d.full_name || "")}</dd></div>
        <div class="detail-item"><dt>Mobile</dt><dd>${escapeHtml(d.mobile || "")}</dd></div>
        <div class="detail-item"><dt>Email</dt><dd>${escapeHtml(d.email || "—")}</dd></div>
        <div class="detail-item"><dt>Status</dt><dd>${statusTag(d.status)}</dd></div>
        <div class="detail-item"><dt>Preferred Language</dt><dd>${escapeHtml(d.preferred_language || "")}</dd></div>
      </dl>
      <div style="display:flex;justify-content:space-between;align-items:center;margin:18px 0 8px">
        <h3 style="margin:0">Bank Accounts</h3>
      </div>
      ${accounts}
      <div style="display:flex;justify-content:space-between;align-items:center;margin:18px 0 8px">
        <h3 style="margin:0">Issued Cards (${(d.cards || []).length})</h3>
        <button class="btn btn-primary btn-sm" data-issue-card-cust="${escapeHtml(d.id)}" data-cust-name="${escapeHtml(d.full_name || '')}" type="button">+ Issue Card</button>
      </div>
      ${cards}
      <h3 style="margin:18px 0 8px">Mobile App Access</h3>${userCred}`;
  } catch (e) {
    body.innerHTML = errorPanel(e);
  }
}

// ── Create Customer Workflow ────────────────────────────────────────────────────

function openCreateCustomerWorkflow() {
  const dialog = document.getElementById("workflow-dialog");
  const title  = document.getElementById("workflow-title");
  const body   = document.getElementById("workflow-body");
  title.textContent = "Create New Customer Account";
  body.innerHTML = `
    <form id="workflow-form" data-action="create-customer">
      <div class="field">
        <label for="wf-name">Full Name *</label>
        <input id="wf-name" name="fullName" type="text" required placeholder="e.g. Kamal Perera">
      </div>
      <div class="field">
        <label for="wf-mobile">Mobile Number *</label>
        <input id="wf-mobile" name="mobile" type="tel" required placeholder="e.g. 0771234567">
      </div>
      <div class="field">
        <label for="wf-email">Email (optional)</label>
        <input id="wf-email" name="email" type="email" placeholder="e.g. kamal@example.com">
      </div>
      <div class="field">
        <label for="wf-lang">Preferred Language</label>
        <select id="wf-lang" name="preferredLanguage">
          <option value="EN">English</option>
          <option value="SI">Sinhala (සිංහල)</option>
          <option value="TA">Tamil (தமிழ்)</option>
        </select>
      </div>
      <div class="field">
        <label for="wf-type">Account Type *</label>
        <select id="wf-type" name="accountType" required>
          <option value="SAVINGS">Savings Account</option>
          <option value="CURRENT">Current Account</option>
          <option value="FIXED_DEPOSIT">Fixed Deposit</option>
        </select>
      </div>
      <div class="field">
        <label for="wf-balance">Initial Balance (LKR) *</label>
        <input id="wf-balance" name="initialBalance" type="number" required min="0" step="0.01" placeholder="e.g. 25000.00">
        <small>This is the real opening balance. Enter 0 for empty account.</small>
      </div>
      <div class="field">
        <label for="wf-accno">Account Number (leave blank to auto-generate)</label>
        <input id="wf-accno" name="accountNo" type="text" placeholder="e.g. ACC123456789">
        <small>If blank, a unique account number will be generated automatically.</small>
      </div>
      <div id="wf-result"></div>
      <div style="display:flex;gap:12px;margin-top:20px">
        <button class="btn btn-primary" type="submit">Create Account</button>
        <button class="btn btn-secondary" data-close-workflow type="button">Cancel</button>
      </div>
    </form>`;
  dialog.showModal();
}

async function submitCreateCustomer(form) {
  const resultEl = document.getElementById("wf-result");
  resultEl.innerHTML = `<div class="loading" style="margin-top:12px">Creating account…</div>`;
  const fd = new FormData(form);
  const payload = {
    fullName:          fd.get("fullName"),
    mobile:            fd.get("mobile"),
    email:             fd.get("email") || null,
    preferredLanguage: fd.get("preferredLanguage"),
    accountType:       fd.get("accountType"),
    initialBalance:    parseFloat(fd.get("initialBalance") || "0"),
    accountNo:         fd.get("accountNo") || null
  };

  try {
    const resp = await api("/admin/customers", {
      method: "POST",
      body: JSON.stringify(payload)
    });
    const d = await resp.json();
    resultEl.innerHTML = `
      <div class="notice success" style="background:rgba(74,222,128,.1);border:1px solid rgba(74,222,128,.3);border-radius:8px;padding:16px;margin-top:12px">
        <strong style="color:#4ade80">✓ Account Created Successfully!</strong>
        <div style="margin-top:10px;display:grid;grid-template-columns:1fr 1fr;gap:8px;font-size:13px">
          <div><span style="color:#8899aa">Customer Name:</span><br><strong>${escapeHtml(d.fullName)}</strong></div>
          <div><span style="color:#8899aa">CIF:</span><br><strong>${escapeHtml(d.cif)}</strong></div>
          <div><span style="color:#8899aa">Account Number:</span><br><strong style="font-size:15px;color:#60a5fa">${escapeHtml(d.accountNo)}</strong></div>
          <div><span style="color:#8899aa">Account Type:</span><br><strong>${escapeHtml(d.accountType)}</strong></div>
          <div><span style="color:#8899aa">Initial Balance:</span><br><strong style="color:#4ade80">${formatMoney(d.initialBalance, "LKR")}</strong></div>
          <div><span style="color:#8899aa">Currency:</span><br><strong>${escapeHtml(d.currency)}</strong></div>
        </div>
        <div style="margin-top:12px;padding:10px;background:rgba(96,165,250,.1);border-radius:6px;font-size:12px;color:#8899aa">
          📋 <strong>Give the customer their account number: <span style="color:#60a5fa">${escapeHtml(d.accountNo)}</span></strong><br>
          They will use it to register on the mobile app.
        </div>
      </div>`;
    // Refresh customer list if currently on users page
    if (state.page === "users") setTimeout(() => loadModule("users"), 1000);
  } catch (error) {
    resultEl.innerHTML = `<div class="notice" role="alert" style="margin-top:12px">${escapeHtml(apiErrorText(error))}</div>`;
  }
}

// ── Create Card Workflow ────────────────────────────────────────────────────────

async function openCreateCardWorkflow(opts = {}) {
  const dialog = document.getElementById("workflow-dialog");
  const title  = document.getElementById("workflow-title");
  const body   = document.getElementById("workflow-body");
  title.textContent = "Issue Debit or Credit Card";

  let customerId = opts.customerId || "";
  let customerName = opts.customerName || "";
  let targetAccountNo = opts.accountNo || "";
  let accounts = opts.accounts || [];

  // If customerId is provided but accounts not loaded, fetch them
  if (customerId && (!accounts || !accounts.length)) {
    try {
      const resp = await api(`/admin/customers/${customerId}`);
      const data = await resp.json();
      accounts = data.accounts || [];
      if (!customerName) customerName = data.full_name;
    } catch {}
  }

  // If opened without a specific customer or account, list existing customers
  let customerSelectOptions = "";
  if (!customerId && !targetAccountNo) {
    const cachedUsers = state.tableRows["users"] || [];
    if (cachedUsers.length > 0) {
      customerSelectOptions = `<div class="field">
        <label for="wf-card-cust-select">Select Customer (optional)</label>
        <select id="wf-card-cust-select">
          <option value="">-- Choose existing customer --</option>
          ${cachedUsers.map(u => `<option value="${escapeHtml(u.id)}">${escapeHtml(u.full_name)} (${escapeHtml(u.cif)})</option>`).join("")}
        </select>
      </div>`;
    }
  }

  let accountInputHtml = "";
  if (accounts.length > 0) {
    accountInputHtml = `
      <div class="field">
        <label for="wf-card-acc">Linked Bank Account *</label>
        <select id="wf-card-acc" name="accountNo" required>
          ${accounts.map(a => `
            <option value="${escapeHtml(a.account_no)}" ${a.account_no === targetAccountNo ? "selected" : ""}>
              ${escapeHtml(a.account_no)} · ${escapeHtml(a.product_type)} (${formatMoney(a.balance, a.currency || "LKR")})
            </option>`).join("")}
        </select>
      </div>`;
  } else {
    accountInputHtml = `
      <div class="field">
        <label for="wf-card-accno">Account Number *</label>
        <input id="wf-card-accno" name="accountNo" type="text" required placeholder="e.g. ACC1729758291" value="${escapeHtml(targetAccountNo)}">
        <small>Enter the customer's existing account number to link this card.</small>
      </div>`;
  }

  body.innerHTML = `
    <form id="workflow-form" data-action="create-card">
      <input type="hidden" name="customerId" value="${escapeHtml(customerId)}">
      ${customerName ? `
        <div style="background:rgba(96,165,250,0.1);border:1px solid rgba(96,165,250,0.3);border-radius:8px;padding:10px 14px;margin-bottom:14px;font-size:13px">
          Customer: <strong style="color:#60a5fa">${escapeHtml(customerName)}</strong>
        </div>` : ""}
      ${customerSelectOptions}
      <div id="wf-card-acc-container">
        ${accountInputHtml}
      </div>
      <div class="field">
        <label for="wf-card-type">Card Type *</label>
        <select id="wf-card-type" name="cardType" required>
          <option value="DEBIT" selected>Debit Card</option>
          <option value="CREDIT">Credit Card</option>
        </select>
      </div>
      <div class="field">
        <label for="wf-card-network">Card Network *</label>
        <select id="wf-card-network" name="network" required>
          <option value="VISA" selected>Visa</option>
          <option value="MASTERCARD">Mastercard</option>
        </select>
      </div>
      <div class="field">
        <label for="wf-card-nickname">Card Nickname (optional)</label>
        <input id="wf-card-nickname" name="nickname" type="text" placeholder="e.g. Visa Platinum Debit Card">
      </div>
      <div class="field" id="wf-card-limit-field" style="display:none">
        <label for="wf-card-limit">Credit Limit (LKR) *</label>
        <input id="wf-card-limit" name="creditLimit" type="number" min="1000" step="1000" value="100000.00" placeholder="100000.00">
        <small>Approved credit limit for this credit card.</small>
      </div>
      <div class="field">
        <label for="wf-card-last4">Card Last 4 Digits (optional)</label>
        <input id="wf-card-last4" name="cardLast4" type="text" maxlength="4" pattern="\\d{4}" placeholder="Leave blank to auto-generate">
        <small>4 digits printed on the card. Auto-generated if left blank.</small>
      </div>
      <div id="wf-result"></div>
      <div style="display:flex;gap:12px;margin-top:20px">
        <button class="btn btn-primary" type="submit">Issue Card</button>
        <button class="btn btn-secondary" data-close-workflow type="button">Cancel</button>
      </div>
    </form>`;

  // Dynamic show/hide limit on card type change
  const cardTypeSelect = document.getElementById("wf-card-type");
  const limitField = document.getElementById("wf-card-limit-field");
  cardTypeSelect?.addEventListener("change", (e) => {
    if (e.target.value === "CREDIT") {
      limitField.style.display = "block";
    } else {
      limitField.style.display = "none";
    }
  });

  // Dynamic customer select handler
  const custSelect = document.getElementById("wf-card-cust-select");
  custSelect?.addEventListener("change", async (e) => {
    const selectedCid = e.target.value;
    if (!selectedCid) return;
    try {
      const resp = await api(`/admin/customers/${selectedCid}`);
      const data = await resp.json();
      const accs = data.accounts || [];
      const hiddenCid = document.querySelector("#workflow-form input[name=customerId]");
      if (hiddenCid) hiddenCid.value = selectedCid;
      const container = document.getElementById("wf-card-acc-container");
      if (container && accs.length > 0) {
        container.innerHTML = `
          <div class="field">
            <label for="wf-card-acc">Linked Bank Account *</label>
            <select id="wf-card-acc" name="accountNo" required>
              ${accs.map(a => `<option value="${escapeHtml(a.account_no)}">${escapeHtml(a.account_no)} · ${escapeHtml(a.product_type)} (${formatMoney(a.balance, a.currency || "LKR")})</option>`).join("")}
            </select>
          </div>`;
      }
    } catch {}
  });

  dialog.showModal();
}

async function submitCreateCard(form) {
  const resultEl = document.getElementById("wf-result");
  resultEl.innerHTML = `<div class="loading" style="margin-top:12px">Issuing card…</div>`;
  const fd = new FormData(form);
  const cardType = fd.get("cardType") || "DEBIT";
  const creditLimit = cardType === "CREDIT" ? parseFloat(fd.get("creditLimit") || "100000") : null;
  const payload = {
    customerId:  fd.get("customerId") || null,
    accountNo:   fd.get("accountNo") ? fd.get("accountNo").trim() : null,
    cardType:    cardType,
    network:     fd.get("network") || "VISA",
    nickname:    fd.get("nickname") ? fd.get("nickname").trim() : null,
    cardLast4:   fd.get("cardLast4") ? fd.get("cardLast4").trim() : null,
    creditLimit: creditLimit
  };

  try {
    const resp = await api("/admin/cards", {
      method: "POST",
      body: JSON.stringify(payload)
    });
    const d = await resp.json();

    const isCredit = d.cardType === "CREDIT";
    const bgGradient = isCredit
      ? "linear-gradient(135deg, #831843, #be185d, #f43f5e)"
      : "linear-gradient(135deg, #1e3a8a, #2563eb, #38bdf8)";

    resultEl.innerHTML = `
      <div class="notice success" style="background:rgba(74,222,128,.1);border:1px solid rgba(74,222,128,.3);border-radius:8px;padding:16px;margin-top:12px">
        <strong style="color:#4ade80">✓ Card Issued Successfully!</strong>

        <div style="margin-top:12px;background:${bgGradient};border-radius:14px;padding:16px;color:#fff;box-shadow:0 8px 24px rgba(0,0,0,0.4)">
          <div style="display:flex;justify-content:space-between;align-items:center;font-size:12px;font-weight:700;letter-spacing:1.5px">
            <span>AGENTIC BANK</span>
            <span style="background:rgba(255,255,255,0.2);padding:2px 8px;border-radius:8px;font-size:10px">${escapeHtml(d.cardType)}</span>
          </div>
          <div style="font-family:monospace;font-size:19px;letter-spacing:3px;margin:18px 0;font-weight:700">
            •••• •••• •••• ${escapeHtml(d.cardLast4)}
          </div>
          <div style="display:flex;justify-content:space-between;align-items:flex-end;font-size:12px">
            <div>
              <div style="font-size:9px;color:rgba(255,255,255,0.7);letter-spacing:0.5px">CARD HOLDER</div>
              <strong style="font-size:13px">${escapeHtml(d.customerName || "VALUED CUSTOMER")}</strong>
            </div>
            <strong style="font-size:16px;font-style:italic">${escapeHtml(d.network)}</strong>
          </div>
        </div>

        <div style="margin-top:12px;display:grid;grid-template-columns:1fr 1fr;gap:8px;font-size:13px">
          <div><span style="color:#8899aa">Linked Account:</span><br><strong>${escapeHtml(d.accountNo || "—")}</strong></div>
          <div><span style="color:#8899aa">Status:</span><br><span class="badge badge-success">ACTIVE</span></div>
          ${d.creditLimit ? `<div><span style="color:#8899aa">Credit Limit:</span><br><strong style="color:#4ade80">${formatMoney(d.creditLimit, "LKR")}</strong></div>` : ""}
          <div><span style="color:#8899aa">Nickname:</span><br><strong>${escapeHtml(d.nickname || "—")}</strong></div>
        </div>

        <div style="margin-top:14px;padding:10px;background:rgba(96,165,250,.12);border-radius:6px;font-size:12px;color:#93c5fd">
          📱 <strong>Visible in Mobile App:</strong> This card is now live and will appear in the customer's mobile app under <strong>My Cards</strong>.
        </div>
      </div>`;

    // If customer detail dialog is currently open for this customer, refresh it
    if (state.lastViewedCustomerIdx !== undefined) {
      setTimeout(() => openCustomerDetail(state.lastViewedCustomerIdx), 800);
    }
  } catch (error) {
    resultEl.innerHTML = `<div class="notice" role="alert" style="margin-top:12px">${escapeHtml(apiErrorText(error))}</div>`;
  }
}

// ── Deposit / Update Balance Workflow ───────────────────────────────────────────

async function openDepositWorkflow(opts = {}) {
  const dialog = document.getElementById("workflow-dialog");
  const title  = document.getElementById("workflow-title");
  const body   = document.getElementById("workflow-body");
  title.textContent = "Branch Counter: Deposit / Update Balance";

  let customerId = opts.customerId || "";
  let customerName = opts.customerName || "";
  let targetAccountNo = opts.accountNo || "";
  let accounts = opts.accounts || [];

  if (customerId && (!accounts || !accounts.length)) {
    try {
      const resp = await api(`/admin/customers/${customerId}`);
      const data = await resp.json();
      accounts = data.accounts || [];
      if (!customerName) customerName = data.full_name;
    } catch {}
  }

  let customerSelectOptions = "";
  if (!customerId && !targetAccountNo) {
    const cachedUsers = state.tableRows["users"] || [];
    if (cachedUsers.length > 0) {
      customerSelectOptions = `<div class="field">
        <label for="wf-dep-cust-select">Select Customer (optional)</label>
        <select id="wf-dep-cust-select">
          <option value="">-- Choose existing customer --</option>
          ${cachedUsers.map(u => `<option value="${escapeHtml(u.id)}">${escapeHtml(u.full_name)} (${escapeHtml(u.cif)})</option>`).join("")}
        </select>
      </div>`;
    }
  }

  let accountInputHtml = "";
  if (accounts.length > 0) {
    accountInputHtml = `
      <div class="field">
        <label for="wf-dep-acc">Target Bank Account *</label>
        <select id="wf-dep-acc" name="accountNo" required>
          ${accounts.map(a => `
            <option value="${escapeHtml(a.account_no)}" ${a.account_no === targetAccountNo ? "selected" : ""}>
              ${escapeHtml(a.account_no)} · ${escapeHtml(a.product_type)} (Balance: ${formatMoney(a.balance, a.currency || "LKR")})
            </option>`).join("")}
        </select>
      </div>`;
  } else {
    accountInputHtml = `
      <div class="field">
        <label for="wf-dep-accno">Account Number *</label>
        <input id="wf-dep-accno" name="accountNo" type="text" required placeholder="e.g. ACC1729758291" value="${escapeHtml(targetAccountNo)}">
        <small>Enter the customer's account number to credit the deposit.</small>
      </div>`;
  }

  body.innerHTML = `
    <form id="workflow-form" data-action="deposit-funds">
      ${customerName ? `
        <div style="background:rgba(16,185,129,0.12);border:1px solid rgba(16,185,129,0.3);border-radius:8px;padding:10px 14px;margin-bottom:14px;font-size:13px">
          Customer: <strong style="color:#34d399">${escapeHtml(customerName)}</strong>
        </div>` : ""}
      ${customerSelectOptions}
      <div id="wf-dep-acc-container">
        ${accountInputHtml}
      </div>
      <div class="field">
        <label for="wf-dep-amount">Deposit Amount (LKR) *</label>
        <input id="wf-dep-amount" name="amount" type="number" required min="1" step="0.01" placeholder="e.g. 50000.00">
        <small>Amount handed over by customer to add to their balance.</small>
      </div>
      <div class="field">
        <label for="wf-dep-remarks">Transaction Remarks</label>
        <input id="wf-dep-remarks" name="remarks" type="text" placeholder="e.g. Cash deposit at branch counter" value="Cash deposit at branch counter">
      </div>
      <div id="wf-dep-result"></div>
      <div style="display:flex;gap:12px;margin-top:20px">
        <button class="btn btn-primary" type="submit" style="background:#059669;border-color:#10b981">Process Deposit</button>
        <button class="btn btn-secondary" data-close-workflow type="button">Cancel</button>
      </div>
    </form>`;

  const custSelect = document.getElementById("wf-dep-cust-select");
  custSelect?.addEventListener("change", async (e) => {
    const selectedCid = e.target.value;
    if (!selectedCid) return;
    try {
      const resp = await api(`/admin/customers/${selectedCid}`);
      const data = await resp.json();
      const accs = data.accounts || [];
      const container = document.getElementById("wf-dep-acc-container");
      if (container && accs.length > 0) {
        container.innerHTML = `
          <div class="field">
            <label for="wf-dep-acc">Target Bank Account *</label>
            <select id="wf-dep-acc" name="accountNo" required>
              ${accs.map(a => `<option value="${escapeHtml(a.account_no)}">${escapeHtml(a.account_no)} · ${escapeHtml(a.product_type)} (${formatMoney(a.balance, a.currency || "LKR")})</option>`).join("")}
            </select>
          </div>`;
      }
    } catch {}
  });

  dialog.showModal();
}

async function submitDeposit(form) {
  const resultEl = document.getElementById("wf-dep-result") || document.getElementById("wf-result");
  if (resultEl) resultEl.innerHTML = `<div class="loading" style="margin-top:12px">Processing deposit…</div>`;
  const fd = new FormData(form);
  const payload = {
    accountNo: fd.get("accountNo") ? fd.get("accountNo").trim() : "",
    amount: parseFloat(fd.get("amount") || "0"),
    remarks: fd.get("remarks") ? fd.get("remarks").trim() : "Cash deposit at branch counter"
  };

  try {
    const resp = await api("/admin/accounts/deposit", {
      method: "POST",
      body: JSON.stringify(payload)
    });
    const d = await resp.json();
    if (!resp.ok) {
      throw new Error(d.error?.message || d.message || "Failed to process deposit.");
    }

    if (resultEl) {
      resultEl.innerHTML = `
        <div class="notice success" style="background:rgba(74,222,128,.1);border:1px solid rgba(74,222,128,.3);border-radius:8px;padding:16px;margin-top:12px">
          <strong style="color:#4ade80">✓ Deposit Processed Successfully!</strong>
          <div style="margin-top:10px;display:grid;grid-template-columns:1fr 1fr;gap:8px;font-size:13px">
            <div><span style="color:#8899aa">Account Number:</span><br><strong>${escapeHtml(d.accountNo)}</strong></div>
            <div><span style="color:#8899aa">Customer Name:</span><br><strong>${escapeHtml(d.customerName || "—")}</strong></div>
            <div><span style="color:#8899aa">Previous Balance:</span><br><span>${formatMoney(d.previousBalance, "LKR")}</span></div>
            <div><span style="color:#8899aa">New Updated Balance:</span><br><strong style="font-size:16px;color:#4ade80">${formatMoney(d.newBalance, "LKR")}</strong></div>
            <div style="grid-column:span 2"><span style="color:#8899aa">Reference:</span><br><code>${escapeHtml(d.txnRef)}</code></div>
          </div>
          <div style="margin-top:12px;padding:10px;background:rgba(16,185,129,.15);border-radius:6px;font-size:12px;color:#34d399">
            📱 <strong>Updated in Real-Time:</strong> The customer's mobile app will reflect this new balance immediately.
          </div>
        </div>`;
    }

    // Refresh views if customer detail or list is open
    if (state.lastViewedCustomerIdx !== undefined) {
      setTimeout(() => openCustomerDetail(state.lastViewedCustomerIdx), 800);
    }
    if (state.page === "users") {
      setTimeout(() => loadModule("users"), 1000);
    } else if (state.page === "dashboard") {
      setTimeout(() => loadDashboard(), 1000);
    }
  } catch (error) {
    if (resultEl) resultEl.innerHTML = `<div class="notice" role="alert" style="margin-top:12px">${escapeHtml(apiErrorText(error))}</div>`;
  }
}

// ── Settings Page ──────────────────────────────────────────────────────────────

function loadSettings() {
  const host = document.getElementById("page-content");
  if (!host) return;
  host.innerHTML = `${heading("settings")}
    <section class="panel">
      <form id="settings-form" style="max-width:480px">
        <div class="field">
          <label for="settings-api-base">Admin API Base URL</label>
          <input id="settings-api-base" name="apiBase" type="url" value="${escapeHtml(state.apiBase)}">
          <small>Spring Boot core API, e.g. http://localhost:8081/api/v1</small>
        </div>
        <div style="display:flex;gap:12px;margin-top:16px">
          <button class="btn btn-primary" type="submit">Save</button>
        </div>
        <p id="settings-saved" style="color:#4ade80;margin-top:8px;display:none">✓ Saved. Reload page to apply.</p>
      </form>
    </section>`;
}

// ── Navigation ─────────────────────────────────────────────────────────────────

function navigate(page) {
  if (!PAGES[page]) return;
  state.page = page;
  // Update nav active states
  document.querySelectorAll(".nav-link").forEach(el => {
    el.classList.toggle("active", el.dataset.page === page);
  });
  const breadcrumb = document.querySelector(".breadcrumb");
  if (breadcrumb) breadcrumb.innerHTML = `Agentic AI Bank <strong>/ ${escapeHtml(PAGES[page]?.label || page)}</strong>`;

  switch (page) {
    case "dashboard":    return loadDashboard();
    case "users":        return loadModule("users");
    case "transactions": return loadModule("transactions");
    case "audit":        return loadModule("audit");
    case "settings":     return loadSettings();
  }
}

// ── Render ─────────────────────────────────────────────────────────────────────

// ── Live WebSocket Stream ──────────────────────────────────────────────────────

let wsClient = null;
let wsReconnectTimer = null;
let wsPingInterval = null;

function initAdminWebSocket() {
  if (!state.token) return;
  if (wsClient && (wsClient.readyState === WebSocket.OPEN || wsClient.readyState === WebSocket.CONNECTING)) {
    return;
  }

  const wsUrl = state.apiBase.replace(/^http/, "ws") + "/ws?token=" + encodeURIComponent(state.token);
  try {
    wsClient = new WebSocket(wsUrl);

    wsClient.onopen = () => {
      console.log("[Admin WS] Connected to real-time event stream");
      updateWsBadge(true);
      clearInterval(wsPingInterval);
      wsPingInterval = setInterval(() => {
        if (wsClient && wsClient.readyState === WebSocket.OPEN) {
          wsClient.send("PING");
        }
      }, 25000);
    };

    wsClient.onmessage = (event) => {
      try {
        const msg = JSON.parse(event.data);
        handleLiveEvent(msg);
      } catch (err) {
        console.warn("[Admin WS] Failed to parse message:", err);
      }
    };

    wsClient.onclose = () => {
      updateWsBadge(false);
      clearInterval(wsPingInterval);
      wsClient = null;
      if (state.token) {
        clearTimeout(wsReconnectTimer);
        wsReconnectTimer = setTimeout(initAdminWebSocket, 3000);
      }
    };

    wsClient.onerror = (err) => {
      console.warn("[Admin WS] Connection error:", err);
    };
  } catch (err) {
    console.warn("[Admin WS] Socket init failed:", err);
  }
}

function updateWsBadge(online) {
  const dot = document.getElementById("ws-status-dot");
  const text = document.getElementById("ws-status-text");
  if (dot) dot.style.background = online ? "#00e5a3" : "#ff9900";
  if (text) text.textContent = online ? "Live WS" : "Connecting...";
}

function handleLiveEvent(msg) {
  if (!msg || !msg.event) return;
  const event = msg.event;
  const data = msg.data || {};

  // Auto-refresh active page if state has changed
  if (state.page === "dashboard") {
    loadDashboard();
  } else if (state.page === "users") {
    loadModule("users");
  } else if (state.page === "audit") {
    loadModule("audit");
  } else if (state.page === "transactions") {
    loadModule("transactions");
  }

  // Show live toast banner in admin interface
  if (event === "BALANCE_UPDATED") {
    showLiveToast(`Live Update: ${data.accountNo || "Account"} balance is now LKR ${formatMoney(data.balance)}`);
  } else if (event === "ADMIN_STATS_UPDATED") {
    showLiveToast(`Live Activity: ${humanize(data.reason || "System updated")}`);
  } else if (event === "CARD_STATUS_CHANGED") {
    showLiveToast(`Card status updated to ${data.status}`);
  }
}

function showLiveToast(text) {
  let container = document.getElementById("admin-toast-container");
  if (!container) {
    container = document.createElement("div");
    container.id = "admin-toast-container";
    container.style.cssText = "position:fixed;bottom:24px;right:24px;z-index:99999;display:flex;flex-direction:column;gap:8px;pointer-events:none;";
    document.body.appendChild(container);
  }
  const toast = document.createElement("div");
  toast.style.cssText = "background:rgba(18,24,38,0.95);border:1px solid rgba(0,229,163,0.4);border-radius:8px;color:#fff;padding:11px 16px;font-size:13px;box-shadow:0 8px 24px rgba(0,0,0,0.6);display:flex;align-items:center;gap:10px;pointer-events:auto;backdrop-filter:blur(8px);transition:all 0.3s ease;";
  toast.innerHTML = `<span style="color:#00e5a3;font-size:15px">⚡</span><span>${escapeHtml(text)}</span>`;
  container.appendChild(toast);
  setTimeout(() => {
    toast.style.opacity = "0";
    toast.style.transform = "translateY(8px)";
    setTimeout(() => toast.remove(), 350);
  }, 4000);
}

function render() {
  if (!state.token || !state.admin) {
    root.innerHTML = loginMarkup();
    attachLoginHandlers();
  } else {
    root.innerHTML = shellMarkup();
    attachShellHandlers();
    navigate(state.page);
    initAdminWebSocket();
  }
}

// ── Login handlers ─────────────────────────────────────────────────────────────

function attachLoginHandlers() {
  const form = document.getElementById("login-form");
  if (!form) return;
  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    const email    = form.elements["email"].value.trim();
    const password = form.elements["password"].value;
    const btn = form.querySelector("button[type=submit]");
    btn.disabled = true;
    btn.textContent = "Signing in…";
    try {
      const resp = await fetch(`${state.apiBase}/admin/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password })
      });
      if (!resp.ok) {
        const err = await resp.json().catch(() => ({}));
        throw new Error(err?.error?.message || err?.message || err?.detail || `Login failed (${resp.status})`);
      }
      const data = await resp.json();
      state.token = data.accessToken;
      state.admin = {
        adminId:      data.adminId,
        fullName:     data.fullName,
        role:         data.role,
        permissions:  data.permissions,
        capabilities: data.capabilities
      };
      sessionStorage.setItem(STORAGE_KEYS.token, state.token);
      sessionStorage.setItem(STORAGE_KEYS.admin, JSON.stringify(state.admin));
      render();
      initAdminWebSocket();
    } catch (err) {
      btn.disabled = false;
      btn.textContent = "Sign in securely";
      const message = (err.name === "TypeError" || !err.message)
        ? `Cannot connect to Core API at ${state.apiBase}. Verify that the backend is running.`
        : err.message;
      root.innerHTML = loginMarkup(message);
      attachLoginHandlers();
    }
  });
}

// ── Shell handlers ─────────────────────────────────────────────────────────────

function attachShellHandlers() {
  // Logout
  document.getElementById("logout")?.addEventListener("click", () => {
    if (wsClient) {
      try { wsClient.close(); } catch {}
      wsClient = null;
    }
    clearInterval(wsPingInterval);
    clearTimeout(wsReconnectTimer);
    state.token = null;
    state.admin = null;
    sessionStorage.removeItem(STORAGE_KEYS.token);
    sessionStorage.removeItem(STORAGE_KEYS.admin);
    render();
  });

  // Sidebar toggle
  document.getElementById("menu-toggle")?.addEventListener("click", () => {
    state.sidebarOpen = !state.sidebarOpen;
    document.getElementById("sidebar")?.classList.toggle("open", state.sidebarOpen);
  });

  // Nav clicks and actions (event delegation)
  document.addEventListener("click", handleClick);
  document.addEventListener("submit", handleSubmit);
}

function handleClick(e) {
  const target = e.target.closest("[data-page], [data-retry], [data-workflow], [data-detail], [data-close-dialog], [data-close-workflow], [data-issue-card-idx], [data-issue-card-cust], [data-issue-card-for-acc], [data-deposit-idx], [data-deposit-for-acc]") || e.target;

  // Page navigation
  if (target.dataset?.page) { navigate(target.dataset.page); return; }

  // Retry
  if (target.dataset?.retry) { navigate(target.dataset.retry); return; }

  // Open workflows
  if (target.dataset?.workflow === "create-customer") { openCreateCustomerWorkflow(); return; }
  if (target.dataset?.workflow === "create-card") { openCreateCardWorkflow(); return; }
  if (target.dataset?.workflow === "deposit-funds") { openDepositWorkflow(); return; }

  // Deposit from customer row
  if (target.dataset?.depositIdx !== undefined) {
    const idx = parseInt(target.dataset.depositIdx, 10);
    const rec = state.tableRows["users"]?.[idx];
    if (rec) {
      openDepositWorkflow({ customerId: rec.id, customerName: rec.full_name });
    }
    return;
  }

  // Deposit for specific account
  if (target.dataset?.depositForAcc !== undefined) {
    openDepositWorkflow({
      customerId: target.dataset.custId,
      customerName: target.dataset.custName,
      accountNo: target.dataset.depositForAcc
    });
    return;
  }

  // Issue card from customer row
  if (target.dataset?.issueCardIdx !== undefined) {
    const idx = parseInt(target.dataset.issueCardIdx, 10);
    const rec = state.tableRows["users"]?.[idx];
    if (rec) {
      openCreateCardWorkflow({ customerId: rec.id, customerName: rec.full_name });
    }
    return;
  }

  // Issue card from customer detail header
  if (target.dataset?.issueCardCust !== undefined) {
    openCreateCardWorkflow({
      customerId: target.dataset.issueCardCust,
      customerName: target.dataset.custName
    });
    return;
  }

  // Issue card for specific account
  if (target.dataset?.issueCardForAcc !== undefined) {
    openCreateCardWorkflow({
      customerId: target.dataset.custId,
      customerName: target.dataset.custName,
      accountNo: target.dataset.issueCardForAcc
    });
    return;
  }

  // Customer detail
  if (target.dataset?.detail === "users") {
    openCustomerDetail(parseInt(target.dataset.idx, 10)); return;
  }

  // Close dialogs
  if (target.dataset?.closeDialog !== undefined || target.hasAttribute?.("data-close-dialog")) {
    document.getElementById("record-dialog")?.close(); return;
  }
  if (target.dataset?.closeWorkflow !== undefined || target.hasAttribute?.("data-close-workflow")) {
    document.getElementById("workflow-dialog")?.close(); return;
  }
}

function handleSubmit(e) {
  if (e.target.id === "workflow-form") {
    e.preventDefault();
    if (e.target.dataset.action === "create-customer") {
      submitCreateCustomer(e.target);
    } else if (e.target.dataset.action === "create-card") {
      submitCreateCard(e.target);
    } else if (e.target.dataset.action === "deposit-funds") {
      submitDeposit(e.target);
    }
  } else if (e.target.id === "settings-form") {
    e.preventDefault();
    const val = e.target.elements["apiBase"]?.value.trim().replace(/\/+$/, "");
    if (val) {
      state.apiBase = val;
      localStorage.setItem(STORAGE_KEYS.apiBase, val);
      const savedNotice = document.getElementById("settings-saved");
      if (savedNotice) savedNotice.style.display = "block";
    }
  }
}

// ── Bootstrap ──────────────────────────────────────────────────────────────────

render();
