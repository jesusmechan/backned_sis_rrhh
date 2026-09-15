import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';

export function useFlashOk() {
  const location = useLocation();
  const navigate = useNavigate();
  const [ok, setOk] = useState(location.state?.ok || '');

  useEffect(() => {
    if (location.state?.ok) {
      navigate(location.pathname, { replace: true, state: {} });
    }
  }, []);

  return [ok, setOk];
}
