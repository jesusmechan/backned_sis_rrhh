import { Alert, Avatar, Badge, Button } from '../ui';
import { PasosInstancia } from '../../pages/flujoShared';
import { factsOf } from '../../pages/bandejaShared';
import { fmtDateTime } from '../../lib/format';
import { SolicitudHistorial } from './SolicitudHistorial';

export function SolicitudVista({
  detalle,
  historial,
  tipoSolicitud,
  subtitle,
  loading,
  error,
  ok,
  extra
}) {
  const facts = factsOf(detalle, tipoSolicitud);

  return (
    <>
      <Alert>{error}</Alert>
      <Alert ok>{ok}</Alert>
      {loading ? (
        <p className="text-sm text-muted">Cargando solicitud…</p>
      ) : !detalle ? null : (
        <div className="grid gap-4 lg:grid-cols-[1.1fr_0.9fr]">
          <section className="rounded-xl border border-line bg-white p-5">
            <div className="flex items-center gap-3">
              <Avatar name={detalle.empleado} />
              <div>
                <p className="font-semibold text-navy">{detalle.empleado}</p>
                <p className="text-xs text-muted">{subtitle}</p>
              </div>
            </div>
            <div className="mt-5 grid gap-3 sm:grid-cols-2">
              {facts.map(([label, value]) => (
                <div key={label}>
                  <p className="text-xs text-slate-500">{label}</p>
                  <p className="mt-0.5 text-sm font-medium text-navy">{value || '—'}</p>
                </div>
              ))}
              <div>
                <p className="text-xs text-slate-500">Registrada</p>
                <p className="mt-0.5 text-sm font-medium text-navy">{fmtDateTime(detalle.fechaCreacion)}</p>
              </div>
            </div>
            {detalle.motivo && (
              <div className="mt-5">
                <p className="text-xs text-slate-500">Motivo</p>
                <p className="mt-1 text-sm text-slate-700">{detalle.motivo}</p>
              </div>
            )}
            {extra}
          </section>
          <div className="space-y-4">
            <section className="rounded-xl border border-line bg-white p-5">
              <p className="mb-3 text-sm font-semibold text-navy">Circuito</p>
              <PasosInstancia pasos={detalle.pasos || []} />
            </section>
            <SolicitudHistorial historial={historial} />
          </div>
        </div>
      )}
    </>
  );
}

export function SolicitudAcciones({ detalle, saving, onCancel, onDuplicar }) {
  if (!detalle) return null;
  return (
    <div className="flex flex-wrap items-center gap-2">
      <Badge value={detalle.estado} />
      {detalle.estado === 'PENDIENTE' && (
        <Button variant="danger" disabled={saving} onClick={onCancel}>
          {saving ? 'Cancelando…' : 'Cancelar solicitud'}
        </Button>
      )}
      <Button variant="secondary" onClick={onDuplicar}>Duplicar</Button>
    </div>
  );
}
