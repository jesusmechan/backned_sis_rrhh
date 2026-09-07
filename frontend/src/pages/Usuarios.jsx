import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Eye, EyeOff, Plus } from 'lucide-react';
import { emptyPage, http, PAGE_SIZE, pagePath, SELECT_SIZE } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Alert, Avatar, Badge, Button, Empty, Field, FilterBar, FormGrid, Kpi, KpiRow, Modal, Pager, SearchField } from '../components/ui';
import { correoAndina, slugCuenta } from './altaShared';

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
  const location = useLocation();
  const navigate = useNavigate();
  const [rows, setRows] = useState([]);
  const [meta, setMeta] = useState(emptyPage);
  const [page, setPage] = useState(1);
  const [roles, setRoles] = useState([]);
  const [empleados, setEmpleados] = useState([]);
  const [ocupados, setOcupados] = useState([]);
  const [catalogosReady, setCatalogosReady] = useState(false);
  const [form, setForm] = useState(empty);
  const [editId, setEditId] = useState(null);
  const [open, setOpen] = useState(false);
  const [showPass, setShowPass] = useState(false);
  const [userTouched, setUserTouched] = useState(false);
  const [mailTouched, setMailTouched] = useState(false);
  const [desdeAlta, setDesdeAlta] = useState(false);
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
      http.page(pagePath('/api/empleados', { page: 1, size: SELECT_SIZE })),
      http.page(pagePath('/api/usuarios', { page: 1, size: SELECT_SIZE }))
    ])
      .then(([rolesData, emp, users]) => {
        setRoles(rolesData);
        setEmpleados(emp.content || []);
        setOcupados((users.content || []).map((u) => u.idEmpleado).filter(Boolean));
        setCatalogosReady(true);
      })
      .catch((e) => setError(e.message));
  }, []);

  async function refreshOcupados() {
    const users = await http.page(pagePath('/api/usuarios', { page: 1, size: SELECT_SIZE }));
    setOcupados((users.content || []).map((u) => u.idEmpleado).filter(Boolean));
  }

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

  function rolEmpleadoId(list = roles) {
    return list.find((r) => r.codigo === 'EMPLEADO')?.id || '';
  }

  function datosCuenta(emp, prefill = {}) {
    const nombres = prefill.nombres || emp?.nombres || '';
    const apellido = prefill.apellidoPaterno || emp?.apellidoPaterno || '';
    return {
      nombreUsuario: slugCuenta(nombres, apellido),
      correo: prefill.correo || emp?.correoInstitucional || correoAndina(nombres, apellido)
    };
  }

  function elegirColaborador(id) {
    const emp = empleados.find((e) => String(e.idEmpleado) === String(id));
    const auto = datosCuenta(emp);
    setForm((f) => ({
      ...f,
      idEmpleado: id,
      nombreUsuario: userTouched ? f.nombreUsuario : auto.nombreUsuario,
      correo: mailTouched ? f.correo : auto.correo
    }));
  }

  function abrir(row, prefill) {
    setError('');
    setShowPass(false);
    setDesdeAlta(Boolean(prefill?.nuevo || prefill?.idEmpleado));
    if (row) {
      setEditId(row.idUsuario);
      setUserTouched(true);
      setMailTouched(true);
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
      setUserTouched(false);
      setMailTouched(false);
      const idEmpleado = prefill?.idEmpleado || '';
      const emp = empleados.find((e) => String(e.idEmpleado) === String(idEmpleado));
      const auto = datosCuenta(emp, prefill || {});
      setForm({
        ...empty,
        idEmpleado,
        idRol: rolEmpleadoId(),
        nombreUsuario: auto.nombreUsuario,
        correo: auto.correo,
        password: 'Andina2026',
        activo: true
      });
    }
    setOpen(true);
  }

  useEffect(() => {
    if (!catalogosReady || !location.state?.nuevo || !esAdmin) return;
    abrir(null, location.state);
    navigate(location.pathname, { replace: true, state: {} });
  }, [catalogosReady, location.state, esAdmin]);

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
      setDesdeAlta(false);
      await Promise.all([load(), loadCounts(), refreshOcupados()]);
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

  const colaboradoresOpciones = empleados.filter((e) =>
    !ocupados.some((id) => String(id) === String(e.idEmpleado))
    || String(e.idEmpleado) === String(form.idEmpleado)
  );

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

      <KpiRow>
        <Kpi value={counts.total} label="Cuentas" hint="Usuarios registrados" active={tab === ''} onClick={() => { setTab(''); setPage(1); }} />
        <Kpi value={counts.activos} label="Activos" hint="Pueden iniciar sesión" active={tab === 'activos'} onClick={() => { setTab('activos'); setPage(1); }} />
        <Kpi value={counts.inactivos} label="Inactivos" hint="Acceso suspendido" active={tab === 'inactivos'} onClick={() => { setTab('inactivos'); setPage(1); }} />
      </KpiRow>

      <FilterBar>
        <SearchField placeholder="Buscar usuario, nombre, correo o perfil" value={q} onChange={(e) => { setQ(e.target.value); setPage(1); }} />
        <select className="w-auto" value={rol} onChange={(e) => { setRol(e.target.value); setPage(1); }}>
          <option value="">Todos los perfiles</option>
          {roles.map((r) => <option key={r.id} value={r.codigo}>{r.nombre || r.codigo}</option>)}
        </select>
      </FilterBar>

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
        <Modal title={editId ? 'Editar cuenta' : 'Nueva cuenta de acceso'} onClose={() => { setOpen(false); setDesdeAlta(false); }}>
          {desdeAlta && (
            <p className="mb-3 rounded-lg bg-slate-50 px-3 py-2 text-sm text-slate-600">
              El colaborador ya está registrado. Complete la cuenta para que pueda iniciar sesión.
            </p>
          )}
          <Alert>{error}</Alert>
          <FormGrid onSubmit={guardar}>
            <Field label="Colaborador" hint={editId ? 'La ficha no se cambia después de crear la cuenta' : 'Solo aparecen quienes aún no tienen usuario'} full>
              <select
                value={form.idEmpleado}
                onChange={(e) => elegirColaborador(e.target.value)}
                required
                disabled={Boolean(editId)}
              >
                <option value="">Seleccione</option>
                {colaboradoresOpciones.map((e) => (
                  <option key={e.idEmpleado} value={e.idEmpleado}>{e.nombreCompleto} · {e.codigoEmpleado}</option>
                ))}
              </select>
            </Field>
            {!editId && colaboradoresOpciones.length === 0 && (
              <p className="text-sm text-muted md:col-span-2">
                Todos los colaboradores ya tienen cuenta. Registre primero a la persona en Personal.
              </p>
            )}
            <Field label="Usuario" hint="Se sugiere nombre.apellido">
              <input
                value={form.nombreUsuario}
                onChange={(e) => { setUserTouched(true); set('nombreUsuario', e.target.value.toLowerCase().replace(/\s+/g, '')); }}
                required
                autoComplete="off"
              />
            </Field>
            <Field label="Perfil">
              <select value={form.idRol} onChange={(e) => set('idRol', e.target.value)} required>
                <option value="">Seleccione</option>
                {roles.map((r) => <option key={r.id} value={r.id}>{r.nombre || r.codigo}</option>)}
              </select>
            </Field>
            <Field label="Correo" hint="Usa el correo institucional del colaborador" full>
              <input
                type="email"
                value={form.correo}
                onChange={(e) => { setMailTouched(true); set('correo', e.target.value); }}
                required
                autoComplete="off"
              />
            </Field>
            <Field
              label="Contraseña"
              hint={editId ? 'Vacío = no cambia' : 'Contraseña inicial sugerida: Andina2026'}
              full
            >
              <div className="relative">
                <input
                  type={showPass ? 'text' : 'password'}
                  className="pr-11"
                  value={form.password}
                  onChange={(e) => set('password', e.target.value)}
                  placeholder={editId ? 'Dejar vacío para no cambiar' : ''}
                  required={!editId}
                  autoComplete={editId ? 'new-password' : 'off'}
                />
                <button
                  type="button"
                  className="absolute right-2 top-1/2 -translate-y-1/2 text-slate-500 hover:text-navy"
                  onClick={() => setShowPass((v) => !v)}
                  aria-label={showPass ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                >
                  {showPass ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
              </div>
            </Field>
            <Field label="Estado">
              <select value={form.activo ? '1' : '0'} onChange={(e) => set('activo', e.target.value === '1')}>
                <option value="1">Activo</option>
                <option value="0">Inactivo</option>
              </select>
            </Field>
            <div className="flex flex-wrap justify-end gap-2 md:col-span-2">
              <Button type="button" variant="secondary" onClick={() => { setOpen(false); setDesdeAlta(false); }}>Cancelar</Button>
              <Button type="submit" disabled={saving}>{saving ? 'Guardando…' : 'Guardar'}</Button>
            </div>
          </FormGrid>
        </Modal>
      )}
    </div>
  );
}
