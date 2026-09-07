import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ClipboardCheck, Clock3, Inbox, LogIn, Users } from 'lucide-react';
import { emptyPage, http, pagePath } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { menuIcon } from '../layout/icons';
import { Avatar, Badge } from '../components/ui';
import { TIPO, fmtDate, fmtDateTime } from './bandejaShared';

function fmtHora(iso) {
  if (!iso) return '—';
  return new Date(iso).toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit', timeZone: 'America/Lima' });
}

function todayIso() {
  return new Intl.DateTimeFormat('en-CA', {
    timeZone: 'America/Lima',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  }).format(new Date());
}

function esFinDeSemana() {
  const dia = new Intl.DateTimeFormat('en-US', { weekday: 'short', timeZone: 'America/Lima' }).format(new Date());
  return dia === 'Sat' || dia === 'Sun';
}

function saludo() {
  const hora = Number(new Intl.DateTimeFormat('en-US', {
    hour: 'numeric',
    hour12: false,
    timeZone: 'America/Lima'
  }).format(new Date()));
  if (hora < 12) return 'Buenos días';
  if (hora < 19) return 'Buenas tardes';
  return 'Buenas noches';
}

function primerNombre(usuario) {
  const full = usuario?.nombreCompleto?.trim();
  if (full) return full.split(/\s+/)[0];
  return usuario?.nombreUsuario || 'colaborador';
}

async function safePage(path, params) {
  try {
    return await http.page(pagePath(path, params));
  } catch {
    return emptyPage;
  }
}

export function Inicio() {
  const { usuario, perfil, menu, canAccess } = useAuth();
  const [now, setNow] = useState(new Date());
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState({
    bandeja: 0,
    permisosPend: 0,
    hextrasPend: 0,
    empleados: 0,
    usuarios: 0
  });
  const [bandeja, setBandeja] = useState([]);
  const [permisos, setPermisos] = useState([]);
  const [hextras, setHextras] = useState([]);
  const [hoy, setHoy] = useState([]);

  const canInbox = canAccess('/bandeja');
  const canPermisos = canAccess('/permisos');
  const canHextras = canAccess('/horas-extras');
  const canMarcar = canAccess('/marcar');
  const canAsistencia = canAccess('/asistencia') || canMarcar;
  const canPersonal = canAccess('/empleados');
  const canUsuarios = canAccess('/usuarios');
  const weekend = esFinDeSemana();
  const modules = (menu || []).flatMap((grupo) => grupo.items || []).filter((item) => item.ruta !== '/');

  useEffect(() => {
    const t = setInterval(() => setNow(new Date()), 30000);
    return () => clearInterval(t);
  }, []);

  useEffect(() => {
    let cancelled = false;
    const fecha = todayIso();

    async function load() {
      const tasks = [
        canInbox ? safePage('/api/bandeja', { page: 1, size: 5 }) : Promise.resolve(emptyPage),
        canPermisos ? safePage('/api/permisos', { page: 1, size: 1, estado: 'PENDIENTE' }) : Promise.resolve(emptyPage),
        canHextras ? safePage('/api/horas-extras', { page: 1, size: 1, estado: 'PENDIENTE' }) : Promise.resolve(emptyPage),
        canPermisos ? safePage('/api/permisos', { page: 1, size: 5 }) : Promise.resolve(emptyPage),
        canHextras ? safePage('/api/horas-extras', { page: 1, size: 5 }) : Promise.resolve(emptyPage),
        canAsistencia ? safePage('/api/asistencias', {
          page: 1,
          size: 10,
          desde: fecha,
          hasta: fecha,
          idEmpleado: usuario?.idEmpleado
        }) : Promise.resolve(emptyPage),
        canPersonal ? safePage('/api/empleados', { page: 1, size: 1 }) : Promise.resolve(emptyPage),
        canUsuarios ? safePage('/api/usuarios', { page: 1, size: 1 }) : Promise.resolve(emptyPage)
      ];
      const [b, pp, hp, pl, hl, marks, emp, usr] = await Promise.all(tasks);
      if (cancelled) return;
      setBandeja(b.content || []);
      setPermisos(pl.content || []);
      setHextras(hl.content || []);
      setHoy(marks.content || []);
      setStats({
        bandeja: b.totalElements || 0,
        permisosPend: pp.totalElements || 0,
        hextrasPend: hp.totalElements || 0,
        empleados: emp.totalElements || 0,
        usuarios: usr.totalElements || 0
      });
      setLoading(false);
    }

    setLoading(true);
    load();
    return () => { cancelled = true; };
  }, [usuario?.idEmpleado, canInbox, canPermisos, canHextras, canAsistencia, canPersonal, canUsuarios]);

  const ingreso = hoy.find((m) => m.tipo === 'INGRESO');
  const salida = hoy.find((m) => m.tipo === 'SALIDA');
  const hora = now.toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit', timeZone: 'America/Lima' });
  const fechaLarga = now.toLocaleDateString('es-PE', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    timeZone: 'America/Lima'
  });

  const acciones = [
    canPermisos && { to: '/permisos/nuevo', label: 'Nuevo permiso', hint: 'Registrar ausencia o trámite', icon: ClipboardCheck },
    canHextras && { to: '/horas-extras/nuevo', label: 'Horas extras', hint: 'Solicitar tiempo adicional', icon: Clock3 },
    canMarcar && { to: '/marcar', label: 'Marcar jornada', hint: weekend ? 'No disponible hoy' : 'Entrada o salida', icon: LogIn },
    canInbox && { to: '/bandeja', label: 'Revisar bandeja', hint: stats.bandeja ? `${stats.bandeja} por atender` : 'Sin pendientes', icon: Inbox },
    canPersonal && { to: '/empleados', label: 'Personal', hint: 'Directorio de colaboradores', icon: Users }
  ].filter(Boolean);

  const kpis = [
    canInbox && { label: 'Bandeja', value: stats.bandeja, hint: 'Pasos en curso', to: '/bandeja' },
    canPermisos && { label: 'Permisos', value: stats.permisosPend, hint: 'Pendientes de aprobación', to: '/permisos' },
    canHextras && { label: 'Horas extras', value: stats.hextrasPend, hint: 'Pendientes de aprobación', to: '/horas-extras' },
    canPersonal && { label: 'Colaboradores', value: stats.empleados, hint: 'En el directorio', to: '/empleados' },
    !canPersonal && canUsuarios && { label: 'Cuentas', value: stats.usuarios, hint: 'Usuarios activos y no', to: '/usuarios' }
  ].filter(Boolean).slice(0, 4);

  return (
    <div>
      <div className="mb-6 flex flex-col gap-4 border-b border-line pb-5 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-500">Panel de control</p>
          <h1 className="page-title mt-1">{saludo()}, {primerNombre(usuario)}</h1>
          <p className="mt-2 text-sm capitalize text-muted">{fechaLarga} · {perfil || usuario?.rol}</p>
        </div>
        <div className="rounded-xl border border-line bg-white px-4 py-3">
          <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-500">Hora Lima</p>
          <p className="mt-0.5 text-2xl font-bold tabular-nums text-navy">{hora}</p>
        </div>
      </div>

      {loading ? (
        <p className="text-sm text-muted">Cargando el panel…</p>
      ) : (
        <>
          {kpis.length > 0 && (
            <div className="mb-5 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
              {kpis.map((k) => (
                <Link key={k.label} to={k.to} className="rounded-xl border border-line bg-white p-4 hover:border-slate-300">
                  <p className="text-3xl font-bold text-navy">{k.value}</p>
                  <p className="mt-1 text-sm font-medium text-navy">{k.label}</p>
                  <p className="text-xs text-muted">{k.hint}</p>
                </Link>
              ))}
            </div>
          )}

          {acciones.length > 0 && (
            <div className="mb-5">
              <h2 className="mb-3 text-sm font-semibold text-navy">Acciones rápidas</h2>
              <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
                {acciones.map((a) => {
                  const Icon = a.icon;
                  return (
                    <Link key={a.to} to={a.to} className="flex items-start gap-3 rounded-xl border border-line bg-white p-4 hover:border-slate-300">
                      <span className="grid h-10 w-10 place-items-center rounded-lg bg-slate-100 text-navy">
                        <Icon size={18} />
                      </span>
                      <span>
                        <p className="font-semibold text-navy">{a.label}</p>
                        <p className="mt-0.5 text-xs text-muted">{a.hint}</p>
                      </span>
                    </Link>
                  );
                })}
              </div>
            </div>
          )}

          <div className="mb-5 grid gap-5 xl:grid-cols-5">
            <section className="xl:col-span-3">
              {canInbox ? (
                <PanelList
                  title="Por atender"
                  empty="No tiene pasos por revisar."
                  moreTo="/bandeja"
                  moreLabel="Ir a bandeja"
                >
                  {bandeja.map((it) => (
                    <Link
                      key={it.idPasoSolicitud}
                      to={`/bandeja/${it.idPasoSolicitud}`}
                      className="flex items-start gap-3 border-t border-line px-5 py-3 hover:bg-slate-50"
                    >
                      <Avatar name={it.solicitante} />
                      <div className="min-w-0 flex-1">
                        <p className="truncate text-sm font-medium text-navy">{it.solicitante}</p>
                        <p className="text-xs text-muted">{TIPO[it.tipoSolicitud] || it.tipoSolicitud} · {it.tipoTramite}</p>
                        <p className="mt-0.5 text-xs text-muted">Paso {it.numeroPaso}: {it.nombrePaso}</p>
                      </div>
                      <p className="shrink-0 text-xs text-muted">{fmtDateTime(it.fechaInicio)}</p>
                    </Link>
                  ))}
                </PanelList>
              ) : canPermisos ? (
                <PanelList
                  title="Mis permisos recientes"
                  empty="Aún no hay solicitudes de permiso."
                  moreTo="/permisos"
                  moreLabel="Ver permisos"
                >
                  {permisos.map((p) => (
                    <Link
                      key={p.idSolicitudPermiso}
                      to={`/permisos/${p.idSolicitudPermiso}`}
                      className="flex items-center justify-between gap-3 border-t border-line px-5 py-3 hover:bg-slate-50"
                    >
                      <div className="min-w-0">
                        <p className="truncate text-sm font-medium text-navy">{p.tipoPermiso}</p>
                        <p className="text-xs text-muted">{fmtDate(p.fechaInicio)} – {fmtDate(p.fechaFin)}</p>
                      </div>
                      <Badge value={p.estado} />
                    </Link>
                  ))}
                </PanelList>
              ) : (
                <div className="rounded-xl border border-line bg-white p-5">
                  <p className="text-sm font-semibold text-navy">Bienvenido</p>
                  <p className="mt-1 text-sm text-muted">Use el menú para abrir los módulos de su perfil.</p>
                </div>
              )}
            </section>

            <div className="space-y-5 xl:col-span-2">
              {canAsistencia && (
                <div className="rounded-xl border border-line bg-white p-5">
                  <div className="mb-3 flex items-center justify-between">
                    <h3 className="text-sm font-semibold text-navy">Jornada de hoy</h3>
                    {canMarcar && (
                      <Link to="/marcar" className="text-xs font-medium text-navy hover:underline">Marcar</Link>
                    )}
                  </div>
                  {weekend ? (
                    <p className="text-sm text-muted">Hoy es fin de semana. La marcación web está deshabilitada.</p>
                  ) : (
                    <div className="grid grid-cols-2 gap-3">
                      <MarcaBox label="Entrada" value={ingreso ? fmtHora(ingreso.fechaHora) : 'Pendiente'} done={Boolean(ingreso)} />
                      <MarcaBox label="Salida" value={salida ? fmtHora(salida.fechaHora) : 'Pendiente'} done={Boolean(salida)} />
                    </div>
                  )}
                </div>
              )}

              {canHextras && (
                <PanelList
                  title="Horas extras recientes"
                  empty="Sin solicitudes de horas extras."
                  moreTo="/horas-extras"
                  moreLabel="Ver horas extras"
                >
                  {hextras.map((h) => (
                    <Link
                      key={h.idSolicitudHoraExtra}
                      to={`/horas-extras/${h.idSolicitudHoraExtra}`}
                      className="flex items-center justify-between gap-3 border-t border-line px-5 py-3 hover:bg-slate-50"
                    >
                      <div className="min-w-0">
                        <p className="truncate text-sm font-medium text-navy">{h.empleado}</p>
                        <p className="text-xs text-muted">{fmtDate(h.fecha)} · {h.cantidadHoras} h</p>
                      </div>
                      <Badge value={h.estado} />
                    </Link>
                  ))}
                </PanelList>
              )}
            </div>
          </div>

          {canInbox && canPermisos && (
            <PanelList
              title="Permisos recientes"
              empty="Sin solicitudes de permiso."
              moreTo="/permisos"
              moreLabel="Ver permisos"
            >
              {permisos.map((p) => (
                <Link
                  key={p.idSolicitudPermiso}
                  to={`/permisos/${p.idSolicitudPermiso}`}
                  className="flex items-center justify-between gap-3 border-t border-line px-5 py-3 hover:bg-slate-50"
                >
                  <div className="min-w-0">
                    <p className="truncate text-sm font-medium text-navy">{p.empleado}</p>
                    <p className="text-xs text-muted">{p.tipoPermiso} · {fmtDate(p.fechaInicio)}</p>
                  </div>
                  <Badge value={p.estado} />
                </Link>
              ))}
            </PanelList>
          )}

          {modules.length > 0 && (
            <div className="mt-5">
              <h2 className="mb-3 text-sm font-semibold text-navy">Módulos de su perfil</h2>
              <div className="grid gap-2 sm:grid-cols-2 xl:grid-cols-3">
                {modules.map((m) => {
                  const Icon = menuIcon(m.icono);
                  return (
                    <Link
                      key={m.codigo}
                      to={m.ruta}
                      className="flex items-center gap-3 rounded-xl border border-line bg-white px-4 py-3 hover:border-slate-300"
                    >
                      <span className="grid h-9 w-9 place-items-center rounded-lg bg-slate-100 text-navy">
                        <Icon size={16} />
                      </span>
                      <span>
                        <p className="text-sm font-medium text-navy">{m.etiqueta}</p>
                        <p className="text-xs text-muted">{m.descripcion || m.ruta}</p>
                      </span>
                    </Link>
                  );
                })}
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
}

function PanelList({ title, empty, moreTo, moreLabel, children }) {
  const items = Array.isArray(children) ? children : children ? [children] : [];
  return (
    <section className="overflow-hidden rounded-xl border border-line bg-white">
      <div className="flex items-center justify-between px-5 py-4">
        <h3 className="text-sm font-semibold text-navy">{title}</h3>
        {moreTo && (
          <Link to={moreTo} className="text-xs font-medium text-navy hover:underline">{moreLabel}</Link>
        )}
      </div>
      {items.length === 0 ? (
        <p className="border-t border-line px-5 py-8 text-sm text-muted">{empty}</p>
      ) : items}
    </section>
  );
}

function MarcaBox({ label, value, done }) {
  return (
    <div className="rounded-lg bg-slate-50 px-3 py-3">
      <p className="text-xs text-slate-500">{label}</p>
      <p className={`mt-1 text-sm font-semibold ${done ? 'text-navy' : 'text-muted'}`}>{value}</p>
    </div>
  );
}
