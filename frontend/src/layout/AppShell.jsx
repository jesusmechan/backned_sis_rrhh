import { useEffect, useRef, useState } from 'react';
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { Bell, ChevronDown, CircleHelp, LogOut, Menu, PanelLeftClose, Search, X } from 'lucide-react';
import { useAuth } from '../auth/AuthContext';
import { useNotificaciones } from '../auth/NotificationContext';
import { Avatar, Button, Modal } from '../components/ui';
import { menuIcon } from './icons';

const SIDEBAR_KEY = 'andina.sidebar';
const GROUPS_KEY = 'andina.menu.groups';
const SEARCH_PATHS = ['/bandeja', '/permisos', '/horas-extras', '/empleados', '/usuarios', '/contratos', '/maestros', '/asistencia', '/flujos', '/reportes', '/menu'];

function readSidebar() {
  return localStorage.getItem(SIDEBAR_KEY) !== 'hidden';
}

function readCollapsed() {
  try {
    const raw = JSON.parse(localStorage.getItem(GROUPS_KEY) || '{}');
    return raw && typeof raw === 'object' ? raw : {};
  } catch {
    return {};
  }
}

function pathOf(ruta = '') {
  return String(ruta).split('?')[0];
}

function hace(iso) {
  if (!iso) return '';
  const s = (Date.now() - new Date(iso).getTime()) / 1000;
  if (s < 60) return 'Ahora';
  if (s < 3600) return `Hace ${Math.floor(s / 60)} min`;
  if (s < 86400) return `Hace ${Math.floor(s / 3600)} h`;
  return new Date(iso).toLocaleString('es-PE', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' });
}

function formatDate() {
  return new Date().toLocaleDateString('es-PE', { weekday: 'long', day: 'numeric', month: 'long' });
}

function NavItems({ groups, collapsed, onToggle, onNavigate }) {
  const location = useLocation();

  return (groups || []).filter((group) => (group.items || []).length > 0).map((group) => {
    const open = collapsed[group.grupo] !== true;
    const hasActive = (group.items || []).some((item) => {
      const ruta = pathOf(item.ruta);
      return ruta === '/' ? location.pathname === '/' : location.pathname === ruta || location.pathname.startsWith(`${ruta}/`);
    });

    return (
      <div key={group.grupo} className="mb-2">
        <button
          type="button"
          onClick={() => onToggle(group.grupo)}
          aria-expanded={open}
          className={`flex w-full items-center justify-between rounded-lg px-3 py-2 text-left ${
            hasActive && !open ? 'bg-slate-50' : 'hover:bg-slate-50'
          }`}
        >
          <span className="text-[11px] font-semibold uppercase tracking-[0.14em] text-slate-400">
            {group.grupo}
          </span>
          <ChevronDown
            size={14}
            className={`shrink-0 text-slate-400 ${open ? '' : '-rotate-90'}`}
          />
        </button>
        {open && (
          <div className="mt-0.5 space-y-0.5">
            {group.items.map((link) => {
              const Icon = menuIcon(link.icono);
              return (
                <NavLink
                  key={link.codigo}
                  to={link.ruta}
                  end={link.ruta === '/'}
                  onClick={onNavigate}
                  className={({ isActive }) =>
                    `flex items-center gap-2.5 rounded-lg px-3 py-2 text-sm ${
                      isActive ? 'bg-navy font-medium text-white' : 'text-slate-600 hover:bg-slate-100 hover:text-navy'
                    }`
                  }
                >
                  <Icon size={16} />
                  {link.etiqueta}
                </NavLink>
              );
            })}
          </div>
        )}
      </div>
    );
  });
}

export function AppShell() {
  const { usuario, perfil, menu, logout, canAccess } = useAuth();
  const { items, noLeidas, marcarLeida, marcarTodas } = useNotificaciones();
  const navigate = useNavigate();
  const location = useLocation();
  const [open, setOpen] = useState(false);
  const [sidebar, setSidebar] = useState(readSidebar);
  const [collapsed, setCollapsed] = useState(readCollapsed);
  const [q, setQ] = useState(() => new URLSearchParams(location.search).get('q') || '');
  const [helpOpen, setHelpOpen] = useState(false);
  const [logoutOpen, setLogoutOpen] = useState(false);
  const [notifOpen, setNotifOpen] = useState(false);
  const notifRef = useRef(null);
  const canInbox = canAccess('/bandeja');

  useEffect(() => {
    setQ(new URLSearchParams(location.search).get('q') || '');
  }, [location.pathname, location.search]);

  useEffect(() => {
    setNotifOpen(false);
  }, [location.pathname]);

  useEffect(() => {
    if (!notifOpen) return undefined;
    function onDown(e) {
      if (notifRef.current && !notifRef.current.contains(e.target)) {
        setNotifOpen(false);
      }
    }
    document.addEventListener('pointerdown', onDown);
    return () => document.removeEventListener('pointerdown', onDown);
  }, [notifOpen]);

  async function abrirNotificacion(n) {
    if (!n.leida) {
      try { await marcarLeida(n.idNotificacion); } catch { /* navegamos igual */ }
    }
    setNotifOpen(false);
    setOpen(false);
    if (n.ruta) navigate(n.ruta);
  }

  function toggleGroup(name) {
    setCollapsed((prev) => {
      const next = { ...prev, [name]: prev[name] !== true };
      localStorage.setItem(GROUPS_KEY, JSON.stringify(next));
      return next;
    });
  }

  function toggleSidebar() {
    setSidebar((visible) => {
      const next = !visible;
      localStorage.setItem(SIDEBAR_KEY, next ? 'visible' : 'hidden');
      return next;
    });
  }

  function onMenuButton() {
    if (window.matchMedia('(min-width: 1024px)').matches) {
      if (!sidebar) toggleSidebar();
      return;
    }
    setOpen(true);
  }

  function onSearch(e) {
    e.preventDefault();
    const dest = SEARCH_PATHS.find((p) => canAccess(p));
    if (!dest) return;
    const query = q.trim();
    navigate(query ? `${dest}?q=${encodeURIComponent(query)}` : dest);
    setOpen(false);
  }

  function confirmLogout() {
    setLogoutOpen(false);
    setOpen(false);
    logout();
  }

  return (
    <div className={`min-h-dvh min-w-0 bg-surface ${sidebar ? 'lg:pl-60' : ''}`}>
      <aside className={`fixed inset-y-0 left-0 z-30 w-60 flex-col border-r border-line bg-white ${sidebar ? 'hidden lg:flex' : 'hidden'}`}>
        <div className="flex items-start justify-between gap-2 border-b border-line px-4 py-4">
          <div>
            <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-400">RR. HH.</p>
            <p className="mt-0.5 text-sm font-semibold text-navy">Consultora Andina</p>
          </div>
          <button
            type="button"
            className="rounded-lg p-1.5 text-slate-500 hover:bg-slate-100"
            title="Ocultar menú"
            onClick={toggleSidebar}
          >
            <PanelLeftClose size={18} />
          </button>
        </div>
        <nav className="flex-1 overflow-y-auto px-3 py-4">
          <NavItems groups={menu} collapsed={collapsed} onToggle={toggleGroup} />
        </nav>
        <div className="border-t border-line px-3 py-3">
          <NavLink to="/perfil" className="block rounded-lg px-2 py-1.5 hover:bg-slate-100">
            <p className="truncate text-sm font-medium text-navy">{usuario?.nombreUsuario}</p>
            <p className="text-xs text-muted">{perfil || usuario?.rol}</p>
          </NavLink>
        </div>
      </aside>

      <header className="sticky top-0 z-20 border-b border-line bg-white">
        <div className="flex items-center gap-2 px-3 py-2.5 sm:gap-3 sm:px-4 md:px-6">
          <button
            type="button"
            className={`shrink-0 rounded-lg border border-line p-2 ${sidebar ? 'lg:hidden' : ''}`}
            title="Mostrar menú"
            onClick={onMenuButton}
          >
            <Menu size={18} />
          </button>
          <p className="hidden min-w-0 capitalize text-sm text-muted xl:block">{formatDate()}</p>
          <form onSubmit={onSearch} className="relative mx-auto hidden min-w-0 max-w-xl flex-1 md:block">
            <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
            <input
              value={q}
              onChange={(e) => setQ(e.target.value)}
              placeholder="Buscar solicitud, empleado o código..."
              className="w-full rounded-full bg-slate-100 pl-10"
            />
          </form>
          <div className="ml-auto flex shrink-0 items-center gap-1 sm:gap-2">
            <div className="relative" ref={notifRef}>
              <button
                type="button"
                className="relative inline-flex rounded-full p-2 text-slate-500 hover:bg-slate-100"
                title={noLeidas ? `${noLeidas} notificaciones sin leer` : 'Notificaciones'}
                onClick={() => setNotifOpen((v) => !v)}
                aria-expanded={notifOpen}
              >
                <Bell size={18} />
                {noLeidas > 0 && (
                  <span className="absolute -right-0.5 -top-0.5 grid min-w-4 place-items-center rounded-full bg-red-600 px-1 text-[10px] font-semibold leading-4 text-white">
                    {noLeidas > 99 ? '99+' : noLeidas}
                  </span>
                )}
              </button>
              {notifOpen && (
                <div className="absolute right-0 z-50 mt-2 w-[min(22rem,calc(100vw-1.5rem))] overflow-hidden rounded-xl border border-line bg-white shadow-lg">
                  <div className="flex items-center justify-between border-b border-line px-3 py-2">
                    <p className="text-sm font-semibold text-navy">Notificaciones</p>
                    {noLeidas > 0 && (
                      <button type="button" className="text-xs font-medium text-navy hover:underline" onClick={() => marcarTodas()}>
                        Marcar todas
                      </button>
                    )}
                  </div>
                  <div className="max-h-80 overflow-y-auto">
                    {items.length === 0 ? (
                      <p className="px-3 py-6 text-center text-sm text-muted">No hay notificaciones.</p>
                    ) : items.map((n) => (
                      <button
                        key={n.idNotificacion}
                        type="button"
                        onClick={() => abrirNotificacion(n)}
                        className={`block w-full border-b border-line px-3 py-2.5 text-left last:border-b-0 ${n.leida ? 'bg-white' : 'bg-slate-50'}`}
                      >
                        <p className={`text-sm ${n.leida ? 'font-medium text-slate-700' : 'font-semibold text-navy'}`}>{n.titulo}</p>
                        <p className="mt-0.5 text-xs text-slate-600">{n.mensaje}</p>
                        <p className="mt-1 text-[11px] text-muted">{hace(n.fechaCreacion)}</p>
                      </button>
                    ))}
                  </div>
                </div>
              )}
            </div>
            <button
              type="button"
              className="hidden rounded-full p-2 text-slate-500 hover:bg-slate-100 sm:inline-flex"
              title="Guía rápida"
              onClick={() => setHelpOpen(true)}
            >
              <CircleHelp size={18} />
            </button>
            <button type="button" onClick={() => navigate('/perfil')} className="hidden text-right lg:block">
              <p className="max-w-36 truncate text-sm font-medium text-navy">{usuario?.nombreUsuario}</p>
              <p className="text-xs text-muted">{perfil || usuario?.rol}</p>
            </button>
            <button type="button" onClick={() => navigate('/perfil')} title="Ver perfil">
              <Avatar name={usuario?.nombreCompleto || usuario?.nombreUsuario} />
            </button>
            <button
              type="button"
              onClick={() => setLogoutOpen(true)}
              title="Cerrar sesión"
              className="rounded-full p-2 text-slate-500 hover:bg-slate-100"
            >
              <LogOut size={18} />
            </button>
          </div>
        </div>
        <form onSubmit={onSearch} className="border-t border-line px-3 py-2 md:hidden">
          <div className="relative">
            <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
            <input
              value={q}
              onChange={(e) => setQ(e.target.value)}
              placeholder="Buscar solicitud o colaborador..."
              className="w-full rounded-full bg-slate-100 pl-10"
            />
          </div>
        </form>
      </header>

      {open && (
        <div className="fixed inset-0 z-40 lg:hidden">
          <div className="absolute inset-0 bg-slate-900/30" onClick={() => setOpen(false)} />
          <div className="relative flex h-full w-[min(18rem,88vw)] flex-col bg-white">
            <div className="flex items-center justify-between border-b border-line px-4 py-3">
              <p className="text-sm font-semibold text-navy">Menú</p>
              <button type="button" className="text-slate-500" onClick={() => setOpen(false)}><X size={18} /></button>
            </div>
            <nav className="flex-1 overflow-y-auto px-3 py-4">
              <NavItems groups={menu} collapsed={collapsed} onToggle={toggleGroup} onNavigate={() => setOpen(false)} />
            </nav>
            <div className="border-t border-line">
              <button
                type="button"
                className="flex w-full items-center justify-between px-4 py-3 text-left text-sm text-navy hover:bg-slate-50"
                onClick={() => { setOpen(false); setNotifOpen(true); }}
              >
                Notificaciones
                {noLeidas > 0 && (
                  <span className="rounded-full bg-red-600 px-1.5 text-[10px] font-semibold leading-5 text-white">
                    {noLeidas > 99 ? '99+' : noLeidas}
                  </span>
                )}
              </button>
              {canInbox && (
                <button
                  type="button"
                  className="flex w-full items-center justify-between px-4 py-3 text-left text-sm text-navy hover:bg-slate-50"
                  onClick={() => { setOpen(false); navigate('/bandeja'); }}
                >
                  Bandeja
                </button>
              )}
              <button type="button" className="w-full px-4 py-3 text-left text-sm text-navy hover:bg-slate-50" onClick={() => { setOpen(false); setHelpOpen(true); }}>
                Guía rápida
              </button>
              <button type="button" className="w-full px-4 py-3 text-left text-sm text-navy hover:bg-slate-50" onClick={() => { setOpen(false); navigate('/perfil'); }}>
                Ver perfil
              </button>
              <button type="button" className="w-full px-4 py-3 text-left text-sm text-muted hover:bg-slate-50" onClick={() => { setOpen(false); setLogoutOpen(true); }}>
                Cerrar sesión
              </button>
            </div>
          </div>
        </div>
      )}

      {helpOpen && (
        <Modal title="Guía rápida" onClose={() => setHelpOpen(false)}>
          <div className="space-y-4 text-sm text-slate-600">
            <p>Sistema de RR. HH. de Consultora Contable Andina. Lo esencial para empezar:</p>
            <ul className="list-disc space-y-2 pl-5">
              <li><span className="font-medium text-navy">Buscar</span> en la barra superior filtra bandeja, permisos, horas extras o personal, según su perfil.</li>
              <li><span className="font-medium text-navy">Marcar</span> registra un ingreso y una salida por día hábil (hora de Lima). Fines de semana no aplica.</li>
              <li><span className="font-medium text-navy">Permisos y horas extras</span> se envían al flujo configurado. Siga el estado en el detalle de cada solicitud.</li>
              {canInbox && (
                <li><span className="font-medium text-navy">Bandeja</span> muestra los pasos que debe aprobar o rechazar. La campana avisa en el momento si le toca el siguiente paso.</li>
              )}
              <li><span className="font-medium text-navy">Mi perfil</span> (avatar) abre sus datos y permite cambiar la contraseña.</li>
              {canAccess('/maestros') && (
                <li><span className="font-medium text-navy">Maestros</span> mantiene áreas, cargos, horarios, tipos de permiso, parámetros y el plan de cuentas de planilla.</li>
              )}
            </ul>
            <p className="text-xs text-muted">Si un menú no aparece, su perfil no tiene acceso a ese módulo.</p>
          </div>
        </Modal>
      )}

      {logoutOpen && (
        <Modal title="Cerrar sesión" onClose={() => setLogoutOpen(false)}>
          <p className="text-sm text-slate-600">¿Desea salir de su cuenta en Consultora Andina?</p>
          <div className="mt-5 flex flex-wrap justify-end gap-2">
            <Button type="button" variant="secondary" onClick={() => setLogoutOpen(false)}>Cancelar</Button>
            <Button type="button" variant="danger" onClick={confirmLogout}>Cerrar sesión</Button>
          </div>
        </Modal>
      )}

      <main className="min-w-0 px-3 py-4 sm:px-4 sm:py-6 md:px-6">
        <Outlet />
      </main>
    </div>
  );
}
