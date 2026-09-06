import { useEffect, useState } from 'react';
import { AlertCircle, CheckCircle2, Clock3, Download, Filter, RefreshCw } from 'lucide-react';
import { emptyPage, http, PAGE_SIZE, pagePath, SELECT_SIZE } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Alert, Avatar, Badge, Button, Empty, Field, Pager, downloadBlob } from '../components/ui';

export function Bandeja() {
  const { usuario } = useAuth();
  const [items, setItems] = useState([]);
  const [permisos, setPermisos] = useState([]);
  const [meta, setMeta] = useState(emptyPage);
  const [page, setPage] = useState(1);
  const [bandejaTotal, setBandejaTotal] = useState(0);
  const [completadasMes, setCompletadasMes] = useState(0);
  const [detalle, setDetalle] = useState(null);
  const [sel, setSel] = useState(null);
  const [comentario, setComentario] = useState('Conforme');
  const [tab, setTab] = useState('pendientes');
  const [q, setQ] = useState('');
  const [tipo, setTipo] = useState('');
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');
  const [checked, setChecked] = useState({});

  async function loadCounts() {
    const now = new Date();
    const [b, aprobadas] = await Promise.all([
      http.page(pagePath('/api/bandeja', { page: 1, size: 1 })),
      http.page(pagePath('/api/permisos', { page: 1, size: SELECT_SIZE, estado: 'APROBADO' })).catch(() => emptyPage)
    ]);
    setBandejaTotal(b.totalElements || 0);
    const mes = (aprobadas.content || []).filter((p) => {
      if (!p.fechaCreacion) return false;
      const d = new Date(p.fechaCreacion);
      return d.getMonth() === now.getMonth() && d.getFullYear() === now.getFullYear();
    }).length;
    setCompletadasMes(mes);
  }

  async function load() {
    const pendientes = tab === 'pendientes' || tab === 'curso';
    if (pendientes) {
      const b = await http.page(pagePath('/api/bandeja', { page, size: PAGE_SIZE, q, tipo }));
      setItems(b.content);
      setMeta(b);
      if (!sel && b.content[0]) seleccionar(b.content[0]);
    } else {
      const estado = tab === 'aprobadas' ? 'APROBADO' : 'RECHAZADO,CANCELADO';
      const p = await http.page(pagePath('/api/permisos', { page, size: PAGE_SIZE, estado, q })).catch(() => emptyPage);
      setPermisos(p.content);
      setMeta(p);
    }
  }

  useEffect(() => { load().catch((e) => setError(e.message)); }, [page, tab, q, tipo]);
  useEffect(() => { loadCounts().catch(() => {}); }, []);

  async function seleccionar(it) {
    setSel(it);
    setComentario('Conforme');
    try {
      const path = it.tipoSolicitud === 'PERMISO' ? `/api/permisos/${it.idSolicitud}` : `/api/horas-extras/${it.idSolicitud}`;
      setDetalle(await http.get(path));
    } catch {
      setDetalle(null);
    }
  }

  async function decidir(accion, pasoId = sel?.idPasoSolicitud) {
    if (!pasoId) return;
    setError('');
    setOk('');
    try {
      await http.post(`/api/pasos/${pasoId}/${accion}`, { comentario });
      setOk(accion === 'aprobar' ? 'Paso aprobado' : 'Paso rechazado');
      setDetalle(null);
      setSel(null);
      setChecked({});
      await Promise.all([load(), loadCounts()]);
    } catch (e) {
      setError(e.message);
    }
  }

  async function masiva() {
    const ids = Object.entries(checked).filter(([, v]) => v).map(([id]) => Number(id));
    if (!ids.length) return;
    setError('');
    try {
      for (const id of ids) {
        await http.post(`/api/pasos/${id}/aprobar`, { comentario: 'Aprobación masiva' });
      }
      setOk(`${ids.length} paso(s) aprobado(s)`);
      setChecked({});
      await Promise.all([load(), loadCounts()]);
    } catch (e) {
      setError(e.message);
    }
  }

  function exportar() {
    const rows = [['Solicitante', 'Tipo', 'Paso', 'Motivo', 'Fecha']];
    filtered.forEach((it) => {
      rows.push([it.solicitante, `${it.tipoSolicitud} ${it.tipoTramite}`, `${it.numeroPaso} ${it.nombrePaso}`, it.motivo, it.fechaInicio || '']);
    });
    const csv = rows.map((r) => r.map((c) => `"${String(c).replaceAll('"', '""')}"`).join(';')).join('\n');
    downloadBlob(new Blob([csv], { type: 'text/csv;charset=utf-8' }), 'bandeja.csv');
  }

  const now = new Date();
  const filtered = tab === 'pendientes' || tab === 'curso' ? items : permisos;

  const tabs = [
    { id: 'pendientes', label: 'Pendientes', n: bandejaTotal },
    { id: 'curso', label: 'En curso', n: bandejaTotal },
    { id: 'aprobadas', label: 'Aprobadas' },
    { id: 'historial', label: 'Historial / Rechazadas' }
  ];

  return (
    <div>
      <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-500">Gestión de talento humano · Workflow</p>
      <h1 className="mt-1 text-3xl font-bold text-navy">Bandeja de aprobación</h1>
      <p className="mt-2 mb-5 text-sm text-muted">Pasos EN_CURSO asignados a su rol, jefatura o usuario.</p>

      <Alert>{error}</Alert>
      <Alert ok>{ok}</Alert>

      <div className="mb-5 grid gap-3 sm:grid-cols-3">
        <Kpi icon={<AlertCircle className="text-red-600" size={18} />} tone="bg-red-50" value={bandejaTotal} label="URGENTES" hint="Pendientes de su firma" />
        <Kpi icon={<CheckCircle2 className="text-sky-700" size={18} />} tone="bg-sky-50" value={completadasMes} label={now.toLocaleDateString('es-PE', { month: 'long' })} hint="Completadas este mes" />
        <Kpi icon={<Clock3 className="text-slate-600" size={18} />} tone="bg-slate-100" value={bandejaTotal ? '—' : '0'} label="Tiempo prom." hint="Respuesta del periodo" />
      </div>

      <div className="grid gap-5 xl:grid-cols-[1fr_320px]">
        <div>
          <div className="mb-3 flex flex-wrap items-center gap-2">
            {tabs.map((t) => (
              <button
                key={t.id}
                type="button"
                onClick={() => { setTab(t.id); setPage(1); }}
                className={`rounded-full px-3 py-1.5 text-sm ${tab === t.id ? 'bg-navy text-white' : 'bg-white text-slate-600 ring-1 ring-line'}`}
              >
                {t.label}
                {t.n != null && (
                  <span className={`ml-2 rounded-full px-1.5 text-xs ${tab === t.id ? 'bg-red-500 text-white' : 'bg-slate-100'}`}>{t.n}</span>
                )}
              </button>
            ))}
            <div className="ml-auto flex gap-2">
              <Button onClick={masiva} disabled={!Object.values(checked).some(Boolean)}>Aprobación masiva</Button>
              <Button variant="secondary" onClick={exportar}><Download size={14} /> Exportar</Button>
            </div>
          </div>

          <div className="mb-3 flex flex-wrap gap-2 rounded-xl border border-line bg-white p-3">
            <input className="min-w-52 flex-1" placeholder="Buscar colaborador..." value={q} onChange={(e) => { setQ(e.target.value); setPage(1); }} />
            <select value={tipo} onChange={(e) => { setTipo(e.target.value); setPage(1); }}>
              <option value="">Tipo de solicitud</option>
              <option value="PERMISO">Permiso</option>
              <option value="HORA_EXTRA">Hora extra</option>
            </select>
            <button type="button" className="rounded-lg border border-line px-3 text-slate-500"><Filter size={16} /></button>
          </div>

          {filtered.length === 0 ? (
            <div className="rounded-xl border border-line bg-white"><Empty text="No hay registros en esta vista." /></div>
          ) : tab === 'pendientes' || tab === 'curso' ? (
            <div className="space-y-3">
              <div className="hidden grid-cols-[auto_1.2fr_1fr_1fr] gap-3 px-2 text-[11px] font-semibold uppercase tracking-wide text-slate-400 md:grid">
                <span />
                <span>Solicitante</span>
                <span>Tipo y plazo</span>
                <span>Flujo de firmas</span>
              </div>
              {filtered.map((it) => (
                <article
                  key={it.idPasoSolicitud}
                  onClick={() => seleccionar(it)}
                  className={`cursor-pointer rounded-xl border bg-white p-4 shadow-sm ${sel?.idPasoSolicitud === it.idPasoSolicitud ? 'border-navy' : 'border-line'}`}
                >
                  <div className="grid items-start gap-3 md:grid-cols-[auto_1.2fr_1fr_1fr_auto]">
                    <input
                      type="checkbox"
                      checked={Boolean(checked[it.idPasoSolicitud])}
                      onChange={(e) => { e.stopPropagation(); setChecked((c) => ({ ...c, [it.idPasoSolicitud]: e.target.checked })); }}
                      onClick={(e) => e.stopPropagation()}
                    />
                    <div className="flex gap-3">
                      <Avatar name={it.solicitante} />
                      <div>
                        <p className="font-semibold text-navy">{it.solicitante}</p>
                        <p className="text-xs text-muted">REQ-{it.idSolicitud} · {it.tipoSolicitud}</p>
                      </div>
                    </div>
                    <div>
                      <span className="rounded-md bg-sky-50 px-2 py-0.5 text-xs font-medium text-sky-800">{it.tipoSolicitud} · {it.tipoTramite}</span>
                      {it.fechaInicio && <p className="mt-1 text-xs text-muted">{new Date(it.fechaInicio).toLocaleString('es-PE')}</p>}
                    </div>
                    <div>
                      <p className="text-sm font-medium">Paso {it.numeroPaso}: {it.nombrePaso}</p>
                      <p className="text-xs text-muted">Tú ({usuario?.nombreUsuario})</p>
                    </div>
                    <Button onClick={(e) => { e.stopPropagation(); seleccionar(it); }}>Revisar</Button>
                  </div>
                  {it.motivo && (
                    <div className="mt-3 rounded-lg border-l-4 border-sky-400 bg-sky-50 px-3 py-2 text-sm text-slate-700">
                      <span className="font-medium">Motivo declarado: </span>{it.motivo}
                    </div>
                  )}
                </article>
              ))}
            </div>
          ) : (
            <div className="space-y-2">
              {filtered.map((p) => (
                <div key={p.idSolicitudPermiso || p.idSolicitud} className="flex items-center justify-between rounded-xl border border-line bg-white p-4">
                  <div>
                    <p className="font-semibold text-navy">{p.empleado}</p>
                    <p className="text-sm text-muted">{p.tipoPermiso} · {p.fechaInicio} → {p.fechaFin}</p>
                  </div>
                  <Badge value={p.estado} />
                </div>
              ))}
            </div>
          )}

          <div className="mt-4 flex items-center gap-2 rounded-xl bg-sky-50 px-4 py-3 text-sm text-sky-900">
            <RefreshCw size={16} />
            Sincronización con asistencia. Las decisiones quedan en auditoría.
          </div>
          <div className="mt-3 overflow-hidden rounded-xl border border-line bg-white">
            <Pager page={meta.page} totalPages={meta.totalPages} totalElements={meta.totalElements} size={meta.size} onPage={setPage} />
          </div>
        </div>

        <aside className="space-y-4">
          <div className="rounded-xl border border-line bg-white p-5 shadow-sm">
            <p className="text-[11px] font-semibold uppercase tracking-wide text-slate-500">Flujo de firma seleccionada</p>
            {sel ? (
              <>
                <p className="mt-1 text-xs text-muted">REQ-{sel.idSolicitud}</p>
                <div className="mt-4 flex items-center gap-3">
                  <Avatar name={sel.solicitante} />
                  <div>
                    <p className="font-semibold text-navy">{sel.solicitante}</p>
                    <p className="text-xs text-muted">{sel.tipoTramite}</p>
                  </div>
                </div>
                <ol className="mt-5 space-y-4 border-l border-line pl-4">
                  {(detalle?.pasos || [{ numeroPaso: sel.numeroPaso, nombrePaso: sel.nombrePaso, estado: 'EN_CURSO' }]).map((p) => (
                    <li key={p.numeroPaso || p.idPasoSolicitud} className="relative">
                      <span className={`absolute -left-[21px] top-1 h-2.5 w-2.5 rounded-full ${p.estado === 'EN_CURSO' ? 'bg-navy' : p.estado === 'APROBADO' ? 'bg-ok' : 'bg-slate-300'}`} />
                      <div className="flex items-center gap-2">
                        <p className="text-sm font-medium">Paso {p.numeroPaso}: {p.nombrePaso}</p>
                        {p.estado === 'EN_CURSO' && <span className="rounded bg-red-100 px-1.5 py-0.5 text-[10px] font-semibold text-red-700">TU TURNO</span>}
                      </div>
                      <p className="text-xs text-muted">{p.rol || p.usuarioAsignado || p.tipoAprobador || 'Asignado'}</p>
                    </li>
                  ))}
                </ol>
                <Field label="Comentario">
                  <textarea className="mt-3" value={comentario} onChange={(e) => setComentario(e.target.value)} />
                </Field>
                <Button className="mt-3 w-full" onClick={() => decidir('aprobar')}>Aprobar paso inmediatamente</Button>
                <Button variant="danger" className="mt-2 w-full" onClick={() => decidir('rechazar')}>Rechazar u observar solicitud</Button>
              </>
            ) : (
              <p className="mt-3 text-sm text-muted">Seleccione una solicitud de la lista.</p>
            )}
          </div>
          <div className="rounded-xl border border-line bg-white p-5 shadow-sm">
            <p className="text-[11px] font-semibold uppercase tracking-wide text-slate-500">Resumen de carga</p>
            <p className="mt-3 text-sm text-muted">Pendientes en su mesa</p>
            <div className="mt-2 h-2 overflow-hidden rounded-full bg-slate-100">
              <div className="h-full bg-navy" style={{ width: `${Math.min(100, bandejaTotal * 20)}%` }} />
            </div>
            <p className="mt-2 text-xs text-muted">{bandejaTotal} en curso · {completadasMes} cerradas este mes</p>
          </div>
        </aside>
      </div>
    </div>
  );
}

function Kpi({ icon, tone, value, label, hint }) {
  return (
    <div className="flex items-center gap-3 rounded-xl border border-line bg-white p-4 shadow-sm">
      <div className={`grid h-10 w-10 place-items-center rounded-lg ${tone}`}>{icon}</div>
      <div>
        <p className="text-2xl font-bold text-navy">{value} <span className="text-sm font-medium uppercase">{label}</span></p>
        <p className="text-xs text-muted">{hint}</p>
      </div>
    </div>
  );
}
