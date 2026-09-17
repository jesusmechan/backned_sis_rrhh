import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus, Download } from 'lucide-react';
import { emptyPage, http, PAGE_SIZE, pagePath } from '../api/client';
import { Alert, Badge, Button, Empty, Field, FilterBar, FormGrid, Kpi, KpiRow, Modal, Pager, downloadBlob } from '../components/ui';
import { fmtMoney } from '../lib/format';

const MESES = [
  ['1', 'Enero'], ['2', 'Febrero'], ['3', 'Marzo'], ['4', 'Abril'],
  ['5', 'Mayo'], ['6', 'Junio'], ['7', 'Julio'], ['8', 'Agosto'],
  ['9', 'Septiembre'], ['10', 'Octubre'], ['11', 'Noviembre'], ['12', 'Diciembre']
];

export function Planillas() {
  const navigate = useNavigate();
  const now = new Date();
  const [rows, setRows] = useState([]);
  const [meta, setMeta] = useState(emptyPage);
  const [page, setPage] = useState(1);
  const [tab, setTab] = useState('');
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');
  const [open, setOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [pdfId, setPdfId] = useState(null);
  const [form, setForm] = useState({ anio: String(now.getFullYear()), mes: String(now.getMonth() + 1), observaciones: '' });
  const [counts, setCounts] = useState({ total: 0, calculadas: 0, cerradas: 0 });

  async function load() {
    const data = await http.page(pagePath('/api/planillas', { page, size: PAGE_SIZE, estado: tab }));
    setRows(data.content || []);
    setMeta(data);
  }

  useEffect(() => {
    load().catch((e) => setError(e.message));
  }, [page, tab]);

  useEffect(() => {
    Promise.all([
      http.page(pagePath('/api/planillas', { page: 1, size: 1 })),
      http.page(pagePath('/api/planillas', { page: 1, size: 1, estado: 'CALCULADA' })),
      http.page(pagePath('/api/planillas', { page: 1, size: 1, estado: 'CERRADA' }))
    ]).then(([all, calc, cerr]) => {
      setCounts({
        total: all.totalElements || 0,
        calculadas: calc.totalElements || 0,
        cerradas: cerr.totalElements || 0
      });
    }).catch(() => {});
  }, []);

  async function bajarPdf(r) {
    setError('');
    setPdfId(r.idPlanilla);
    try {
      const blob = await http.download(`/api/planillas/${r.idPlanilla}/boletas/pdf`);
      downloadBlob(blob, `boletas-${r.anio}-${String(r.mes).padStart(2, '0')}.pdf`);
    } catch (err) {
      setError(err.message);
    } finally {
      setPdfId(null);
    }
  }

  async function crear(e) {
    e.preventDefault();
    setError('');
    setSaving(true);
    try {
      const saved = await http.post('/api/planillas', {
        anio: Number(form.anio),
        mes: Number(form.mes),
        observaciones: form.observaciones || null
      });
      setOpen(false);
      navigate(`/planillas/${saved.idPlanilla}`, { state: { ok: 'Periodo abierto. Calcule las boletas.' } });
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
          <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-500">Gestión</p>
          <h1 className="page-title mt-1">Planillas</h1>
          <p className="mt-2 text-sm text-muted">
            El cálculo toma la remuneración del contrato, las horas extras aprobadas y los permisos del mes. Al cerrar se genera el asiento contable.
          </p>
        </div>
        <Button onClick={() => { setError(''); setOpen(true); }}><Plus size={16} /> Abrir periodo</Button>
      </div>

      <Alert>{error}</Alert>
      <Alert ok>{ok}</Alert>

      <KpiRow>
        <Kpi value={counts.total} label="Periodos" hint="Planillas abiertas" active={tab === ''} onClick={() => { setTab(''); setPage(1); }} />
        <Kpi value={counts.calculadas} label="Calculadas" hint="Listas para cerrar" active={tab === 'CALCULADA'} onClick={() => { setTab('CALCULADA'); setPage(1); }} />
        <Kpi value={counts.cerradas} label="Cerradas" hint="Ya contabilizadas" active={tab === 'CERRADA'} onClick={() => { setTab('CERRADA'); setPage(1); }} />
      </KpiRow>

      <FilterBar>
        <select className="w-auto" value={tab} onChange={(e) => { setTab(e.target.value); setPage(1); }}>
          <option value="">Todos los estados</option>
          <option value="BORRADOR">Borrador</option>
          <option value="CALCULADA">Calculada</option>
          <option value="CERRADA">Cerrada</option>
        </select>
      </FilterBar>

      {rows.length === 0 ? (
        <div className="rounded-xl border border-line bg-white">
          <Empty text="No hay planillas en este filtro." />
        </div>
      ) : (
        <div className="space-y-3">
          {rows.map((r) => (
            <article key={r.idPlanilla} className="rounded-xl border border-line bg-white p-4">
              <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                <div>
                  <p className="font-semibold text-navy">{r.periodo}</p>
                  <p className="mt-1 text-sm text-slate-600">
                    Bruto {fmtMoney(r.totalBruto)} · Neto {fmtMoney(r.totalNeto)} · Aportes {fmtMoney(r.totalAportes)}
                  </p>
                </div>
                <div className="flex shrink-0 flex-col items-start gap-2 sm:items-end">
                  <Badge value={r.estado} />
                  <div className="flex flex-wrap gap-2">
                    {(r.estado === 'CALCULADA' || r.estado === 'CERRADA') && (
                      <Button variant="secondary" disabled={pdfId === r.idPlanilla} onClick={() => bajarPdf(r)}>
                        <Download size={14} /> {pdfId === r.idPlanilla ? 'Descargando…' : 'PDF'}
                      </Button>
                    )}
                    <Button variant="secondary" onClick={() => navigate(`/planillas/${r.idPlanilla}`)}>Ver boletas</Button>
                  </div>
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
        <Modal title="Abrir planilla" onClose={() => setOpen(false)}>
          <Alert>{error}</Alert>
          <FormGrid onSubmit={crear}>
            <Field label="Año">
              <input type="number" min="2020" max="2100" value={form.anio} onChange={(e) => setForm((f) => ({ ...f, anio: e.target.value }))} required />
            </Field>
            <Field label="Mes">
              <select value={form.mes} onChange={(e) => setForm((f) => ({ ...f, mes: e.target.value }))} required>
                {MESES.map(([v, n]) => <option key={v} value={v}>{n}</option>)}
              </select>
            </Field>
            <Field label="Observaciones" full>
              <textarea value={form.observaciones} onChange={(e) => setForm((f) => ({ ...f, observaciones: e.target.value }))} />
            </Field>
            <div className="flex justify-end gap-2 md:col-span-2">
              <Button type="button" variant="secondary" onClick={() => setOpen(false)}>Cancelar</Button>
              <Button type="submit" disabled={saving}>{saving ? 'Abriendo…' : 'Abrir'}</Button>
            </div>
          </FormGrid>
        </Modal>
      )}
    </div>
  );
}
