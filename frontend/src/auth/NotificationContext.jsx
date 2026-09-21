import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { getAccessToken, http } from '../api/client';
import { useAuth } from './AuthContext';

const NotificationContext = createContext(null);

function applyEvento(prev, evento) {
  const incoming = evento?.notificacion;
  if (!incoming) return prev;
  const rest = prev.filter((n) => n.idNotificacion !== incoming.idNotificacion);
  return [incoming, ...rest].slice(0, 40);
}

export function NotificationProvider({ children }) {
  const { isAuth } = useAuth();
  const [items, setItems] = useState([]);
  const [noLeidas, setNoLeidas] = useState(0);
  const clientRef = useRef(null);

  const load = useCallback(async () => {
    if (!isAuth) {
      setItems([]);
      setNoLeidas(0);
      return;
    }
    try {
      const [lista, contador] = await Promise.all([
        http.get('/api/notificaciones'),
        http.get('/api/notificaciones/no-leidas')
      ]);
      setItems(Array.isArray(lista) ? lista : []);
      setNoLeidas(contador?.noLeidas || 0);
    } catch {
      setItems([]);
      setNoLeidas(0);
    }
  }, [isAuth]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    if (!isAuth) {
      clientRef.current?.deactivate();
      clientRef.current = null;
      return undefined;
    }
    const token = getAccessToken();
    if (!token) return undefined;

    const client = new Client({
      webSocketFactory: () => new SockJS(`/ws?access_token=${encodeURIComponent(token)}`),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 4000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        client.subscribe('/user/queue/notificaciones', (frame) => {
          try {
            const evento = JSON.parse(frame.body);
            setItems((prev) => applyEvento(prev, evento));
            if (typeof evento.noLeidas === 'number') {
              setNoLeidas(evento.noLeidas);
            } else {
              setNoLeidas((n) => n + 1);
            }
          } catch {
            /* frame inválido */
          }
        });
      }
    });
    client.activate();
    clientRef.current = client;
    return () => {
      client.deactivate();
      if (clientRef.current === client) clientRef.current = null;
    };
  }, [isAuth]);

  const value = useMemo(() => ({
    items,
    noLeidas,
    refresh: load,
    marcarLeida: async (id) => {
      const updated = await http.post(`/api/notificaciones/${id}/leer`);
      setItems((prev) => prev.map((n) => (n.idNotificacion === id ? { ...n, leida: true } : n)));
      setNoLeidas((n) => Math.max(0, updated?.leida ? n - 1 : n));
      try {
        const contador = await http.get('/api/notificaciones/no-leidas');
        setNoLeidas(contador?.noLeidas || 0);
      } catch {
        /* se mantiene el conteo local */
      }
    },
    marcarTodas: async () => {
      await http.post('/api/notificaciones/leer-todas');
      setItems((prev) => prev.map((n) => ({ ...n, leida: true })));
      setNoLeidas(0);
    }
  }), [items, noLeidas, load]);

  return <NotificationContext.Provider value={value}>{children}</NotificationContext.Provider>;
}

export function useNotificaciones() {
  const ctx = useContext(NotificationContext);
  if (!ctx) {
    throw new Error('useNotificaciones debe usarse dentro de NotificationProvider');
  }
  return ctx;
}
