import { useCallback, useEffect, useRef, useState } from 'react';
import { ArrowUpRight, Minus, Plus, RefreshCw, RotateCcw, UserMinus, UserPlus, Users } from 'lucide-react';
import { http, SELECT_SIZE, pagePath } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { useConfig } from '../auth/ConfigContext';
import { Alert, Button, Field, Modal, initials } from '../components/ui';
import { hoyISO } from './altaShared';

const ZOOM_MIN = 0.4;
const ZOOM_MAX = 1.8;
const ZOOM_STEP = 0.1;

/** Acciones contextuales sobre la persona seleccionada (dentro del organigrama). */
function AccionesNodo({ onAccion }) {
  return (
    <div
      className="absolute left-1/2 top-full z-20 mt-2 flex -translate-x-1/2 flex-col gap-1 rounded-xl border border-slate-200 bg-white p-1.5 shadow-lg"
      onClick={(e) => e.stopPropagation()}
    >
      <button
        type="button"
        className="inline-flex items-center gap-1.5 whitespace-nowrap rounded-lg px-2.5 py-1.5 text-left text-xs font-medium text-slate-700 hover:bg-sky-50 hover:text-sky-800"
        onClick={() => onAccion('asignar')}
      >
        <UserPlus size={14} /> Asignar
      </button>
      <button
        type="button"
        className="inline-flex items-center gap-1.5 whitespace-nowrap rounded-lg px-2.5 py-1.5 text-left text-xs font-medium text-slate-700 hover:bg-sky-50 hover:text-sky-800"
        onClick={() => onAccion('reasignar')}
      >
        <Users size={14} /> Reasignar
      </button>
      <button
        type="button"
        className="inline-flex items-center gap-1.5 whitespace-nowrap rounded-lg px-2.5 py-1.5 text-left text-xs font-medium text-slate-700 hover:bg-sky-50 hover:text-sky-800"
        onClick={() => onAccion('promover')}
      >
        <ArrowUpRight size={14} /> Promover
      </button>
      <button
        type="button"
        className="inline-flex items-center gap-1.5 whitespace-nowrap rounded-lg px-2.5 py-1.5 text-left text-xs font-medium text-danger hover:bg-danger-soft"
        onClick={() => onAccion('cesar')}
      >
        <UserMinus size={14} /> Cesar
      </button>
    </div>
  );
}

/** Tarjeta de persona: avatar, nombre, cargo y menú de acciones si está seleccionada. */
function PersonaCard({ nodo, selected, onSelect, puedeGestionar, onAccion }) {
  const activo = selected?.idEmpleado === nodo.idEmpleado;
  return (
    <div className="relative flex flex-col items-center">
      <button
        type="button"
        onClick={() => onSelect(activo ? null : nodo)}
        className={`flex w-[9.5rem] flex-col items-center rounded-xl border bg-white px-3 py-3 text-center shadow-sm transition sm:w-[10.5rem] ${
          activo
            ? 'border-sky-500 ring-2 ring-sky-200'
            : 'border-slate-200 hover:border-sky-300 hover:shadow'
        }`}
      >
        <div className="grid h-14 w-14 place-items-center overflow-hidden rounded-full bg-gradient-to-br from-sky-100 to-slate-100 text-sm font-semibold text-sky-800 ring-2 ring-white shadow">
          {initials(nodo.nombreCompleto)}
        </div>
        <p className="mt-2 w-full truncate text-sm font-bold leading-tight text-slate-800">
          {nodo.nombreCompleto}
        </p>
        <p className="mt-0.5 w-full truncate text-xs italic text-slate-500">
          {nodo.cargo || 'Sin cargo'}
        </p>
      </button>
      {activo && puedeGestionar && <AccionesNodo onAccion={onAccion} />}
    </div>
  );
}

function OrgNodo({ nodo, selected, onSelect, puedeGestionar, onAccion }) {
  const hijos = nodo.reportes || [];
  const n = hijos.length;

  return (
    <div className="flex flex-col items-center">
      <PersonaCard
        nodo={nodo}
        selected={selected}
        onSelect={onSelect}
        puedeGestionar={puedeGestionar}
        onAccion={onAccion}
      />

      {n > 0 && (
        <>
          {/* Espacio extra si el menú de acciones está abierto sobre este nodo */}
          <div
            className={`w-px bg-sky-400 ${
              selected?.idEmpleado === nodo.idEmpleado && puedeGestionar ? 'h-36' : 'h-7'
            }`}
            aria-hidden
          />

          <ul className="flex items-start">
            {hijos.map((hijo, i) => (
              <li key={hijo.idEmpleado} className="relative flex flex-col items-center px-3 sm:px-5">
                <div className="relative h-7 w-full" aria-hidden>
                  <div className="absolute left-1/2 top-0 h-full w-px -translate-x-1/2 bg-sky-400" />
                  {n > 1 && i > 0 && (
                    <div className="absolute left-0 top-0 h-px w-1/2 bg-sky-400" />
                  )}
                  {n > 1 && i < n - 1 && (
                    <div className="absolute right-0 top-0 h-px w-1/2 bg-sky-400" />
                  )}
                </div>
                <OrgNodo
                  nodo={hijo}
                  selected={selected}
                  onSelect={onSelect}
                  puedeGestionar={puedeGestionar}
                  onAccion={onAccion}
                />
              </li>
            ))}
          </ul>
        </>
      )}
    </div>
  );
}

function OrgChart({ raices, selected, onSelect, zoom, onZoomDelta, puedeGestionar, onAccion }) {
  const viewportRef = useRef(null);

  useEffect(() => {
    const el = viewportRef.current;
    if (!el) return undefined;
    const onWheel = (e) => {
      if (!e.ctrlKey && !e.metaKey) return;
      e.preventDefault();
      onZoomDelta(e.deltaY < 0 ? ZOOM_STEP : -ZOOM_STEP);
    };
    el.addEventListener('wheel', onWheel, { passive: false });
    return () => el.removeEventListener('wheel', onWheel);
  }, [onZoomDelta]);

  if (!raices?.length) {
    return <p className="py-12 text-center text-sm text-muted">No hay personal activo para armar el organigrama.</p>;
  }

  return (
    <div ref={viewportRef} className="max-h-[70vh] overflow-auto px-2 py-6">
      <div
        className="mx-auto"
        style={{
          width: 'max-content',
          transform: `scale(${zoom})`,
          transformOrigin: 'top center',
          // Reserva vertical para el menú de acciones bajo la tarjeta seleccionada
          paddingBottom: puedeGestionar && selected ? '9rem' : '1rem'
        }}
      >
        {raices.length === 1 ? (
          <div className="flex justify-center">
            <OrgNodo
              nodo={raices[0]}
              selected={selected}
              onSelect={onSelect}
              puedeGestionar={puedeGestionar}
              onAccion={onAccion}
            />
          </div>
        ) : (
          <div className="flex justify-center gap-10">
            {raices.map((raiz) => (
              <OrgNodo
                key={raiz.idEmpleado}
                nodo={raiz}
                selected={selected}
                onSelect={onSelect}
                puedeGestionar={puedeGestionar}
                onAccion={onAccion}
              />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

export function Organigrama() {
  const { hasPermission } = useAuth();
  const { param } = useConfig();
  const puedeGestionar = hasPermission('ORGANIGRAMA_GESTIONAR', 'PERSONAL_ACTUALIZAR');
  const empresa = param('empresa_nombre_comercial') || param('empresa_razon_social') || 'Organigrama';

  const [arbol, setArbol] = useState(null);
  const [cargos, setCargos] = useState([]);
  const [empleados, setEmpleados] = useState([]);
  const [selected, setSelected] = useState(null);
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');
  const [loading, setLoading] = useState(true);
  const [modal, setModal] = useState(null);
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState({});
  const [zoom, setZoom] = useState(1);

  const ajustarZoom = useCallback((delta) => {
    setZoom((z) => Math.min(ZOOM_MAX, Math.max(ZOOM_MIN, Math.round((z + delta) * 10) / 10)));
  }, []);

  const cargar = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [tree, cats, emps] = await Promise.all([
        http.get('/api/organigrama'),
        http.get('/api/catalogos/cargos'),
        http.page(pagePath('/api/empleados', { page: 1, size: SELECT_SIZE, estado: 'ACTIVO' }))
      ]);
      setArbol(tree);
      setCargos(cats || []);
      setEmpleados(emps.content || []);
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { cargar(); }, [cargar]);

  function abrir(tipo, nodo = selected) {
    setError('');
    setOk('');
    if (!nodo) {
      setError('Seleccione a una persona en el organigrama');
      return;
    }
    setSelected(nodo);
    setForm({
      idEmpleado: nodo.idEmpleado,
      idsEmpleado: [],
      idNuevoJefe: '',
      idCargoNuevo: '',
      idArea: nodo.idArea || '',
      idJefeSaliente: '',
      asumirEquipoDelJefeActual: true,
      idSucesor: '',
      idCargoSucesor: '',
      idNuevoJefeDeReportes: '',
      fechaCese: hoyISO()
    });
    setModal(tipo);
  }

  async function ejecutar(e) {
    e.preventDefault();
    setSaving(true);
    setError('');
    try {
      if (modal === 'asignar') {
        const ids = (form.idsEmpleado || []).map(Number).filter(Boolean);
        if (!ids.length) {
          throw new Error('Seleccione al menos un colaborador para asignar');
        }
        await http.put('/api/organigrama/reasignar', {
          idsEmpleado: ids,
          idNuevoJefe: Number(form.idEmpleado)
        });
        setOk('Colaboradores asignados bajo este jefe');
      } else if (modal === 'reasignar') {
        await http.put('/api/organigrama/reasignar', {
          idsEmpleado: [Number(form.idEmpleado)],
          idNuevoJefe: form.idNuevoJefe ? Number(form.idNuevoJefe) : null
        });
        setOk('Jefatura reasignada');
      } else if (modal === 'promover') {
        await http.post('/api/organigrama/promover', {
          idEmpleado: Number(form.idEmpleado),
          idCargoNuevo: Number(form.idCargoNuevo),
          idArea: form.idArea ? Number(form.idArea) : null,
          idNuevoJefeDelPromovido: form.idNuevoJefe ? Number(form.idNuevoJefe) : null,
          idJefeSaliente: form.idJefeSaliente ? Number(form.idJefeSaliente) : null,
          asumirEquipoDelJefeActual: !!form.asumirEquipoDelJefeActual
        });
        setOk('Colaborador promovido');
      } else if (modal === 'cesar') {
        await http.post('/api/organigrama/cesar', {
          idEmpleado: Number(form.idEmpleado),
          fechaCese: form.fechaCese || hoyISO(),
          idSucesor: form.idSucesor ? Number(form.idSucesor) : null,
          idCargoSucesor: form.idCargoSucesor ? Number(form.idCargoSucesor) : null,
          idNuevoJefeDeReportes: form.idNuevoJefeDeReportes ? Number(form.idNuevoJefeDeReportes) : null
        });
        setOk('Cese registrado y equipo reasignado');
      }
      setModal(null);
      setSelected(null);
      await cargar();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  function toggleAsignado(id) {
    setForm((f) => {
      const actual = new Set((f.idsEmpleado || []).map(Number));
      const n = Number(id);
      if (actual.has(n)) actual.delete(n);
      else actual.add(n);
      return { ...f, idsEmpleado: [...actual] };
    });
  }

  const cargosJefe = cargos.filter((c) => String(c.nombre || '').toLowerCase().startsWith('jefe '));
  const candidatosAsignar = empleados.filter(
    (e) => e.idEmpleado !== selected?.idEmpleado && e.idJefeInmediato !== selected?.idEmpleado
  );

  const tituloModal = {
    asignar: 'Asignar colaboradores',
    reasignar: 'Reasignar jefe',
    promover: 'Promover colaborador',
    cesar: 'Cesar y reasignar equipo'
  }[modal] || '';

  return (
    <div>
      <div className="mb-6 flex flex-col gap-4 border-b border-line pb-5 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-500">Administración</p>
          <h1 className="page-title mt-1">Organigrama</h1>
          <p className="mt-2 max-w-2xl text-sm text-muted">
            Haga clic en una persona para asignar, reasignar, promover o cesar desde el organigrama.
          </p>
        </div>
        <Button variant="secondary" onClick={cargar} disabled={loading}>
          <RefreshCw size={16} /> Actualizar
        </Button>
      </div>

      <Alert>{error && !modal ? error : null}</Alert>
      <Alert ok>{ok}</Alert>

      {loading && <p className="text-sm text-muted">Cargando organigrama…</p>}

      {!loading && (
        <div className="rounded-2xl border border-slate-200 bg-slate-50/80">
          <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-200 px-4 py-3 sm:px-6">
            <div>
              <p className="text-sm font-semibold text-slate-700">{empresa}</p>
              <p className="text-xs text-slate-500">
                {arbol?.totalActivos ?? 0} colaboradores activos · Ctrl + rueda para zoom
                {puedeGestionar ? ' · Clic en una tarjeta para ver acciones' : ''}
              </p>
            </div>
            <div className="inline-flex items-center gap-1 rounded-lg border border-slate-200 bg-white p-1">
              <button
                type="button"
                className="rounded-md p-2 text-slate-600 hover:bg-slate-100 disabled:opacity-40"
                aria-label="Alejar"
                disabled={zoom <= ZOOM_MIN}
                onClick={() => ajustarZoom(-ZOOM_STEP)}
              >
                <Minus size={16} />
              </button>
              <span className="min-w-[3.25rem] text-center text-xs font-medium tabular-nums text-slate-700">
                {Math.round(zoom * 100)}%
              </span>
              <button
                type="button"
                className="rounded-md p-2 text-slate-600 hover:bg-slate-100 disabled:opacity-40"
                aria-label="Acercar"
                disabled={zoom >= ZOOM_MAX}
                onClick={() => ajustarZoom(ZOOM_STEP)}
              >
                <Plus size={16} />
              </button>
              <button
                type="button"
                className="rounded-md p-2 text-slate-600 hover:bg-slate-100 disabled:opacity-40"
                aria-label="Restablecer zoom"
                disabled={zoom === 1}
                onClick={() => setZoom(1)}
              >
                <RotateCcw size={16} />
              </button>
            </div>
          </div>
          <OrgChart
            raices={arbol?.raices || []}
            selected={selected}
            onSelect={setSelected}
            zoom={zoom}
            onZoomDelta={ajustarZoom}
            puedeGestionar={puedeGestionar}
            onAccion={(tipo) => abrir(tipo)}
          />
        </div>
      )}

      {modal && (
        <Modal onClose={() => setModal(null)} title={tituloModal}>
          <form onSubmit={ejecutar} className="space-y-4">
            <Alert>{error}</Alert>

            {modal === 'asignar' && (
              <div className="space-y-3">
                <p className="text-sm text-muted">
                  Asignar colaboradores bajo{' '}
                  <span className="font-medium text-ink">{selected?.nombreCompleto}</span>
                  {' '}({selected?.cargo}).
                </p>
                <div className="max-h-64 space-y-1 overflow-y-auto rounded-lg border border-line p-2">
                  {candidatosAsignar.length === 0 && (
                    <p className="px-2 py-3 text-sm text-muted">No hay otros colaboradores disponibles.</p>
                  )}
                  {candidatosAsignar.map((e) => {
                    const checked = (form.idsEmpleado || []).map(Number).includes(e.idEmpleado);
                    return (
                      <label
                        key={e.idEmpleado}
                        className="flex cursor-pointer items-center gap-2 rounded-md px-2 py-1.5 text-sm hover:bg-slate-50"
                      >
                        <input
                          type="checkbox"
                          checked={checked}
                          onChange={() => toggleAsignado(e.idEmpleado)}
                        />
                        <span className="min-w-0 flex-1 truncate">
                          {e.nombreCompleto}
                          <span className="text-muted"> — {e.cargo}</span>
                        </span>
                      </label>
                    );
                  })}
                </div>
              </div>
            )}

            {modal === 'reasignar' && (
              <div className="grid gap-4 md:grid-cols-2">
                <Field label="Colaborador">
                  <input disabled value={selected?.nombreCompleto || ''} />
                </Field>
                <Field label="Nuevo jefe inmediato" hint="Vacío = sin jefe (p. ej. Gerente General)">
                  <select
                    value={form.idNuevoJefe || ''}
                    onChange={(e) => setForm((f) => ({ ...f, idNuevoJefe: e.target.value }))}
                  >
                    <option value="">Sin jefe</option>
                    {empleados.filter((e) => e.idEmpleado !== selected?.idEmpleado).map((e) => (
                      <option key={e.idEmpleado} value={e.idEmpleado}>
                        {e.nombreCompleto} — {e.cargo}
                      </option>
                    ))}
                  </select>
                </Field>
              </div>
            )}

            {modal === 'promover' && (
              <div className="grid gap-4 md:grid-cols-2">
                <Field label="Colaborador">
                  <input disabled value={selected?.nombreCompleto || ''} />
                </Field>
                <Field label="Cargo destino" required>
                  <select
                    required
                    value={form.idCargoNuevo || ''}
                    onChange={(e) => setForm((f) => ({ ...f, idCargoNuevo: e.target.value }))}
                  >
                    <option value="">Seleccione</option>
                    {(cargosJefe.length ? cargosJefe : cargos).map((c) => (
                      <option key={c.id} value={c.id}>{c.nombre}</option>
                    ))}
                  </select>
                </Field>
                <Field label="Jefe del promovido">
                  <select
                    value={form.idNuevoJefe || ''}
                    onChange={(e) => setForm((f) => ({ ...f, idNuevoJefe: e.target.value }))}
                  >
                    <option value="">Mantener actual</option>
                    {empleados.filter((e) => e.idEmpleado !== selected?.idEmpleado).map((e) => (
                      <option key={e.idEmpleado} value={e.idEmpleado}>
                        {e.nombreCompleto} — {e.cargo}
                      </option>
                    ))}
                  </select>
                </Field>
                <Field label="Jefe saliente (opcional)">
                  <select
                    value={form.idJefeSaliente || ''}
                    onChange={(e) => setForm((f) => ({ ...f, idJefeSaliente: e.target.value }))}
                  >
                    <option value="">Ninguno</option>
                    {empleados.filter((e) => e.idEmpleado !== selected?.idEmpleado).map((e) => (
                      <option key={e.idEmpleado} value={e.idEmpleado}>
                        {e.nombreCompleto} — {e.cargo}
                      </option>
                    ))}
                  </select>
                </Field>
                <label className="flex items-center gap-2 text-sm text-ink md:col-span-2">
                  <input
                    type="checkbox"
                    checked={!!form.asumirEquipoDelJefeActual}
                    onChange={(e) => setForm((f) => ({ ...f, asumirEquipoDelJefeActual: e.target.checked }))}
                  />
                  El promovido asume los reportes del jefe saliente
                </label>
              </div>
            )}

            {modal === 'cesar' && (
              <div className="grid gap-4 md:grid-cols-2">
                <Field label="Trabajador a cesar">
                  <input disabled value={selected?.nombreCompleto || ''} />
                </Field>
                <Field label="Fecha de cese" required>
                  <input
                    type="date"
                    required
                    value={form.fechaCese || ''}
                    onChange={(e) => setForm((f) => ({ ...f, fechaCese: e.target.value }))}
                  />
                </Field>
                <Field label="Sucesor (colaborador que sube)" hint="Opcional si indica solo nuevo jefe de reportes">
                  <select
                    value={form.idSucesor || ''}
                    onChange={(e) => setForm((f) => ({ ...f, idSucesor: e.target.value }))}
                  >
                    <option value="">Ninguno</option>
                    {empleados.filter((e) => e.idEmpleado !== selected?.idEmpleado).map((e) => (
                      <option key={e.idEmpleado} value={e.idEmpleado}>
                        {e.nombreCompleto} — {e.cargo}
                      </option>
                    ))}
                  </select>
                </Field>
                <Field label="Cargo del sucesor">
                  <select
                    value={form.idCargoSucesor || ''}
                    onChange={(e) => setForm((f) => ({ ...f, idCargoSucesor: e.target.value }))}
                  >
                    <option value="">Sin cambio de cargo</option>
                    {(cargosJefe.length ? cargosJefe : cargos).map((c) => (
                      <option key={c.id} value={c.id}>{c.nombre}</option>
                    ))}
                  </select>
                </Field>
                <Field label="Nuevo jefe de los reportes" hint="Obligatorio si el cesado tiene equipo y no hay sucesor">
                  <select
                    value={form.idNuevoJefeDeReportes || ''}
                    onChange={(e) => setForm((f) => ({ ...f, idNuevoJefeDeReportes: e.target.value }))}
                  >
                    <option value="">Usar sucesor / ninguno</option>
                    {empleados.filter((e) => e.idEmpleado !== selected?.idEmpleado).map((e) => (
                      <option key={e.idEmpleado} value={e.idEmpleado}>
                        {e.nombreCompleto} — {e.cargo}
                      </option>
                    ))}
                  </select>
                </Field>
              </div>
            )}

            <div className="flex justify-end gap-2">
              <Button type="button" variant="secondary" onClick={() => setModal(null)}>Cancelar</Button>
              <Button type="submit" disabled={saving}>{saving ? 'Guardando…' : 'Confirmar'}</Button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
}
