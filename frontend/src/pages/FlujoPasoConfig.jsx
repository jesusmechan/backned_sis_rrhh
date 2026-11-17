import { useEffect, useMemo, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { http, pagePath, SELECT_SIZE } from '../api/client';
import { Alert, Button, CatalogoOptions, Field } from '../components/ui';
import { emptyPaso, toForm, toPayload } from './flujoShared';
import { label } from '../lib/input';

export function FlujoPasoConfig() {
  const { id, numeroPaso } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const creating = !id || id === 'nuevo';
  const index = Math.max(0, Number(numeroPaso) - 1);
  const backTo = creating ? '/flujos/nuevo' : `/flujos/${id}`;
  const isNewPaso = Boolean(location.state?.isNewPaso);
  const draftOriginal = location.state?.formDraft;

  const [form, setForm] = useState(null);
  const [roles, setRoles] = useState([]);
  const [usuarios, setUsuarios] = useState([]);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const [loading, setLoading] = useState(true);

  function volverSinGuardar() {
    navigate(backTo, {
      state: {
        formDraft: isNewPaso && draftOriginal ? draftOriginal : form,
        tab: 'pasos'
      }
    });
  }

  useEffect(() => {
    let cancelled = false;
    async function load() {
      setLoading(true);
      setError('');
      try {
        const [rolesData, users, flujo] = await Promise.all([
          http.get('/api/catalogos/roles'),
          http.page(pagePath('/api/usuarios', { page: 1, size: SELECT_SIZE })),
          creating ? Promise.resolve(null) : http.get(`/api/flujos/${id}`)
        ]);
        if (cancelled) return;
        setRoles(rolesData);
        setUsuarios(users.content || []);
        const base = location.state?.formDraft
          || (flujo ? toForm(flujo) : null);
        if (!base?.pasos?.length) {
          setError('No hay datos del flujo para configurar el paso.');
          setForm(null);
          return;
        }
        const pasos = [...base.pasos];
        while (pasos.length <= index) pasos.push(emptyPaso(pasos.length + 1));
        setForm({ ...base, pasos });
      } catch (e) {
        if (!cancelled) setError(e.message);
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    load();
    return () => { cancelled = true; };
  }, [id, creating, index, location.state]);

  const paso = form?.pasos?.[index];

  const destinos = useMemo(() => {
    if (!form) return [];
    return form.pasos
      .map((p, i) => ({ n: i + 1, nombre: p.nombrePaso || `Paso ${i + 1}` }))
      .filter((d) => d.n < index + 1);
  }, [form, index]);

  function setPaso(key, value) {
    setForm((f) => ({
      ...f,
      pasos: f.pasos.map((p, i) => (i === index ? { ...p, [key]: value } : p))
    }));
  }

  function setTipoActividad(value) {
    setForm((f) => ({
      ...f,
      pasos: f.pasos.map((p, i) => {
        if (i !== index) return p;
        if (value === 'DECISION') {
          return {
            ...p,
            tipoActividad: value,
            accionNoConforme: p.accionNoConforme || 'CERRAR',
            numeroPasoDevolver: p.accionNoConforme === 'DEVOLVER' ? p.numeroPasoDevolver : ''
          };
        }
        return {
          ...p,
          tipoActividad: value,
          accionNoConforme: '',
          numeroPasoDevolver: '',
          requiereAdjunto: value === 'CARGA' ? true : p.requiereAdjunto
        };
      })
    }));
  }

  function setAccionNoConforme(value) {
    setForm((f) => ({
      ...f,
      pasos: f.pasos.map((p, i) => {
        if (i !== index) return p;
        return {
          ...p,
          accionNoConforme: value,
          numeroPasoDevolver: value === 'DEVOLVER' ? (p.numeroPasoDevolver || (index > 0 ? '1' : '')) : ''
        };
      })
    }));
  }

  function validarPaso() {
    if (!paso) return 'Paso no encontrado.';
    if (!String(paso.nombrePaso || '').trim()) return 'Indique el nombre del paso.';
    if (paso.tipoAprobador === 'ROL' && !paso.idRol) return 'Seleccione el perfil.';
    if (paso.tipoAprobador === 'USUARIO' && !paso.idUsuario) return 'Seleccione el usuario.';
    if ((paso.tipoActividad || 'DECISION') === 'DECISION' && paso.accionNoConforme === 'DEVOLVER') {
      const dest = Number(paso.numeroPasoDevolver);
      if (!dest || dest < 1 || dest >= index + 1) return 'Indique un paso anterior para devolver.';
    }
    return '';
  }

  async function guardar(e) {
    e.preventDefault();
    const msg = validarPaso();
    if (msg) {
      setError(msg);
      return;
    }
    setError('');
    setSaving(true);
    try {
      if (creating) {
        navigate(backTo, { replace: true, state: { formDraft: form, tab: 'pasos' } });
        return;
      }
      await http.put(`/api/flujos/${id}`, toPayload(form));
      navigate(backTo, { replace: true, state: { formDraft: form, tab: 'pasos', ok: 'Actividad guardada' } });
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
        onClick={volverSinGuardar}
        className="mb-4 inline-flex items-center gap-1.5 text-sm text-slate-500 hover:text-navy"
      >
        ← Volver al flujo
      </button>

      <div className="mb-6 flex flex-col gap-4 border-b border-line pb-5 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-500">Aprobaciones</p>
          <h1 className="page-title mt-1">{isNewPaso ? 'Nueva actividad' : 'Configurar actividad'}</h1>
          <p className="mt-2 text-sm text-muted">Paso {index + 1} del circuito.</p>
        </div>
        {!loading && paso && (
          <div className="flex flex-wrap gap-2">
            <Button type="button" variant="secondary" onClick={volverSinGuardar}>Cancelar</Button>
            <Button type="submit" disabled={saving}>{saving ? 'Guardando…' : 'Guardar actividad'}</Button>
          </div>
        )}
      </div>

      <Alert>{error}</Alert>

      {loading ? (
        <p className="text-sm text-muted">Cargando…</p>
      ) : !paso ? null : (
        <section className="form-dense w-full space-y-5 rounded-xl border border-line bg-white p-5 shadow-sm sm:p-6">
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            <Field label="Nombre" full>
              <input
                value={paso.nombrePaso}
                onChange={(e) => setPaso('nombrePaso', label(e.target.value, 80))}
                required
              />
            </Field>
            <Field label="Tipo de actividad" hint="Decisión aprueba/rechaza; carga y derivación solo continúan">
              <select value={paso.tipoActividad || 'DECISION'} onChange={(e) => setTipoActividad(e.target.value)}>
                <CatalogoOptions tipo="TIPO_ACTIVIDAD" />
              </select>
            </Field>
            <Field label="Quién actúa">
              <select value={paso.tipoAprobador} onChange={(e) => setPaso('tipoAprobador', e.target.value)}>
                <CatalogoOptions tipo="TIPO_APROBADOR" />
              </select>
            </Field>
            {paso.tipoAprobador === 'ROL' ? (
              <Field label="Perfil">
                <select value={paso.idRol} onChange={(e) => setPaso('idRol', e.target.value)} required>
                  <option value="">Seleccione</option>
                  {roles.map((r) => <option key={r.id} value={r.id}>{r.nombre || r.codigo}</option>)}
                </select>
              </Field>
            ) : paso.tipoAprobador === 'USUARIO' ? (
              <Field label="Usuario">
                <select value={paso.idUsuario} onChange={(e) => setPaso('idUsuario', e.target.value)} required>
                  <option value="">Seleccione</option>
                  {usuarios.map((u) => (
                    <option key={u.idUsuario} value={u.idUsuario}>{u.nombreCompleto || u.nombreUsuario}</option>
                  ))}
                </select>
              </Field>
            ) : (
              <Field label="Asignación" hint={paso.tipoAprobador === 'SOLICITANTE' ? 'Dueño de la solicitud' : 'Jefe del solicitante'}>
                <input
                  value={paso.tipoAprobador === 'SOLICITANTE' ? 'Dueño de la solicitud' : 'Jefe del solicitante'}
                  disabled
                />
              </Field>
            )}
            <Field label="Requiere adjunto">
              <select
                value={paso.requiereAdjunto ? '1' : '0'}
                onChange={(e) => setPaso('requiereAdjunto', e.target.value === '1')}
              >
                <option value="0">No</option>
                <option value="1">Sí</option>
              </select>
            </Field>
            {(paso.tipoActividad || 'DECISION') === 'DECISION' && (
              <>
                <Field label="Si no conforme" hint="Cerrar termina el trámite; devolver reabre un paso anterior">
                  <select
                    value={paso.accionNoConforme || 'CERRAR'}
                    onChange={(e) => setAccionNoConforme(e.target.value)}
                  >
                    <CatalogoOptions tipo="ACCION_NO_CONFORME" />
                  </select>
                </Field>
                {paso.accionNoConforme === 'DEVOLVER' && (
                  <Field label="Devolver al paso">
                    <select
                      value={paso.numeroPasoDevolver || ''}
                      onChange={(e) => setPaso('numeroPasoDevolver', e.target.value)}
                      required
                    >
                      <option value="">Seleccione</option>
                      {destinos.map((d) => (
                        <option key={d.n} value={d.n}>{d.n}. {d.nombre}</option>
                      ))}
                    </select>
                  </Field>
                )}
              </>
            )}
          </div>
        </section>
      )}
    </form>
  );
}
