import { useEffect, useState } from 'react';
import { Plus } from 'lucide-react';
import { emptyPage, http, PAGE_SIZE, pagePath } from '../api/client';
import { Alert, Badge, Button, Empty, Field, FilterBar, FormGrid, Kpi, KpiRow, Modal, Pager, SearchField, TimePicker } from '../components/ui';
import { useQuerySearch } from '../lib/useQuerySearch';
import { code, digits, label, text } from '../lib/input';
import { fmtTime } from '../lib/format';

const TABS = [
  { id: 'areas', label: 'Áreas', hint: 'Organización', path: '/api/maestros/areas', nuevo: 'Nueva área' },
  { id: 'cargos', label: 'Cargos', hint: 'Puestos', path: '/api/maestros/cargos', nuevo: 'Nuevo cargo' },
  { id: 'horarios', label: 'Horarios', hint: 'Jornadas', path: '/api/maestros/horarios', nuevo: 'Nuevo horario' },
  { id: 'tipos', label: 'Tipos de permiso', hint: 'Catálogo de ausencias', path: '/api/maestros/tipos-permiso', nuevo: 'Nuevo tipo' },
  { id: 'parametros', label: 'Parámetros', hint: 'Reglas del sistema', path: '/api/maestros/parametros', nuevo: 'Nuevo parámetro' },
  { id: 'cuentas', label: 'Cuentas', hint: 'Planilla / asientos', path: '/api/maestros/cuentas', nuevo: 'Nueva cuenta' }
];

const USOS = [
  { id: 'SUELDOS', label: 'Sueldos (asiento debe)' },
  { id: 'ESSALUD_GASTO', label: 'EsSalud gasto' },
  { id: 'ONP_POR_PAGAR', label: 'ONP por pagar' },
  { id: 'ESSALUD_POR_PAGAR', label: 'EsSalud por pagar' },
  { id: 'REMU_POR_PAGAR', label: 'Remuneraciones por pagar' },
  { id: 'DESC_AUSENCIAS', label: 'Descuentos por ausencias' }
];

const emptyByTab = {
  areas: { nombre: '', descripcion: '', activo: true },
  cargos: { nombre: '', descripcion: '', activo: true },
  horarios: { nombre: '', horaIngreso: '08:00', horaSalida: '17:00', minutosRefrigerio: '60', activo: true },
  tipos: { codigo: '', nombre: '', requiereSustento: false, activo: true },
  parametros: { clave: '', valor: '', descripcion: '' },
  cuentas: { codigo: '', nombre: '', uso: 'SUELDOS', naturaleza: 'GASTO', activo: true }
};

function paramKey(value) {
  return String(value ?? '').toLowerCase().replace(/[^a-z0-9_]/g, '').slice(0, 80);
}

function rowId(tab, row) {
  if (tab === 'areas') return row.idArea;
  if (tab === 'cargos') return row.idCargo;
  if (tab === 'horarios') return row.idHorario;
  if (tab === 'tipos') return row.idTipoPermiso;
  if (tab === 'parametros') return row.clave;
  return row.idCuenta;
}

function fromRow(tab, row) {
  if (tab === 'areas' || tab === 'cargos') {
    return { nombre: row.nombre || '', descripcion: row.descripcion || '', activo: row.activo !== false };
  }
  if (tab === 'horarios') {
    return {
      nombre: row.nombre || '',
      horaIngreso: fmtTime(row.horaIngreso, '08:00'),
      horaSalida: fmtTime(row.horaSalida, '17:00'),
      minutosRefrigerio: String(row.minutosRefrigerio ?? 60),
      activo: row.activo !== false
    };
  }
  if (tab === 'tipos') {
    return {
      codigo: row.codigo || '',
      nombre: row.nombre || '',
      requiereSustento: row.requiereSustento === true,
      activo: row.activo !== false
    };
  }
  if (tab === 'parametros') {
    return { clave: row.clave || '', valor: row.valor || '', descripcion: row.descripcion || '' };
  }
  return {
    codigo: row.codigo || '',
    nombre: row.nombre || '',
    uso: row.uso || 'SUELDOS',
    naturaleza: row.naturaleza || 'GASTO',
    activo: row.activo !== false
  };
}

function payload(tab, form) {
  if (tab === 'areas' || tab === 'cargos') {
    return { nombre: form.nombre.trim(), descripcion: form.descripcion || null, activo: form.activo !== false };
  }
  if (tab === 'horarios') {
    return {
      nombre: form.nombre.trim(),
      horaIngreso: form.horaIngreso,
      horaSalida: form.horaSalida,
      minutosRefrigerio: Number(form.minutosRefrigerio) || 0,
      activo: form.activo !== false
    };
  }
  if (tab === 'tipos') {
    return {
      codigo: form.codigo,
      nombre: form.nombre.trim(),
      requiereSustento: form.requiereSustento === true,
      activo: form.activo !== false
    };
  }
  if (tab === 'parametros') {
    return { clave: form.clave, valor: form.valor.trim(), descripcion: form.descripcion || null };
  }
  return {
    codigo: form.codigo,
    nombre: form.nombre.trim(),
    uso: form.uso,
    naturaleza: form.naturaleza,
    activo: form.activo !== false
  };
}

export function Maestros() {
  const [tab, setTab] = useState('areas');
  const [rows, setRows] = useState([]);
  const [meta, setMeta] = useState(emptyPage);
  const [page, setPage] = useState(1);
  const [estado, setEstado] = useState('');
  const [form, setForm] = useState(emptyByTab.areas);
  const [editId, setEditId] = useState(null);
  const [open, setOpen] = useState(false);
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');
  const [saving, setSaving] = useState(false);
  const [q, setQ, qDebounced] = useQuerySearch();
  const [counts, setCounts] = useState({});

  const current = TABS.find((t) => t.id === tab) || TABS[0];
  const hasEstado = tab !== 'parametros';

  useEffect(() => { setPage(1); }, [qDebounced, tab]);

  async function loadCounts() {
    const next = {};
    await Promise.all(TABS.map(async (item) => {
      const data = await http.page(pagePath(item.path, { page: 1, size: 1 }));
      next[item.id] = data.totalElements || 0;
    }));
    setCounts(next);
  }

  async function load() {
    const data = await http.page(pagePath(current.path, {
      page,
      size: PAGE_SIZE,
      q: qDebounced,
      activo: !hasEstado || estado === '' ? undefined : estado === 'activos'
    }));
    setRows(data.content || []);
    setMeta(data);
  }

  useEffect(() => {
    load().catch((e) => setError(e.message));
  }, [page, qDebounced, tab, estado]);

  useEffect(() => { loadCounts().catch(() => {}); }, []);

  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }));

  function cambiarTab(id) {
    setTab(id);
    setEstado('');
    setError('');
    setOpen(false);
  }

  function abrir(row) {
    setError('');
    if (row) {
      setEditId(rowId(tab, row));
      setForm(fromRow(tab, row));
    } else {
      setEditId(null);
      setForm(emptyByTab[tab]);
    }
    setOpen(true);
  }

  async function guardar(e) {
    e.preventDefault();
    setError('');
    setOk('');
    setSaving(true);
    const body = payload(tab, form);
    try {
      if (editId != null) {
        const path = tab === 'parametros' ? `${current.path}/${encodeURIComponent(editId)}` : `${current.path}/${editId}`;
        await http.put(path, body);
      } else {
        await http.post(current.path, body);
      }
      setOpen(false);
      setOk('Registro guardado');
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
          <h1 className="page-title mt-1">Maestros</h1>
          <p className="mt-2 text-sm text-muted">Catálogos que alimentan personal, contratos, permisos y el asiento de planilla. Desactivar oculta el ítem en los combos; no se borra por las referencias.</p>
        </div>
        <Button onClick={() => abrir(null)}><Plus size={16} /> {current.nuevo}</Button>
      </div>

      <Alert>{error}</Alert>
      <Alert ok>{ok}</Alert>

      <KpiRow>
        {TABS.map((item) => (
          <Kpi
            key={item.id}
            value={counts[item.id] ?? '—'}
            label={item.label}
            hint={item.hint}
            active={tab === item.id}
            onClick={() => cambiarTab(item.id)}
          />
        ))}
      </KpiRow>

      <FilterBar>
        <SearchField placeholder={`Buscar en ${current.label.toLowerCase()}`} value={q} onChange={(e) => { setQ(e.target.value); setPage(1); }} />
        {hasEstado && (
          <select className="w-auto" value={estado} onChange={(e) => { setEstado(e.target.value); setPage(1); }}>
            <option value="">Todos</option>
            <option value="activos">Activos</option>
            <option value="inactivos">Inactivos</option>
          </select>
        )}
      </FilterBar>

      {rows.length === 0 ? (
        <div className="rounded-xl border border-line bg-white">
          <Empty text={`No hay ${current.label.toLowerCase()} en este filtro.`} />
        </div>
      ) : (
        <div className="space-y-3">
          {rows.map((r) => (
            <article key={String(rowId(tab, r))} className="rounded-xl border border-line bg-white p-4">
              <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                <div className="min-w-0">
                  <p className="font-semibold text-navy">{r.nombre || r.clave}</p>
                  {detalle(tab, r) ? <p className="mt-1 text-xs text-muted">{detalle(tab, r)}</p> : null}
                  {r.descripcion ? <p className="mt-2 text-sm text-slate-600">{r.descripcion}</p> : null}
                </div>
                <div className="flex shrink-0 flex-col items-start gap-2 sm:items-end">
                  {tab === 'parametros' ? (
                    <Badge value="SISTEMA" />
                  ) : (
                    <Badge value={r.activo ? 'ACTIVO' : 'INACTIVO'} />
                  )}
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
        <Modal title={editId != null ? `Editar ${current.label.toLowerCase().replace(/s$/, '')}` : current.nuevo} onClose={() => setOpen(false)}>
          <FormGrid onSubmit={guardar}>
            {(tab === 'areas' || tab === 'cargos') && (
              <>
                <Field label="Nombre">
                  <input value={form.nombre} onChange={(e) => set('nombre', label(e.target.value))} required />
                </Field>
                <Field label="Estado">
                  <select value={form.activo ? '1' : '0'} onChange={(e) => set('activo', e.target.value === '1')}>
                    <option value="1">Activo</option>
                    <option value="0">Inactivo</option>
                  </select>
                </Field>
                <Field label="Descripción" full>
                  <input value={form.descripcion} onChange={(e) => set('descripcion', text(e.target.value, 250))} />
                </Field>
              </>
            )}
            {tab === 'horarios' && (
              <>
                <Field label="Nombre">
                  <input value={form.nombre} onChange={(e) => set('nombre', label(e.target.value))} required />
                </Field>
                <Field label="Estado">
                  <select value={form.activo ? '1' : '0'} onChange={(e) => set('activo', e.target.value === '1')}>
                    <option value="1">Activo</option>
                    <option value="0">Inactivo</option>
                  </select>
                </Field>
                <Field label="Ingreso">
                  <TimePicker value={form.horaIngreso} onChange={(v) => set('horaIngreso', v)} required />
                </Field>
                <Field label="Salida">
                  <TimePicker value={form.horaSalida} onChange={(v) => set('horaSalida', v)} required />
                </Field>
                <Field label="Refrigerio (minutos)" hint="0 a 180">
                  <input inputMode="numeric" value={form.minutosRefrigerio} onChange={(e) => set('minutosRefrigerio', digits(e.target.value, 3))} required />
                </Field>
              </>
            )}
            {tab === 'tipos' && (
              <>
                <Field label="Código" hint="Ej. VACACIONES, SALUD">
                  <input value={form.codigo} onChange={(e) => set('codigo', code(e.target.value, 30))} required disabled={editId != null && form.codigo === 'VACACIONES'} />
                </Field>
                <Field label="Nombre">
                  <input value={form.nombre} onChange={(e) => set('nombre', label(e.target.value))} required />
                </Field>
                <Field label="Sustento">
                  <select value={form.requiereSustento ? '1' : '0'} onChange={(e) => set('requiereSustento', e.target.value === '1')}>
                    <option value="0">Opcional</option>
                    <option value="1">Obligatorio</option>
                  </select>
                </Field>
                <Field label="Estado">
                  <select value={form.activo ? '1' : '0'} onChange={(e) => set('activo', e.target.value === '1')} disabled={form.codigo === 'VACACIONES'}>
                    <option value="1">Activo</option>
                    <option value="0">Inactivo</option>
                  </select>
                </Field>
              </>
            )}
            {tab === 'parametros' && (
              <>
                <Field label="Clave" hint="minúsculas y guion bajo">
                  <input value={form.clave} onChange={(e) => set('clave', paramKey(e.target.value))} required disabled={editId != null} />
                </Field>
                <Field label="Valor">
                  <input value={form.valor} onChange={(e) => set('valor', text(e.target.value, 200))} required />
                </Field>
                <Field label="Descripción" full>
                  <input value={form.descripcion} onChange={(e) => set('descripcion', text(e.target.value, 300))} />
                </Field>
              </>
            )}
            {tab === 'cuentas' && (
              <>
                <Field label="Código PCGE" hint="Ej. 6211">
                  <input value={form.codigo} onChange={(e) => set('codigo', code(e.target.value, 20))} required />
                </Field>
                <Field label="Nombre">
                  <input value={form.nombre} onChange={(e) => set('nombre', label(e.target.value))} required />
                </Field>
                <Field label="Uso en planilla">
                  <select value={form.uso} onChange={(e) => set('uso', e.target.value)} required>
                    {USOS.map((u) => <option key={u.id} value={u.id}>{u.label}</option>)}
                  </select>
                </Field>
                <Field label="Naturaleza">
                  <select value={form.naturaleza} onChange={(e) => set('naturaleza', e.target.value)}>
                    <option value="GASTO">Gasto</option>
                    <option value="PASIVO">Pasivo</option>
                    <option value="ACTIVO">Activo</option>
                  </select>
                </Field>
                <Field label="Estado">
                  <select value={form.activo ? '1' : '0'} onChange={(e) => set('activo', e.target.value === '1')}>
                    <option value="1">Activo</option>
                    <option value="0">Inactivo</option>
                  </select>
                </Field>
              </>
            )}
            <div className="md:col-span-2">
              <Button type="submit" disabled={saving}>{saving ? 'Guardando…' : 'Guardar'}</Button>
            </div>
          </FormGrid>
        </Modal>
      )}
    </div>
  );
}

function detalle(tab, r) {
  if (tab === 'horarios') {
    return `${fmtTime(r.horaIngreso)} – ${fmtTime(r.horaSalida)} · refrigerio ${r.minutosRefrigerio ?? 0} min`;
  }
  if (tab === 'tipos') {
    return `${r.codigo} · ${r.requiereSustento ? 'Sustento obligatorio' : 'Sustento opcional'}`;
  }
  if (tab === 'parametros') {
    return `Valor: ${r.valor}`;
  }
  if (tab === 'cuentas') {
    const uso = USOS.find((u) => u.id === r.uso);
    return `${r.codigo} · ${uso ? uso.label : r.uso} · ${r.naturaleza}`;
  }
  return r.descripcion ? '' : 'Sin descripción';
}
