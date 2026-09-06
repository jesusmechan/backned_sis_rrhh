import { useEffect, useState } from 'react';
import { emptyPage, http, PAGE_SIZE, pagePath, SELECT_SIZE } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Alert, Badge, Button, Empty, Field, FormGrid, Modal, PageHeader, Pager, Panel } from '../components/ui';

const empty = { idEmpleado: '', idRol: '', nombreUsuario: '', correo: '', password: 'Andina2026', activo: true };

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

  async function load() {
    const [usuarios, rolesData, emp] = await Promise.all([
      http.page(pagePath('/api/usuarios', { page, size: PAGE_SIZE })),
      http.get('/api/catalogos/roles'),
      http.page(pagePath('/api/empleados', { page: 1, size: SELECT_SIZE }))
    ]);
    setRows(usuarios.content || []);
    setMeta(usuarios);
    setRoles(rolesData);
    setEmpleados(emp.content || []);
  }
  useEffect(() => { load().catch((e) => setError(e.message)); }, [page]);
  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }));

  async function guardar(e) {
    e.preventDefault();
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
      setOpen(false);
      await load();
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <div>
      <PageHeader
        kicker="Accesos"
        title="Usuarios"
        subtitle="Cuentas y perfil de acceso. El menú se carga según el perfil asignado."
        actions={esAdmin && <Button onClick={() => { setEditId(null); setForm(empty); setOpen(true); }}>Nuevo</Button>}
      />
      <Alert>{error}</Alert>
      <Panel padded={false}>
        {rows.length === 0 ? <Empty text="Sin usuarios." /> : (
          <div className="overflow-x-auto">
            <table>
              <thead><tr><th>Usuario</th><th>Nombre</th><th>Perfil</th><th>Estado</th><th></th></tr></thead>
              <tbody>
                {rows.map((r) => (
                  <tr key={r.idUsuario}>
                    <td>{r.nombreUsuario}</td>
                    <td>{r.nombreCompleto}</td>
                    <td>{r.perfil || r.rol}</td>
                    <td><Badge value={r.activo ? 'ACTIVO' : 'INACTIVO'} /></td>
                    <td>
                      {esAdmin && (
                        <div className="flex flex-wrap gap-2">
                          <Button variant="secondary" onClick={() => { setEditId(r.idUsuario); setForm({ ...empty, ...r, password: '' }); setOpen(true); }}>Editar</Button>
                          <Button variant="secondary" onClick={() => http.patch(`/api/usuarios/${r.idUsuario}/estado?activo=${!r.activo}`).then(load)}>
                            {r.activo ? 'Desactivar' : 'Activar'}
                          </Button>
                        </div>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        <Pager page={meta.page} totalPages={meta.totalPages} totalElements={meta.totalElements} size={meta.size} onPage={setPage} />
      </Panel>
      {open && (
        <Modal title={editId ? 'Editar usuario' : 'Nuevo usuario'} onClose={() => setOpen(false)}>
          <FormGrid onSubmit={guardar}>
            <Field label="Empleado" full>
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
            <Field label="Correo" full><input type="email" value={form.correo} onChange={(e) => set('correo', e.target.value)} required /></Field>
            <Field label="Contraseña" full>
              <input type="password" value={form.password} onChange={(e) => set('password', e.target.value)} placeholder={editId ? 'Dejar vacío para no cambiar' : ''} required={!editId} />
            </Field>
            <div className="md:col-span-2"><Button type="submit">Guardar</Button></div>
          </FormGrid>
        </Modal>
      )}
    </div>
  );
}
