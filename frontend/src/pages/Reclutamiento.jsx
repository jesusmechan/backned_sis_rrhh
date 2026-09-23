import { useEffect, useState } from 'react';
import { Plus } from 'lucide-react';
import { emptyPage, http, PAGE_SIZE, pagePath } from '../api/client';
import { Alert, Badge, Button, DataList, DatePicker, Field, FilterBar, FormGrid, ListActions, MobileRow, Modal, Pager, PersonCell, SearchField } from '../components/ui';
import { useQuerySearch } from '../lib/useQuerySearch';
import { hoyISO } from './altaShared';
import { email, letters, phone, text } from '../lib/input';
import { fmtDate } from '../lib/format';

const emptyConv = { puesto: '', idArea: '', vacantes: '1', fechaInicio: hoyISO(), fechaFin: '', descripcion: '', estado: 'ABIERTA' };
const emptyPost = { nombres: '', apellidos: '', documento: '', correo: '', telefono: '', puntaje: '', estado: 'POSTULADO', observacion: '' };

export function Reclutamiento() {
  const [rows, setRows] = useState([]);
  const [meta, setMeta] = useState(emptyPage);
  const [page, setPage] = useState(1);
  const [areas, setAreas] = useState([]);
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState(emptyConv);
  const [saving, setSaving] = useState(false);
  const [sel, setSel] = useState(null);
  const [postulantes, setPostulantes] = useState([]);
  const [openPost, setOpenPost] = useState(false);
  const [post, setPost] = useState(emptyPost);
  const [q, setQ, qDebounced] = useQuerySearch();

  useEffect(() => { setPage(1); }, [qDebounced]);

  useEffect(() => {
    http.get('/api/catalogos/areas').then(setAreas).catch((e) => setError(e.message));
  }, []);

  async function load() {
    const data = await http.page(pagePath('/api/convocatorias', { page, size: PAGE_SIZE, q: qDebounced }));
    setRows(data.content || []);
    setMeta(data);
  }

  useEffect(() => {
    load().catch((e) => setError(e.message));
  }, [page, qDebounced]);

  async function abrir(row) {
    setSel(row);
    setError('');
    try {
      setPostulantes(await http.get(`/api/convocatorias/${row.idConvocatoria}/postulaciones`));
    } catch (e) {
      setError(e.message);
    }
  }

  async function guardar(e) {
    e.preventDefault();
    setError('');
    setSaving(true);
    try {
      await http.post('/api/convocatorias', {
        puesto: form.puesto.trim(),
        idArea: form.idArea ? Number(form.idArea) : null,
        vacantes: Number(form.vacantes) || 1,
        fechaInicio: form.fechaInicio,
        fechaFin: form.fechaFin || null,
        descripcion: form.descripcion || null,
        estado: form.estado
      });
      setOk('Convocatoria publicada');
      setOpen(false);
      setForm(emptyConv);
      await load();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  async function guardarPost(e) {
    e.preventDefault();
    if (!sel) return;
    setError('');
    setSaving(true);
    try {
      await http.post(`/api/convocatorias/${sel.idConvocatoria}/postulaciones`, {
        nombres: post.nombres.trim(),
        apellidos: post.apellidos.trim(),
        documento: post.documento.trim(),
        correo: post.correo || null,
        telefono: post.telefono || null,
        puntaje: post.puntaje ? Number(post.puntaje) : null,
        estado: post.estado,
        observacion: post.observacion || null
      });
      setOk('Postulante registrado');
      setOpenPost(false);
      setPost(emptyPost);
      setPostulantes(await http.get(`/api/convocatorias/${sel.idConvocatoria}/postulaciones`));
      await load();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  async function cambiarEstado(p, estado) {
    setError('');
    try {
      await http.put(`/api/convocatorias/postulaciones/${p.idPostulacion}`, { ...p, estado });
      setPostulantes(await http.get(`/api/convocatorias/${sel.idConvocatoria}/postulaciones`));
    } catch (e) {
      setError(e.message);
    }
  }

  return (
    <div>
      <div className="mb-6 flex flex-col gap-4 border-b border-line pb-5 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-500">Gestión</p>
          <h1 className="page-title mt-1">Reclutamiento</h1>
          <p className="mt-2 text-sm text-muted">
            Publique convocatorias y siga a los postulantes hasta la contratación o el descarte.
          </p>
        </div>
        <Button onClick={() => { setError(''); setForm({ ...emptyConv, fechaInicio: hoyISO() }); setOpen(true); }}>
          <Plus size={16} /> Nueva convocatoria
        </Button>
      </div>

      <Alert>{error}</Alert>
      <Alert ok>{ok}</Alert>

      <FilterBar>
        <SearchField placeholder="Buscar puesto, código o área" value={q} onChange={(e) => { setQ(e.target.value); setPage(1); }} />
      </FilterBar>

      <DataList
        empty={rows.length === 0}
        emptyText="No hay convocatorias."
        cards={rows.map((r) => (
          <MobileRow
            key={r.idConvocatoria}
            title={r.puesto}
            meta={`${r.codigo}${r.area ? ` · ${r.area}` : ''} · ${r.postulantes} postulantes`}
            badge={<Badge value={r.estado} />}
            actions={<Button variant="secondary" className="px-3 py-1.5 text-xs" onClick={() => abrir(r)}>Postulantes</Button>}
          />
        ))}
        table={(
          <table>
            <thead>
              <tr>
                <th>Puesto</th>
                <th>Área</th>
                <th>Vigencia</th>
                <th>Vacantes</th>
                <th>Postulantes</th>
                <th>Estado</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {rows.map((r) => (
                <tr key={r.idConvocatoria}>
                  <td>
                    <p className="font-medium text-navy">{r.puesto}</p>
                    <p className="text-xs text-muted">{r.codigo}</p>
                  </td>
                  <td className="text-sm">{r.area || '—'}</td>
                  <td className="text-sm">{fmtDate(r.fechaInicio)} → {fmtDate(r.fechaFin, 'Abierta')}</td>
                  <td className="text-sm">{r.vacantes}</td>
                  <td className="text-sm">{r.postulantes}</td>
                  <td><Badge value={r.estado} /></td>
                  <td>
                    <ListActions>
                      <Button variant="secondary" className="px-3 py-1.5 text-xs" onClick={() => abrir(r)}>Postulantes</Button>
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

      {sel && (
        <div className="mt-6">
          <div className="mb-3 flex flex-wrap items-center justify-between gap-3">
            <div>
              <p className="font-semibold text-navy">{sel.puesto}</p>
              <p className="text-xs text-muted">Seguimiento de postulantes</p>
            </div>
            {sel.estado === 'ABIERTA' && (
              <Button onClick={() => { setPost(emptyPost); setOpenPost(true); }}><Plus size={16} /> Postulante</Button>
            )}
          </div>
          <DataList
            empty={postulantes.length === 0}
            emptyText="Sin postulantes registrados."
            cards={postulantes.map((p) => (
              <MobileRow
                key={p.idPostulacion}
                title={p.nombreCompleto}
                meta={`${p.documento}${p.correo ? ` · ${p.correo}` : ''}${p.puntaje != null ? ` · ${p.puntaje} pts` : ''}`}
                badge={<Badge value={p.estado} />}
                actions={(
                  <select className="w-auto" value={p.estado} onChange={(e) => cambiarEstado(p, e.target.value)}>
                    <option value="POSTULADO">Postulado</option>
                    <option value="ENTREVISTA">Entrevista</option>
                    <option value="SELECCIONADO">Seleccionado</option>
                    <option value="CONTRATADO">Contratado</option>
                    <option value="DESCARTADO">Descartado</option>
                  </select>
                )}
              />
            ))}
            table={(
              <table>
                <thead>
                  <tr>
                    <th>Postulante</th>
                    <th>Contacto</th>
                    <th>Puntaje</th>
                    <th>Estado</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {postulantes.map((p) => (
                    <tr key={p.idPostulacion}>
                      <td>
                        <PersonCell name={p.nombreCompleto} meta={p.documento} showAvatar={false} />
                      </td>
                      <td className="text-sm text-muted">{p.correo || '—'}</td>
                      <td className="text-sm">{p.puntaje != null ? `${p.puntaje} pts` : '—'}</td>
                      <td><Badge value={p.estado} /></td>
                      <td>
                        <select className="w-auto" value={p.estado} onChange={(e) => cambiarEstado(p, e.target.value)}>
                          <option value="POSTULADO">Postulado</option>
                          <option value="ENTREVISTA">Entrevista</option>
                          <option value="SELECCIONADO">Seleccionado</option>
                          <option value="CONTRATADO">Contratado</option>
                          <option value="DESCARTADO">Descartado</option>
                        </select>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          />
        </div>
      )}

      {open && (
        <Modal title="Nueva convocatoria" onClose={() => setOpen(false)}>
          <Alert>{error}</Alert>
          <FormGrid onSubmit={guardar}>
            <Field label="Puesto" full>
              <input value={form.puesto} onChange={(e) => setForm((f) => ({ ...f, puesto: text(e.target.value, 120) }))} required />
            </Field>
            <Field label="Área">
              <select value={form.idArea} onChange={(e) => setForm((f) => ({ ...f, idArea: e.target.value }))}>
                <option value="">Sin área</option>
                {areas.map((a) => <option key={a.id} value={a.id}>{a.nombre}</option>)}
              </select>
            </Field>
            <Field label="Vacantes">
              <input type="number" min="1" value={form.vacantes} onChange={(e) => setForm((f) => ({ ...f, vacantes: e.target.value }))} required />
            </Field>
            <Field label="Inicio">
              <DatePicker value={form.fechaInicio} onChange={(v) => setForm((f) => ({ ...f, fechaInicio: v }))} required />
            </Field>
            <Field label="Fin">
              <DatePicker value={form.fechaFin} onChange={(v) => setForm((f) => ({ ...f, fechaFin: v }))} min={form.fechaInicio || undefined} />
            </Field>
            <Field label="Descripción" full>
              <textarea value={form.descripcion} onChange={(e) => setForm((f) => ({ ...f, descripcion: text(e.target.value, 400) }))} />
            </Field>
            <div className="flex justify-end gap-2 md:col-span-2">
              <Button type="button" variant="secondary" onClick={() => setOpen(false)}>Cancelar</Button>
              <Button type="submit" disabled={saving}>{saving ? 'Publicando…' : 'Publicar'}</Button>
            </div>
          </FormGrid>
        </Modal>
      )}

      {openPost && (
        <Modal title="Registrar postulante" onClose={() => setOpenPost(false)}>
          <Alert>{error}</Alert>
          <FormGrid onSubmit={guardarPost}>
            <Field label="Nombres">
              <input value={post.nombres} onChange={(e) => setPost((f) => ({ ...f, nombres: letters(e.target.value) }))} required />
            </Field>
            <Field label="Apellidos">
              <input value={post.apellidos} onChange={(e) => setPost((f) => ({ ...f, apellidos: letters(e.target.value) }))} required />
            </Field>
            <Field label="Documento">
              <input value={post.documento} onChange={(e) => setPost((f) => ({ ...f, documento: text(e.target.value, 20) }))} required />
            </Field>
            <Field label="Correo">
              <input value={post.correo} onChange={(e) => setPost((f) => ({ ...f, correo: email(e.target.value) }))} />
            </Field>
            <Field label="Teléfono">
              <input value={post.telefono} onChange={(e) => setPost((f) => ({ ...f, telefono: phone(e.target.value) }))} />
            </Field>
            <Field label="Puntaje">
              <input type="number" min="0" max="100" value={post.puntaje} onChange={(e) => setPost((f) => ({ ...f, puntaje: e.target.value }))} />
            </Field>
            <Field label="Observación" full>
              <textarea value={post.observacion} onChange={(e) => setPost((f) => ({ ...f, observacion: text(e.target.value, 400) }))} />
            </Field>
            <div className="flex justify-end gap-2 md:col-span-2">
              <Button type="button" variant="secondary" onClick={() => setOpenPost(false)}>Cancelar</Button>
              <Button type="submit" disabled={saving}>{saving ? 'Guardando…' : 'Registrar'}</Button>
            </div>
          </FormGrid>
        </Modal>
      )}
    </div>
  );
}
