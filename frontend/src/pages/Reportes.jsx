import { useEffect, useState } from 'react';
import { Download } from 'lucide-react';
import { http, pagePath, SELECT_SIZE } from '../api/client';
import { Alert, Avatar, Button, DatePicker, Empty, FilterBar, PageHeader, Panel, SearchField, StackTable, downloadBlob } from '../components/ui';

const TIPOS = [
  { id: 'ASISTENCIA', label: 'Asistencia', hint: 'Todas las marcaciones de ingreso y salida' },
  { id: 'TRABAJADORES', label: 'Trabajadores', hint: 'Directorio con tipo de contrato' },
  { id: 'PERMISOS', label: 'Permisos', hint: 'Solicitudes de permiso y vacaciones' },
  { id: 'HORAS_EXTRAS', label: 'Horas extras', hint: 'Tiempo extra registrado' },
  { id: 'USUARIOS', label: 'Usuarios', hint: 'Cuentas de acceso' }
];

function monthStart() {
  const d = new Date();
  return `${d.getFullYear()}-01-01`;
}

function todayIso() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

function formatStamp(iso) {
  if (!iso) return '—';
  return new Date(iso).toLocaleString('es-PE', {
    day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit', second: '2-digit'
  });
}

export function Reportes() {
  const [error, setError] = useState('');
  const [empleados, setEmpleados] = useState([]);
  const [rows, setRows] = useState([]);
  const [idEmpleado, setIdEmpleado] = useState('');
  const [desde, setDesde] = useState(monthStart());
  const [hasta, setHasta] = useState(todayIso());
  const [tipo, setTipo] = useState('');
  const [q, setQ] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    http.page(pagePath('/api/empleados', { page: 1, size: SELECT_SIZE }))
      .then((data) => setEmpleados(data.content || []))
      .catch((e) => setError(e.message));
  }, []);

  async function loadAsistencias() {
    setError('');
    setLoading(true);
    try {
      const data = await http.get(pagePath('/api/reportes/asistencia', {
        idEmpleado: idEmpleado || undefined,
        desde,
        hasta
      }));
      setRows(Array.isArray(data) ? data : []);
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadAsistencias();
  }, [idEmpleado, desde, hasta]);

  const visibles = rows.filter((r) => {
    if (tipo && r.tipo !== tipo) return false;
    if (!q.trim()) return true;
    const hay = `${r.empleado || ''} ${r.origen || ''} ${r.observacion || ''}`.toLowerCase();
    return hay.includes(q.trim().toLowerCase());
  });

  async function bajar(tipoRep, formato) {
    setError('');
    try {
      const blob = await http.download(`/api/reportes/${tipoRep}/${formato}`);
      downloadBlob(blob, `${tipoRep.toLowerCase()}.${formato === 'excel' ? 'xlsx' : 'pdf'}`);
    } catch (e) {
      setError(e.message);
    }
  }

  return (
    <div>
      <PageHeader
        kicker="Control"
        title="Reportes"
        subtitle="Consulta de asistencias de todos los trabajadores y exportación Excel/PDF."
      />
      <Alert>{error}</Alert>

      <section className="mb-8">
        <div className="mb-4 flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <h2 className="text-base font-semibold text-navy">Asistencias de trabajadores</h2>
            <p className="text-sm text-muted">Ingresos y salidas del periodo. {visibles.length} registro{visibles.length === 1 ? '' : 's'}.</p>
          </div>
          <Button variant="secondary" onClick={() => bajar('ASISTENCIA', 'excel')}>
            <Download size={14} /> Excel
          </Button>
        </div>

        <FilterBar>
          <select className="w-auto" value={idEmpleado} onChange={(e) => setIdEmpleado(e.target.value)}>
            <option value="">Todos los trabajadores</option>
            {empleados.map((e) => (
              <option key={e.idEmpleado} value={e.idEmpleado}>{e.nombreCompleto}</option>
            ))}
          </select>
          <DatePicker value={desde} onChange={setDesde} />
          <DatePicker value={hasta} onChange={setHasta} min={desde || undefined} />
          <select className="w-auto" value={tipo} onChange={(e) => setTipo(e.target.value)}>
            <option value="">Tipo</option>
            <option value="INGRESO">Ingreso</option>
            <option value="SALIDA">Salida</option>
          </select>
          <SearchField placeholder="Buscar trabajador u origen" value={q} onChange={(e) => setQ(e.target.value)} />
        </FilterBar>

        <Panel padded={false}>
          {loading ? (
            <Empty text="Cargando marcaciones…" />
          ) : visibles.length === 0 ? (
            <Empty text="No hay asistencias en este periodo." />
          ) : (
            <StackTable
              cards={visibles.map((r) => (
                <div key={r.idMarcacion} className="flex gap-3 px-4 py-3">
                  <Avatar name={r.empleado} />
                  <div className="min-w-0">
                    <p className="font-medium text-navy">{r.empleado}</p>
                    <p className="text-xs text-muted">{formatStamp(r.fechaHora)}</p>
                    <div className="mt-2 flex flex-wrap gap-2">
                      <span className={`rounded-md px-2 py-0.5 text-xs font-semibold ${r.tipo === 'INGRESO' ? 'bg-sky-50 text-sky-800' : 'bg-slate-100 text-slate-700'}`}>
                        {r.tipo}
                      </span>
                      <span className="text-xs text-muted">{r.origen || 'WEB'}</span>
                    </div>
                  </div>
                </div>
              ))}
              table={(
                <table>
                  <thead>
                    <tr>
                      <th>Trabajador</th>
                      <th>Tipo</th>
                      <th>Fecha y hora</th>
                      <th>Origen</th>
                      <th>Observación</th>
                    </tr>
                  </thead>
                  <tbody>
                    {visibles.map((r) => (
                      <tr key={r.idMarcacion}>
                        <td>
                          <div className="flex items-center gap-3">
                            <Avatar name={r.empleado} />
                            <div>
                              <p className="font-medium text-navy">{r.empleado}</p>
                              <p className="text-xs text-muted">#{r.idEmpleado}</p>
                            </div>
                          </div>
                        </td>
                        <td>
                          <span className={`rounded-md px-2 py-0.5 text-xs font-semibold ${r.tipo === 'INGRESO' ? 'bg-sky-50 text-sky-800' : 'bg-slate-100 text-slate-700'}`}>
                            {r.tipo}
                          </span>
                        </td>
                        <td>{formatStamp(r.fechaHora)}</td>
                        <td>{r.origen || 'WEB'}</td>
                        <td className="text-sm text-muted">{r.observacion || '—'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            />
          )}
        </Panel>
      </section>

      <h2 className="mb-3 text-base font-semibold text-navy">Exportar</h2>
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {TIPOS.map((t) => (
          <Panel key={t.id}>
            <h3 className="text-base font-semibold text-navy">{t.label}</h3>
            <p className="mt-1 text-sm text-muted">{t.hint}</p>
            <div className="mt-3 flex flex-wrap gap-2">
              <Button onClick={() => bajar(t.id, 'excel')}>Excel</Button>
              <Button variant="secondary" onClick={() => bajar(t.id, 'pdf')}>PDF</Button>
            </div>
          </Panel>
        ))}
      </div>
    </div>
  );
}
