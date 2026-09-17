import { useEffect, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { Download } from 'lucide-react';
import { http } from '../api/client';
import { Alert, BackLink, Badge, Button, Empty, downloadBlob } from '../components/ui';
import { fmtMoney } from '../lib/format';

function archivoPeriodo(detalle, prefix, extra = '') {
  const mes = String(detalle?.mes || 0).padStart(2, '0');
  return `${prefix}${extra}${detalle?.anio || ''}-${mes}.pdf`;
}

function slugNombre(nombre) {
  const slug = String(nombre || 'boleta')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-zA-Z0-9]+/g, '-')
    .replace(/^-|-$/g, '')
    .slice(0, 40);
  return slug || 'boleta';
}

export function PlanillaDetalle() {
  const { id } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const [detalle, setDetalle] = useState(null);
  const [error, setError] = useState('');
  const [ok, setOk] = useState(location.state?.ok || '');
  const [saving, setSaving] = useState(false);
  const [pdf, setPdf] = useState('');

  async function load() {
    setDetalle(await http.get(`/api/planillas/${id}`));
  }

  useEffect(() => {
    load().catch((e) => setError(e.message));
  }, [id]);

  async function calcular() {
    setError('');
    setSaving(true);
    try {
      const data = await http.post(`/api/planillas/${id}/calcular`);
      setDetalle(data);
      setOk('Boletas calculadas con la remuneración, horas extras y permisos del mes.');
    } catch (e) {
      setError(e.message);
    } finally {
      setSaving(false);
    }
  }

  async function cerrar() {
    if (!window.confirm('¿Cerrar la planilla y generar el asiento contable? Esta acción no se revierte.')) return;
    setError('');
    setSaving(true);
    try {
      const data = await http.post(`/api/planillas/${id}/cerrar`);
      setDetalle(data);
      setOk('Planilla cerrada. El asiento quedó en Contabilidad.');
    } catch (e) {
      setError(e.message);
    } finally {
      setSaving(false);
    }
  }

  async function bajar(path, filename, token) {
    setError('');
    setOk('');
    setPdf(token);
    try {
      downloadBlob(await http.download(path), filename);
    } catch (e) {
      setError(e.message);
    } finally {
      setPdf('');
    }
  }

  const boletas = detalle?.boletas || [];
  const puedePdf = boletas.length > 0 && (detalle?.estado === 'CALCULADA' || detalle?.estado === 'CERRADA');

  return (
    <div>
      <BackLink to="/planillas">Planillas</BackLink>
      <div className="mb-6 flex flex-col gap-4 border-b border-line pb-5 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-500">Gestión</p>
          <h1 className="page-title mt-1">{detalle?.periodo || 'Planilla'}</h1>
          <p className="mt-2 text-sm text-muted">
            Bruto {fmtMoney(detalle?.totalBruto)} · Descuentos {fmtMoney(detalle?.totalDescuentos)} · Neto {fmtMoney(detalle?.totalNeto)}
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          {detalle?.estado && <Badge value={detalle.estado} />}
          {puedePdf && (
            <Button
              variant="secondary"
              disabled={Boolean(pdf)}
              onClick={() => bajar(`/api/planillas/${id}/boletas/pdf`, archivoPeriodo(detalle, 'boletas-'), 'todas')}
            >
              <Download size={16} /> {pdf === 'todas' ? 'Descargando…' : 'PDF todas'}
            </Button>
          )}
          {detalle?.estado !== 'CERRADA' && detalle?.estado !== 'ANULADA' && (
            <Button disabled={saving} onClick={calcular}>{saving ? 'Calculando…' : 'Calcular'}</Button>
          )}
          {detalle?.estado === 'CALCULADA' && (
            <Button disabled={saving} onClick={cerrar}>Cerrar y contabilizar</Button>
          )}
          {detalle?.estado === 'CERRADA' && (
            <Button variant="secondary" onClick={() => navigate('/contabilidad')}>Ver asiento</Button>
          )}
        </div>
      </div>

      <Alert>{error}</Alert>
      <Alert ok>{ok}</Alert>

      {boletas.length === 0 ? (
        <div className="rounded-xl border border-line bg-white">
          <Empty text="Aún no hay boletas. Pulse Calcular para generarlas." />
        </div>
      ) : (
        <div className="overflow-x-auto rounded-xl border border-line bg-white">
          <table className="min-w-full text-sm">
            <thead className="bg-slate-50 text-left text-xs uppercase tracking-wide text-slate-500">
              <tr>
                <th className="px-4 py-3">Trabajador</th>
                <th className="px-4 py-3">Básico</th>
                <th className="px-4 py-3">H. extras</th>
                <th className="px-4 py-3">Ausencias</th>
                <th className="px-4 py-3">ONP</th>
                <th className="px-4 py-3">EsSalud</th>
                <th className="px-4 py-3">Neto</th>
                <th className="px-4 py-3 text-right"> </th>
              </tr>
            </thead>
            <tbody>
              {boletas.map((b) => (
                <tr key={b.idDetalle} className="border-t border-line">
                  <td className="px-4 py-3">
                    <p className="font-medium text-navy">{b.empleado}</p>
                    <p className="text-xs text-muted">{b.modalidad}</p>
                  </td>
                  <td className="px-4 py-3">{fmtMoney(b.remuneracionBasica)}</td>
                  <td className="px-4 py-3">{b.horasExtras} h · {fmtMoney(b.montoHorasExtras)}</td>
                  <td className="px-4 py-3">{b.diasNoLaborados} d · {fmtMoney(b.descuentoAusencias)}</td>
                  <td className="px-4 py-3">{fmtMoney(b.onp)}</td>
                  <td className="px-4 py-3">{fmtMoney(b.essalud)}</td>
                  <td className="px-4 py-3 font-semibold text-navy">{fmtMoney(b.neto)}</td>
                  <td className="px-4 py-3 text-right">
                    {puedePdf && (
                      <Button
                        variant="ghost"
                        className="px-2 py-1.5 text-xs"
                        disabled={Boolean(pdf)}
                        onClick={() => bajar(
                          `/api/planillas/${id}/boletas/${b.idDetalle}/pdf`,
                          archivoPeriodo(detalle, 'boleta-', `${slugNombre(b.empleado)}-`),
                          String(b.idDetalle)
                        )}
                      >
                        <Download size={14} /> {pdf === String(b.idDetalle) ? '…' : 'PDF'}
                      </Button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
