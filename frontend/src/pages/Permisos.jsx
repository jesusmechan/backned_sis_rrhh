import { useEffect, useState } from 'react';
import { Search } from 'lucide-react';
import { emptyPage, http, PAGE_SIZE, pagePath, SELECT_SIZE, toTime } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Alert, Avatar, Badge, Button, Empty, Field, FormGrid, Modal, Pager, Panel } from '../components/ui';

const empty = {
  idEmpleado: '', idTipoPermiso: '', fechaInicio: '', fechaFin: '', horaInicio: '', horaFin: '', motivo: ''
};

export function Permisos() {
  const { usuario, hasAnyRole } = useAuth();
  const [rows, setRows] = useState([]);
  const [meta, setMeta] = useState(emptyPage);
  const [page, setPage] = useState(1);
  const [counts, setCounts] = useState({ total: 0, pendientes: 0, aprobados: 0 });
  const [tipos, setTipos] = useState([]);
  const [empleados, setEmpleados] = useState([]);
  const [form, setForm] = useState(empty);
  const [open, setOpen] = useState(false);
  const [hist, setHist] = useState(null);
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');
  const [tab, setTab] = useState('todas');
  const [q, setQ] = useState('');
  const puedeElegirEmpleado = hasAnyRole('ADMIN', 'RRHH');

  async function loadCounts() {
    const [all, pend, apr] = await Promise.all([
      http.page(pagePath('/api/permisos', { page: 1, size: 1 })),
      http.page(pagePath('/api/permisos', { page: 1, size: 1, estado: 'PENDIENTE' })),
      http.page(pagePath('/api/permisos', { page: 1, size: 1, estado: 'APROBADO' }))
    ]);
    setCounts({ total: all.totalElements, pendientes: pend.totalElements, aprobados: apr.totalElements });
  }

  async function load() {
    const estado = tab === 'todas' ? '' : tab;
    const [p, t] = await Promise.all([
      http.page(pagePath('/api/permisos', { page, size: PAGE_SIZE, estado, q })),
      http.get('/api/catalogos/tipos-permiso')
    ]);
    setRows(p.content || []);
    setMeta(p);
    setTipos(t);
    if (puedeElegirEmpleado) {
      const emp = await http.page(pagePath('/api/empleados', { page: 1, size: SELECT_SIZE }));
      setEmpleados(emp.content || []);
    }
  }
  useEffect(() => { load().catch((e) => setError(e.message)); }, [page, tab, q]);
  useEffect(() => { loadCounts().catch(() => {}); }, []);
  function set(k, v) { setForm((f) => ({ ...f, [k]: v })); }

  async function crear(e) {
    e.preventDefault();
    setError('');
    try {
      await http.post('/api/permisos', {
        idEmpleado: form.idEmpleado ? Number(form.idEmpleado) : usuario.idEmpleado,
        idTipoPermiso: Number(form.idTipoPermiso),
        fechaInicio: form.fechaInicio,
        fechaFin: form.fechaFin,
        horaInicio: toTime(form.horaInicio),
        horaFin: toTime(form.horaFin),
        motivo: form.motivo
      });
      setOk('Permiso registrado. El flujo se instanció automáticamente.');
      setOpen(false);
      setForm(empty);
      await Promise.all([load(), loadCounts()]);
    } catch (err) {
      setError(err.message);
    }
  }

  async function cancelar(id) {
    if (!confirm('¿Cancelar esta solicitud?')) return;
    await http.post(`/api/permisos/${id}/cancelar`);
    await Promise.all([load(), loadCounts()]);
  }

  const nPend = counts.pendientes;
  const nApr = counts.aprobados;
  const periodo = new Date().toLocaleDateString('es-PE', { month: 'long', year: 'numeric' });

  return (
    <div>
      <div className="mb-5 flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-500">Trámites · Consultora Contable Andina</p>
          <div className="mt-1 flex flex-wrap items-center gap-2">
            <h1 className="text-3xl font-bold text-navy">Permisos</h1>
            <span className="rounded-full bg-sky-50 px-2.5 py-1 text-xs font-medium capitalize text-sky-800">{periodo}</span>
          </div>
          <p className="mt-2 text-sm text-muted">El aprobador no se elige aquí: lo define el flujo configurado.</p>
        </div>
        <Button onClick={() => setOpen(true)}>Nueva solicitud</Button>
      </div>

      <Alert>{error}</Alert>
      <Alert ok>{ok}</Alert>

      <div className="mb-5 grid gap-3 sm:grid-cols-3">
        <Panel><p className="text-xs text-muted">Total</p><p className="mt-1 text-3xl font-bold text-navy">{counts.total}</p></Panel>
        <Panel><p className="text-xs text-muted">Pendientes</p><p className="mt-1 text-3xl font-bold text-navy">{nPend}</p></Panel>
        <Panel><p className="text-xs text-muted">Aprobados</p><p className="mt-1 text-3xl font-bold text-navy">{nApr}</p></Panel>
      </div>

      <div className="mb-3 flex flex-wrap items-center gap-2">
        {[
          { id: 'todas', label: 'Todas', n: counts.total },
          { id: 'PENDIENTE', label: 'Pendientes', n: nPend },
          { id: 'APROBADO', label: 'Aprobadas', n: nApr }
        ].map((t) => (
          <button key={t.id} type="button" onClick={() => { setTab(t.id); setPage(1); }} className={`rounded-full px-3 py-1.5 text-sm ${tab === t.id ? 'bg-navy text-white' : 'bg-white text-slate-600 ring-1 ring-line'}`}>
            {t.label} ({t.n})
          </button>
        ))}
        <div className="relative ml-auto">
          <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input className="pl-8" placeholder="Buscar colaborador o tipo..." value={q} onChange={(e) => { setQ(e.target.value); setPage(1); }} />
        </div>
      </div>

      <Panel padded={false}>
        {rows.length === 0 ? <Empty text="Sin solicitudes en esta vista." /> : (
          <div className="overflow-x-auto">
            <table>
              <thead>
                <tr><th>Colaborador</th><th>Tipo</th><th>Fechas</th><th>Estado</th><th></th></tr>
              </thead>
              <tbody>
                {rows.map((r) => (
                  <tr key={r.idSolicitudPermiso}>
                    <td>
                      <div className="flex items-center gap-3">
                        <Avatar name={r.empleado} />
                        <div>
                          <p className="font-medium text-navy">{r.empleado}</p>
                          <p className="text-xs text-muted">REQ-{r.idSolicitudPermiso}</p>
                        </div>
                      </div>
                    </td>
                    <td><span className="rounded-md bg-sky-50 px-2 py-0.5 text-xs font-medium text-sky-800">{r.tipoPermiso}</span></td>
                    <td className="text-sm">{r.fechaInicio} → {r.fechaFin}</td>
                    <td><Badge value={r.estado} /></td>
                    <td>
                      <div className="flex flex-wrap gap-2">
                        <Button variant="secondary" onClick={() => http.get(`/api/permisos/${r.idSolicitudPermiso}/historial`).then(setHist)}>Historial</Button>
                        {r.estado === 'PENDIENTE' && <Button variant="danger" onClick={() => cancelar(r.idSolicitudPermiso)}>Cancelar</Button>}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        <Pager page={meta.page} totalPages={meta.totalPages} totalElements={meta.totalElements} size={meta.size} onPage={setPage} />
      </Panel>

      {open && (
        <Modal title="Nueva solicitud de permiso" onClose={() => setOpen(false)}>
          <FormGrid onSubmit={crear}>
            {puedeElegirEmpleado && (
              <Field label="Empleado" full>
                <select value={form.idEmpleado} onChange={(e) => set('idEmpleado', e.target.value)} required>
                  <option value="">Seleccione</option>
                  {empleados.map((e) => <option key={e.idEmpleado} value={e.idEmpleado}>{e.nombreCompleto}</option>)}
                </select>
              </Field>
            )}
            <Field label="Tipo" full>
              <select value={form.idTipoPermiso} onChange={(e) => set('idTipoPermiso', e.target.value)} required>
                <option value="">Seleccione</option>
                {tipos.map((t) => <option key={t.id} value={t.id}>{t.nombre}</option>)}
              </select>
            </Field>
            <Field label="Desde"><input type="date" value={form.fechaInicio} onChange={(e) => set('fechaInicio', e.target.value)} required /></Field>
            <Field label="Hasta"><input type="date" value={form.fechaFin} onChange={(e) => set('fechaFin', e.target.value)} required /></Field>
            <Field label="Hora inicio"><input type="time" value={form.horaInicio} onChange={(e) => set('horaInicio', e.target.value)} /></Field>
            <Field label="Hora fin"><input type="time" value={form.horaFin} onChange={(e) => set('horaFin', e.target.value)} /></Field>
            <Field label="Motivo" full>
              <textarea value={form.motivo} onChange={(e) => set('motivo', e.target.value)} required minLength={5} />
            </Field>
            <div className="md:col-span-2"><Button type="submit">Registrar</Button></div>
          </FormGrid>
        </Modal>
      )}
      {hist && (
        <Modal title="Historial" onClose={() => setHist(null)}>
          <div className="overflow-x-auto">
            <table>
              <thead><tr><th>Acción</th><th>Usuario</th><th>Comentario</th><th>Fecha</th></tr></thead>
              <tbody>
                {hist.map((h) => (
                  <tr key={h.idHistorial}>
                    <td>{h.accion}</td><td>{h.usuario}</td><td>{h.comentario}</td><td>{h.fechaHora}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Modal>
      )}
    </div>
  );
}
