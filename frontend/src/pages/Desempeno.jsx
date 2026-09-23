import { useEffect, useState } from 'react';
import { Plus } from 'lucide-react';
import { emptyPage, http, PAGE_SIZE, pagePath, SELECT_SIZE } from '../api/client';
import { Alert, Avatar, Badge, Button, DataList, DatePicker, Field, FilterBar, FormGrid, MobileRow, Modal, Pager, PersonCell, SearchField } from '../components/ui';
import { useQuerySearch } from '../lib/useQuerySearch';
import { hoyISO } from './altaShared';
import { text } from '../lib/input';
import { useAuth } from '../auth/AuthContext';

const empty = {
  idEmpleado: '', periodo: '', fecha: hoyISO(), puntualidad: '4', calidad: '4', cooperacion: '4', iniciativa: '4', comentario: ''
};

export function Desempeno() {
  const { hasAnyRole } = useAuth();
  const puedeRegistrar = hasAnyRole('ADMIN', 'RRHH', 'JEFE', 'GERENCIA');
  const [rows, setRows] = useState([]);
  const [meta, setMeta] = useState(emptyPage);
  const [page, setPage] = useState(1);
  const [empleados, setEmpleados] = useState([]);
  const [form, setForm] = useState(empty);
  const [open, setOpen] = useState(false);
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');
  const [saving, setSaving] = useState(false);
  const [q, setQ, qDebounced] = useQuerySearch();

  useEffect(() => { setPage(1); }, [qDebounced]);

  useEffect(() => {
    if (!puedeRegistrar) return;
    http.page(pagePath('/api/empleados', { page: 1, size: SELECT_SIZE }))
      .then((data) => setEmpleados(data.content || []))
      .catch((e) => setError(e.message));
  }, [puedeRegistrar]);

  useEffect(() => {
    http.page(pagePath('/api/evaluaciones', { page, size: PAGE_SIZE, q: qDebounced }))
      .then((data) => {
        setRows(data.content || []);
        setMeta(data);
      })
      .catch((e) => setError(e.message));
  }, [page, qDebounced]);

  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }));

  async function guardar(e) {
    e.preventDefault();
    setError('');
    setSaving(true);
    try {
      await http.post('/api/evaluaciones', {
        idEmpleado: Number(form.idEmpleado),
        periodo: form.periodo.trim(),
        fecha: form.fecha,
        puntualidad: Number(form.puntualidad),
        calidad: Number(form.calidad),
        cooperacion: Number(form.cooperacion),
        iniciativa: Number(form.iniciativa),
        comentario: form.comentario || null
      });
      setOk('Evaluación registrada');
      setOpen(false);
      setForm(empty);
      const data = await http.page(pagePath('/api/evaluaciones', { page: 1, size: PAGE_SIZE, q: qDebounced }));
      setRows(data.content || []);
      setMeta(data);
      setPage(1);
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <div>
      <div className="mb-6 flex flex-col gap-4 border-b border-line pb-5 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-500">Gestión</p>
          <h1 className="page-title mt-1">Desempeño</h1>
          <p className="mt-2 text-sm text-muted">
            Evaluación de puntualidad, calidad, cooperación e iniciativa (escala 1 a 5). El promedio se calcula al guardar.
          </p>
        </div>
        {puedeRegistrar && <Button onClick={() => { setError(''); setForm({ ...fecha: hoyISO() }); setOpen(true); }}><Plus size={16} /> Nueva evaluación</Button>}
      </div>

      <Alert>{error}</Alert>
      <Alert ok>{ok}</Alert>

      <FilterBar>
        <SearchField placeholder="Buscar colaborador, periodo o comentario" value={q} onChange={(e) => { setQ(e.target.value); setPage(1); }} />
      </FilterBar>

      <DataList
        empty={rows.length === 0}
        emptyText="No hay evaluaciones registradas."
        cards={rows.map((r) => (
          <MobileRow
            key={r.idEvaluacion}
            leading={<Avatar name={r.empleado} />}
            title={r.empleado}
            meta={`${r.periodo} · ${r.fecha}${r.evaluador ? ` · ${r.evaluador}` : ''}`}
            badge={(
              <div className="text-right">
                <Badge value={r.estado} />
                <p className="mt-1 text-sm font-semibold text-navy">{Number(r.promedio).toFixed(2)}</p>
              </div>
            )}
          />
        ))}
        table={(
          <table>
            <thead>
              <tr>
                <th>Colaborador</th>
                <th>Periodo</th>
                <th>Criterios</th>
                <th>Promedio</th>
                <th>Estado</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((r) => (
                <tr key={r.idEvaluacion}>
                  <td>
                    <PersonCell name={r.empleado} meta={r.evaluador ? `Evaluador: ${r.evaluador}` : undefined} />
                  </td>
                  <td>
                    <p className="text-sm text-navy">{r.periodo}</p>
                    <p className="text-xs text-muted">{r.fecha}</p>
                  </td>
                  <td className="text-sm text-muted">
                    Punt. {r.puntualidad} · Cal. {r.calidad} · Coop. {r.cooperacion} · Ini. {r.iniciativa}
                    {r.comentario ? <p className="mt-1 text-xs">{r.comentario}</p> : null}
                  </td>
                  <td className="text-sm font-semibold text-navy">{Number(r.promedio).toFixed(2)}</td>
                  <td><Badge value={r.estado} /></td>
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
        <Modal title="Nueva evaluación" onClose={() => setOpen(false)}>
          <Alert>{error}</Alert>
          <FormGrid onSubmit={guardar}>
            <Field label="Trabajador" full>
              <select value={form.idEmpleado} onChange={(e) => set('idEmpleado', e.target.value)} required>
                <option value="">Seleccione</option>
                {empleados.map((e) => <option key={e.idEmpleado} value={e.idEmpleado}>{e.nombreCompleto}</option>)}
              </select>
            </Field>
            <Field label="Periodo" hint="Ej. 2026-Q3">
              <input value={form.periodo} onChange={(e) => set('periodo', text(e.target.value, 20))} required />
            </Field>
            <Field label="Fecha">
              <DatePicker value={form.fecha} onChange={(v) => set('fecha', v)} required />
            </Field>
            {['puntualidad', 'calidad', 'cooperacion', 'iniciativa'].map((k) => (
              <Field key={k} label={k.charAt(0).toUpperCase() + k.slice(1)}>
                <select value={form[k]} onChange={(e) => set(k, e.target.value)} required>
                  {[1, 2, 3, 4, 5].map((n) => <option key={n} value={n}>{n}</option>)}
                </select>
              </Field>
            ))}
            <Field label="Comentario" full>
              <textarea value={form.comentario} onChange={(e) => set('comentario', text(e.target.value, 400))} />
            </Field>
            <div className="flex justify-end gap-2 md:col-span-2">
              <Button type="button" variant="secondary" onClick={() => setOpen(false)}>Cancelar</Button>
              <Button type="submit" disabled={saving}>{saving ? 'Guardando…' : 'Registrar'}</Button>
            </div>
          </FormGrid>
        </Modal>
      )}
    </div>
  );
}
