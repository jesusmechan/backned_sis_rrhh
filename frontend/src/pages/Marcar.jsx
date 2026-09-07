import { useEffect, useState } from 'react';
import { LogIn, LogOut } from 'lucide-react';
import { http, pagePath } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Alert, Avatar, Button } from '../components/ui';

const TIPOS = [
  { id: 'INGRESO', label: 'Entrada', hint: 'Inicio de jornada', icon: LogIn },
  { id: 'SALIDA', label: 'Salida', hint: 'Fin de jornada', icon: LogOut }
];

function todayIso() {
  return new Intl.DateTimeFormat('en-CA', {
    timeZone: 'America/Lima',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  }).format(new Date());
}

function esFinDeSemana(date = new Date()) {
  const dia = new Intl.DateTimeFormat('en-US', { weekday: 'short', timeZone: 'America/Lima' }).format(date);
  return dia === 'Sat' || dia === 'Sun';
}

function formatTime(iso) {
  if (!iso) return '—';
  return new Date(iso).toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' });
}

export function Marcar() {
  const { usuario } = useAuth();
  const [now, setNow] = useState(new Date());
  const [pick, setPick] = useState('INGRESO');
  const [hoy, setHoy] = useState([]);
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
    const marks = data.content;
    setHoy(marks);
    const tieneEntrada = marks.some((r) => r.tipo === 'INGRESO');
    const tieneSalida = marks.some((r) => r.tipo === 'SALIDA');
    setPick(tieneEntrada && !tieneSalida ? 'SALIDA' : 'INGRESO');
  }

  useEffect(() => { load().catch((e) => setError(e.message)); }, []);
  useEffect(() => {
    const id = setInterval(() => setNow(new Date()), 1000);
    return () => clearInterval(id);
  }, []);

  const entrada = hoy.find((r) => r.tipo === 'INGRESO');
  const salida = hoy.find((r) => r.tipo === 'SALIDA');
  const selected = TIPOS.find((t) => t.id === pick);
  const SelectedIcon = selected.icon;
  const already = pick === 'INGRESO' ? entrada : salida;
  const finDeSemana = esFinDeSemana(now);

  async function marcar() {
    setError('');
    setOk('');
    if (!usuario?.idEmpleado) {
      setError('Su usuario no está asociado a un trabajador.');
      return;
    }
    if (esFinDeSemana(now)) {
      setError('La marcación no está disponible sábados ni domingos.');
      return;
    }
    if (already) {
      setError(`Ya registró su ${selected.label.toLowerCase()} de hoy.`);
      return;
    }
    setSaving(true);
    try {
      await http.post('/api/asistencias/marcar', {
        idEmpleado: usuario.idEmpleado,
        tipo: selected.id,
        fechaHora: new Date().toISOString(),
        origen: 'WEB',
        observacion: null
      });
      setOk(`${selected.label} registrada.`);
      await load();
    } catch (e) {
      setError(e.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="mx-auto max-w-2xl">
      <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-500">Control de jornada</p>
          <h1 className="page-title mt-1">Marcar asistencia</h1>
          <p className="mt-2 text-sm text-muted">Elija entrada o salida. Una marca de cada tipo por día hábil.</p>
        </div>
        <div className="flex items-center gap-3 rounded-xl border border-line bg-white px-4 py-3 shadow-sm">
          <Avatar name={usuario?.nombreCompleto || usuario?.nombreUsuario} />
          <div>
            <p className="text-sm font-semibold text-navy">{usuario?.nombreCompleto || usuario?.nombreUsuario}</p>
            <p className="text-xs text-muted">{usuario?.nombreUsuario}</p>
          </div>
        </div>
      </div>

      <div className="mb-4 overflow-hidden rounded-2xl border border-line bg-navy px-4 py-5 text-white shadow-sm sm:px-6 sm:py-6">
        <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-300">Hora actual</p>
        <p className="mt-1 font-mono text-4xl font-semibold tracking-tight sm:text-5xl">
          {now.toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit', second: '2-digit' })}
        </p>
        <p className="mt-2 text-sm capitalize text-slate-300">
          {now.toLocaleDateString('es-PE', { weekday: 'long', day: 'numeric', month: 'long' })} · Lima
        </p>
      </div>

      <Alert>{error}</Alert>
      <Alert ok>{ok}</Alert>
      {finDeSemana && (
        <Alert>Hoy es fin de semana. La marcación de entrada y salida está deshabilitada.</Alert>
      )}

      <div className="rounded-2xl border border-line bg-white p-5 shadow-sm">
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          {TIPOS.map((t) => {
            const Icon = t.icon;
            const done = t.id === 'INGRESO' ? entrada : salida;
            const active = pick === t.id;
            return (
              <button
                key={t.id}
                type="button"
                disabled={finDeSemana}
                onClick={() => setPick(t.id)}
                className={`rounded-xl px-4 py-6 text-left transition-colors ${
                  finDeSemana
                    ? 'cursor-not-allowed bg-slate-50 text-slate-400 ring-1 ring-line'
                    : active ? 'bg-navy text-white' : 'bg-slate-50 text-slate-700 ring-1 ring-line hover:bg-slate-100'
                }`}
              >
                <div className="flex items-center justify-between">
                  <span className={`grid h-10 w-10 place-items-center rounded-lg ${active ? 'bg-white/10' : 'bg-white ring-1 ring-line'}`}>
                    <Icon size={18} />
                  </span>
                  <span className={`rounded-full px-2 py-0.5 text-[10px] font-semibold uppercase ${
                    done
                      ? active ? 'bg-white/15 text-white' : 'bg-sky-50 text-sky-800'
                      : active ? 'bg-white/15 text-white' : 'bg-slate-200 text-slate-600'
                  }`}>
                    {done ? 'Registrada' : 'Pendiente'}
                  </span>
                </div>
                <p className="mt-5 text-xl font-semibold">{t.label}</p>
                <p className={`mt-1 text-sm ${active ? 'text-slate-300' : 'text-muted'}`}>
                  {done ? `Hoy a las ${formatTime(done.fechaHora)}` : t.hint}
                </p>
              </button>
            );
          })}
        </div>
        <Button className="mt-5 w-full py-3.5 text-base" onClick={marcar} disabled={finDeSemana || !usuario?.idEmpleado || Boolean(already) || saving}>
          <SelectedIcon size={18} />
          {finDeSemana
            ? 'Marcación no disponible el fin de semana'
            : already ? `${selected.label} ya registrada` : `Registrar ${selected.label.toLowerCase()} ahora`}
        </Button>
      </div>
    </div>
  );
}
