import { useEffect, useRef, useState } from 'react';
import { Download, Plus, Upload } from 'lucide-react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { api, http, PAGE_SIZE, pagePath, SELECT_SIZE } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { useConfig } from '../auth/ConfigContext';
import { Alert, Avatar, Badge, Button, CatalogoOptions, DataList, DatePicker, Field, FilterBar, FormGrid, Kpi, KpiRow, ListActions, MobileRow, Modal, Pager, PersonCell, SearchField, downloadBlob } from '../components/ui';
import { correoInstitucional, hoyISO, siguienteCodigo } from './altaShared';
import { useQuerySearch } from '../lib/useQuerySearch';
import { usePagedLoad } from '../lib/usePagedLoad';
import { calcAge } from '../lib/format';
import { address, documentNumber, email, isEmail, isEmployeeCode, isLetters, letters, phone } from '../lib/input';

function fichaVacia(porDefecto) {
  return {
    codigoEmpleado: '', tipoDocumento: porDefecto('TIPO_DOCUMENTO'), numeroDocumento: '', nombres: '',
    apellidoPaterno: '', apellidoMaterno: '', fechaNacimiento: '', sexo: porDefecto('SEXO'),
    correoInstitucional: '', correoPersonal: '', telefono: '', direccion: '', fechaIngreso: '',
    fechaCese: '', idArea: '', idCargo: '', idHorario: '', tipoContrato: porDefecto('TIPO_CONTRATO'),
    estado: porDefecto('ESTADO_EMPLEADO'), idJefeInmediato: ''
  };
}

export function Empleados() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const fileRef = useRef(null);
  const { hasPermission } = useAuth();
  const { param, num, porDefecto, etiqueta, valor, validarRegla } = useConfig();
  const empty = fichaVacia(porDefecto);
  const prefijoCodigo = param('codigo_empleado_prefijo');
  const digitosCodigo = num('codigo_empleado_digitos', 1);
  const dominioCorreo = param('empresa_dominio_correo');
  const nuevoCodigo = (lista) => siguienteCodigo(lista, prefijoCodigo, digitosCodigo);
  const puedeRegistrar = hasPermission('PERSONAL_REGISTRAR');
  const puedeEditar = hasPermission('PERSONAL_ACTUALIZAR');
  const puedeCargar = hasPermission('PERSONAL_CARGAR_EXCEL');
  const [jefes, setJefes] = useState([]);
  const [cats, setCats] = useState({ areas: [], cargos: [], horarios: [] });
  const [ocupados, setOcupados] = useState([]);
  const [form, setForm] = useState(empty);
  const [editId, setEditId] = useState(null);
  const [open, setOpen] = useState(false);
  const [step, setStep] = useState(1);
  const [crearCuenta, setCrearCuenta] = useState(true);
  const [emailTouched, setEmailTouched] = useState(false);
  const [ok, setOk] = useState('');
  const [saving, setSaving] = useState(false);
  const [tab, setTab] = useState('');
  const [idArea, setIdArea] = useState('');
  const [q, setQ, qDebounced] = useQuerySearch();
  const [counts, setCounts] = useState({ total: 0, activos: 0, inactivos: 0, cesados: 0 });

  const { page, setPage, rows, meta, error, setError, reload } = usePagedLoad(
    [qDebounced, tab, idArea],
    (pageNum) => http.page(pagePath('/api/empleados', {
      page: pageNum,
      size: PAGE_SIZE,
      q: qDebounced,
      estado: tab === 'baja' ? 'INACTIVO,CESADO' : tab,
      idArea: idArea || undefined
    }))
  );

  useEffect(() => {
    Promise.all([
      http.page(pagePath('/api/empleados', { page: 1, size: SELECT_SIZE })),
      http.page(pagePath('/api/usuarios', { page: 1, size: SELECT_SIZE })),
      http.get('/api/catalogos/areas'),
      http.get('/api/catalogos/cargos'),
      http.get('/api/catalogos/horarios')
    ])
      .then(([options, users, areas, cargos, horarios]) => {
        setJefes(options.content || []);
        setOcupados((users.content || []).map((u) => u.idEmpleado).filter(Boolean));
        setCats({ areas, cargos, horarios });
      })
      .catch((e) => setError(e.message));
  }, []);

  async function loadCounts() {
    const [all, act, ina, ces] = await Promise.all([
      http.page(pagePath('/api/empleados', { page: 1, size: 1 })),
      http.page(pagePath('/api/empleados', { page: 1, size: 1, estado: 'ACTIVO' })),
      http.page(pagePath('/api/empleados', { page: 1, size: 1, estado: 'INACTIVO' })),
      http.page(pagePath('/api/empleados', { page: 1, size: 1, estado: 'CESADO' }))
    ]);
    setCounts({
      total: all.totalElements || 0,
      activos: act.totalElements || 0,
      inactivos: ina.totalElements || 0,
      cesados: ces.totalElements || 0
    });
  }

  useEffect(() => { loadCounts().catch(() => {}); }, []);

  useEffect(() => {
    if (!open || editId || !jefes.length) return;
    setForm((f) => {
      const next = nuevoCodigo(jefes);
      if (!f.codigoEmpleado || f.codigoEmpleado === nuevoCodigo([])) {
        return { ...f, codigoEmpleado: next };
      }
      return f;
    });
  }, [jefes, open, editId]);

  useEffect(() => {
    if (params.get('nuevo') === '1') abrir(null);
    if (params.get('carga') === '1') fileRef.current?.click();
  }, []);

  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }));

  function setNombre(k, v) {
    setForm((f) => {
      const next = { ...f, [k]: letters(v) };
      if (!emailTouched) next.correoInstitucional = correoInstitucional(next.nombres, next.apellidoPaterno, dominioCorreo);
      return next;
    });
  }

  function setDocumento(value) {
    set('numeroDocumento', documentNumber(value));
  }

  function irCrearCuenta(emp) {
    navigate('/usuarios', {
      state: {
        nuevo: true,
        idEmpleado: emp.idEmpleado,
        nombres: emp.nombres,
        apellidoPaterno: emp.apellidoPaterno,
        correo: emp.correoInstitucional
      }
    });
  }

  function sinCuenta(id) {
    return !ocupados.some((oid) => String(oid) === String(id));
  }

  function abrir(emp) {
    setError('');
    setStep(1);
    setEmailTouched(Boolean(emp?.correoInstitucional));
    if (emp) {
      setEditId(emp.idEmpleado);
      setCrearCuenta(false);
      setForm({
        ...empty,
        ...emp,
        idArea: emp.idArea || '',
        idCargo: emp.idCargo || '',
        idHorario: emp.idHorario || '',
        idJefeInmediato: emp.idJefeInmediato || '',
        fechaNacimiento: emp.fechaNacimiento || '',
        fechaIngreso: emp.fechaIngreso || '',
        fechaCese: emp.fechaCese || '',
        correoPersonal: emp.correoPersonal || '',
        telefono: emp.telefono || '',
        direccion: emp.direccion || ''
      });
    } else {
      setEditId(null);
      setCrearCuenta(true);
      setForm({
        ...empty,
        codigoEmpleado: nuevoCodigo(jefes),
        fechaIngreso: hoyISO()
      });
    }
    setOpen(true);
  }

  function validarPaso(n) {
    if (n === 1) {
      if (!form.codigoEmpleado.trim() || !form.numeroDocumento.trim() || !form.nombres.trim() || !form.apellidoPaterno.trim() || !form.apellidoMaterno.trim()) {
        return 'Complete código, documento y apellidos.';
      }
      if (!isEmployeeCode(form.codigoEmpleado.trim(), prefijoCodigo, digitosCodigo)) {
        return `El código debe tener el formato ${nuevoCodigo([])}.`;
      }
      const reglaDoc = validarRegla('TIPO_DOCUMENTO', form.tipoDocumento, form.numeroDocumento);
      if (reglaDoc) return reglaDoc;
      if (![form.nombres, form.apellidoPaterno, form.apellidoMaterno].every(isLetters)) {
        return 'Nombres y apellidos solo admiten letras.';
      }
    }
    if (n === 2) {
      if (!form.idArea || !form.idCargo || !form.idHorario || !form.fechaIngreso) {
        return 'Seleccione área, cargo, horario y fecha de ingreso.';
      }
    }
    if (n === 3) {
      if (!form.correoInstitucional.trim()) return 'Indique el correo institucional.';
      if (!isEmail(form.correoInstitucional)) return 'Indique un correo institucional válido.';
      if (form.correoPersonal && !isEmail(form.correoPersonal)) return 'Indique un correo personal válido.';
      if (form.telefono && (form.telefono.length < 7 || form.telefono.length > 9)) {
        return 'El teléfono debe tener entre 7 y 9 dígitos.';
      }
    }
    return '';
  }

  function siguientePaso() {
    const msg = validarPaso(step);
    if (msg) { setError(msg); return; }
    setError('');
    setStep((s) => Math.min(3, s + 1));
  }

  async function guardar(e) {
    e.preventDefault();
    const msg = validarPaso(step);
    if (step < 3) {
      if (msg) setError(msg);
      else siguientePaso();
      return;
    }
    if (msg) { setError(msg); return; }
    setError('');
    setSaving(true);
    const body = {
      codigoEmpleado: form.codigoEmpleado.trim(),
      tipoDocumento: form.tipoDocumento || null,
      numeroDocumento: form.numeroDocumento.trim(),
      nombres: form.nombres.trim(),
      apellidoPaterno: form.apellidoPaterno.trim(),
      apellidoMaterno: (form.apellidoMaterno || '').trim(),
      fechaNacimiento: form.fechaNacimiento || null,
      sexo: form.sexo || null,
      correoInstitucional: form.correoInstitucional.trim(),
      correoPersonal: form.correoPersonal.trim() || null,
      telefono: form.telefono.trim() || null,
      direccion: form.direccion.trim() || null,
      fechaIngreso: form.fechaIngreso,
      fechaCese: form.fechaCese || null,
      idArea: Number(form.idArea),
      idCargo: Number(form.idCargo),
      idHorario: Number(form.idHorario),
      tipoContrato: form.tipoContrato || null,
      estado: form.estado || null,
      idJefeInmediato: form.idJefeInmediato ? Number(form.idJefeInmediato) : null
    };
    try {
      const saved = editId
        ? await http.put(`/api/empleados/${editId}`, body)
        : await http.post('/api/empleados', body);
      setOk(editId ? 'Colaborador actualizado' : 'Colaborador registrado');
      setOpen(false);
      await Promise.all([reload(), loadCounts()]);
      const catalog = await http.page(pagePath('/api/empleados', { page: 1, size: SELECT_SIZE }));
      setJefes(catalog.content || []);
      if (!editId && crearCuenta) {
        navigate('/usuarios', {
          state: {
            nuevo: true,
            idEmpleado: saved.idEmpleado,
            nombres: saved.nombres,
            apellidoPaterno: saved.apellidoPaterno,
            correo: saved.correoInstitucional
          }
        });
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  async function plantilla() {
    setError('');
    try {
      downloadBlob(await http.download('/api/empleados/plantilla-excel'), 'plantilla_trabajadores.xlsx');
    } catch (err) {
      setError(err.message);
    }
  }

  async function cargar(ev) {
    const file = ev.target.files?.[0];
    ev.target.value = '';
    if (!file) return;
    const data = new FormData();
    data.append('archivo', file);
    setError('');
    try {
      const json = await api('/api/empleados/carga-excel', { method: 'POST', body: data });
      setOk(`Carga ${json.estado}: ${json.filasExitosas} ok, ${json.filasFallidas} errores`);
      await Promise.all([reload(), loadCounts()]);
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <div>
      <div className="mb-6 flex flex-col gap-4 border-b border-line pb-5 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-500">Administración</p>
          <h1 className="page-title mt-1">Personal</h1>
          <p className="mt-2 text-sm text-muted">Directorio de colaboradores, organigrama y carga masiva Excel.</p>
        </div>
        <div className="flex flex-wrap gap-2">
          {puedeCargar && <Button variant="secondary" onClick={plantilla}><Download size={16} /> Plantilla</Button>}
          {puedeCargar && (
            <label className="inline-flex cursor-pointer items-center gap-2 rounded-lg border border-line bg-white px-4 py-2.5 text-sm font-medium text-slate-700 hover:bg-slate-50">
              <Upload size={16} /> Cargar Excel
              <input ref={fileRef} type="file" accept=".xlsx" hidden onChange={cargar} />
            </label>
          )}
          {puedeRegistrar && <Button onClick={() => abrir(null)}><Plus size={16} /> Registrar</Button>}
        </div>
      </div>

      <Alert>{error}</Alert>
      <Alert ok>{ok}</Alert>

      <KpiRow>
        <Kpi value={counts.total} label="Colaboradores" hint="Personas en el directorio" active={tab === ''} onClick={() => { setTab(''); setPage(1); }} />
        <Kpi value={counts.activos} label="Activos" hint="Con vínculo vigente" active={tab === 'ACTIVO'} onClick={() => { setTab('ACTIVO'); setPage(1); }} />
        <Kpi
          value={counts.inactivos + counts.cesados}
          label="Baja o cese"
          hint={`${counts.inactivos} inactivos · ${counts.cesados} cesados`}
          active={tab === 'baja'}
          onClick={() => { setTab('baja'); setPage(1); }}
        />
      </KpiRow>

      <FilterBar>
        <SearchField placeholder="Buscar código, nombre, documento, área o cargo" value={q} onChange={(e) => { setQ(e.target.value); setPage(1); }} />
        <select className="w-auto" value={idArea} onChange={(e) => { setIdArea(e.target.value); setPage(1); }}>
          <option value="">Todas las áreas</option>
          {cats.areas.map((a) => <option key={a.id} value={a.id}>{a.nombre}</option>)}
        </select>
        {(q || idArea || tab) && (
          <button type="button" className="text-xs font-medium text-navy hover:underline" onClick={() => { setQ(''); setIdArea(''); setTab(''); setPage(1); }}>
            Limpiar filtros
          </button>
        )}
      </FilterBar>

      <DataList
        empty={rows.length === 0}
        emptyText="No hay colaboradores en este filtro."
        cards={rows.map((r) => (
          <MobileRow
            key={r.idEmpleado}
            leading={<Avatar name={r.nombreCompleto} />}
            title={r.nombreCompleto}
            meta={`${r.codigoEmpleado} · ${r.area} · ${r.cargo}`}
            badge={<Badge tipo="ESTADO_EMPLEADO" value={r.estado} />}
            actions={(
              <>
                {puedeEditar && <Button variant="secondary" className="px-3 py-1.5 text-xs" onClick={() => abrir(r)}>Editar</Button>}
                {sinCuenta(r.idEmpleado) && (
                  <Button variant="secondary" className="px-3 py-1.5 text-xs" onClick={() => irCrearCuenta(r)}>Crear cuenta</Button>
                )}
              </>
            )}
          />
        ))}
        table={(
          <table>
            <thead>
              <tr>
                <th>Colaborador</th>
                <th>Área / Cargo</th>
                <th>Contrato</th>
                <th>Jefe</th>
                <th>Estado</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {rows.map((r) => (
                <tr key={r.idEmpleado}>
                  <td>
                    <PersonCell name={r.nombreCompleto} meta={r.codigoEmpleado} />
                  </td>
                  <td>
                    <p className="text-sm text-navy">{r.area || '—'}</p>
                    <p className="text-xs text-muted">{r.cargo || '—'}</p>
                  </td>
                  <td>
                    <p className="text-sm">{etiqueta('TIPO_CONTRATO', r.tipoContrato)}</p>
                    <p className="text-xs text-muted">{r.horario || '—'}</p>
                  </td>
                  <td className="text-sm text-muted">{r.jefeInmediato || 'Sin jefe'}</td>
                  <td><Badge tipo="ESTADO_EMPLEADO" value={r.estado} /></td>
                  <td>
                    <ListActions>
                      {puedeEditar && <Button variant="secondary" className="px-3 py-1.5 text-xs" onClick={() => abrir(r)}>Editar</Button>}
                      {sinCuenta(r.idEmpleado) && (
                        <Button variant="secondary" className="px-3 py-1.5 text-xs" onClick={() => irCrearCuenta(r)}>Crear cuenta</Button>
                      )}
                    </ListActions>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        footer={(
          <Pager page={meta.page} totalPages={meta.totalPages} totalElements={meta.totalElements} size={meta.size} onPage={setPage} />
        )}
      />

      {open && (
        <Modal title={editId ? 'Editar colaborador' : 'Registrar colaborador'} onClose={() => setOpen(false)}>
          <p className="mb-3 text-sm text-muted">
            {editId
              ? 'Actualice la ficha. El cese solo se registra cuando ya existe el colaborador.'
              : 'Alta en tres pasos: identidad, puesto y contacto. El código y el correo se proponen solos.'}
          </p>
          <div className="mb-4 grid grid-cols-3 gap-2">
            {['Identidad', 'Puesto', 'Contacto'].map((label, i) => {
              const n = i + 1;
              return (
                <button
                  key={label}
                  type="button"
                  onClick={() => { if (n < step || editId) { setError(''); setStep(n); } }}
                  className={`rounded-lg px-2 py-2 text-xs font-medium ${step === n ? 'bg-primary text-white' : 'bg-slate-100 text-slate-600'}`}
                >
                  {n}. {label}
                </button>
              );
            })}
          </div>
          <Alert>{error}</Alert>
          <FormGrid onSubmit={guardar}>
            {step === 1 && (
              <>
                <Field label="Código" hint={`Formato ${nuevoCodigo([])}`}>
                  <input value={form.codigoEmpleado} onChange={(e) => set('codigoEmpleado', e.target.value.toUpperCase().replace(/[^A-Z0-9-]/g, '').slice(0, 20))} required />
                </Field>
                <Field label="Tipo de documento">
                  <select
                    value={form.tipoDocumento}
                    onChange={(e) => {
                      const tipo = e.target.value;
                      setForm((f) => ({
                        ...f,
                        tipoDocumento: tipo,
                        numeroDocumento: documentNumber(f.numeroDocumento)
                      }));
                    }}
                  >
                    <CatalogoOptions tipo="TIPO_DOCUMENTO" />
                  </select>
                </Field>
                <Field label="Número de documento" hint={valor('TIPO_DOCUMENTO', form.tipoDocumento)?.mensajeRegla || 'Solo letras y números'}>
                  <input
                    value={form.numeroDocumento}
                    onChange={(e) => setDocumento(e.target.value)}
                    maxLength={20}
                    required
                  />
                </Field>
                <Field label="Sexo">
                  <select value={form.sexo} onChange={(e) => set('sexo', e.target.value)}>
                    <CatalogoOptions tipo="SEXO" />
                  </select>
                </Field>
                <Field label="Nombres" hint="Solo letras">
                  <input value={form.nombres} onChange={(e) => setNombre('nombres', e.target.value)} required />
                </Field>
                <Field label="Apellido paterno" hint="Solo letras">
                  <input value={form.apellidoPaterno} onChange={(e) => setNombre('apellidoPaterno', e.target.value)} required />
                </Field>
                <Field label="Apellido materno" hint="Solo letras">
                  <input value={form.apellidoMaterno} onChange={(e) => set('apellidoMaterno', letters(e.target.value))} required />
                </Field>
                <Field label="Fecha de nacimiento">
                  <DatePicker value={form.fechaNacimiento} onChange={(v) => set('fechaNacimiento', v)} max={hoyISO()} />
                </Field>
                <Field label="Edad" hint="Se calcula automáticamente">
                  <input
                    value={form.fechaNacimiento ? `${calcAge(form.fechaNacimiento)} años` : ''}
                    placeholder="—"
                    readOnly
                    tabIndex={-1}
                  />
                </Field>
              </>
            )}
            {step === 2 && (
              <>
                <Field label="Área">
                  <select value={form.idArea} onChange={(e) => set('idArea', e.target.value)} required>
                    <option value="">Seleccione</option>
                    {cats.areas.map((a) => <option key={a.id} value={a.id}>{a.nombre}</option>)}
                  </select>
                </Field>
                <Field label="Cargo">
                  <select value={form.idCargo} onChange={(e) => set('idCargo', e.target.value)} required>
                    <option value="">Seleccione</option>
                    {cats.cargos.map((a) => <option key={a.id} value={a.id}>{a.nombre}</option>)}
                  </select>
                </Field>
                <Field label="Horario">
                  <select value={form.idHorario} onChange={(e) => set('idHorario', e.target.value)} required>
                    <option value="">Seleccione</option>
                    {cats.horarios.map((a) => <option key={a.id} value={a.id}>{a.nombre}</option>)}
                  </select>
                </Field>
                <Field label="Jefe inmediato" hint="Resuelve el primer paso JEFE_INMEDIATO del flujo">
                  <select value={form.idJefeInmediato} onChange={(e) => set('idJefeInmediato', e.target.value)}>
                    <option value="">Ninguno</option>
                    {jefes.filter((r) => r.idEmpleado !== editId).map((r) => (
                      <option key={r.idEmpleado} value={r.idEmpleado}>{r.nombreCompleto}</option>
                    ))}
                  </select>
                </Field>
                <Field label="Contrato" hint="La modalidad colaborador/practicante se define en Contratos">
                  <select value={form.tipoContrato} onChange={(e) => set('tipoContrato', e.target.value)}>
                    <CatalogoOptions tipo="TIPO_CONTRATO" />
                  </select>
                </Field>
                <Field label="Estado">
                  <select value={form.estado} onChange={(e) => set('estado', e.target.value)}>
                    <CatalogoOptions tipo="ESTADO_EMPLEADO" />
                  </select>
                </Field>
                <Field label="Ingreso">
                  <DatePicker value={form.fechaIngreso} onChange={(v) => set('fechaIngreso', v)} required />
                </Field>
                {editId && (
                  <Field label="Cese">
                    <DatePicker value={form.fechaCese} onChange={(v) => set('fechaCese', v)} />
                  </Field>
                )}
              </>
            )}
            {step === 3 && (
              <>
                <Field label="Correo institucional" hint={dominioCorreo ? `Se arma con nombre.apellido@${dominioCorreo}` : 'Correo del colaborador'} full>
                  <input
                    type="email"
                    value={form.correoInstitucional}
                    onChange={(e) => { setEmailTouched(true); set('correoInstitucional', email(e.target.value)); }}
                    required
                  />
                </Field>
                <Field label="Correo personal" hint="Opcional">
                  <input type="email" value={form.correoPersonal} onChange={(e) => set('correoPersonal', email(e.target.value))} />
                </Field>
                <Field label="Teléfono" hint="Solo dígitos, 7 a 9">
                  <input type="tel" inputMode="numeric" value={form.telefono} onChange={(e) => set('telefono', phone(e.target.value))} />
                </Field>
                <Field label="Dirección" full>
                  <input value={form.direccion} onChange={(e) => set('direccion', address(e.target.value))} />
                </Field>
                {!editId && (
                  <label className="flex items-start gap-2 rounded-lg border border-line px-3 py-2 text-sm md:col-span-2">
                    <input type="checkbox" className="mt-0.5" checked={crearCuenta} onChange={(e) => setCrearCuenta(e.target.checked)} />
                    <span>
                      Crear también la cuenta de acceso
                      <span className="mt-0.5 block text-xs text-muted">Luego se abre Usuarios con el colaborador, el correo y la contraseña inicial configurada.</span>
                    </span>
                  </label>
                )}
                {editId && sinCuenta(editId) && (
                  <div className="rounded-lg border border-line px-3 py-2 text-sm md:col-span-2">
                    <p className="text-slate-600">Este colaborador aún no tiene usuario.</p>
                    <Button type="button" variant="secondary" className="mt-2" onClick={() => irCrearCuenta({ ...form, idEmpleado: editId })}>
                      Crear cuenta de acceso
                    </Button>
                  </div>
                )}
              </>
            )}
            <div className="flex flex-wrap justify-end gap-2 md:col-span-2">
              <Button type="button" variant="secondary" onClick={() => setOpen(false)}>Cancelar</Button>
              {step > 1 && (
                <Button type="button" variant="secondary" onClick={() => { setError(''); setStep((s) => s - 1); }}>Atrás</Button>
              )}
              <Button type="submit" disabled={saving}>
                {saving ? 'Guardando…' : step < 3 ? 'Continuar' : 'Guardar'}
              </Button>
            </div>
          </FormGrid>
        </Modal>
      )}
    </div>
  );
}
