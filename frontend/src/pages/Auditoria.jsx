import { useEffect, useState } from 'react';
import { emptyPage, http, PAGE_SIZE, pagePath } from '../api/client';
import { Alert, Button, Empty, PageHeader, Pager, Panel } from '../components/ui';

export function Auditoria() {
  const [tab, setTab] = useState('auditoria');
  const [rows, setRows] = useState([]);
  const [meta, setMeta] = useState(emptyPage);
  const [page, setPage] = useState(1);
  const [error, setError] = useState('');

  useEffect(() => {
    const path = tab === 'auditoria' ? '/api/auditoria' : '/api/trazabilidad';
    http.page(pagePath(path, { page, size: PAGE_SIZE }))
      .then((data) => { setRows(data.content); setMeta(data); })
      .catch((e) => setError(e.message));
  }, [tab, page]);

  return (
    <div>
      <PageHeader
        kicker="Gobierno"
        title="Auditoría"
        subtitle="Bitácora de acciones y trazabilidad de solicitudes."
        actions={(
          <div className="flex gap-2">
            <Button variant={tab === 'auditoria' ? 'primary' : 'secondary'} onClick={() => { setTab('auditoria'); setPage(1); }}>Bitácora</Button>
            <Button variant={tab === 'trazabilidad' ? 'primary' : 'secondary'} onClick={() => { setTab('trazabilidad'); setPage(1); }}>Trazabilidad</Button>
          </div>
        )}
      />
      <Alert>{error}</Alert>
      <Panel padded={false}>
        {rows.length === 0 ? <Empty text="Sin registros." /> : tab === 'auditoria' ? (
          <div className="overflow-x-auto">
            <table>
              <thead><tr><th>Usuario</th><th>Acción</th><th>Entidad</th><th>Detalle</th><th>Fecha</th></tr></thead>
              <tbody>
                {rows.map((r) => (
                  <tr key={r.idAuditoria}>
                    <td>{r.usuario}</td>
                    <td>{r.accion}</td>
                    <td>{r.entidad} #{r.idEntidad}</td>
                    <td>{r.detalle}</td>
                    <td>{r.fechaHora}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="overflow-x-auto">
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
          </div>
        )}
        <Pager page={meta.page} totalPages={meta.totalPages} totalElements={meta.totalElements} size={meta.size} onPage={setPage} />
      </Panel>
    </div>
  );
}
