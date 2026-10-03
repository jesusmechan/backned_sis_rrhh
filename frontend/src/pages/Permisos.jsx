import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus, Search } from 'lucide-react';
import { Alert, Avatar, Badge, Button, DataList, FilterBar, Kpi, KpiRow, ListActions, MobileRow, Modal, Pager, PersonCell, SearchField } from '../components/ui';
import { SolicitudVista } from '../components/solicitud/SolicitudVista';
import { useSolicitudList } from '../lib/useSolicitudList';
import { http } from '../api/client';
import { fmtDate, fmtDateTime, fmtTime } from '../lib/format';

export function Permisos() {
  const navigate = useNavigate();
  const { rows, meta, setPage, tab, setTab, counts, q, setQ, error, ok } = useSolicitudList('/api/permisos');
  const [previewId, setPreviewId] = useState(null);
  const [preview, setPreview] = useState({ detalle: null, historial: [], loading: false, error: '' });

  async function abrirVista(id) {
    setPreviewId(id);
    setPreview({ detalle: null, historial: [], loading: true, error: '' });
    try {
      const [detalle, historial] = await Promise.all([
        http.get(`/api/permisos/${id}`),
        http.get(`/api/permisos/${id}/historial`).catch(() => [])
      ]);
      setPreview({ detalle, historial: historial || [], loading: false, error: '' });
    } catch (e) {
      setPreview({ detalle: null, historial: [], loading: false, error: e.message });
    }
  }

  function cerrarVista() {
    setPreviewId(null);
    setPreview({ detalle: null, historial: [], loading: false, error: '' });
  }

  const lupa = (id) => (
    <button
      type="button"
      title="Vista rápida"
      onClick={() => abrirVista(id)}
      className="inline-flex h-8 w-8 items-center justify-center rounded-lg border border-line bg-white text-navy hover:bg-surface"
    >
      <Search size={15} />
    </button>
  );

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
        {(q || (tab !== 'todas' && tab !== 'PENDIENTE' && tab !== 'APROBADO')) && (
          <button
            type="button"
            className="text-xs font-medium text-navy hover:underline"
            onClick={() => { setQ(''); setTab('todas'); setPage(1); }}
          >
            Limpiar filtros
          </button>
        )}
      </FilterBar>

      <DataList
        empty={rows.length === 0}
        emptyText="No hay solicitudes en este filtro."
        cards={rows.map((r) => (
          <MobileRow
            key={r.idSolicitudPermiso}
            leading={<Avatar name={r.empleado} />}
            title={r.empleado}
            meta={`${r.tipoPermiso} · ${fmtDate(r.fechaInicio)}${r.fechaInicio !== r.fechaFin ? ` → ${fmtDate(r.fechaFin)}` : ''}${r.horaInicio ? ` · ${fmtTime(r.horaInicio)} – ${fmtTime(r.horaFin)}` : ''}${r.fechaCreacion ? ` · Reg. ${fmtDateTime(r.fechaCreacion)}` : ''}`}
            badge={<Badge tipo="ESTADO_SOLICITUD" value={r.estado} />}
            actions={(
              <div className="flex items-center gap-2">
                {lupa(r.idSolicitudPermiso)}
                <Button variant="secondary" className="px-3 py-1.5 text-xs" onClick={() => navigate(`/permisos/${r.idSolicitudPermiso}`)}>
                  Abrir
                </Button>
              </div>
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
                <th>Registrada</th>
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
                    <p>
                      {fmtDate(r.fechaInicio)}
                      {r.fechaInicio !== r.fechaFin ? <> → {fmtDate(r.fechaFin)}</> : null}
                    </p>
                    {r.horaInicio ? (
                      <p className="text-xs text-muted">{fmtTime(r.horaInicio)} – {fmtTime(r.horaFin)}</p>
                    ) : (
                      <p className="text-xs text-muted">Jornada completa</p>
                    )}
                  </td>
                  <td className="text-sm whitespace-nowrap">
                    {fmtDateTime(r.fechaCreacion)}
                  </td>
                  <td className="max-w-xs truncate text-sm text-muted">{r.motivo || '—'}</td>
                  <td><Badge tipo="ESTADO_SOLICITUD" value={r.estado} /></td>
                  <td>
                    <ListActions>
                      {lupa(r.idSolicitudPermiso)}
                      <Button variant="secondary" className="px-3 py-1.5 text-xs" onClick={() => navigate(`/permisos/${r.idSolicitudPermiso}`)}>
                        Abrir
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

      {previewId != null && (
        <Modal
          wide
          title={preview.detalle
            ? `Permiso · ${preview.detalle.tipoPermiso} · #${preview.detalle.idSolicitudPermiso}`
            : 'Vista del permiso'}
          onClose={cerrarVista}
        >
          <SolicitudVista
            detalle={preview.detalle}
            historial={preview.historial}
            tipoSolicitud="PERMISO"
            subtitle={preview.detalle?.tipoPermiso}
            loading={preview.loading}
            error={preview.error}
            compact
          />
          {preview.detalle && (
            <div className="mt-4 flex justify-end border-t border-line pt-4">
              <Button onClick={() => navigate(`/permisos/${previewId}`)}>
                Abrir pantalla completa
              </Button>
            </div>
          )}
        </Modal>
      )}
    </div>
  );
}
