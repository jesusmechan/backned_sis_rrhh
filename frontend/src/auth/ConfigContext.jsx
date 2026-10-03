import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { http } from '../api/client';
import { setRegional } from '../lib/format';
import { useAuth } from './AuthContext';

const ConfigContext = createContext(null);

function legible(codigo) {
  if (codigo == null || codigo === '') return '';
  const raw = String(codigo).replace(/_/g, ' ').toLowerCase();
  return raw.charAt(0).toUpperCase() + raw.slice(1);
}

/**
 * Parámetros del sistema y catálogos de valores. Antes del login solo están los parámetros públicos
 * (marca, idioma, zona, moneda); con sesión se agregan los parámetros de sesión y los catálogos.
 */
export function ConfigProvider({ children }) {
  const { isAuth } = useAuth();
  const [publica, setPublica] = useState(null);
  const [sesion, setSesion] = useState({});
  const [valores, setValores] = useState({});

  const cargarPublica = useCallback(() => http.get('/api/configuracion/publica')
    .then((cfg) => {
      setRegional({
        locale: cfg.locale,
        zona: cfg.zona_horaria,
        moneda: cfg.moneda_codigo,
        simboloMoneda: cfg.moneda_simbolo
      });
      if (cfg.app_nombre) {
        document.title = cfg.empresa_nombre_comercial ? `${cfg.app_nombre} · ${cfg.empresa_nombre_comercial}` : cfg.app_nombre;
      }
      setPublica(cfg);
    })
    .catch(() => setPublica({})), []);

  const cargarSesion = useCallback(() => Promise.all([
    http.get('/api/catalogos/parametros').catch(() => []),
    http.get('/api/catalogos/valores').catch(() => ({}))
  ]).then(([params, vals]) => {
    setSesion(Object.fromEntries((params || []).map((p) => [p.clave, p.valor])));
    setValores(vals || {});
  }), []);

  useEffect(() => { cargarPublica(); }, [cargarPublica]);

  useEffect(() => {
    if (isAuth) cargarSesion();
    else { setSesion({}); setValores({}); }
  }, [isAuth, cargarSesion]);

  const value = useMemo(() => {
    const params = { ...(publica || {}), ...sesion };
    const buscar = (tipo, codigo) => {
      if (codigo == null) return null;
      if (tipo) return (valores[tipo] || []).find((v) => v.codigo === codigo) || null;
      for (const lista of Object.values(valores)) {
        const v = lista.find((x) => x.codigo === codigo);
        if (v) return v;
      }
      return null;
    };
    return {
      listo: publica != null,
      params,
      param: (clave, vacio = '') => params[clave] ?? vacio,
      num: (clave, vacio) => {
        const n = Number(params[clave]);
        return params[clave] == null || params[clave] === '' || Number.isNaN(n) ? vacio : n;
      },
      opciones: (tipo) => valores[tipo] || [],
      porDefecto: (tipo) => (valores[tipo] || []).find((v) => v.porDefecto)?.codigo ?? (valores[tipo]?.[0]?.codigo || ''),
      valor: buscar,
      etiqueta: (tipo, codigo, vacio = '—') => {
        if (codigo == null || codigo === '') return vacio;
        return buscar(tipo, codigo)?.nombre || legible(codigo);
      },
      tono: (tipo, codigo) => buscar(tipo, codigo)?.tono || null,
      /** Valida un valor contra la regla (regex) del catálogo; devuelve el mensaje de error o ''. */
      validarRegla: (tipo, codigo, texto) => {
        const v = buscar(tipo, codigo);
        if (!v?.regla || !texto) return '';
        try {
          return new RegExp(v.regla).test(texto) ? '' : (v.mensajeRegla || 'Formato no válido');
        } catch {
          return '';
        }
      },
      recargar: () => Promise.all([cargarPublica(), isAuth ? cargarSesion() : null])
    };
  }, [publica, sesion, valores, isAuth, cargarPublica, cargarSesion]);

  if (publica == null) return null;
  return <ConfigContext.Provider value={value}>{children}</ConfigContext.Provider>;
}

export function useConfig() {
  const ctx = useContext(ConfigContext);
  if (!ctx) {
    throw new Error('useConfig debe usarse dentro de ConfigProvider');
  }
  return ctx;
}
