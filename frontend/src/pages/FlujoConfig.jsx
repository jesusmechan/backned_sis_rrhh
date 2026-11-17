import { useEffect, useMemo, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { ChevronDown, ChevronUp, Plus, Settings2, Trash2 } from 'lucide-react';
import { http, pagePath, SELECT_SIZE } from '../api/client';
import { useConfig } from '../auth/ConfigContext';
import { Alert, BackLink, Badge, Button, CatalogoOptions, Field, cn } from '../components/ui';
import { Flujograma, asignado, codigoDesdeNombre, emptyForm, toForm, toPayload } from './flujoShared';
import { code, label, text } from '../lib/input';

const TABS = [
  { id: 'datos', label: 'Datos' },
  { id: 'pasos', label: 'Pasos' },
  { id: 'preview', label: 'Vista previa' }
];

/** Devuelve mensaje y pestaña a la que saltar si falla la validación. */
function validarForm(form) {
  if (!form.nombre.trim()) return { msg: 'Indique el nombre del flujo.', tab: 'datos' };
  if (!form.codigo.trim()) return { msg: 'Indique el código.', tab: 'datos' };
  if (!form.pasos.length) return { msg: 'El flujo debe tener al menos un paso.', tab: 'pasos' };
  for (const [i, p] of form.pasos.entries()) {
    if (!String(p.nombrePaso || '').trim()) {
      return { msg: `El paso ${i + 1} necesita un nombre.`, tab: 'pasos' };
    }
    if (p.tipoAprobador === 'ROL' && !p.idRol) {
      return { msg: `Seleccione el perfil del paso ${i + 1}.`, tab: 'pasos' };
    }
    if (p.tipoAprobador === 'USUARIO' && !p.idUsuario) {
      return { msg: `Seleccione el usuario del paso ${i + 1}.`, tab: 'pasos' };
    }
    if ((p.tipoActividad || 'DECISION') === 'DECISION' && p.accionNoConforme === 'DEVOLVER') {
      const dest = Number(p.numeroPasoDevolver);
      if (!dest || dest < 1 || dest >= i + 1) {
        return { msg: `El paso ${i + 1} debe indicar un paso anterior al devolver.`, tab: 'pasos' };
      }
    }
  }
  return { msg: '', tab: null };
}

export function FlujoConfig() {
  const { id } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const creating = !id;
  const { etiqueta } = useConfig();
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
  const [tab, setTab] = useState('datos');

  useEffect(() => {
    let cancelled = false;
    async function load() {
      setError('');
      setOk(location.state?.ok || '');
      setLoading(!creating);
      const draft = location.state?.formDraft;
      setTab(draft || location.state?.tab === 'pasos' ? 'pasos' : 'datos');
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
        if (draft) {
          setForm(draft);
          setCodigoTouched(true);
        } else if (flujo) {
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

  function addPaso() {
    const base = creating ? '/flujos/nuevo' : `/flujos/${id}`;
    navigate(`${base}/pasos/${form.pasos.length + 1}`, {
      state: { formDraft: form, isNewPaso: true }
    });
  }

  function configurarPaso(index) {
    const base = creating ? '/flujos/nuevo' : `/flujos/${id}`;
    navigate(`${base}/pasos/${index + 1}`, { state: { formDraft: form } });
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

  async function guardar(e) {
    e.preventDefault();
    setError('');
    setOk('');
    const { msg, tab: tabError } = validarForm(form);
    if (msg) {
      setError(msg);
      if (tabError) setTab(tabError);
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
              <span className="text-sm text-slate-500">{etiqueta('TIPO_ORIGEN_FLUJO', form.tipoOrigen)}</span>
              {form.codigo && <span className="text-xs text-slate-400">{form.codigo}</span>}
              <Badge tipo="ESTADO_REGISTRO" value={form.activo ? 'ACTIVO' : 'INACTIVO'} />
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
        <>
          <div className="-mx-1 mb-4 overflow-x-auto px-1">
            <div className="flex min-w-max gap-1 border-b border-line" role="tablist" aria-label="Secciones del flujo">
              {TABS.map((t) => (
                <button
                  key={t.id}
                  type="button"
                  role="tab"
                  aria-selected={tab === t.id}
                  onClick={() => setTab(t.id)}
                  className={cn(
                    'relative px-4 py-2.5 text-sm font-medium transition-colors',
                    tab === t.id
                      ? 'text-navy after:absolute after:inset-x-0 after:bottom-0 after:h-0.5 after:bg-navy'
                      : 'text-muted hover:text-navy'
                  )}
                >
                  {t.label}
                  {t.id === 'pasos' && form.pasos.length > 0 && (
                    <span className="ml-1.5 rounded-md bg-slate-100 px-1.5 py-0.5 text-[10px] font-semibold text-slate-600">
                      {form.pasos.length}
                    </span>
                  )}
                </button>
              ))}
            </div>
          </div>

          <section className="form-dense rounded-xl border border-line bg-white p-5 shadow-sm" role="tabpanel">
            {tab === 'datos' && (
              <>
                <p className="mb-1 text-sm font-semibold text-navy">Datos del flujo</p>
                <p className="mb-4 text-xs text-muted">Identificación y alcance del circuito de aprobación.</p>
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
                      <CatalogoOptions tipo="TIPO_ORIGEN_FLUJO" />
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
              </>
            )}

            {tab === 'pasos' && (
              <>
                <div className="mb-3 flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
                  <div>
                    <p className="text-sm font-semibold text-navy">Pasos</p>
                    <p className="text-xs text-muted">Resumen del circuito. Use Configurar para editar cada actividad.</p>
                  </div>
                  <Button type="button" variant="secondary" className="px-3 py-1.5 text-xs" onClick={addPaso}>
                    <Plus size={14} /> Agregar paso
                  </Button>
                </div>
                <div className="-mx-1 overflow-x-auto">
                  <table className="w-full min-w-[44rem] border-collapse text-left text-sm">
                    <thead>
                      <tr className="border-b border-line">
                        <th className="w-10 px-2 py-2">#</th>
                        <th className="px-2 py-2">Nombre</th>
                        <th className="px-2 py-2">Tipo</th>
                        <th className="px-2 py-2">Responsable</th>
                        <th className="px-2 py-2">Si no conforme</th>
                        <th className="w-[8.5rem] px-2 py-2 text-right">Acciones</th>
                      </tr>
                    </thead>
                    <tbody>
                      {form.pasos.map((p, i) => {
                        const tipo = p.tipoActividad || 'DECISION';
                        const noConforme = tipo !== 'DECISION'
                          ? '—'
                          : p.accionNoConforme === 'DEVOLVER'
                            ? `Devolver → ${p.numeroPasoDevolver || '?'}`
                            : etiqueta('ACCION_NO_CONFORME', p.accionNoConforme || 'CERRAR');
                        return (
                          <tr key={p.key} className="border-b border-line last:border-b-0">
                            <td className="px-2 py-2 align-middle">
                              <span className="grid h-6 w-6 place-items-center rounded bg-primary text-[11px] font-semibold text-white">
                                {i + 1}
                              </span>
                            </td>
                            <td className="px-2 py-2 align-middle">
                              <p className="font-medium text-navy">{p.nombrePaso || 'Sin nombre'}</p>
                              {p.requiereAdjunto && <p className="text-[11px] text-muted">Requiere adjunto</p>}
                            </td>
                            <td className="px-2 py-2 align-middle">
                              <Badge tipo="TIPO_ACTIVIDAD" value={tipo} />
                            </td>
                            <td className="px-2 py-2 align-middle text-slate-600">
                              <span className="block text-xs text-muted">{etiqueta('TIPO_APROBADOR', p.tipoAprobador)}</span>
                              {asignado(p, roles, usuarios)}
                            </td>
                            <td className="px-2 py-2 align-middle text-slate-600">{noConforme}</td>
                            <td className="px-2 py-2 align-middle">
                              <div className="flex justify-end gap-0.5">
                                <button type="button" className="rounded p-1.5 text-slate-500 hover:bg-slate-100" onClick={() => configurarPaso(i)} aria-label="Configurar paso"><Settings2 size={15} /></button>
                                <button type="button" className="rounded p-1.5 text-slate-500 hover:bg-slate-100 disabled:opacity-30" disabled={i === 0} onClick={() => movePaso(i, -1)} aria-label="Subir paso"><ChevronUp size={15} /></button>
                                <button type="button" className="rounded p-1.5 text-slate-500 hover:bg-slate-100 disabled:opacity-30" disabled={i === form.pasos.length - 1} onClick={() => movePaso(i, 1)} aria-label="Bajar paso"><ChevronDown size={15} /></button>
                                <button type="button" className="rounded p-1.5 text-slate-500 hover:bg-slate-100 disabled:opacity-30" disabled={form.pasos.length === 1} onClick={() => removePaso(i)} aria-label="Quitar paso"><Trash2 size={15} /></button>
                              </div>
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              </>
            )}

            {tab === 'preview' && (
              <>
                <p className="mb-1 text-sm font-semibold text-navy">Vista previa</p>
                <p className="mb-4 text-xs text-muted">
                  Recorrido del circuito según el tipo de cada actividad (carga, derivación o decisión).
                </p>
                <Flujograma pasos={form.pasos} roles={roles} usuarios={usuarios} />
              </>
            )}
          </section>
        </>
      )}
    </form>
  );
}
