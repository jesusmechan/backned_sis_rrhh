import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Download } from 'lucide-react';
import { emptyPage, http, PAGE_SIZE, pagePath } from '../api/client';
import { Alert, Avatar, Badge, Button, Empty, FilterBar, Kpi, KpiRow, Pager, SearchField, downloadBlob } from '../components/ui';
import { TIPO, fmtDateTime } from './bandejaShared';

export function Bandeja() {
  const navigate = useNavigate();
  const location = useLocation();
  const [tab, setTab] = useState(location.state?.tab === 'seguimiento' ? 'seguimiento' : 'pendientes');
  const [items, setItems] = useState([]);
  const [seguimiento, setSeguimiento] = useState([]);
  const [meta, setMeta] = useState(emptyPage);
  const [page, setPage] = useState(1);
  const [counts, setCounts] = useState({ total: 0, permiso: 0, hora: 0, seguimiento: 0 });
  const [q, setQ] = useState('');
  const [qDebounced, setQDebounced] = useState('');
  const [tipo, setTipo] = useState('');
  const [estadoSeguimiento, setEstadoSeguimiento] = useState('');
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
    const [perm, hex, seg] = await Promise.all([
      http.page(pagePath('/api/bandeja', { page: 1, size: 1, tipo: 'PERMISO' })),
      http.page(pagePath('/api/bandeja', { page: 1, size: 1, tipo: 'HORA_EXTRA' })),
      http.page(pagePath('/api/bandeja', { page: 1, size: 1, vista: 'SEGUIMIENTO' }))
    ]);
    setCounts({
      permiso: perm.totalElements || 0,
      hora: hex.totalElements || 0,
      total: (perm.totalElements || 0) + (hex.totalElements || 0),
      seguimiento: seg.totalElements || 0
    });
  }

  async function loadPendientes() {
    const b = await http.page(pagePath('/api/bandeja', { page, size: PAGE_SIZE, q: qDebounced, tipo }));
    setItems(b.content || []);
    setMeta(b);
  }

  async function loadSeguimiento() {
    const b = await http.page(pagePath('/api/bandeja', {
      page,
      size: PAGE_SIZE,
      q: qDebounced,
      tipo,
      vista: 'SEGUIMIENTO',
      estado: estadoSeguimiento
    }));
    setSeguimiento(b.content || []);
    setMeta(b);
  }

  useEffect(() => {
    let cancelled = false;
    setError('');
    const run = tab === 'pendientes' ? loadPendientes : loadSeguimiento;
    run().catch((e) => { if (!cancelled) setError(e.message); });
    return () => { cancelled = true; };
  }, [page, tab, qDebounced, tipo, estadoSeguimiento]);

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
      : [['Solicitante', 'Tipo', 'Trámite', 'Estado', 'Paso', 'Motivo', 'Fecha']].concat(
        seguimiento.map((it) => [it.solicitante, TIPO[it.tipoSolicitud] || it.tipoSolicitud, it.tipoTramite, it.estadoSolicitud, it.nombrePaso, it.motivo, it.fechaInicio || ''])
      );
    const csv = rows.map((r) => r.map((c) => `"${String(c ?? '').replaceAll('"', '""')}"`).join(';')).join('\n');
    downloadBlob(new Blob([csv], { type: 'text/csv;charset=utf-8' }), `bandeja-${tab}.csv`);
  }

  const lista = tab === 'pendientes' ? items : seguimiento;
  const seleccionados = Object.values(checked).filter(Boolean).length;

  return (
    <div>
      <div className="mb-6 border-b border-line pb-5">
        <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-500">Aprobaciones</p>
        <h1 className="page-title mt-1">Bandeja</h1>
        <p className="mt-2 text-sm text-muted">Atienda los pasos que le corresponden y siga las solicitudes en las que ya participó o que usted creó.</p>
      </div>

      <Alert>{error}</Alert>
      <Alert ok>{ok}</Alert>

      <KpiRow>
        <Kpi
          value={counts.total}
          label="Por atender"
          hint="Pasos que esperan su decisión"
          active={tab === 'pendientes'}
          onClick={() => { setTab('pendientes'); setPage(1); setChecked({}); }}
        />
        <Kpi
          value={counts.seguimiento}
          label="En seguimiento"
          hint="Creadas o ya decididas por usted"
          active={tab === 'seguimiento'}
          onClick={() => { setTab('seguimiento'); setPage(1); setChecked({}); }}
        />
        <Kpi value={counts.permiso + counts.hora} label="Pendientes por tipo" hint={`${counts.permiso} permisos · ${counts.hora} horas extras`} />
      </KpiRow>

      <FilterBar>
        <SearchField placeholder="Buscar colaborador, trámite o motivo" value={q} onChange={(e) => { setQ(e.target.value); setPage(1); }} />
        <select className="w-auto" value={tipo} onChange={(e) => { setTipo(e.target.value); setPage(1); }}>
          <option value="">Todos los tipos</option>
          <option value="PERMISO">Permiso</option>
          <option value="HORA_EXTRA">Horas extras</option>
        </select>
        {tab === 'seguimiento' && (
          <select className="w-auto" value={estadoSeguimiento} onChange={(e) => { setEstadoSeguimiento(e.target.value); setPage(1); }}>
            <option value="">Todos los estados</option>
            <option value="PENDIENTE">Pendiente</option>
            <option value="APROBADO">Aprobado</option>
            <option value="RECHAZADO">Rechazado</option>
            <option value="CANCELADO">Cancelado</option>
          </select>
        )}
        {tab === 'pendientes' && (
          <Button onClick={masiva} disabled={saving || !seleccionados}>
            Aprobar seleccionadas{seleccionados ? ` (${seleccionados})` : ''}
          </Button>
        )}
        <Button variant="secondary" onClick={exportar}><Download size={14} /> Exportar</Button>
      </FilterBar>

      {lista.length === 0 ? (
        <div className="rounded-xl border border-line bg-white">
          <Empty text={tab === 'pendientes' ? 'No tiene pasos por atender.' : 'No hay solicitudes en seguimiento con este filtro.'} />
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
          {seguimiento.map((it) => (
            <article key={`${it.tipoSolicitud}-${it.idSolicitud}`} className="rounded-xl border border-line bg-white p-4">
              <div className="flex items-start justify-between gap-3">
                <div className="flex gap-3">
                  <Avatar name={it.solicitante} />
                  <div>
                    <p className="font-semibold text-navy">{it.solicitante}</p>
                    <p className="text-xs text-muted">{TIPO[it.tipoSolicitud]} · {it.tipoTramite} · #{it.idSolicitud}</p>
                    {it.motivo && <p className="mt-2 text-sm text-slate-600">{it.motivo}</p>}
                    <p className="mt-2 text-xs text-muted">{fmtDateTime(it.fechaInicio)}</p>
                  </div>
                </div>
                <div className="flex flex-col items-end gap-2">
                  <Badge value={it.estadoSolicitud} />
                  {it.nombrePaso && (
                    <span className="rounded-md bg-slate-100 px-2 py-0.5 text-xs font-medium text-navy">
                      {it.estadoSolicitud === 'PENDIENTE' ? 'En curso' : 'Último paso'} {it.numeroPaso}: {it.nombrePaso}
                    </span>
                  )}
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
