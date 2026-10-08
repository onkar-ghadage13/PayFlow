const API = import.meta.env.VITE_API_URL || '/api';

export async function api(path, options = {}) {
  const token = localStorage.getItem('payflow-token') || sessionStorage.getItem('payflow-token');
  let response;
  try {
    response = await fetch(`${API}${path}`, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...options.headers,
      },
    });
  } catch {
    throw new Error('Cannot reach the PayFlow API. Start PayFlowApplication in IntelliJ on port 8080, then try again.');
  }

  const data = await response.json().catch(() => ({}));
  if (response.status === 401 && !/^\/(auth\/login|auth\/register)(\?|$)/.test(path)) {
    window.dispatchEvent(new Event('payflow:session-expired'));
  }
  if (!response.ok) throw new Error(data.message || data.error || `Request failed (${response.status})`);
  return data;
}
