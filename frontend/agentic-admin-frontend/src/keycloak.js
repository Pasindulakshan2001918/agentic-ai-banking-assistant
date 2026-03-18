import Keycloak from "keycloak-js";

const keycloak = new Keycloak({
  url: "http://localhost:8180",
  realm: "agentic-bank",
  clientId: "agentic-admin-frontend",
});

export default keycloak;
