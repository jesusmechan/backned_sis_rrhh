import { useCallback, useEffect, useRef, useState } from 'react';
import { emptyPage } from '../api/client';

/**
 * Carga paginada segura: cancela respuestas viejas, reinicia a página 1
 * cuando cambian los filtros y sincroniza la página con la respuesta del API.
 *
 * @param {unknown[]} filterDeps dependencias de filtro (sin incluir page)
 * @param {(page: number) => Promise<{content: any[], page: number, size: number, totalElements: number, totalPages: number}>} fetchPage
 */
export function usePagedLoad(filterDeps, fetchPage) {
  const [page, setPage] = useState(1);
  const [rows, setRows] = useState([]);
  const [meta, setMeta] = useState(emptyPage);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const filterKey = JSON.stringify(filterDeps);
  const prevFilterKey = useRef(filterKey);
  const reqId = useRef(0);
  const fetchRef = useRef(fetchPage);
  fetchRef.current = fetchPage;

  const run = useCallback((pageNum) => {
    const id = ++reqId.current;
    setLoading(true);
    setError('');
    return Promise.resolve()
      .then(() => fetchRef.current(pageNum))
      .then((data) => {
        if (id !== reqId.current) return null;
        const next = data || emptyPage;
        setRows(Array.isArray(next.content) ? next.content : []);
        setMeta(next);
        if (next.page && next.page !== pageNum) {
          setPage(next.page);
        }
        return next;
      })
      .catch((e) => {
        if (id !== reqId.current) return null;
        setError(e.message || 'No se pudo cargar el listado');
        return null;
      })
      .finally(() => {
        if (id === reqId.current) setLoading(false);
      });
  }, []);

  useEffect(() => {
    const filtersChanged = prevFilterKey.current !== filterKey;
    prevFilterKey.current = filterKey;

    if (filtersChanged && page !== 1) {
      setPage(1);
      return undefined;
    }

    run(page);
    return () => {
      reqId.current += 1;
    };
  }, [page, filterKey, run]);

  const reload = useCallback((overridePage) => run(overridePage ?? page), [run, page]);

  return {
    page,
    setPage,
    rows,
    setRows,
    meta,
    setMeta,
    error,
    setError,
    loading,
    reload
  };
}
