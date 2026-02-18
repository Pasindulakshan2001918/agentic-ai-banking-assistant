import Keycloak from "keycloak-js";

const keycloak = new Keycloak({
  url: "http://localhost:8080",
  realm: "agentic-bank",
  clientId: "agentic-frontend",
});

export default keycloak;
