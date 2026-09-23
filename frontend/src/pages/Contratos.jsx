import { useEffect, useState } from 'react';
import { Plus } from 'lucide-react';
import { emptyPage, http, PAGE_SIZE, pagePath, SELECT_SIZE } from '../api/client';
import { Alert, Avatar, Badge, Button, DataList, DatePicker, Field, FilterBar, FormGrid, Kpi, KpiRow, ListActions, MobileRow, Modal, Pager, PersonCell, SearchField } from '../components/ui';
import { hoyISO } from './altaShared';
import { useQuerySearch } from '../lib/useQuerySearch';
import { decimal, text } from '../lib/input';
import { fmtDate as formatDate, fmtMoney } from '../lib/format';

const MODALIDAD = { COLABORADOR: 'Colaborador', PRACTICANTE: 'Practicante' };

const empty = {
  idEmpleado: '', modalidad: 'COLABORADOR', idHorario: '', fechaInicio: '', fechaFin: '', remuneracionBasica: '', estado: 'VIGENTE', observaciones: ''
};

function fmtDate(value) {
  return formatDate(value, 'Sin término');
}

export function Contratos() {
  const [rows, setRows] = useState([]);
  const [meta, setMeta] = useState(emptyPage);
  const [page, setPage] = useState(1);
  const [empleados, setEmpleados] = useState([]);
  const [horarios, setHorarios] = useState([]);
  const [form, setForm] = useState(empty);
  const [editId, setEditId] = useState(null);
  const [open, setOpen] = useState(false);
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');
  const [saving, setSaving] = useState(false);
  const [tab, setTab] = useState('');
  const [modalidad, setModalidad] = useState('');
  const [q, setQ, qDebounced] = useQuerySearch();
  const [counts, setCounts] = useState({ total: 0, vigentes: 0, colaboradores: 0, practicantes: 0 });

  useEffect(() => { setPage(1); }, [qDebounced]);

  useEffect(() => {
    Promise.all([
      http.page(pagePath('/api/empleados', { page: 1, size: SELECT_SIZE })),
      http.get('/api/catalogos/horarios')
    ])
      .then(([emp, hrs]) => {
        setEmpleados(emp.content || []);
        setHorarios(hrs);
      })
      .catch((e) => setError(e.message));
  }, []);

  async function loadCounts() {
    const [all, vig, col, pra] = await Promise.all([
      http.page(pagePath('/api/contratos', { page: 1, size: 1 })),
      http.page(pagePath('/api/contratos', { page: 1, size: 1, estado: 'VIGENTE' })),
      http.page(pagePath('/api/contratos', { page: 1, size: 1, modalidad: 'COLABORADOR' })),
      http.page(pagePath('/api/contratos', { page: 1, size: 1, modalidad: 'PRACTICANTE' }))
    ]);
    setCounts({
      total: all.totalElements || 0,
      vigentes: vig.totalElements || 0,
      colaboradores: col.totalElements || 0,
      practicantes: pra.totalElements || 0
    });
  }

  async function load() {
    const data = await http.page(pagePath('/api/contratos', {
      page,
      size: PAGE_SIZE,
      q: qDebounced,
      modalidad,
      estado: tab
    }));
    setRows(data.content || []);
    setMeta(data);
  }

  useEffect(() => {
    load().catch((e) => setError(e.message));
  }, [page, qDebounced, tab, modalidad]);

  useEffect(() => { loadCounts().catch(() => {}); }, []);

  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }));

  function horarioPracticasId() {
    const h = horarios.find((x) => /pr[aá]ctica/i.test(x.nombre));
    return h?.id || '';
  }

  function horarioColaboradorId() {
    const h = horarios.find((x) => /administrativa/i.test(x.nombre)) || horarios[0];
    return h?.id || '';
  }

  function setModalidadForm(value) {
    setForm((f) => ({
      ...f,
      modalidad: value,
      idHorario: value === 'PRACTICANTE' ? (horarioPracticasId() || f.idHorario) : (f.idHorario || horarioColaboradorId()),
      fechaFin: value === 'PRACTICANTE' ? f.fechaFin : f.fechaFin
    }));
  }

  function abrir(row) {
    setError('');
    if (row) {
      setEditId(row.idContrato);
      setForm({
        idEmpleado: row.idEmpleado || '',
        modalidad: row.modalidad || 'COLABORADOR',
        idHorario: row.idHorario || '',
        fechaInicio: row.fechaInicio || '',
        fechaFin: row.fechaFin || '',
        remuneracionBasica: row.remuneracionBasica != null ? String(row.remuneracionBasica) : '',
        estado: row.estado || 'VIGENTE',
        observaciones: row.observaciones || ''
      });
    } else {
      setEditId(null);
      setForm({
        ...empty,
        fechaInicio: hoyISO(),
        idHorario: horarioColaboradorId()
      });
    }
    setOpen(true);
  }

  async function guardar(e) {
    e.preventDefault();
    setError('');
    if (form.modalidad === 'PRACTICANTE' && !form.fechaFin) {
      setError('El contrato de practicante debe tener fecha de fin.');
      return;
    }
    setSaving(true);
    const body = {
      idEmpleado: Number(form.idEmpleado),
      modalidad: form.modalidad,
      idHorario: Number(form.idHorario),
      fechaInicio: form.fechaInicio,
      fechaFin: form.fechaFin || null,
      remuneracionBasica: form.remuneracionBasica ? Number(form.remuneracionBasica) : 0,
      estado: form.estado,
      observaciones: form.observaciones || null
    };
    try {
      if (editId) await http.put(`/api/contratos/${editId}`, body);
      else await http.post('/api/contratos', body);
      setOk(editId ? 'Contrato actualizado' : 'Contrato registrado');
      setOpen(false);
      await Promise.all([load(), loadCounts()]);
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
          <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-500">Administración</p>
          <h1 className="page-title mt-1">Contratos</h1>
          <p className="mt-2 text-sm text-muted">
            Define si el trabajador es colaborador o practicante, asigna su horario y controla las vacaciones (1.5 días por mes).
          </p>
        </div>
        <Button onClick={() => abrir(null)}><Plus size={16} /> Nuevo contrato</Button>
      </div>

      <Alert>{error}</Alert>
      <Alert ok>{ok}</Alert>

      <KpiRow>
        <Kpi value={counts.total} label="Contratos" hint="Histórico registrado" active={tab === '' && modalidad === ''} onClick={() => { setTab(''); setModalidad(''); setPage(1); }} />
        <Kpi value={counts.vigentes} label="Vigentes" hint="Vínculo activo" active={tab === 'VIGENTE'} onClick={() => { setTab('VIGENTE'); setPage(1); }} />
        <Kpi value={counts.colaboradores} label="Colaboradores" hint="Modalidad laboral" active={modalidad === 'COLABORADOR'} onClick={() => { setModalidad('COLABORADOR'); setPage(1); }} />
        <Kpi value={counts.practicantes} label="Practicantes" hint="Jornada de prácticas" active={modalidad === 'PRACTICANTE'} onClick={() => { setModalidad('PRACTICANTE'); setPage(1); }} />
      </KpiRow>

      <FilterBar>
        <SearchField placeholder="Buscar código, trabajador u horario" value={q} onChange={(e) => { setQ(e.target.value); setPage(1); }} />
        <select className="w-auto" value={tab} onChange={(e) => { setTab(e.target.value); setPage(1); }}>
          <option value="">Todos los estados</option>
          <option value="VIGENTE">Vigente</option>
          <option value="FINALIZADO">Finalizado</option>
          <option value="ANULADO">Anulado</option>
        </select>
      </FilterBar>

      <DataList
        empty={rows.length === 0}
        emptyText="No hay contratos en este filtro."
        cards={rows.map((r) => (
          <MobileRow
            key={r.idContrato}
            leading={<Avatar name={r.empleado} />}
            title={r.empleado}
            meta={`${r.codigoEmpleado} · ${MODALIDAD[r.modalidad] || r.modalidad} · ${fmtDate(r.fechaInicio)} → ${fmtDate(r.fechaFin)}`}
            badge={<Badge value={r.estado} />}
            actions={<Button variant="secondary" className="px-3 py-1.5 text-xs" onClick={() => abrir(r)}>Editar</Button>}
          />
        ))}
        table={(
          <table>
            <thead>
              <tr>
                <th>Trabajador</th>
                <th>Modalidad</th>
                <th>Horario</th>
                <th>Vigencia</th>
                <th>Remuneración</th>
                <th>Vacaciones</th>
                <th>Estado</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {rows.map((r) => (
                <tr key={r.idContrato}>
                  <td>
                    <PersonCell name={r.empleado} meta={`${r.codigo} · ${r.codigoEmpleado}`} />
                  </td>
                  <td>{MODALIDAD[r.modalidad] || r.modalidad}</td>
                  <td>
                    <p className="text-sm text-navy">{r.horario || '—'}</p>
                    {r.horaIngreso && r.horaSalida ? (
                      <p className="text-xs text-muted">{r.horaIngreso.slice(0, 5)}–{r.horaSalida.slice(0, 5)}</p>
                    ) : null}
                  </td>
                  <td className="text-sm">
                    {fmtDate(r.fechaInicio)} → {fmtDate(r.fechaFin)}
                  </td>
                  <td className="text-sm">{r.remuneracionBasica ? fmtMoney(r.remuneracionBasica) : '—'}</td>
                  <td className="text-sm text-muted">
                    {r.vacaciones
                      ? `${r.vacaciones.diasDisponibles} disp. (${r.vacaciones.diasGanados} ganados)`
                      : '—'}
                  </td>
                  <td><Badge value={r.estado} /></td>
                  <td>
                    <ListActions>
                      <Button variant="secondary" className="px-3 py-1.5 text-xs" onClick={() => abrir(r)}>Editar</Button>
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
        <Modal title={editId ? 'Editar contrato' : 'Nuevo contrato'} onClose={() => setOpen(false)}>
          <p className="mb-3 text-sm text-muted">
            El contrato vigente determina el horario y si la persona es colaborador o practicante. Las vacaciones se ganan a 1.5 días por mes completo.
          </p>
          <Alert>{error}</Alert>
          <FormGrid onSubmit={guardar}>
            <Field label="Trabajador" full>
              <select value={form.idEmpleado} onChange={(e) => set('idEmpleado', e.target.value)} required disabled={Boolean(editId)}>
                <option value="">Seleccione</option>
                {empleados.map((e) => (
                  <option key={e.idEmpleado} value={e.idEmpleado}>{e.nombreCompleto} · {e.codigoEmpleado}</option>
                ))}
              </select>
            </Field>
            <Field label="Modalidad">
              <select value={form.modalidad} onChange={(e) => setModalidadForm(e.target.value)} required>
                <option value="COLABORADOR">Colaborador</option>
                <option value="PRACTICANTE">Practicante</option>
              </select>
            </Field>
            <Field label="Horario" hint={form.modalidad === 'PRACTICANTE' ? 'Jornada de prácticas' : 'Jornada del colaborador'}>
              <select value={form.idHorario} onChange={(e) => set('idHorario', e.target.value)} required>
                <option value="">Seleccione</option>
                {horarios.map((h) => <option key={h.id} value={h.id}>{h.nombre}</option>)}
              </select>
            </Field>
            <Field label="Inicio">
              <DatePicker value={form.fechaInicio} onChange={(v) => set('fechaInicio', v)} required />
            </Field>
            <Field label="Fin" hint={form.modalidad === 'PRACTICANTE' ? 'Obligatorio para prácticas' : 'Vacío = indefinido'}>
              <DatePicker value={form.fechaFin} onChange={(v) => set('fechaFin', v)} required={form.modalidad === 'PRACTICANTE'} min={form.fechaInicio || undefined} />
            </Field>
            <Field label="Remuneración básica" hint="Soles mensuales. Se usa en la planilla.">
              <input inputMode="decimal" value={form.remuneracionBasica} onChange={(e) => set('remuneracionBasica', decimal(e.target.value, 99999))} required />
            </Field>
            <Field label="Estado">
              <select value={form.estado} onChange={(e) => set('estado', e.target.value)}>
                <option value="VIGENTE">Vigente</option>
                <option value="FINALIZADO">Finalizado</option>
                <option value="ANULADO">Anulado</option>
              </select>
            </Field>
            <Field label="Observaciones" full>
              <textarea value={form.observaciones} onChange={(e) => set('observaciones', text(e.target.value, 400))} />
            </Field>
            <div className="flex flex-wrap justify-end gap-2 md:col-span-2">
              <Button type="button" variant="secondary" onClick={() => setOpen(false)}>Cancelar</Button>
              <Button type="submit" disabled={saving}>{saving ? 'Guardando…' : 'Guardar'}</Button>
            </div>
          </FormGrid>
        </Modal>
      )}
    </div>
  );
}
