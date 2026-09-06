import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { http, pagePath } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { menuIcon } from '../layout/icons';
import { Badge, Button, Panel } from '../components/ui';

export function Inicio() {
  const { usuario, perfil, menu, canAccess } = useAuth();
  const [bandejaTotal, setBandejaTotal] = useState(0);
  const [permisos, setPermisos] = useState([]);
  const [pendientes, setPendientes] = useState(0);
  const canInbox = canAccess('/bandeja');
  const first = usuario?.nombreCompleto?.split(' ')[0] || usuario?.nombreUsuario;
  const periodo = new Date().toLocaleDateString('es-PE', { month: 'long', year: 'numeric' });
  const modules = (menu || []).flatMap((grupo) => grupo.items || []).filter((item) => item.ruta !== '/');

  useEffect(() => {
    http.page(pagePath('/api/permisos', { page: 1, size: 5 }))
      .then((data) => setPermisos(data.content))
      .catch(() => setPermisos([]));
    http.page(pagePath('/api/permisos', { page: 1, size: 1, estado: 'PENDIENTE' }))
      .then((data) => setPendientes(data.totalElements))
      .catch(() => setPendientes(0));
    if (canInbox) {
      http.page(pagePath('/api/bandeja', { page: 1, size: 1 }))
        .then((data) => setBandejaTotal(data.totalElements))
        .catch(() => setBandejaTotal(0));
    }
  }, [usuario]);

  return (
    <div>
      <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-500">
        Gestión de talento humano · Consultora Contable Andina
      </p>
      <div className="mt-1 flex flex-wrap items-center gap-2">
        <h1 className="text-3xl font-bold text-navy">Inicio</h1>
        <span className="rounded-full bg-sky-50 px-2.5 py-1 text-xs font-medium capitalize text-sky-800">{periodo}</span>
      </div>
      <p className="mt-2 mb-5 text-sm text-muted">
        Hola, {first}. Perfil <b>{perfil || usuario?.rol}</b> · {usuario?.nombreUsuario}.
      </p>

      <div className="mb-5 grid gap-3 sm:grid-cols-3">
        <Panel>
          <p className="text-xs text-muted">Permisos pendientes</p>
          <p className="mt-1 text-3xl font-bold text-navy">{pendientes}</p>
        </Panel>
        <Panel>
          <p className="text-xs text-muted">Bandeja</p>
          <p className="mt-1 text-3xl font-bold text-navy">{canInbox ? bandejaTotal : '—'}</p>
        </Panel>
        <Panel>
          <p className="text-xs text-muted">Perfil activo</p>
          <p className="mt-1 text-3xl font-bold text-navy">{perfil || usuario?.rol}</p>
        </Panel>
      </div>

      <h2 className="mb-3 text-sm font-semibold text-navy">Opciones de su perfil</h2>
      <div className="mb-6 grid gap-3 md:grid-cols-2 xl:grid-cols-3">
        {modules.map((m) => {
          const Icon = menuIcon(m.icono);
          return (
            <Link key={m.codigo} to={m.ruta} className="block">
              <Panel className="h-full hover:border-slate-300">
                <div className="mb-3 grid h-10 w-10 place-items-center rounded-lg bg-slate-100 text-navy">
                  <Icon size={18} />
                </div>
                <p className="font-semibold text-navy">{m.etiqueta}</p>
                <p className="mt-1 text-sm text-muted">{m.descripcion || m.ruta}</p>
              </Panel>
            </Link>
          );
        })}
      </div>

      {canAccess('/permisos') && (
        <Panel title="Permisos recientes" padded={false}>
          <div className="p-5 pb-2"><h3 className="text-sm font-semibold text-navy">Permisos recientes</h3></div>
          {permisos.length === 0 ? (
            <p className="px-5 pb-5 text-sm text-muted">Sin solicitudes.</p>
          ) : permisos.map((p) => (
            <div key={p.idSolicitudPermiso} className="flex items-center justify-between border-t border-line px-5 py-3">
              <div>
                <p className="text-sm font-medium">{p.empleado}</p>
                <p className="text-xs text-muted">{p.tipoPermiso}</p>
              </div>
              <Badge value={p.estado} />
            </div>
          ))}
          <div className="border-t border-line px-5 py-3">
            <Link to="/permisos"><Button variant="secondary">Ver permisos</Button></Link>
          </div>
        </Panel>
      )}
    </div>
  );
}
