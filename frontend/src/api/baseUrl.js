/** Base del API en producción (Cloudflare Pages). Vacío en dev → rutas relativas y proxy Vite. */
const raw = (import.meta.env.VITE_API_BASE_URL || '').trim();

export const apiBaseUrl = raw.replace(/\/$/, '');

export function apiUrl(path) {
  const normalized = path.startsWith('/') ? path : `/${path}`;
  return apiBaseUrl ? `${apiBaseUrl}${normalized}` : normalized;
}

export function wsUrl(path = '/ws') {
  const normalized = path.startsWith('/') ? path : `/${path}`;
  if (!apiBaseUrl) return normalized;
  let origin = apiBaseUrl;
  if (origin.startsWith('http://')) {
    origin = `https://${origin.slice(7)}`;
  }
  return `${origin}${normalized}`;
}
