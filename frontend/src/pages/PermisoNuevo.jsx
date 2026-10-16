import { useEffect, useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { api, http, pagePath, SELECT_SIZE, toTime } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { useConfig } from '../auth/ConfigContext';
import { Alert, BackLink, Button, DatePicker, Field, TimePicker } from '../components/ui';
import { Dropzone } from '../components/Dropzone';
import { useDuplicarPrefill } from '../lib/useDuplicarPrefill';
import { text } from '../lib/input';

const empty = {
  idEmpleado: '', idTipoPermiso: '', fechaInicio: '', fechaFin: '', horaInicio: '', horaFin: '', motivo: ''
};

const MAX_ARCHIVOS = 3;
const MAX_BYTES = 5 * 1024 * 1024;
const EXTENSIONES = ['pdf', 'png', 'jpg', 'jpeg', 'webp'];

function extensionDe(nombre) {
  const i = String(nombre || '').lastIndexOf('.');
  return i < 0 ? '' : nombre.slice(i + 1).toLowerCase();
}

export function PermisoNuevo() {
  const navigate = useNavigate();
  const location = useLocation();
  const { usuario, hasPermission } = useAuth();
  const { num } = useConfig();
  const puedeElegirEmpleado = hasPermission('ALCANCE_TOTAL');
  const motivoMin = num('motivo_min_caracteres', 1);
  const [form, setForm] = useDuplicarPrefill(empty);
  const [empleados, setEmpleados] = useState([]);
  const [tipos, setTipos] = useState([]);
  const [saldo, setSaldo] = useState(null);
  const [requisitos, setRequisitos] = useState(null);
  const [archivos, setArchivos] = useState({});
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const prefillVacaciones = Boolean(location.state?.vacaciones);

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
        if (prefillVacaciones) {
          const vac = (t || []).find((x) => x.esVacaciones);
          if (vac) setForm((f) => ({ ...f, idTipoPermiso: String(vac.id) }));
        }
      })
      .catch((e) => setError(e.message));
  }, [puedeElegirEmpleado]);

  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }));

  useEffect(() => {
    setArchivos({});
    if (!form.idTipoPermiso) {
      setRequisitos(null);
      return;
    }
    let cancelled = false;
    http.get(`/api/permisos/requisitos-adjunto?idTipoPermiso=${form.idTipoPermiso}`)
      .then((data) => { if (!cancelled) setRequisitos(data); })
      .catch((e) => { if (!cancelled) setError(e.message); });
    return () => { cancelled = true; };
  }, [form.idTipoPermiso]);

  const tipoSel = tipos.find((t) => String(t.id) === String(form.idTipoPermiso));
  const esVacaciones = Boolean(tipoSel?.esVacaciones);
  const idEmpleadoSaldo = form.idEmpleado || usuario?.idEmpleado;
  const actividadesConAdjunto = (requisitos?.pasos || []).filter((p) => p.requiereAdjunto);

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
    if (form.horaInicio && form.horaFin && form.horaFin <= form.horaInicio) {
      setError('La hora de fin debe ser posterior a la de inicio. Para un día completo, deje las horas vacías.');
      return;
    }
    if (form.motivo.trim().length < motivoMin) {
      setError(`El motivo debe tener al menos ${motivoMin} caracteres.`);
      return;
    }
    for (const paso of actividadesConAdjunto) {
      const files = archivos[paso.numeroPaso] || [];
      if (!files.length) {
        setError(`Adjunte el sustento de "${paso.nombrePaso}".`);
        return;
      }
      if (files.length > MAX_ARCHIVOS) {
        setError(`"${paso.nombrePaso}" admite como máximo ${MAX_ARCHIVOS} archivos.`);
        return;
      }
      for (const file of files) {
        if (!EXTENSIONES.includes(extensionDe(file.name))) {
          setError(`"${file.name}" no es PDF, PNG, JPG ni WEBP.`);
          return;
        }
        if (file.size > MAX_BYTES) {
          setError(`"${file.name}" supera los 5 MB.`);
          return;
        }
      }
    }
    const payload = {
      idEmpleado: form.idEmpleado ? Number(form.idEmpleado) : usuario.idEmpleado,
      idTipoPermiso: Number(form.idTipoPermiso),
      fechaInicio: form.fechaInicio,
      fechaFin: form.fechaFin,
      horaInicio: toTime(form.horaInicio),
      horaFin: toTime(form.horaFin),
      motivo: form.motivo.trim()
    };
    setSaving(true);
    try {
      const saved = actividadesConAdjunto.length
        ? await enviarConAdjuntos(payload, actividadesConAdjunto, archivos)
        : await http.post('/api/permisos', payload);
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
      <BackLink to="/permisos">Permisos</BackLink>

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
                    <span className="mt-1 block text-danger">Aún no ha ganado días. No puede registrar vacaciones.</span>
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
            <TimePicker value={form.horaInicio} onChange={(v) => set('horaInicio', v)} />
          </Field>
          <Field label="Hora fin">
            <TimePicker value={form.horaFin} onChange={(v) => set('horaFin', v)} />
          </Field>
          <Field label="Motivo" full>
            <textarea value={form.motivo} onChange={(e) => set('motivo', text(e.target.value, 400))} required minLength={motivoMin} placeholder={`Mínimo ${motivoMin} caracteres`} />
          </Field>
        </div>
        <p className="mt-4 text-xs text-muted">Las horas son opcionales. Si las indica, deben ir las dos y la de fin debe ser posterior a la de inicio. Dejarlas vacías registra el día completo.</p>
      </section>

      {form.idTipoPermiso && (
        <section className="mt-4 rounded-xl border border-line bg-white p-5">
          <p className="text-sm font-semibold text-navy">Sustentación</p>
          {actividadesConAdjunto.length === 0 ? (
            <p className="mt-2 text-sm text-muted">
              {requisitos ? `El flujo ${requisitos.flujo || ''} no exige archivos.` : 'Consultando el flujo…'}
            </p>
          ) : (
            <div className="mt-3 space-y-4">
              <p className="text-xs text-muted">
                Adjunte al menos un archivo por cada actividad que exige sustento.
              </p>
              {actividadesConAdjunto.map((paso) => (
                <div key={paso.numeroPaso} className="rounded-lg border border-line bg-slate-50 p-3">
                  <p className="mb-2 text-sm font-medium text-navy">Paso {paso.numeroPaso} · {paso.nombrePaso}</p>
                  <Dropzone
                    files={archivos[paso.numeroPaso] || []}
                    onChange={(lista) => setArchivos((prev) => ({ ...prev, [paso.numeroPaso]: lista }))}
                    extensiones={EXTENSIONES}
                    maxArchivos={MAX_ARCHIVOS}
                    maxBytes={MAX_BYTES}
                    label="Arrastre aquí el sustento"
                    hint={`PDF, PNG, JPG o WEBP · hasta 5 MB · máximo ${MAX_ARCHIVOS} archivos`}
                    disabled={saving}
                  />
                </div>
              ))}
            </div>
          )}
        </section>
      )}
    </form>
  );
}

async function enviarConAdjuntos(payload, actividades, archivos) {
  const data = new FormData();
  data.append('datos', new Blob([JSON.stringify(payload)], { type: 'application/json' }));
  for (const paso of actividades) {
    for (const file of archivos[paso.numeroPaso] || []) {
      data.append('archivos', file);
      data.append('numeroPaso', String(paso.numeroPaso));
    }
  }
  return api('/api/permisos', { method: 'POST', body: data });
}
