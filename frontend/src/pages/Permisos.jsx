import { useNavigate } from 'react-router-dom';
import { Plus } from 'lucide-react';
import { Alert, Avatar, Badge, Button, DataList, FilterBar, Kpi, KpiRow, ListActions, MobileRow, Pager, PersonCell, SearchField } from '../components/ui';
import { useSolicitudList } from '../lib/useSolicitudList';
import { fmtDate, fmtTime } from '../lib/format';

export function Permisos() {
  const navigate = useNavigate();
  const { rows, meta, setPage, tab, setTab, counts, q, setQ, error, ok } = useSolicitudList('/api/permisos');

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

      <DataList
        empty={rows.length === 0}
        emptyText="No hay solicitudes en este filtro."
        cards={rows.map((r) => (
          <MobileRow
            key={r.idSolicitudPermiso}
            leading={<Avatar name={r.empleado} />}
            title={r.empleado}
            meta={`${r.tipoPermiso} · ${fmtDate(r.fechaInicio)} → ${fmtDate(r.fechaFin)}`}
            badge={<Badge value={r.estado} />}
            actions={(
              <Button variant="secondary" className="px-3 py-1.5 text-xs" onClick={() => navigate(`/permisos/${r.idSolicitudPermiso}`)}>
                Ver
              </Button>
            )}
          />
        ))}
        table={(
          <table>
            <thead>
              <tr>
                <th>Solicitante</th>
                <th>Tipo</th>
                <th>Periodo</th>
                <th>Motivo</th>
                <th>Estado</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {rows.map((r) => (
                <tr key={r.idSolicitudPermiso}>
                  <td>
                    <PersonCell name={r.empleado} meta={`#${r.idSolicitudPermiso}`} />
                  </td>
                  <td className="text-sm">{r.tipoPermiso}</td>
                  <td className="text-sm">
                    {fmtDate(r.fechaInicio)} → {fmtDate(r.fechaFin)}
                    {r.horaInicio ? (
                      <p className="text-xs text-muted">{fmtTime(r.horaInicio)} – {fmtTime(r.horaFin)}</p>
                    ) : null}
                  </td>
                  <td className="max-w-xs truncate text-sm text-muted">{r.motivo || '—'}</td>
                  <td><Badge value={r.estado} /></td>
                  <td>
                    <ListActions>
                      <Button variant="secondary" className="px-3 py-1.5 text-xs" onClick={() => navigate(`/permisos/${r.idSolicitudPermiso}`)}>
                        Ver
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
