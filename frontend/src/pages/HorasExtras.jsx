import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Plus, Search } from 'lucide-react';
import { emptyPage, http, PAGE_SIZE, pagePath } from '../api/client';
import { Alert, Avatar, Badge, Button, Empty, Pager } from '../components/ui';
import { fmtDate, fmtTime } from './bandejaShared';

export function HorasExtras() {
  const navigate = useNavigate();
  const location = useLocation();
  const [rows, setRows] = useState([]);
  const [meta, setMeta] = useState(emptyPage);
  const [page, setPage] = useState(1);
  const [counts, setCounts] = useState({ total: 0, pendientes: 0, aprobados: 0, rechazados: 0 });
  const [tab, setTab] = useState('todas');
  const [q, setQ] = useState('');
  const [qDebounced, setQDebounced] = useState('');
  const [error, setError] = useState('');
  const [ok, setOk] = useState(location.state?.ok || '');

  useEffect(() => {
    const t = setTimeout(() => setQDebounced(q.trim()), 300);
    return () => clearTimeout(t);
  }, [q]);

  useEffect(() => {
    if (location.state?.ok) navigate(location.pathname, { replace: true, state: {} });
  }, []);

  async function loadCounts() {
    const [all, pend, apr, rec] = await Promise.all([
      http.page(pagePath('/api/horas-extras', { page: 1, size: 1 })),
      http.page(pagePath('/api/horas-extras', { page: 1, size: 1, estado: 'PENDIENTE' })),
      http.page(pagePath('/api/horas-extras', { page: 1, size: 1, estado: 'APROBADO' })),
      http.page(pagePath('/api/horas-extras', { page: 1, size: 1, estado: 'RECHAZADO' }))
    ]);
    setCounts({
      total: all.totalElements || 0,
      pendientes: pend.totalElements || 0,
      aprobados: apr.totalElements || 0,
      rechazados: rec.totalElements || 0
    });
  }

  async function load() {
    const estado = tab === 'todas' ? '' : tab;
    const data = await http.page(pagePath('/api/horas-extras', { page, size: PAGE_SIZE, estado, q: qDebounced }));
    setRows(data.content || []);
    setMeta(data);
  }

  useEffect(() => {
    load().catch((e) => setError(e.message));
  }, [page, tab, qDebounced]);

  useEffect(() => { loadCounts().catch(() => {}); }, []);

  return (
    <div>
      <div className="mb-6 flex flex-col gap-4 border-b border-line pb-5 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-500">Trámites</p>
          <h1 className="page-title mt-1">Horas extras</h1>
          <p className="mt-2 text-sm text-muted">El aprobador lo define el flujo configurado. El tope diario y semanal lo valida el registro.</p>
        </div>
        <Button onClick={() => navigate('/horas-extras/nuevo')}><Plus size={16} /> Nueva solicitud</Button>
      </div>

      <Alert>{error}</Alert>
      <Alert ok>{ok}</Alert>

      <div className="mb-5 grid gap-3 sm:grid-cols-3">
        <Kpi value={counts.total} label="Total" hint="Solicitudes visibles" />
        <Kpi value={counts.pendientes} label="Pendientes" hint="En circuito de aprobación" />
        <Kpi value={counts.aprobados} label="Aprobadas" hint="Flujo cerrado a favor" />
      </div>

      <div className="mb-3 flex flex-col gap-3 sm:flex-row sm:items-center">
        <div className="flex flex-wrap gap-2">
          {[
            ['todas', 'Todas', counts.total],
            ['PENDIENTE', 'Pendientes', counts.pendientes],
            ['APROBADO', 'Aprobadas', counts.aprobados],
            ['RECHAZADO', 'Rechazadas', counts.rechazados],
            ['CANCELADO', 'Canceladas', null]
          ].map(([id, label, n]) => (
            <button
              key={id}
              type="button"
              onClick={() => { setTab(id); setPage(1); }}
              className={`rounded-lg px-3 py-1.5 text-sm ${tab === id ? 'bg-navy text-white' : 'border border-line bg-white text-slate-600 hover:bg-slate-50'}`}
            >
              {label}{n != null ? ` (${n})` : ''}
            </button>
          ))}
        </div>
        <div className="relative flex-1">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            className="pl-9"
            placeholder="Buscar colaborador o motivo"
            value={q}
            onChange={(e) => { setQ(e.target.value); setPage(1); }}
          />
        </div>
      </div>

      {rows.length === 0 ? (
        <div className="rounded-xl border border-line bg-white">
          <Empty text="No hay solicitudes en este filtro." />
        </div>
      ) : (
        <div className="space-y-3">
          {rows.map((r) => (
            <article key={r.idSolicitudHoraExtra} className="rounded-xl border border-line bg-white p-4">
              <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                <div className="flex min-w-0 gap-3">
                  <Avatar name={r.empleado} />
                  <div className="min-w-0">
                    <p className="font-semibold text-navy">{r.empleado}</p>
                    <p className="text-xs text-muted">Horas extras · #{r.idSolicitudHoraExtra}</p>
                    <p className="mt-2 text-sm text-slate-600">
                      {fmtDate(r.fecha)} · {fmtTime(r.horaInicio)} – {fmtTime(r.horaFin)} · {r.cantidadHoras} h
                    </p>
                    {r.motivo && <p className="mt-1 text-sm text-slate-600">{r.motivo}</p>}
                  </div>
                </div>
                <div className="flex shrink-0 flex-col items-start gap-2 sm:items-end">
                  <Badge value={r.estado} />
                  <Button variant="secondary" onClick={() => navigate(`/horas-extras/${r.idSolicitudHoraExtra}`)}>
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
