import { useEffect, useState } from 'react';
import { Plus } from 'lucide-react';
import { http, PAGE_SIZE, pagePath } from '../api/client';
import { useConfig } from '../auth/ConfigContext';
import { Alert, Badge, Button, CatalogoOptions, DataList, DatePicker, Field, FilterBar, FormGrid, Kpi, KpiRow, ListActions, MobileRow, Modal, Pager, SearchField, TimePicker } from '../components/ui';
import { useQuerySearch } from '../lib/useQuerySearch';
import { usePagedLoad } from '../lib/usePagedLoad';
import { code, decimal, digits, label, text } from '../lib/input';
import { fmtDate, fmtTime } from '../lib/format';

const TABS = [
  { id: 'areas', label: 'Áreas', singular: 'área', hint: 'Organización', path: '/api/maestros/areas', nuevo: 'Nueva área' },
  { id: 'cargos', label: 'Cargos', singular: 'cargo', hint: 'Puestos', path: '/api/maestros/cargos', nuevo: 'Nuevo cargo' },
  { id: 'horarios', label: 'Horarios', singular: 'horario', hint: 'Jornadas', path: '/api/maestros/horarios', nuevo: 'Nuevo horario' },
  { id: 'tipos', label: 'Tipos de permiso', singular: 'tipo de permiso', hint: 'Catálogo de ausencias', path: '/api/maestros/tipos-permiso', nuevo: 'Nuevo tipo' },
  { id: 'cuentas', label: 'Cuentas', singular: 'cuenta', hint: 'Planilla / asientos', path: '/api/maestros/cuentas', nuevo: 'Nueva cuenta' },
  { id: 'catalogos', label: 'Catálogos', singular: 'valor de catálogo', hint: 'Combos, etiquetas y colores', path: '/api/maestros/catalogos', nuevo: 'Nuevo valor' },
  { id: 'parametros', label: 'Parámetros', singular: 'parámetro', hint: 'Reglas y empresa', path: '/api/maestros/parametros', nuevo: 'Nuevo parámetro', sinEstado: true },
  { id: 'vigencias', label: 'Valores por fecha', singular: 'valor por fecha', hint: 'RMV, UIT…', path: '/api/maestros/vigencias', nuevo: 'Nuevo valor', sinEstado: true },
  { id: 'afps', label: 'AFP', singular: 'AFP', hint: 'Comisiones', path: '/api/maestros/afps', nuevo: 'Nueva AFP' },
  { id: 'regimenes', label: 'Regímenes laborales', singular: 'régimen laboral', hint: 'Gratificación / CTS', path: '/api/maestros/regimenes-laborales', nuevo: 'Nuevo régimen' },
  { id: 'tramos', label: 'Tramos de 5ta', singular: 'tramo', hint: 'Renta de quinta', path: '/api/maestros/tramos-quinta', nuevo: 'Nuevo tramo' }
];

const AMBITOS = [
  { id: 'PUBLICO', corto: 'Público', label: 'Público (visible antes de iniciar sesión)' },
  { id: 'SESION', corto: 'Sesión', label: 'Sesión (usuarios autenticados)' },
  { id: 'PRIVADO', corto: 'Privado', label: 'Privado (solo servidor)' }
];

const TONOS = [
  { id: '', label: 'Sin color' },
  { id: 'ok', label: 'Verde' },
  { id: 'warn', label: 'Ámbar' },
  { id: 'danger', label: 'Rojo' },
  { id: 'info', label: 'Azul' },
  { id: 'neutral', label: 'Neutro' },
  { id: 'muted', label: 'Atenuado' }
];

function paramKey(value) {
  return String(value ?? '').toLowerCase().replace(/[^a-z0-9_]/g, '').slice(0, 80);
}

function pct(value) {
  const n = Number(value);
  return Number.isFinite(n) ? `${Math.round(n * 10000) / 100}%` : '—';
}

function vacio(tab, ctx) {
  switch (tab) {
    case 'areas':
    case 'cargos': return { nombre: '', descripcion: '', activo: true };
    case 'horarios': return { nombre: '', horaIngreso: '', horaSalida: '', minutosRefrigerio: String(ctx.num('horario_refrigerio_defecto', 0)), activo: true };
    case 'tipos': return { codigo: '', nombre: '', requiereSustento: false, esVacaciones: false, activo: true };
    case 'parametros': return { clave: '', valor: '', descripcion: '', ambito: 'SESION' };
    case 'cuentas': return { codigo: '', nombre: '', uso: ctx.porDefecto('USO_CUENTA'), naturaleza: ctx.porDefecto('NATURALEZA_CUENTA'), activo: true };
    case 'catalogos': return { tipo: ctx.tipoCatalogo, codigo: '', nombre: '', tono: '', orden: '0', porDefecto: false, regla: '', mensajeRegla: '', activo: true };
    case 'vigencias': return { clave: '', valor: '', vigenteDesde: '', descripcion: '' };
    case 'afps': return { codigo: '', nombre: '', tasaComision: '', orden: '0', activo: true };
    case 'regimenes': return { codigo: '', nombre: '', descripcion: '', factorGratificacion: '', factorCts: '', aplicaEssalud: true, porDefecto: false, orden: '0', activo: true };
    case 'tramos': return { orden: '', hastaUit: '', tasa: '', activo: true };
    default: return {};
  }
}

function rowId(tab, row) {
  switch (tab) {
    case 'areas': return row.idArea;
    case 'cargos': return row.idCargo;
    case 'horarios': return row.idHorario;
    case 'tipos': return row.idTipoPermiso;
    case 'parametros': return row.clave;
    case 'catalogos': return row.idValor;
    case 'vigencias': return row.idVigencia;
    case 'afps':
    case 'regimenes': return row.codigo;
    case 'tramos': return row.idTramo;
    default: return row.idCuenta;
  }
}

const str = (v) => (v == null ? '' : String(v));

function fromRow(tab, row) {
  switch (tab) {
    case 'areas':
    case 'cargos': return { nombre: row.nombre || '', descripcion: row.descripcion || '', activo: row.activo !== false };
    case 'horarios': return {
      nombre: row.nombre || '',
      horaIngreso: fmtTime(row.horaIngreso, ''),
      horaSalida: fmtTime(row.horaSalida, ''),
      minutosRefrigerio: str(row.minutosRefrigerio),
      activo: row.activo !== false
    };
    case 'tipos': return {
      codigo: row.codigo || '',
      nombre: row.nombre || '',
      requiereSustento: row.requiereSustento === true,
      esVacaciones: row.esVacaciones === true,
      activo: row.activo !== false
    };
    case 'parametros': return { clave: row.clave || '', valor: row.valor || '', descripcion: row.descripcion || '', ambito: row.ambito || 'SESION' };
    case 'catalogos': return {
      tipo: row.tipo, codigo: row.codigo || '', nombre: row.nombre || '', tono: row.tono || '', orden: str(row.orden),
      porDefecto: row.porDefecto === true, regla: row.regla || '', mensajeRegla: row.mensajeRegla || '', activo: row.activo !== false
    };
    case 'vigencias': return { clave: row.clave || '', valor: row.valor || '', vigenteDesde: row.vigenteDesde || '', descripcion: row.descripcion || '' };
    case 'afps': return { codigo: row.codigo || '', nombre: row.nombre || '', tasaComision: str(row.tasaComision), orden: str(row.orden), activo: row.activo !== false };
    case 'regimenes': return {
      codigo: row.codigo || '', nombre: row.nombre || '', descripcion: row.descripcion || '',
      factorGratificacion: str(row.factorGratificacion), factorCts: str(row.factorCts),
      aplicaEssalud: row.aplicaEssalud !== false, porDefecto: row.porDefecto === true, orden: str(row.orden), activo: row.activo !== false
    };
    case 'tramos': return { orden: str(row.orden), hastaUit: str(row.hastaUit), tasa: str(row.tasa), activo: row.activo !== false };
    default: return {
      codigo: row.codigo || '',
      nombre: row.nombre || '',
      uso: row.uso || '',
      naturaleza: row.naturaleza || '',
      activo: row.activo !== false
    };
  }
}

const numOrNull = (v) => (v === '' || v == null ? null : Number(v));

function payload(tab, form) {
  const activo = form.activo !== false;
  switch (tab) {
    case 'areas':
    case 'cargos': return { nombre: form.nombre.trim(), descripcion: form.descripcion || null, activo };
    case 'horarios': return {
      nombre: form.nombre.trim(),
      horaIngreso: form.horaIngreso,
      horaSalida: form.horaSalida,
      minutosRefrigerio: Number(form.minutosRefrigerio) || 0,
      activo
    };
    case 'tipos': return {
      codigo: form.codigo,
      nombre: form.nombre.trim(),
      requiereSustento: form.requiereSustento === true,
      esVacaciones: form.esVacaciones === true,
      activo
    };
    case 'parametros': return { clave: form.clave, valor: form.valor.trim(), descripcion: form.descripcion || null, ambito: form.ambito || null };
    case 'catalogos': return {
      tipo: form.tipo, codigo: form.codigo, nombre: form.nombre.trim(), tono: form.tono || null, orden: Number(form.orden) || 0,
      porDefecto: form.porDefecto === true, regla: form.regla || null, mensajeRegla: form.mensajeRegla || null, activo
    };
    case 'vigencias': return { clave: form.clave, valor: form.valor.trim(), vigenteDesde: form.vigenteDesde, descripcion: form.descripcion || null };
    case 'afps': return { codigo: form.codigo, nombre: form.nombre.trim(), tasaComision: Number(form.tasaComision), orden: Number(form.orden) || 0, activo };
    case 'regimenes': return {
      codigo: form.codigo, nombre: form.nombre.trim(), descripcion: form.descripcion || null,
      factorGratificacion: Number(form.factorGratificacion), factorCts: Number(form.factorCts),
      aplicaEssalud: form.aplicaEssalud === true, porDefecto: form.porDefecto === true, orden: Number(form.orden) || 0, activo
    };
    case 'tramos': return { orden: Number(form.orden), hastaUit: numOrNull(form.hastaUit), tasa: Number(form.tasa), activo };
    default: return { codigo: form.codigo, nombre: form.nombre.trim(), uso: form.uso, naturaleza: form.naturaleza, activo };
  }
}

function titulo(tab, r) {
  if (tab === 'tramos') return `Tramo ${r.orden}`;
  return r.nombre || r.clave;
}

function EstadoSelect({ form, set, disabled }) {
  return (
    <Field label="Estado">
      <select value={form.activo ? '1' : '0'} onChange={(e) => set('activo', e.target.value === '1')} disabled={disabled}>
        <option value="1">Activo</option>
        <option value="0">Inactivo</option>
      </select>
    </Field>
  );
}

function SiNo({ label: etiqueta, value, onChange, si = 'Sí', no = 'No', hint }) {
  return (
    <Field label={etiqueta} hint={hint}>
      <select value={value ? '1' : '0'} onChange={(e) => onChange(e.target.value === '1')}>
        <option value="0">{no}</option>
        <option value="1">{si}</option>
      </select>
    </Field>
  );
}

export function Maestros() {
  const config = useConfig();
  const { num, etiqueta, recargar } = config;
  const [tab, setTab] = useState('areas');
  const [estado, setEstado] = useState('');
  const [tipoCatalogo, setTipoCatalogo] = useState('');
  const [tiposCatalogo, setTiposCatalogo] = useState([]);
  const [form, setForm] = useState({});
  const [editId, setEditId] = useState(null);
  const [open, setOpen] = useState(false);
  const [ok, setOk] = useState('');
  const [saving, setSaving] = useState(false);
  const [q, setQ, qDebounced, resetQ] = useQuerySearch();
  const [counts, setCounts] = useState({});

  const current = TABS.find((t) => t.id === tab) || TABS[0];
  const hasEstado = !current.sinEstado;
  const tipoSel = tiposCatalogo.find((t) => t.codigo === tipoCatalogo);
  const puedeCrear = tab !== 'catalogos' || (tipoCatalogo ? Boolean(tipoSel?.extensible) : tiposCatalogo.some((t) => t.extensible));
  const refrigerioMax = num('horario_refrigerio_max', 0);

  const { page, setPage, rows, meta, error, setError, loading, reload } = usePagedLoad(
    [qDebounced, tab, estado, tipoCatalogo, current.path],
    (pageNum) => http.page(pagePath(current.path, {
      page: pageNum,
      size: PAGE_SIZE,
      q: qDebounced,
      tipo: tab === 'catalogos' ? tipoCatalogo : undefined,
      activo: !hasEstado || estado === '' ? undefined : estado === 'activos'
    })).then((data) => ({ ...data, tab }))
  );
  // Cada pestaña tiene filas de distinta forma: no pintar las que llegaron para otra pestaña.
  const delTab = meta.tab === tab;
  const filas = delTab ? rows : [];

  async function loadCounts() {
    const results = await Promise.allSettled(TABS.map((item) => http.page(pagePath(item.path, { page: 1, size: 1 }))));
    const next = {};
    results.forEach((r, i) => {
      if (r.status === 'fulfilled') next[TABS[i].id] = r.value.totalElements || 0;
    });
    setCounts(next);
  }

  useEffect(() => {
    loadCounts();
    http.get('/api/maestros/catalogo-tipos').then(setTiposCatalogo).catch(() => {});
  }, []);

  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }));

  function cambiarTab(id) {
    setTab(id);
    setEstado('');
    setTipoCatalogo('');
    resetQ();
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
      setForm(vacio(tab, { ...config, tipoCatalogo }));
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
      if (editId != null) await http.put(`${current.path}/${encodeURIComponent(editId)}`, body);
      else await http.post(current.path, body);
      setOpen(false);
      setOk('Registro guardado');
      await Promise.all([reload(), loadCounts()]);
      if (tab === 'parametros' || tab === 'catalogos') recargar();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  async function eliminar(row) {
    if (!window.confirm(`¿Eliminar el valor de ${row.clave} vigente desde ${fmtDate(row.vigenteDesde)}?`)) return;
    setError('');
    try {
      await http.delete(`${current.path}/${rowId(tab, row)}`);
      setOk('Registro eliminado');
      await Promise.all([reload(), loadCounts()]);
    } catch (err) {
      setError(err.message);
    }
  }

  function detalle(r) {
    switch (tab) {
      case 'horarios': return `${fmtTime(r.horaIngreso)} – ${fmtTime(r.horaSalida)} · refrigerio ${r.minutosRefrigerio ?? 0} min`;
      case 'tipos': return `${r.codigo} · ${r.requiereSustento ? 'Sustento obligatorio' : 'Sustento opcional'}${r.esVacaciones ? ' · Descuenta vacaciones' : ''}`;
      case 'parametros': return `Valor: ${r.valor} · ${AMBITOS.find((a) => a.id === r.ambito)?.corto || r.ambito || ''}`;
      case 'cuentas': return `${r.codigo} · ${etiqueta('USO_CUENTA', r.uso)} · ${etiqueta('NATURALEZA_CUENTA', r.naturaleza)}`;
      case 'catalogos': return `${r.tipoNombre} · ${r.codigo}${r.porDefecto ? ' · por defecto' : ''}`;
      case 'vigencias': return `${r.clave} = ${r.valor} desde ${fmtDate(r.vigenteDesde)}`;
      case 'afps': return `${r.codigo} · comisión ${pct(r.tasaComision)}`;
      case 'regimenes': return `Gratificación ×${r.factorGratificacion} · CTS ×${r.factorCts} · ${r.aplicaEssalud ? 'con EsSalud' : 'sin EsSalud'}${r.porDefecto ? ' · por defecto' : ''}`;
      case 'tramos': return `Hasta ${r.hastaUit != null ? `${r.hastaUit} UIT` : 'sin tope'} · tasa ${pct(r.tasa)}`;
      default: return r.descripcion ? '' : 'Sin descripción';
    }
  }

  function estadoBadge(r) {
    if (!hasEstado) return <Badge tipo="ESTADO_REGISTRO" value="SISTEMA" />;
    return <Badge tipo="ESTADO_REGISTRO" value={r.activo ? 'ACTIVO' : 'INACTIVO'} />;
  }

  function acciones(r) {
    return (
      <ListActions>
        <Button variant="secondary" className="px-3 py-1.5 text-xs" onClick={() => abrir(r)}>Editar</Button>
        {tab === 'vigencias' && (
          <Button variant="secondary" className="px-3 py-1.5 text-xs" onClick={() => eliminar(r)}>Eliminar</Button>
        )}
      </ListActions>
    );
  }

  return (
    <div>
      <div className="mb-6 flex flex-col gap-4 border-b border-line pb-5 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-500">Administración</p>
          <h1 className="page-title mt-1">Maestros</h1>
          <p className="mt-2 text-sm text-muted">Catálogos y reglas que alimentan personal, contratos, permisos, planilla y la marca de la empresa. Desactivar oculta el ítem en los combos; no se borra por las referencias.</p>
        </div>
        {puedeCrear && <Button onClick={() => abrir(null)}><Plus size={16} /> {current.nuevo}</Button>}
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
        {tab === 'catalogos' && (
          <select className="w-auto" value={tipoCatalogo} onChange={(e) => { setTipoCatalogo(e.target.value); setPage(1); }}>
            <option value="">Todos los catálogos</option>
            {tiposCatalogo.map((t) => <option key={t.codigo} value={t.codigo}>{t.nombre}</option>)}
          </select>
        )}
        {hasEstado && (
          <select className="w-auto" value={estado} onChange={(e) => { setEstado(e.target.value); setPage(1); }}>
            <option value="">Todos</option>
            <option value="activos">Activos</option>
            <option value="inactivos">Inactivos</option>
          </select>
        )}
        {(q || estado || tipoCatalogo) && (
          <button type="button" className="text-xs font-medium text-navy hover:underline" onClick={() => { resetQ(); setEstado(''); setTipoCatalogo(''); setPage(1); }}>
            Limpiar filtros
          </button>
        )}
      </FilterBar>
      {tab === 'catalogos' && tipoCatalogo && tipoSel && !tipoSel.extensible && (
        <p className="mb-3 text-xs text-muted">Los códigos de este catálogo los usa la lógica del sistema: solo se pueden editar la etiqueta, el color, el orden y el valor por defecto.</p>
      )}

      <DataList
        empty={filas.length === 0}
        emptyText={loading ? 'Cargando…' : `No hay ${current.label.toLowerCase()} en este filtro.`}
        cards={filas.map((r) => (
          <MobileRow
            key={String(rowId(tab, r))}
            title={titulo(tab, r)}
            meta={detalle(r) || undefined}
            badge={estadoBadge(r)}
            actions={acciones(r)}
          />
        ))}
        table={(
          <table>
            <thead>
              <tr>
                <th>Nombre</th>
                <th>Detalle</th>
                <th>Estado</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {filas.map((r) => (
                <tr key={String(rowId(tab, r))}>
                  <td>
                    <p className="font-medium text-navy">{titulo(tab, r)}</p>
                    {r.descripcion ? <p className="text-xs text-muted">{r.descripcion}</p> : null}
                  </td>
                  <td className="text-sm text-muted">
                    {tab === 'catalogos' ? <Badge tipo={r.tipo} value={r.codigo} /> : null} {detalle(r) || '—'}
                  </td>
                  <td>{estadoBadge(r)}</td>
                  <td>{acciones(r)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        footer={delTab && (
          <Pager page={meta.page} totalPages={meta.totalPages} totalElements={meta.totalElements} size={meta.size} onPage={setPage} />
        )}
      />

      {open && (
        <Modal title={editId != null ? `Editar ${current.singular}` : current.nuevo} onClose={() => setOpen(false)}>
          <Alert>{error}</Alert>
          <FormGrid onSubmit={guardar}>
            {(tab === 'areas' || tab === 'cargos') && (
              <>
                <Field label="Nombre">
                  <input value={form.nombre} onChange={(e) => set('nombre', label(e.target.value))} required />
                </Field>
                <EstadoSelect form={form} set={set} />
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
                <EstadoSelect form={form} set={set} />
                <Field label="Ingreso">
                  <TimePicker value={form.horaIngreso} onChange={(v) => set('horaIngreso', v)} required />
                </Field>
                <Field label="Salida">
                  <TimePicker value={form.horaSalida} onChange={(v) => set('horaSalida', v)} required />
                </Field>
                <Field label="Refrigerio (minutos)" hint={refrigerioMax ? `0 a ${refrigerioMax}` : undefined}>
                  <input inputMode="numeric" value={form.minutosRefrigerio} onChange={(e) => {
                    const v = digits(e.target.value, 3);
                    set('minutosRefrigerio', v === '' || !refrigerioMax ? v : String(Math.min(Number(v), refrigerioMax)));
                  }} required />
                </Field>
              </>
            )}
            {tab === 'tipos' && (
              <>
                <Field label="Código">
                  <input value={form.codigo} onChange={(e) => set('codigo', code(e.target.value, 30))} required disabled={editId != null && form.esVacaciones} />
                </Field>
                <Field label="Nombre">
                  <input value={form.nombre} onChange={(e) => set('nombre', label(e.target.value))} required />
                </Field>
                <SiNo label="Sustento" value={form.requiereSustento} onChange={(v) => set('requiereSustento', v)} si="Obligatorio" no="Opcional" />
                <SiNo label="Descuenta vacaciones" value={form.esVacaciones} onChange={(v) => set('esVacaciones', v)} hint="Valida y descuenta el saldo vacacional" />
                <EstadoSelect form={form} set={set} disabled={form.esVacaciones} />
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
                <Field label="Visibilidad" full>
                  <select value={form.ambito} onChange={(e) => set('ambito', e.target.value)}>
                    {AMBITOS.map((a) => <option key={a.id} value={a.id}>{a.label}</option>)}
                  </select>
                </Field>
                <Field label="Descripción" full>
                  <input value={form.descripcion} onChange={(e) => set('descripcion', text(e.target.value, 300))} />
                </Field>
              </>
            )}
            {tab === 'cuentas' && (
              <>
                <Field label="Código contable">
                  <input value={form.codigo} onChange={(e) => set('codigo', code(e.target.value, 20))} required />
                </Field>
                <Field label="Nombre">
                  <input value={form.nombre} onChange={(e) => set('nombre', label(e.target.value))} required />
                </Field>
                <Field label="Uso en planilla">
                  <select value={form.uso} onChange={(e) => set('uso', e.target.value)} required>
                    <option value="">Seleccione</option>
                    <CatalogoOptions tipo="USO_CUENTA" />
                  </select>
                </Field>
                <Field label="Naturaleza">
                  <select value={form.naturaleza} onChange={(e) => set('naturaleza', e.target.value)} required>
                    <option value="">Seleccione</option>
                    <CatalogoOptions tipo="NATURALEZA_CUENTA" />
                  </select>
                </Field>
                <EstadoSelect form={form} set={set} />
              </>
            )}
            {tab === 'catalogos' && (
              <>
                <Field label="Catálogo">
                  <select value={form.tipo} onChange={(e) => set('tipo', e.target.value)} required disabled={editId != null}>
                    <option value="">Seleccione</option>
                    {tiposCatalogo.filter((t) => editId != null || t.extensible).map((t) => (
                      <option key={t.codigo} value={t.codigo}>{t.nombre}</option>
                    ))}
                  </select>
                </Field>
                <Field label="Código">
                  <input value={form.codigo} onChange={(e) => set('codigo', code(e.target.value, 40))} required disabled={editId != null} />
                </Field>
                <Field label="Etiqueta">
                  <input value={form.nombre} onChange={(e) => set('nombre', text(e.target.value, 120))} required />
                </Field>
                <Field label="Color">
                  <select value={form.tono} onChange={(e) => set('tono', e.target.value)}>
                    {TONOS.map((t) => <option key={t.id} value={t.id}>{t.label}</option>)}
                  </select>
                </Field>
                <Field label="Orden">
                  <input inputMode="numeric" value={form.orden} onChange={(e) => set('orden', digits(e.target.value, 4))} />
                </Field>
                <SiNo label="Valor por defecto" value={form.porDefecto} onChange={(v) => set('porDefecto', v)} />
                <Field label="Validación (expresión regular)" full hint="Opcional. Ej. ^\d{8}$ para un DNI">
                  <input value={form.regla} onChange={(e) => set('regla', text(e.target.value, 200))} />
                </Field>
                <Field label="Mensaje si no cumple" full>
                  <input value={form.mensajeRegla} onChange={(e) => set('mensajeRegla', text(e.target.value, 200))} />
                </Field>
                <EstadoSelect form={form} set={set} />
              </>
            )}
            {tab === 'vigencias' && (
              <>
                <Field label="Clave" hint="Ej. rmv, uit">
                  <input value={form.clave} onChange={(e) => set('clave', paramKey(e.target.value))} required disabled={editId != null} />
                </Field>
                <Field label="Valor">
                  <input value={form.valor} onChange={(e) => set('valor', text(e.target.value, 200))} required />
                </Field>
                <Field label="Vigente desde">
                  <DatePicker value={form.vigenteDesde} onChange={(v) => set('vigenteDesde', v)} required />
                </Field>
                <Field label="Descripción" full>
                  <input value={form.descripcion} onChange={(e) => set('descripcion', text(e.target.value, 300))} />
                </Field>
              </>
            )}
            {tab === 'afps' && (
              <>
                <Field label="Código">
                  <input value={form.codigo} onChange={(e) => set('codigo', code(e.target.value, 20))} required disabled={editId != null} />
                </Field>
                <Field label="Nombre">
                  <input value={form.nombre} onChange={(e) => set('nombre', label(e.target.value))} required />
                </Field>
                <Field label="Comisión" hint="Fracción: 0.0147 = 1.47%">
                  <input inputMode="decimal" value={form.tasaComision} onChange={(e) => set('tasaComision', decimal(e.target.value, 1))} required />
                </Field>
                <Field label="Orden">
                  <input inputMode="numeric" value={form.orden} onChange={(e) => set('orden', digits(e.target.value, 4))} />
                </Field>
                <EstadoSelect form={form} set={set} />
              </>
            )}
            {tab === 'regimenes' && (
              <>
                <Field label="Código">
                  <input value={form.codigo} onChange={(e) => set('codigo', code(e.target.value, 20))} required disabled={editId != null} />
                </Field>
                <Field label="Nombre">
                  <input value={form.nombre} onChange={(e) => set('nombre', label(e.target.value))} required />
                </Field>
                <Field label="Factor de gratificación" hint="1 = sueldo completo, 0.5 = medio sueldo">
                  <input inputMode="decimal" value={form.factorGratificacion} onChange={(e) => set('factorGratificacion', decimal(e.target.value, 9.9999))} required />
                </Field>
                <Field label="Factor de CTS">
                  <input inputMode="decimal" value={form.factorCts} onChange={(e) => set('factorCts', decimal(e.target.value, 9.9999))} required />
                </Field>
                <SiNo label="Aporta EsSalud" value={form.aplicaEssalud} onChange={(v) => set('aplicaEssalud', v)} />
                <SiNo label="Régimen por defecto" value={form.porDefecto} onChange={(v) => set('porDefecto', v)} />
                <Field label="Orden">
                  <input inputMode="numeric" value={form.orden} onChange={(e) => set('orden', digits(e.target.value, 4))} />
                </Field>
                <EstadoSelect form={form} set={set} />
                <Field label="Descripción" full>
                  <input value={form.descripcion} onChange={(e) => set('descripcion', text(e.target.value, 250))} />
                </Field>
              </>
            )}
            {tab === 'tramos' && (
              <>
                <Field label="Orden">
                  <input inputMode="numeric" value={form.orden} onChange={(e) => set('orden', digits(e.target.value, 3))} required />
                </Field>
                <Field label="Hasta (UIT)" hint="Vacío = sin tope (último tramo)">
                  <input inputMode="decimal" value={form.hastaUit} onChange={(e) => set('hastaUit', decimal(e.target.value, 99999))} />
                </Field>
                <Field label="Tasa" hint="Fracción: 0.08 = 8%">
                  <input inputMode="decimal" value={form.tasa} onChange={(e) => set('tasa', decimal(e.target.value, 1))} required />
                </Field>
                <EstadoSelect form={form} set={set} />
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
