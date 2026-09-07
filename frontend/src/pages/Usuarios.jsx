import { useEffect, useState } from 'react';
import { Plus, Search } from 'lucide-react';
import { emptyPage, http, PAGE_SIZE, pagePath, SELECT_SIZE } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Alert, Avatar, Badge, Button, Empty, Field, FormGrid, Kpi, Modal, Pager } from '../components/ui';

const empty = { idEmpleado: '', idRol: '', nombreUsuario: '', correo: '', password: 'Andina2026', activo: true };

function fmtAccess(value) {
  if (!value) return 'Sin acceso registrado';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return 'Sin acceso registrado';
  return `Último acceso ${d.toLocaleString('es-PE', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' })}`;
}

export function Usuarios() {
  const { hasAnyRole } = useAuth();
  const esAdmin = hasAnyRole('ADMIN');
  const [rows, setRows] = useState([]);
  const [meta, setMeta] = useState(emptyPage);
  const [page, setPage] = useState(1);
  const [roles, setRoles] = useState([]);
  const [empleados, setEmpleados] = useState([]);
  const [form, setForm] = useState(empty);
  const [editId, setEditId] = useState(null);
  const [open, setOpen] = useState(false);
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');
  const [saving, setSaving] = useState(false);
  const [tab, setTab] = useState('');
  const [rol, setRol] = useState('');
  const [q, setQ] = useState('');
  const [qDebounced, setQDebounced] = useState('');
  const [counts, setCounts] = useState({ total: 0, activos: 0, inactivos: 0 });

  useEffect(() => {
    const t = setTimeout(() => setQDebounced(q.trim()), 300);
    return () => clearTimeout(t);
  }, [q]);

  useEffect(() => {
    Promise.all([
      http.get('/api/catalogos/roles'),
      http.page(pagePath('/api/empleados', { page: 1, size: SELECT_SIZE }))
    ])
      .then(([rolesData, emp]) => {
        setRoles(rolesData);
        setEmpleados(emp.content || []);
      })
      .catch((e) => setError(e.message));
  }, []);

  async function loadCounts() {
    const [all, act, ina] = await Promise.all([
      http.page(pagePath('/api/usuarios', { page: 1, size: 1 })),
      http.page(pagePath('/api/usuarios', { page: 1, size: 1, activo: true })),
      http.page(pagePath('/api/usuarios', { page: 1, size: 1, activo: false }))
    ]);
    setCounts({
      total: all.totalElements || 0,
      activos: act.totalElements || 0,
      inactivos: ina.totalElements || 0
    });
  }

  async function load() {
    const data = await http.page(pagePath('/api/usuarios', {
      page,
      size: PAGE_SIZE,
      q: qDebounced,
      rol,
      activo: tab === '' ? undefined : tab === 'activos'
    }));
    setRows(data.content || []);
    setMeta(data);
  }

  useEffect(() => {
    load().catch((e) => setError(e.message));
  }, [page, qDebounced, tab, rol]);

  useEffect(() => { loadCounts().catch(() => {}); }, []);

  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }));

  function abrir(row) {
    setError('');
    if (row) {
      setEditId(row.idUsuario);
      setForm({
        idEmpleado: row.idEmpleado || '',
        idRol: row.idRol || '',
        nombreUsuario: row.nombreUsuario || '',
        correo: row.correo || '',
        password: '',
        activo: row.activo !== false
      });
    } else {
      setEditId(null);
      setForm(empty);
    }
    setOpen(true);
  }

  async function guardar(e) {
    e.preventDefault();
    setError('');
    setSaving(true);
    const body = {
      idEmpleado: form.idEmpleado ? Number(form.idEmpleado) : null,
      idRol: Number(form.idRol),
      nombreUsuario: form.nombreUsuario,
      correo: form.correo,
      activo: form.activo !== false
    };
    if (form.password) body.password = form.password;
    try {
      if (editId) await http.put(`/api/usuarios/${editId}`, body);
      else await http.post('/api/usuarios', body);
      setOk(editId ? 'Usuario actualizado' : 'Usuario creado');
      setOpen(false);
      await Promise.all([load(), loadCounts()]);
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  async function cambiarEstado(row) {
    const next = !row.activo;
    if (!next && !window.confirm(`¿Desactivar la cuenta ${row.nombreUsuario}?`)) return;
    setError('');
    try {
      await http.patch(`/api/usuarios/${row.idUsuario}/estado?activo=${next}`);
      setOk(next ? 'Usuario activado' : 'Usuario desactivado');
      await Promise.all([load(), loadCounts()]);
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <div>
      <div className="mb-6 flex flex-col gap-4 border-b border-line pb-5 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-500">Administración</p>
          <h1 className="page-title mt-1">Usuarios</h1>
          <p className="mt-2 text-sm text-muted">Cuentas de acceso. El menú se carga según el perfil asignado.</p>
        </div>
        {esAdmin && <Button onClick={() => abrir(null)}><Plus size={16} /> Nueva cuenta</Button>}
      </div>

      <Alert>{error}</Alert>
      <Alert ok>{ok}</Alert>

      <div className="mb-5 grid gap-3 sm:grid-cols-3">
        <Kpi value={counts.total} label="Cuentas" hint="Usuarios registrados" />
        <Kpi value={counts.activos} label="Activos" hint="Pueden iniciar sesión" />
        <Kpi value={counts.inactivos} label="Inactivos" hint="Acceso suspendido" />
      </div>

      <div className="mb-3 flex flex-col gap-3 sm:flex-row sm:items-center">
        <div className="flex flex-wrap gap-2">
          {[
            ['', 'Todos', counts.total],
            ['activos', 'Activos', counts.activos],
            ['inactivos', 'Inactivos', counts.inactivos]
          ].map(([id, label, n]) => (
            <button
              key={id || 'todos'}
              type="button"
              onClick={() => { setTab(id); setPage(1); }}
              className={`rounded-lg px-3 py-1.5 text-sm ${tab === id ? 'bg-navy text-white' : 'border border-line bg-white text-slate-600 hover:bg-slate-50'}`}
            >
              {label} ({n})
            </button>
          ))}
        </div>
        <div className="flex min-w-0 flex-1 flex-wrap gap-2">
          <div className="relative min-w-0 flex-1">
            <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
            <input
              className="pl-9"
              placeholder="Buscar usuario, nombre, correo o perfil"
              value={q}
              onChange={(e) => { setQ(e.target.value); setPage(1); }}
            />
          </div>
          <select value={rol} onChange={(e) => { setRol(e.target.value); setPage(1); }}>
            <option value="">Todos los perfiles</option>
            {roles.map((r) => <option key={r.id} value={r.codigo}>{r.nombre || r.codigo}</option>)}
          </select>
        </div>
      </div>

      {rows.length === 0 ? (
        <div className="rounded-xl border border-line bg-white">
          <Empty text="No hay cuentas en este filtro." />
        </div>
      ) : (
        <div className="space-y-3">
          {rows.map((r) => (
            <article key={r.idUsuario} className="rounded-xl border border-line bg-white p-4">
              <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                <div className="flex min-w-0 gap-3">
                  <Avatar name={r.nombreCompleto || r.nombreUsuario} />
                  <div className="min-w-0">
                    <p className="font-semibold text-navy">{r.nombreCompleto || r.nombreUsuario}</p>
                    <p className="text-xs text-muted">{r.nombreUsuario} · {r.correo}</p>
                    <p className="mt-2 text-sm text-slate-600">{r.perfil || r.rol}</p>
                    <p className="mt-1 text-xs text-muted">{fmtAccess(r.ultimoAcceso)}</p>
                  </div>
                </div>
                <div className="flex shrink-0 flex-col items-start gap-2 sm:items-end">
                  <Badge value={r.activo ? 'ACTIVO' : 'INACTIVO'} />
                  {esAdmin && (
                    <div className="flex flex-wrap gap-2">
                      <Button variant="secondary" onClick={() => abrir(r)}>Editar</Button>
                      <Button variant={r.activo ? 'danger' : 'secondary'} onClick={() => cambiarEstado(r)}>
                        {r.activo ? 'Desactivar' : 'Activar'}
                      </Button>
                    </div>
                  )}
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
        <Modal title={editId ? 'Editar usuario' : 'Nueva cuenta'} onClose={() => setOpen(false)}>
          <FormGrid onSubmit={guardar}>
            <Field label="Colaborador" full>
              <select value={form.idEmpleado} onChange={(e) => set('idEmpleado', e.target.value)} required>
                <option value="">Seleccione</option>
                {empleados.map((e) => <option key={e.idEmpleado} value={e.idEmpleado}>{e.nombreCompleto}</option>)}
              </select>
            </Field>
            <Field label="Usuario"><input value={form.nombreUsuario} onChange={(e) => set('nombreUsuario', e.target.value)} required /></Field>
            <Field label="Perfil">
              <select value={form.idRol} onChange={(e) => set('idRol', e.target.value)} required>
                <option value="">Seleccione</option>
                {roles.map((r) => <option key={r.id} value={r.id}>{r.nombre || r.codigo}</option>)}
              </select>
            </Field>
            <Field label="Correo" full>
              <input type="email" value={form.correo} onChange={(e) => set('correo', e.target.value)} required />
            </Field>
            <Field label="Contraseña" full>
              <input
                type="password"
                value={form.password}
                onChange={(e) => set('password', e.target.value)}
                placeholder={editId ? 'Dejar vacío para no cambiar' : ''}
                required={!editId}
              />
            </Field>
            <Field label="Estado">
              <select value={form.activo ? '1' : '0'} onChange={(e) => set('activo', e.target.value === '1')}>
                <option value="1">Activo</option>
                <option value="0">Inactivo</option>
              </select>
            </Field>
            <div className="md:col-span-2">
              <Button type="submit" disabled={saving}>{saving ? 'Guardando…' : 'Guardar'}</Button>
            </div>
          </FormGrid>
        </Modal>
      )}
    </div>
  );
}
