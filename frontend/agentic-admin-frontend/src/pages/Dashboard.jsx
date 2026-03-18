import React from "react";
import { getUserRoles, hasRole, hasAnyRole } from "../utils/rbac";

export default function Dashboard({ keycloak }) {
  const roles = getUserRoles(keycloak);
  const username = keycloak.tokenParsed?.preferred_username;

  return (
    <div className="min-h-screen bg-slate-950 text-white flex flex-col items-center justify-center">
      
      <h1 className="text-4xl font-bold mb-6">
        Secure Administrative Dashboard
      </h1>

      <p className="mb-2 text-slate-400">
        Authenticated as: {username}
      </p>

      <p className="mb-6 text-slate-400">
        Roles: {roles.join(", ")}
      </p>

      {/* ================= ROLE BASED SECTIONS ================= */}

      {/* SUPER ADMIN */}
      {hasRole(keycloak, "SUPER_ADMIN") && (
        <div className="mb-4 p-4 bg-indigo-800/40 rounded-xl">
          SUPER ADMIN PANEL — Full System Control
        </div>
      )}

      {/* ADMIN */}
      {hasRole(keycloak, "ADMIN") && (
        <div className="mb-4 p-4 bg-blue-800/40 rounded-xl">
          ADMIN PANEL — User & Operations Management
        </div>
      )}

      {/* CREATOR */}
      {hasRole(keycloak, "CREATOR") && (
        <div className="mb-4 p-4 bg-green-800/40 rounded-xl">
          CREATOR PANEL — Create Transactions / Records
        </div>
      )}

      {/* APPROVER */}
      {hasRole(keycloak, "APPROVER") && (
        <div className="mb-4 p-4 bg-yellow-700/40 rounded-xl">
          APPROVER PANEL — Approve Pending Operations
        </div>
      )}

      {/* VIEWER */}
      {hasRole(keycloak, "VIEWER") && (
        <div className="mb-4 p-4 bg-gray-700/40 rounded-xl">
          VIEWER PANEL — Read-Only Monitoring Access
        </div>
      )}

      {/* ========================================================= */}

      <button
        onClick={() =>
          keycloak.logout({ redirectUri: window.location.origin })
        }
        className="mt-8 px-6 py-3 bg-indigo-600 rounded-xl hover:bg-indigo-700 transition"
      >
        Logout Securely
      </button>
    </div>
  );
}