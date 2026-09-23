import { useEffect, useState } from 'react';
import { Download } from 'lucide-react';
import { http, pagePath, SELECT_SIZE } from '../api/client';
import { Alert, Avatar, Badge, Button, DatePicker, Empty, FilterBar, PageHeader, Panel, SearchField, StackTable, downloadBlob } from '../components/ui';
import { useQuerySearch } from '../lib/useQuerySearch';
import { fmtDate, fmtTime } from './bandejaShared';

const TIPOS = [
  { id: 'ASISTENCIA', label: 'Asistencia', hint: 'Todas las marcaciones de ingreso y salida' },
  { id: 'TRABAJADORES', label: 'Trabajadores', hint: 'Directorio con tipo de contrato' },
  { id: 'PERMISOS', label: 'Permisos', hint: 'Solicitudes de permiso y vacaciones' },
  { id: 'HORAS_EXTRAS', label: 'Horas extras', hint: 'Tiempo extra registrado' },
  { id: 'USUARIOS', label: 'Usuarios', hint: 'Cuentas de acceso' }
];

const PATHS = {
  ASISTENCIA: '/api/reportes/asistencia',
  TRABAJADORES: '/api/reportes/trabajadores',
  PERMISOS: '/api/reportes/permisos',
  HORAS_EXTRAS: '/api/reportes/horas-extras',
  USUARIOS: '/api/reportes/usuarios'
};

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

function haystack(vista, r) {
  if (vista === 'ASISTENCIA') return `${r.empleado || ''} ${r.origen || ''} ${r.observacion || ''}`;
  if (vista === 'TRABAJADORES') return `${r.nombreCompleto || ''} ${r.codigoEmpleado || ''} ${r.area || ''} ${r.cargo || ''}`;
  if (vista === 'PERMISOS') return `${r.empleado || ''} ${r.tipoPermiso || ''} ${r.motivo || ''} ${r.estado || ''}`;
  if (vista === 'HORAS_EXTRAS') return `${r.empleado || ''} ${r.motivo || ''} ${r.estado || ''}`;
  return `${r.nombreUsuario || ''} ${r.nombreCompleto || ''} ${r.correo || ''} ${r.perfil || ''} ${r.rol || ''}`;
}

export function Reportes() {
  const [error, setError] = useState('');
  const [empleados, setEmpleados] = useState([]);
  const [rows, setRows] = useState([]);
  const [idEmpleado, setIdEmpleado] = useState('');
  const [desde, setDesde] = useState(monthStart());
  const [hasta, setHasta] = useState(todayIso());
  const [tipo, setTipo] = useState('');
  const [vista, setVista] = useState('ASISTENCIA');
  const [q, setQ] = useQuerySearch();
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    http.page(pagePath('/api/empleados', { page: 1, size: SELECT_SIZE }))
      .then((data) => setEmpleados(data.content || []))
      .catch((e) => setError(e.message));
  }, []);

  useEffect(() => {
    let cancelled = false;
    async function load() {
      setError('');
      setLoading(true);
      try {
        const path = vista === 'ASISTENCIA'
          ? pagePath(PATHS.ASISTENCIA, { idEmpleado: idEmpleado || undefined, desde, hasta })
          : PATHS[vista];
        const data = await http.get(path);
        if (!cancelled) setRows(Array.isArray(data) ? data : []);
      } catch (e) {
        if (!cancelled) {
          setError(e.message);
          setRows([]);
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    load();
    return () => { cancelled = true; };
  }, [vista, idEmpleado, desde, hasta]);

  const visibles = rows.filter((r) => {
    if (vista === 'ASISTENCIA' && tipo && r.tipo !== tipo) return false;
    if (!q.trim()) return true;
    return haystack(vista, r).toLowerCase().includes(q.trim().toLowerCase());
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

  const actual = TIPOS.find((t) => t.id === vista) || TIPOS[0];

  return (
    <div>
      <PageHeader
        kicker="Control"
        title="Reportes"
        subtitle="Consulte en pantalla y exporte Excel o PDF."
      />
      <Alert>{error}</Alert>

      <FilterBar>
        <select className="w-auto" value={vista} onChange={(e) => setVista(e.target.value)}>
          {TIPOS.map((t) => <option key={t.id} value={t.id}>{t.label}</option>)}
        </select>
        {vista === 'ASISTENCIA' && (
          <>
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
          </>
        )}
        <SearchField placeholder="Buscar en la vista" value={q} onChange={(e) => setQ(e.target.value)} />
        <Button variant="secondary" onClick={() => bajar(vista, 'excel')}><Download size={14} /> Excel</Button>
        <Button variant="secondary" onClick={() => bajar(vista, 'pdf')}>PDF</Button>
      </FilterBar>

      <section className="mb-8">
        <div className="mb-3">
          <h2 className="text-base font-semibold text-navy">{actual.label}</h2>
          <p className="text-sm text-muted">{actual.hint}. {visibles.length} registro{visibles.length === 1 ? '' : 's'}.</p>
        </div>
        <Panel padded={false}>
          {loading ? (
            <Empty text="Cargando…" />
          ) : visibles.length === 0 ? (
            <Empty text="No hay datos para esta vista." />
          ) : (
            <PreviewTable vista={vista} rows={visibles} />
          )}
        </Panel>
      </section>
    </div>
  );
}

function PreviewTable({ vista, rows }) {
  if (vista === 'ASISTENCIA') {
    return (
      <StackTable
        cards={rows.map((r) => (
          <div key={r.idMarcacion} className="flex gap-3 px-4 py-3">
            <Avatar name={r.empleado} />
            <div className="min-w-0">
              <p className="font-medium text-navy">{r.empleado}</p>
              <p className="text-xs text-muted">{formatStamp(r.fechaHora)}</p>
              <div className="mt-2 flex flex-wrap gap-2">
                <span className={`rounded-md px-2 py-0.5 text-xs font-semibold ${r.tipo === 'INGRESO' ? 'bg-info-soft text-info' : 'bg-slate-100 text-slate-700'}`}>
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
              {rows.map((r) => (
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
                    <span className={`rounded-md px-2 py-0.5 text-xs font-semibold ${r.tipo === 'INGRESO' ? 'bg-info-soft text-info' : 'bg-slate-100 text-slate-700'}`}>
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
    );
  }

  if (vista === 'TRABAJADORES') {
    return (
      <StackTable
        cards={rows.map((r) => (
          <div key={r.idEmpleado} className="px-4 py-3">
            <p className="font-medium text-navy">{r.nombreCompleto}</p>
            <p className="text-xs text-muted">{r.codigoEmpleado} · {r.area} · {r.cargo}</p>
          </div>
        ))}
        table={(
          <table>
            <thead>
              <tr>
                <th>Trabajador</th>
                <th>Área</th>
                <th>Cargo</th>
                <th>Estado</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((r) => (
                <tr key={r.idEmpleado}>
                  <td>
                    <p className="font-medium text-navy">{r.nombreCompleto}</p>
                    <p className="text-xs text-muted">{r.codigoEmpleado}</p>
                  </td>
                  <td>{r.area || '—'}</td>
                  <td>{r.cargo || '—'}</td>
                  <td><Badge value={r.estado} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      />
    );
  }

  if (vista === 'PERMISOS' || vista === 'HORAS_EXTRAS') {
    return (
      <StackTable
        cards={rows.map((r) => (
          <div key={r.idSolicitudPermiso || r.idSolicitudHoraExtra} className="px-4 py-3">
            <p className="font-medium text-navy">{r.empleado}</p>
            <p className="text-xs text-muted">
              {vista === 'PERMISOS' ? `${r.tipoPermiso} · ${fmtDate(r.fechaInicio)} – ${fmtDate(r.fechaFin)}` : `${fmtDate(r.fecha)} · ${r.cantidadHoras} h`}
            </p>
            <div className="mt-2"><Badge value={r.estado} /></div>
          </div>
        ))}
        table={(
          <table>
            <thead>
              <tr>
                <th>Colaborador</th>
                <th>{vista === 'PERMISOS' ? 'Tipo' : 'Horas'}</th>
                <th>Periodo</th>
                <th>Estado</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((r) => (
                <tr key={r.idSolicitudPermiso || r.idSolicitudHoraExtra}>
                  <td>{r.empleado}</td>
                  <td>{vista === 'PERMISOS' ? r.tipoPermiso : `${r.cantidadHoras} h`}</td>
                  <td>
                    {vista === 'PERMISOS'
                      ? `${fmtDate(r.fechaInicio)} – ${fmtDate(r.fechaFin)}`
                      : `${fmtDate(r.fecha)} ${fmtTime(r.horaInicio)}–${fmtTime(r.horaFin)}`}
                  </td>
                  <td><Badge value={r.estado} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      />
    );
  }

  return (
    <StackTable
      cards={rows.map((r) => (
        <div key={r.idUsuario} className="px-4 py-3">
          <p className="font-medium text-navy">{r.nombreUsuario}</p>
          <p className="text-xs text-muted">{r.nombreCompleto} · {r.perfil || r.rol}</p>
        </div>
      ))}
      table={(
        <table>
          <thead>
            <tr>
              <th>Usuario</th>
              <th>Nombre</th>
              <th>Perfil</th>
              <th>Estado</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((r) => (
              <tr key={r.idUsuario}>
                <td>{r.nombreUsuario}</td>
                <td>{r.nombreCompleto || '—'}</td>
                <td>{r.perfil || r.rol}</td>
                <td><Badge value={r.activo === false ? 'INACTIVO' : 'ACTIVO'} /></td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    />
  );
}
