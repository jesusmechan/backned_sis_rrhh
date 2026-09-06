import { useEffect, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import { http } from '../api/client';
import { Alert, Avatar, Badge, Button } from '../components/ui';
import { PasosInstancia } from './flujoShared';
import { factsOf, fmtDateTime } from './bandejaShared';

export function HoraExtraDetalle() {
  const { id } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const [detalle, setDetalle] = useState(null);
  const [historial, setHistorial] = useState([]);
  const [error, setError] = useState('');
  const [ok, setOk] = useState(location.state?.ok || '');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  async function load() {
    const [d, h] = await Promise.all([
      http.get(`/api/horas-extras/${id}`),
      http.get(`/api/horas-extras/${id}/historial`).catch(() => [])
    ]);
    setDetalle(d);
    setHistorial(h || []);
  }

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError('');
    load()
      .catch((e) => { if (!cancelled) setError(e.message); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [id]);

  async function cancelar() {
    if (!window.confirm('¿Cancelar esta solicitud? Los pasos pendientes se cierran.')) return;
    setError('');
    setSaving(true);
    try {
      await http.post(`/api/horas-extras/${id}/cancelar`);
      setOk('Solicitud cancelada');
      await load();
    } catch (e) {
      setError(e.message);
    } finally {
      setSaving(false);
    }
  }

  const facts = factsOf(detalle, 'HORA_EXTRA');

  return (
    <div>
      <button
        type="button"
        onClick={() => navigate('/horas-extras')}
        className="mb-4 inline-flex items-center gap-1.5 text-sm text-slate-500 hover:text-navy"
      >
        <ArrowLeft size={16} /> Horas extras
      </button>

      <div className="mb-6 flex flex-col gap-4 border-b border-line pb-5 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-500">Trámites</p>
          <h1 className="mt-1 text-3xl font-bold text-navy">Solicitud de horas extras</h1>
          {detalle && (
            <p className="mt-2 text-sm text-muted">#{detalle.idSolicitudHoraExtra}</p>
          )}
        </div>
        <div className="flex flex-wrap items-center gap-2">
          {detalle?.estado && <Badge value={detalle.estado} />}
          {detalle?.estado === 'PENDIENTE' && (
            <Button variant="danger" disabled={saving} onClick={cancelar}>
              {saving ? 'Cancelando…' : 'Cancelar solicitud'}
            </Button>
          )}
        </div>
      </div>

      <Alert>{error}</Alert>
      <Alert ok>{ok}</Alert>

      {loading ? (
        <p className="text-sm text-muted">Cargando solicitud…</p>
      ) : !detalle ? null : (
        <div className="grid gap-4 lg:grid-cols-[1.1fr_0.9fr]">
          <section className="rounded-xl border border-line bg-white p-5">
            <div className="flex items-center gap-3">
              <Avatar name={detalle.empleado} />
              <div>
                <p className="font-semibold text-navy">{detalle.empleado}</p>
                <p className="text-xs text-muted">Horas extras</p>
              </div>
            </div>
            <div className="mt-5 grid gap-3 sm:grid-cols-2">
              {facts.map(([label, value]) => (
                <div key={label}>
                  <p className="text-xs text-slate-500">{label}</p>
                  <p className="mt-0.5 text-sm font-medium text-navy">{value || '—'}</p>
                </div>
              ))}
              <div>
                <p className="text-xs text-slate-500">Registrada</p>
                <p className="mt-0.5 text-sm font-medium text-navy">{fmtDateTime(detalle.fechaCreacion)}</p>
              </div>
            </div>
            {detalle.motivo && (
              <div className="mt-5">
                <p className="text-xs text-slate-500">Motivo</p>
                <p className="mt-1 text-sm text-slate-700">{detalle.motivo}</p>
              </div>
            )}
          </section>

          <div className="space-y-4">
            <section className="rounded-xl border border-line bg-white p-5">
              <p className="mb-3 text-sm font-semibold text-navy">Circuito</p>
              <PasosInstancia pasos={detalle.pasos || []} />
            </section>
            <section className="rounded-xl border border-line bg-white p-5">
              <p className="mb-3 text-sm font-semibold text-navy">Historial</p>
              {historial.length === 0 ? (
                <p className="text-sm text-muted">Sin movimientos.</p>
              ) : (
                <ul className="space-y-3">
                  {historial.map((h) => (
                    <li key={h.idHistorial} className="text-sm text-slate-600">
                      <p>
                        <span className="font-medium text-navy">{h.accion}</span>
                        {h.estadoNuevo ? ` → ${h.estadoNuevo}` : ''}
                        {h.usuario ? ` · ${h.usuario}` : ''}
                      </p>
                      <p className="text-xs text-muted">{fmtDateTime(h.fechaHora)}</p>
                      {h.comentario && <p className="mt-0.5 text-xs">{h.comentario}</p>}
                    </li>
                  ))}
                </ul>
              )}
            </section>
          </div>
        </div>
      )}
    </div>
  );
}
