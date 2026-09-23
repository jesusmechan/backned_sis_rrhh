import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Copy, GitBranch, Plus } from 'lucide-react';
import { emptyPage, http, PAGE_SIZE, pagePath, SELECT_SIZE } from '../api/client';
import { Alert, Badge, Button, DataList, FilterBar, Kpi, KpiRow, ListActions, MobileRow, Pager, SearchField } from '../components/ui';
import { useQuerySearch } from '../lib/useQuerySearch';
import { FlujogramaCompact, ORIGEN, codigoDesdeNombre, toForm, toPayload } from './flujoShared';

export function Flujos() {
  const navigate = useNavigate();
  const location = useLocation();
  const [rows, setRows] = useState([]);
  const [meta, setMeta] = useState(emptyPage);
  const [page, setPage] = useState(1);
  const [origen, setOrigen] = useState('');
  const [estado, setEstado] = useState('');
  const [q, setQ, qDebounced] = useQuerySearch();
  const [roles, setRoles] = useState([]);
  const [usuarios, setUsuarios] = useState([]);
  const [error, setError] = useState('');
  const [ok, setOk] = useState(location.state?.ok || '');
  const [counts, setCounts] = useState({ total: 0, permiso: 0, horaExtra: 0, activos: 0 });

  useEffect(() => { setPage(1); }, [qDebounced]);

  useEffect(() => {
    if (!location.state?.ok) return;
    navigate(location.pathname, { replace: true, state: {} });
  }, [location.state, location.pathname, navigate]);

  useEffect(() => {
    Promise.all([
      http.get('/api/catalogos/roles'),
      http.page(pagePath('/api/usuarios', { page: 1, size: SELECT_SIZE }))
    ])
      .then(([rolesData, users]) => {
        setRoles(rolesData);
        setUsuarios(users.content || []);
      })
      .catch((e) => setError(e.message));
  }, []);

  async function loadCounts() {
    const [all, perm, extra, act] = await Promise.all([
      http.page(pagePath('/api/flujos', { page: 1, size: 1 })),
      http.page(pagePath('/api/flujos', { page: 1, size: 1, tipoOrigen: 'PERMISO' })),
      http.page(pagePath('/api/flujos', { page: 1, size: 1, tipoOrigen: 'HORA_EXTRA' })),
      http.page(pagePath('/api/flujos', { page: 1, size: 1, activo: true }))
    ]);
    setCounts({
      total: all.totalElements || 0,
      permiso: perm.totalElements || 0,
      horaExtra: extra.totalElements || 0,
      activos: act.totalElements || 0
    });
  }

  async function load() {
    const data = await http.page(pagePath('/api/flujos', {
      page,
      size: PAGE_SIZE,
      q: qDebounced,
      tipoOrigen: origen || undefined,
      activo: estado === '' ? undefined : estado === 'activos'
    }));
    setRows(data.content || []);
    setMeta(data);
  }

  useEffect(() => {
    load().catch((e) => setError(e.message));
  }, [page, qDebounced, origen, estado]);

  useEffect(() => { loadCounts().catch(() => {}); }, []);

  async function cambiarEstado(row) {
    const next = !row.activo;
    if (!next && !window.confirm(`¿Desactivar el flujo ${row.nombre}? Dejará de usarse en solicitudes nuevas.`)) return;
    setError('');
    try {
      await http.put(`/api/flujos/${row.idConfiguracion}`, toPayload({ ...toForm(row), activo: next }));
      setOk(next ? 'Flujo activado' : 'Flujo desactivado');
      await Promise.all([load(), loadCounts()]);
    } catch (err) {
      setError(err.message);
    }
  }

  function duplicar(row) {
    const form = toForm(row);
    navigate('/flujos/nuevo', {
      state: {
        duplicar: {
          ...form,
          codigo: `${codigoDesdeNombre(row.nombre) || row.codigo}-COPIA`.slice(0, 40),
          nombre: `${row.nombre} (copia)`,
          activo: true
        }
      }
    });
  }

  return (
    <div>
      <div className="mb-6 flex flex-col gap-4 border-b border-line pb-5 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-500">Aprobaciones</p>
          <h1 className="page-title mt-1">Flujos</h1>
          <p className="mt-2 text-sm text-muted">
            Circuitos de aprobación por tipo de trámite. El primer paso queda en curso al registrar; un rechazo cierra la solicitud.
          </p>
        </div>
        <Button onClick={() => navigate('/flujos/nuevo')}><Plus size={16} /> Nuevo flujo</Button>
      </div>

      <Alert>{error}</Alert>
      <Alert ok>{ok}</Alert>

      <KpiRow cols={4}>
        <Kpi value={counts.total} label="Total" hint="Circuitos configurados" active={origen === '' && estado === ''} onClick={() => { setOrigen(''); setEstado(''); setPage(1); }} />
        <Kpi value={counts.permiso} label="Permisos" hint="Flujos de permiso" active={origen === 'PERMISO'} onClick={() => { setOrigen('PERMISO'); setPage(1); }} />
        <Kpi value={counts.horaExtra} label="Horas extras" hint="Flujos de tiempo extra" active={origen === 'HORA_EXTRA'} onClick={() => { setOrigen('HORA_EXTRA'); setPage(1); }} />
        <Kpi value={counts.activos} label="Activos" hint="Se usan en solicitudes nuevas" active={estado === 'activos'} onClick={() => { setEstado(estado === 'activos' ? '' : 'activos'); setPage(1); }} />
      </KpiRow>

      <FilterBar>
        <SearchField placeholder="Buscar nombre, código o tipo" value={q} onChange={(e) => { setQ(e.target.value); setPage(1); }} />
        <select className="w-auto" value={estado} onChange={(e) => { setEstado(e.target.value); setPage(1); }}>
          <option value="">Todos los estados</option>
          <option value="activos">Activos</option>
          <option value="inactivos">Inactivos</option>
        </select>
      </FilterBar>

      <DataList
        empty={rows.length === 0}
        emptyText="No hay flujos en este filtro."
        cards={rows.map((r) => (
          <MobileRow
            key={r.idConfiguracion}
            leading={(
              <span className="grid h-10 w-10 shrink-0 place-items-center rounded-lg bg-slate-100 text-navy">
                <GitBranch size={16} />
              </span>
            )}
            title={r.nombre}
            meta={`${r.codigo} · ${ORIGEN[r.tipoOrigen] || r.tipoOrigen} · ${(r.pasos || []).length} pasos`}
            badge={<Badge value={r.activo ? 'ACTIVO' : 'INACTIVO'} />}
            actions={(
              <>
                <Button variant="secondary" className="px-3 py-1.5 text-xs" onClick={() => navigate(`/flujos/${r.idConfiguracion}`)}>Configurar</Button>
                <Button variant="secondary" className="px-3 py-1.5 text-xs" onClick={() => duplicar(r)}><Copy size={14} /> Duplicar</Button>
                <Button variant={r.activo ? 'danger' : 'secondary'} className="px-3 py-1.5 text-xs" onClick={() => cambiarEstado(r)}>
                  {r.activo ? 'Desactivar' : 'Activar'}
                </Button>
              </>
            )}
          />
        ))}
        table={(
          <table>
            <thead>
              <tr>
                <th>Flujo</th>
                <th>Circuito</th>
                <th>Estado</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {rows.map((r) => (
                <tr key={r.idConfiguracion}>
                  <td>
                    <div className="flex items-start gap-3">
                      <span className="grid h-9 w-9 shrink-0 place-items-center rounded-lg bg-slate-100 text-navy">
                        <GitBranch size={16} />
                      </span>
                      <div className="min-w-0">
                        <button
                          type="button"
                          className="text-left font-medium text-navy hover:underline"
                          onClick={() => navigate(`/flujos/${r.idConfiguracion}`)}
                        >
                          {r.nombre}
                        </button>
                        <p className="text-xs text-muted">
                          {r.codigo} · {ORIGEN[r.tipoOrigen] || r.tipoOrigen}
                          {r.tipoPermiso ? ` · ${r.tipoPermiso}` : r.tipoOrigen === 'PERMISO' ? ' · Por defecto' : ''}
                          {' · '}{(r.pasos || []).length} paso{(r.pasos || []).length === 1 ? '' : 's'}
                        </p>
                        {r.descripcion ? <p className="mt-1 text-sm text-slate-600">{r.descripcion}</p> : null}
                      </div>
                    </div>
                  </td>
                  <td>
                    <FlujogramaCompact pasos={r.pasos} roles={roles} usuarios={usuarios} />
                  </td>
                  <td><Badge value={r.activo ? 'ACTIVO' : 'INACTIVO'} /></td>
                  <td>
                    <ListActions>
                      <Button variant="secondary" className="px-3 py-1.5 text-xs" onClick={() => navigate(`/flujos/${r.idConfiguracion}`)}>Configurar</Button>
                      <Button variant="secondary" className="px-3 py-1.5 text-xs" onClick={() => duplicar(r)}><Copy size={14} /> Duplicar</Button>
                      <Button variant={r.activo ? 'danger' : 'secondary'} className="px-3 py-1.5 text-xs" onClick={() => cambiarEstado(r)}>
                        {r.activo ? 'Desactivar' : 'Activar'}
                      </Button>
                    </ListActions>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        footer={(
          <Pager page={meta.page} totalPages={meta.totalPages} totalElements={meta.totalElements} size={meta.size} onPage={setPage} />
        )}
      />
    </div>
  );
}
