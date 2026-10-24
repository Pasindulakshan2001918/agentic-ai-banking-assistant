export function createApiClient({ getApiBase, getToken }) {
  return async function api(path, options = {}) {
    const headers = new Headers(options.headers || {});
    headers.set("Accept", "application/json");
    const token = getToken();
    if (token) headers.set("Authorization", `Bearer ${token}`);
    if (options.body && !(options.body instanceof FormData)) {
      headers.set("Content-Type", "application/json");
    }

    const response = await fetch(`${getApiBase()}${path}`, { ...options, headers });

    if (!response.ok) {
      let message = `Request failed (${response.status})`;
      try {
        const payload = await response.json();
        // Handle Spring Boot error format: { error: { code, message } }
        message = payload?.error?.message || payload?.message || payload?.detail || message;
      } catch {
        // Use the HTTP status message when the server did not return JSON.
      }
      const error = new Error(message);
      error.status = response.status;
      throw error;
    }

    if (response.status === 204) return null;
    return response;
  };
}

export function apiErrorText(error) {
  if (error instanceof TypeError) return "The admin API could not be reached. Check the API URL, server status, and CORS configuration.";
  if (error.status === 401) return "Authentication failed or the session expired. Please sign in again.";
  if (error.status === 403) return "Your account is not permitted to perform this action.";
  if (error.status === 404) return "The requested resource was not found.";
  if (error.status === 409) return error.message || "This record already exists.";
  return error.message || "The request could not be completed.";
}
