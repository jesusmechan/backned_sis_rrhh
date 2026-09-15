import { useEffect, useState } from 'react';
import { emptyPage, http, PAGE_SIZE, pagePath } from '../api/client';
import { Alert, Badge, Empty, FilterBar, Pager } from '../components/ui';
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

      {rows.length === 0 ? (
        <div className="rounded-xl border border-line bg-white">
          <Empty text="Aún no hay asientos. Cierre una planilla calculada para generarlos." />
        </div>
      ) : (
        <div className="space-y-3">
          {rows.map((r) => (
            <article key={r.idAsiento} className="rounded-xl border border-line bg-white p-4">
              <button type="button" className="flex w-full items-start justify-between gap-3 text-left" onClick={() => setAbierto(abierto === r.idAsiento ? null : r.idAsiento)}>
                <div>
                  <p className="font-semibold text-navy">{r.codigo}</p>
                  <p className="mt-1 text-sm text-slate-600">{r.glosa}</p>
                  <p className="mt-1 text-xs text-muted">{fmtDate(r.fecha)} · Debe {fmtMoney(r.totalDebe)} · Haber {fmtMoney(r.totalHaber)}</p>
                </div>
                <Badge value={r.estado} />
              </button>
              {abierto === r.idAsiento && (
                <table className="mt-4 min-w-full text-sm">
                  <thead className="text-left text-xs uppercase tracking-wide text-slate-500">
                    <tr>
                      <th className="py-2">Cuenta</th>
                      <th className="py-2">Nombre</th>
                      <th className="py-2 text-right">Debe</th>
                      <th className="py-2 text-right">Haber</th>
                    </tr>
                  </thead>
                  <tbody>
                    {(r.lineas || []).map((l) => (
                      <tr key={l.idLinea} className="border-t border-line">
                        <td className="py-2 font-medium text-navy">{l.cuenta}</td>
                        <td className="py-2">{l.nombreCuenta}</td>
                        <td className="py-2 text-right">{Number(l.debe) ? fmtMoney(l.debe) : ''}</td>
                        <td className="py-2 text-right">{Number(l.haber) ? fmtMoney(l.haber) : ''}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </article>
          ))}
        </div>
      )}

      <div className="mt-3 overflow-hidden rounded-xl border border-line bg-white">
        <Pager page={meta.page} totalPages={meta.totalPages} totalElements={meta.totalElements} size={meta.size} onPage={setPage} />
      </div>
    </div>
  );
}
