import { Fragment, useEffect, useState } from 'react';
import { http, PAGE_SIZE, pagePath } from '../api/client';
import { Alert, Badge, DataList, FilterBar, Kpi, KpiRow, MobileRow, Pager, SearchField } from '../components/ui';
import { useQuerySearch } from '../lib/useQuerySearch';
import { usePagedLoad } from '../lib/usePagedLoad';
import { fmtDate, fmtMoney } from '../lib/format';

export function Contabilidad() {
  const [abierto, setAbierto] = useState(null);
  const [estado, setEstado] = useState('');
  const [q, setQ, qDebounced] = useQuerySearch();
  const [counts, setCounts] = useState({ total: 0, contabilizados: 0, anulados: 0 });

  const { page, setPage, rows, meta, error } = usePagedLoad(
    [qDebounced, estado],
    (pageNum) => http.page(pagePath('/api/asientos', { page: pageNum, size: PAGE_SIZE, q: qDebounced, estado }))
  );

  useEffect(() => {
    Promise.all([
      http.page(pagePath('/api/asientos', { page: 1, size: 1 })),
      http.page(pagePath('/api/asientos', { page: 1, size: 1, estado: 'CONTABILIZADO' })),
      http.page(pagePath('/api/asientos', { page: 1, size: 1, estado: 'ANULADO' }))
    ]).then(([all, cont, anul]) => {
      setCounts({
        total: all.totalElements || 0,
        contabilizados: cont.totalElements || 0,
        anulados: anul.totalElements || 0
      });
    }).catch(() => {});
  }, []);

  function toggle(id) {
    setAbierto((cur) => (cur === id ? null : id));
  }

  function limpiar() {
    setQ('');
    setEstado('');
    setPage(1);
  }

  const filtrosActivos = Boolean(qDebounced || estado);

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

      <KpiRow>
        <Kpi value={counts.total} label="Asientos" hint="Registrados" active={estado === ''} onClick={() => { setEstado(''); setPage(1); }} />
        <Kpi value={counts.contabilizados} label="Contabilizados" hint="Validados" active={estado === 'CONTABILIZADO'} onClick={() => { setEstado('CONTABILIZADO'); setPage(1); }} />
        <Kpi value={counts.anulados} label="Anulados" hint="Fuera de vigencia" active={estado === 'ANULADO'} onClick={() => { setEstado('ANULADO'); setPage(1); }} />
      </KpiRow>

      <FilterBar>
        <SearchField placeholder="Buscar código, glosa o periodo" value={q} onChange={(e) => { setQ(e.target.value); setPage(1); }} />
        <select className="w-auto" value={estado} onChange={(e) => { setEstado(e.target.value); setPage(1); }}>
          <option value="">Todos los estados</option>
          <option value="CONTABILIZADO">Contabilizado</option>
          <option value="BORRADOR">Borrador</option>
          <option value="ANULADO">Anulado</option>
        </select>
        {filtrosActivos && (
          <button type="button" className="text-xs font-medium text-navy hover:underline" onClick={limpiar}>
            Limpiar filtros
          </button>
        )}
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
