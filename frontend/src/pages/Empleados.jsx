import { useEffect, useRef, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Download, Plus, Upload } from 'lucide-react';
import { api, emptyPage, http, PAGE_SIZE, pagePath, SELECT_SIZE } from '../api/client';
import { Alert, Avatar, Badge, Button, Empty, Field, FilterBar, FormGrid, Kpi, KpiRow, Modal, Pager, SearchField, downloadBlob } from '../components/ui';

const CONTRATO = {
  PLANILLA: 'Planilla',
  RECIBO_HONORARIOS: 'Recibo por honorarios',
  PRACTICAS: 'Prácticas'
};

const empty = {
  codigoEmpleado: '', tipoDocumento: 'DNI', numeroDocumento: '', nombres: '',
  apellidoPaterno: '', apellidoMaterno: '', fechaNacimiento: '', sexo: 'M',
  correoInstitucional: '', correoPersonal: '', telefono: '', direccion: '', fechaIngreso: '',
  fechaCese: '', idArea: '', idCargo: '', idHorario: '', tipoContrato: 'PLANILLA',
  estado: 'ACTIVO', idJefeInmediato: ''
};

export function Empleados() {
  const [params] = useSearchParams();
  const fileRef = useRef(null);
  const [rows, setRows] = useState([]);
  const [jefes, setJefes] = useState([]);
  const [meta, setMeta] = useState(emptyPage);
  const [page, setPage] = useState(1);
  const [cats, setCats] = useState({ areas: [], cargos: [], horarios: [] });
  const [form, setForm] = useState(empty);
  const [editId, setEditId] = useState(null);
  const [open, setOpen] = useState(false);
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');
  const [saving, setSaving] = useState(false);
  const [tab, setTab] = useState('');
  const [idArea, setIdArea] = useState('');
  const [q, setQ] = useState('');
  const [qDebounced, setQDebounced] = useState('');
  const [counts, setCounts] = useState({ total: 0, activos: 0, inactivos: 0, cesados: 0 });

  useEffect(() => {
    const t = setTimeout(() => setQDebounced(q.trim()), 300);
    return () => clearTimeout(t);
  }, [q]);

  useEffect(() => {
    Promise.all([
      http.page(pagePath('/api/empleados', { page: 1, size: SELECT_SIZE })),
      http.get('/api/catalogos/areas'),
      http.get('/api/catalogos/cargos'),
      http.get('/api/catalogos/horarios')
    ])
      .then(([options, areas, cargos, horarios]) => {
        setJefes(options.content || []);
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

  async function load() {
    const data = await http.page(pagePath('/api/empleados', {
      page,
      size: PAGE_SIZE,
      q: qDebounced,
      estado: tab === 'baja' ? 'INACTIVO,CESADO' : tab,
      idArea: idArea || undefined
    }));
    setRows(data.content || []);
    setMeta(data);
  }

  useEffect(() => {
    load().catch((e) => setError(e.message));
  }, [page, qDebounced, tab, idArea]);

  useEffect(() => { loadCounts().catch(() => {}); }, []);

  useEffect(() => {
    if (params.get('nuevo') === '1') abrir(null);
    if (params.get('carga') === '1') fileRef.current?.click();
  }, []);

  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }));

  function abrir(emp) {
    setError('');
    if (emp) {
      setEditId(emp.idEmpleado);
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
      setForm(empty);
    }
    setOpen(true);
  }

  async function guardar(e) {
    e.preventDefault();
    setError('');
    setSaving(true);
    const body = {
      codigoEmpleado: form.codigoEmpleado,
      tipoDocumento: form.tipoDocumento || 'DNI',
      numeroDocumento: form.numeroDocumento,
      nombres: form.nombres,
      apellidoPaterno: form.apellidoPaterno,
      apellidoMaterno: form.apellidoMaterno || '',
      fechaNacimiento: form.fechaNacimiento || null,
      sexo: form.sexo,
      correoInstitucional: form.correoInstitucional,
      correoPersonal: form.correoPersonal || null,
      telefono: form.telefono || null,
      direccion: form.direccion || null,
      fechaIngreso: form.fechaIngreso,
      fechaCese: form.fechaCese || null,
      idArea: Number(form.idArea),
      idCargo: Number(form.idCargo),
      idHorario: Number(form.idHorario),
      tipoContrato: form.tipoContrato,
      estado: form.estado,
      idJefeInmediato: form.idJefeInmediato ? Number(form.idJefeInmediato) : null
    };
    try {
      if (editId) await http.put(`/api/empleados/${editId}`, body);
      else await http.post('/api/empleados', body);
      setOk(editId ? 'Colaborador actualizado' : 'Colaborador registrado');
      setOpen(false);
      await Promise.all([load(), loadCounts()]);
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
      await Promise.all([load(), loadCounts()]);
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
          <Button variant="secondary" onClick={plantilla}><Download size={16} /> Plantilla</Button>
          <label className="inline-flex cursor-pointer items-center gap-2 rounded-lg border border-line bg-white px-4 py-2.5 text-sm font-medium text-slate-700 hover:bg-slate-50">
            <Upload size={16} /> Cargar Excel
            <input ref={fileRef} type="file" accept=".xlsx" hidden onChange={cargar} />
          </label>
          <Button onClick={() => abrir(null)}><Plus size={16} /> Registrar</Button>
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
      </FilterBar>

      {rows.length === 0 ? (
        <div className="rounded-xl border border-line bg-white">
          <Empty text="No hay colaboradores en este filtro." />
        </div>
      ) : (
        <div className="space-y-3">
          {rows.map((r) => (
            <article key={r.idEmpleado} className="rounded-xl border border-line bg-white p-4">
              <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                <div className="flex min-w-0 gap-3">
                  <Avatar name={r.nombreCompleto} />
                  <div className="min-w-0">
                    <p className="font-semibold text-navy">{r.nombreCompleto}</p>
                    <p className="text-xs text-muted">{r.codigoEmpleado} · {r.area} · {r.cargo}</p>
                    <p className="mt-2 text-sm text-slate-600">
                      {CONTRATO[r.tipoContrato] || r.tipoContrato}
                      {r.horario ? ` · ${r.horario}` : ''}
                    </p>
                    <p className="mt-1 text-xs text-muted">
                      Jefe: {r.jefeInmediato || 'Sin jefe inmediato'}
                    </p>
                  </div>
                </div>
                <div className="flex shrink-0 flex-col items-start gap-2 sm:items-end">
                  <Badge value={r.estado} />
                  <Button variant="secondary" onClick={() => abrir(r)}>Editar</Button>
                </div>
              </div>
            </article>
          ))}
        </div>
      )}

      <div className="mt-3 overflow-hidden rounded-xl border border-line bg-white">
        <Pager page={meta.page} totalPages={meta.totalPages} totalElements={meta.totalElements} size={meta.size} onPage={setPage} />
      </div>

      {open && (
        <Modal title={editId ? 'Editar colaborador' : 'Nuevo colaborador'} onClose={() => setOpen(false)}>
          <FormGrid onSubmit={guardar}>
            <p className="text-[11px] font-semibold uppercase tracking-[0.14em] text-slate-500 md:col-span-2">Identidad</p>
            <Field label="Código"><input value={form.codigoEmpleado} onChange={(e) => set('codigoEmpleado', e.target.value)} required /></Field>
            <Field label="Tipo de documento">
              <select value={form.tipoDocumento} onChange={(e) => set('tipoDocumento', e.target.value)}>
                <option value="DNI">DNI</option>
                <option value="CE">Carné de extranjería</option>
                <option value="PASAPORTE">Pasaporte</option>
              </select>
            </Field>
            <Field label="Número de documento"><input value={form.numeroDocumento} onChange={(e) => set('numeroDocumento', e.target.value)} required /></Field>
            <Field label="Sexo">
              <select value={form.sexo} onChange={(e) => set('sexo', e.target.value)}>
                <option value="M">Masculino</option>
                <option value="F">Femenino</option>
              </select>
            </Field>
            <Field label="Nombres"><input value={form.nombres} onChange={(e) => set('nombres', e.target.value)} required /></Field>
            <Field label="Apellido paterno"><input value={form.apellidoPaterno} onChange={(e) => set('apellidoPaterno', e.target.value)} required /></Field>
            <Field label="Apellido materno"><input value={form.apellidoMaterno} onChange={(e) => set('apellidoMaterno', e.target.value)} required /></Field>
            <Field label="Nacimiento"><input type="date" value={form.fechaNacimiento} onChange={(e) => set('fechaNacimiento', e.target.value)} /></Field>

            <p className="text-[11px] font-semibold uppercase tracking-[0.14em] text-slate-500 md:col-span-2">Puesto</p>
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
            <Field label="Jefe inmediato">
              <select value={form.idJefeInmediato} onChange={(e) => set('idJefeInmediato', e.target.value)}>
                <option value="">Ninguno</option>
                {jefes.filter((r) => r.idEmpleado !== editId).map((r) => (
                  <option key={r.idEmpleado} value={r.idEmpleado}>{r.nombreCompleto}</option>
                ))}
              </select>
            </Field>
            <Field label="Contrato">
              <select value={form.tipoContrato} onChange={(e) => set('tipoContrato', e.target.value)}>
                <option value="PLANILLA">Planilla</option>
                <option value="RECIBO_HONORARIOS">Recibo por honorarios</option>
                <option value="PRACTICAS">Prácticas</option>
              </select>
            </Field>
            <Field label="Estado">
              <select value={form.estado} onChange={(e) => set('estado', e.target.value)}>
                <option value="ACTIVO">Activo</option>
                <option value="INACTIVO">Inactivo</option>
                <option value="CESADO">Cesado</option>
              </select>
            </Field>
            <Field label="Ingreso"><input type="date" value={form.fechaIngreso} onChange={(e) => set('fechaIngreso', e.target.value)} required /></Field>
            <Field label="Cese"><input type="date" value={form.fechaCese} onChange={(e) => set('fechaCese', e.target.value)} /></Field>

            <p className="text-[11px] font-semibold uppercase tracking-[0.14em] text-slate-500 md:col-span-2">Contacto</p>
            <Field label="Correo institucional" full>
              <input type="email" value={form.correoInstitucional} onChange={(e) => set('correoInstitucional', e.target.value)} required />
            </Field>
            <Field label="Correo personal">
              <input type="email" value={form.correoPersonal} onChange={(e) => set('correoPersonal', e.target.value)} />
            </Field>
            <Field label="Teléfono"><input value={form.telefono} onChange={(e) => set('telefono', e.target.value)} /></Field>
            <Field label="Dirección" full><input value={form.direccion} onChange={(e) => set('direccion', e.target.value)} /></Field>
            <div className="md:col-span-2">
              <Button type="submit" disabled={saving}>{saving ? 'Guardando…' : 'Guardar'}</Button>
            </div>
          </FormGrid>
        </Modal>
      )}
    </div>
  );
}
