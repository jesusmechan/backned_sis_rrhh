import { useEffect, useState } from 'react';
import { http, PAGE_SIZE, pagePath } from '../api/client';
import { useQuerySearch } from './useQuerySearch';
import { useFlashOk } from './useFlashOk';
import { usePagedLoad } from './usePagedLoad';

export function useSolicitudList(apiPath) {
  const [counts, setCounts] = useState({ total: 0, pendientes: 0, aprobados: 0, rechazados: 0 });
  const [tab, setTab] = useState('todas');
  const [q, setQ, qDebounced] = useQuerySearch();
  const [ok] = useFlashOk();

  const estado = tab === 'todas' ? '' : tab;

  const {
    page,
    setPage,
    rows,
    setRows,
    meta,
    setMeta,
    error,
    setError
  } = usePagedLoad([apiPath, estado, qDebounced], (pageNum) =>
    http.page(pagePath(apiPath, { page: pageNum, size: PAGE_SIZE, estado, q: qDebounced }))
  );

  useEffect(() => {
    let cancelled = false;
    Promise.all([
      http.page(pagePath(apiPath, { page: 1, size: 1 })),
      http.page(pagePath(apiPath, { page: 1, size: 1, estado: 'PENDIENTE' })),
      http.page(pagePath(apiPath, { page: 1, size: 1, estado: 'APROBADO' })),
      http.page(pagePath(apiPath, { page: 1, size: 1, estado: 'RECHAZADO' }))
    ])
      .then(([all, pend, apr, rec]) => {
        if (cancelled) return;
        setCounts({
          total: all.totalElements || 0,
          pendientes: pend.totalElements || 0,
          aprobados: apr.totalElements || 0,
          rechazados: rec.totalElements || 0
        });
      })
      .catch(() => {});
    return () => { cancelled = true; };
  }, [apiPath]);

  return { rows, setRows, meta, setMeta, page, setPage, tab, setTab, counts, q, setQ, error, setError, ok };
}
