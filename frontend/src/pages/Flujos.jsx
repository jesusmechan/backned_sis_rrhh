import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus } from 'lucide-react';
import { http, pagePath, SELECT_SIZE } from '../api/client';
import { Alert, Badge, Button, Empty, FilterBar, Kpi, KpiRow, SearchField } from '../components/ui';
import { FlujogramaCompact, ORIGEN } from './flujoShared';

export function Flujos() {
  const navigate = useNavigate();
  const [rows, setRows] = useState([]);
  const [origen, setOrigen] = useState('TODOS');
  const [q, setQ] = useState('');
  const [roles, setRoles] = useState([]);
  const [usuarios, setUsuarios] = useState([]);
  const [error, setError] = useState('');

  useEffect(() => {
    Promise.all([
      http.page(pagePath('/api/flujos', { page: 1, size: SELECT_SIZE })),
      http.get('/api/catalogos/roles'),
      http.page(pagePath('/api/usuarios', { page: 1, size: SELECT_SIZE }))
    ])
      .then(([data, rolesData, users]) => {
        setRows(data.content || []);
        setRoles(rolesData);
        setUsuarios(users.content || []);
      })
      .catch((e) => setError(e.message));
  }, []);

  const filtered = useMemo(() => {
    const needle = q.trim().toLowerCase();
    return rows.filter((r) => {
      if (origen !== 'TODOS' && r.tipoOrigen !== origen) return false;
      if (!needle) return true;
      return [r.codigo, r.nombre, r.tipoPermiso, r.descripcion]
        .filter(Boolean)
        .some((v) => String(v).toLowerCase().includes(needle));
    });
  }, [rows, origen, q]);

  const counts = {
    TODOS: rows.length,
    PERMISO: rows.filter((r) => r.tipoOrigen === 'PERMISO').length,
    HORA_EXTRA: rows.filter((r) => r.tipoOrigen === 'HORA_EXTRA').length
  };

  return (
    <div>
      <div className="mb-6 flex flex-col gap-4 border-b border-line pb-5 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-500">Aprobaciones</p>
          <h1 className="page-title mt-1">Flujos</h1>
        </div>
        <Button onClick={() => navigate('/flujos/nuevo')}><Plus size={16} /> Nuevo</Button>
      </div>

      <KpiRow>
        <Kpi value={counts.TODOS} label="Total" hint="Circuitos configurados" active={origen === 'TODOS'} onClick={() => setOrigen('TODOS')} />
        <Kpi value={counts.PERMISO} label="Permisos" hint="Flujos de permiso" active={origen === 'PERMISO'} onClick={() => setOrigen('PERMISO')} />
        <Kpi value={counts.HORA_EXTRA} label="Horas extras" hint="Flujos de tiempo extra" active={origen === 'HORA_EXTRA'} onClick={() => setOrigen('HORA_EXTRA')} />
      </KpiRow>

      <FilterBar>
        <SearchField placeholder="Filtrar por nombre o código" value={q} onChange={(e) => setQ(e.target.value)} />
      </FilterBar>

      <Alert>{error}</Alert>

      {filtered.length === 0 ? (
        <Empty text="No hay flujos en este filtro." />
      ) : (
        <div className="space-y-3">
          {filtered.map((r) => (
            <article key={r.idConfiguracion} className="overflow-hidden rounded-xl border border-line bg-white">
              <div className="flex flex-col gap-4 px-5 py-4 lg:flex-row lg:items-start lg:justify-between">
                <div className="min-w-0 shrink-0 lg:w-56">
                  <p className="text-[11px] font-semibold uppercase tracking-[0.14em] text-slate-400">
                    {ORIGEN[r.tipoOrigen]}{r.tipoPermiso ? ` · ${r.tipoPermiso}` : ''}
                  </p>
                  <h2 className="mt-1 text-lg font-semibold text-navy">{r.nombre}</h2>
                  <div className="mt-2 flex flex-wrap items-center gap-2">
                    <span className="text-xs text-slate-400">{r.codigo}</span>
                    <Badge value={r.activo ? 'ACTIVO' : 'INACTIVO'} />
                  </div>
                </div>
                <div className="min-w-0 flex-1">
                  <FlujogramaCompact pasos={r.pasos} roles={roles} usuarios={usuarios} />
                </div>
                <Button type="button" variant="secondary" onClick={() => navigate(`/flujos/${r.idConfiguracion}`)}>
                  Configurar
                </Button>
              </div>
            </article>
          ))}
        </div>
      )}
    </div>
  );
}
