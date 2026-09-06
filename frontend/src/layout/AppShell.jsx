import { useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { Bell, CircleHelp, LogOut, Menu, PanelLeftClose, Search, X } from 'lucide-react';
import { useAuth } from '../auth/AuthContext';
import { Avatar } from '../components/ui';
import { menuIcon } from './icons';

const SIDEBAR_KEY = 'andina.sidebar';

function readSidebar() {
  return localStorage.getItem(SIDEBAR_KEY) !== 'hidden';
}

function formatDate() {
  return new Date().toLocaleDateString('es-PE', { weekday: 'long', day: 'numeric', month: 'long' });
}

function NavItems({ groups, onNavigate }) {
  return (groups || []).filter((group) => (group.items || []).length > 0).map((group) => (
    <div key={group.grupo} className="mb-5">
      <p className="mb-1.5 px-3 text-[11px] font-semibold uppercase tracking-[0.14em] text-slate-400">
        {group.grupo}
      </p>
      <div className="space-y-0.5">
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
    </div>
  ));
}

export function AppShell() {
  const { usuario, perfil, menu, logout, canAccess } = useAuth();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [sidebar, setSidebar] = useState(readSidebar);
  const [q, setQ] = useState('');

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
    if (canAccess('/bandeja')) navigate('/bandeja');
    else if (canAccess('/permisos')) navigate('/permisos');
  }

  return (
    <div className={`min-h-screen bg-surface ${sidebar ? 'lg:pl-60' : ''}`}>
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
          <NavItems groups={menu} />
        </nav>
        <div className="border-t border-line px-3 py-3">
          <NavLink to="/perfil" className="block rounded-lg px-2 py-1.5 hover:bg-slate-100">
            <p className="truncate text-sm font-medium text-navy">{usuario?.nombreUsuario}</p>
            <p className="text-xs text-muted">{perfil || usuario?.rol}</p>
          </NavLink>
        </div>
      </aside>

      <header className="sticky top-0 z-20 border-b border-line bg-white">
        <div className="flex items-center gap-4 px-4 py-3 md:px-6">
          <button
            type="button"
            className={`rounded-lg border border-line p-2 ${sidebar ? 'lg:hidden' : ''}`}
            title="Mostrar menú"
            onClick={onMenuButton}
          >
            <Menu size={18} />
          </button>
          <p className="hidden capitalize text-sm text-muted md:block">{formatDate()}</p>
          <form onSubmit={onSearch} className="relative mx-auto hidden max-w-xl flex-1 sm:block">
            <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
            <input
              value={q}
              onChange={(e) => setQ(e.target.value)}
              placeholder="Buscar solicitud, empleado o código..."
              className="w-full rounded-full bg-slate-100 pl-10"
            />
          </form>
          <div className="ml-auto flex items-center gap-3">
            <button type="button" className="relative rounded-full p-2 text-slate-500 hover:bg-slate-100">
              <Bell size={18} />
              <span className="absolute right-1.5 top-1.5 h-2 w-2 rounded-full bg-red-500" />
            </button>
            <button type="button" className="rounded-full p-2 text-slate-500 hover:bg-slate-100">
              <CircleHelp size={18} />
            </button>
            <button type="button" onClick={() => navigate('/perfil')} className="hidden text-right sm:block">
              <p className="text-sm font-medium text-navy">{usuario?.nombreUsuario}</p>
              <p className="text-xs text-muted">{perfil || usuario?.rol}</p>
            </button>
            <button type="button" onClick={() => navigate('/perfil')} title="Ver perfil">
              <Avatar name={usuario?.nombreCompleto || usuario?.nombreUsuario} />
            </button>
            <button type="button" onClick={logout} title="Cerrar sesión" className="rounded-full p-2 text-slate-500 hover:bg-slate-100">
              <LogOut size={18} />
            </button>
          </div>
        </div>
      </header>

      {open && (
        <div className="fixed inset-0 z-40 lg:hidden">
          <div className="absolute inset-0 bg-slate-900/30" onClick={() => setOpen(false)} />
          <div className="relative flex h-full w-64 flex-col bg-white">
            <div className="flex items-center justify-between border-b border-line px-4 py-3">
              <p className="text-sm font-semibold text-navy">Menú</p>
              <button type="button" className="text-slate-500" onClick={() => setOpen(false)}><X size={18} /></button>
            </div>
            <nav className="flex-1 overflow-y-auto px-3 py-4">
              <NavItems groups={menu} onNavigate={() => setOpen(false)} />
            </nav>
            <div className="border-t border-line">
              <button type="button" className="w-full px-4 py-3 text-left text-sm text-navy hover:bg-slate-50" onClick={() => { setOpen(false); navigate('/perfil'); }}>
                Ver perfil
              </button>
              <button type="button" className="w-full px-4 py-3 text-left text-sm text-muted hover:bg-slate-50" onClick={logout}>
                Cerrar sesión
              </button>
            </div>
          </div>
        </div>
      )}

      <main className="px-4 py-6 md:px-6">
        <Outlet />
      </main>
    </div>
  );
}
