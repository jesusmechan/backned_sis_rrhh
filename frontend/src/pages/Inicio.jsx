import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { emptyPage, http, pagePath } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Avatar, Badge, PageHeader, Panel } from '../components/ui';
import { fmtMoney } from '../lib/format';
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

function BarRow({ label, value, max, tone = 'bg-primary' }) {
  const width = max > 0 ? Math.max(value > 0 ? 6 : 0, Math.round((value / max) * 100)) : 0;
  return (
    <div>
      <div className="mb-1 flex items-baseline justify-between gap-2 text-xs">
        <span className="text-slate-600">{label}</span>
        <span className="tabular-nums font-medium text-navy">{value}</span>
      </div>
      <div className="h-1.5 overflow-hidden rounded-full bg-slate-100">
        <div className={`h-full rounded-full ${tone}`} style={{ width: `${width}%` }} />
      </div>
    </div>
  );
}

function TramiteBars({ title, to, pend, apro, rech }) {
  const max = Math.max(1, pend, apro, rech);
  return (
    <Panel>
      <div className="mb-4 flex items-center justify-between gap-2">
        <h3 className="text-sm font-semibold text-navy">{title}</h3>
        {to && <Link to={to} className="text-xs font-medium text-navy hover:underline">Ver listado</Link>}
      </div>
      <div className="space-y-3">
        <BarRow label="Pendientes" value={pend} max={max} tone="bg-warn" />
        <BarRow label="Aprobados" value={apro} max={max} tone="bg-ok" />
        <BarRow label="Rechazados" value={rech} max={max} tone="bg-danger" />
      </div>
    </Panel>
  );
}

export function Inicio() {
  const { usuario, perfil, canAccess } = useAuth();
  const [now, setNow] = useState(new Date());
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState({
    bandeja: 0,
    permisosPend: 0,
    permisosApro: 0,
    permisosRech: 0,
    hextrasPend: 0,
    hextrasApro: 0,
    hextrasRech: 0,
    empleados: 0,
    usuarios: 0,
    marcacionesHoy: 0
  });
  const [bandeja, setBandeja] = useState([]);
  const [permisos, setPermisos] = useState([]);
  const [hextras, setHextras] = useState([]);
  const [hoy, setHoy] = useState([]);
  const [planilla, setPlanilla] = useState(null);

  const canInbox = canAccess('/bandeja');
  const canPermisos = canAccess('/permisos');
  const canHextras = canAccess('/horas-extras');
  const canMarcar = canAccess('/marcar');
  const canAsistencia = canAccess('/asistencia') || canMarcar;
  const canTeamAsist = canAccess('/asistencia');
  const canPersonal = canAccess('/empleados');
  const canUsuarios = canAccess('/usuarios');
  const canPlanillas = canAccess('/planillas');
  const weekend = esFinDeSemana();

  useEffect(() => {
    const t = setInterval(() => setNow(new Date()), 30000);
    return () => clearInterval(t);
  }, []);

  useEffect(() => {
    let cancelled = false;
    const fecha = todayIso();

    async function load() {
      const tasks = [
        canInbox ? safePage('/api/bandeja', { page: 1, size: 6 }) : Promise.resolve(emptyPage),
        canPermisos ? safePage('/api/permisos', { page: 1, size: 1, estado: 'PENDIENTE' }) : Promise.resolve(emptyPage),
        canPermisos ? safePage('/api/permisos', { page: 1, size: 1, estado: 'APROBADO' }) : Promise.resolve(emptyPage),
        canPermisos ? safePage('/api/permisos', { page: 1, size: 1, estado: 'RECHAZADO' }) : Promise.resolve(emptyPage),
        canHextras ? safePage('/api/horas-extras', { page: 1, size: 1, estado: 'PENDIENTE' }) : Promise.resolve(emptyPage),
        canHextras ? safePage('/api/horas-extras', { page: 1, size: 1, estado: 'APROBADO' }) : Promise.resolve(emptyPage),
        canHextras ? safePage('/api/horas-extras', { page: 1, size: 1, estado: 'RECHAZADO' }) : Promise.resolve(emptyPage),
        canPermisos ? safePage('/api/permisos', { page: 1, size: 6 }) : Promise.resolve(emptyPage),
        canHextras ? safePage('/api/horas-extras', { page: 1, size: 6 }) : Promise.resolve(emptyPage),
        canAsistencia ? safePage('/api/asistencias', {
          page: 1,
          size: 10,
          desde: fecha,
          hasta: fecha,
          idEmpleado: usuario?.idEmpleado
        }) : Promise.resolve(emptyPage),
        canTeamAsist ? safePage('/api/asistencias', { page: 1, size: 1, desde: fecha, hasta: fecha }) : Promise.resolve(emptyPage),
        canPersonal ? safePage('/api/empleados', { page: 1, size: 1 }) : Promise.resolve(emptyPage),
        canUsuarios ? safePage('/api/usuarios', { page: 1, size: 1 }) : Promise.resolve(emptyPage),
        canPlanillas ? safePage('/api/planillas', { page: 1, size: 1 }) : Promise.resolve(emptyPage)
      ];
      const [
        b, pp, pa, pr, hp, ha, hr, pl, hl, marks, teamMarks, emp, usr, pla
      ] = await Promise.all(tasks);
      if (cancelled) return;
      setBandeja(b.content || []);
      setPermisos(pl.content || []);
      setHextras(hl.content || []);
      setHoy(marks.content || []);
      setPlanilla((pla.content || [])[0] || null);
      setStats({
        bandeja: b.totalElements || 0,
        permisosPend: pp.totalElements || 0,
        permisosApro: pa.totalElements || 0,
        permisosRech: pr.totalElements || 0,
        hextrasPend: hp.totalElements || 0,
        hextrasApro: ha.totalElements || 0,
        hextrasRech: hr.totalElements || 0,
        empleados: emp.totalElements || 0,
        usuarios: usr.totalElements || 0,
        marcacionesHoy: teamMarks.totalElements || 0
      });
      setLoading(false);
    }

    setLoading(true);
    load();
    function onVis() {
      if (document.visibilityState === 'visible') load();
    }
    document.addEventListener('visibilitychange', onVis);
    window.addEventListener('focus', onVis);
    const poll = setInterval(load, 60000);
    return () => {
      cancelled = true;
      document.removeEventListener('visibilitychange', onVis);
      window.removeEventListener('focus', onVis);
      clearInterval(poll);
    };
  }, [
    usuario?.idEmpleado, canInbox, canPermisos, canHextras, canAsistencia,
    canTeamAsist, canPersonal, canUsuarios, canPlanillas
  ]);

  const ingreso = hoy.find((m) => m.tipo === 'INGRESO');
  const salida = hoy.find((m) => m.tipo === 'SALIDA');
  const hora = now.toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit', timeZone: 'America/Lima' });
  const fechaLarga = now.toLocaleDateString('es-PE', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    timeZone: 'America/Lima'
  });

  const kpis = [
    canInbox && { label: 'Bandeja', value: stats.bandeja, hint: 'Pasos por atender', to: '/bandeja' },
    canPermisos && { label: 'Permisos', value: stats.permisosPend, hint: 'Pendientes de aprobación', to: '/permisos' },
    canHextras && { label: 'Horas extras', value: stats.hextrasPend, hint: 'Pendientes de aprobación', to: '/horas-extras' },
    canPersonal && { label: 'Colaboradores', value: stats.empleados, hint: 'En el directorio', to: '/empleados' },
    canTeamAsist && { label: 'Marcaciones hoy', value: stats.marcacionesHoy, hint: 'Registros del día', to: '/asistencia' },
    !canPersonal && canUsuarios && { label: 'Cuentas', value: stats.usuarios, hint: 'Usuarios del sistema', to: '/usuarios' }
  ].filter(Boolean).slice(0, 4);

  return (
    <div>
      <PageHeader
        kicker="Panel"
        title={`${saludo()}, ${primerNombre(usuario)}`}
        subtitle={`${fechaLarga} · ${perfil || usuario?.rol}`}
        actions={(
          <div className="rounded-xl border border-line bg-white px-4 py-2.5 text-right">
            <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-500">Hora Lima</p>
            <p className="text-xl font-bold tabular-nums text-navy">{hora}</p>
          </div>
        )}
      />

      {loading ? (
        <p className="text-sm text-muted">Cargando el panel…</p>
      ) : (
        <>
          {kpis.length > 0 && (
            <div className="mb-5 grid grid-cols-2 gap-3 lg:grid-cols-4">
              {kpis.map((k) => (
                <Link key={k.label} to={k.to} className="rounded-xl border border-line bg-white px-4 py-3 hover:border-slate-300">
                  <p className="text-xl font-bold tabular-nums text-navy">{k.value}</p>
                  <p className="mt-0.5 text-sm font-medium text-navy">{k.label}</p>
                  <p className="text-xs text-muted">{k.hint}</p>
                </Link>
              ))}
            </div>
          )}

          {(canPermisos || canHextras) && (
            <div className="mb-5 grid gap-3 md:grid-cols-2">
              {canPermisos && (
                <TramiteBars
                  title="Permisos"
                  to="/permisos"
                  pend={stats.permisosPend}
                  apro={stats.permisosApro}
                  rech={stats.permisosRech}
                />
              )}
              {canHextras && (
                <TramiteBars
                  title="Horas extras"
                  to="/horas-extras"
                  pend={stats.hextrasPend}
                  apro={stats.hextrasApro}
                  rech={stats.hextrasRech}
                />
              )}
            </div>
          )}

          <div className="grid gap-5 lg:grid-cols-12">
            <div className="space-y-5 lg:col-span-7">
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
                <Panel>
                  <p className="text-sm font-semibold text-navy">Resumen</p>
                  <p className="mt-1 text-sm text-muted">Los indicadores de su perfil aparecen arriba. Use el menú para abrir cada módulo.</p>
                </Panel>
              )}

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
            </div>

            <div className="space-y-5 lg:col-span-5">
              {canAsistencia && (
                <Panel>
                  <div className="mb-4 flex items-center justify-between">
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
                </Panel>
              )}

              {canPlanillas && planilla && (
                <Panel>
                  <div className="mb-3 flex items-center justify-between gap-2">
                    <h3 className="text-sm font-semibold text-navy">Última planilla</h3>
                    <Link to={`/planillas/${planilla.idPlanilla}`} className="text-xs font-medium text-navy hover:underline">
                      Ver boletas
                    </Link>
                  </div>
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <p className="font-medium text-navy">{planilla.periodo}</p>
                      <p className="mt-1 text-sm text-muted">Neto {fmtMoney(planilla.totalNeto)}</p>
                    </div>
                    <Badge value={planilla.estado} />
                  </div>
                </Panel>
              )}

              {canHextras && (
                <PanelList
                  title="Horas extras recientes"
                  empty="Sin solicitudes de horas extras."
                  moreTo="/horas-extras"
                  moreLabel="Ver listado"
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
        </>
      )}
    </div>
  );
}

function PanelList({ title, empty, moreTo, moreLabel, children }) {
  const items = Array.isArray(children) ? children : children ? [children] : [];
  return (
    <section className="overflow-hidden rounded-xl border border-line bg-white shadow-sm">
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
    <div className={`rounded-lg px-3 py-3 ${done ? 'bg-ok-soft' : 'bg-slate-50'}`}>
      <p className="text-xs text-slate-500">{label}</p>
      <p className={`mt-1 text-sm font-semibold ${done ? 'text-ok' : 'text-muted'}`}>{value}</p>
    </div>
  );
}
