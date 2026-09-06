import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { CalendarDays, Clock3, Download, Search, Timer } from 'lucide-react';
import { emptyPage, http, pagePath, SELECT_SIZE } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Alert, Avatar, Button, Empty, Pager, Panel, downloadBlob } from '../components/ui';

function sameMonth(iso, d = new Date()) {
  const x = new Date(iso);
  return x.getMonth() === d.getMonth() && x.getFullYear() === d.getFullYear();
}

function businessDaysSoFar(d = new Date()) {
  let n = 0;
  for (let day = 1; day <= d.getDate(); day += 1) {
    const w = new Date(d.getFullYear(), d.getMonth(), day).getDay();
    if (w !== 0 && w !== 6) n += 1;
  }
  return n;
}

function pairHours(list) {
  const byDay = {};
  list.forEach((r) => {
    const day = new Date(r.fechaHora).toISOString().slice(0, 10);
    byDay[day] = byDay[day] || {};
    byDay[day][r.tipo] = new Date(r.fechaHora);
  });
  let hours = 0;
  Object.values(byDay).forEach((p) => {
    if (p.INGRESO && p.SALIDA) hours += (p.SALIDA - p.INGRESO) / 36e5;
  });
  return Math.max(0, hours);
}

function formatStamp(iso) {
  if (!iso) return '—';
  const d = new Date(iso);
  return d.toLocaleString('es-PE', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit', second: '2-digit' });
}

export function Asistencia() {
  const { usuario, hasAnyRole } = useAuth();
  const canSupervise = hasAnyRole('ADMIN', 'RRHH');
  const [rows, setRows] = useState([]);
  const [mineRows, setMineRows] = useState([]);
  const [mineTotal, setMineTotal] = useState(0);
  const [equipoTotal, setEquipoTotal] = useState(0);
  const [meta, setMeta] = useState({ ...emptyPage, size: 7 });
  const [horario, setHorario] = useState('');
  const [error, setError] = useState('');
  const [tab, setTab] = useState('mias');
  const [tipoFiltro, setTipoFiltro] = useState('');
  const [q, setQ] = useState('');
  const [page, setPage] = useState(1);
  const pageSize = 7;

  function monthRange() {
    const d = new Date();
    const desde = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-01`;
    const hasta = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
    return { desde, hasta };
  }

  async function loadMetrics() {
    const { desde, hasta } = monthRange();
    const mine = await http.page(pagePath('/api/asistencias', {
      desde, hasta, page: 1, size: SELECT_SIZE, idEmpleado: usuario?.idEmpleado
    }));
    setMineRows(mine.content);
    setMineTotal(mine.totalElements);
    if (canSupervise) {
      const team = await http.page(pagePath('/api/asistencias', { desde, hasta, page: 1, size: 1 }));
      setEquipoTotal(team.totalElements || 0);
    }
    if (usuario?.idEmpleado) {
      try {
        const emp = await http.get(`/api/empleados/${usuario.idEmpleado}`);
        setHorario(emp.horario || '');
      } catch {
        setHorario('');
      }
    }
  }

  async function loadTable() {
    const { desde, hasta } = monthRange();
    const data = await http.page(pagePath('/api/asistencias', {
      desde,
      hasta,
      page,
      size: pageSize,
      tipo: tipoFiltro,
      q,
      idEmpleado: tab === 'mias' ? usuario?.idEmpleado : undefined
    }));
    setRows(data.content);
    setMeta(data);
  }

  useEffect(() => { loadMetrics().catch((e) => setError(e.message)); }, []);
  useEffect(() => { loadTable().catch((e) => setError(e.message)); }, [page, tab, tipoFiltro, q]);

  const monthMine = mineRows.filter((r) => sameMonth(r.fechaHora));
  const dias = new Set(monthMine.filter((r) => r.tipo === 'INGRESO').map((r) => new Date(r.fechaHora).toDateString())).size;
  const habiles = businessDaysSoFar();
  const horas = pairHours(monthMine);
  const metaHoras = habiles * 8;
  const tardanzas = monthMine.filter((r) => {
    if (r.tipo !== 'INGRESO') return false;
    const t = new Date(r.fechaHora);
    return t.getHours() > 8 || (t.getHours() === 8 && t.getMinutes() > 30);
  }).length;

  async function exportar() {
    try {
      const blob = await http.download('/api/reportes/ASISTENCIA/excel');
      downloadBlob(blob, 'asistencia.xlsx');
    } catch (e) {
      setError(e.message);
    }
  }

  const periodo = useMemo(() => new Date().toLocaleDateString('es-PE', { month: 'long', year: 'numeric' }), []);
  const ua = typeof navigator !== 'undefined' ? navigator.userAgent : 'WEB';
  const browser = /Edg\//.test(ua) ? 'Edge' : /Chrome\//.test(ua) ? 'Chrome' : /Firefox\//.test(ua) ? 'Firefox' : 'Navegador';

  return (
    <div>
      <div className="mb-5 flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
        <div>
          <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-500">
            Presencia y control horario · Consultora Contable Andina
          </p>
          <div className="mt-1 flex flex-wrap items-center gap-2">
            <h1 className="text-3xl font-bold text-navy">Asistencia</h1>
            <span className="rounded-full bg-sky-50 px-2.5 py-1 text-xs font-medium capitalize text-sky-800">
              Periodo {periodo}
            </span>
          </div>
          <p className="mt-2 text-sm text-muted">Consulta de entrada y salida. La marcación se hace en el apartado Marcar.</p>
        </div>
        <Link to="/marcar">
          <Button>Ir a marcar</Button>
        </Link>
      </div>

      <Alert>{error}</Alert>

      <div className="mb-5 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
          <Metric icon={<CalendarDays size={16} />} label="Días asistidos" value={`${dias} / ${habiles}`} hint="días hábiles del mes" pct={habiles ? (dias / habiles) * 100 : 0} />
          <Metric icon={<Clock3 size={16} />} label="Horas efectivas" value={`${horas.toFixed(1)} hrs`} hint={`meta aprox. ${metaHoras} hrs`} pct={metaHoras ? (horas / metaHoras) * 100 : 0} />
          <Metric icon={<Timer size={16} />} label="Tardanzas mes" value={`${tardanzas}`} hint={tardanzas === 0 ? 'Sin tardanzas detectadas' : 'Ingresos después de 08:30'} />
          <Panel>
            <p className="text-xs text-muted">Jornada asignada</p>
            <p className="mt-1 text-sm font-semibold text-navy">{horario || 'Horario de la ficha de personal'}</p>
            <p className="mt-1 text-xs text-muted">Lunes a viernes · tolerancia según política interna</p>
          </Panel>
      </div>

      <div className="mb-3 flex flex-wrap items-center gap-2">
        <button type="button" onClick={() => { setTab('mias'); setPage(1); }} className={`rounded-full px-3 py-1.5 text-sm ${tab === 'mias' ? 'bg-navy text-white' : 'bg-white text-slate-600 ring-1 ring-line'}`}>
          Mis marcaciones ({mineTotal})
        </button>
        {canSupervise && (
          <button type="button" onClick={() => { setTab('equipo'); setPage(1); }} className={`rounded-full px-3 py-1.5 text-sm ${tab === 'equipo' ? 'bg-navy text-white' : 'bg-white text-slate-600 ring-1 ring-line'}`}>
            Supervisión de equipo ({equipoTotal})
          </button>
        )}
        <div className="ml-auto flex flex-wrap gap-2">
          <select value={tipoFiltro} onChange={(e) => { setTipoFiltro(e.target.value); setPage(1); }}>
            <option value="">Tipo</option>
            <option value="INGRESO">Entrada</option>
            <option value="SALIDA">Salida</option>
          </select>
          <div className="relative">
            <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
            <input className="pl-8" placeholder="Buscar colaborador..." value={q} onChange={(e) => { setQ(e.target.value); setPage(1); }} />
          </div>
          {canSupervise && <Button variant="secondary" onClick={exportar}><Download size={14} /> Exportar reporte</Button>}
        </div>
      </div>

      <Panel padded={false}>
        {rows.length === 0 ? <Empty text="Sin marcaciones en esta vista." /> : (
          <div className="overflow-x-auto">
            <table>
              <thead>
                <tr>
                  <th>Colaborador</th>
                  <th>Tipo</th>
                  <th>Fecha y hora</th>
                  <th>Origen</th>
                  <th>Estado</th>
                  <th>Observación</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((r) => (
                  <tr key={r.idMarcacion}>
                    <td>
                      <div className="flex items-center gap-3">
                        <Avatar name={r.empleado} />
                        <div>
                          <p className="font-medium text-navy">{r.empleado}</p>
                          <p className="text-xs text-muted">#{r.idEmpleado} · {r.tipo}</p>
                        </div>
                      </div>
                    </td>
                    <td>
                      <span className={`rounded-md px-2 py-0.5 text-xs font-semibold ${r.tipo === 'INGRESO' ? 'bg-sky-50 text-sky-800' : 'bg-slate-100 text-slate-700'}`}>
                        {r.tipo}
                      </span>
                    </td>
                    <td>
                      <p>{formatStamp(r.fechaHora)}</p>
                      <p className="text-xs text-muted">Hora del registro</p>
                    </td>
                    <td>
                      <p className="text-sm">{r.origen || 'WEB'}</p>
                      <p className="text-xs text-muted">{browser}</p>
                    </td>
                    <td>
                      <span className="rounded-full bg-sky-50 px-2 py-0.5 text-xs font-medium text-sky-800">
                        {r.tipo === 'INGRESO' ? 'Entrada registrada' : 'Salida registrada'}
                      </span>
                    </td>
                    <td className="text-sm text-muted">{r.observacion || 'Jornada ordinaria'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        <Pager page={meta.page} totalPages={meta.totalPages} totalElements={meta.totalElements} size={meta.size} onPage={setPage} />
      </Panel>
    </div>
  );
}

function Metric({ icon, label, value, hint, pct }) {
  return (
    <Panel>
      <div className="flex items-start justify-between">
        <p className="text-xs text-muted">{label}</p>
        <span className="text-slate-400">{icon}</span>
      </div>
      <p className="mt-1 text-2xl font-bold text-navy">{value}</p>
      <p className="text-xs text-muted">{hint}</p>
      {pct != null && (
        <div className="mt-2 h-1.5 overflow-hidden rounded-full bg-slate-100">
          <div className="h-full bg-navy" style={{ width: `${Math.min(100, Math.round(pct))}%` }} />
        </div>
      )}
    </Panel>
  );
}
