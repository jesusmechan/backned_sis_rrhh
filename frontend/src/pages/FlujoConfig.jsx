import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, ChevronDown, ChevronUp, Plus, Trash2 } from 'lucide-react';
import { http, pagePath, SELECT_SIZE } from '../api/client';
import { Alert, Badge, Button, Field } from '../components/ui';
import { APROBADOR, Flujograma, ORIGEN, emptyForm, emptyPaso, toForm, toPayload } from './flujoShared';

export function FlujoConfig() {
  const { id } = useParams();
  const navigate = useNavigate();
  const creating = !id;
  const [form, setForm] = useState(emptyForm);
  const [roles, setRoles] = useState([]);
  const [usuarios, setUsuarios] = useState([]);
  const [tipos, setTipos] = useState([]);
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');
  const [saving, setSaving] = useState(false);
  const [loading, setLoading] = useState(!creating);

  useEffect(() => {
    let cancelled = false;
    async function load() {
      setError('');
      setOk('');
      setLoading(!creating);
      try {
        const [rolesData, users, tiposData, flujo] = await Promise.all([
          http.get('/api/catalogos/roles'),
          http.page(pagePath('/api/usuarios', { page: 1, size: SELECT_SIZE })),
          http.get('/api/catalogos/tipos-permiso'),
          creating ? Promise.resolve(null) : http.get(`/api/flujos/${id}`)
        ]);
        if (cancelled) return;
        setRoles(rolesData);
        setUsuarios(users.content || []);
        setTipos(tiposData);
        setForm(flujo ? toForm(flujo) : emptyForm());
      } catch (e) {
        if (!cancelled) setError(e.message);
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    load();
    return () => { cancelled = true; };
  }, [id, creating]);

  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }));

  function setPaso(index, key, value) {
    setForm((f) => ({ ...f, pasos: f.pasos.map((p, i) => (i === index ? { ...p, [key]: value } : p)) }));
  }

  function addPaso() {
    setForm((f) => ({ ...f, pasos: [...f.pasos, emptyPaso(f.pasos.length + 1)] }));
  }

  function removePaso(index) {
    setForm((f) => ({
      ...f,
      pasos: f.pasos.filter((_, i) => i !== index).map((p, i) => ({ ...p, numeroPaso: i + 1 }))
    }));
  }

  function movePaso(index, dir) {
    setForm((f) => {
      const next = index + dir;
      if (next < 0 || next >= f.pasos.length) return f;
      const pasos = [...f.pasos];
      [pasos[index], pasos[next]] = [pasos[next], pasos[index]];
      return { ...f, pasos: pasos.map((p, i) => ({ ...p, numeroPaso: i + 1 })) };
    });
  }

  async function guardar(e) {
    e.preventDefault();
    setError('');
    setOk('');
    if (!form.pasos.length) {
      setError('El flujo debe tener al menos un paso.');
      return;
    }
    setSaving(true);
    try {
      const saved = creating
        ? await http.post('/api/flujos', toPayload(form))
        : await http.put(`/api/flujos/${id}`, toPayload(form));
      setOk('Flujo guardado');
      setForm(toForm(saved));
      if (creating) navigate(`/flujos/${saved.idConfiguracion}`, { replace: true });
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <form onSubmit={guardar}>
      <button
        type="button"
        onClick={() => navigate('/flujos')}
        className="mb-4 inline-flex items-center gap-1.5 text-sm text-slate-500 hover:text-navy"
      >
        <ArrowLeft size={16} /> Flujos
      </button>

      <div className="mb-6 flex flex-col gap-4 border-b border-line pb-5 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-500">Aprobaciones</p>
          <h1 className="mt-1 text-3xl font-bold text-navy">
            {creating ? 'Nuevo flujo' : 'Configurar flujo'}
          </h1>
          {!creating && (
            <div className="mt-2 flex flex-wrap items-center gap-2">
              <span className="text-sm text-slate-500">{ORIGEN[form.tipoOrigen] || form.tipoOrigen}</span>
              {form.codigo && <span className="text-xs text-slate-400">{form.codigo}</span>}
              <Badge value={form.activo ? 'ACTIVO' : 'INACTIVO'} />
            </div>
          )}
        </div>
        <div className="flex flex-wrap gap-2">
          <Button type="button" variant="secondary" onClick={() => navigate('/flujos')}>Cancelar</Button>
          <Button type="submit" disabled={saving || loading}>{saving ? 'Guardando…' : 'Guardar'}</Button>
        </div>
      </div>

      <Alert>{error}</Alert>
      <Alert ok>{ok}</Alert>

      {loading ? (
        <p className="text-sm text-muted">Cargando flujo…</p>
      ) : (
        <div className="space-y-4">
          <section className="rounded-xl border border-line bg-white p-5">
            <p className="mb-4 text-sm font-semibold text-navy">Datos del flujo</p>
            <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
              <Field label="Nombre" full>
                <input value={form.nombre} onChange={(e) => set('nombre', e.target.value)} required />
              </Field>
              <Field label="Código">
                <input value={form.codigo} onChange={(e) => set('codigo', e.target.value.toUpperCase())} required />
              </Field>
              <Field label="Origen">
                <select value={form.tipoOrigen} onChange={(e) => set('tipoOrigen', e.target.value)}>
                  <option value="PERMISO">Permiso</option>
                  <option value="HORA_EXTRA">Horas extras</option>
                </select>
              </Field>
              <Field label="Tipo de permiso">
                <select value={form.idTipoPermiso} onChange={(e) => set('idTipoPermiso', e.target.value)} disabled={form.tipoOrigen !== 'PERMISO'}>
                  <option value="">Por defecto</option>
                  {tipos.map((t) => <option key={t.id} value={t.id}>{t.nombre}</option>)}
                </select>
              </Field>
              <Field label="Descripción" full>
                <input value={form.descripcion} onChange={(e) => set('descripcion', e.target.value)} />
              </Field>
              <Field label="Estado">
                <select value={form.activo ? '1' : '0'} onChange={(e) => set('activo', e.target.value === '1')}>
                  <option value="1">ACTIVO</option>
                  <option value="0">INACTIVO</option>
                </select>
              </Field>
            </div>
          </section>

          <section className="rounded-xl border border-line bg-white p-5">
            <p className="mb-1 text-sm font-semibold text-navy">Flujograma</p>
            <p className="mb-4 text-xs text-muted">Así avanzará una solicitud con estos pasos.</p>
            <Flujograma pasos={form.pasos} roles={roles} usuarios={usuarios} />
          </section>

          <section className="rounded-xl border border-line bg-white p-5">
            <div className="mb-4 flex items-center justify-between">
              <p className="text-sm font-semibold text-navy">Pasos</p>
              <Button type="button" variant="secondary" onClick={addPaso}><Plus size={14} /> Paso</Button>
            </div>
            <div className="space-y-2">
              {form.pasos.map((p, i) => (
                <div key={p.key} className="grid items-end gap-3 rounded-lg border border-line bg-slate-50 p-3 md:grid-cols-[2rem_1fr_1fr_1fr_auto]">
                  <p className="pb-2.5 text-sm font-semibold text-navy">{i + 1}</p>
                  <Field label="Nombre">
                    <input value={p.nombrePaso} onChange={(e) => setPaso(i, 'nombrePaso', e.target.value)} required />
                  </Field>
                  <Field label="Aprueba">
                    <select value={p.tipoAprobador} onChange={(e) => setPaso(i, 'tipoAprobador', e.target.value)}>
                      {Object.entries(APROBADOR).map(([value, meta]) => (
                        <option key={value} value={value}>{meta.label}</option>
                      ))}
                    </select>
                  </Field>
                  {p.tipoAprobador === 'ROL' ? (
                    <Field label="Perfil">
                      <select value={p.idRol} onChange={(e) => setPaso(i, 'idRol', e.target.value)} required>
                        <option value="">Seleccione</option>
                        {roles.map((r) => <option key={r.id} value={r.id}>{r.nombre || r.codigo}</option>)}
                      </select>
                    </Field>
                  ) : p.tipoAprobador === 'USUARIO' ? (
                    <Field label="Usuario">
                      <select value={p.idUsuario} onChange={(e) => setPaso(i, 'idUsuario', e.target.value)} required>
                        <option value="">Seleccione</option>
                        {usuarios.map((u) => (
                          <option key={u.idUsuario} value={u.idUsuario}>{u.nombreCompleto || u.nombreUsuario}</option>
                        ))}
                      </select>
                    </Field>
                  ) : (
                    <Field label="Asignación">
                      <input value="Jefe del solicitante" disabled />
                    </Field>
                  )}
                  <div className="flex gap-1 pb-0.5">
                    <button type="button" className="rounded-md p-2 text-slate-500 hover:bg-white disabled:opacity-30" disabled={i === 0} onClick={() => movePaso(i, -1)}><ChevronUp size={16} /></button>
                    <button type="button" className="rounded-md p-2 text-slate-500 hover:bg-white disabled:opacity-30" disabled={i === form.pasos.length - 1} onClick={() => movePaso(i, 1)}><ChevronDown size={16} /></button>
                    <button type="button" className="rounded-md p-2 text-slate-500 hover:bg-white disabled:opacity-30" disabled={form.pasos.length === 1} onClick={() => removePaso(i)}><Trash2 size={16} /></button>
                  </div>
                </div>
              ))}
            </div>
          </section>
        </div>
      )}
    </form>
  );
}
