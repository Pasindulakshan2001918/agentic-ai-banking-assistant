import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App";
import "./index.css";
import "./App.css";
import keycloak from "./keycloak";

keycloak
  .init({
    onLoad: "check-sso",
    pkceMethod: "S256",
  })
  .then((authenticated) => {
    ReactDOM.createRoot(document.getElementById("root")).render(
      <React.StrictMode>
        <App keycloak={keycloak} />
      </React.StrictMode>
    );
  })
  .catch((error) => {
    console.error("Keycloak initialization failed:", error);
  });