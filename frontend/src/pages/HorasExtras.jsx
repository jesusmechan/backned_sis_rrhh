import { useEffect, useState } from 'react';
import { Search } from 'lucide-react';
import { emptyPage, http, PAGE_SIZE, pagePath, SELECT_SIZE, toTime } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Alert, Avatar, Badge, Button, Empty, Field, FormGrid, Modal, Pager, Panel } from '../components/ui';

const empty = { idEmpleado: '', fecha: '', horaInicio: '', horaFin: '', cantidadHoras: '', motivo: '' };

export function HorasExtras() {
  const { usuario, hasAnyRole } = useAuth();
  const [rows, setRows] = useState([]);
  const [meta, setMeta] = useState(emptyPage);
  const [page, setPage] = useState(1);
  const [counts, setCounts] = useState({ total: 0, pendientes: 0, horas: 0 });
  const [empleados, setEmpleados] = useState([]);
  const [form, setForm] = useState(empty);
  const [open, setOpen] = useState(false);
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');
  const [tab, setTab] = useState('todas');
  const [q, setQ] = useState('');
  const puedeElegir = hasAnyRole('ADMIN', 'RRHH');

  async function loadCounts() {
    const [all, pend, sample] = await Promise.all([
      http.page(pagePath('/api/horas-extras', { page: 1, size: 1 })),
      http.page(pagePath('/api/horas-extras', { page: 1, size: 1, estado: 'PENDIENTE' })),
      http.page(pagePath('/api/horas-extras', { page: 1, size: SELECT_SIZE }))
    ]);
    setCounts({
      total: all.totalElements,
      pendientes: pend.totalElements,
      horas: (sample.content || []).reduce((a, r) => a + Number(r.cantidadHoras || 0), 0)
    });
  }

  async function load() {
    const estado = tab === 'todas' ? '' : tab;
    const data = await http.page(pagePath('/api/horas-extras', { page, size: PAGE_SIZE, estado, q }));
    setRows(data.content || []);
    setMeta(data);
    if (puedeElegir) {
      const emp = await http.page(pagePath('/api/empleados', { page: 1, size: SELECT_SIZE }));
      setEmpleados(emp.content || []);
    }
  }
  useEffect(() => { load().catch((e) => setError(e.message)); }, [page, tab, q]);
  useEffect(() => { loadCounts().catch(() => {}); }, []);
  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }));

  async function crear(e) {
    e.preventDefault();
    try {
      await http.post('/api/horas-extras', {
        idEmpleado: form.idEmpleado ? Number(form.idEmpleado) : usuario.idEmpleado,
        fecha: form.fecha,
        horaInicio: toTime(form.horaInicio),
        horaFin: toTime(form.horaFin),
        cantidadHoras: Number(form.cantidadHoras),
        motivo: form.motivo
      });
      setOk('Horas extras registradas.');
      setOpen(false);
      setForm(empty);
      await Promise.all([load(), loadCounts()]);
    } catch (err) {
      setError(err.message);
    }
  }

  const nPend = counts.pendientes;
  const horas = counts.horas;
  const periodo = new Date().toLocaleDateString('es-PE', { month: 'long', year: 'numeric' });

  return (
    <div>
      <div className="mb-5 flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-500">Tiempo extra · Consultora Contable Andina</p>
          <div className="mt-1 flex flex-wrap items-center gap-2">
            <h1 className="text-3xl font-bold text-navy">Horas extras</h1>
            <span className="rounded-full bg-sky-50 px-2.5 py-1 text-xs font-medium capitalize text-sky-800">{periodo}</span>
          </div>
          <p className="mt-2 text-sm text-muted">El tope diario y semanal lo valida la base de datos al registrar.</p>
        </div>
        <Button onClick={() => setOpen(true)}>Registrar horas</Button>
      </div>

      <Alert>{error}</Alert>
      <Alert ok>{ok}</Alert>

      <div className="mb-5 grid gap-3 sm:grid-cols-3">
        <Panel><p className="text-xs text-muted">Solicitudes</p><p className="mt-1 text-3xl font-bold text-navy">{counts.total}</p></Panel>
        <Panel><p className="text-xs text-muted">Pendientes</p><p className="mt-1 text-3xl font-bold text-navy">{nPend}</p></Panel>
        <Panel><p className="text-xs text-muted">Horas registradas</p><p className="mt-1 text-3xl font-bold text-navy">{horas}</p></Panel>
      </div>

      <div className="mb-3 flex flex-wrap items-center gap-2">
        {[
          { id: 'todas', label: 'Todas', n: counts.total },
          { id: 'PENDIENTE', label: 'Pendientes', n: nPend }
        ].map((t) => (
          <button key={t.id} type="button" onClick={() => { setTab(t.id); setPage(1); }} className={`rounded-full px-3 py-1.5 text-sm ${tab === t.id ? 'bg-navy text-white' : 'bg-white text-slate-600 ring-1 ring-line'}`}>
            {t.label} ({t.n})
          </button>
        ))}
        <div className="relative ml-auto">
          <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input className="pl-8" placeholder="Buscar colaborador..." value={q} onChange={(e) => { setQ(e.target.value); setPage(1); }} />
        </div>
      </div>

      <Panel padded={false}>
        {rows.length === 0 ? <Empty text="Sin registros en esta vista." /> : (
          <div className="overflow-x-auto">
            <table>
              <thead><tr><th>Colaborador</th><th>Fecha</th><th>Horas</th><th>Estado</th><th></th></tr></thead>
              <tbody>
                {rows.map((r) => (
                  <tr key={r.idSolicitudHoraExtra}>
                    <td>
                      <div className="flex items-center gap-3">
                        <Avatar name={r.empleado} />
                        <div>
                          <p className="font-medium text-navy">{r.empleado}</p>
                          <p className="text-xs text-muted">{r.motivo}</p>
                        </div>
                      </div>
                    </td>
                    <td className="text-sm">{r.fecha} · {r.horaInicio}–{r.horaFin}</td>
                    <td className="font-semibold text-navy">{r.cantidadHoras}</td>
                    <td><Badge value={r.estado} /></td>
                    <td>
                      {r.estado === 'PENDIENTE' && (
                        <Button variant="danger" onClick={() => http.post(`/api/horas-extras/${r.idSolicitudHoraExtra}/cancelar`).then(() => Promise.all([load(), loadCounts()]))}>Cancelar</Button>
                      )}
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
        <Modal title="Nueva hora extra" onClose={() => setOpen(false)}>
          <FormGrid onSubmit={crear}>
            {puedeElegir && (
              <Field label="Empleado" full>
                <select value={form.idEmpleado} onChange={(e) => set('idEmpleado', e.target.value)} required>
                  <option value="">Seleccione</option>
                  {empleados.map((e) => <option key={e.idEmpleado} value={e.idEmpleado}>{e.nombreCompleto}</option>)}
                </select>
              </Field>
            )}
            <Field label="Fecha"><input type="date" value={form.fecha} onChange={(e) => set('fecha', e.target.value)} required /></Field>
            <Field label="Cantidad de horas"><input type="number" step="0.5" min="0.5" max="8" value={form.cantidadHoras} onChange={(e) => set('cantidadHoras', e.target.value)} required /></Field>
            <Field label="Desde"><input type="time" value={form.horaInicio} onChange={(e) => set('horaInicio', e.target.value)} required /></Field>
            <Field label="Hasta"><input type="time" value={form.horaFin} onChange={(e) => set('horaFin', e.target.value)} required /></Field>
            <Field label="Motivo" full><textarea value={form.motivo} onChange={(e) => set('motivo', e.target.value)} required minLength={5} /></Field>
            <div className="md:col-span-2"><Button type="submit">Guardar</Button></div>
          </FormGrid>
        </Modal>
      )}
    </div>
  );
}
