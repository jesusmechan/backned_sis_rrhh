import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { clearSession, getStoredUser, http, saveSession, saveUser } from '../api/client';

const AuthContext = createContext(null);

function pathOf(ruta = '') {
  return String(ruta).split('?')[0];
}

export function AuthProvider({ children }) {
  const [usuario, setUsuario] = useState(getStoredUser);

  useEffect(() => {
    if (!usuario) return;
    http.get('/api/sesion')
      .then((sesion) => {
        setUsuario(sesion);
        saveUser(sesion);
      })
      .catch(() => { /* se mantiene la sesión local */ });
  }, []);

  const value = useMemo(() => {
    const rol = usuario?.rol;
    const menu = Array.isArray(usuario?.menu) ? usuario.menu : [];
    const permisos = Array.isArray(usuario?.permisos) ? usuario.permisos : [];
    const rutas = menu.flatMap((grupo) => (grupo.items || []).map((item) => pathOf(item.ruta)));
    return {
      usuario,
      rol,
      perfil: usuario?.perfil || rol,
      menu,
      permisos,
      isAuth: Boolean(usuario),
      hasAnyRole: (...roles) => roles.includes(rol),
      hasPermission: (...codes) => codes.some((code) => permisos.includes(code)),
      canAccess: (path) => {
        const clean = pathOf(path);
        if (rutas.length === 0) {
          return ['/', '/perfil', '/permisos', '/horas-extras', '/marcar', '/asistencia'].includes(clean);
        }
        return rutas.includes(clean);
      },
      refreshSesion: async () => {
        const sesion = await http.get('/api/sesion');
        setUsuario(sesion);
        saveUser(sesion);
        return sesion;
      },
      login: async (nombreUsuario, password) => {
        const data = await http.post('/api/auth/login', { nombreUsuario, password });
        saveSession(data);
        setUsuario(data.usuario);
        return data.usuario;
      },
      logout: async () => {
        try {
          const refreshToken = localStorage.getItem('andina.refresh');
          if (refreshToken) await http.post('/api/auth/logout', { refreshToken });
        } catch {
          /* sesión local igual se limpia */
        }
        clearSession();
        setUsuario(null);
      }
    };
  }, [usuario]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth debe usarse dentro de AuthProvider');
  }
  return ctx;
}
