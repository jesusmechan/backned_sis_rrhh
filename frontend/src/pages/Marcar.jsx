import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { CalendarDays, CheckCircle2, Clock3, LogIn, LogOut } from 'lucide-react';
import { http, pagePath } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Alert, Avatar, Button } from '../components/ui';

const LIMA = { timeZone: 'America/Lima' };

const TIPOS = [
  { id: 'INGRESO', label: 'Entrada', hint: 'Inicio de jornada', icon: LogIn },
  { id: 'SALIDA', label: 'Salida', hint: 'Fin de jornada', icon: LogOut }
];

function todayIso() {
  return new Intl.DateTimeFormat('en-CA', { ...LIMA, year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date());
}

function esFinDeSemana(date = new Date()) {
  const dia = new Intl.DateTimeFormat('en-US', { weekday: 'short', ...LIMA }).format(date);
  return dia === 'Sat' || dia === 'Sun';
}

function formatTime(iso) {
  if (!iso) return '—';
  return new Date(iso).toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit', ...LIMA });
}

function formatDuration(ms) {
  if (!Number.isFinite(ms) || ms < 0) ms = 0;
  const totalMin = Math.floor(ms / 60000);
  const h = Math.floor(totalMin / 60);
  const m = totalMin % 60;
  if (h === 0) return `${m} min`;
  return `${h} h ${String(m).padStart(2, '0')} min`;
}

export function Marcar() {
  const { usuario, canAccess } = useAuth();
  const [now, setNow] = useState(new Date());
  const [hoy, setHoy] = useState([]);
  const [horario, setHorario] = useState('');
  const [cargo, setCargo] = useState('');
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');
  const [saving, setSaving] = useState(false);

  async function load() {
    const fecha = todayIso();
    const data = await http.page(pagePath('/api/asistencias', {
      desde: fecha,
      hasta: fecha,
      page: 1,
      size: 10,
      idEmpleado: usuario?.idEmpleado
    }));
    setHoy(data.content);
    if (usuario?.idEmpleado) {
      try {
        const emp = await http.get(`/api/empleados/${usuario.idEmpleado}`);
        setHorario(emp.horario || '');
        setCargo(emp.cargo || '');
      } catch {
        setHorario('');
        setCargo('');
      }
    }
  }

  useEffect(() => { load().catch((e) => setError(e.message)); }, []);
  useEffect(() => {
    const id = setInterval(() => setNow(new Date()), 1000);
    return () => clearInterval(id);
  }, []);

  const entrada = hoy.find((r) => r.tipo === 'INGRESO');
  const salida = hoy.find((r) => r.tipo === 'SALIDA');
  const finDeSemana = esFinDeSemana(now);
  const jornadaCerrada = Boolean(entrada && salida);
  const siguiente = finDeSemana ? null : (!entrada ? 'INGRESO' : (!salida ? 'SALIDA' : null));
  const nextTipo = TIPOS.find((t) => t.id === siguiente);
  const NextIcon = nextTipo?.icon || CheckCircle2;

  const trabajadoMs = useMemo(() => {
    if (!entrada) return 0;
    const start = new Date(entrada.fechaHora).getTime();
    const end = salida ? new Date(salida.fechaHora).getTime() : now.getTime();
    return end - start;
  }, [entrada, salida, now]);

  const estadoDia = finDeSemana
    ? 'Fin de semana'
    : jornadaCerrada
      ? 'Jornada completa'
      : entrada
        ? 'En jornada'
        : 'Sin marcar';

  async function marcar() {
    setError('');
    setOk('');
    if (!usuario?.idEmpleado) {
      setError('Su usuario no está asociado a un trabajador.');
      return;
    }
    if (finDeSemana) {
      setError('La marcación no está disponible sábados ni domingos.');
      return;
    }
    if (!nextTipo) {
      setError('La entrada y la salida de hoy ya están registradas.');
      return;
    }
    setSaving(true);
    try {
      await http.post('/api/asistencias/marcar', {
        idEmpleado: usuario.idEmpleado,
        tipo: nextTipo.id,
        fechaHora: new Date().toISOString(),
        origen: 'WEB',
        observacion: null
      });
      setOk(`${nextTipo.label} registrada.`);
      await load();
    } catch (e) {
      setError(e.message);
    } finally {
      setSaving(false);
    }
  }

  const hora = now.toLocaleTimeString('es-PE', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
    ...LIMA
  });
  const fechaLarga = now.toLocaleDateString('es-PE', { weekday: 'long', day: 'numeric', month: 'long', ...LIMA });

  return (
    <div className="mx-auto max-w-4xl">
      <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-500">Control de jornada</p>
          <h1 className="page-title mt-1">Marcar asistencia</h1>
          <p className="mt-2 text-sm text-muted">
            Un ingreso y una salida por día hábil, con hora de Lima. El botón registra el siguiente paso.
          </p>
        </div>
        {canAccess('/asistencia') && (
          <Link to="/asistencia">
            <Button variant="secondary">Ver historial</Button>
          </Link>
        )}
      </div>

      <Alert>{error}</Alert>
      <Alert ok>{ok}</Alert>
      {finDeSemana && (
        <Alert>Hoy es fin de semana. La marcación de entrada y salida está deshabilitada.</Alert>
      )}

      <div className="grid gap-4 md:grid-cols-2">
        <section className="overflow-hidden rounded-2xl border border-line bg-navy px-5 py-6 text-white shadow-sm sm:px-7 sm:py-7">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-300">Hora Lima</p>
              <p className="mt-1 font-mono text-4xl font-semibold tracking-tight tabular-nums sm:text-5xl">{hora}</p>
              <p className="mt-2 text-sm capitalize text-slate-300">{fechaLarga}</p>
            </div>
            <span className={`rounded-full px-2.5 py-1 text-[11px] font-semibold uppercase tracking-wide ${
              finDeSemana ? 'bg-white/10 text-slate-200' : jornadaCerrada ? 'bg-emerald-400/20 text-emerald-100' : 'bg-white/15 text-white'
            }`}>
              {estadoDia}
            </span>
          </div>

          <div className="mt-6 flex items-center gap-3 rounded-xl bg-white/10 px-4 py-3 ring-1 ring-white/10">
            <Avatar name={usuario?.nombreCompleto || usuario?.nombreUsuario} className="bg-white/15 text-white" />
            <div className="min-w-0">
              <p className="truncate text-sm font-semibold">{usuario?.nombreCompleto || usuario?.nombreUsuario}</p>
              <p className="truncate text-xs text-slate-300">
                {[cargo, horario].filter(Boolean).join(' · ') || usuario?.nombreUsuario}
              </p>
            </div>
          </div>

          <div className="mt-6 grid grid-cols-2 gap-3 text-sm">
            <div className="rounded-xl bg-white/10 px-4 py-3 ring-1 ring-white/10">
              <p className="text-[11px] font-semibold uppercase tracking-[0.14em] text-slate-300">Tiempo de hoy</p>
              <p className="mt-1 text-lg font-semibold tabular-nums">{entrada ? formatDuration(trabajadoMs) : '—'}</p>
              <p className="mt-0.5 text-xs text-slate-400">{salida ? 'Jornada cerrada' : entrada ? 'En curso' : 'Aún no inicia'}</p>
            </div>
            <div className="rounded-xl bg-white/10 px-4 py-3 ring-1 ring-white/10">
              <p className="text-[11px] font-semibold uppercase tracking-[0.14em] text-slate-300">Horario</p>
              <p className="mt-1 text-lg font-semibold leading-tight">{horario || 'Según ficha'}</p>
              <p className="mt-0.5 text-xs text-slate-400">Lunes a viernes</p>
            </div>
          </div>
        </section>

        <section className="rounded-2xl border border-line bg-white p-5 shadow-sm">
          <p className="text-sm font-semibold text-navy">Estado del día</p>
          <p className="mt-1 text-xs text-muted">La marcación avanza sola: primero entrada, luego salida.</p>

          <ol className="mt-4 space-y-3">
            {TIPOS.map((t, i) => {
              const Icon = t.icon;
              const done = t.id === 'INGRESO' ? entrada : salida;
              const current = siguiente === t.id;
              return (
                <li
                  key={t.id}
                  className={`flex items-center gap-3 rounded-xl px-3 py-3 ring-1 ${
                    done
                      ? 'bg-emerald-50 ring-emerald-100'
                      : current
                        ? 'bg-slate-50 ring-navy/20'
                        : 'bg-slate-50 ring-line'
                  }`}
                >
                  <span className={`grid h-10 w-10 place-items-center rounded-lg ${
                    done ? 'bg-white text-ok' : current ? 'bg-navy text-white' : 'bg-white text-slate-400 ring-1 ring-line'
                  }`}>
                    {done ? <CheckCircle2 size={18} /> : <Icon size={18} />}
                  </span>
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-semibold text-navy">{t.label}</p>
                    <p className="text-xs text-muted">
                      {done ? `Registrada a las ${formatTime(done.fechaHora)}` : current ? 'Siguiente paso' : t.hint}
                    </p>
                  </div>
                  <span className="text-[10px] font-semibold uppercase tracking-wide text-slate-400">{i + 1}/2</span>
                </li>
              );
            })}
          </ol>

          {jornadaCerrada ? (
            <div className="mt-5 rounded-xl bg-emerald-50 px-4 py-4 text-center">
              <p className="text-sm font-semibold text-ok">Jornada de hoy completa</p>
              <p className="mt-1 text-xs text-emerald-800">
                {formatTime(entrada.fechaHora)} – {formatTime(salida.fechaHora)} · {formatDuration(trabajadoMs)}
              </p>
              {canAccess('/asistencia') && (
                <Link to="/asistencia" className="mt-3 inline-block text-xs font-medium text-navy hover:underline">
                  Revisar historial de asistencia
                </Link>
              )}
            </div>
          ) : (
            <Button
              className="mt-5 w-full py-3.5 text-base"
              onClick={marcar}
              disabled={finDeSemana || !usuario?.idEmpleado || !nextTipo || saving}
            >
              <NextIcon size={18} />
              {finDeSemana
                ? 'No disponible el fin de semana'
                : saving
                  ? 'Registrando…'
                  : `Registrar ${nextTipo?.label?.toLowerCase() || 'marcación'} ahora`}
            </Button>
          )}
        </section>
      </div>

      <div className="mt-4 grid gap-3 sm:grid-cols-3">
        <Hint icon={<CalendarDays size={16} />} title="Día hábil" text="Sábados y domingo no se marca por web." />
        <Hint icon={<Clock3 size={16} />} title="Una marca por tipo" text="No se puede repetir la entrada ni la salida del mismo día." />
        <Hint icon={<LogIn size={16} />} title="Origen WEB" text="El registro usa la hora oficial America/Lima." />
      </div>
    </div>
  );
}

function Hint({ icon, title, text }) {
  return (
    <div className="flex items-start gap-3 rounded-xl border border-line bg-white px-4 py-3">
      <span className="mt-0.5 text-slate-400">{icon}</span>
      <div>
        <p className="text-sm font-medium text-navy">{title}</p>
        <p className="text-xs text-muted">{text}</p>
      </div>
    </div>
  );
}
