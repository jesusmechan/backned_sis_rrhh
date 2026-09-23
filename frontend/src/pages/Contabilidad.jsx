import { Fragment, useEffect, useState } from 'react';
import { emptyPage, http, PAGE_SIZE, pagePath } from '../api/client';
import { Alert, Badge, DataList, FilterBar, MobileRow, Pager } from '../components/ui';
import { fmtDate, fmtMoney } from '../lib/format';

export function Contabilidad() {
  const [rows, setRows] = useState([]);
  const [meta, setMeta] = useState(emptyPage);
  const [page, setPage] = useState(1);
  const [error, setError] = useState('');
  const [abierto, setAbierto] = useState(null);

  useEffect(() => {
    http.page(pagePath('/api/asientos', { page, size: PAGE_SIZE }))
      .then((data) => {
        setRows(data.content || []);
        setMeta(data);
      })
      .catch((e) => setError(e.message));
  }, [page]);

  function toggle(id) {
    setAbierto((cur) => (cur === id ? null : id));
  }

  return (
    <div>
      <div className="mb-6 border-b border-line pb-5">
        <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-500">Gestión</p>
        <h1 className="page-title mt-1">Contabilidad</h1>
        <p className="mt-2 text-sm text-muted">
          Asientos de partida doble generados al cerrar cada planilla (sueldos, ONP, EsSalud y remuneraciones por pagar).
        </p>
      </div>

      <Alert>{error}</Alert>
      <FilterBar>
        <p className="text-sm text-muted">{meta.totalElements || 0} asientos</p>
      </FilterBar>

      <DataList
        empty={rows.length === 0}
        emptyText="Aún no hay asientos. Cierre una planilla calculada para generarlos."
        cards={rows.map((r) => (
          <div key={r.idAsiento}>
            <button type="button" className="w-full text-left" onClick={() => toggle(r.idAsiento)}>
              <MobileRow
                title={r.codigo}
                meta={`${fmtDate(r.fecha)} · Debe ${fmtMoney(r.totalDebe)} · Haber ${fmtMoney(r.totalHaber)}`}
                badge={<Badge value={r.estado} />}
              />
            </button>
            {abierto === r.idAsiento && (
              <div className="border-t border-line px-4 pb-3">
                <p className="py-2 text-sm text-slate-600">{r.glosa}</p>
                <LineasTable lineas={r.lineas} />
              </div>
            )}
          </div>
        ))}
        table={(
          <table>
            <thead>
              <tr>
                <th>Asiento</th>
                <th>Fecha</th>
                <th>Debe</th>
                <th>Haber</th>
                <th>Estado</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {rows.map((r) => (
                <Fragment key={r.idAsiento}>
                  <tr>
                    <td>
                      <p className="font-medium text-navy">{r.codigo}</p>
                      <p className="text-xs text-muted">{r.glosa}</p>
                    </td>
                    <td className="text-sm">{fmtDate(r.fecha)}</td>
                    <td className="text-sm">{fmtMoney(r.totalDebe)}</td>
                    <td className="text-sm">{fmtMoney(r.totalHaber)}</td>
                    <td><Badge value={r.estado} /></td>
                    <td>
                      <button
                        type="button"
                        className="text-xs font-medium text-navy hover:underline"
                        onClick={() => toggle(r.idAsiento)}
                      >
                        {abierto === r.idAsiento ? 'Ocultar' : 'Ver líneas'}
                      </button>
                    </td>
                  </tr>
                  {abierto === r.idAsiento && (
                    <tr>
                      <td colSpan={6} className="bg-surface px-4 py-3">
                        <LineasTable lineas={r.lineas} />
                      </td>
                    </tr>
                  )}
                </Fragment>
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

function LineasTable({ lineas }) {
  return (
    <table className="min-w-full text-sm">
      <thead className="text-left text-xs uppercase tracking-wide text-slate-500">
        <tr>
          <th className="py-2">Cuenta</th>
          <th className="py-2">Nombre</th>
          <th className="py-2 text-right">Debe</th>
          <th className="py-2 text-right">Haber</th>
        </tr>
      </thead>
      <tbody>
        {(lineas || []).map((l) => (
          <tr key={l.idLinea} className="border-t border-line">
            <td className="py-2 font-medium text-navy">{l.cuenta}</td>
            <td className="py-2">{l.nombreCuenta}</td>
            <td className="py-2 text-right">{Number(l.debe) ? fmtMoney(l.debe) : ''}</td>
            <td className="py-2 text-right">{Number(l.haber) ? fmtMoney(l.haber) : ''}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
