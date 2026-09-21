import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';

export function useQuerySearch() {
  const [params] = useSearchParams();
  const fromUrl = params.get('q') || '';
  const [q, setQ] = useState(fromUrl);
  const [qDebounced, setQDebounced] = useState(fromUrl.trim());

  useEffect(() => {
    setQ(fromUrl);
    setQDebounced(fromUrl.trim());
  }, [fromUrl]);

  useEffect(() => {
    const next = q.trim();
    if (next === qDebounced) return undefined;
    const t = setTimeout(() => setQDebounced(next), 300);
    return () => clearTimeout(t);
  }, [q, qDebounced]);

  return [q, setQ, qDebounced];
}
