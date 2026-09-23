import { useEffect, useMemo, useState } from 'react';
import { Plus } from 'lucide-react';
import { emptyPage, http, PAGE_SIZE, pagePath, SELECT_SIZE } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Alert, Badge, Button, DataList, Field, FilterBar, FormGrid, Kpi, KpiRow, ListActions, MobileRow, Modal, Pager, SearchField } from '../components/ui';
import { useQuerySearch } from '../lib/useQuerySearch';
import { code, label, text } from '../lib/input';

const empty = {
  codigo: '',
  nombre: '',
  descripcion: '',
  activo: true,
  idMenus: [],
  idPermisos: []
};

function toggleId(list, id) {
  return list.includes(id) ? list.filter((x) => x !== id) : [...list, id];
}

export function Roles() {
  const { refreshSesion } = useAuth();
  const [rows, setRows] = useState([]);
  const [meta, setMeta] = useState(emptyPage);
  const [page, setPage] = useState(1);
  const [menus, setMenus] = useState([]);
  const [permisos, setPermisos] = useState([]);
  const [form, setForm] = useState(empty);
  const [editId, setEditId] = useState(null);
  const [editCodigo, setEditCodigo] = useState('');
  const [open, setOpen] = useState(false);
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');
  const [saving, setSaving] = useState(false);
  const [estado, setEstado] = useState('');
  const [q, setQ, qDebounced] = useQuerySearch();
  const [counts, setCounts] = useState({ total: 0, activos: 0, inactivos: 0 });

  useEffect(() => { setPage(1); }, [qDebounced]);

  useEffect(() => {
    Promise.all([
      http.page(pagePath('/api/menus', { page: 1, size: SELECT_SIZE, activo: true })),
      http.get('/api/catalogos/permisos-funcionales')
    ])
      .then(([menuData, permData]) => {
        setMenus(menuData.content || []);
        setPermisos(Array.isArray(permData) ? permData : []);
      })
      .catch((e) => setError(e.message));
  }, []);

  async function loadCounts() {
    const [all, act, ina] = await Promise.all([
      http.page(pagePath('/api/roles', { page: 1, size: 1 })),
      http.page(pagePath('/api/roles', { page: 1, size: 1, activo: true })),
      http.page(pagePath('/api/roles', { page: 1, size: 1, activo: false }))
    ]);
    setCounts({
      total: all.totalElements || 0,
      activos: act.totalElements || 0,
      inactivos: ina.totalElements || 0
    });
  }

  async function load() {
    const data = await http.page(pagePath('/api/roles', {
      page,
      size: PAGE_SIZE,
      q: qDebounced,
      activo: estado === '' ? undefined : estado === 'activos'
    }));
    setRows(data.content || []);
    setMeta(data);
  }

  useEffect(() => {
    load().catch((e) => setError(e.message));
  }, [page, qDebounced, estado]);

  useEffect(() => { loadCounts().catch(() => {}); }, []);

  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }));
  const esAdmin = editCodigo === 'ADMIN';

  const menusPorGrupo = useMemo(() => {
    const grupos = {};
    menus.forEach((m) => {
      const g = m.grupo || 'Otros';
      (grupos[g] ||= []).push(m);
    });
    return Object.entries(grupos);
  }, [menus]);

  const permisosPorModulo = useMemo(() => {
    const grupos = {};
    permisos.forEach((p) => {
      const g = p.modulo || 'GENERAL';
      (grupos[g] ||= []).push(p);
    });
    return Object.entries(grupos);
  }, [permisos]);

  function abrir(row) {
    setError('');
    if (row) {
      setEditId(row.idRol);
      setEditCodigo(row.codigo || '');
      setForm({
        codigo: row.codigo || '',
        nombre: row.nombre || '',
        descripcion: row.descripcion || '',
        activo: row.activo !== false,
        idMenus: row.idMenus || [],
        idPermisos: row.idPermisos || []
      });
    } else {
      setEditId(null);
      setEditCodigo('');
      setForm(empty);
    }
    setOpen(true);
  }

  async function guardar(e) {
    e.preventDefault();
    setError('');
    setOk('');
    setSaving(true);
    const body = {
      codigo: form.codigo,
      nombre: form.nombre.trim(),
      descripcion: form.descripcion || null,
      activo: form.activo !== false,
      idMenus: form.idMenus,
      idPermisos: form.idPermisos
    };
    try {
      if (editId) await http.put(`/api/roles/${editId}`, body);
      else await http.post('/api/roles', body);
      setOpen(false);
      setOk('Rol guardado');
      await Promise.all([load(), loadCounts()]);
      await refreshSesion();
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
          <h1 className="page-title mt-1">Roles</h1>
          <p className="mt-2 text-sm text-muted">
            Los perfiles se guardan en la tabla <span className="font-medium text-navy">rol</span>.
            Cada uno define el menú y los permisos funcionales de las cuentas.
          </p>
        </div>
        <Button onClick={() => abrir(null)}><Plus size={16} /> Nuevo rol</Button>
      </div>

      <Alert>{error}</Alert>
      <Alert ok>{ok}</Alert>

      <KpiRow>
        <Kpi value={counts.total} label="Roles" hint="Perfiles en la tabla" active={estado === ''} onClick={() => { setEstado(''); setPage(1); }} />
        <Kpi value={counts.activos} label="Activos" hint="Disponibles al crear cuentas" active={estado === 'activos'} onClick={() => { setEstado('activos'); setPage(1); }} />
        <Kpi value={counts.inactivos} label="Inactivos" hint="Ocultos en los combos" active={estado === 'inactivos'} onClick={() => { setEstado('inactivos'); setPage(1); }} />
      </KpiRow>

      <FilterBar>
        <SearchField placeholder="Buscar código, nombre o menú" value={q} onChange={(e) => { setQ(e.target.value); setPage(1); }} />
      </FilterBar>

      <DataList
        empty={rows.length === 0}
        emptyText="No hay roles en este filtro."
        cards={rows.map((r) => (
          <MobileRow
            key={r.idRol}
            title={r.nombre}
            meta={`${r.codigo} · ${r.usuarios} ${r.usuarios === 1 ? 'cuenta' : 'cuentas'}`}
            badge={<Badge value={r.activo ? 'ACTIVO' : 'INACTIVO'} />}
            actions={<Button variant="secondary" className="px-3 py-1.5 text-xs" onClick={() => abrir(r)}>Editar</Button>}
          />
        ))}
        table={(
          <table>
            <thead>
              <tr>
                <th>Rol</th>
                <th>Cuentas</th>
                <th>Menú</th>
                <th>Estado</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {rows.map((r) => (
                <tr key={r.idRol}>
                  <td>
                    <p className="font-medium text-navy">{r.nombre}</p>
                    <p className="text-xs text-muted">{r.codigo}{r.descripcion ? ` · ${r.descripcion}` : ''}</p>
                  </td>
                  <td className="text-sm">{r.usuarios}</td>
                  <td>
                    <div className="flex max-w-md flex-wrap gap-1">
                      {(r.menus || []).length === 0 ? (
                        <span className="text-xs text-muted">Sin opciones</span>
                      ) : (r.menus || []).slice(0, 6).map((m) => (
                        <span key={m} className="rounded-md bg-slate-100 px-2 py-0.5 text-xs font-medium text-navy">{m}</span>
                      ))}
                      {(r.menus || []).length > 6 && (
                        <span className="text-xs text-muted">+{(r.menus || []).length - 6}</span>
                      )}
                    </div>
                  </td>
                  <td><Badge value={r.activo ? 'ACTIVO' : 'INACTIVO'} /></td>
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
        <Modal title={editId ? 'Editar rol' : 'Nuevo rol'} onClose={() => setOpen(false)}>
          <Alert>{error}</Alert>
          <FormGrid onSubmit={guardar}>
            <Field label="Código" hint={esAdmin ? 'El código Administrador no se cambia' : 'Mayúsculas, números y guion'}>
              <input value={form.codigo} onChange={(e) => set('codigo', code(e.target.value, 30))} required disabled={esAdmin} />
            </Field>
            <Field label="Nombre">
              <input value={form.nombre} onChange={(e) => set('nombre', label(e.target.value))} required />
            </Field>
            <Field label="Descripción" full>
              <input value={form.descripcion} onChange={(e) => set('descripcion', text(e.target.value, 250))} />
            </Field>
            <Field label="Estado" hint={esAdmin ? 'El Administrador permanece activo' : 'Inactivo se oculta al asignar cuentas'}>
              <select
                value={form.activo ? '1' : '0'}
                onChange={(e) => set('activo', e.target.value === '1')}
                disabled={esAdmin}
              >
                <option value="1">Activo</option>
                <option value="0">Inactivo</option>
              </select>
            </Field>
            <div className="md:col-span-2">
              <p className="mb-2 text-xs font-medium text-slate-500">Menú que verá este rol</p>
              <div className="max-h-56 space-y-3 overflow-y-auto rounded-lg border border-line p-3">
                {menusPorGrupo.map(([grupo, items]) => (
                  <div key={grupo}>
                    <p className="mb-1.5 text-[11px] font-semibold uppercase tracking-wide text-slate-400">{grupo}</p>
                    <div className="grid gap-1.5 sm:grid-cols-2">
                      {items.map((m) => (
                        <label key={m.idMenu} className="flex items-center gap-2 text-sm">
                          <input
                            type="checkbox"
                            checked={form.idMenus.includes(m.idMenu)}
                            onChange={() => setForm((f) => ({ ...f, idMenus: toggleId(f.idMenus, m.idMenu) }))}
                          />
                          <span>{m.etiqueta}</span>
                        </label>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            </div>
            <div className="md:col-span-2">
              <p className="mb-2 text-xs font-medium text-slate-500">Permisos funcionales</p>
              <div className="max-h-56 space-y-3 overflow-y-auto rounded-lg border border-line p-3">
                {permisosPorModulo.map(([modulo, items]) => (
                  <div key={modulo}>
                    <p className="mb-1.5 text-[11px] font-semibold uppercase tracking-wide text-slate-400">{modulo.replaceAll('_', ' ')}</p>
                    <div className="grid gap-1.5 sm:grid-cols-2">
                      {items.map((p) => (
                        <label key={p.id} className="flex items-center gap-2 text-sm">
                          <input
                            type="checkbox"
                            checked={form.idPermisos.includes(p.id)}
                            onChange={() => setForm((f) => ({ ...f, idPermisos: toggleId(f.idPermisos, p.id) }))}
                          />
                          <span>{p.nombre}</span>
                        </label>
                      ))}
                    </div>
                  </div>
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
