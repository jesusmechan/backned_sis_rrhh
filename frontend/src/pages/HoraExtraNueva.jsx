import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import { http, pagePath, SELECT_SIZE, toTime } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Alert, Button, Field } from '../components/ui';

const empty = { idEmpleado: '', fecha: '', horaInicio: '', horaFin: '', cantidadHoras: '', motivo: '' };

function horasEntre(inicio, fin) {
  if (!inicio || !fin) return '';
  const [sh, sm] = inicio.split(':').map(Number);
  const [eh, em] = fin.split(':').map(Number);
  const diff = (eh * 60 + em) - (sh * 60 + sm);
  if (diff <= 0) return '';
  return String(Math.round((diff / 60) * 2) / 2);
}

export function HoraExtraNueva() {
  const navigate = useNavigate();
  const { usuario, hasAnyRole } = useAuth();
  const puedeElegirEmpleado = hasAnyRole('ADMIN', 'RRHH');
  const [form, setForm] = useState(empty);
  const [empleados, setEmpleados] = useState([]);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!puedeElegirEmpleado) return;
    http.page(pagePath('/api/empleados', { page: 1, size: SELECT_SIZE }))
      .then((emp) => setEmpleados(emp.content || []))
      .catch((e) => setError(e.message));
  }, [puedeElegirEmpleado]);

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
      <button
        type="button"
        onClick={() => navigate('/horas-extras')}
        className="mb-4 inline-flex items-center gap-1.5 text-sm text-slate-500 hover:text-navy"
      >
        <ArrowLeft size={16} /> Horas extras
      </button>

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
            <input type="date" value={form.fecha} onChange={(e) => set('fecha', e.target.value)} required />
          </Field>
          <Field label="Cantidad de horas">
            <input type="number" step="0.5" min="0.5" max="8" value={form.cantidadHoras} onChange={(e) => set('cantidadHoras', e.target.value)} required />
          </Field>
          <Field label="Desde">
            <input type="time" value={form.horaInicio} onChange={(e) => set('horaInicio', e.target.value)} required />
          </Field>
          <Field label="Hasta">
            <input type="time" value={form.horaFin} onChange={(e) => set('horaFin', e.target.value)} required />
          </Field>
          <Field label="Motivo" full>
            <textarea value={form.motivo} onChange={(e) => set('motivo', e.target.value)} required minLength={5} placeholder="Mínimo 5 caracteres" />
          </Field>
        </div>
        <p className="mt-4 text-xs text-muted">La hora de fin debe ser posterior a la de inicio. Máximo 8 horas por solicitud.</p>
      </section>
    </form>
  );
}
