import { useEffect, useState } from 'react';
import { emptyPage, http, PAGE_SIZE, pagePath } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { MENU_ICON_OPTIONS, menuIcon } from '../layout/icons';
import { Alert, Badge, Button, Empty, Field, FormGrid, Modal, PageHeader, Pager, Panel, StackTable } from '../components/ui';

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

  async function load() {
    const [data, roles] = await Promise.all([
      http.page(pagePath('/api/menus', { page, size: PAGE_SIZE })),
      http.get('/api/catalogos/roles')
    ]);
    setRows(data.content || []);
    setMeta(data);
    setPerfiles(roles);
  }

  useEffect(() => { load().catch((e) => setError(e.message)); }, [page]);
  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }));

  function abrir(row) {
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
      await load();
      await refreshSesion();
    } catch (err) {
      setError(err.message);
    }
  }

  async function eliminar(row) {
    if (!window.confirm(`¿Eliminar la opción ${row.etiqueta}?`)) return;
    setError('');
    try {
      await http.delete(`/api/menus/${row.idMenu}`);
      setOk('Opción eliminada');
      await load();
      await refreshSesion();
    } catch (err) {
      setError(err.message);
    }
  }

  const IconPreview = menuIcon(form.icono);

  return (
    <div>
      <PageHeader
        kicker="Administración"
        title="Menú"
        subtitle="Cada opción se asocia a uno o más perfiles. Al iniciar sesión solo se cargan las del perfil de la cuenta."
        actions={<Button onClick={() => abrir(null)}>Nueva opción</Button>}
      />
      <Alert>{error}</Alert>
      <Alert ok>{ok}</Alert>
      <Panel padded={false}>
        {rows.length === 0 ? <Empty text="Sin opciones de menú." /> : (
          <StackTable
            cards={rows.map((r) => {
              const Icon = menuIcon(r.icono);
              return (
                <div key={r.idMenu} className="px-4 py-3">
                  <div className="flex items-start gap-3">
                    <span className="grid h-9 w-9 shrink-0 place-items-center rounded-md bg-slate-100 text-navy">
                      <Icon size={15} />
                    </span>
                    <div className="min-w-0 flex-1">
                      <p className="font-medium text-navy">{r.etiqueta}</p>
                      <p className="text-xs text-muted">{r.codigo} · {r.ruta} · {r.grupo}</p>
                      <p className="mt-1 text-xs text-muted">{(r.perfiles || []).join(', ') || 'Sin perfiles'}</p>
                      <div className="mt-2"><Badge value={r.activo ? 'ACTIVO' : 'INACTIVO'} /></div>
                    </div>
                  </div>
                  <div className="mt-3 flex flex-wrap gap-2">
                    <Button variant="secondary" className="px-3 py-2" onClick={() => abrir(r)}>Editar</Button>
                    <Button variant="secondary" className="px-3 py-2" onClick={() => eliminar(r)}>Eliminar</Button>
                  </div>
                </div>
              );
            })}
            table={(
              <table>
                <thead>
                  <tr>
                    <th>Orden</th>
                    <th>Opción</th>
                    <th>Ruta</th>
                    <th>Grupo</th>
                    <th>Perfiles</th>
                    <th>Estado</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((r) => {
                    const Icon = menuIcon(r.icono);
                    return (
                      <tr key={r.idMenu}>
                        <td>{r.orden}</td>
                        <td>
                          <div className="flex items-center gap-2">
                            <span className="grid h-8 w-8 place-items-center rounded-md bg-slate-100 text-navy">
                              <Icon size={15} />
                            </span>
                            <div>
                              <p className="font-medium text-navy">{r.etiqueta}</p>
                              <p className="text-xs text-muted">{r.codigo}</p>
                            </div>
                          </div>
                        </td>
                        <td>{r.ruta}</td>
                        <td>{r.grupo}</td>
                        <td>{(r.perfiles || []).join(', ') || '—'}</td>
                        <td><Badge value={r.activo ? 'ACTIVO' : 'INACTIVO'} /></td>
                        <td>
                          <div className="flex flex-wrap gap-2">
                            <Button variant="secondary" onClick={() => abrir(r)}>Editar</Button>
                            <Button variant="secondary" onClick={() => eliminar(r)}>Eliminar</Button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            )}
          />
        )}
        <Pager page={meta.page} totalPages={meta.totalPages} totalElements={meta.totalElements} size={meta.size} onPage={setPage} />
      </Panel>

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
                <option value="1">ACTIVO</option>
                <option value="0">INACTIVO</option>
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
            <div className="md:col-span-2"><Button type="submit">Guardar</Button></div>
          </FormGrid>
        </Modal>
      )}
    </div>
  );
}
