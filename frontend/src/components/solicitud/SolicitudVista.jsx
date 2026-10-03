import { useState } from 'react';
import { ChevronDown, ChevronUp } from 'lucide-react';
import { Alert, Avatar, Badge, Button } from '../ui';
import { useConfig } from '../../auth/ConfigContext';
import { fmtDate, fmtDateTime, fmtRelative, fmtTime } from '../../lib/format';
import { SolicitudHistorial } from './SolicitudHistorial';

function Field({ label, children }) {
  return (
    <div>
      <p className="text-[10px] font-semibold uppercase tracking-[0.12em] text-slate-500">{label}</p>
      <div className="mt-1 text-sm font-semibold leading-snug text-navy">{children || '—'}</div>
    </div>
  );
}

function periodoTexto(detalle, tipoSolicitud) {
  if (!detalle) return '—';
  if (tipoSolicitud === 'HORA_EXTRA' || detalle.cantidadHoras != null) {
    return fmtDate(detalle.fecha);
  }
  if (detalle.fechaInicio === detalle.fechaFin) return fmtDate(detalle.fechaInicio);
  return (
    <>
      {fmtDate(detalle.fechaInicio)}
      <span className="mx-1.5 text-slate-300">→</span>
      {fmtDate(detalle.fechaFin)}
    </>
  );
}

function horarioTexto(detalle, tipoSolicitud) {
  if (!detalle) return '—';
  if (tipoSolicitud === 'HORA_EXTRA' || detalle.cantidadHoras != null) {
    const rango = detalle.horaInicio
      ? `${fmtTime(detalle.horaInicio)} – ${fmtTime(detalle.horaFin)}`
      : '—';
    return detalle.cantidadHoras != null ? `${rango} · ${detalle.cantidadHoras} h` : rango;
  }
  if (detalle.horaInicio) {
    return `${fmtTime(detalle.horaInicio)} – ${fmtTime(detalle.horaFin)}`;
  }
  return 'Jornada completa';
}

function asignadoDe(paso, etiqueta) {
  return (
    paso.usuarioAsignado
    || paso.rol
    || etiqueta('TIPO_APROBADOR', paso.tipoAprobador, '')
    || 'Sin asignar'
  );
}

function Collapsible({ title, hint, meta, defaultOpen = true, children }) {
  const [open, setOpen] = useState(defaultOpen);
  return (
    <section className="rounded-xl border border-line bg-white shadow-sm">
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        className="flex w-full items-center justify-between gap-3 px-4 py-3.5 text-left sm:px-5"
      >
        <div className="min-w-0">
          <p className="text-[10px] font-semibold uppercase tracking-[0.14em] text-slate-500">{title}</p>
          {hint && <p className="mt-0.5 text-xs text-muted">{hint}</p>}
        </div>
        <div className="flex shrink-0 items-center gap-2">
          {meta && <span className="text-xs tabular-nums text-muted">{meta}</span>}
          <span className="grid h-8 w-8 place-items-center rounded-lg bg-surface text-navy ring-1 ring-line">
            {open ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
          </span>
        </div>
      </button>
      {open && <div className="border-t border-line px-4 py-4 sm:px-5">{children}</div>}
    </section>
  );
}

function AprobacionesTimeline({ pasos = [] }) {
  const { etiqueta } = useConfig();
  if (!pasos.length) {
    return <p className="text-sm text-muted">Sin pasos de aprobación.</p>;
  }

  return (
    <ol className="relative space-y-0 border-l border-line pl-4">
      {pasos.map((p, i) => {
        const activo = p.estado === 'EN_CURSO';
        const hecho = p.estado === 'APROBADO';
        const rechazo = p.estado === 'RECHAZADO';
        const last = i === pasos.length - 1;
        const dot = hecho
          ? 'bg-ok'
          : activo
            ? 'bg-navy ring-4 ring-navy/15'
            : rechazo
              ? 'bg-danger'
              : 'bg-slate-300';

        return (
          <li key={p.idPasoSolicitud || p.numeroPaso} className={`relative ${last ? 'pb-0' : 'pb-5'}`}>
            <span className={`absolute -left-[1.35rem] top-1.5 h-2.5 w-2.5 rounded-full ${dot}`} />

            <div className="flex flex-wrap items-center justify-between gap-2">
              <p className="text-sm font-semibold text-navy">
                Paso {p.numeroPaso} · {p.nombrePaso}
              </p>
              <Badge tipo="ESTADO_PASO" value={p.estado} />
            </div>

            {p.usuarioDecision || p.fechaDecision ? (
              <div className="mt-2.5 flex items-start gap-2.5 rounded-lg bg-surface px-3 py-2.5 ring-1 ring-line">
                <Avatar name={p.usuarioDecision || '?'} className="h-8 w-8 text-[10px]" />
                <div className="min-w-0">
                  <p className="text-sm font-semibold text-navy">{p.usuarioDecision || 'Usuario'}</p>
                  <p className="mt-0.5 text-xs text-muted">
                    {hecho ? 'Aprobó' : rechazo ? 'Rechazó' : 'Decidió'}
                    {p.fechaDecision ? (
                      <>
                        {' '}el <span className="font-medium text-slate-700">{fmtDateTime(p.fechaDecision)}</span>
                        {' '}({fmtRelative(p.fechaDecision)})
                      </>
                    ) : null}
                  </p>
                  {p.comentario && (
                    <p className="mt-1 text-xs text-slate-600">“{p.comentario}”</p>
                  )}
                </div>
              </div>
            ) : (
              <div className="mt-2.5 rounded-lg bg-surface px-3 py-2.5 ring-1 ring-line">
                <p className="text-xs text-slate-700">
                  <span className="font-medium text-navy">Responsable:</span> {asignadoDe(p, etiqueta)}
                </p>
                <p className="mt-1 text-[11px] text-muted">
                  {activo
                    ? 'Esperando decisión. Aún no hay hora de aprobación.'
                    : 'Paso aún no iniciado.'}
                </p>
              </div>
            )}
          </li>
        );
      })}
    </ol>
  );
}

export function SolicitudVista({
  detalle,
  historial,
  tipoSolicitud,
  subtitle,
  loading,
  error,
  ok,
  extra,
  compact = false
}) {
  const pasos = detalle?.pasos || [];
  const aprobados = pasos.filter((p) => p.estado === 'APROBADO').length;

  return (
    <>
      <Alert>{error}</Alert>
      <Alert ok>{ok}</Alert>
      {loading ? (
        <p className="text-sm text-muted">Cargando solicitud…</p>
      ) : !detalle ? null : (
        <div className={`grid gap-4 lg:items-start ${compact ? 'lg:grid-cols-1' : 'lg:grid-cols-2'}`}>
          <section className="rounded-xl border border-line bg-white p-5 shadow-sm sm:p-6">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <p className="text-[10px] font-semibold uppercase tracking-[0.14em] text-slate-500">
                Información del permiso
              </p>
              <Badge tipo="ESTADO_SOLICITUD" value={detalle.estado} />
            </div>

            <div className="mt-4 flex items-center gap-3">
              <Avatar name={detalle.empleado} className="h-11 w-11 text-sm" />
              <div className="min-w-0">
                <p className="truncate text-lg font-semibold tracking-tight text-navy">
                  {detalle.empleado}
                </p>
                <p className="mt-0.5 text-xs text-muted">
                  {subtitle || detalle.tipoPermiso || 'Solicitud'}
                </p>
              </div>
            </div>

            <div className="mt-5 space-y-4">
              <Field label="Periodo solicitado">{periodoTexto(detalle, tipoSolicitud)}</Field>
              <Field label="Horario">{horarioTexto(detalle, tipoSolicitud)}</Field>
              <div className="grid gap-4 sm:grid-cols-2">
                <Field label="Flujo">{detalle.flujo}</Field>
                <Field label="Registrada">{fmtDateTime(detalle.fechaCreacion)}</Field>
              </div>
              {detalle.motivo && (
                <div>
                  <p className="text-[10px] font-semibold uppercase tracking-[0.12em] text-slate-500">
                    Motivo
                  </p>
                  <p className="mt-1.5 text-sm leading-relaxed text-slate-700">{detalle.motivo}</p>
                </div>
              )}
            </div>
            {extra}
          </section>

          <div className="space-y-3">
            <Collapsible
              title="Aprobaciones"
              hint="Quién aprobó cada paso y a qué hora"
              meta={`${aprobados}/${pasos.length}`}
              defaultOpen
            >
              <AprobacionesTimeline pasos={pasos} />
            </Collapsible>

            <Collapsible
              title="Historial"
              hint="Línea de tiempo de movimientos"
              meta={`${historial?.length || 0}`}
              defaultOpen={!compact}
            >
              <SolicitudHistorial historial={historial} />
            </Collapsible>
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
      {detalle.estado === 'PENDIENTE' && (
        <Button variant="danger" disabled={saving} onClick={onCancel}>
          {saving ? 'Cancelando…' : 'Cancelar solicitud'}
        </Button>
      )}
      <Button variant="secondary" onClick={onDuplicar}>Duplicar</Button>
    </div>
  );
}
