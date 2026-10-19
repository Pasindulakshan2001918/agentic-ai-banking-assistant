export function createApiClient({ getApiBase, getToken }) {
  return async function api(path, options = {}) {
    const headers = new Headers(options.headers || {});
    headers.set("Accept", "application/json");
    const token = getToken();
    if (token) headers.set("Authorization", `Bearer ${token}`);
    if (options.body && !(options.body instanceof FormData)) headers.set("Content-Type", "application/json");

    const response = await fetch(`${getApiBase()}${path}`, { ...options, headers });
    if (!response.ok) {
      let message = `Request failed (${response.status})`;
      try {
        const payload = await response.json();
        message = payload?.error?.message || payload?.message || message;
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
  if (error.status === 401) return "Administrator authentication failed or the session expired. Check your credentials and sign in again.";
  if (error.status === 403) return "Your administrator account is not permitted to view this information.";
  if (error.status === 404) return "The requested admin API route or record was not found.";
  return error.message || "The request could not be completed.";
}
