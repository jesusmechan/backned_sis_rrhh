import { useEffect, useRef, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { api, emptyPage, http, PAGE_SIZE, pagePath, SELECT_SIZE } from '../api/client';
import { Alert, Badge, Button, Empty, Field, FormGrid, Modal, Pager, Panel, StackTable, downloadBlob } from '../components/ui';

const empty = {
  codigoEmpleado: '', tipoDocumento: 'DNI', numeroDocumento: '', nombres: '',
  apellidoPaterno: '', apellidoMaterno: '', fechaNacimiento: '', sexo: 'M',
  correoInstitucional: '', telefono: '', direccion: '', fechaIngreso: '',
  idArea: '', idCargo: '', idHorario: '', tipoContrato: 'PLANILLA',
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

  async function load() {
    const [emp, options, areas, cargos, horarios] = await Promise.all([
      http.page(pagePath('/api/empleados', { page, size: PAGE_SIZE })),
      http.page(pagePath('/api/empleados', { page: 1, size: SELECT_SIZE })),
      http.get('/api/catalogos/areas'),
      http.get('/api/catalogos/cargos'),
      http.get('/api/catalogos/horarios')
    ]);
    setRows(emp.content || []);
    setMeta(emp);
    setJefes(options.content || []);
    setCats({ areas, cargos, horarios });
  }
  useEffect(() => { load().catch((e) => setError(e.message)); }, [page]);
  useEffect(() => {
    if (params.get('nuevo') === '1') abrir(null);
    if (params.get('carga') === '1') fileRef.current?.click();
  }, []);
  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }));

  function abrir(emp) {
    if (emp) {
      setEditId(emp.idEmpleado);
      setForm({
        ...empty, ...emp,
        idArea: emp.idArea || '', idCargo: emp.idCargo || '', idHorario: emp.idHorario || '',
        idJefeInmediato: emp.idJefeInmediato || '',
        fechaNacimiento: emp.fechaNacimiento || '', fechaIngreso: emp.fechaIngreso || ''
      });
    } else {
      setEditId(null);
      setForm(empty);
    }
    setOpen(true);
  }

  async function guardar(e) {
    e.preventDefault();
    const body = {
      codigoEmpleado: form.codigoEmpleado,
      tipoDocumento: form.tipoDocumento || 'DNI',
      numeroDocumento: form.numeroDocumento,
      nombres: form.nombres,
      apellidoPaterno: form.apellidoPaterno,
      apellidoMaterno: form.apellidoMaterno,
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
      setOk('Empleado guardado');
      setOpen(false);
      await load();
    } catch (err) {
      setError(err.message);
    }
  }

  async function plantilla() {
    downloadBlob(await http.download('/api/empleados/plantilla-excel'), 'plantilla_trabajadores.xlsx');
  }

  async function cargar(ev) {
    const file = ev.target.files?.[0];
    if (!file) return;
    const data = new FormData();
    data.append('archivo', file);
    try {
      const json = await api('/api/empleados/carga-excel', { method: 'POST', body: data });
      setOk(`Carga ${json.estado}: ${json.filasExitosas} ok, ${json.filasFallidas} errores`);
      await load();
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <div>
      <div className="mb-5 flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-500">Directorio · Consultora Contable Andina</p>
          <h1 className="page-title mt-1">Personal</h1>
          <p className="mt-2 text-sm text-muted">Alta de colaboradores, edición y carga masiva Excel.</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button variant="secondary" onClick={plantilla}>Descargar plantilla</Button>
          <label className="inline-flex cursor-pointer items-center rounded-lg border border-line bg-white px-4 py-2.5 text-sm font-medium text-slate-700 hover:bg-slate-50">
            Cargar Excel
            <input ref={fileRef} type="file" accept=".xlsx" hidden onChange={cargar} />
          </label>
          <Button onClick={() => abrir(null)}>Registrar empleado</Button>
        </div>
      </div>
      <Panel className="mb-4">
        <p className="text-sm font-semibold text-navy">Carga masiva</p>
        <p className="mt-1 text-sm text-muted">Descargue la plantilla, complete las filas y súbala con Cargar Excel. El resultado queda en auditoría.</p>
      </Panel>
      <Alert>{error}</Alert>
      <Alert ok>{ok}</Alert>
      <Panel padded={false}>
        {rows.length === 0 ? <Empty text="Sin empleados." /> : (
          <StackTable
            cards={rows.map((r) => (
              <div key={r.idEmpleado} className="flex items-start justify-between gap-3 px-4 py-3">
                <div className="min-w-0">
                  <p className="font-medium text-navy">{r.nombreCompleto}</p>
                  <p className="text-xs text-muted">{r.codigoEmpleado} · {r.area} · {r.cargo}</p>
                  <div className="mt-2"><Badge value={r.estado} /></div>
                </div>
                <Button variant="secondary" className="shrink-0 px-3 py-2" onClick={() => abrir(r)}>Editar</Button>
              </div>
            ))}
            table={(
              <table>
                <thead><tr><th>Código</th><th>Nombre</th><th>Área</th><th>Cargo</th><th>Estado</th><th></th></tr></thead>
                <tbody>
                  {rows.map((r) => (
                    <tr key={r.idEmpleado}>
                      <td>{r.codigoEmpleado}</td>
                      <td>{r.nombreCompleto}</td>
                      <td>{r.area}</td>
                      <td>{r.cargo}</td>
                      <td><Badge value={r.estado} /></td>
                      <td><Button variant="secondary" onClick={() => abrir(r)}>Editar</Button></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          />
        )}
        <Pager page={meta.page} totalPages={meta.totalPages} totalElements={meta.totalElements} size={meta.size} onPage={setPage} />
      </Panel>
      {open && (
        <Modal title={editId ? 'Editar colaborador' : 'Nuevo colaborador'} onClose={() => setOpen(false)}>
          <FormGrid onSubmit={guardar}>
            <Field label="Código"><input value={form.codigoEmpleado} onChange={(e) => set('codigoEmpleado', e.target.value)} required /></Field>
            <Field label="Documento"><input value={form.numeroDocumento} onChange={(e) => set('numeroDocumento', e.target.value)} required /></Field>
            <Field label="Nombres"><input value={form.nombres} onChange={(e) => set('nombres', e.target.value)} required /></Field>
            <Field label="Apellido paterno"><input value={form.apellidoPaterno} onChange={(e) => set('apellidoPaterno', e.target.value)} required /></Field>
            <Field label="Apellido materno"><input value={form.apellidoMaterno} onChange={(e) => set('apellidoMaterno', e.target.value)} required /></Field>
            <Field label="Sexo">
              <select value={form.sexo} onChange={(e) => set('sexo', e.target.value)}><option value="M">M</option><option value="F">F</option></select>
            </Field>
            <Field label="Correo institucional" full>
              <input type="email" value={form.correoInstitucional} onChange={(e) => set('correoInstitucional', e.target.value)} required />
            </Field>
            <Field label="Ingreso"><input type="date" value={form.fechaIngreso} onChange={(e) => set('fechaIngreso', e.target.value)} required /></Field>
            <Field label="Nacimiento"><input type="date" value={form.fechaNacimiento} onChange={(e) => set('fechaNacimiento', e.target.value)} /></Field>
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
                <option>PLANILLA</option><option>RECIBO_HONORARIOS</option><option>PRACTICAS</option>
              </select>
            </Field>
            <Field label="Estado">
              <select value={form.estado} onChange={(e) => set('estado', e.target.value)}>
                <option>ACTIVO</option><option>INACTIVO</option><option>CESADO</option>
              </select>
            </Field>
            <Field label="Teléfono"><input value={form.telefono || ''} onChange={(e) => set('telefono', e.target.value)} /></Field>
            <Field label="Dirección" full><input value={form.direccion || ''} onChange={(e) => set('direccion', e.target.value)} /></Field>
            <div className="md:col-span-2"><Button type="submit">Guardar</Button></div>
          </FormGrid>
        </Modal>
      )}
    </div>
  );
}
