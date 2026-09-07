import { useEffect, useState } from 'react';
import { Plus } from 'lucide-react';
import { emptyPage, http, PAGE_SIZE, pagePath } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { MENU_ICON_OPTIONS, menuIcon } from '../layout/icons';
import { Alert, Badge, Button, Empty, Field, FilterBar, FormGrid, Kpi, KpiRow, Modal, Pager, SearchField } from '../components/ui';

const GRUPOS = ['Operación', 'Administración', 'Control'];
const empty = {
  codigo: '', etiqueta: '', ruta: '', icono: 'Home', grupo: 'Operación',
  descripcion: '', orden: 10, activo: true, idPerfiles: []
};

export function Menus() {
  const { refreshSesion } = useAuth();
  const [rows, setRows] = useState([]);
  const [meta, setMeta] = useState(emptyPage);
  const [page, setPage] = useState(1);
  const [perfiles, setPerfiles] = useState([]);
  const [form, setForm] = useState(empty);
  const [editId, setEditId] = useState(null);
  const [open, setOpen] = useState(false);
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');
  const [saving, setSaving] = useState(false);
  const [tab, setTab] = useState('');
  const [estado, setEstado] = useState('');
  const [q, setQ] = useState('');
  const [qDebounced, setQDebounced] = useState('');
  const [counts, setCounts] = useState({ total: 0, operacion: 0, administracion: 0, control: 0, activos: 0, inactivos: 0 });

  useEffect(() => {
    const t = setTimeout(() => setQDebounced(q.trim()), 300);
    return () => clearTimeout(t);
  }, [q]);

  useEffect(() => {
    http.get('/api/catalogos/roles')
      .then(setPerfiles)
      .catch((e) => setError(e.message));
  }, []);

  async function loadCounts() {
    const [all, op, ad, co, act, ina] = await Promise.all([
      http.page(pagePath('/api/menus', { page: 1, size: 1 })),
      http.page(pagePath('/api/menus', { page: 1, size: 1, grupo: 'Operación' })),
      http.page(pagePath('/api/menus', { page: 1, size: 1, grupo: 'Administración' })),
      http.page(pagePath('/api/menus', { page: 1, size: 1, grupo: 'Control' })),
      http.page(pagePath('/api/menus', { page: 1, size: 1, activo: true })),
      http.page(pagePath('/api/menus', { page: 1, size: 1, activo: false }))
    ]);
    setCounts({
      total: all.totalElements || 0,
      operacion: op.totalElements || 0,
      administracion: ad.totalElements || 0,
      control: co.totalElements || 0,
      activos: act.totalElements || 0,
      inactivos: ina.totalElements || 0
    });
  }

  async function load() {
    const data = await http.page(pagePath('/api/menus', {
      page,
      size: PAGE_SIZE,
      q: qDebounced,
      grupo: tab,
      activo: estado === '' ? undefined : estado === 'activos'
    }));
    setRows(data.content || []);
    setMeta(data);
  }

  useEffect(() => {
    load().catch((e) => setError(e.message));
  }, [page, qDebounced, tab, estado]);

  useEffect(() => { loadCounts().catch(() => {}); }, []);

  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }));

  function abrir(row) {
    setError('');
    if (row) {
      setEditId(row.idMenu);
      setForm({
        codigo: row.codigo,
        etiqueta: row.etiqueta,
        ruta: row.ruta,
        icono: row.icono,
        grupo: row.grupo,
        descripcion: row.descripcion || '',
        orden: row.orden ?? 10,
        activo: row.activo !== false,
        idPerfiles: row.idPerfiles || []
      });
    } else {
      setEditId(null);
      setForm(empty);
    }
    setOpen(true);
  }

  function togglePerfil(id) {
    setForm((f) => {
      const has = f.idPerfiles.includes(id);
      return { ...f, idPerfiles: has ? f.idPerfiles.filter((x) => x !== id) : [...f.idPerfiles, id] };
    });
  }

  async function guardar(e) {
    e.preventDefault();
    setError('');
    setOk('');
    setSaving(true);
    const body = {
      codigo: form.codigo,
      etiqueta: form.etiqueta,
      ruta: form.ruta,
      icono: form.icono,
      grupo: form.grupo,
      descripcion: form.descripcion || null,
      orden: Number(form.orden) || 0,
      activo: form.activo !== false,
      idPerfiles: form.idPerfiles
    };
    try {
      if (editId) await http.put(`/api/menus/${editId}`, body);
      else await http.post('/api/menus', body);
      setOpen(false);
      setOk('Opción de menú guardada');
      await Promise.all([load(), loadCounts()]);
      await refreshSesion();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  async function eliminar(row) {
    if (!window.confirm(`¿Eliminar la opción ${row.etiqueta}? El menú de las cuentas se actualizará al recargar.`)) return;
    setError('');
    try {
      await http.delete(`/api/menus/${row.idMenu}`);
      setOk('Opción eliminada');
      await Promise.all([load(), loadCounts()]);
      await refreshSesion();
    } catch (err) {
      setError(err.message);
    }
  }

  const IconPreview = menuIcon(form.icono);

  return (
    <div>
      <div className="mb-6 flex flex-col gap-4 border-b border-line pb-5 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-500">Administración</p>
          <h1 className="page-title mt-1">Menú</h1>
          <p className="mt-2 text-sm text-muted">Cada opción se asocia a uno o más perfiles. Al iniciar sesión solo se cargan las del perfil de la cuenta.</p>
        </div>
        <Button onClick={() => abrir(null)}><Plus size={16} /> Nueva opción</Button>
      </div>

      <Alert>{error}</Alert>
      <Alert ok>{ok}</Alert>

      <KpiRow>
        <Kpi value={counts.total} label="Opciones" hint="Ítems del menú" active={estado === ''} onClick={() => { setEstado(''); setPage(1); }} />
        <Kpi value={counts.activos} label="Activas" hint="Visibles para los perfiles asignados" active={estado === 'activos'} onClick={() => { setEstado('activos'); setPage(1); }} />
        <Kpi value={counts.inactivos} label="Inactivas" hint="Ocultas en el menú de sesión" active={estado === 'inactivos'} onClick={() => { setEstado('inactivos'); setPage(1); }} />
      </KpiRow>

      <FilterBar>
        <SearchField placeholder="Buscar código, etiqueta, ruta o perfil" value={q} onChange={(e) => { setQ(e.target.value); setPage(1); }} />
        <select className="w-auto" value={tab} onChange={(e) => { setTab(e.target.value); setPage(1); }}>
          <option value="">Todos los grupos</option>
          <option value="Operación">Operación ({counts.operacion})</option>
          <option value="Administración">Administración ({counts.administracion})</option>
          <option value="Control">Control ({counts.control})</option>
        </select>
      </FilterBar>

      {rows.length === 0 ? (
        <div className="rounded-xl border border-line bg-white">
          <Empty text="No hay opciones de menú en este filtro." />
        </div>
      ) : (
        <div className="space-y-3">
          {rows.map((r) => {
            const Icon = menuIcon(r.icono);
            return (
              <article key={r.idMenu} className="rounded-xl border border-line bg-white p-4">
                <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                  <div className="flex min-w-0 gap-3">
                    <span className="grid h-10 w-10 shrink-0 place-items-center rounded-lg bg-slate-100 text-navy">
                      <Icon size={16} />
                    </span>
                    <div className="min-w-0">
                      <p className="font-semibold text-navy">{r.etiqueta}</p>
                      <p className="text-xs text-muted">{r.codigo} · {r.ruta} · {r.grupo} · orden {r.orden}</p>
                      {r.descripcion && <p className="mt-2 text-sm text-slate-600">{r.descripcion}</p>}
                      <div className="mt-2 flex flex-wrap gap-1.5">
                        {(r.perfiles || []).length === 0 ? (
                          <span className="text-xs text-muted">Sin perfiles</span>
                        ) : (r.perfiles || []).map((p) => (
                          <span key={p} className="rounded-md bg-slate-100 px-2 py-0.5 text-xs font-medium text-navy">{p}</span>
                        ))}
                      </div>
                    </div>
                  </div>
                  <div className="flex shrink-0 flex-col items-start gap-2 sm:items-end">
                    <Badge value={r.activo ? 'ACTIVO' : 'INACTIVO'} />
                    <div className="flex flex-wrap gap-2">
                      <Button variant="secondary" onClick={() => abrir(r)}>Editar</Button>
                      <Button variant="danger" onClick={() => eliminar(r)}>Eliminar</Button>
                    </div>
                  </div>
                </div>
              </article>
            );
          })}
        </div>
      )}

      <div className="mt-3 overflow-hidden rounded-xl border border-line bg-white">
        <Pager page={meta.page} totalPages={meta.totalPages} totalElements={meta.totalElements} size={meta.size} onPage={setPage} />
      </div>

      {open && (
        <Modal title={editId ? 'Editar opción' : 'Nueva opción'} onClose={() => setOpen(false)}>
          <FormGrid onSubmit={guardar}>
            <Field label="Código">
              <input value={form.codigo} onChange={(e) => set('codigo', e.target.value.toUpperCase())} required />
            </Field>
            <Field label="Etiqueta">
              <input value={form.etiqueta} onChange={(e) => set('etiqueta', e.target.value)} required />
            </Field>
            <Field label="Ruta">
              <input value={form.ruta} onChange={(e) => set('ruta', e.target.value)} placeholder="/permisos" required />
            </Field>
            <Field label="Grupo">
              <select value={form.grupo} onChange={(e) => set('grupo', e.target.value)}>
                {GRUPOS.map((g) => <option key={g}>{g}</option>)}
              </select>
            </Field>
            <Field label="Icono">
              <div className="flex items-center gap-2">
                <span className="grid h-10 w-10 place-items-center rounded-md bg-slate-100 text-navy">
                  <IconPreview size={16} />
                </span>
                <select className="flex-1" value={form.icono} onChange={(e) => set('icono', e.target.value)}>
                  {MENU_ICON_OPTIONS.map((name) => <option key={name}>{name}</option>)}
                </select>
              </div>
            </Field>
            <Field label="Orden">
              <input type="number" value={form.orden} onChange={(e) => set('orden', e.target.value)} />
            </Field>
            <Field label="Descripción" full>
              <input value={form.descripcion} onChange={(e) => set('descripcion', e.target.value)} />
            </Field>
            <Field label="Estado">
              <select value={form.activo ? '1' : '0'} onChange={(e) => set('activo', e.target.value === '1')}>
                <option value="1">Activo</option>
                <option value="0">Inactivo</option>
              </select>
            </Field>
            <div className="md:col-span-2">
              <p className="mb-2 text-xs font-medium text-slate-500">Perfiles que verán esta opción</p>
              <div className="grid gap-2 sm:grid-cols-2">
                {perfiles.map((p) => (
                  <label key={p.id} className="flex items-center gap-2 rounded-lg border border-line px-3 py-2 text-sm">
                    <input
                      type="checkbox"
                      checked={form.idPerfiles.includes(p.id)}
                      onChange={() => togglePerfil(p.id)}
                    />
                    <span>{p.nombre || p.codigo}</span>
                  </label>
                ))}
              </div>
            </div>
            <div className="md:col-span-2">
              <Button type="submit" disabled={saving}>{saving ? 'Guardando…' : 'Guardar'}</Button>
            </div>
          </FormGrid>
        </Modal>
      )}
    </div>
  );
}
