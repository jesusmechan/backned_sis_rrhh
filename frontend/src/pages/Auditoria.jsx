import { useEffect, useState } from 'react';
import { http, PAGE_SIZE, pagePath } from '../api/client';
import { Alert, Empty, FilterBar, Kpi, KpiRow, PageHeader, Pager, Panel, SearchField, StackTable } from '../components/ui';
import { useQuerySearch } from '../lib/useQuerySearch';
import { usePagedLoad } from '../lib/usePagedLoad';

export function Auditoria() {
  const [tab, setTab] = useState('auditoria');
  const [counts, setCounts] = useState({ auditoria: 0, trazabilidad: 0 });
  const [q, setQ, qDebounced] = useQuerySearch();

  const { page, setPage, rows, meta, error } = usePagedLoad(
    [tab, qDebounced],
    (pageNum) => {
      const path = tab === 'auditoria' ? '/api/auditoria' : '/api/trazabilidad';
      return http.page(pagePath(path, { page: pageNum, size: PAGE_SIZE, q: qDebounced }));
    }
  );

  useEffect(() => {
    Promise.all([
      http.page(pagePath('/api/auditoria', { page: 1, size: 1 })),
      http.page(pagePath('/api/trazabilidad', { page: 1, size: 1 }))
    ])
      .then(([a, t]) => setCounts({ auditoria: a.totalElements || 0, trazabilidad: t.totalElements || 0 }))
      .catch(() => {});
  }, []);

  return (
    <div>
      <PageHeader
        kicker="Gobierno"
        title="Auditoría"
        subtitle="Bitácora de acciones y trazabilidad de solicitudes."
      />
      <KpiRow>
        <Kpi
          value={counts.auditoria}
          label="Bitácora"
          hint="Acciones de usuarios"
          active={tab === 'auditoria'}
          onClick={() => { setTab('auditoria'); setPage(1); }}
        />
        <Kpi
          value={counts.trazabilidad}
          label="Trazabilidad"
          hint="Historial de solicitudes"
          active={tab === 'trazabilidad'}
          onClick={() => { setTab('trazabilidad'); setPage(1); }}
        />
      </KpiRow>
      <Alert>{error}</Alert>
      <FilterBar>
        <SearchField
          placeholder={tab === 'auditoria' ? 'Buscar usuario, acción, entidad o detalle' : 'Buscar acción, usuario o comentario'}
          value={q}
          onChange={(e) => { setQ(e.target.value); setPage(1); }}
        />
        {qDebounced && (
          <button type="button" className="text-xs font-medium text-navy hover:underline" onClick={() => { setQ(''); setPage(1); }}>
            Limpiar filtros
          </button>
        )}
      </FilterBar>
      <Panel padded={false}>
        {rows.length === 0 ? <Empty text="Sin registros." /> : tab === 'auditoria' ? (
          <StackTable
            cards={rows.map((r) => (
              <div key={r.idAuditoria} className="px-4 py-3">
                <p className="font-medium text-navy">{r.accion}</p>
                <p className="text-xs text-muted">{r.usuario} · {r.entidad} #{r.idEntidad}</p>
                {r.detalle && <p className="mt-1 break-any text-sm text-slate-600">{String(r.detalle)}</p>}
                <p className="mt-1 text-xs text-muted">{r.fechaHora}</p>
              </div>
            ))}
            table={(
              <table>
                <thead><tr><th>Usuario</th><th>Acción</th><th>Entidad</th><th>Detalle</th><th>Fecha</th></tr></thead>
                <tbody>
                  {rows.map((r) => (
                    <tr key={r.idAuditoria}>
                      <td>{r.usuario}</td>
                      <td>{r.accion}</td>
                      <td>{r.entidad} #{r.idEntidad}</td>
                      <td className="max-w-xs break-any">{r.detalle}</td>
                      <td>{r.fechaHora}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          />
        ) : (
          <StackTable
            cards={rows.map((r) => (
              <div key={r.idHistorial} className="px-4 py-3">
                <p className="font-medium text-navy">{r.accion}</p>
                <p className="text-xs text-muted">{r.estadoAnterior} → {r.estadoNuevo} · {r.usuario}</p>
                {r.comentario && <p className="mt-1 text-sm text-slate-600">{r.comentario}</p>}
                <p className="mt-1 text-xs text-muted">{r.fechaHora}</p>
              </div>
            ))}
            table={(
              <table>
                <thead><tr><th>Acción</th><th>De → A</th><th>Usuario</th><th>Comentario</th><th>Fecha</th></tr></thead>
                <tbody>
                  {rows.map((r) => (
                    <tr key={r.idHistorial}>
                      <td>{r.accion}</td>
                      <td>{r.estadoAnterior} → {r.estadoNuevo}</td>
                      <td>{r.usuario}</td>
                      <td>{r.comentario}</td>
                      <td>{r.fechaHora}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          />
        )}
        <Pager page={meta.page} totalPages={meta.totalPages} totalElements={meta.totalElements} size={meta.size} onPage={setPage} />
      </Panel>
    </div>
  );
}
