export const STORAGE_KEYS = Object.freeze({
  apiBase: "northstar.admin.apiBase",
  token: "northstar.admin.accessToken",
  admin: "northstar.admin.profile"
});

export const DEFAULT_API_BASE = window.ADMIN_API_BASE_URL
  || `${window.location.protocol}//${window.location.hostname}:8081/api/v1`;

export const PAGES = Object.freeze({
  dashboard: { label: "Dashboard", section: "OVERVIEW", glyph: "DB", permission: "dashboard:read", description: "Live overview of banking operations and risk." },
  users: { label: "User management", section: "OPERATIONS", glyph: "US", permission: "users:read", description: "Review customer profiles and account status." },
  transactions: { label: "Transaction audit", section: "OPERATIONS", glyph: "TX", permission: "transactions:read", description: "Search and review the financial transaction ledger." },
  bills: { label: "Bill payment audit", section: "OPERATIONS", glyph: "BP", permission: "bill-payments:read", description: "Monitor bill payments and investigate exceptions." },
  security: { label: "Security monitoring", section: "RISK & CONTROLS", glyph: "SC", permission: "security:read", description: "Review risk signals and investigation cases." },
  ai: { label: "AI agent monitoring", section: "RISK & CONTROLS", glyph: "AI", permission: "ai:read", description: "Audit agent interactions and backend tool execution." },
  audit: { label: "Admin audit logs", section: "RISK & CONTROLS", glyph: "AL", permission: "audit:read", description: "Trace administrative actions and outcomes." },
  reports: { label: "Reports & analytics", section: "ADMINISTRATION", glyph: "RP", permission: "reports:read", description: "Generate permission-scoped banking reports." },
  administrators: { label: "Administrators", section: "ADMINISTRATION", glyph: "AD", permission: "administrators:read", description: "Review administrators and assigned roles." },
  settings: { label: "System settings", section: "ADMINISTRATION", glyph: "ST", description: "Configure the frontend connection to the admin API." }
});

export const MODULES = Object.freeze({
  users: {
    endpoint: "/admin/users",
    title: "Registered users",
    columns: [["customerId", "User ID"], ["fullName", "Customer"], ["mobileMasked", "Mobile"], ["emailMasked", "Email"], ["status", "Account status"], ["registeredAt", "Registered"]],
    filters: ["status"]
  },
  transactions: {
    endpoint: "/admin/transactions",
    title: "Transactions",
    columns: [["transactionRef", "Reference"], ["transactionType", "Type"], ["customerName", "Customer"], ["amount", "Amount"], ["currency", "Currency"], ["status", "Status"], ["createdAt", "Date"]],
    filters: ["status", "transactionType"]
  },
  bills: {
    endpoint: "/admin/bill-payments",
    title: "Bill payments",
    columns: [["paymentRef", "Reference"], ["billerName", "Biller"], ["customerName", "Customer"], ["amount", "Amount"], ["currency", "Currency"], ["status", "Status"], ["createdAt", "Date"]],
    filters: ["status"]
  },
  security: {
    endpoint: "/admin/security/alerts",
    title: "Security alerts",
    columns: [["alertId", "Alert"], ["severity", "Severity"], ["reason", "Detection reason"], ["customerName", "Related user"], ["status", "Investigation"], ["createdAt", "Detected"]],
    filters: ["status", "severity"]
  },
  ai: {
    endpoint: "/admin/ai/interactions",
    title: "AI interactions",
    columns: [["interactionId", "Interaction"], ["customerReference", "User"], ["intent", "Intent"], ["tools", "Tools"], ["status", "Execution"], ["latencyMs", "Latency"], ["createdAt", "Date"]],
    filters: ["status"]
  },
  audit: {
    endpoint: "/admin/audit-logs",
    title: "Administrative activity",
    columns: [["eventId", "Event"], ["administratorName", "Administrator"], ["action", "Action"], ["resourceType", "Resource"], ["outcome", "Outcome"], ["createdAt", "Date"]],
    filters: ["outcome", "action"]
  },
  administrators: {
    endpoint: "/admin/administrators",
    title: "Administrator accounts",
    columns: [["administratorId", "Admin ID"], ["fullName", "Administrator"], ["email", "Email"], ["role", "Role"], ["status", "Status"], ["mustChangeCredentials", "Credential change required"], ["lastLoginAt", "Last login"]],
    filters: ["role", "status"]
  }
});
