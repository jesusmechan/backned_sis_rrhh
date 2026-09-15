import { useEffect, useState } from 'react';
import { emptyPage, http, PAGE_SIZE, pagePath } from '../api/client';
import { useQuerySearch } from './useQuerySearch';
import { useFlashOk } from './useFlashOk';

export function useSolicitudList(apiPath) {
  const [rows, setRows] = useState([]);
  const [meta, setMeta] = useState(emptyPage);
  const [page, setPage] = useState(1);
  const [counts, setCounts] = useState({ total: 0, pendientes: 0, aprobados: 0, rechazados: 0 });
  const [tab, setTab] = useState('todas');
  const [q, setQ, qDebounced] = useQuerySearch();
  const [error, setError] = useState('');
  const [ok] = useFlashOk();

  useEffect(() => { setPage(1); }, [qDebounced]);

  useEffect(() => {
    Promise.all([
      http.page(pagePath(apiPath, { page: 1, size: 1 })),
      http.page(pagePath(apiPath, { page: 1, size: 1, estado: 'PENDIENTE' })),
      http.page(pagePath(apiPath, { page: 1, size: 1, estado: 'APROBADO' })),
      http.page(pagePath(apiPath, { page: 1, size: 1, estado: 'RECHAZADO' }))
    ])
      .then(([all, pend, apr, rec]) => {
        setCounts({
          total: all.totalElements || 0,
          pendientes: pend.totalElements || 0,
          aprobados: apr.totalElements || 0,
          rechazados: rec.totalElements || 0
        });
      })
      .catch(() => {});
  }, [apiPath]);

  useEffect(() => {
    const estado = tab === 'todas' ? '' : tab;
    http.page(pagePath(apiPath, { page, size: PAGE_SIZE, estado, q: qDebounced }))
      .then((data) => {
        setRows(data.content || []);
        setMeta(data);
      })
      .catch((e) => setError(e.message));
  }, [apiPath, page, tab, qDebounced]);

  return { rows, meta, page, setPage, tab, setTab, counts, q, setQ, error, ok };
}
