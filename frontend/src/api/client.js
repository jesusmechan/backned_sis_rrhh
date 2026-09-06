const TOKEN_KEY = 'andina.access';
const REFRESH_KEY = 'andina.refresh';
const USER_KEY = 'andina.user';

export function getAccessToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function getRefreshToken() {
  return localStorage.getItem(REFRESH_KEY);
}

export function getStoredUser() {
  const raw = localStorage.getItem(USER_KEY);
  return raw ? JSON.parse(raw) : null;
}

export function saveUser(usuario) {
  localStorage.setItem(USER_KEY, JSON.stringify(usuario));
}

export function saveSession(data) {
  localStorage.setItem(TOKEN_KEY, data.accessToken);
  localStorage.setItem(REFRESH_KEY, data.refreshToken);
  saveUser(data.usuario);
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(REFRESH_KEY);
  localStorage.removeItem(USER_KEY);
}

async function parseError(response) {
  try {
    const body = await response.json();
    return body.message || body.error || `Error ${response.status}`;
  } catch {
    return `Error ${response.status}`;
  }
}

let refreshPromise = null;

async function refreshAccess() {
  const refreshToken = getRefreshToken();
  if (!refreshToken) {
    throw new Error('Sesión expirada');
  }
  const response = await fetch('/api/auth/refresh', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken })
  });
  if (!response.ok) {
    clearSession();
    throw new Error('Sesión expirada');
  }
  const data = await response.json();
  saveSession(data);
  return data.accessToken;
}

export async function api(path, options = {}) {
  const headers = new Headers(options.headers || {});
  if (!(options.body instanceof FormData) && !headers.has('Content-Type') && options.body) {
    headers.set('Content-Type', 'application/json');
  }
  const token = getAccessToken();
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  let response = await fetch(path, { ...options, headers });
  if (response.status === 401 && !path.startsWith('/api/auth/')) {
    try {
      if (!refreshPromise) {
        refreshPromise = refreshAccess().finally(() => { refreshPromise = null; });
      }
      const nextToken = await refreshPromise;
      headers.set('Authorization', `Bearer ${nextToken}`);
      response = await fetch(path, { ...options, headers });
    } catch {
      clearSession();
      window.location.assign('/login');
      throw new Error('Debe iniciar sesión');
    }
  }

  if (!response.ok) {
    throw new Error(await parseError(response));
  }
  if (response.status === 204) {
    return null;
  }
  const contentType = response.headers.get('Content-Type') || '';
  if (contentType.includes('application/json')) {
    return response.json();
  }
  return response.blob();
}

export function asPage(data) {
  if (Array.isArray(data)) {
    return {
      content: data,
      page: 1,
      size: data.length || 10,
      totalElements: data.length,
      totalPages: 1
    };
  }
  return {
    content: Array.isArray(data?.content) ? data.content : [],
    page: data?.page || 1,
    size: data?.size || 10,
    totalElements: data?.totalElements ?? 0,
    totalPages: Math.max(1, data?.totalPages || 1)
  };
}

export function toTime(value) {
  if (!value) return null;
  return value.length === 5 ? `${value}:00` : value;
}

export const http = {
  get: (path) => api(path),
  post: (path, body) => api(path, { method: 'POST', body: JSON.stringify(body) }),
  put: (path, body) => api(path, { method: 'PUT', body: JSON.stringify(body) }),
  patch: (path) => api(path, { method: 'PATCH' }),
  delete: (path) => api(path, { method: 'DELETE' }),
  download: (path) => api(path),
  page: async (path) => asPage(await api(path))
};

export function pagePath(path, params = {}) {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      query.set(key, String(value));
    }
  });
  const qs = query.toString();
  return qs ? `${path}?${qs}` : path;
}

export const emptyPage = {
  content: [],
  page: 1,
  size: 10,
  totalElements: 0,
  totalPages: 1
};

export const PAGE_SIZE = 10;
export const SELECT_SIZE = 100;
