import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { http, pagePath, SELECT_SIZE, toTime } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Alert, BackLink, Button, DatePicker, Field, TimePicker } from '../components/ui';
import { useDuplicarPrefill } from '../lib/useDuplicarPrefill';
import { decimal, text } from '../lib/input';

const empty = { idEmpleado: '', fecha: '', horaInicio: '', horaFin: '', cantidadHoras: '', motivo: '' };

function horasEntre(inicio, fin) {
  if (!inicio || !fin) return '';
  const [sh, sm] = inicio.split(':').map(Number);
  const [eh, em] = fin.split(':').map(Number);
  const diff = (eh * 60 + em) - (sh * 60 + sm);
  if (diff <= 0) return '';
  return String(Math.round((diff / 60) * 2) / 2);
}

function mondayOf(iso) {
  if (!iso) return '';
  const [y, m, d] = iso.split('-').map(Number);
  const date = new Date(y, m - 1, d);
  const day = date.getDay();
  date.setDate(date.getDate() + (day === 0 ? -6 : 1 - day));
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

function addDays(iso, n) {
  const [y, m, d] = iso.split('-').map(Number);
  const date = new Date(y, m - 1, d + n);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

export function HoraExtraNueva() {
  const navigate = useNavigate();
  const { usuario, hasAnyRole } = useAuth();
  const puedeElegirEmpleado = hasAnyRole('ADMIN', 'RRHH');
  const [form, setForm] = useDuplicarPrefill(empty);
  const [empleados, setEmpleados] = useState([]);
  const [existentes, setExistentes] = useState([]);
  const [maxDia, setMaxDia] = useState(4);
  const [maxSemana, setMaxSemana] = useState(12);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    http.get('/api/catalogos/parametros')
      .then((params) => {
        const list = Array.isArray(params) ? params : [];
        const dia = list.find((p) => p.clave === 'max_horas_extras_diarias');
        const sem = list.find((p) => p.clave === 'max_horas_extras_semanales');
        if (dia?.valor) setMaxDia(Number(dia.valor) || 4);
        if (sem?.valor) setMaxSemana(Number(sem.valor) || 12);
      })
      .catch(() => {});
  }, []);

  useEffect(() => {
    if (!puedeElegirEmpleado) return;
    http.page(pagePath('/api/empleados', { page: 1, size: SELECT_SIZE }))
      .then((emp) => setEmpleados(emp.content || []))
      .catch((e) => setError(e.message));
  }, [puedeElegirEmpleado]);

  const idEmpleadoSel = form.idEmpleado || usuario?.idEmpleado;

  useEffect(() => {
    if (!idEmpleadoSel) {
      setExistentes([]);
      return;
    }
    http.page(pagePath('/api/horas-extras', { page: 1, size: SELECT_SIZE }))
      .then((data) => setExistentes((data.content || []).filter((h) => String(h.idEmpleado) === String(idEmpleadoSel))))
      .catch(() => setExistentes([]));
  }, [idEmpleadoSel]);

  function set(k, v) {
    setForm((f) => {
      const next = { ...f, [k]: v };
      if (k === 'horaInicio' || k === 'horaFin') {
        const sugerida = horasEntre(next.horaInicio, next.horaFin);
        if (sugerida) next.cantidadHoras = sugerida;
      }
      return next;
    });
  }

  const vigentes = existentes.filter((h) => h.estado === 'PENDIENTE' || h.estado === 'APROBADO');
  const acumDia = form.fecha
    ? vigentes.filter((h) => h.fecha === form.fecha).reduce((s, h) => s + Number(h.cantidadHoras || 0), 0)
    : 0;
  const iniSem = mondayOf(form.fecha);
  const finSem = iniSem ? addDays(iniSem, 6) : '';
  const acumSem = iniSem
    ? vigentes.filter((h) => h.fecha >= iniSem && h.fecha <= finSem).reduce((s, h) => s + Number(h.cantidadHoras || 0), 0)
    : 0;
  const pedidas = Number(form.cantidadHoras) || 0;
  const restoDia = Math.max(0, maxDia - acumDia);
  const restoSem = Math.max(0, maxSemana - acumSem);

  async function crear(e) {
    e.preventDefault();
    setError('');
    if (form.horaFin && form.horaInicio && form.horaFin <= form.horaInicio) {
      setError('La hora de fin debe ser posterior a la de inicio.');
      return;
    }
    const horas = Number(form.cantidadHoras);
    if (!horas || horas <= 0 || horas > 8) {
      setError('La cantidad de horas debe ser mayor a 0 y como máximo 8.');
      return;
    }
    if (horas > restoDia) {
      setError(`El tope diario es ${maxDia} h. Ya tiene ${acumDia} h pendientes o aprobadas este día.`);
      return;
    }
    if (horas > restoSem) {
      setError(`El tope semanal es ${maxSemana} h. Ya tiene ${acumSem} h pendientes o aprobadas esta semana.`);
      return;
    }
    if (form.motivo.trim().length < 5) {
      setError('El motivo debe tener al menos 5 caracteres.');
      return;
    }
    setSaving(true);
    try {
      const saved = await http.post('/api/horas-extras', {
        idEmpleado: form.idEmpleado ? Number(form.idEmpleado) : usuario.idEmpleado,
        fecha: form.fecha,
        horaInicio: toTime(form.horaInicio),
        horaFin: toTime(form.horaFin),
        cantidadHoras: horas,
        motivo: form.motivo.trim()
      });
      navigate(`/horas-extras/${saved.idSolicitudHoraExtra}`, {
        replace: true,
        state: { ok: 'Horas extras registradas. El flujo de aprobación ya está en curso.' }
      });
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <form onSubmit={crear}>
      <BackLink to="/horas-extras">Horas extras</BackLink>

      <div className="mb-6 flex flex-col gap-4 border-b border-line pb-5 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-500">Trámites</p>
          <h1 className="page-title mt-1">Nueva solicitud</h1>
          <p className="mt-2 text-sm text-muted">Al guardar se instancia el circuito configurado para horas extras.</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button type="button" variant="secondary" onClick={() => navigate('/horas-extras')}>Cancelar</Button>
          <Button type="submit" disabled={saving}>{saving ? 'Registrando…' : 'Registrar'}</Button>
        </div>
      </div>

      <Alert>{error}</Alert>

      <section className="rounded-xl border border-line bg-white p-5">
        <p className="mb-4 text-sm font-semibold text-navy">Datos de la solicitud</p>
        <div className="grid gap-3 sm:grid-cols-2">
          {puedeElegirEmpleado && (
            <Field label="Empleado" full>
              <select value={form.idEmpleado} onChange={(e) => set('idEmpleado', e.target.value)} required>
                <option value="">Seleccione</option>
                {empleados.map((e) => <option key={e.idEmpleado} value={e.idEmpleado}>{e.nombreCompleto}</option>)}
              </select>
            </Field>
          )}
          <Field label="Fecha">
            <DatePicker value={form.fecha} onChange={(v) => set('fecha', v)} required />
          </Field>
          <Field label="Cantidad de horas">
            <input inputMode="decimal" value={form.cantidadHoras} onChange={(e) => set('cantidadHoras', decimal(e.target.value, 8))} required />
          </Field>
          <Field label="Desde">
            <TimePicker value={form.horaInicio} onChange={(v) => set('horaInicio', v)} required />
          </Field>
          <Field label="Hasta">
            <TimePicker value={form.horaFin} onChange={(v) => set('horaFin', v)} required />
          </Field>
          <Field label="Motivo" full>
            <textarea value={form.motivo} onChange={(e) => set('motivo', text(e.target.value, 400))} required minLength={5} placeholder="Mínimo 5 caracteres" />
          </Field>
        </div>
        <p className="mt-4 text-xs text-muted">
          Tope {maxDia} h al día y {maxSemana} h a la semana (pendientes + aprobadas).
          {form.fecha ? ` Disponible hoy: ${restoDia} h · esta semana: ${restoSem} h.` : ' Elija fecha para ver el saldo.'}
        </p>
      </section>
    </form>
  );
}
