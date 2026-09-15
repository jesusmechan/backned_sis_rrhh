import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';

export function useDuplicarPrefill(empty) {
  const location = useLocation();
  const navigate = useNavigate();
  const [form, setForm] = useState(empty);

  useEffect(() => {
    if (!location.state?.duplicar) return;
    setForm({ ...empty, ...location.state.duplicar });
    navigate(location.pathname, { replace: true, state: {} });
  }, []);

  return [form, setForm];
}
