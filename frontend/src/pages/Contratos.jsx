import { useEffect, useState } from 'react';
import { Plus } from 'lucide-react';
import { http, PAGE_SIZE, pagePath, SELECT_SIZE } from '../api/client';
import { useConfig } from '../auth/ConfigContext';
import { Alert, Avatar, Badge, Button, CatalogoOptions, DataList, DatePicker, Field, FilterBar, FormGrid, Kpi, KpiRow, ListActions, MobileRow, Modal, Pager, PersonCell, SearchField } from '../components/ui';
import { hoyISO } from './altaShared';
import { useQuerySearch } from '../lib/useQuerySearch';
import { usePagedLoad } from '../lib/usePagedLoad';
import { decimal, text } from '../lib/input';
import { fmtDate as formatDate, fmtMoney } from '../lib/format';

function fmtDate(value) {
  return formatDate(value, 'Sin término');
}

function pct(tasa) {
  const n = Number(tasa);
  return Number.isFinite(n) ? `${Math.round(n * 10000) / 100}%` : '';
}

export function Contratos() {
  const { num, porDefecto, etiqueta } = useConfig();
  const empty = {
    idEmpleado: '', modalidad: porDefecto('MODALIDAD_CONTRATO'), idHorario: '', fechaInicio: '', fechaFin: '',
    remuneracionBasica: '', regimenPensionario: porDefecto('REGIMEN_PENSIONARIO'), afpNombre: '', regimenLaboral: '',
    tieneAsignacionFamiliar: false, estado: porDefecto('ESTADO_CONTRATO'), observaciones: ''
  };
  const [empleados, setEmpleados] = useState([]);
  const [horarios, setHorarios] = useState([]);
  const [afps, setAfps] = useState([]);
  const [regimenes, setRegimenes] = useState([]);
  const [form, setForm] = useState(empty);
  const [editId, setEditId] = useState(null);
  const [open, setOpen] = useState(false);
  const [ok, setOk] = useState('');
  const [saving, setSaving] = useState(false);
  const [tab, setTab] = useState('');
  const [modalidad, setModalidad] = useState('');
  const [q, setQ, qDebounced] = useQuerySearch();
  const [counts, setCounts] = useState({ total: 0, vigentes: 0, colaboradores: 0, practicantes: 0 });
  const remuneracionMax = num('remuneracion_maxima', 0);
  const diasVacaciones = num('dias_vacaciones_mensuales', 0);

  const { page, setPage, rows, meta, error, setError, reload } = usePagedLoad(
    [qDebounced, tab, modalidad],
    (pageNum) => http.page(pagePath('/api/contratos', {
      page: pageNum,
      size: PAGE_SIZE,
      q: qDebounced,
      modalidad,
      estado: tab
    }))
  );

  useEffect(() => {
    Promise.all([
      http.page(pagePath('/api/empleados', { page: 1, size: SELECT_SIZE })),
      http.get('/api/catalogos/horarios'),
      http.get('/api/catalogos/afps'),
      http.get('/api/catalogos/regimenes-laborales')
    ])
      .then(([emp, hrs, afp, reg]) => {
        setEmpleados(emp.content || []);
        setHorarios(hrs);
        setAfps(afp || []);
        setRegimenes(reg || []);
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

  useEffect(() => { loadCounts().catch(() => {}); }, []);

  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }));
  const regimenPorDefecto = () => regimenes.find((r) => r.porDefecto)?.codigo || '';
  const pensionColaborador = () => {
    const def = porDefecto('REGIMEN_PENSIONARIO');
    return def && def !== 'NINGUNO' ? def : 'ONP';
  };

  function elegirEmpleado(id) {
    const emp = empleados.find((e) => String(e.idEmpleado) === String(id));
    setForm((f) => ({ ...f, idEmpleado: id, idHorario: f.idHorario || emp?.idHorario || '' }));
  }

  function setModalidadForm(value) {
    setForm((f) => ({
      ...f,
      modalidad: value,
      regimenPensionario: value === 'PRACTICANTE' ? 'NINGUNO' : (f.regimenPensionario === 'NINGUNO' ? pensionColaborador() : f.regimenPensionario),
      afpNombre: value === 'PRACTICANTE' ? '' : f.afpNombre,
      tieneAsignacionFamiliar: value === 'PRACTICANTE' ? false : f.tieneAsignacionFamiliar
    }));
  }

  function abrir(row) {
    setError('');
    if (row) {
      setEditId(row.idContrato);
      setForm({
        idEmpleado: row.idEmpleado || '',
        modalidad: row.modalidad || empty.modalidad,
        idHorario: row.idHorario || '',
        fechaInicio: row.fechaInicio || '',
        fechaFin: row.fechaFin || '',
        remuneracionBasica: row.remuneracionBasica != null ? String(row.remuneracionBasica) : '',
        regimenPensionario: row.regimenPensionario || empty.regimenPensionario,
        afpNombre: row.afpNombre || '',
        regimenLaboral: row.regimenLaboral || regimenPorDefecto(),
        tieneAsignacionFamiliar: Boolean(row.tieneAsignacionFamiliar),
        estado: row.estado || empty.estado,
        observaciones: row.observaciones || ''
      });
    } else {
      setEditId(null);
      setForm({
        ...empty,
        fechaInicio: hoyISO(),
        regimenLaboral: regimenPorDefecto()
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
    if (form.modalidad !== 'PRACTICANTE' && form.regimenPensionario === 'AFP' && !form.afpNombre) {
      setError('Seleccione la AFP del trabajador.');
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
      regimenPensionario: form.modalidad === 'PRACTICANTE' ? 'NINGUNO' : form.regimenPensionario,
      afpNombre: form.regimenPensionario === 'AFP' ? form.afpNombre : null,
      regimenLaboral: form.regimenLaboral || null,
      tieneAsignacionFamiliar: form.modalidad !== 'PRACTICANTE' && Boolean(form.tieneAsignacionFamiliar),
      estado: form.estado,
      observaciones: form.observaciones || null
    };
    try {
      if (editId) await http.put(`/api/contratos/${editId}`, body);
      else await http.post('/api/contratos', body);
      setOk(editId ? 'Contrato actualizado' : 'Contrato registrado');
      setOpen(false);
      await Promise.all([reload(), loadCounts()]);
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  const regimenSel = regimenes.find((r) => r.codigo === form.regimenLaboral);

  return (
    <div>
      <div className="mb-6 flex flex-col gap-4 border-b border-line pb-5 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-500">Administración</p>
          <h1 className="page-title mt-1">Contratos</h1>
          <p className="mt-2 text-sm text-muted">
            Define la modalidad, el régimen laboral y el horario del trabajador, y controla las vacaciones ({diasVacaciones} días por mes).
          </p>
        </div>
        <Button onClick={() => abrir(null)}><Plus size={16} /> Nuevo contrato</Button>
      </div>

      <Alert>{error}</Alert>
      <Alert ok>{ok}</Alert>

      <KpiRow>
        <Kpi value={counts.total} label="Contratos" hint="Histórico registrado" active={tab === '' && modalidad === ''} onClick={() => { setTab(''); setModalidad(''); setPage(1); }} />
        <Kpi value={counts.vigentes} label={etiqueta('ESTADO_CONTRATO', 'VIGENTE')} hint="Vínculo activo" active={tab === 'VIGENTE'} onClick={() => { setTab('VIGENTE'); setPage(1); }} />
        <Kpi value={counts.colaboradores} label={etiqueta('MODALIDAD_CONTRATO', 'COLABORADOR')} hint="Modalidad laboral" active={modalidad === 'COLABORADOR'} onClick={() => { setModalidad('COLABORADOR'); setPage(1); }} />
        <Kpi value={counts.practicantes} label={etiqueta('MODALIDAD_CONTRATO', 'PRACTICANTE')} hint="Jornada de prácticas" active={modalidad === 'PRACTICANTE'} onClick={() => { setModalidad('PRACTICANTE'); setPage(1); }} />
      </KpiRow>

      <FilterBar>
        <SearchField placeholder="Buscar código, trabajador u horario" value={q} onChange={(e) => { setQ(e.target.value); setPage(1); }} />
        <select className="w-auto" value={tab} onChange={(e) => { setTab(e.target.value); setPage(1); }}>
          <option value="">Todos los estados</option>
          <CatalogoOptions tipo="ESTADO_CONTRATO" />
        </select>
        {(q || tab || modalidad) && (
          <button type="button" className="text-xs font-medium text-navy hover:underline" onClick={() => { setQ(''); setTab(''); setModalidad(''); setPage(1); }}>
            Limpiar filtros
          </button>
        )}
      </FilterBar>

      <DataList
        empty={rows.length === 0}
        emptyText="No hay contratos en este filtro."
        cards={rows.map((r) => (
          <MobileRow
            key={r.idContrato}
            leading={<Avatar name={r.empleado} />}
            title={r.empleado}
            meta={`${r.codigoEmpleado} · ${etiqueta('MODALIDAD_CONTRATO', r.modalidad)} · ${etiqueta('REGIMEN_PENSIONARIO', r.regimenPensionario)} · ${fmtDate(r.fechaInicio)} → ${fmtDate(r.fechaFin)}`}
            badge={<Badge tipo="ESTADO_CONTRATO" value={r.estado} />}
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
                  <td>
                    <p className="text-sm">{etiqueta('MODALIDAD_CONTRATO', r.modalidad)}</p>
                    {r.regimenLaboralNombre && <p className="text-xs text-muted">{r.regimenLaboralNombre}</p>}
                  </td>
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
                  <td><Badge tipo="ESTADO_CONTRATO" value={r.estado} /></td>
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
            El contrato vigente determina el horario, la modalidad y el régimen laboral. Las vacaciones se ganan a {diasVacaciones} días por mes completo.
          </p>
          <Alert>{error}</Alert>
          <FormGrid onSubmit={guardar}>
            <Field label="Trabajador" full>
              <select value={form.idEmpleado} onChange={(e) => elegirEmpleado(e.target.value)} required disabled={Boolean(editId)}>
                <option value="">Seleccione</option>
                {empleados.map((e) => (
                  <option key={e.idEmpleado} value={e.idEmpleado}>{e.nombreCompleto} · {e.codigoEmpleado}</option>
                ))}
              </select>
            </Field>
            <Field label="Modalidad">
              <select value={form.modalidad} onChange={(e) => setModalidadForm(e.target.value)} required>
                <CatalogoOptions tipo="MODALIDAD_CONTRATO" />
              </select>
            </Field>
            <Field label="Horario" hint="Por defecto, el de la ficha del trabajador">
              <select value={form.idHorario} onChange={(e) => set('idHorario', e.target.value)} required>
                <option value="">Seleccione</option>
                {horarios.map((h) => <option key={h.id} value={h.id}>{h.nombre}</option>)}
              </select>
            </Field>
            <Field label="Régimen laboral" hint={regimenSel?.descripcion || 'Define gratificación, CTS y EsSalud'}>
              <select value={form.regimenLaboral} onChange={(e) => set('regimenLaboral', e.target.value)} required>
                <option value="">Seleccione</option>
                {regimenes.map((r) => <option key={r.codigo} value={r.codigo}>{r.nombre}</option>)}
              </select>
            </Field>
            <Field label="Inicio">
              <DatePicker value={form.fechaInicio} onChange={(v) => set('fechaInicio', v)} required />
            </Field>
            <Field label="Fin" hint={form.modalidad === 'PRACTICANTE' ? 'Obligatorio para prácticas' : 'Vacío = indefinido'}>
              <DatePicker value={form.fechaFin} onChange={(v) => set('fechaFin', v)} required={form.modalidad === 'PRACTICANTE'} min={form.fechaInicio || undefined} />
            </Field>
            <Field label="Remuneración básica" hint="Monto mensual. Se usa en la planilla.">
              <input inputMode="decimal" value={form.remuneracionBasica} onChange={(e) => set('remuneracionBasica', decimal(e.target.value, remuneracionMax || Infinity))} required />
            </Field>
            {form.modalidad !== 'PRACTICANTE' && (
              <>
                <Field label="Régimen pensionario" hint={`ONP ${pct(num('tasa_onp'))} o AFP (${pct(num('tasa_afp_aporte'))} + comisión + seguro)`}>
                  <select
                    value={form.regimenPensionario}
                    onChange={(e) => setForm((f) => ({
                      ...f,
                      regimenPensionario: e.target.value,
                      afpNombre: e.target.value === 'AFP' ? (f.afpNombre || afps[0]?.codigo || '') : ''
                    }))}
                    required
                  >
                    <CatalogoOptions tipo="REGIMEN_PENSIONARIO" excluir={['NINGUNO']} />
                  </select>
                </Field>
                {form.regimenPensionario === 'AFP' && (
                  <Field label="AFP">
                    <select value={form.afpNombre} onChange={(e) => set('afpNombre', e.target.value)} required>
                      <option value="">Seleccione</option>
                      {afps.map((a) => <option key={a.codigo} value={a.codigo}>{a.nombre}</option>)}
                    </select>
                  </Field>
                )}
                <Field label="Asignación familiar" hint={`${pct(num('tasa_asignacion_familiar'))} de la RMV si tiene hijos menores`}>
                  <select
                    value={form.tieneAsignacionFamiliar ? '1' : '0'}
                    onChange={(e) => set('tieneAsignacionFamiliar', e.target.value === '1')}
                  >
                    <option value="0">No</option>
                    <option value="1">Sí</option>
                  </select>
                </Field>
              </>
            )}
            <Field label="Estado">
              <select value={form.estado} onChange={(e) => set('estado', e.target.value)}>
                <CatalogoOptions tipo="ESTADO_CONTRATO" />
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
