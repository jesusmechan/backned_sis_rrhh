import { useEffect, useMemo, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { ChevronDown, ChevronUp, Plus, Trash2 } from 'lucide-react';
import { http, pagePath, SELECT_SIZE } from '../api/client';
import { Alert, BackLink, Badge, Button, Field } from '../components/ui';
import { APROBADOR, Flujograma, ORIGEN, codigoDesdeNombre, emptyForm, emptyPaso, toForm, toPayload } from './flujoShared';
import { code, label, text } from '../lib/input';

export function FlujoConfig() {
  const { id } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const creating = !id;
  const [form, setForm] = useState(emptyForm);
  const [codigoTouched, setCodigoTouched] = useState(false);
  const [roles, setRoles] = useState([]);
  const [usuarios, setUsuarios] = useState([]);
  const [tipos, setTipos] = useState([]);
  const [otros, setOtros] = useState([]);
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
        const [rolesData, users, tiposData, lista, flujo] = await Promise.all([
          http.get('/api/catalogos/roles'),
          http.page(pagePath('/api/usuarios', { page: 1, size: SELECT_SIZE })),
          http.get('/api/catalogos/tipos-permiso'),
          http.page(pagePath('/api/flujos', { page: 1, size: SELECT_SIZE })),
          creating ? Promise.resolve(null) : http.get(`/api/flujos/${id}`)
        ]);
        if (cancelled) return;
        setRoles(rolesData);
        setUsuarios(users.content || []);
        setTipos(tiposData);
        setOtros(lista.content || []);
        if (flujo) {
          setForm(toForm(flujo));
          setCodigoTouched(true);
        } else if (location.state?.duplicar) {
          setForm({ ...emptyForm(), ...location.state.duplicar });
          setCodigoTouched(true);
        } else {
          setForm(emptyForm());
          setCodigoTouched(false);
        }
      } catch (e) {
        if (!cancelled) setError(e.message);
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    load();
    return () => { cancelled = true; };
  }, [id, creating, location.state]);

  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }));

  function setNombre(value) {
    setForm((f) => {
      const next = { ...f, nombre: label(value, 120) };
      if (creating && !codigoTouched) next.codigo = codigoDesdeNombre(next.nombre);
      return next;
    });
  }

  function setOrigen(value) {
    setForm((f) => ({
      ...f,
      tipoOrigen: value,
      idTipoPermiso: value === 'PERMISO' ? f.idTipoPermiso : ''
    }));
  }

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

  const conflicto = useMemo(() => {
    return otros.find((f) => {
      if (String(f.idConfiguracion) === String(id)) return false;
      if (!f.activo) return false;
      if (f.tipoOrigen !== form.tipoOrigen) return false;
      const mismoTipo = String(f.idTipoPermiso || '') === String(form.idTipoPermiso || '');
      return mismoTipo;
    });
  }, [otros, form.tipoOrigen, form.idTipoPermiso, id]);

  function validar() {
    if (!form.nombre.trim()) return 'Indique el nombre del flujo.';
    if (!form.codigo.trim()) return 'Indique el código.';
    if (!form.pasos.length) return 'El flujo debe tener al menos un paso.';
    for (const [i, p] of form.pasos.entries()) {
      if (!String(p.nombrePaso || '').trim()) return `El paso ${i + 1} necesita un nombre.`;
      if (p.tipoAprobador === 'ROL' && !p.idRol) return `Seleccione el perfil del paso ${i + 1}.`;
      if (p.tipoAprobador === 'USUARIO' && !p.idUsuario) return `Seleccione el usuario del paso ${i + 1}.`;
    }
    return '';
  }

  async function guardar(e) {
    e.preventDefault();
    setError('');
    setOk('');
    const msg = validar();
    if (msg) {
      setError(msg);
      return;
    }
    setSaving(true);
    try {
      const saved = creating
        ? await http.post('/api/flujos', toPayload(form))
        : await http.put(`/api/flujos/${id}`, toPayload(form));
      if (creating) {
        navigate('/flujos', { replace: true, state: { ok: 'Flujo creado. Ya puede usarse en solicitudes nuevas.' } });
        return;
      }
      setOk('Flujo guardado');
      setForm(toForm(saved));
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <form onSubmit={guardar}>
      <BackLink to="/flujos">Flujos</BackLink>

      <div className="mb-6 flex flex-col gap-4 border-b border-line pb-5 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-500">Aprobaciones</p>
          <h1 className="page-title mt-1">
            {creating ? 'Nuevo flujo' : 'Configurar flujo'}
          </h1>
          {!creating && (
            <div className="mt-2 flex flex-wrap items-center gap-2">
              <span className="text-sm text-slate-500">{ORIGEN[form.tipoOrigen] || form.tipoOrigen}</span>
              {form.codigo && <span className="text-xs text-slate-400">{form.codigo}</span>}
              <Badge value={form.activo ? 'ACTIVO' : 'INACTIVO'} />
            </div>
          )}
          {creating && location.state?.duplicar && (
            <p className="mt-2 text-sm text-muted">Copia de un flujo existente. Revise código y pasos antes de guardar.</p>
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
              <Field label="Nombre" hint="Letras y números" full>
                <input value={form.nombre} onChange={(e) => setNombre(e.target.value)} required />
              </Field>
              <Field label="Código" hint="Mayúsculas, números y guion">
                <input
                  value={form.codigo}
                  onChange={(e) => { setCodigoTouched(true); set('codigo', code(e.target.value)); }}
                  required
                />
              </Field>
              <Field label="Origen">
                <select value={form.tipoOrigen} onChange={(e) => setOrigen(e.target.value)}>
                  <option value="PERMISO">Permiso</option>
                  <option value="HORA_EXTRA">Horas extras</option>
                </select>
              </Field>
              <Field label="Tipo de permiso" hint={form.tipoOrigen !== 'PERMISO' ? 'Solo aplica a permisos' : 'Vacío = flujo por defecto'}>
                <select value={form.idTipoPermiso} onChange={(e) => set('idTipoPermiso', e.target.value)} disabled={form.tipoOrigen !== 'PERMISO'}>
                  <option value="">Por defecto</option>
                  {tipos.map((t) => <option key={t.id} value={t.id}>{t.nombre}</option>)}
                </select>
              </Field>
              <Field label="Descripción" full>
                <input value={form.descripcion} onChange={(e) => set('descripcion', text(e.target.value, 200))} />
              </Field>
              <Field label="Estado">
                <select value={form.activo ? '1' : '0'} onChange={(e) => set('activo', e.target.value === '1')}>
                  <option value="1">ACTIVO</option>
                  <option value="0">INACTIVO</option>
                </select>
              </Field>
            </div>
            {conflicto && (
              <p className="mt-4 rounded-lg bg-warn-soft px-3 py-2 text-sm text-warn">
                Ya existe un flujo activo para este origen
                {form.idTipoPermiso ? ` y tipo` : ' por defecto'}: {conflicto.nombre} ({conflicto.codigo}).
                El sistema usará uno de los dos según el más específico.
              </p>
            )}
          </section>

          <section className="rounded-xl border border-line bg-white p-5">
            <div className="mb-4 flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <p className="text-sm font-semibold text-navy">Pasos</p>
                <p className="text-xs text-muted">El orden es el de aprobación. Puede moverlos o eliminarlos.</p>
              </div>
              <Button type="button" variant="secondary" onClick={addPaso}><Plus size={14} /> Agregar paso</Button>
            </div>
            <div className="space-y-3">
              {form.pasos.map((p, i) => (
                <div key={p.key} className="rounded-lg border border-line bg-slate-50 p-3">
                  <div className="mb-3 flex items-center justify-between">
                    <p className="text-sm font-semibold text-navy">Paso {i + 1}</p>
                    <div className="flex gap-1">
                      <button type="button" className="rounded-md p-2 text-slate-500 hover:bg-white disabled:opacity-30" disabled={i === 0} onClick={() => movePaso(i, -1)} aria-label="Subir paso"><ChevronUp size={16} /></button>
                      <button type="button" className="rounded-md p-2 text-slate-500 hover:bg-white disabled:opacity-30" disabled={i === form.pasos.length - 1} onClick={() => movePaso(i, 1)} aria-label="Bajar paso"><ChevronDown size={16} /></button>
                      <button type="button" className="rounded-md p-2 text-slate-500 hover:bg-white disabled:opacity-30" disabled={form.pasos.length === 1} onClick={() => removePaso(i)} aria-label="Quitar paso"><Trash2 size={16} /></button>
                    </div>
                  </div>
                  <div className="grid gap-3 md:grid-cols-3">
                    <Field label="Nombre">
                      <input value={p.nombrePaso} onChange={(e) => setPaso(i, 'nombrePaso', label(e.target.value, 80))} required />
                    </Field>
                    <Field label="Quién aprueba">
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
                      <Field label="Asignación" hint="Se toma del jefe del solicitante">
                        <input value="Jefe del solicitante" disabled />
                      </Field>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </section>

          <section className="rounded-xl border border-line bg-white p-5">
            <p className="mb-1 text-sm font-semibold text-navy">Flujograma</p>
            <p className="mb-4 text-xs text-muted">Así avanzará una solicitud con estos pasos.</p>
            <Flujograma pasos={form.pasos} roles={roles} usuarios={usuarios} />
          </section>
        </div>
      )}
    </form>
  );
}
