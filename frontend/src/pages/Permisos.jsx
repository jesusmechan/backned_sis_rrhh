import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Plus } from 'lucide-react';
import { emptyPage, http, PAGE_SIZE, pagePath } from '../api/client';
import { Alert, Avatar, Badge, Button, Empty, FilterBar, Kpi, KpiRow, Pager, SearchField } from '../components/ui';
import { fmtDate, fmtTime } from './bandejaShared';

export function Permisos() {
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
      http.page(pagePath('/api/permisos', { page: 1, size: 1 })),
      http.page(pagePath('/api/permisos', { page: 1, size: 1, estado: 'PENDIENTE' })),
      http.page(pagePath('/api/permisos', { page: 1, size: 1, estado: 'APROBADO' })),
      http.page(pagePath('/api/permisos', { page: 1, size: 1, estado: 'RECHAZADO' }))
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
    const data = await http.page(pagePath('/api/permisos', { page, size: PAGE_SIZE, estado, q: qDebounced }));
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
          <h1 className="page-title mt-1">Permisos</h1>
          <p className="mt-2 text-sm text-muted">El aprobador lo define el flujo configurado, no se elige al registrar.</p>
        </div>
        <Button onClick={() => navigate('/permisos/nuevo')}><Plus size={16} /> Nueva solicitud</Button>
      </div>

      <Alert>{error}</Alert>
      <Alert ok>{ok}</Alert>

      <KpiRow>
        <Kpi value={counts.total} label="Total" hint="Solicitudes visibles" active={tab === 'todas'} onClick={() => { setTab('todas'); setPage(1); }} />
        <Kpi value={counts.pendientes} label="Pendientes" hint="En circuito de aprobación" active={tab === 'PENDIENTE'} onClick={() => { setTab('PENDIENTE'); setPage(1); }} />
        <Kpi value={counts.aprobados} label="Aprobadas" hint="Flujo cerrado a favor" active={tab === 'APROBADO'} onClick={() => { setTab('APROBADO'); setPage(1); }} />
      </KpiRow>

      <FilterBar>
        <SearchField placeholder="Buscar colaborador, tipo o motivo" value={q} onChange={(e) => { setQ(e.target.value); setPage(1); }} />
        <select
          className="w-auto"
          value={tab === 'RECHAZADO' || tab === 'CANCELADO' ? tab : ''}
          onChange={(e) => { setTab(e.target.value || 'todas'); setPage(1); }}
        >
          <option value="">Más estados</option>
          <option value="RECHAZADO">Rechazadas ({counts.rechazados})</option>
          <option value="CANCELADO">Canceladas</option>
        </select>
      </FilterBar>

      {rows.length === 0 ? (
        <div className="rounded-xl border border-line bg-white">
          <Empty text="No hay solicitudes en este filtro." />
        </div>
      ) : (
        <div className="space-y-3">
          {rows.map((r) => (
            <article key={r.idSolicitudPermiso} className="rounded-xl border border-line bg-white p-4">
              <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                <div className="flex min-w-0 gap-3">
                  <Avatar name={r.empleado} />
                  <div className="min-w-0">
                    <p className="font-semibold text-navy">{r.empleado}</p>
                    <p className="text-xs text-muted">{r.tipoPermiso} · #{r.idSolicitudPermiso}</p>
                    <p className="mt-2 text-sm text-slate-600">
                      {fmtDate(r.fechaInicio)} → {fmtDate(r.fechaFin)}
                      {r.horaInicio ? ` · ${fmtTime(r.horaInicio)} – ${fmtTime(r.horaFin)}` : ''}
                    </p>
                    {r.motivo && <p className="mt-1 text-sm text-slate-600">{r.motivo}</p>}
                  </div>
                </div>
                <div className="flex shrink-0 flex-col items-start gap-2 sm:items-end">
                  <Badge value={r.estado} />
                  <Button variant="secondary" onClick={() => navigate(`/permisos/${r.idSolicitudPermiso}`)}>
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
