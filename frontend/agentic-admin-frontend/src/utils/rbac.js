export const getUserRoles = (keycloak) => {
  return keycloak.tokenParsed?.realm_access?.roles || [];
};

export const hasRole = (keycloak, role) => {
  const roles = getUserRoles(keycloak);
  return roles.includes(role);
};

export const hasAnyRole = (keycloak, roleArray) => {
  const roles = getUserRoles(keycloak);
  return roleArray.some((role) => roles.includes(role));
};