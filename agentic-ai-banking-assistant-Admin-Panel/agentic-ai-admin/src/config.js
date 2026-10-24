export const STORAGE_KEYS = Object.freeze({
  apiBase: "northstar.admin.apiBase",
  token:   "northstar.admin.accessToken",
  admin:   "northstar.admin.profile"
});

export const DEFAULT_API_BASE = window.ADMIN_API_BASE_URL
  || `${window.location.protocol}//${window.location.hostname}:8081/api/v1`;

export const PAGES = Object.freeze({
  dashboard:      { label: "Dashboard",          section: "OVERVIEW",          glyph: "DB", permission: "dashboard:read",       description: "Live overview of banking operations." },
  users:          { label: "Customer Accounts",  section: "OPERATIONS",        glyph: "US", permission: "users:read",           description: "View, create and manage customer bank accounts." },
  transactions:   { label: "Transaction Audit",  section: "OPERATIONS",        glyph: "TX", permission: "transactions:read",    description: "Search and review the financial transaction ledger." },
  audit:          { label: "Audit Logs",         section: "RISK & CONTROLS",   glyph: "AL", permission: "audit:read",           description: "Trace AI agent actions and their outcomes." },
  settings:       { label: "System Settings",    section: "ADMINISTRATION",    glyph: "ST",                                     description: "Configure the frontend connection to the admin API." }
});

export const MODULES = Object.freeze({
  users: {
    endpoint: "/admin/customers",
    title: "Customer Accounts",
    columns: [
      ["cif",          "CIF"],
      ["full_name",    "Customer Name"],
      ["mobile",       "Mobile"],
      ["account_count","Accounts"],
      ["username",     "App Username"],
      ["is_registered","Registered"],
      ["status",       "Status"],
      ["created_at",   "Created"]
    ],
    filters: ["status"]
  },
  transactions: {
    endpoint: "/admin/transactions",
    title: "Transactions",
    columns: [
      ["txn_ref",       "Reference"],
      ["action_type",   "Type"],
      ["customer_name", "Customer"],
      ["amount",        "Amount"],
      ["currency",      "Currency"],
      ["status",        "Status"],
      ["created_at",    "Date"]
    ],
    filters: ["status"]
  },
  audit: {
    endpoint: "/admin/audit-logs",
    title: "AI Agent Audit Logs",
    columns: [
      ["event_id",   "Event"],
      ["action",     "Stage"],
      ["intent",     "Intent"],
      ["outcome",    "Decision"],
      ["tool_name",  "Tool Used"],
      ["created_at", "Timestamp"]
    ],
    filters: []
  }
});
