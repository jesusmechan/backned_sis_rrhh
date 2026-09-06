import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Download, Search } from 'lucide-react';
import { emptyPage, http, PAGE_SIZE, pagePath, SELECT_SIZE } from '../api/client';
import { Alert, Avatar, Badge, Button, Empty, Pager, downloadBlob } from '../components/ui';
import { TIPO, asResueltaHora, asResueltaPermiso, fmtDateTime } from './bandejaShared';

export function Bandeja() {
  const navigate = useNavigate();
  const location = useLocation();
  const [tab, setTab] = useState('pendientes');
  const [items, setItems] = useState([]);
  const [resueltas, setResueltas] = useState([]);
  const [meta, setMeta] = useState(emptyPage);
  const [page, setPage] = useState(1);
  const [counts, setCounts] = useState({ total: 0, permiso: 0, hora: 0 });
  const [q, setQ] = useState('');
  const [qDebounced, setQDebounced] = useState('');
  const [tipo, setTipo] = useState('');
  const [estadoResuelta, setEstadoResuelta] = useState('');
  const [checked, setChecked] = useState({});
  const [error, setError] = useState('');
  const [ok, setOk] = useState(location.state?.ok || '');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    const t = setTimeout(() => setQDebounced(q.trim()), 300);
    return () => clearTimeout(t);
  }, [q]);

  useEffect(() => {
    if (location.state?.ok) {
      navigate(location.pathname, { replace: true, state: {} });
    }
  }, []);

  async function loadCounts() {
    const [perm, hex] = await Promise.all([
      http.page(pagePath('/api/bandeja', { page: 1, size: 1, tipo: 'PERMISO' })),
      http.page(pagePath('/api/bandeja', { page: 1, size: 1, tipo: 'HORA_EXTRA' }))
    ]);
    setCounts({
      permiso: perm.totalElements || 0,
      hora: hex.totalElements || 0,
      total: (perm.totalElements || 0) + (hex.totalElements || 0)
    });
  }

  async function loadPendientes() {
    const b = await http.page(pagePath('/api/bandeja', { page, size: PAGE_SIZE, q: qDebounced, tipo }));
    setItems(b.content || []);
    setMeta(b);
  }

  async function loadResueltas() {
    const estado = estadoResuelta || 'APROBADO,RECHAZADO,CANCELADO';
    const params = { page: 1, size: SELECT_SIZE, estado, q: qDebounced };
    const [perm, hex] = await Promise.all([
      tipo === 'HORA_EXTRA' ? Promise.resolve(emptyPage) : http.page(pagePath('/api/permisos', params)).catch(() => emptyPage),
      tipo === 'PERMISO' ? Promise.resolve(emptyPage) : http.page(pagePath('/api/horas-extras', params)).catch(() => emptyPage)
    ]);
    const merged = [
      ...(perm.content || []).map(asResueltaPermiso),
      ...(hex.content || []).map(asResueltaHora)
    ].sort((a, b) => String(b.fecha || '').localeCompare(String(a.fecha || '')));
    const totalElements = merged.length;
    const totalPages = Math.max(1, Math.ceil(totalElements / PAGE_SIZE) || 1);
    const safePage = Math.min(page, totalPages);
    const slice = merged.slice((safePage - 1) * PAGE_SIZE, safePage * PAGE_SIZE);
    setResueltas(slice);
    setMeta({ content: slice, page: safePage, size: PAGE_SIZE, totalElements, totalPages });
    if (safePage !== page) setPage(safePage);
  }

  useEffect(() => {
    let cancelled = false;
    setError('');
    const run = tab === 'pendientes' ? loadPendientes : loadResueltas;
    run().catch((e) => { if (!cancelled) setError(e.message); });
    return () => { cancelled = true; };
  }, [page, tab, qDebounced, tipo, estadoResuelta]);

  useEffect(() => { loadCounts().catch(() => {}); }, []);

  async function masiva() {
    const ids = Object.entries(checked).filter(([, v]) => v).map(([id]) => Number(id));
    if (!ids.length) return;
    if (!window.confirm(`¿Aprobar ${ids.length} paso(s)?`)) return;
    setError('');
    setOk('');
    setSaving(true);
    try {
      for (const id of ids) {
        await http.post(`/api/pasos/${id}/aprobar`, { comentario: 'Aprobación masiva' });
      }
      setOk(`${ids.length} paso(s) aprobado(s)`);
      setChecked({});
      await Promise.all([loadPendientes(), loadCounts()]);
    } catch (e) {
      setError(e.message);
    } finally {
      setSaving(false);
    }
  }

  function exportar() {
    const rows = tab === 'pendientes'
      ? [['Solicitante', 'Tipo', 'Trámite', 'Paso', 'Motivo', 'Desde']].concat(
        items.map((it) => [it.solicitante, TIPO[it.tipoSolicitud] || it.tipoSolicitud, it.tipoTramite, `${it.numeroPaso} ${it.nombrePaso}`, it.motivo, it.fechaInicio || ''])
      )
      : [['Solicitante', 'Tipo', 'Trámite', 'Estado', 'Motivo', 'Fecha']].concat(
        resueltas.map((it) => [it.solicitante, TIPO[it.tipoSolicitud] || it.tipoSolicitud, it.tipoTramite, it.estado, it.motivo, it.fecha || ''])
      );
    const csv = rows.map((r) => r.map((c) => `"${String(c ?? '').replaceAll('"', '""')}"`).join(';')).join('\n');
    downloadBlob(new Blob([csv], { type: 'text/csv;charset=utf-8' }), `bandeja-${tab}.csv`);
  }

  const lista = tab === 'pendientes' ? items : resueltas;
  const seleccionados = Object.values(checked).filter(Boolean).length;

  return (
    <div>
      <div className="mb-6 border-b border-line pb-5">
        <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-500">Aprobaciones</p>
        <h1 className="mt-1 text-3xl font-bold text-navy">Bandeja</h1>
        <p className="mt-2 text-sm text-muted">Pasos en curso asignados a su jefatura, perfil o usuario.</p>
      </div>

      <Alert>{error}</Alert>
      <Alert ok>{ok}</Alert>

      <div className="mb-5 grid gap-3 sm:grid-cols-3">
        <Kpi value={counts.total} label="Por atender" hint="Pasos que esperan su decisión" />
        <Kpi value={counts.permiso} label="Permisos" hint="En su bandeja" />
        <Kpi value={counts.hora} label="Horas extras" hint="En su bandeja" />
      </div>

      <div className="mb-3 flex flex-wrap items-center gap-2">
        {[
          ['pendientes', 'Por atender', counts.total],
          ['resueltas', 'Resueltas', null]
        ].map(([id, label, n]) => (
          <button
            key={id}
            type="button"
            onClick={() => { setTab(id); setPage(1); setChecked({}); }}
            className={`rounded-lg px-3 py-1.5 text-sm ${tab === id ? 'bg-navy text-white' : 'border border-line bg-white text-slate-600 hover:bg-slate-50'}`}
          >
            {label}
            {n != null && (
              <span className={`ml-2 rounded-md px-1.5 text-xs ${tab === id ? 'bg-white/15 text-white' : 'bg-slate-100 text-slate-600'}`}>{n}</span>
            )}
          </button>
        ))}
        <div className="ml-auto flex flex-wrap gap-2">
          {tab === 'pendientes' && (
            <Button onClick={masiva} disabled={saving || !seleccionados}>
              Aprobar seleccionadas{seleccionados ? ` (${seleccionados})` : ''}
            </Button>
          )}
          <Button variant="secondary" onClick={exportar}><Download size={14} /> Exportar</Button>
        </div>
      </div>

      <div className="mb-3 flex flex-wrap gap-2 rounded-xl border border-line bg-white p-3">
        <div className="relative min-w-52 flex-1">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            className="pl-9"
            placeholder="Buscar colaborador, trámite o motivo"
            value={q}
            onChange={(e) => { setQ(e.target.value); setPage(1); }}
          />
        </div>
        <select value={tipo} onChange={(e) => { setTipo(e.target.value); setPage(1); }}>
          <option value="">Todos los tipos</option>
          <option value="PERMISO">Permiso</option>
          <option value="HORA_EXTRA">Horas extras</option>
        </select>
        {tab === 'resueltas' && (
          <select value={estadoResuelta} onChange={(e) => { setEstadoResuelta(e.target.value); setPage(1); }}>
            <option value="">Todos los estados</option>
            <option value="APROBADO">Aprobado</option>
            <option value="RECHAZADO">Rechazado</option>
            <option value="CANCELADO">Cancelado</option>
          </select>
        )}
      </div>

      {lista.length === 0 ? (
        <div className="rounded-xl border border-line bg-white">
          <Empty text={tab === 'pendientes' ? 'No tiene pasos por atender.' : 'No hay solicitudes resueltas en este filtro.'} />
        </div>
      ) : tab === 'pendientes' ? (
        <div className="space-y-3">
          {items.map((it) => (
            <article key={it.idPasoSolicitud} className="rounded-xl border border-line bg-white p-4">
              <div className="flex items-start gap-3">
                <input
                  type="checkbox"
                  checked={Boolean(checked[it.idPasoSolicitud])}
                  onChange={(e) => setChecked((c) => ({ ...c, [it.idPasoSolicitud]: e.target.checked }))}
                />
                <Avatar name={it.solicitante} />
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-start justify-between gap-2">
                    <div>
                      <p className="font-semibold text-navy">{it.solicitante}</p>
                      <p className="text-xs text-muted">{TIPO[it.tipoSolicitud]} · {it.tipoTramite} · #{it.idSolicitud}</p>
                    </div>
                    <span className="rounded-md bg-slate-100 px-2 py-0.5 text-xs font-medium text-navy">
                      Paso {it.numeroPaso}: {it.nombrePaso}
                    </span>
                  </div>
                  {it.motivo && <p className="mt-2 text-sm text-slate-600">{it.motivo}</p>}
                  <div className="mt-3 flex flex-wrap items-center justify-between gap-2">
                    <p className="text-xs text-muted">{fmtDateTime(it.fechaInicio)}</p>
                    <Button variant="secondary" onClick={() => navigate(`/bandeja/${it.idPasoSolicitud}`)}>
                      Revisar
                    </Button>
                  </div>
                </div>
              </div>
            </article>
          ))}
        </div>
      ) : (
        <div className="space-y-3">
          {resueltas.map((it) => (
            <article key={it.key} className="rounded-xl border border-line bg-white p-4">
              <div className="flex items-start justify-between gap-3">
                <div className="flex gap-3">
                  <Avatar name={it.solicitante} />
                  <div>
                    <p className="font-semibold text-navy">{it.solicitante}</p>
                    <p className="text-xs text-muted">{TIPO[it.tipoSolicitud]} · {it.tipoTramite} · #{it.idSolicitud}</p>
                    {it.motivo && <p className="mt-2 text-sm text-slate-600">{it.motivo}</p>}
                    <p className="mt-2 text-xs text-muted">{fmtDateTime(it.fecha)}</p>
                  </div>
                </div>
                <div className="flex flex-col items-end gap-2">
                  <Badge value={it.estado} />
                  <Button
                    variant="secondary"
                    onClick={() => navigate(`/bandeja/ver/${it.tipoSolicitud === 'HORA_EXTRA' ? 'hora-extra' : 'permiso'}/${it.idSolicitud}`)}
                  >
                    Ver
                  </Button>
                </div>
              </div>
            </article>
          ))}
        </div>
      )}

      <div className="mt-3 overflow-hidden rounded-xl border border-line bg-white">
        <Pager page={meta.page} totalPages={meta.totalPages} totalElements={meta.totalElements} size={meta.size} onPage={setPage} />
      </div>
    </div>
  );
}

function Kpi({ value, label, hint }) {
  return (
    <div className="rounded-xl border border-line bg-white p-4">
      <p className="text-2xl font-bold text-navy">{value}</p>
      <p className="mt-1 text-sm font-medium text-navy">{label}</p>
      <p className="text-xs text-muted">{hint}</p>
    </div>
  );
}
