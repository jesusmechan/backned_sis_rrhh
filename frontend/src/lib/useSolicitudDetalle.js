import { useEffect, useState } from 'react';
import { http } from '../api/client';
import { useFlashOk } from './useFlashOk';

export function useSolicitudDetalle(apiBase, id) {
  const [detalle, setDetalle] = useState(null);
  const [historial, setHistorial] = useState([]);
  const [error, setError] = useState('');
  const [ok, setOk] = useFlashOk();
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  async function reload() {
    const [d, h] = await Promise.all([
      http.get(`${apiBase}/${id}`),
      http.get(`${apiBase}/${id}/historial`).catch(() => [])
    ]);
    setDetalle(d);
    setHistorial(h || []);
  }

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError('');
    reload()
      .catch((e) => { if (!cancelled) setError(e.message); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [apiBase, id]);

  async function cancelar() {
    if (!window.confirm('¿Cancelar esta solicitud? Los pasos pendientes se cierran.')) return;
    setError('');
    setSaving(true);
    try {
      await http.post(`${apiBase}/${id}/cancelar`);
      setOk('Solicitud cancelada');
      await reload();
    } catch (e) {
      setError(e.message);
    } finally {
      setSaving(false);
    }
  }

  return { detalle, historial, loading, error, ok, saving, cancelar };
}
