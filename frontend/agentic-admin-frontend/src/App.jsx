import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import Landing from "./pages/Landing";
import SuperAdminDashboard from "./pages/SuperAdminDashboard";
import AdminDashboard from "./pages/AdminDashboard";
import CreatorDashboard from "./pages/CreatorDashboard";
import ApproverDashboard from "./pages/ApproverDashboard";
import ViewerDashboard from "./pages/ViewerDashboard";
import keycloak from "./keycloak";
import { hasRole } from "./utils/rbac";

function ProtectedRoute({ children, role }) {
  if (!keycloak.authenticated) {
    return <Navigate to="/" replace />;
  }

  if (role && !hasRole(keycloak, role)) {
    return <Navigate to="/" replace />;
  }

  return children;
}

function RoleRedirect() {
  if (hasRole(keycloak, "SUPER_ADMIN")) return <Navigate to="/super-admin" />;
  if (hasRole(keycloak, "ADMIN")) return <Navigate to="/admin" />;
  if (hasRole(keycloak, "CREATOR")) return <Navigate to="/creator" />;
  if (hasRole(keycloak, "APPROVER")) return <Navigate to="/approver" />;
  if (hasRole(keycloak, "VIEWER")) return <Navigate to="/viewer" />;

  return <Navigate to="/" />;
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route
          path="/"
          element={
            keycloak.authenticated ? <RoleRedirect /> : <Landing />
          }
        />

        <Route
          path="/super-admin"
          element={
            <ProtectedRoute role="SUPER_ADMIN">
              <SuperAdminDashboard />
            </ProtectedRoute>
          }
        />

        <Route
          path="/admin"
          element={
            <ProtectedRoute role="ADMIN">
              <AdminDashboard />
            </ProtectedRoute>
          }
        />

        <Route
          path="/creator"
          element={
            <ProtectedRoute role="CREATOR">
              <CreatorDashboard />
            </ProtectedRoute>
          }
        />

        <Route
          path="/approver"
          element={
            <ProtectedRoute role="APPROVER">
              <ApproverDashboard />
            </ProtectedRoute>
          }
        />

        <Route
          path="/viewer"
          element={
            <ProtectedRoute role="VIEWER">
              <ViewerDashboard />
            </ProtectedRoute>
          }
        />
      </Routes>
    </BrowserRouter>
  );
}