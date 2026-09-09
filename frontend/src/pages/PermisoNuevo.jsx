import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import { http, pagePath, SELECT_SIZE, toTime } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Alert, Button, DatePicker, Field } from '../components/ui';
import { text } from '../lib/input';

const empty = {
  idEmpleado: '', idTipoPermiso: '', fechaInicio: '', fechaFin: '', horaInicio: '', horaFin: '', motivo: ''
};

export function PermisoNuevo() {
  const navigate = useNavigate();
  const { usuario, hasAnyRole } = useAuth();
  const puedeElegirEmpleado = hasAnyRole('ADMIN', 'RRHH');
  const [form, setForm] = useState(empty);
  const [empleados, setEmpleados] = useState([]);
  const [tipos, setTipos] = useState([]);
  const [saldo, setSaldo] = useState(null);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    Promise.all([
      http.get('/api/catalogos/tipos-permiso'),
      puedeElegirEmpleado
        ? http.page(pagePath('/api/empleados', { page: 1, size: SELECT_SIZE }))
        : Promise.resolve({ content: [] })
    ])
      .then(([t, emp]) => {
        setTipos(t);
        setEmpleados(emp.content || []);
      })
      .catch((e) => setError(e.message));
  }, [puedeElegirEmpleado]);

  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }));

  const tipoSel = tipos.find((t) => String(t.id) === String(form.idTipoPermiso));
  const esVacaciones = tipoSel?.codigo === 'VACACIONES';
  const idEmpleadoSaldo = form.idEmpleado || usuario?.idEmpleado;

  useEffect(() => {
    if (!esVacaciones || !idEmpleadoSaldo) {
      setSaldo(null);
      return;
    }
    http.get(`/api/contratos/saldo-vacaciones?idEmpleado=${idEmpleadoSaldo}`)
      .then(setSaldo)
      .catch((e) => setError(e.message));
  }, [esVacaciones, idEmpleadoSaldo]);

  async function crear(e) {
    e.preventDefault();
    setError('');
    if (form.fechaFin && form.fechaInicio && form.fechaFin < form.fechaInicio) {
      setError('La fecha de fin no puede ser anterior al inicio.');
      return;
    }
    if ((form.horaInicio && !form.horaFin) || (!form.horaInicio && form.horaFin)) {
      setError('Indique hora de inicio y de fin, o deje ambas vacías.');
      return;
    }
    if (form.motivo.trim().length < 5) {
      setError('El motivo debe tener al menos 5 caracteres.');
      return;
    }
    setSaving(true);
    try {
      const saved = await http.post('/api/permisos', {
        idEmpleado: form.idEmpleado ? Number(form.idEmpleado) : usuario.idEmpleado,
        idTipoPermiso: Number(form.idTipoPermiso),
        fechaInicio: form.fechaInicio,
        fechaFin: form.fechaFin,
        horaInicio: toTime(form.horaInicio),
        horaFin: toTime(form.horaFin),
        motivo: form.motivo.trim()
      });
      navigate(`/permisos/${saved.idSolicitudPermiso}`, {
        replace: true,
        state: { ok: 'Permiso registrado. El flujo de aprobación ya está en curso.' }
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
        onClick={() => navigate('/permisos')}
        className="mb-4 inline-flex items-center gap-1.5 text-sm text-slate-500 hover:text-navy"
      >
        <ArrowLeft size={16} /> Permisos
      </button>

      <div className="mb-6 flex flex-col gap-4 border-b border-line pb-5 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-500">Trámites</p>
          <h1 className="page-title mt-1">Nueva solicitud</h1>
          <p className="mt-2 text-sm text-muted">Al guardar se instancia el circuito configurado para ese tipo de permiso.</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button type="button" variant="secondary" onClick={() => navigate('/permisos')}>Cancelar</Button>
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
          <Field label="Tipo de permiso" full>
            <select value={form.idTipoPermiso} onChange={(e) => set('idTipoPermiso', e.target.value)} required>
              <option value="">Seleccione</option>
              {tipos.map((t) => <option key={t.id} value={t.id}>{t.nombre}</option>)}
            </select>
          </Field>
          {esVacaciones && (
            <div className="rounded-lg border border-line bg-slate-50 px-3 py-2 text-sm md:col-span-2">
              {saldo ? (
                <p>
                  Saldo de vacaciones: <strong>{saldo.diasDisponibles}</strong> días
                  {' '}({saldo.diasGanados} ganados en {saldo.mesesCompletos} meses · {saldo.tasaMensual} por mes).
                  {Number(saldo.diasDisponibles) <= 0 && (
                    <span className="mt-1 block text-red-600">Aún no ha ganado días. No puede registrar vacaciones.</span>
                  )}
                </p>
              ) : (
                <p className="text-muted">Consultando saldo de vacaciones…</p>
              )}
            </div>
          )}
          <Field label="Desde">
            <DatePicker value={form.fechaInicio} onChange={(v) => set('fechaInicio', v)} required />
          </Field>
          <Field label="Hasta">
            <DatePicker value={form.fechaFin} onChange={(v) => set('fechaFin', v)} required min={form.fechaInicio || undefined} />
          </Field>
          <Field label="Hora inicio">
            <input type="time" value={form.horaInicio} onChange={(e) => set('horaInicio', e.target.value)} />
          </Field>
          <Field label="Hora fin">
            <input type="time" value={form.horaFin} onChange={(e) => set('horaFin', e.target.value)} />
          </Field>
          <Field label="Motivo" full>
            <textarea value={form.motivo} onChange={(e) => set('motivo', text(e.target.value, 400))} required minLength={5} placeholder="Mínimo 5 caracteres" />
          </Field>
        </div>
        <p className="mt-4 text-xs text-muted">Las horas son opcionales. Si las indica, deben ir las dos.</p>
      </section>
    </form>
  );
}
